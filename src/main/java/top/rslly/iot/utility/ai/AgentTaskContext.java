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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import top.rslly.iot.services.UserConfigServiceImpl;
import top.rslly.iot.utility.ai.llm.FunctionToolResultMessage;
import top.rslly.iot.utility.ai.llm.FunctionToolSpec;
import top.rslly.iot.utility.ai.llm.LLM;
import top.rslly.iot.utility.ai.prompts.AgentTaskContextPrompt;
import top.rslly.iot.utility.ai.tools.ToolPrefix;
import top.rslly.iot.utility.ai.llm.usage.LlmUsageContext;

/**
 * Request-local working context for one Agent invocation. Its summary and retained tool turns never
 * escape into Router memory, and compaction only edits the transcript, never tool effects.
 */
@Slf4j
public final class AgentTaskContext {
  private static final ConversationMemoryPolicy TOKENS =
      new ConversationMemoryPolicy(300_000, 0.75, 4);
  private static final String SUMMARY_LABEL = "Task progress summary (historical data):\n";
  private static final String TRUNCATED = "\n[内容过长，已截断中间部分]\n";

  public record Settings(long window, double ratio, int recentTurns, boolean unlimited) {
    public static Settings resolve(String scope, int productId,
        UserConfigServiceImpl configs, Environment environment) {
      long window = (long) number(scope + ".memory.context-window-tokens", 300_000, 1,
          1_000_000, true, productId, configs, environment);
      double ratio = number(scope + ".memory.summary-threshold-ratio", 0.75, 0.1,
          1, false, productId, configs, environment);
      int recent = (int) number(scope + ".memory.recent-turns", 4, 1, 100,
          true, productId, configs, environment);
      String fallback = environment == null ? "false"
          : environment.getProperty("ai." + scope + ".unlimited", "false");
      String configured = read(configs, productId, scope + ".unlimited");
      boolean unlimited = "true".equalsIgnoreCase(
          "true".equalsIgnoreCase(configured) || "false".equalsIgnoreCase(configured)
              ? configured : fallback);
      return new Settings(window, ratio, recent, unlimited);
    }

    private static double number(String key, double defaultValue, double min, double max,
        boolean integer, int productId, UserConfigServiceImpl configs, Environment environment) {
      double fallback = parse(environment == null ? null
          : environment.getProperty("ai." + key), defaultValue, min, max, integer);
      return parse(read(configs, productId, key), fallback, min, max, integer);
    }

    private static double parse(String value, double fallback, double min, double max,
        boolean integer) {
      try {
        double parsed = Double.parseDouble(value);
        return Double.isFinite(parsed) && parsed >= min && parsed <= max
            && (!integer || parsed == Math.rint(parsed)) ? parsed : fallback;
      } catch (RuntimeException e) {
        return fallback;
      }
    }

    private static String read(UserConfigServiceImpl configs, int productId, String key) {
      try {
        String value = configs == null ? null : configs.getConfigValue(productId, key);
        return value == null ? "" : value.trim();
      } catch (RuntimeException e) {
        log.warn("Unable to read task context setting, productId={}, key={}", productId, key);
        return "";
      }
    }
  }

  /** A complete tool interaction, kept intact to preserve call/result pairing. */
  private record Turn(List<ModelMessage> messages, String observation) {}

  private final Settings settings;
  private final String question;
  private final List<ModelMessage> history;
  // Only the recent working set stays in memory sent to the task model; older turns are summarized.
  private final List<Turn> turns = new ArrayList<>();
  private final Supplier<LLM> summaryModel;
  private final AgentTaskContextPrompt contextPrompt;
  private final BooleanSupplier cancelled;
  private final Consumer<String> events;
  private final long timeoutSeconds;
  // Always one rolling summary, updated with previous summary plus newly removed records.
  private String summary = "";
  // The initial conversation history is also eligible for compaction on the first budget check.
  private boolean needsCompaction;
  // Per-run context (time, memory) sent with the task instead of the system prompt, see
  // #taskMessage.
  private String runtimeContext = "";

  public AgentTaskContext(Settings settings, String question, List<ModelMessage> history,
      Supplier<LLM> summaryModel, BooleanSupplier cancelled, Consumer<String> events,
      long timeoutSeconds, AgentTaskContextPrompt contextPrompt) {
    this.settings = settings;
    this.question = question;
    this.history = new ArrayList<>(history);
    needsCompaction = !history.isEmpty();
    this.summaryModel = summaryModel;
    this.contextPrompt = contextPrompt;
    this.cancelled = cancelled;
    this.events = events;
    this.timeoutSeconds = Math.max(1, timeoutSeconds);
  }

  public boolean unlimited() {
    return settings.unlimited();
  }

  public void append(ModelMessage call, ModelMessage result, String observation) {
    // Keep calls and results together so compaction can never orphan a function-call ID.
    turns.add(new Turn(List.of(call, result), observation));
    needsCompaction = true;
  }

