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
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import top.rslly.iot.services.McpEndpointConfigService;
import top.rslly.iot.services.agent.ProductSkillsServiceImpl;
import top.rslly.iot.services.agent.WorkspaceService;
import top.rslly.iot.utility.SpringBeanUtils;
import top.rslly.iot.utility.ai.DescriptionUtil;
import top.rslly.iot.utility.ai.LlmDiyUtility;
import top.rslly.iot.utility.ai.Manage;
import top.rslly.iot.utility.ai.llm.FunctionToolSpec;
import top.rslly.iot.utility.ai.llm.LLM;
import top.rslly.iot.utility.ai.mcp.McpAgent;
import top.rslly.iot.utility.ai.mcp.McpWebsocket;
import top.rslly.iot.utility.ai.sandbox.SandboxService;
import top.rslly.iot.utility.ai.toolAgent.Agent;

class ImageToolTest {
  private static final int MAX_BYTES = 4 * 1024 * 1024;
  private final ObjectMapper mapper = new ObjectMapper();
  private final Map<String, Object> context = Map.of("productId", 12,
      "chatId", "request-1", "conversationChatId", "conversation-1");
  private final SandboxService sandbox = mock(SandboxService.class);
  private final LlmDiyUtility models = mock(LlmDiyUtility.class);
  private final LLM model = mock(LLM.class);
  private ImageTool tool;

  @BeforeEach
  void setUp() {
    tool = new ImageTool();
    ReflectionTestUtils.setField(tool, "sandboxService", sandbox);
    ReflectionTestUtils.setField(tool, "objectMapper", mapper);
    ReflectionTestUtils.setField(tool, "llmDiyUtility", models);
    ReflectionTestUtils.setField(tool, "visionModel", "test-vision");
    when(sandbox.isAvailable()).thenReturn(true);
    when(models.getDiyLlm(12, "test-vision", "image")).thenReturn(model);
    when(model.imageToWord(anyString(), anyString()))
        .thenReturn("The title is clipped at the right edge.");
  }

  @ParameterizedTest
  @ValueSource(strings = {"png", "jpeg", "gif"})
  void sendsRealImageBytesAndInspectionPromptToProductVisionModel(String format) throws Exception {
    byte[] bytes = image(format);
    when(sandbox.exportWorkspaceFile("conversation-1", "/workspace/output/screenshot.bin",
        MAX_BYTES))
            .thenReturn(bytes);
    String result = tool
        .run("{\"path\":\"output/screenshot.bin\",\"prompt\":\"Check title clipping\"}", context);
    assertTrue(result.contains("The title is clipped"), result);
    ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
    verify(model).imageToWord(prompt.capture(), url.capture());
    assertTrue(prompt.getValue().contains("Check title clipping"));
    assertTrue(prompt.getValue().contains("untrusted data"));
    assertTrue(url.getValue().startsWith("data:image/" + format + ";base64,"));
    assertArrayEquals(bytes, Base64.getDecoder().decode(url.getValue().split(",", 2)[1]));
    assertFalse(result.contains("base64"));
    verify(models).getDiyLlm(12, "test-vision", "image");
  }

