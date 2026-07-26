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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversationMemoryPolicyTest {
  private final ConversationMemoryPolicy policy = new ConversationMemoryPolicy(100, 0.5, 4);

  @Test
  void doesNotSummarizeBelowThreshold() {
    List<ModelMessage> messages = messages(9);

    assertTrue(policy.estimateTokens(messages) < policy.summaryThresholdTokens());
    assertFalse(policy.shouldSummarize(messages));
  }

  @Test
  void doesNotSummarizeAtExactThreshold() {
    List<ModelMessage> messages = messages(10);

    assertEquals(policy.summaryThresholdTokens(), policy.estimateTokens(messages));
    assertFalse(policy.shouldSummarize(messages));
  }

  @Test
  void summarizesAboveThresholdAndKeepsFourTurns() {
    List<ModelMessage> messages = messages(12);

    assertTrue(policy.shouldSummarize(messages));
    ModelMessage firstCompacted = messages.getFirst();
    ModelMessage lastCompacted = messages.get(3);
    List<ModelMessage> prefix = policy.compactionPrefix(messages);
    policy.retainRecentTurns(messages);

    assertEquals(4, prefix.size());
    assertEquals(8, messages.size());
    assertSame(firstCompacted, prefix.getFirst());
    assertSame(lastCompacted, prefix.getLast());
  }

  @Test
  void compactionSnapshotIsIndependentFromLiveWindow() {
    List<ModelMessage> messages = messages(12);
    ModelMessage firstCompacted = messages.getFirst();
    List<ModelMessage> prefix = policy.compactionPrefix(messages);

    messages.subList(0, 4).clear();

    assertEquals(4, prefix.size());
    assertSame(firstCompacted, prefix.getFirst());
  }

  @Test
  void recentMessagesSnapshotKeepsLastFourTurnsAndIgnoresLiveListChanges() {
    List<ModelMessage> messages = messages(12);
    ModelMessage expectedFirst = messages.get(4);
    ModelMessage expectedLast = messages.getLast();

    List<ModelMessage> snapshot = policy.recentMessagesSnapshot(messages);
    messages.clear();

    assertEquals(8, snapshot.size());
    assertSame(expectedFirst, snapshot.getFirst());
    assertSame(expectedLast, snapshot.getLast());
  }

  private List<ModelMessage> messages(int count) {
    List<ModelMessage> messages = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      messages.add(new ModelMessage("u", ""));
    }
    return messages;
  }
}
