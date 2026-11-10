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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.rslly.iot.utility.ai.ConversationMemoryPolicy;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.llm.FunctionToolSpec;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Entry point used by LLM clients to report usage. Static because the clients are created by
 * {@code LLMFactory} outside of Spring; {@code LlmUsageServiceImpl} configures it at startup. Never
 * throws into the model call path.
 */
public final class LlmUsageTracker {
  /** Dedicated logger so usage lines can be routed or silenced independently. */
  private static final Logger USAGE_LOG = LoggerFactory.getLogger("llm.usage");
  /**
   * DEBUG only: per-message fingerprints to locate where two requests stop sharing a prefix. Enable
   * with {@code logging.level.llm.usage.prefix=DEBUG}.
   */
  private static final Logger PREFIX_LOG = LoggerFactory.getLogger("llm.usage.prefix");
  private static final Logger log = LoggerFactory.getLogger(LlmUsageTracker.class);
  private static final StackWalker STACK_WALKER = StackWalker.getInstance();
  private static final String APP_PACKAGE = "top.rslly.iot.";
  private static final String LLM_PACKAGE = "top.rslly.iot.utility.ai.llm.";

  private static volatile boolean enabled = true;
  private static volatile boolean streamIncludeUsage = true;
  private static volatile double cacheHitPriceRatio = 0.2D;
  private static volatile double cacheCreationPriceRatio = 1.25D;
  private static volatile long minCacheableTokens = 1024L;
  private static volatile Consumer<LlmCallRecord> sink;

  private LlmUsageTracker() {}

  public static void configure(boolean enabled, boolean streamIncludeUsage,
      double cacheHitPriceRatio, double cacheCreationPriceRatio, long minCacheableTokens) {
    LlmUsageTracker.enabled = enabled;
    LlmUsageTracker.streamIncludeUsage = streamIncludeUsage;
    LlmUsageTracker.cacheHitPriceRatio = Math.max(0D, cacheHitPriceRatio);
    LlmUsageTracker.cacheCreationPriceRatio = Math.max(0D, cacheCreationPriceRatio);
    LlmUsageTracker.minCacheableTokens = Math.max(0L, minCacheableTokens);
  }

  public static void setSink(Consumer<LlmCallRecord> recordSink) {
    sink = recordSink;
  }

  public static boolean streamIncludeUsage() {
    return enabled && streamIncludeUsage;
  }

  public static double cacheHitPriceRatio() {
    return cacheHitPriceRatio;
  }

  public static double cacheCreationPriceRatio() {
    return cacheCreationPriceRatio;
  }

  public static long minCacheableTokens() {
    return minCacheableTokens;
  }

  /**
   * Captures attribution for a call. Must run on the caller's thread (before handing streaming work
   * to an executor) so the scope and the scene can be resolved.
   */
  public static Call begin(String provider, String model, List<ModelMessage> messages,
      boolean stream) {
    return begin(provider, model, messages, null, stream);
  }

  /**
   * @param tools function definitions sent with the request, so the estimate covers the whole
   *        prompt like {@code AgentTaskContext} does
   */
  public static Call begin(String provider, String model, List<ModelMessage> messages,
      List<FunctionToolSpec> tools, boolean stream) {
    if (!enabled) {
      return Call.NOOP;
    }
    try {
      LlmUsageContext.Frame frame = LlmUsageContext.current();
      long estimate = messages == null ? 0L
          : ConversationMemoryPolicy.estimateMessageTokens(messages);
      if (tools != null && !tools.isEmpty()) {
        estimate += ConversationMemoryPolicy.estimateText(FunctionToolSpec.toEstimateJson(tools));
      }
      String scene = detectScene();
      if (PREFIX_LOG.isDebugEnabled()) {
        PREFIX_LOG.debug("scene={} {}", scene, prefixFingerprint(messages, tools));
      }
      return new Call(provider, model, scene, frame, estimate, stream);
    } catch (RuntimeException e) {
      log.debug("llm usage begin failed", e);
      return Call.NOOP;
    }
  }

  static void record(LlmCallRecord record, LlmUsageContext.Frame frame) {
    if (frame != null) {
      for (LlmUsageAccumulator accumulator : frame.accumulators()) {
        accumulator.add(record);
      }
    }
    logRecord(record);
    Consumer<LlmCallRecord> currentSink = sink;
    if (currentSink != null) {
      currentSink.accept(record);
    }
  }

