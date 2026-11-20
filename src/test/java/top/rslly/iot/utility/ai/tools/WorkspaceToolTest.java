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
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import top.rslly.iot.services.agent.WorkspaceService;
import top.rslly.iot.services.agent.ProductSkillsServiceImpl;
import top.rslly.iot.services.McpEndpointConfigService;
import top.rslly.iot.utility.SpringBeanUtils;
import top.rslly.iot.utility.ai.*;
import top.rslly.iot.utility.ai.llm.FunctionToolSpec;
import top.rslly.iot.utility.ai.mcp.*;
import top.rslly.iot.utility.ai.sandbox.SandboxService;
import top.rslly.iot.utility.ai.toolAgent.Agent;

class WorkspaceToolTest {
  WorkspaceService service = mock(WorkspaceService.class);
  WorkspaceTool tool = new WorkspaceTool(service, new ObjectMapper());

  @Test
  void rejectsMissingOrOverriddenContext() {
    assertTrue(tool.run("{}").startsWith("Error:"));
    assertTrue(tool.run("{}", Map.of()).startsWith("Error:"));
    assertTrue(tool.run("{\"action\":\"list\",\"productId\":13}", context("request-1"))
        .startsWith("Error:"));
    assertTrue(tool.run("{\"action\":\"list\",\"chatId\":\"other\"}", context("request-1"))
        .startsWith("Error:"));
    verifyNoInteractions(service);
  }

  @Test
  void usesStableConversationForMultipleRequestsAndManageDispatch() {
    try (var beans = mockStatic(SpringBeanUtils.class)) {
      beans.when(() -> SpringBeanUtils.getBean("workspaceTool")).thenReturn(tool);
      Manage manage = new Manage();
      manage.runTool("workspaceTool", "{\"action\":\"list\"}", context("request-1"));
      manage.runTool("workspaceTool", "{\"action\":\"list\"}", context("request-2"));
      verify(service, times(2)).listFiles(12, "chatProduct12", 1, 20);
    }
  }

  @Test
  void passesReadWriteAndTransferArgumentsToSharedService() {
    Map<String, Object> context = context("request");
    tool.run("{\"action\":\"read\",\"fileId\":\"id\",\"offset\":10,\"limit\":20}", context);
    tool.run("{\"action\":\"write\",\"name\":\"报告.md\",\"content\":\"hello\"}", context);
    when(service.importToSandbox(12, "chatProduct12", "id"))
        .thenReturn("/workspace/imports/id/a.txt");
    assertTrue(tool.run("{\"action\":\"import_to_sandbox\",\"fileId\":\"id\"}", context)
        .contains("sandboxPath"));
    tool.run("{\"action\":\"export_from_sandbox\",\"path\":\"results/report.pdf\"}", context);
    verify(service).read(12, "chatProduct12", "id", 10, 20);
    verify(service).write(12, "chatProduct12", "报告.md", "hello");
    verify(service).exportFromSandbox(12, "chatProduct12", "results/report.pdf");
  }

  @Test
  void dispatchesDeleteAndPreciseEditsUsingStableConversation() throws Exception {
    var context = context("request");
    var deleted = new ObjectMapper().readTree(
        tool.run("{\"action\":\"delete\",\"fileId\":\"id\"}", context));
    assertTrue(deleted.path("deleted").asBoolean());
    assertEquals("id", deleted.path("fileId").asText());
    tool.run("""
        {"action":"str_replace","fileId":"id","old_str":"hello","new_str":""}
        """, context);
    tool.run("""
        {"action":"insert","fileId":"id","insert_line":0,"content":"header\\n"}
        """, context);
    verify(service).delete(12, "chatProduct12", "id");
    verify(service).strReplace(12, "chatProduct12", "id", "hello", "", false);
    verify(service).insert(12, "chatProduct12", "id", 0, "header\n");
  }

  @Test
  void forwardsReplaceAllWhenEnabled() {
    tool.run("""
        {"action":"str_replace","fileId":"id","old_str":"old","new_str":"new","replace_all":true}
        """, context("request"));
    verify(service).strReplace(12, "chatProduct12", "id", "old", "new", true);
  }