  /** Includes the summary and only the bounded working set, including at a finite epoch limit. */
  public List<String> observations() {
    List<String> result = new ArrayList<>();
    if (!summary.isBlank()) {
      result.add(SUMMARY_LABEL + summary);
    }
    for (Turn turn : turns) {
      if (turn.observation() != null && !turn.observation().isBlank()) {
        result.add(turn.observation());
      }
    }
    return result;
  }

  public List<ModelMessage> prepare(String system, List<FunctionToolSpec> tools) {
    checkCancelled();
    long schemaTokens =
        tools.isEmpty() ? 0 : TOKENS.estimateTextTokens(FunctionToolSpec.toEstimateJson(tools));
    // Fixed input cannot be compacted, so reject it before attempting any summary calls.
    long fixedTokens = count(List.of(new ModelMessage("system", system),
        new ModelMessage("user", taskMessage()))) + schemaTokens;
    if (fixedTokens >= settings.window()) {
      throw new IllegalArgumentException("Agent 固定提示、原始任务和工具定义超过上下文预算，请增大上下文窗口或减少输入。 ");
    }
    long threshold = Math.max(1, (long) Math.floor(settings.window() * settings.ratio()));
    List<ModelMessage> messages = compose(system);
    if (needsCompaction && count(messages) + schemaTokens >= threshold
        && (!history.isEmpty() || !turns.isEmpty())) {
      compact(system, schemaTokens, fixedTokens, threshold);
      messages = compose(system);
    }
    checkCancelled();
    return messages;
  }

  /**
   * Sets context that changes every run (time, long-term memory). It is placed before the task in
   * the user message so system prompt, tools and history remain a cacheable prefix across turns.
   */
  public void setRuntimeContext(String runtimeContext) {
    this.runtimeContext = runtimeContext == null ? "" : runtimeContext.strip();
  }

  private String taskMessage() {
    return runtimeContext.isEmpty() ? question : runtimeContext + "\n\n" + question;
  }

  private List<ModelMessage> compose(String system) {
    List<ModelMessage> messages = new ArrayList<>();
    messages.add(new ModelMessage("system", system));
    if (!summary.isBlank()) {
      messages.add(new ModelMessage("user", SUMMARY_LABEL + summary));
      messages.add(new ModelMessage("assistant", "I will continue from this progress summary."));
    }
    messages.addAll(history);
    messages.add(new ModelMessage("user", taskMessage()));
    for (Turn turn : turns) {
      messages.addAll(turn.messages());
    }
    return messages;
  }

  private void compact(String system, long schemaTokens, long fixedTokens, long threshold) {
    events.accept(ToolPrefix.COMPACT.getPrefix());
    int summaryLimit = (int) Math.max(1, Math.min(2048, threshold / 4));
    long target = fixedTokens + summaryLimit + 64 < threshold ? threshold : settings.window();
    // Move older records out of the working set; only this bounded window reaches the next call.
    List<ModelMessage> removed = new ArrayList<>(history);
    history.clear();
    while (turns.size() > settings.recentTurns()) {
      removed.addAll(turns.removeFirst().messages());
    }
    // Reserve room for the new summary before deciding which complete tool turns to keep.
    while (turns.size() > 1
        && count(compose(system)) + schemaTokens + summaryLimit + 64 >= target) {
      removed.addAll(turns.removeFirst().messages());
    }
    // One huge observation must also be summarized even if fewer than recentTurns exist.
    if (!turns.isEmpty()
        && count(compose(system)) + schemaTokens + summaryLimit + 64 >= target) {
      Turn last = turns.getLast();
      removed.addAll(last.messages());
      long available = target - fixedTokens - summaryLimit - 128;
      if (available > 32) {
        shortenLastResult((int) Math.min(Integer.MAX_VALUE, available));
      }
      if (available <= 32 || count(compose(system)) + schemaTokens + summaryLimit + 64 >= target) {
        turns.clear();
      }
    }
    if (!removed.isEmpty()) {
      try {
        summary = summarize(removed, summaryLimit);
      } catch (CancellationException e) {
        throw e;
      } catch (Exception e) {
        // A failed compactor must not erase the prior summary or abort useful recent context.
        log.warn("Task context summary failed; retaining previous summary and recent records", e);
        events.accept("%compact：摘要失败，已保留近期上下文。");
      }
    }
    // The provider can ignore a requested output size. Enforce the budget locally as well.
    summary = head(summary, summaryLimit);
    while (count(compose(system)) + schemaTokens >= target && !turns.isEmpty()) {
      if (turns.size() > 1) {
        turns.removeFirst();
      } else {
        long excess = count(compose(system)) + schemaTokens - target + 1;
        String observation = turns.getLast().observation();
        int budget = (int) Math.max(0, TOKENS.estimateTextTokens(observation) - excess - 16);
        if (observation == null || budget <= 0) {
          turns.clear();
        } else {
          shortenLastResult(budget);
          if (count(compose(system)) + schemaTokens >= target) {
            turns.clear();
          }
        }
      }
    }
    if (count(compose(system)) + schemaTokens >= target) {
      long remaining = target - fixedTokens - 64;
      summary = remaining > 0 ? head(summary, (int) remaining) : "";
    }
    checkCancelled();
    needsCompaction = false;
  }