  private static void logRecord(LlmCallRecord record) {
    if (!USAGE_LOG.isInfoEnabled()) {
      return;
    }
    LlmUsage usage = record.usage();
    if (usage == null) {
      USAGE_LOG.info("scene={} product={} chat={} model={} usage=missing stream={} latency={}ms",
          record.scene(), record.productId(), record.chatId(), record.model(), record.stream(),
          record.latencyMs());
      return;
    }
    USAGE_LOG.info(
        "scene={} product={} chat={} model={} prompt={} cached={} hit={} creation={} completion={} "
            + "reasoning={} estimate={} stream={} latency={}ms",
        record.scene(), record.productId(), record.chatId(), record.model(),
        usage.promptTokens(), usage.cachedTokens(),
        String.format(Locale.ROOT, "%.1f%%",
            LlmUsageAccumulator.ratio(usage.cachedTokens(), usage.promptTokens()) * 100D),
        usage.cacheCreationTokens(), usage.completionTokens(), usage.reasoningTokens(),
        record.estimatedPromptTokens(), record.stream(), record.latencyMs());
  }

  /**
   * {@code tools=len#hash [index:role:len#hash ...]}; equal entries mean an equal prefix so far.
   */
  static String prefixFingerprint(List<ModelMessage> messages, List<FunctionToolSpec> tools) {
    StringBuilder builder = new StringBuilder();
    String toolsJson = FunctionToolSpec.toEstimateJson(tools);
    builder.append("tools=").append(toolsJson.length()).append('#')
        .append(Integer.toHexString(toolsJson.hashCode())).append(" [");
    if (messages != null) {
      for (int index = 0; index < messages.size(); index++) {
        ModelMessage message = messages.get(index);
        String content = message == null ? "" : String.valueOf(message.getContent());
        builder.append(index == 0 ? "" : " ").append(index).append(':')
            .append(message == null ? null : message.getRole()).append(':')
            .append(content.length()).append('#').append(Integer.toHexString(content.hashCode()));
      }
    }
    return builder.append(']').toString();
  }

  /** First application frame outside the LLM client package, e.g. {@code ChatTool.run}. */
  static String detectScene() {
    return STACK_WALKER.walk(frames -> frames
        .filter(frame -> frame.getClassName().startsWith(APP_PACKAGE)
            && !frame.getClassName().startsWith(LLM_PACKAGE))
        .findFirst()
        .map(frame -> normalizeScene(frame.getClassName(), frame.getMethodName()))
        .orElse("unknown"));
  }

  /** Drops packages, inner/proxy class suffixes and lambda decorations. */
  static String normalizeScene(String className, String methodName) {
    String simpleName = className.substring(className.lastIndexOf('.') + 1);
    int innerIndex = simpleName.indexOf('$');
    if (innerIndex > 0) {
      simpleName = simpleName.substring(0, innerIndex);
    }
    String method = methodName == null ? "" : methodName;
    if (method.startsWith("lambda$")) {
      String[] parts = method.split("\\$");
      method = parts.length > 1 && !parts[1].isBlank() ? parts[1] : "lambda";
    }
    if (method.startsWith("<")) {
      method = "init";
    }
    return method.isEmpty() ? simpleName : simpleName + "." + method;
  }

  /** An in-flight call. {@link #finish} is idempotent so error paths may call it again. */
  public static final class Call {
    static final Call NOOP = new Call(null, null, null, null, 0L, false);

    private final String provider;
    private final String model;
    private final String scene;
    private final LlmUsageContext.Frame frame;
    private final long estimatedPromptTokens;
    private final boolean stream;
    private final long startNanos = System.nanoTime();
    private final AtomicBoolean finished = new AtomicBoolean();

    private Call(String provider, String model, String scene, LlmUsageContext.Frame frame,
        long estimatedPromptTokens, boolean stream) {
      this.provider = provider;
      this.model = model;
      this.scene = scene;
      this.frame = frame;
      this.estimatedPromptTokens = estimatedPromptTokens;
      this.stream = stream;
    }

    /** @param usage null if the provider did not report usage */
    public void finish(LlmUsage usage) {
      if (this == NOOP || !finished.compareAndSet(false, true)) {
        return;
      }
      try {
        long latencyMs = (System.nanoTime() - startNanos) / 1_000_000L;
        LlmCallRecord record = new LlmCallRecord(System.currentTimeMillis(), provider, model,
            scene, frame == null ? null : frame.productId(),
            frame == null ? null : frame.chatId(), usage, estimatedPromptTokens, latencyMs,
            stream);
        LlmUsageTracker.record(record, frame);
      } catch (RuntimeException e) {
        log.debug("llm usage record failed", e);
      }
    }
  }
}
