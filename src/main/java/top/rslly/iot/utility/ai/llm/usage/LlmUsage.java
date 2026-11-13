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

import com.openai.core.JsonValue;
import com.openai.models.completions.CompletionUsage;

import java.util.Map;
import java.util.Optional;

/**
 * Token usage reported by the provider for one model call.
 *
 * <p>
 * {@code cachedTokens} and {@code cacheCreationTokens} are subsets of {@code promptTokens}
 * (DashScope / OpenAI semantics).
 */
public record LlmUsage(long promptTokens, long cachedTokens, long cacheCreationTokens,
    long completionTokens, long reasoningTokens) {

  /**
   * Parses the OpenAI-compatible usage block. Reads {@code prompt_tokens_details.cached_tokens}
   * and {@code prompt_tokens_details.cache_creation_input_tokens} (DashScope), falling back to the
   * official DeepSeek {@code prompt_cache_hit_tokens} field.
   */
  public static LlmUsage fromOpenAi(CompletionUsage usage) {
    if (usage == null) {
      return null;
    }
    long prompt = usage._promptTokens().asKnown().orElse(0L);
    long completion = usage._completionTokens().asKnown().orElse(0L);
    long cached = 0L;
    long creation = 0L;
    var promptDetails = safe(usage::promptTokensDetails);
    if (promptDetails.isPresent()) {
      cached = safe(() -> promptDetails.get().cachedTokens()).orElse(0L);
      creation = readLong(promptDetails.get()._additionalProperties(),
          "cache_creation_input_tokens");
    }
    if (cached == 0L) {
      cached = readLong(usage._additionalProperties(), "prompt_cache_hit_tokens");
    }
    long reasoning = safe(usage::completionTokensDetails)
        .flatMap(details -> safe(details::reasoningTokens))
        .orElse(0L);
    return new LlmUsage(prompt, cached, creation, completion, reasoning);
  }

  /** Prompt tokens billed at the normal input price (neither cache hit nor cache creation). */
  public long uncachedPromptTokens() {
    return Math.max(0L, promptTokens - cachedTokens - cacheCreationTokens);
  }

  private static long readLong(Map<String, JsonValue> properties, String key) {
    if (properties == null) {
      return 0L;
    }
    JsonValue value = properties.get(key);
    if (value == null) {
      return 0L;
    }
    Optional<?> number = value.asNumber();
    return number.filter(Number.class::isInstance)
        .map(item -> ((Number) item).longValue())
        .orElse(0L);
  }

  private static <T> Optional<T> safe(java.util.function.Supplier<Optional<T>> supplier) {
    try {
      Optional<T> value = supplier.get();
      return value == null ? Optional.empty() : value;
    } catch (RuntimeException e) {
      return Optional.empty();
    }
  }
}
