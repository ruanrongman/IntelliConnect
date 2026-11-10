/**
 * Copyright © 2023-2030 The ruanrongman Authors
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package top.rslly.iot.utility.ai.tools;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.alibaba.fastjson.JSON;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import top.rslly.iot.models.KnowledgeGraphicNodeEntity;
import top.rslly.iot.services.UserConfigServiceImpl;
import top.rslly.iot.services.knowledgeGraphic.KnowledgeGraphicService;
import top.rslly.iot.services.knowledgeGraphic.dbo.KnowledgeGraphic;
import top.rslly.iot.services.knowledgeGraphic.dbo.KnowledgeGraphicFileProgress;
import top.rslly.iot.utility.ai.LlmDiyUtility;
import top.rslly.iot.utility.ai.llm.LLM;
import top.rslly.iot.utility.ai.prompts.KnowledgeGraphicPrompt;
import top.rslly.iot.utility.result.ResultTool;

class KnowledgeGraphicAgentGenerationTest {
  @TempDir
  Path temporary;
  private final KnowledgeGraphicTool generator = new KnowledgeGraphicTool();
  private final KnowledgeGraphicService graphService = mock(KnowledgeGraphicService.class);
  private final LlmDiyUtility llms = mock(LlmDiyUtility.class);
  private final LLM llm = mock(LLM.class);
  private final KnowledgeGraphicPrompt prompt = mock(KnowledgeGraphicPrompt.class);
  private final UserConfigServiceImpl config = mock(UserConfigServiceImpl.class);

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(generator, "knowledgeGraphicService", graphService);
    ReflectionTestUtils.setField(generator, "llmDiyUtility", llms);
    ReflectionTestUtils.setField(generator, "knowledgeGraphicPrompt", prompt);
    ReflectionTestUtils.setField(generator, "userConfigService", config);
    ReflectionTestUtils.setField(generator, "llmName", "test");
    ReflectionTestUtils.setField(generator, "maxGraphicLength", 30000);
    ReflectionTestUtils.setField(generator, "chunkChars", 12000);
    when(config.getConfigValue(anyInt(), anyString())).thenReturn("");
    when(llms.getDiyLlm(12, "test", "knowledgeGraphic")).thenReturn(llm);
    when(prompt.getKnowledgeGraphicExtractPrompt(12)).thenReturn("Extract graph");
    when(prompt.buildGraphLimitBlock(anyInt(), anyInt(), anyInt(), anyBoolean())).thenReturn("");
    doReturn(ResultTool.success(new KnowledgeGraphic())).when(graphService)
        .getKnowledgeGraphicByProductId(12);
  }

  @Test
  void stopsAfterModelReturnsIfSwitchWasClosedWithoutWriting() throws Exception {
    AtomicBoolean enabled = new AtomicBoolean(true);
    when(llm.jsonChat(anyString(), anyList(), eq(false))).thenAnswer(invocation -> {
      enabled.set(false);
      return response();
    });
    var staged = generator.prepareFiles(List.of(text("source.txt", "User document")), 10000);
    Path directory = (Path) ReflectionTestUtils.getField(staged, "directory");
    var progress = KnowledgeGraphicFileProgress.create("task", "source.txt");
    generator.generateFiles(12, staged, progress, () -> {
      if (!enabled.get())
        throw new IllegalArgumentException("switch closed");
    });
    assertTrue(progress.isFinished());
    assertFalse(progress.isSuccess());
    assertFalse(Files.exists(directory));
    verify(graphService, never()).addNode(anyString(), anyString(), anyInt());
  }

  @Test
  void sharesZipAndBatchGenerationPipelineAndPreservesNoDeleteMode() throws Exception {
    byte[] zip = zip("inner.txt", "source text");
    var staged = generator.prepareFiles(List.of(
        new KnowledgeGraphicTool.FileSource("bundle.zip", () -> new ByteArrayInputStream(zip)),
        text("second.md", "other text")), 10000);
    when(llm.jsonChat(anyString(), anyList(), eq(false))).thenReturn(response());
    KnowledgeGraphicNodeEntity node = new KnowledgeGraphicNodeEntity();
    node.setId(5L);
    doReturn(ResultTool.success(node)).when(graphService).addNode("设备", "说明", 12);
    KnowledgeGraphic persisted = new KnowledgeGraphic();
    persisted.addNode("设备", "说明");
    doReturn(ResultTool.success(persisted)).when(graphService).getKnowledgeGraphicByProductId(12);
    var progress = KnowledgeGraphicFileProgress.create("task", "files");

    generator.generateFiles(12, staged, progress, () -> {
    });

    assertTrue(progress.isSuccess(), progress.getErrorMessage());
    assertEquals(2, progress.getTotalChunks());
    verify(graphService, times(2)).addNode("设备", "说明", 12);
    verify(graphService, never()).deleteNode(anyLong());
    verify(graphService, never()).deleteRelationByFromAndTo(anyLong(), anyLong());
  }

  @Test
  void rejectsZipTraversalAndExpandedSizeLimitAndCleansStaging() throws Exception {
    for (byte[] zip : List.of(zip("../escape.txt", "text"), zip("large.txt", "x".repeat(2000)))) {
      var staged = generator.prepareFiles(List.of(new KnowledgeGraphicTool.FileSource("bundle.zip",
          () -> new ByteArrayInputStream(zip))), 1024);
      Path directory = (Path) ReflectionTestUtils.getField(staged, "directory");
      var progress = KnowledgeGraphicFileProgress.create("task", "bundle.zip");
      generator.generateFiles(12, staged, progress, () -> {
      });
      assertTrue(progress.isFinished());
      assertFalse(progress.isSuccess());
      assertFalse(Files.exists(directory));
    }
    verifyNoInteractions(llm);
  }

  @Test
  void rejectsInvalidEmptyAndOversizedSourcesBeforeCreatingTasks() {
    assertThrows(IllegalArgumentException.class,
        () -> generator.prepareFiles(List.of(text("script.exe", "data")), 100));
    assertThrows(IllegalArgumentException.class,
        () -> generator.prepareFiles(List.of(text("empty.txt", "")), 100));
    assertThrows(IllegalArgumentException.class,
        () -> generator.prepareFiles(List.of(text("large.txt", "x".repeat(101))), 100));
  }

  @Test
  void limitsAgentZipEntriesWithoutChangingManualUploadLimits() throws Exception {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
      for (int i = 0; i < 1001; i++) {
        zip.putNextEntry(new ZipEntry("folder-" + i + "/"));
        zip.closeEntry();
      }
    }
    byte[] archive = bytes.toByteArray();
    Path file = Files.write(temporary.resolve("directories.zip"), archive);
    List<?> manual = ReflectionTestUtils.invokeMethod(generator, "parseSourceDocuments",
        file, "directories.zip", temporary);
    assertTrue(manual.isEmpty());
    var staged =
        generator.prepareFiles(List.of(new KnowledgeGraphicTool.FileSource("directories.zip",
            () -> new ByteArrayInputStream(archive))), archive.length + 1);
    var progress = KnowledgeGraphicFileProgress.create("task", "directories.zip");
    generator.generateFiles(12, staged, progress, () -> {
    });
    assertFalse(progress.isSuccess());
    assertTrue(progress.getErrorMessage().contains("too many entries"));
  }

  private KnowledgeGraphicTool.FileSource text(String name, String content) {
    return new KnowledgeGraphicTool.FileSource(name,
        () -> new ByteArrayInputStream(content.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
  }

  private com.alibaba.fastjson.JSONObject response() {
    return JSON.parseObject("""
        {"action":{"nodes":[{"name":"设备","des":"说明","nodeAction":"New"},
        {"name":"已有","nodeAction":"Delete"}],"relations":[]}}
        """);
  }

  private byte[] zip(String name, String content) throws Exception {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(output)) {
      zip.putNextEntry(new ZipEntry(name));
      zip.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      zip.closeEntry();
    }
    return output.toByteArray();
  }
}
