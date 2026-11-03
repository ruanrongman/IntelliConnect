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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import top.rslly.iot.models.WorkspaceFileEntity;
import top.rslly.iot.services.McpEndpointConfigService;
import top.rslly.iot.services.agent.ProductSkillsServiceImpl;
import top.rslly.iot.services.agent.ProductToolsBanServiceImpl;
import top.rslly.iot.services.agent.WorkspaceService;
import top.rslly.iot.utility.SpringBeanUtils;
import top.rslly.iot.utility.ai.DescriptionUtil;
import top.rslly.iot.utility.ai.Manage;
import top.rslly.iot.utility.ai.llm.FunctionToolSpec;
import top.rslly.iot.utility.ai.mcp.McpAgent;
import top.rslly.iot.utility.ai.mcp.McpWebsocket;
import top.rslly.iot.utility.ai.pdf.MarkdownPdfProperty;
import top.rslly.iot.utility.ai.pdf.MarkdownPdfRenderer;
import top.rslly.iot.utility.ai.sandbox.SandboxService;
import top.rslly.iot.utility.ai.toolAgent.Agent;

class MarkdownToPdfToolTest {
  private final ObjectMapper mapper = new ObjectMapper();
  private final Map<String, Object> context = Map.of("productId", 12, "chatId", "stream-1",
      "conversationChatId", "conversation-1");
  private SandboxService sandbox;
  private WorkspaceService workspace;
  private ProductToolsBanServiceImpl bans;
  private MarkdownPdfRenderer renderer;
  private MarkdownPdfProperty property;
  private MarkdownToPdfTool tool;

  @BeforeEach
  void setUp() {
    sandbox = mock(SandboxService.class);
    workspace = mock(WorkspaceService.class);
    bans = mock(ProductToolsBanServiceImpl.class);
    renderer = mock(MarkdownPdfRenderer.class);
    property = new MarkdownPdfProperty();
    when(sandbox.isAvailable()).thenReturn(true);
    tool = new MarkdownToPdfTool(sandbox, workspace, bans, mapper, renderer, property);
  }

  @Test
  void convertsInJavaAndPersistsUsingStableConversationWithoutSandboxCommands() throws Exception {
    byte[] markdown = "# 中文报告".getBytes(StandardCharsets.UTF_8);
    byte[] pdf = "%PDF-fake-for-orchestration-test".getBytes(StandardCharsets.US_ASCII);
    when(sandbox.exportWorkspaceFile("conversation-1", "/workspace/报告 ' $(hello).md",
        property.getMaxSourceBytes()))
            .thenReturn(markdown);
    when(renderer.render(eq(markdown), eq("/workspace/报告 ' $(hello).md"), eq("标题 <&>"), any()))
        .thenAnswer(call -> {
          java.util.function.Function<String, byte[]> loader = call.getArgument(3);
          loader.apply("/workspace/img/chart.png");
          return new MarkdownPdfRenderer.Rendered(pdf, 2, List.of("Missing optional image"));
        });
    WorkspaceFileEntity file = file();
    when(workspace.writeBinary(12, "conversation-1", "报告.pdf", pdf)).thenReturn(file);

    var result =
        mapper.readTree(tool.run(mapper.writeValueAsString(Map.of("path", "报告 ' $(hello).md",
            "outputName", "报告.pdf", "title", "标题 <&>")), context));

    assertTrue(result.path("success").asBoolean(), result.toString());
    assertEquals("saved-file", result.path("fileId").asText());
    assertEquals(2, result.path("pages").asInt());
    assertEquals("Missing optional image", result.path("warnings").get(0).asText());
    assertTrue(result.path("downloadPath").asText().contains("chatId=conversation-1"));
    verify(sandbox).exportWorkspaceFile("conversation-1", "/workspace/img/chart.png",
        property.getMaxImageBytes());
    verify(sandbox, never()).runCommand(anyString(), anyString());
    verify(sandbox, never()).writeFile(anyString(), anyString(), any(byte[].class));
    verify(workspace, never()).exportFromSandbox(anyInt(), anyString(), anyString());
  }

