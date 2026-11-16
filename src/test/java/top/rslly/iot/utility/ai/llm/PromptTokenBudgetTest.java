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
package top.rslly.iot.utility.ai.llm;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.llm.usage.LlmUsage;
import top.rslly.iot.utility.ai.llm.usage.LlmUsageContext;

class PromptTokenBudgetTest {
  private final PromptTokenBudget budget = new PromptTokenBudget();
  private final List<FunctionToolSpec> tools = List.of(new FunctionToolSpec("weather",
      "weather lookup", FunctionToolSpec.defaultRouterArgsSchema()));

  @Test
  void usesActualInputIncludingCachedTokensAndEstimatesOnlyTheDelta() {
    try (var scope = LlmUsageContext.open(1, "chat", null)) {
      List<ModelMessage> first = prompt(1);
      assertEquals(local(first), budget.estimate(first, tools));
      budget.record(budget.capture(first, tools), new LlmUsage(2000, 1800, 0, 9000, 0));
      List<ModelMessage> next = prompt(2);
      assertEquals(2000 + local(next) - local(first), budget.estimate(next, tools));
      next.set(next.size() - 1, new ModelMessage("user", "additional context ".repeat(100)));
      assertEquals(2000 + local(next) - local(first), budget.estimate(next, tools));
    }
  }

  @Test
  void isolatesProductsConversationsAndModelClients() {
    try (var scope = LlmUsageContext.open(1, "chat", null)) {
      budget.record(budget.capture(prompt(1), tools), new LlmUsage(2000, 0, 0, 0, 0));
      assertEquals(local(prompt(2)), new PromptTokenBudget().estimate(prompt(2), tools));
    }
    try (var scope = LlmUsageContext.open(2, "chat", null)) {
      assertEquals(local(prompt(2)), budget.estimate(prompt(2), tools));
    }
    try (var scope = LlmUsageContext.open(1, "other-chat", null)) {
      assertEquals(local(prompt(2)), budget.estimate(prompt(2), tools));
    }
    assertEquals(local(prompt(2)), budget.estimate(prompt(2), tools));
  }

  @Test
  void resetsAfterCompactionHistoryEditsOrToolSchemaChanges() {
    try (var scope = LlmUsageContext.open(1, "chat", null)) {
      budget.record(budget.capture(prompt(3), tools), new LlmUsage(2000, 0, 0, 0, 0));
      assertEquals(local(prompt(1)), budget.estimate(prompt(1), tools));
      List<ModelMessage> edited = prompt(4);
      edited.set(1, new ModelMessage("user", "edited question", "r1"));
      assertEquals(local(edited), budget.estimate(edited, tools));
      List<ModelMessage> changedSystem = prompt(4);
      changedSystem.set(0, new ModelMessage("system", "different instructions"));
      assertEquals(2000 + local(changedSystem) - local(prompt(3)),
          budget.estimate(changedSystem, tools));
      assertEquals(PromptTokenBudget.localEstimate(prompt(4), List.of()),
          budget.estimate(prompt(4), List.of()));
    }
  }

  @Test
  void missingUsageInvalidatesBaselineAndLateCompletionCannotRestoreIt() {
    try (var scope = LlmUsageContext.open(1, "chat", null)) {
      var older = budget.capture(prompt(1), tools);
      var newer = budget.capture(prompt(2), tools);
      budget.record(older, new LlmUsage(2000, 0, 0, 0, 0));
      budget.record(newer, null);
      budget.record(older, new LlmUsage(5000, 0, 0, 0, 0));
      assertEquals(local(prompt(3)), budget.estimate(prompt(3), tools));
      budget.record(budget.capture(prompt(3), tools), new LlmUsage(3000, 0, 0, 0, 0));
      assertEquals(3000 + local(prompt(4)) - local(prompt(3)), budget.estimate(prompt(4), tools));
    }
  }

  @Test
  void auxiliaryPromptsWithDifferentToolsDoNotReplaceConversationBaseline() {
    try (var scope = LlmUsageContext.open(1, "chat", null)) {
      budget.record(budget.capture(prompt(1), tools), new LlmUsage(2000, 0, 0, 0, 0));
      List<ModelMessage> background = List.of(new ModelMessage("system", "extract memory"),
          new ModelMessage("user", "history"));
      budget.record(budget.capture(background, List.of()), new LlmUsage(99999, 0, 0, 0, 0));
      assertEquals(2000 + local(prompt(2)) - local(prompt(1)), budget.estimate(prompt(2), tools));
    }
  }

  private long local(List<ModelMessage> messages) {
    return PromptTokenBudget.localEstimate(messages, tools);
  }

  @Test
  void fallsBackWhenRemovedContextMakesTheCalibratedBudgetInvalid() {
    try (var scope = LlmUsageContext.open(1, "chat", null)) {
      List<ModelMessage> first = prompt(1);
      first.set(0, new ModelMessage("system", "字".repeat(10000)));
      budget.record(budget.capture(first, tools), new LlmUsage(1000, 0, 0, 0, 0));
      List<ModelMessage> next = prompt(2);
      assertEquals(local(next), budget.estimate(next, tools));
    }
  }

  static List<ModelMessage> prompt(int turn) {
    List<ModelMessage> messages = new ArrayList<>();
    messages.add(new ModelMessage("system", "stable instructions"));
    for (int index = 1; index < turn; index++) {
      messages.add(new ModelMessage("user", "question " + index, "r" + index));
      messages.add(new ModelMessage("assistant", "answer " + index, "r" + index));
    }
    messages.add(new ModelMessage("user", "question " + turn));
    return messages;
  }
}