  private void shortenLastResult(int budget) {
    Turn turn = turns.getLast();
    if (turn.observation() == null) {
      return;
    }
    String clipped = headTail(turn.observation(), budget);
    ModelMessage result = turn.messages().getLast();
    Object content = result.getContent()instanceof FunctionToolResultMessage nativeResult
        ? new FunctionToolResultMessage(nativeResult.toolCallId(), clipped)
        : "Observation: " + clipped;
    turns.set(turns.size() - 1, new Turn(List.of(turn.messages().getFirst(),
        new ModelMessage(result.getRole(), content)), clipped));
  }

  private String summarize(List<ModelMessage> removed, int limit) throws Exception {
    StringBuilder records = new StringBuilder();
    for (ModelMessage message : removed) {
      records.append(message.getRole()).append(": ")
          // Tool calls/results are records, which fastjson 1.x serializes as "{}".
          .append(message.getContent() instanceof String ? message.getContent()
              : ConversationHistoryCodec.encode(message.getContent()))
          .append('\n');
    }
    String nextSummary = summary;
    String instruction = contextPrompt.getSummaryPrompt(limit);
    int offset = 0;
    LLM model = summaryModel.get();
    // Chunk long histories so both the summarizer input and its requested output fit the window.
    while (offset < records.length()) {
      checkCancelled();
      String prefix = "Original task:\n" + question + "\nPrevious summary:\n" + nextSummary
          + "\nNew records:\n";
      long overhead = count(List.of(new ModelMessage("system", instruction),
          new ModelMessage("user", prefix)));
      int inputBudget = (int) Math.min(Integer.MAX_VALUE,
          settings.window() - overhead - limit - 16);
      if (inputBudget < 1) {
        throw new IllegalArgumentException("Insufficient context budget for task summary");
      }
      String chunk = head(records.substring(offset), inputBudget);
      if (chunk.isEmpty()) {
        throw new IllegalArgumentException("Insufficient context budget for a summary chunk");
      }
      String input = prefix + chunk;
      List<ModelMessage> messages = List.of(new ModelMessage("system", instruction),
          new ModelMessage("user", input));
      String answer = awaitSummary(() -> model.commonChat(input, messages, false));
      if (answer == null || answer.isBlank()) {
        throw new IllegalStateException("Empty task summary");
      }
      nextSummary = head(answer.strip(), limit);
      offset += chunk.length();
    }
    return nextSummary;
  }

  private String awaitSummary(Supplier<String> call) throws Exception {
    FutureTask<String> task = new FutureTask<>(call::get);
    Thread.ofVirtual().name("agent-context-summary").start(LlmUsageContext.wrap(task));
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
    try {
      while (true) {
        checkCancelled();
        long remaining = deadline - System.nanoTime();
        if (remaining <= 0) {
          throw new TimeoutException("Task summary timeout");
        }
        try {
          return task.get(Math.min(remaining, TimeUnit.MILLISECONDS.toNanos(100)),
              TimeUnit.NANOSECONDS);
        } catch (TimeoutException e) {
          // Check cancellation while waiting; a user stop must not start another tool call.
        }
      }
    } finally {
      task.cancel(true);
    }
  }

  private void checkCancelled() {
    if (Thread.currentThread().isInterrupted() || cancelled.getAsBoolean()) {
      throw new CancellationException("操作已取消");
    }
  }

  private static long count(List<ModelMessage> messages) {
    return TOKENS.estimateTokens(messages);
  }

  static String head(String text, int budget) {
    if (text == null || budget <= 0) {
      return "";
    }
    if (TOKENS.estimateTextTokens(text) <= budget) {
      return text;
    }
    int low = 0;
    int high = text.length();
    while (low < high) {
      int mid = low + (high - low + 1) / 2;
      if (TOKENS.estimateTextTokens(text.substring(0, mid)) <= budget) {
        low = mid;
      } else {
        high = mid - 1;
      }
    }
    if (low > 0 && Character.isHighSurrogate(text.charAt(low - 1))) {
      low--;
    }
    return text.substring(0, low);
  }

  private static String headTail(String text, int budget) {
    if (TOKENS.estimateTextTokens(text) <= budget) {
      return text;
    }
    int remaining = budget - (int) TOKENS.estimateTextTokens(TRUNCATED);
    if (remaining <= 0) {
      return head(text, budget);
    }
    // Reversing code points preserves Unicode boundaries when keeping the tail.
    String tail = new StringBuilder(head(new StringBuilder(text).reverse().toString(),
        remaining / 2)).reverse().toString();
    return head(text, remaining - remaining / 2) + TRUNCATED + tail;
  }
}
