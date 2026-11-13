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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import org.junit.jupiter.api.Test;
import top.rslly.iot.utility.ai.llm.FunctionToolCallMessage;
import top.rslly.iot.utility.ai.llm.FunctionToolResultMessage;
import top.rslly.iot.utility.ai.llm.LLM;
import top.rslly.iot.utility.ai.prompts.AgentTaskContextPrompt;
import top.rslly.iot.utility.ai.tools.ToolPrefix;

class AgentTaskContextTest {
  @Test
  void compactsOldFunctionTurnsIntoOneBoundedSummaryAndKeepsRecentPair() {
    LLM model = mock(LLM.class);
    when(model.commonChat(anyString(), any(), anyBoolean()))
        .thenReturn("completed old research");
    List<String> events = new ArrayList<>();
    AgentTaskContext context = context(model, 4000, 0.1, 1, false, events);
    context.append(functionTurn("old-call", "old-result"),
        functionResult("old-call", "historical research ".repeat(200)),
        "historical research ".repeat(200));
    context.append(functionTurn("recent-call", "recent-result"),
        functionResult("recent-call", "recent finding"), "recent finding");

    List<ModelMessage> request = context.prepare("system prompt", List.of());

    assertEquals(1, events.size());
    assertEquals(ToolPrefix.COMPACT.getPrefix(), events.getFirst());
    assertTrue(request.stream().anyMatch(message -> message.getContent()instanceof String content
        && content.contains("completed old research")));
    FunctionToolCallMessage call = (FunctionToolCallMessage) request.get(request.size() - 2)
        .getContent();
    FunctionToolResultMessage result = (FunctionToolResultMessage) request.getLast().getContent();
    assertEquals("recent-call", call.toolCallId());
    assertEquals("recent-call", result.toolCallId());
    verify(model, atLeastOnce()).commonChat(anyString(), any(), anyBoolean());
  }

  @Test
  void summaryRequestContainsToolCallsAndResults() {
    LLM model = mock(LLM.class);
    List<String> summaryInputs = new ArrayList<>();
    when(model.commonChat(anyString(), any(), anyBoolean())).thenAnswer(invocation -> {
      List<ModelMessage> messages = invocation.getArgument(1);
      messages.forEach(message -> summaryInputs.add(String.valueOf(message.getContent())));
      return "completed old research";
    });
    AgentTaskContext context = context(model, 4000, 0.1, 1, false, new ArrayList<>());
    context.append(functionTurn("old-call", "old-result"),
        functionResult("old-call", "historical research ".repeat(200)),
        "historical research ".repeat(200));
    context.append(functionTurn("recent-call", "recent-result"),
        functionResult("recent-call", "recent finding"), "recent finding");

    context.prepare("system prompt", List.of());

    String summaryInput = String.join("\n", summaryInputs);
    assertTrue(summaryInput.contains("historical research"), summaryInput);
    assertTrue(summaryInput.contains("old-call"), summaryInput);
  }

  @Test
  void runtimeContextPrefixesTheTaskMessageAndLeavesSystemStable() {
    AgentTaskContext context = context(mock(LLM.class), 4000, 0.75, 4, false, new ArrayList<>());
    context.setRuntimeContext("<runtime_context>now</runtime_context>\n");

    List<ModelMessage> request = context.prepare("system", List.of());

    assertEquals("system", request.getFirst().getContent());
    assertEquals("<runtime_context>now</runtime_context>\n\noriginal task",
        request.getLast().getContent());
  }

  @Test
  void unlimitedIsAnExecutionSettingAndDoesNotAlterCompaction() {
    AgentTaskContext context = context(mock(LLM.class), 256, 0.75, 4, true, new ArrayList<>());

    assertTrue(context.unlimited());
    assertFalse(context.prepare("system", List.of()).isEmpty());
  }

  @Test
  void rejectsFixedPromptThatCannotFitWithoutEnteringCompactLoop() {
    AgentTaskContext context = context(mock(LLM.class), 8, 0.75, 1, false, new ArrayList<>());

    assertThrows(IllegalArgumentException.class,
        () -> context.prepare("system prompt is too large", List.of()));
  }

  @Test
  void checksCancellationBeforePreparingAnotherModelRequest() {
    AgentTaskContext context = new AgentTaskContext(
        new AgentTaskContext.Settings(256, 0.75, 2, false), "task", List.of(),
        () -> mock(LLM.class), () -> true, ignored -> {
        }, 2, new AgentTaskContextPrompt());

    assertThrows(CancellationException.class, () -> context.prepare("system", List.of()));
  }

  private AgentTaskContext context(LLM model, long window, double ratio, int recent,
      boolean unlimited, List<String> events) {
    return new AgentTaskContext(new AgentTaskContext.Settings(window, ratio, recent, unlimited),
        "original task", List.of(), () -> model, () -> false, events::add, 2,
        new AgentTaskContextPrompt());
  }

  private ModelMessage functionTurn(String callId, String ignored) {
    return new ModelMessage("assistant", new FunctionToolCallMessage(callId, "tool", "{}", ""));
  }

  private ModelMessage functionResult(String callId, String content) {
    return new ModelMessage("tool", new FunctionToolResultMessage(callId, content));
  }
}
