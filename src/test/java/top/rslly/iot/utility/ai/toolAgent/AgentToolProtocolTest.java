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
package top.rslly.iot.utility.ai.toolAgent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.alibaba.fastjson.JSON;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import top.rslly.iot.services.AdminConfigServiceImpl;
import top.rslly.iot.services.UserConfigServiceImpl;
import top.rslly.iot.utility.ai.DescriptionUtil;
import top.rslly.iot.utility.ai.LlmDiyUtility;
import top.rslly.iot.utility.ai.Manage;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.llm.FunctionResult;
import top.rslly.iot.utility.ai.llm.FunctionStreamHandler;
import top.rslly.iot.utility.ai.llm.FunctionToolCallMessage;
import top.rslly.iot.utility.ai.llm.FunctionToolResultMessage;
import top.rslly.iot.utility.ai.llm.FunctionToolSpec;
import top.rslly.iot.utility.ai.llm.LLM;
import top.rslly.iot.utility.ai.prompts.ReactPrompt;

class AgentToolProtocolTest {
  private static final String QUESTION = "获取新闻";
  private static final String COMMAND = "curl -s 'https://example.com/' | head -30";
  private static final String ARGUMENTS = JSON.toJSONString(Map.of("args", COMMAND));
  private Agent agent;
  private LLM llm;
  private Manage manage;
  private Map<String, Object> context;
  private Queue<String> output;
  private final List<List<ModelMessage>> requests = new ArrayList<>();

