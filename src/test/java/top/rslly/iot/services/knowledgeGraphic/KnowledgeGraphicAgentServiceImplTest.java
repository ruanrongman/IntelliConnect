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
package top.rslly.iot.services.knowledgeGraphic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import top.rslly.iot.models.WorkspaceFileEntity;
import top.rslly.iot.services.UserConfigServiceImpl;
import top.rslly.iot.services.agent.WorkspaceService;
import top.rslly.iot.services.knowledgeGraphic.KnowledgeGraphicAgentService.Source;
import top.rslly.iot.services.knowledgeGraphic.dbo.KnowledgeGraphic;
import top.rslly.iot.utility.ai.KnowledgeGraphicExporter;
import top.rslly.iot.utility.ai.sandbox.SandboxService;
import top.rslly.iot.utility.ai.tools.KnowledgeGraphicTool;
import top.rslly.iot.utility.ai.workspace.WorkspaceProperty;
import top.rslly.iot.utility.result.ResultTool;

class KnowledgeGraphicAgentServiceImplTest {
  private final KnowledgeGraphicService graphService = mock(KnowledgeGraphicService.class);
  private final UserConfigServiceImpl config = mock(UserConfigServiceImpl.class);
  private final WorkspaceService workspace = mock(WorkspaceService.class);
  private final SandboxService sandbox = mock(SandboxService.class);
  private final WorkspaceProperty property = new WorkspaceProperty();
  private final TaskExecutor executor = mock(TaskExecutor.class);
  private final KnowledgeGraphicExporter exporter = mock(KnowledgeGraphicExporter.class);
  private final KnowledgeGraphicTool generator = new KnowledgeGraphicTool();
  private final AtomicReference<Runnable> scheduled = new AtomicReference<>();
  private KnowledgeGraphicAgentServiceImpl service;