  @Test
  void rejectsInvalidEditParametersBeforeCallingService() {
    for (String line : List.of("-1", "0.5", "2147483648", "\"0\"", "null")) {
      assertTrue(tool.run("{\"action\":\"insert\",\"fileId\":\"id\",\"insert_line\":"
          + line + ",\"content\":\"text\"}", context("request")).startsWith("Error:"));
    }
    assertTrue(tool.run("""
        {"action":"str_replace","fileId":"id","old_str":"old"}
        """, context("request")).startsWith("Error:"));
    assertTrue(tool.run("""
        {"action":"str_replace","fileId":"id","old_str":"old","new_str":"new","replace_all":"true"}
        """, context("request")).contains("replace_all must be a boolean"));
    assertTrue(tool.run("{\"action\":\"delete\"}", context("request")).startsWith("Error:"));
    verifyNoInteractions(service);
  }

  @Test
  void returnsServiceFailuresWithoutReportingSuccess() {
    doThrow(new IllegalArgumentException("Workspace file not found in this conversation"))
        .when(service).delete(12, "chatProduct12", "id");
    when(service.strReplace(12, "chatProduct12", "id", "old", "new", false))
        .thenThrow(new IllegalArgumentException("old_str must match exactly once"));
    assertTrue(tool.run("{\"action\":\"delete\",\"fileId\":\"id\"}", context("request"))
        .startsWith("Error:"));
    assertTrue(tool.run("""
        {"action":"str_replace","fileId":"id","old_str":"old","new_str":"new"}
        """, context("request")).contains("Error: old_str must match exactly once"));
  }

  @Test
  void registersWithAgentFunctionCalling() {
    DescriptionUtil descriptions = mock(DescriptionUtil.class);
    when(descriptions.getToolDescriptions(12, "chatProduct12"))
        .thenReturn(Map.of(tool.getName(), tool.getDescription()));
    Agent agent = new Agent();
    ReflectionTestUtils.setField(agent, "descriptionUtil", descriptions);
    List<FunctionToolSpec> specs =
        ReflectionTestUtils.invokeMethod(agent, "buildFunctionToolSpecs", 12, "chatProduct12");
    assertEquals("workspaceTool", specs.getFirst().name());
    assertEquals(tool.getDescription(), specs.getFirst().description());
  }

  @Test
  void registersWithMcpReactAndFunctionCallingEvenWhenSandboxIsDisabled() {
    McpAgent agent = new McpAgent();
    ReflectionTestUtils.setField(agent, "workspaceTool", tool);
    ReflectionTestUtils.setField(agent, "sandboxService", mock(SandboxService.class));
    ReflectionTestUtils.setField(agent, "mcpWebsocket", mock(McpWebsocket.class));
    ReflectionTestUtils.setField(agent, "mcpEndpointConfigService",
        mock(McpEndpointConfigService.class));
    ReflectionTestUtils.setField(agent, "productSkillsService",
        mock(ProductSkillsServiceImpl.class));
    ReflectionTestUtils.setField(agent, "jsExecuteTool", new JsExecuteTool());
    String react = ReflectionTestUtils.invokeMethod(agent, "workspaceToolDescription");
    assertTrue(react.contains("Tool: skills:workspaceTool"));
    Object registry = ReflectionTestUtils.invokeMethod(agent, "buildMcpFunctionToolRegistry", 12,
        "chatProduct12", Map.of());
    List<FunctionToolSpec> specs = ReflectionTestUtils.invokeMethod(registry, "toolSpecs");
    FunctionToolSpec spec =
        specs.stream().filter(item -> item.description().contains(tool.getDescription()))
            .findFirst().orElseThrow();
    Map<String, ?> refs = ReflectionTestUtils.invokeMethod(registry, "refMap");
    ReflectionTestUtils.invokeMethod(agent, "callMcpFunction", refs.get(spec.name()),
        "{\"action\":\"list\"}", context("request-1"), Map.of());
    verify(service).listFiles(12, "chatProduct12", 1, 20);
  }

  private Map<String, Object> context(String request) {
    Map<String, Object> context = new HashMap<>();
    context.put("productId", 12);
    GlobalMessageContext.putChatIds(context, "chatProduct12:stream:" + request, "chatProduct12");
    return context;
  }
}
