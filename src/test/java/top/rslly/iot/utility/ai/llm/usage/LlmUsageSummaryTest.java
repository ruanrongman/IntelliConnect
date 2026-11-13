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
package top.rslly.iot.utility.ai.llm.usage;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LlmUsageSummaryTest {

  @Test
  void sumsTurnsAndTakesContextFromLatestTurn() {
    LlmUsageSummary first = turn(2, 3000, 1000, 2000, 100, 8000, 0.03);
    LlmUsageSummary second = turn(1, 5000, 4500, 500, 200, 5000, 0.02);
    second.setCacheCreationTokens(64);

    LlmUsageSummary total = LlmUsageSummary.sumOfTurns(List.of(first, second));

    assertEquals(2, total.getTurns());
    assertEquals(3, total.getCalls());
    assertEquals(8000, total.getPromptTokens());
    assertEquals(5500, total.getCachedTokens());
    assertEquals(2500, total.getUncachedPromptTokens());
    assertEquals(64, total.getCacheCreationTokens());
    assertEquals(300, total.getCompletionTokens());
    // 5500 / 8000, recomputed from the sums rather than averaging the per-turn rates
    assertEquals(0.6875, total.getCacheHitRate());
    // Context describes the current conversation, i.e. the latest turn
    assertEquals(5000, total.getMaxPromptTokens());
    assertEquals(0.02, total.getContextUsage());
    assertEquals(250000, total.getContextWindowTokens());
    assertNull(total.getScenes());
  }

  @Test
  void emptyOrNullTurnsGiveNoTotal() {
    assertNull(LlmUsageSummary.sumOfTurns(null));
    assertNull(LlmUsageSummary.sumOfTurns(List.of()));
    assertNull(LlmUsageSummary.sumOfTurns(Arrays.asList(null, null)));
    LlmUsageSummary only =
        LlmUsageSummary.sumOfTurns(Arrays.asList(null, turn(1, 10, 0, 10, 1, 10, 0)));
    assertEquals(1, only.getTurns());
    assertEquals(0D, only.getCacheHitRate());
  }

  private static LlmUsageSummary turn(long calls, long prompt, long cached, long uncached,
      long completion, long maxPrompt, double contextUsage) {
    LlmUsageSummary summary = new LlmUsageSummary();
    summary.setCalls(calls);
    summary.setReportedCalls(calls);
    summary.setPromptTokens(prompt);
    summary.setCachedTokens(cached);
    summary.setUncachedPromptTokens(uncached);
    summary.setCompletionTokens(completion);
    summary.setMaxPromptTokens(maxPrompt);
    summary.setContextWindowTokens(250000);
    summary.setContextUsage(contextUsage);
    return summary;
  }
}