  @BeforeEach
  void setUp() throws Exception {
    service = new KnowledgeGraphicAgentServiceImpl(graphService, generator, config, workspace,
        property, sandbox, exporter, executor);
    when(config.getConfigValue(12, "knowledge_graph.toggle")).thenReturn("true");
    when(exporter.export(any(), anyString(), anyLong()))
        .thenReturn(
            "{\"nodes\":[],\"relations\":[]}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    doAnswer(invocation -> {
      scheduled.set(invocation.getArgument(0));
      return null;
    }).when(executor).execute(any(Runnable.class));
  }

  @ParameterizedTest
  @ValueSource(strings = {"mermaid", "json"})
  void exportsAllFormatsToWorkspaceWithoutSandbox(String format) {
    var graph = new KnowledgeGraphic();
    graph.addNode("设备", "描述");
    doReturn(ResultTool.success(graph)).when(graphService).getKnowledgeGraphicByProductId(12);
    var file = file("saved", "graph." + ("mermaid".equals(format) ? "mmd" : "json"));
    when(workspace.writeBinary(eq(12), eq("conversation"),
        eq("graph." + ("mermaid".equals(format) ? "mmd" : "json")), any()))
            .thenReturn(file);

    Map<String, Object> result = service.export(12, "conversation", format, "workspace",
        "graph." + ("mermaid".equals(format) ? "mmd" : "json"), null);

    assertEquals("saved", result.get("fileId"));
    assertTrue(result.get("downloadPath").toString().contains("chatId=conversation"));
    verifyNoInteractions(sandbox);
  }

  @ParameterizedTest
  @ValueSource(strings = {"mermaid", "json"})
  void exportsAllFormatsSafelyToSandbox(String format) {
    when(sandbox.isAvailable()).thenReturn(true);
    doReturn(ResultTool.success(new KnowledgeGraphic())).when(graphService)
        .getKnowledgeGraphicByProductId(12);
    when(sandbox.importWorkspaceFile(eq("conversation"), anyString(), any(), anyLong()))
        .thenReturn(true);

    var result = service.export(12, "conversation", format, "sandbox", null, null);

    assertTrue(result.get("sandboxPath").toString()
        .startsWith("/workspace/exports/knowledge-graphic-12-"));
    assertTrue(
        result.get("sandboxPath").toString().endsWith("mermaid".equals(format) ? ".mmd" : ".json"));
    verify(workspace, never()).writeBinary(anyInt(), anyString(), anyString(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"mermaid", "json"})
  void checksSwitchAgainAfterExportingBeforeSaving(String format) throws Exception {
    when(sandbox.isAvailable()).thenReturn(true);
    doReturn(ResultTool.success(new KnowledgeGraphic())).when(graphService)
        .getKnowledgeGraphicByProductId(12);
    doAnswer(invocation -> {
      when(config.getConfigValue(12, "knowledge_graph.toggle")).thenReturn("false");
      return new byte[] {1};
    }).when(exporter).export(any(), eq(format), anyLong());

    assertThrows(IllegalArgumentException.class,
        () -> service.export(12, "conversation", format, "workspace", null, null));
    verify(workspace, never()).writeBinary(anyInt(), anyString(), anyString(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"png", "svg"})
  void rejectsRemovedImageFormats(String format) throws Exception {
    var error = assertThrows(IllegalArgumentException.class,
        () -> service.export(12, "conversation", format, "workspace", null, null));
    assertTrue(error.getMessage().contains("Unsupported export format"));
    verify(exporter, never()).export(any(), anyString(), anyLong());
    verify(workspace, never()).writeBinary(anyInt(), anyString(), anyString(), any());
  }

  @Test
  void exportFailureIsReportedWithoutSavingAnArtifact() throws Exception {
    when(sandbox.isAvailable()).thenReturn(true);
    doReturn(ResultTool.success(new KnowledgeGraphic())).when(graphService)
        .getKnowledgeGraphicByProductId(12);
    when(exporter.export(any(), eq("mermaid"), anyLong()))
        .thenThrow(new java.io.IOException("snapshot too large"));
    var error = assertThrows(IllegalArgumentException.class,
        () -> service.export(12, "conversation", "mermaid", "workspace", null, null));
    assertTrue(error.getMessage().contains("snapshot too large"));
    verify(workspace, never()).writeBinary(anyInt(), anyString(), anyString(), any());
  }

  @Test
  void exportsRealMermaidBytesToBothTargetsAndRejectsWrongExtension() throws Exception {
    var graph = new KnowledgeGraphic();
    graph.addNode("传感器", "描述");
    graph.addNode("空调", "设备");
    graph.addRelation("传感器", "控制", "空调");
    doReturn(ResultTool.success(graph)).when(graphService).getKnowledgeGraphicByProductId(12);
    var actual = new KnowledgeGraphicAgentServiceImpl(graphService, generator, config, workspace,
        property, sandbox, new KnowledgeGraphicExporter(), executor);
    when(workspace.writeBinary(eq(12), eq("conversation"), anyString(), any()))
        .thenReturn(file("saved", "graph.mmd"));
    actual.export(12, "conversation", "mermaid", "workspace", "graph.mmd", null);
    var bytes = org.mockito.ArgumentCaptor.forClass(byte[].class);
    verify(workspace).writeBinary(eq(12), eq("conversation"), eq("graph.mmd"), bytes.capture());
    String text = new String(bytes.getValue(), java.nio.charset.StandardCharsets.UTF_8);
    assertTrue(text.startsWith("flowchart LR\n"));
    assertTrue(text.contains("n0 -->|\"控制\"| n1"));
    verifyNoInteractions(sandbox);
    when(sandbox.isAvailable()).thenReturn(true);
    when(sandbox.importWorkspaceFile(eq("conversation"), eq("/workspace/exports/graph.mmd"),
        any(), anyLong())).thenAnswer(invocation -> {
          try (InputStream input = invocation.getArgument(2)) {
            assertArrayEquals(bytes.getValue(), input.readAllBytes());
          }
          return true;
        });
    actual.export(12, "conversation", "mermaid", "sandbox", null, "exports/graph.mmd");
    verify(sandbox).importWorkspaceFile(eq("conversation"), eq("/workspace/exports/graph.mmd"),
        any(), eq(property.getMaxFileBytes()));
    assertThrows(IllegalArgumentException.class,
        () -> actual.export(12, "conversation", "mermaid", "workspace", "graph.svg", null));
  }

  @Test
  void rejectsDisabledMissingAndUnavailableConfiguration() {
    for (String setting : new String[] {"false", "", null}) {
      when(config.getConfigValue(12, "knowledge_graph.toggle")).thenReturn(setting);
      assertFalse(service.isEnabled(12));
      assertThrows(IllegalArgumentException.class, () -> service.clear(12, "conversation"));
    }
    when(config.getConfigValue(12, "knowledge_graph.toggle"))
        .thenThrow(new IllegalStateException());
    assertFalse(service.isEnabled(12));
    verifyNoInteractions(graphService);
  }

  @Test
  void scopesFilesAndProgressAndReleasesBusyOnCancellation() {
    var file = file("source", "source.txt");
    when(workspace.download(12, "conversation", "source"))
        .thenAnswer(invocation -> new WorkspaceService.Download(file,
            new ByteArrayInputStream("knowledge source".getBytes())));

    String taskId =
        service.startImport(12, "conversation", List.of(new Source("workspace", "source", null)))
            .get("taskId");

    assertThrows(IllegalArgumentException.class, () -> service.clear(12, "conversation"));
    assertThrows(IllegalArgumentException.class, () -> service.startImport(12, "other",
        List.of(new Source("workspace", "source", null))));
    assertThrows(IllegalArgumentException.class, () -> service.progress(12, "other", taskId));
    when(config.getConfigValue(13, "knowledge_graph.toggle")).thenReturn("true");
    assertThrows(IllegalArgumentException.class,
        () -> service.progress(13, "conversation", taskId));
    assertFalse(service.progress(12, "conversation", taskId).isFinished());
    // The unauthenticated legacy page progress endpoint cannot discover Agent progress.
    assertFalse(generator.getFileGenerationProgress(taskId).getSuccess());

    when(config.getConfigValue(12, "knowledge_graph.toggle")).thenReturn("false");
    scheduled.get().run();
    when(config.getConfigValue(12, "knowledge_graph.toggle")).thenReturn("true");
    assertTrue(service.progress(12, "conversation", taskId).isFinished());
    assertFalse(service.progress(12, "conversation", taskId).isSuccess());
    service.clear(12, "conversation");
    verify(graphService).deleteNodes(12);
  }

  @Test
  void rejectsCrossConversationFileAndReleasesBusyOnStagingFailure() {
    when(workspace.download(12, "conversation", "foreign"))
        .thenThrow(new IllegalArgumentException("Workspace file not found in this conversation"));
    assertThrows(IllegalArgumentException.class, () -> service.startImport(12, "conversation",
        List.of(new Source("workspace", "foreign", null))));
    service.clear(12, "conversation");
    verifyNoInteractions(executor);
    verify(graphService).deleteNodes(12);
  }

  @Test
  void refusesUnsafeSandboxPathsUnavailableSourcesAndOversizedExports() {
    when(sandbox.isAvailable()).thenReturn(true);
    for (String path : List.of("/tmp/report.json", "../report.json",
        "/workspace/link/../report.json", "/workspace//report.json")) {
      assertThrows(IllegalArgumentException.class,
          () -> service.export(12, "conversation", "json", "sandbox", null, path));
    }
    assertThrows(IllegalArgumentException.class, () -> service.startImport(12, "conversation",
        List.of(new Source("sandbox", null, "/workspace/missing.pdf"))));
    when(sandbox.isAvailable()).thenReturn(false);
    assertThrows(IllegalArgumentException.class,
        () -> service.export(12, "conversation", "json", "sandbox", null, null));
    property.setMaxFileBytes(1);
    doReturn(ResultTool.success(new KnowledgeGraphic())).when(graphService)
        .getKnowledgeGraphicByProductId(12);
    assertThrows(IllegalArgumentException.class,
        () -> service.export(12, "conversation", "json", "workspace", null, null));
    verify(workspace, never()).writeBinary(anyInt(), anyString(), anyString(), any());
  }

  @Test
  void reportsSandboxCollisionAndWorkspaceQuotaFailure() {
    when(sandbox.isAvailable()).thenReturn(true);
    doReturn(ResultTool.success(new KnowledgeGraphic())).when(graphService)
        .getKnowledgeGraphicByProductId(12);
    assertThrows(IllegalArgumentException.class,
        () -> service.export(12, "conversation", "json", "sandbox", "graph.json", null));
    when(workspace.writeBinary(eq(12), eq("conversation"), anyString(), any()))
        .thenThrow(new IllegalArgumentException("quota exceeded"));
    assertThrows(IllegalArgumentException.class,
        () -> service.export(12, "conversation", "json", "workspace", "graph.json", null));
  }

  @Test
  void readsQueriesAndClearsOnlyTheAuthorizedProduct() {
    var expected = ResultTool.success(new KnowledgeGraphic());
    doReturn(expected).when(graphService).getKnowledgeGraphicByProductId(12);
    doReturn(expected).when(graphService).queryKnowledgeGraphicJsonResult("设备", 12);
    doReturn(ResultTool.success()).when(graphService).deleteNodes(12);
    assertSame(expected, service.read(12, "conversation"));
    assertSame(expected, service.query(12, "conversation", "设备"));
    assertTrue(service.clear(12, "conversation").getSuccess());
    when(workspace.listFiles(12, "foreign", 1, 1))
        .thenThrow(new IllegalArgumentException("Conversation does not belong to product"));
    assertThrows(IllegalArgumentException.class, () -> service.clear(12, "foreign"));
    verify(graphService, times(1)).deleteNodes(12);
  }

  @Test
  void stagesSafeSandboxSourceAndHandlesExecutorRejection() {
    when(sandbox.isAvailable()).thenReturn(true);
    when(sandbox.exportWorkspaceFile("conversation", "/workspace/source.txt",
        property.getMaxFileBytes()))
            .thenReturn("sandbox document".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    String task =
        service.startImport(12, "conversation", List.of(new Source("sandbox", null, "source.txt")))
            .get("taskId");
    assertFalse(service.progress(12, "conversation", task).isFinished());
    when(config.getConfigValue(12, "knowledge_graph.toggle")).thenReturn("false");
    scheduled.get().run();
    when(config.getConfigValue(12, "knowledge_graph.toggle")).thenReturn("true");
    doThrow(new org.springframework.core.task.TaskRejectedException("queue full"))
        .when(executor).execute(any(Runnable.class));
    assertThrows(IllegalArgumentException.class, () -> service.startImport(12, "conversation",
        List.of(new Source("sandbox", null, "source.txt"))));
    service.clear(12, "conversation");
    verify(graphService).deleteNodes(12);
  }

  private WorkspaceFileEntity file(String id, String name) {
    WorkspaceFileEntity file = new WorkspaceFileEntity();
    file.setId(id);
    file.setName(name);
    file.setSize(20);
    return file;
  }
}
