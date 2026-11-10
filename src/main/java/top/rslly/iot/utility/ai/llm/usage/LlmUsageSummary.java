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
}
