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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import top.rslly.iot.utility.ai.llm.FunctionResult;
import top.rslly.iot.utility.ai.llm.FunctionStreamHandler;
import top.rslly.iot.utility.ai.llm.LLM;

class FunctionCallingRouterProtocolTest {
  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void streamingRetainsRealIdArgumentsAndAssistantContent(boolean withReasoning) {
    FunctionCallingRouterTool tool = new FunctionCallingRouterTool();
    LLM llm = mock(LLM.class);
    String arguments = "{\"args\":\" raw argument \"}";
    doAnswer(invocation -> {
      FunctionStreamHandler handler = invocation.getArgument(4);
      handler.onTextDelta("assistant preamble");
      if (withReasoning) {
        handler.onToolCall("deepseek-real-id", "route_music", arguments, "reasoning");
      } else {
        handler.onToolCall("deepseek-real-id", "route_music", arguments);
      }
      return null;
    }).when(llm).streamFunctionChat(anyString(), anyList(), anyList(), eq(true), any());

    FunctionResult result = ReflectionTestUtils.invokeMethod(tool, "runWithStreaming", "question",
        "chat", llm, List.of(), List.of(), Map.of("chat", new ConcurrentLinkedQueue<String>()),
        null);

    assertTrue(result.isToolCall());
    assertEquals("deepseek-real-id", result.getToolCallId());
    assertEquals("route_music", result.getFunctionName());
    assertEquals(arguments, result.getArguments());
    assertEquals("assistant preamble", result.getReply());
    assertEquals(withReasoning ? "reasoning" : "", result.getReasoningContent());
    verify(llm, times(1)).streamFunctionChat(anyString(), anyList(), anyList(), eq(true), any());
    verifyNoMoreInteractions(llm);
  }
}
