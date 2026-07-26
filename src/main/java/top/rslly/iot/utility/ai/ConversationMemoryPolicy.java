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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Keeps the conversation window bounded without a model-specific tokenizer. */
@Component
public class ConversationMemoryPolicy {
  private static final int MESSAGE_OVERHEAD_TOKENS = 4;

  private final long contextWindowTokens;
  private final double summaryThresholdRatio;
  private final int recentTurns;

  public ConversationMemoryPolicy(
      @Value("${ai.memory.context-window-tokens:500}") long contextWindowTokens,
      @Value("${ai.memory.summary-threshold-ratio:0.5}") double summaryThresholdRatio,
      @Value("${ai.memory.recent-turns:4}") int recentTurns) {
    this.contextWindowTokens = Math.max(1L, contextWindowTokens);
    this.summaryThresholdRatio = Math.min(1D, Math.max(0D, summaryThresholdRatio));
    this.recentTurns = Math.max(1, recentTurns);
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
        && messages.size() > retainedMessageCount()
        && estimateTokens(messages) > summaryThresholdTokens();
  }

  public List<ModelMessage> compactionPrefix(List<ModelMessage> messages) {
    if (!shouldSummarize(messages)) {
      return List.of();
    }
    int prefixSize = messages.size() - retainedMessageCount();
    return Collections.unmodifiableList(new ArrayList<>(messages.subList(0, prefixSize)));
  }

  public void retainRecentTurns(List<ModelMessage> messages) {
    if (messages == null || messages.size() <= retainedMessageCount()) {
      return;
    }
    messages.subList(0, messages.size() - retainedMessageCount()).clear();
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
    int fromIndex = Math.max(0, messages.size() - retainedMessageCount());
    return copyMessages(messages.subList(fromIndex, messages.size()));
  }

  public int retainedMessageCount() {
    return recentTurns * 2;
  }

  public long summaryThresholdTokens() {
    return (long) Math.floor(contextWindowTokens * summaryThresholdRatio);
  }

  public long contextWindowTokens() {
    return contextWindowTokens;
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