  @Test
  void defaultsFilenameAndTitleFromSource() throws Exception {
    when(sandbox.exportWorkspaceFile(anyString(), anyString(), anyLong()))
        .thenReturn(new byte[] {1});
    when(renderer.render(any(), eq("/workspace/reports/a.md"), eq("a"), any()))
        .thenReturn(new MarkdownPdfRenderer.Rendered(new byte[] {2}, 1, List.of()));
    when(workspace.writeBinary(eq(12), eq("conversation-1"), eq("a.pdf"), any()))
        .thenReturn(file());
    assertTrue(mapper.readTree(tool.run("{\"path\":\"reports/a.md\"}", context)).path("success")
        .asBoolean());
  }

  @Test
  void rechecksBanEvenWhenDispatchedDirectlyByManage() throws Exception {
    assertTrue(tool.isAvailable(12));
    when(bans.getProductToolsBanList(12)).thenReturn(List.of(MarkdownToPdfTool.BAN_KEY));
    try (var beans = mockStatic(SpringBeanUtils.class)) {
      beans.when(() -> SpringBeanUtils.getBean(MarkdownToPdfTool.NAME)).thenReturn(tool);
      assertTrue(new Manage().runTool(MarkdownToPdfTool.NAME, "{\"path\":\"report.md\"}", context)
          .contains("TOOL_DISABLED"));
    }
    assertFalse(tool.isAvailable(12));
    verify(sandbox, never()).exportWorkspaceFile(anyString(), anyString(), anyLong());
    verifyNoInteractions(renderer, workspace);
  }

  @ParameterizedTest
  @ValueSource(strings = {"null", "[]", "bad json", "{}", "{\"path\":4}",
      "{\"path\":\"/etc/report.md\"}", "{\"path\":\"../report.md\"}",
      "{\"path\":\"/workspace/../report.md\"}", "{\"path\":\"report.txt\"}",
      "{\"path\":\"report.md\",\"outputName\":\"../a.pdf\"}",
      "{\"path\":\"report.md\",\"outputName\":\"a.md\"}",
      "{\"path\":\"report.md\",\"title\":null}",
      "{\"path\":\"report.md\",\"productId\":99}",
      "{\"path\":\"report.md\",\"chatId\":\"other\"}",
      "{\"path\":\"report.md\",\"outputPath\":\"a.pdf\"}"})
  void rejectsInvalidArgumentsBeforeRemoteReads(String input) throws Exception {
    assertFalse(mapper.readTree(tool.run(input, context)).path("success").asBoolean());
    verify(sandbox, never()).exportWorkspaceFile(anyString(), anyString(), anyLong());
    verifyNoInteractions(renderer, workspace);
  }

  @Test
  void rejectsMissingContextUnavailableSandboxAndMissingSource() {
    assertTrue(tool.run("{}", null).contains("INVALID_CONTEXT"));
    assertTrue(tool.run("{}", Map.of("productId", 12)).contains("INVALID_CONTEXT"));
    when(sandbox.isAvailable()).thenReturn(false);
    assertTrue(tool.run("{}", context).contains("SANDBOX_UNAVAILABLE"));
    when(sandbox.isAvailable()).thenReturn(true);
    assertTrue(convert().contains("SOURCE_UNAVAILABLE"));
    verifyNoInteractions(renderer, workspace);
  }

  @Test
  void reportsStorageFailureWithoutReturningSuccessfulDownload() throws Exception {
    when(sandbox.exportWorkspaceFile(anyString(), anyString(), anyLong()))
        .thenReturn(new byte[] {1});
    when(renderer.render(any(), anyString(), anyString(), any()))
        .thenReturn(new MarkdownPdfRenderer.Rendered(new byte[] {2}, 1, List.of()));
    when(workspace.writeBinary(anyInt(), anyString(), anyString(), any()))
        .thenThrow(new IllegalArgumentException("Storage limit exceeded"));
    var result = mapper.readTree(convert());
    assertFalse(result.path("success").asBoolean());
    assertEquals("STORAGE_FAILED", result.path("code").asText());
    assertFalse(result.has("downloadPath"));
  }