  @Test
  void detectsWebpByContentAndUsesDefaultVisualInspectionPrompt() {
    byte[] webp =
        Base64.getDecoder().decode("UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEADsD+JaQAA3AAAAAA");
    when(sandbox.exportWorkspaceFile("conversation-1", "/workspace/screenshot.webp", MAX_BYTES))
        .thenReturn(webp);
    assertTrue(
        tool.run("{\"path\":\"/workspace/screenshot.webp\"}", context).contains("The title"));
    verify(model).imageToWord(contains("Inspect this image visually"),
        startsWith("data:image/webp;base64,"));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"bad json", "null", "[]", "{}", "{\"path\":4}",
      "{\"path\":\" \"}", "{\"path\":\"a.png\",\"prompt\":null}",
      "{\"path\":\"a.png\",\"prompt\":\" \"}",
      "{\"path\":\"a.png\",\"productId\":13}",
      "{\"path\":\"a.png\",\"chatId\":\"other\"}", "{\"path\":\"/etc/image.png\"}"})
  void rejectsInvalidInputBeforeReadingFilesOrCallingModel(String input) {
    assertTrue(tool.run(input, context).startsWith("Error:"));
    verify(sandbox, never()).exportWorkspaceFile(anyString(), anyString(), anyLong());
    verifyNoInteractions(models, model);
  }

  @Test
  void rejectsMissingContextAndDisabledSandbox() {
    assertTrue(tool.run("{}").contains("context"));
    assertTrue(tool.run("{}", Map.of("productId", 0, "chatId", "x")).contains("context"));
    when(sandbox.isAvailable()).thenReturn(false);
    assertTrue(tool.run("{\"path\":\"a.png\"}", context).contains("disabled"));
    verify(sandbox, never()).exportWorkspaceFile(anyString(), anyString(), anyLong());
    verifyNoInteractions(models, model);
  }

  @Test
  void rejectsUnreadableOversizedOrNonImageFilesWithoutCallingVision() {
    when(sandbox.exportWorkspaceFile("conversation-1", "/workspace/file", MAX_BYTES))
        .thenReturn(null, new byte[0], new byte[MAX_BYTES + 1],
            "<svg><text>not a bitmap</text></svg>".getBytes(StandardCharsets.UTF_8));
    for (int i = 0; i < 4; i++) {
      assertTrue(tool.run("{\"path\":\"file\"}", context).startsWith("Error:"));
    }
    verifyNoInteractions(models, model);
  }

  @Test
  void usesStableConversationIdAndManageDispatchWithoutExposingBytes() throws Exception {
    when(sandbox.exportWorkspaceFile("conversation-1", "/workspace/a.png", MAX_BYTES))
        .thenReturn(image("png"));
    try (var beans = mockStatic(SpringBeanUtils.class)) {
      beans.when(() -> SpringBeanUtils.getBean("imageTool")).thenReturn(tool);
      Manage manage = new Manage();
      assertTrue(
          manage.runTool("imageTool", "{\"path\":\"a.png\"}", context).contains("The title"));
      assertTrue(manage.runTool("imageTool", "{\"path\":\"a.png\"}", Map.of("productId", 12,
          "chatId", "request-2", "conversationChatId", "conversation-1")).contains("The title"));
    }
    verify(sandbox, times(2)).exportWorkspaceFile("conversation-1", "/workspace/a.png", MAX_BYTES);
    // Other conversations cannot read this mocked conversation's file.
    assertTrue(tool.run("{\"path\":\"a.png\"}", Map.of("productId", 12,
        "chatId", "other-conversation")).startsWith("Error:"));
    verify(model, times(2)).imageToWord(anyString(), anyString());
  }

  @Test
  void reportsEmptyOrFailedModelResponses() throws Exception {
    when(sandbox.exportWorkspaceFile("conversation-1", "/workspace/a.png", MAX_BYTES))
        .thenReturn(image("png"));
    when(model.imageToWord(anyString(), anyString()))
        .thenReturn(null, " ").thenThrow(new IllegalStateException("provider unavailable"));
    for (int i = 0; i < 3; i++) {
      String result = tool.run("{\"path\":\"a.png\"}", context);
      assertTrue(result.startsWith("Error:"), result);
      assertFalse(result.contains("Vision model response"));
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void registersWithLegacyAgentAndMcpAndExecutesMcpFunction(boolean available) throws Exception {
    when(sandbox.isAvailable()).thenReturn(available);
    McpAgent mcp = new McpAgent();
    ReflectionTestUtils.setField(mcp, "sandboxService", sandbox);
    ReflectionTestUtils.setField(mcp, "bashTool", new BashTool());
    ReflectionTestUtils.setField(mcp, "editTool", new EditTool());
    ReflectionTestUtils.setField(mcp, "grepTool", new GrepTool());
    ReflectionTestUtils.setField(mcp, "imageTool", tool);
    WorkspaceTool workspace = new WorkspaceTool(mock(WorkspaceService.class), mapper);
    ReflectionTestUtils.setField(mcp, "workspaceTool", workspace);
    ReflectionTestUtils.setField(mcp, "jsExecuteTool", new JsExecuteTool());
    ReflectionTestUtils.setField(mcp, "mcpWebsocket", mock(McpWebsocket.class));
    ReflectionTestUtils.setField(mcp, "mcpEndpointConfigService",
        mock(McpEndpointConfigService.class));
    ReflectionTestUtils.setField(mcp, "productSkillsService", mock(ProductSkillsServiceImpl.class));
    String react = ReflectionTestUtils.invokeMethod(mcp, "sandboxToolDescription");
    assertEquals(available, react.contains("skills:imageTool"));
    Object registry = ReflectionTestUtils.invokeMethod(mcp, "buildMcpFunctionToolRegistry", 12,
        "request-1", Map.of());
    List<FunctionToolSpec> specs = ReflectionTestUtils.invokeMethod(registry, "toolSpecs");
    var spec =
        specs.stream().filter(item -> item.description().equals(tool.getDescription())).findFirst();
    assertEquals(available, spec.isPresent());

    DescriptionUtil descriptions = new DescriptionUtil();
    ReflectionTestUtils.setField(descriptions, "sandboxService", sandbox);
    ReflectionTestUtils.setField(descriptions, "bashTool", new BashTool());
    ReflectionTestUtils.setField(descriptions, "editTool", new EditTool());
    ReflectionTestUtils.setField(descriptions, "grepTool", new GrepTool());
    ReflectionTestUtils.setField(descriptions, "imageTool", tool);
    ReflectionTestUtils.setField(descriptions, "workspaceTool", workspace);
    KnowledgeTool knowledge = mock(KnowledgeTool.class);
    when(knowledge.getDescription(12)).thenReturn("");
    ReflectionTestUtils.setField(descriptions, "knowledgeTool", knowledge);
    ReflectionTestUtils.setField(descriptions, "mcpAgent", mock(McpAgent.class));
    assertEquals(available,
        descriptions.getToolDescriptions(12, "request-1").containsKey("imageTool"));
    Agent agent = new Agent();
    ReflectionTestUtils.setField(agent, "descriptionUtil", descriptions);
    List<FunctionToolSpec> agentSpecs =
        ReflectionTestUtils.invokeMethod(agent, "buildFunctionToolSpecs", 12, "request-1");
    assertEquals(available, agentSpecs.stream().anyMatch(item -> item.name().equals("imageTool")));
    if (available) {
      assertEquals(FunctionToolSpec.defaultStringArgsSchema(), spec.orElseThrow().parameters());
      Map<String, ?> refs = ReflectionTestUtils.invokeMethod(registry, "refMap");
      when(sandbox.exportWorkspaceFile("conversation-1", "/workspace/a.png", MAX_BYTES))
          .thenReturn(image("png"));
      String result = ReflectionTestUtils.invokeMethod(mcp, "callMcpFunction",
          refs.get(spec.orElseThrow().name()), "{\"path\":\"a.png\"}", context, Map.of());
      assertTrue(result.contains("The title"), result);
    }
  }

  private byte[] image(String format) throws Exception {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    assertTrue(ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), format, output));
    return output.toByteArray();
  }
}
