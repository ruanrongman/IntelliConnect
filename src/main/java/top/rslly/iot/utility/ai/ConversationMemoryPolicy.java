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

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.rslly.iot.services.UserConfigServiceImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Keeps the conversation window bounded without a model-specific tokenizer. */
@Component
public class ConversationMemoryPolicy {
  public static final String CONTEXT_WINDOW_TOKENS_CONFIG_KEY = "memory.context-window-tokens";
  public static final String SUMMARY_THRESHOLD_RATIO_CONFIG_KEY =
      "memory.summary-threshold-ratio";
  public static final String RECENT_TURNS_CONFIG_KEY = "memory.recent-turns";

  private static final long MAX_CONTEXT_WINDOW_TOKENS = 1_000_000L;
  private static final double MIN_SUMMARY_THRESHOLD_RATIO = 0.1D;
  private static final int MAX_RECENT_TURNS = 100;
  private static final int MESSAGE_OVERHEAD_TOKENS = 4;

  private final long contextWindowTokens;
  private final double summaryThresholdRatio;
  private final int recentTurns;

  @Resource
  private UserConfigServiceImpl userConfigService;

  public ConversationMemoryPolicy(
      @Value("${ai.memory.context-window-tokens:2000}") long contextWindowTokens,
      @Value("${ai.memory.summary-threshold-ratio:0.5}") double summaryThresholdRatio,
      @Value("${ai.memory.recent-turns:4}") int recentTurns) {
    this.contextWindowTokens = Math.max(1L,
        Math.min(MAX_CONTEXT_WINDOW_TOKENS, contextWindowTokens));
    this.summaryThresholdRatio =
        Math.min(1D, Math.max(MIN_SUMMARY_THRESHOLD_RATIO, summaryThresholdRatio));
    this.recentTurns = Math.max(1, Math.min(MAX_RECENT_TURNS, recentTurns));
  }

  /** Resolves a product-scoped snapshot without changing this singleton's defaults. */
  public ConversationMemoryPolicy resolveForProduct(int productId) {
    if (userConfigService == null || productId <= 0) {
      return this;
    }
    long productContextWindowTokens = readLongConfig(productId,
        CONTEXT_WINDOW_TOKENS_CONFIG_KEY, contextWindowTokens, 1L,
        MAX_CONTEXT_WINDOW_TOKENS);
    double productSummaryThresholdRatio = readDoubleConfig(productId,
        SUMMARY_THRESHOLD_RATIO_CONFIG_KEY, summaryThresholdRatio,
        MIN_SUMMARY_THRESHOLD_RATIO, 1D);
    int productRecentTurns = readIntConfig(productId, RECENT_TURNS_CONFIG_KEY,
        recentTurns, 1, MAX_RECENT_TURNS);
    return new ConversationMemoryPolicy(productContextWindowTokens,
        productSummaryThresholdRatio, productRecentTurns);
  }

  public long estimateTokens(List<ModelMessage> messages) {
    if (messages == null || messages.isEmpty()) {
      return 0L;
    }
    long tokenCount = 0L;
    for (ModelMessage message : messages) {
      if (message == null) {
        continue;
      }
      tokenCount += MESSAGE_OVERHEAD_TOKENS;
      tokenCount += estimateText(message.getRole());
      tokenCount += estimateText(message.getContent());
    }
    return tokenCount;
  }

  public boolean shouldSummarize(List<ModelMessage> messages) {
    return messages != null
        && recentTurnsStart(messages) > 0
        && estimateTokens(messages) > summaryThresholdTokens();
  }

  public List<ModelMessage> compactionPrefix(List<ModelMessage> messages) {
    if (!shouldSummarize(messages)) {
      return List.of();
    }
    int prefixSize = recentTurnsStart(messages);
    return Collections.unmodifiableList(new ArrayList<>(messages.subList(0, prefixSize)));
  }

  public void retainRecentTurns(List<ModelMessage> messages) {
    if (messages == null || messages.isEmpty()) {
      return;
    }
    messages.subList(0, recentTurnsStart(messages)).clear();
  }

  public List<ModelMessage> copyMessages(List<ModelMessage> messages) {
    if (messages == null || messages.isEmpty()) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList(new ArrayList<>(messages));
  }

  public List<ModelMessage> recentMessagesSnapshot(List<ModelMessage> messages) {
    if (messages == null || messages.isEmpty()) {
      return Collections.emptyList();
    }
    int fromIndex = recentTurnsStart(messages);
    return copyMessages(messages.subList(fromIndex, messages.size()));
  }

  public int retainedMessageCount() {
    return recentTurns * 2;
  }

  private int recentTurnsStart(List<ModelMessage> messages) {
    int turns = 0;
    for (int index = messages.size() - 1; index >= 0; index--) {
      ModelMessage message = messages.get(index);
      if (message == null) {
        continue;
      }
      String requestId = message.getRequestId();
      boolean start = requestId != null
          ? index == 0 || messages.get(index - 1) == null
              || !requestId.equals(messages.get(index - 1).getRequestId())
          : ModelMessageRole.USER.value().equals(message.getRole());
      if (start && ++turns == recentTurns) {
        return index;
      }
    }
    return 0;
  }

  public long summaryThresholdTokens() {
    return (long) Math.floor(contextWindowTokens * summaryThresholdRatio);
  }

  public long contextWindowTokens() {
    return contextWindowTokens;
  }

  private long readLongConfig(int productId, String key, long defaultValue, long min,
      long max) {
    try {
      String value = userConfigService.getConfigValue(productId, key);
      if (value == null || value.isBlank()) {
        return defaultValue;
      }
      long parsed = Long.parseLong(value.trim());
      return parsed >= min && parsed <= max ? parsed : defaultValue;
    } catch (RuntimeException e) {
      return defaultValue;
    }
  }

  private int readIntConfig(int productId, String key, int defaultValue, int min, int max) {
    try {
      String value = userConfigService.getConfigValue(productId, key);
      if (value == null || value.isBlank()) {
        return defaultValue;
      }
      int parsed = Integer.parseInt(value.trim());
      return parsed >= min && parsed <= max ? parsed : defaultValue;
    } catch (RuntimeException e) {
      return defaultValue;
    }
  }

  private double readDoubleConfig(int productId, String key, double defaultValue, double min,
      double max) {
    try {
      String value = userConfigService.getConfigValue(productId, key);
      if (value == null || value.isBlank()) {
        return defaultValue;
      }
      double parsed = Double.parseDouble(value.trim());
      return Double.isFinite(parsed) && parsed >= min && parsed <= max ? parsed : defaultValue;
    } catch (RuntimeException e) {
      return defaultValue;
    }
  }

  private long estimateText(Object value) {
    if (value == null) {
      return 0L;
    }
    String text = String.valueOf(value);
    if (text.isEmpty()) {
      return 0L;
    }
    long tokens = 0L;
    int asciiWordLength = 0;
    for (int offset = 0; offset < text.length();) {
      int codePoint = text.codePointAt(offset);
      offset += Character.charCount(codePoint);
      boolean asciiWord = codePoint <= 0x7F
          && (Character.isLetterOrDigit(codePoint) || codePoint == '_');
      if (asciiWord) {
        asciiWordLength++;
        continue;
      }
      if (asciiWordLength > 0) {
        tokens += (asciiWordLength + 3L) / 4L;
        asciiWordLength = 0;
      }
      if (!Character.isWhitespace(codePoint)) {
        tokens++;
      }
    }
    if (asciiWordLength > 0) {
      tokens += (asciiWordLength + 3L) / 4L;
    }
    return tokens;
  }
}
