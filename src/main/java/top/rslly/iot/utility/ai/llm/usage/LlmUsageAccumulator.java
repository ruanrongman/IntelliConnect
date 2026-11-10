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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Thread-safe running totals for a chat turn, a file task, or a stats query. */
public class LlmUsageAccumulator {
  private final Totals total = new Totals();
  private final Map<String, Totals> scenes = new LinkedHashMap<>();
  private long contextWindowTokens;

  public LlmUsageAccumulator() {}

  public LlmUsageAccumulator(long contextWindowTokens) {
    this.contextWindowTokens = Math.max(0L, contextWindowTokens);
  }

  public synchronized void add(LlmCallRecord record) {
    if (record == null) {
      return;
    }
    long minCacheable = LlmUsageTracker.minCacheableTokens();
    total.add(record, minCacheable);
    String scene = record.scene() == null ? "unknown" : record.scene();
    scenes.computeIfAbsent(scene, key -> new Totals()).add(record, minCacheable);
  }

  public synchronized void setContextWindowTokensIfUnset(long tokens) {
    if (contextWindowTokens <= 0 && tokens > 0) {
      contextWindowTokens = tokens;
    }
  }

  public synchronized boolean isEmpty() {
    return total.calls == 0;
  }

  public synchronized LlmUsageSummary summary() {
    LlmUsageSummary summary = total.toSummary(null, contextWindowTokens);
    List<LlmUsageSummary> sceneSummaries = new ArrayList<>();
    scenes.forEach(
        (scene, totals) -> sceneSummaries.add(totals.toSummary(scene, contextWindowTokens)));
    sceneSummaries.sort((left, right) -> Long.compare(right.getPromptTokens()
        + right.getCompletionTokens(), left.getPromptTokens() + left.getCompletionTokens()));
    summary.setScenes(sceneSummaries);
    return summary;
  }

  private static final class Totals {
    private long calls;
    private long reportedCalls;
    private long promptTokens;
    private long cachedTokens;
    private long cacheCreationTokens;
    private long completionTokens;
    private long reasoningTokens;
    private long maxPromptTokens;
    private long estimatedPromptTokens;
    private long estimatedActualPromptTokens;
    private long eligibleCalls;
    private long eligiblePromptTokens;
    private long eligibleCachedTokens;
    private long latencyMs;

    private void add(LlmCallRecord record, long minCacheableTokens) {
      calls++;
      latencyMs += Math.max(0L, record.latencyMs());
      LlmUsage usage = record.usage();
      if (usage == null) {
        return;
      }
      reportedCalls++;
      promptTokens += usage.promptTokens();
      cachedTokens += usage.cachedTokens();
      cacheCreationTokens += usage.cacheCreationTokens();
      completionTokens += usage.completionTokens();
      reasoningTokens += usage.reasoningTokens();
      maxPromptTokens = Math.max(maxPromptTokens, usage.promptTokens());
      if (record.estimatedPromptTokens() > 0) {
        estimatedPromptTokens += record.estimatedPromptTokens();
        estimatedActualPromptTokens += usage.promptTokens();
      }
      if (usage.promptTokens() >= minCacheableTokens) {
        eligibleCalls++;
        eligiblePromptTokens += usage.promptTokens();
        eligibleCachedTokens += usage.cachedTokens();
      }
    }

    private LlmUsageSummary toSummary(String scene, long contextWindowTokens) {
      double hitRatio = LlmUsageTracker.cacheHitPriceRatio();
      double creationRatio = LlmUsageTracker.cacheCreationPriceRatio();
      long uncached = Math.max(0L, promptTokens - cachedTokens - cacheCreationTokens);
      LlmUsageSummary summary = new LlmUsageSummary();
      summary.setScene(scene);
      summary.setCalls(calls);
      summary.setReportedCalls(reportedCalls);
      summary.setPromptTokens(promptTokens);
      summary.setCachedTokens(cachedTokens);
      summary.setCacheCreationTokens(cacheCreationTokens);
      summary.setUncachedPromptTokens(uncached);
      summary.setCompletionTokens(completionTokens);
      summary.setReasoningTokens(reasoningTokens);
      summary.setCacheHitRate(ratio(cachedTokens, promptTokens));
      summary.setEligibleCalls(eligibleCalls);
      summary.setEligibleCacheHitRate(ratio(eligibleCachedTokens, eligiblePromptTokens));
      summary.setMaxPromptTokens(maxPromptTokens);
      summary.setContextWindowTokens(contextWindowTokens);
      summary.setContextUsage(ratio(maxPromptTokens, contextWindowTokens));
      summary.setEstimatedPromptTokens(estimatedPromptTokens);
      summary.setEstimateRatio(ratio(estimatedPromptTokens, estimatedActualPromptTokens));
      summary.setEffectiveInputTokens(round(uncached + cachedTokens * hitRatio
          + cacheCreationTokens * creationRatio));
      summary.setCacheHitPriceRatio(hitRatio);
      summary.setCacheCreationPriceRatio(creationRatio);
      summary.setMinCacheableTokens(LlmUsageTracker.minCacheableTokens());
      summary.setLatencyMs(latencyMs);
      return summary;
    }
  }

  static double ratio(long numerator, long denominator) {
    if (denominator <= 0) {
      return 0D;
    }
    return round((double) numerator / denominator);
  }

  private static double round(double value) {
    return Math.round(value * 10000D) / 10000D;
  }
}
