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
package top.rslly.iot.utility.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import okhttp3.sse.EventSourceListener;
import top.rslly.iot.services.AdminConfigServiceImpl;
import top.rslly.iot.services.McpEndpointConfigService;
import top.rslly.iot.services.UserConfigServiceImpl;
import top.rslly.iot.services.agent.McpServerServiceImpl;
import top.rslly.iot.services.agent.ProductSkillsServiceImpl;
import top.rslly.iot.utility.ai.llm.FunctionResult;
import top.rslly.iot.utility.ai.llm.FunctionToolSpec;
import top.rslly.iot.utility.ai.llm.LLM;
import top.rslly.iot.utility.ai.mcp.McpAgent;
import top.rslly.iot.utility.ai.mcp.McpWebsocket;
import top.rslly.iot.utility.ai.prompts.ReactPrompt;
import top.rslly.iot.utility.ai.sandbox.SandboxService;
import top.rslly.iot.utility.ai.toolAgent.Agent;
import top.rslly.iot.utility.ai.tools.JsExecuteTool;
import top.rslly.iot.utility.ai.tools.WorkspaceTool;

class AgentConversationContinuationTest {
  private static final String OFFER = "工作区有 part1.txt，需要我打开看看里面写了什么吗？";
  private static final String FILE_ARGS = "{\"action\":\"read\",\"fileId\":\"file-1\"}";
  private static final String RESULT = "part1.txt 的内容是：测试正文。";
  private Map<String, Object> context;
  private List<ModelMessage> memory;
  private LLM llm;
  private LlmDiyUtility llmDiyUtility;
  private ReactPrompt reactPrompt;
  private final AtomicInteger modelCalls = new AtomicInteger();

  @BeforeEach
  void setUp() {
    memory = new ArrayList<>(List.of(
        new ModelMessage("user", "列出工作区文件，part1.txt 的文件 ID 是 file-1"),
        new ModelMessage("assistant", OFFER)));
    context = new HashMap<>();
    context.put("productId", 12);
    GlobalMessageContext.putChatIds(context, "stream-2", "conversation-1");
    context.put("memory", memory);
    context.put("queueMap", Map.of("stream-2", new ConcurrentLinkedQueue<String>()));
    llm = mock(LLM.class);
    llmDiyUtility = mock(LlmDiyUtility.class);
    reactPrompt = mock(ReactPrompt.class);
    when(llm.supportsFunctionCalling()).thenReturn(true);
    when(llmDiyUtility.getDiyLlm(eq(12), eq("test"), anyString(), eq(true)))
        .thenReturn(llm);
    when(reactPrompt.getReact(anyString(), anyString(), anyInt(), anyInt(), anyInt(),
        anyBoolean())).thenReturn("ReAct system");
    when(reactPrompt.getFunctionCalling(anyString(), eq(12))).thenReturn("Function system");
  }

  @ParameterizedTest
  @ValueSource(strings = {"prompt", "function", "stream"})
  void agentUsesNativeHistoryAndExecutesAcceptedFileRead(String mode) {
    Agent agent = new Agent();
    configure(agent, "agentMode", mode);
    DescriptionUtil descriptionUtil = mock(DescriptionUtil.class);
    Manage manage = mock(Manage.class);
    ReflectionTestUtils.setField(agent, "descriptionUtil", descriptionUtil);
    ReflectionTestUtils.setField(agent, "manage", manage);
    when(descriptionUtil.getTools(12, "stream-2")).thenReturn("workspace tool");
    when(descriptionUtil.getToolDescriptions(12, "stream-2"))
        .thenReturn(Map.of("workspaceTool", "workspace tool"));
    when(manage.runTool(eq("workspaceTool"), anyString(), eq(context)))
        .thenReturn("测试正文");
    respondWithReadThenAnswer("workspaceTool");

    assertEquals(RESULT, agent.run("好的", context));

    verify(manage).runTool("workspaceTool", FILE_ARGS, context);
    assertMemoryUnchanged();
  }

  @ParameterizedTest
  @ValueSource(strings = {"prompt", "function", "stream"})
  void mcpAgentReadsWorkspaceWithoutRemoteServersOrSandbox(String mode) {
    McpAgent agent = new McpAgent();
    configure(agent, "mcpAgentMode", mode);
    ReflectionTestUtils.setField(agent, "mcpServerService", mock(McpServerServiceImpl.class));
    ReflectionTestUtils.setField(agent, "productSkillsService",
        mock(ProductSkillsServiceImpl.class));
    ReflectionTestUtils.setField(agent, "mcpWebsocket", mock(McpWebsocket.class));
    ReflectionTestUtils.setField(agent, "mcpEndpointConfigService",
        mock(McpEndpointConfigService.class));
    ReflectionTestUtils.setField(agent, "sandboxService", mock(SandboxService.class));
    WorkspaceTool workspaceTool = mock(WorkspaceTool.class);
    when(workspaceTool.getName()).thenReturn("workspaceTool");
    when(workspaceTool.getDescription()).thenReturn("workspace tool");
    when(workspaceTool.run(anyString(), eq(context))).thenReturn("测试正文");
    ReflectionTestUtils.setField(agent, "workspaceTool", workspaceTool);
    JsExecuteTool jsExecuteTool = mock(JsExecuteTool.class);
    when(jsExecuteTool.getName()).thenReturn("jsExecuteTool");
    when(jsExecuteTool.getDescription()).thenReturn("js tool");
    ReflectionTestUtils.setField(agent, "jsExecuteTool", jsExecuteTool);
    respondWithReadThenAnswer("skills:workspaceTool");

    assertEquals(RESULT, agent.run("好的", context));

    verify(workspaceTool).run(FILE_ARGS, context);
    assertMemoryUnchanged();
  }