  @Test
  void returnsSpecificRendererFailureWithoutPublishingPdf() {
    when(sandbox.exportWorkspaceFile(anyString(), anyString(), anyLong()))
        .thenReturn(new byte[] {1});
    when(renderer.render(any(), anyString(), anyString(), any()))
        .thenThrow(new MarkdownPdfRenderer.PdfException("MISSING_FONT", "Configure backend font"));
    assertTrue(convert().contains("MISSING_FONT"));
    verifyNoInteractions(workspace);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void registersInBothAgentsAndRechecksBanForStaleMcpCalls(boolean disabled) {
    when(bans.getProductToolsBanList(12))
        .thenReturn(disabled ? List.of(MarkdownToPdfTool.BAN_KEY) : List.of());
    McpAgent mcp = new McpAgent();
    ReflectionTestUtils.setField(mcp, "sandboxService", sandbox);
    ReflectionTestUtils.setField(mcp, "markdownToPdfTool", tool);
    ReflectionTestUtils.setField(mcp, "bashTool", new BashTool());
    ReflectionTestUtils.setField(mcp, "editTool", new EditTool());
    ReflectionTestUtils.setField(mcp, "grepTool", new GrepTool());
    ReflectionTestUtils.setField(mcp, "imageTool", new ImageTool());
    ReflectionTestUtils.setField(mcp, "workspaceTool", new WorkspaceTool(workspace, mapper));
    ReflectionTestUtils.setField(mcp, "jsExecuteTool", new JsExecuteTool());
    ReflectionTestUtils.setField(mcp, "mcpWebsocket", mock(McpWebsocket.class));
    ReflectionTestUtils.setField(mcp, "mcpEndpointConfigService",
        mock(McpEndpointConfigService.class));
    ReflectionTestUtils.setField(mcp, "productSkillsService", mock(ProductSkillsServiceImpl.class));
    String react = ReflectionTestUtils.invokeMethod(mcp, "markdownPdfToolDescription", 12);
    assertEquals(!disabled, react.contains("skills:markdownToPdfTool"));
    Object registry = ReflectionTestUtils.invokeMethod(mcp, "buildMcpFunctionToolRegistry", 12,
        "stream-1", Map.of());
    List<FunctionToolSpec> specs = ReflectionTestUtils.invokeMethod(registry, "toolSpecs");
    var spec =
        specs.stream().filter(s -> s.description().equals(tool.getDescription())).findFirst();
    assertEquals(!disabled, spec.isPresent());

    DescriptionUtil descriptions = new DescriptionUtil();
    ReflectionTestUtils.setField(descriptions, "sandboxService", sandbox);
    ReflectionTestUtils.setField(descriptions, "markdownToPdfTool", tool);
    ReflectionTestUtils.setField(descriptions, "bashTool", new BashTool());
    ReflectionTestUtils.setField(descriptions, "editTool", new EditTool());
    ReflectionTestUtils.setField(descriptions, "grepTool", new GrepTool());
    ReflectionTestUtils.setField(descriptions, "imageTool", new ImageTool());
    ReflectionTestUtils.setField(descriptions, "workspaceTool",
        new WorkspaceTool(workspace, mapper));
    KnowledgeTool knowledge = mock(KnowledgeTool.class);
    when(knowledge.getDescription(12)).thenReturn("");
    ReflectionTestUtils.setField(descriptions, "knowledgeTool", knowledge);
    ReflectionTestUtils.setField(descriptions, "mcpAgent", mock(McpAgent.class));
    assertEquals(!disabled, descriptions.getTools(12, "stream-1").contains(MarkdownToPdfTool.NAME));
    Agent agent = new Agent();
    ReflectionTestUtils.setField(agent, "descriptionUtil", descriptions);
    List<FunctionToolSpec> agentSpecs =
        ReflectionTestUtils.invokeMethod(agent, "buildFunctionToolSpecs", 12, "stream-1");
    assertEquals(!disabled,
        agentSpecs.stream().anyMatch(s -> s.name().equals(MarkdownToPdfTool.NAME)));
    if (!disabled) {
      Map<String, ?> refs = ReflectionTestUtils.invokeMethod(registry, "refMap");
      when(bans.getProductToolsBanList(12)).thenReturn(List.of(MarkdownToPdfTool.BAN_KEY));
      String response = ReflectionTestUtils.invokeMethod(mcp, "callMcpFunction",
          refs.get(spec.orElseThrow().name()),
          "{\"path\":\"report.md\"}", context, Map.of());
      assertTrue(response.contains("TOOL_DISABLED"));
      verifyNoInteractions(renderer, workspace);
    }
  }

  private String convert() {
    return tool.run("{\"path\":\"report.md\"}", context);
  }

  private WorkspaceFileEntity file() {
    WorkspaceFileEntity file = new WorkspaceFileEntity();
    file.setId("saved-file");
    file.setName("报告.pdf");
    file.setSize(1024);
    return file;
  }
}
