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
package top.rslly.iot.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import top.rslly.iot.utility.ai.llm.usage.LlmUsageTracker;

/**
 * Applies the {@code ai.usage} settings to {@link LlmUsageTracker}. Usage is only logged and sent
 * with each chat reply; nothing is persisted.
 */
@Configuration
public class LlmUsageConfig {
  @Value("${ai.usage.enabled:true}")
  private boolean enabled;
  @Value("${ai.usage.stream-include-usage:true}")
  private boolean streamIncludeUsage;
  @Value("${ai.usage.cache-hit-price-ratio:0.2}")
  private double cacheHitPriceRatio;
  @Value("${ai.usage.cache-creation-price-ratio:1.25}")
  private double cacheCreationPriceRatio;
  @Value("${ai.usage.min-cacheable-tokens:1024}")
  private long minCacheableTokens;

  @PostConstruct
  void configure() {
    LlmUsageTracker.configure(enabled, streamIncludeUsage, cacheHitPriceRatio,
        cacheCreationPriceRatio, minCacheableTokens);
  }
}