  private void configure(Object agent, String modeField, String mode) {
    ReflectionTestUtils.setField(agent, modeField, "stream".equals(mode) ? "prompt" : mode);
    ReflectionTestUtils.setField(agent, "speedUp", "stream".equals(mode));
    ReflectionTestUtils.setField(agent, "llmName", "test");
    ReflectionTestUtils.setField(agent, "epochLimit", 4);
    ReflectionTestUtils.setField(agent, "llmDiyUtility", llmDiyUtility);
    ReflectionTestUtils.setField(agent, "reactPrompt", reactPrompt);
    ReflectionTestUtils.setField(agent, "adminConfigService", mock(AdminConfigServiceImpl.class));
    ReflectionTestUtils.setField(agent, "userConfigService", mock(UserConfigServiceImpl.class));
  }

  private void respondWithReadThenAnswer(String reactToolName) {
    doAnswer(invocation -> {
      assertHistory(invocation.getArgument(1));
      String response;
      if (modelCalls.getAndIncrement() == 0) {
        response = "<｜DSML｜ calls>\n<｜DSML｜ invoke name=\"" + reactToolName + "\">\n"
            + "<｜DSML｜ parameter name=\"args\" string=\"false\">" + FILE_ARGS
            + "</｜DSML｜ parameter>\n</｜DSML｜ invoke>\n</｜DSML｜ calls>";
      } else {
        response = "{\"action\":{\"name\":\"finish\",\"args\":{\"content\":\"" + RESULT + "\"}}}";
      }
      EventSourceListener listener = invocation.getArgument(3);
      // Split inside DSML tags and JSON values to exercise real stream accumulation.
      for (int offset = 0; offset < response.length(); offset += 7) {
        String fragment = response.substring(offset, Math.min(offset + 7, response.length()));
        listener.onEvent(null, null, null, JSON.toJSONString(
            Map.of("choices", List.of(Map.of("delta", Map.of("content", fragment))))));
      }
      listener.onEvent(null, null, null, "[DONE]");
      return null;
    }).when(llm).streamJsonChat(eq("好的"), anyList(), eq(true), any(EventSourceListener.class));
    when(llm.jsonChat(eq("好的"), anyList(), eq(true))).thenAnswer(invocation -> {
      assertHistory(invocation.getArgument(1));
      if (modelCalls.getAndIncrement() == 0) {
        JSONObject action = new JSONObject();
        action.put("name", reactToolName);
        action.put("args", JSON.parseObject(FILE_ARGS));
        JSONObject response = new JSONObject();
        response.put("action", action);
        return response;
      }
      return JSON.parseObject("{\"action\":{\"name\":\"finish\",\"args\":{\"content\":\""
          + RESULT + "\"}}}");
    });
    when(llm.functionChat(eq("好的"), anyList(), anyList(), eq(true))).thenAnswer(invocation -> {
      List<ModelMessage> messages = invocation.getArgument(1);
      assertHistory(messages);
      if (modelCalls.getAndIncrement() == 0) {
        List<FunctionToolSpec> specs = invocation.getArgument(2);
        String functionName = specs.stream()
            .filter(spec -> "workspace tool".equals(spec.description()))
            .findFirst().orElseThrow().name();
        JSONObject args = new JSONObject();
        args.put("args", FILE_ARGS);
        return FunctionResult.toolCall(functionName, args.toJSONString());
      }
      return FunctionResult.directReply(RESULT);
    });
  }

  private void assertHistory(List<ModelMessage> messages) {
    assertEquals("system", messages.getFirst().getRole());
    assertEquals(memory.getFirst(), messages.get(1));
    assertNotSame(memory.getFirst(), messages.get(1));
    assertEquals("assistant", messages.get(2).getRole());
    assertEquals(OFFER, messages.get(2).getContent());
    assertEquals("好的", messages.get(3).getContent());
  }

  private void assertMemoryUnchanged() {
    assertEquals(2, modelCalls.get());
    assertEquals(2, memory.size());
    assertEquals(OFFER, memory.getLast().getContent());
  }
}
