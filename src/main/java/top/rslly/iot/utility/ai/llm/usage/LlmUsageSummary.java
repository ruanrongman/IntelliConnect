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

import lombok.Data;

import java.util.List;

/**
 * Aggregated usage for a set of model calls. Rates are fractions in [0, 1].
 *
 * <p>
 * {@code cacheHitRate} only counts calls that reported usage. {@code eligibleCacheHitRate} only
 * counts calls whose prompt reached the provider's minimum cacheable length (1024 tokens on
 * DashScope); shorter prompts can never hit and would otherwise drag the rate down.
 */
@Data
public class LlmUsageSummary {
  /** Null for the overall total, set for per-scene entries. */
  private String scene;
  private long calls;
  private long reportedCalls;
  private long promptTokens;
  private long cachedTokens;
  private long cacheCreationTokens;
  private long uncachedPromptTokens;
  private long completionTokens;
  private long reasoningTokens;
  private double cacheHitRate;
  private long eligibleCalls;
  private double eligibleCacheHitRate;
  private long maxPromptTokens;
  private long contextWindowTokens;
  /** maxPromptTokens / contextWindowTokens; 0 when the window is unknown. */
  private double contextUsage;
  private long estimatedPromptTokens;
  /** Local estimate / actual prompt tokens; 0 when unknown. */
  private double estimateRatio;
  /** Input tokens converted to full-price equivalents using the configured cache price ratios. */
  private double effectiveInputTokens;
  private double cacheHitPriceRatio;
  private double cacheCreationPriceRatio;
  private long minCacheableTokens;
  private long latencyMs;
  /** Per-scene breakdown; null on per-scene entries. */
  private List<LlmUsageSummary> scenes;
  /** Number of chat turns summed into this summary; 0 for a single turn. */
  private long turns;

  /**
   * Adds up the stored summaries of a conversation's turns, oldest first. Token counts, calls and
   * latency are summed and the cache hit rate is recomputed from the sums. Context fields describe
   * the current context, so they come from the latest turn. Per-call details that cannot be summed
   * exactly (eligible hit rate, estimate ratio, scenes) are left empty.
   *
   * @return null when there is no turn
   */
  public static LlmUsageSummary sumOfTurns(List<LlmUsageSummary> turns) {
    if (turns == null || turns.isEmpty()) {
      return null;
    }
    LlmUsageSummary total = new LlmUsageSummary();
    LlmUsageSummary latest = null;
    for (LlmUsageSummary turn : turns) {
      if (turn == null) {
        continue;
      }
      latest = turn;
      total.turns++;
      total.calls += turn.calls;
      total.reportedCalls += turn.reportedCalls;
      total.promptTokens += turn.promptTokens;
      total.cachedTokens += turn.cachedTokens;
      total.cacheCreationTokens += turn.cacheCreationTokens;
      total.uncachedPromptTokens += turn.uncachedPromptTokens;
      total.completionTokens += turn.completionTokens;
      total.reasoningTokens += turn.reasoningTokens;
      total.eligibleCalls += turn.eligibleCalls;
      total.estimatedPromptTokens += turn.estimatedPromptTokens;
      total.effectiveInputTokens += turn.effectiveInputTokens;
      total.latencyMs += turn.latencyMs;
    }
    if (latest == null) {
      return null;
    }
    total.cacheHitRate = LlmUsageAccumulator.ratio(total.cachedTokens, total.promptTokens);
    total.maxPromptTokens = latest.maxPromptTokens;
    total.contextWindowTokens = latest.contextWindowTokens;
    total.contextUsage = latest.contextUsage;
    total.cacheHitPriceRatio = latest.cacheHitPriceRatio;
    total.cacheCreationPriceRatio = latest.cacheCreationPriceRatio;
    total.minCacheableTokens = latest.minCacheableTokens;
    return total;
  }
}