  @BeforeEach
  void setUp() {
    agent = new Agent();
    llm = mock(LLM.class);
    manage = mock(Manage.class);
    LlmDiyUtility utility = mock(LlmDiyUtility.class);
    when(utility.getDiyLlm(12, "test", "4", true)).thenReturn(llm);
    when(llm.supportsFunctionCalling()).thenReturn(true);
    DescriptionUtil descriptions = mock(DescriptionUtil.class);
    when(descriptions.getToolDescriptions(12, "chat-1"))
        .thenReturn(Map.of("bashTool", "Run a shell command"));
    when(descriptions.getTools(12, "chat-1")).thenReturn("bashTool");
    ReactPrompt prompt = mock(ReactPrompt.class);
    when(prompt.getFunctionCalling(QUESTION, 12)).thenReturn("Function system");
    when(prompt.getReact(anyString(), eq(QUESTION), eq(12), anyInt(), anyInt(), anyBoolean()))
        .thenReturn("ReAct system");
    ReflectionTestUtils.setField(agent, "manage", manage);
    ReflectionTestUtils.setField(agent, "llmDiyUtility", utility);
    ReflectionTestUtils.setField(agent, "descriptionUtil", descriptions);
    ReflectionTestUtils.setField(agent, "reactPrompt", prompt);
    ReflectionTestUtils.setField(agent, "adminConfigService", mock(AdminConfigServiceImpl.class));
    ReflectionTestUtils.setField(agent, "userConfigService", mock(UserConfigServiceImpl.class));
    ReflectionTestUtils.setField(agent, "llmName", "test");
    ReflectionTestUtils.setField(agent, "agentMode", "function");
    ReflectionTestUtils.setField(agent, "epochLimit", 5);
    output = new ConcurrentLinkedQueue<>();
    context = new HashMap<>(Map.of("productId", 12, "chatId", "chat-1",
        "queueMap", Map.of("chat-1", output)));
    when(manage.runTool("bashTool", COMMAND, context)).thenReturn("exit code: 0\n新闻正文");
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void unknownFunctionReturnsPairedErrorAndModelCorrectsNameWithoutReact(boolean stream) {
    respond(stream,
        FunctionResult.toolCall("call-wrong", "bash", ARGUMENTS, "progress", "reasoning"),
        FunctionResult.toolCall("call-correct", "bashTool", ARGUMENTS),
        FunctionResult.directReply("新闻摘要"));

    assertEquals("新闻摘要", agent.run(QUESTION, context));

    List<ModelMessage> correctedRequest = requests.get(1);
    FunctionToolCallMessage call = (FunctionToolCallMessage) correctedRequest.get(2).getContent();
    FunctionToolResultMessage result =
        (FunctionToolResultMessage) correctedRequest.get(3).getContent();
    assertEquals("call-wrong", call.toolCallId());
    assertEquals("bash", call.functionName());
    assertEquals(JSON.parseObject(ARGUMENTS), JSON.parseObject(call.arguments()));
    if (!stream) {
      assertEquals("progress", call.content());
    }
    assertEquals("reasoning", call.reasoningContent());
    assertEquals("call-wrong", result.toolCallId());
    assertTrue(result.content().contains("unknown function 'bash'"));
    assertTrue(result.content().contains("bashTool"));
    verify(manage).runTool("bashTool", COMMAND, context);
    verifyNoMoreInteractions(manage);
    verify(llm, never()).jsonChat(anyString(), anyList(), anyBoolean());
    assertEquals("[DONE]", new ArrayList<>(output).getLast());
    assertTrue(agent.getLockMap().isEmpty());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void toolValidationErrorIsReturnedToModelForCorrection(boolean stream) {
    when(manage.runTool("bashTool", "", context))
        .thenReturn("Error: empty command. Provide the full shell command to execute.");
    respond(stream,
        FunctionResult.toolCall("call-invalid", "bashTool", "{\"args\":\"\"}"),
        FunctionResult.toolCall("call-valid", "bashTool", ARGUMENTS),
        FunctionResult.directReply("新闻摘要"));

    assertEquals("新闻摘要", agent.run(QUESTION, context));

    FunctionToolResultMessage error =
        (FunctionToolResultMessage) requests.get(1).get(3).getContent();
    assertTrue(error.content().contains("empty command"));
    verify(manage).runTool("bashTool", "", context);
    verify(manage).runTool("bashTool", COMMAND, context);
    verifyNoMoreInteractions(manage);
  }

  @Test
  void reactStringArgsAreUnwrappedBeforeToolDispatch() {
    ReflectionTestUtils.setField(agent, "agentMode", "prompt");
    when(llm.jsonChat(eq(QUESTION), anyList(), eq(true))).thenReturn(
        JSON.parseObject(JSON.toJSONString(Map.of("action",
            Map.of("name", "bashTool", "args", Map.of("args", COMMAND))))),
        JSON.parseObject("{\"action\":{\"name\":\"finish\",\"args\":{\"content\":\"新闻摘要\"}}}"));

    assertEquals("新闻摘要", agent.run(QUESTION, context));

    verify(manage).runTool("bashTool", COMMAND, context);
    verifyNoMoreInteractions(manage);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void modelFailureAfterSuccessfulToolDoesNotRestartReactOrRepeatOperations(boolean stream) {
    respond(stream, FunctionResult.toolCall("call-executed", "bashTool", ARGUMENTS),
        FunctionResult.error("connection reset"));

    assertTrue(agent.run(QUESTION, context).contains("模型工具调用中断"));

    verify(manage).runTool("bashTool", COMMAND, context);
    verifyNoMoreInteractions(manage);
    verify(llm, never()).jsonChat(anyString(), anyList(), anyBoolean());
    assertEquals(2, requests.size());
    assertEquals("[DONE]", new ArrayList<>(output).getLast());
    assertTrue(agent.getLockMap().isEmpty());
  }

  @Test
  void unsupportedAdapterStillUsesReact() {
    when(llm.supportsFunctionCalling()).thenReturn(false);
    when(llm.jsonChat(eq(QUESTION), anyList(), eq(true))).thenReturn(
        JSON.parseObject("{\"action\":{\"name\":\"finish\",\"args\":{\"content\":\"完成\"}}}"));

    assertEquals("完成", agent.run(QUESTION, context));

    verify(llm, never()).functionChat(anyString(), anyList(), anyList(), anyBoolean());
    verifyNoInteractions(manage);
  }

  private void respond(boolean stream, FunctionResult... results) {
    ReflectionTestUtils.setField(agent, "speedUp", stream);
    AtomicInteger index = new AtomicInteger();
    if (!stream) {
      when(llm.functionChat(eq(QUESTION), anyList(), anyList(), eq(true)))
          .thenAnswer(invocation -> {
            capture(invocation.getArgument(1), invocation.getArgument(2));
            return results[index.getAndIncrement()];
          });
    } else {
      doAnswer(invocation -> {
        capture(invocation.getArgument(1), invocation.getArgument(2));
        FunctionResult result = results[index.getAndIncrement()];
        FunctionStreamHandler handler = invocation.getArgument(4);
        if (result.isToolCall()) {
          handler.onToolCall(result.getToolCallId(), result.getFunctionName(),
              result.getArguments(),
              result.getReasoningContent());
        } else if (result.isError()) {
          handler.onTextDelta("正在处理中");
          handler.onFailure(new IllegalStateException(result.getErrorMessage()));
        } else {
          handler.onTextDelta(result.getReply());
          handler.onDirectReplyComplete(result.getReply());
        }
        return null;
      }).when(llm).streamFunctionChat(eq(QUESTION), anyList(), anyList(), eq(true),
          any(FunctionStreamHandler.class));
    }
  }

  private void capture(List<ModelMessage> messages, List<FunctionToolSpec> specs) {
    requests.add(new ArrayList<>(messages));
    FunctionToolSpec bash = specs.stream().filter(spec -> "bashTool".equals(spec.name()))
        .findFirst().orElseThrow();
    assertEquals(List.of("args"), bash.parameters().get("required"));
    assertFalse(((Map<?, ?>) bash.parameters().get("properties")).containsKey("command"));
  }
}
