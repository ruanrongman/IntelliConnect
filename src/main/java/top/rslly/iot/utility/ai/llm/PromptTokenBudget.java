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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import top.rslly.iot.utility.ai.ConversationHistoryCodec;
import top.rslly.iot.utility.ai.ConversationMemoryPolicy;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.llm.usage.LlmUsage;
import top.rslly.iot.utility.ai.llm.usage.LlmUsageContext;

/**
 * Last provider-reported prompt size plus a local estimate of the changed input. Owned by one LLM
 * client, so provider/model/credential changes cannot reuse another client's observations. Only
 * fingerprints and token counts are retained, never conversation text.
 */
final class PromptTokenBudget {
  private static final int MAX_BASELINES = 256;
  private static final long MAX_AGE_NANOS = Duration.ofHours(48).toNanos();
  private final AtomicLong sequence = new AtomicLong();
  private final LinkedHashMap<Key, Baseline> baselines = new LinkedHashMap<>(16, 0.75f, true);

  record Key(Integer productId, String chatId, String purpose, String tools) {}

  record Snapshot(Key key, List<String> history, long estimatedTokens, long sequence) {}

  private record Baseline(Snapshot input, long promptTokens, long timeNanos) {}

  long estimate(List<ModelMessage> messages, List<FunctionToolSpec> tools) {
    long fallback = localEstimate(messages, tools);
    Snapshot input = capture(messages, tools);
    if (input == null) {
      return fallback;
    }
    synchronized (baselines) {
      Baseline previous = baselines.get(input.key());
      if (previous == null || previous.promptTokens() <= 0
          || System.nanoTime() - previous.timeNanos() > MAX_AGE_NANOS
          || !extendsHistory(previous.input().history(), input.history())) {
        return fallback;
      }
      // Removed/replaced runtime context is estimated too; unchanged history cancels out.
      long calibrated = previous.promptTokens() + fallback - previous.input().estimatedTokens();
      // A large removed runtime block can make the estimated delta exceed the measured input.
      // Such a baseline cannot predict this request; never interpret it as zero input.
      return calibrated > 0L ? calibrated : fallback;
    }
  }

  Snapshot capture(List<ModelMessage> messages, List<FunctionToolSpec> tools) {
    LlmUsageContext.Frame frame = LlmUsageContext.current();
    // Conversation requests have [system, native history..., current user]. Auxiliary calls and
    // tool-loop requests with a different layout do not supply conversation baselines.
    if (frame == null || frame.productId() == null || frame.chatId() == null
        || messages == null || messages.size() < 2
        || messages.stream().anyMatch(Objects::isNull)
        || !"system".equals(messages.getFirst().getRole())
        || !"user".equals(messages.getLast().getRole())) {
      return null;
    }
    Key key = new Key(frame.productId(), frame.chatId(), purpose(),
        fingerprint(FunctionToolSpec.toEstimateJson(tools)));
    List<String> history = messages.subList(1, messages.size() - 1).stream()
        .map(PromptTokenBudget::stamp).toList();
    return new Snapshot(key, history, localEstimate(messages, tools), sequence.incrementAndGet());
  }

  void record(Snapshot input, LlmUsage usage) {
    if (input == null) {
      return;
    }
    synchronized (baselines) {
      Baseline previous = baselines.get(input.key());
      if (previous != null && previous.input().sequence() > input.sequence()) {
        return; // An older stream may finish after a newer request.
      }
      // A missing usage response invalidates this baseline until a fresh measurement arrives.
      baselines.put(input.key(), new Baseline(input,
          usage == null ? 0L : usage.promptTokens(), System.nanoTime()));
      while (baselines.size() > MAX_BASELINES) {
        baselines.remove(baselines.keySet().iterator().next());
      }
    }
  }

  static long localEstimate(List<ModelMessage> messages, List<FunctionToolSpec> tools) {
    return ConversationMemoryPolicy.estimateMessageTokens(messages)
        + ConversationMemoryPolicy.estimateText(FunctionToolSpec.toEstimateJson(tools));
  }

  private static boolean extendsHistory(List<String> previous, List<String> current) {
    // A reset, compaction, edited message or restored shorter window needs a new measurement.
    return current.size() > previous.size()
        && current.subList(0, previous.size()).equals(previous);
  }

  private static String stamp(ModelMessage message) {
    Object content = message.getContent();
    String text =
        content instanceof String value ? value : ConversationHistoryCodec.encode(content);
    return fingerprint(message.getRole() + "\0" + message.getRequestId() + "\0" + text);
  }

  private static String fingerprint(String text) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
          .digest(text.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private static String purpose() {
    // Estimation passes through GlobalMessageContext; recording passes through the LLM client.
    // Resolve the same application caller in both paths, isolating background extractors even
    // when they share the conversation's model. System/runtime changes are covered by the delta.
    return StackWalker.getInstance().walk(frames -> frames.map(StackWalker.StackFrame::getClassName)
        .filter(name -> name.startsWith("top.rslly.iot.")
            && !name.startsWith("top.rslly.iot.utility.ai.llm.")
            && !name.equals("top.rslly.iot.utility.ai.GlobalMessageContext"))
        .findFirst().orElse("unknown"));
  }
}
