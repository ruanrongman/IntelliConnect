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
import java.util.List;

/**
 * Thread-bound attribution (product, chat, accumulators) for model calls.
 *
 * <p>
 * Open a scope at a request entry point; calls made on the same thread are attributed to it. Use
 * {@link #wrap(Runnable)} when handing work to another thread. The scene of each call is detected
 * from the call stack by {@link LlmUsageTracker}, so call sites do not need to be changed.
 */
public final class LlmUsageContext {
  private static final ThreadLocal<Frame> CURRENT = new ThreadLocal<>();

  private LlmUsageContext() {}

  public record Frame(Integer productId, String chatId, List<LlmUsageAccumulator> accumulators) {}

  /**
   * Opens a nested scope. Null arguments inherit the enclosing scope's values; a non-null
   * accumulator is added to the enclosing scope's accumulators so outer totals still see the call.
   */
  public static Scope open(Integer productId, String chatId, LlmUsageAccumulator accumulator) {
    Frame parent = CURRENT.get();
    List<LlmUsageAccumulator> accumulators = new ArrayList<>();
    if (parent != null) {
      accumulators.addAll(parent.accumulators());
    }
    if (accumulator != null && !accumulators.contains(accumulator)) {
      accumulators.add(accumulator);
    }
    Integer resolvedProductId =
        productId != null ? productId : parent == null ? null : parent.productId();
    String resolvedChatId = chatId != null ? chatId : parent == null ? null : parent.chatId();
    CURRENT.set(new Frame(resolvedProductId, resolvedChatId, List.copyOf(accumulators)));
    return new Scope(parent);
  }

  public static Frame current() {
    return CURRENT.get();
  }

  /** Propagates the caller's scope to a task that runs on another thread. */
  public static Runnable wrap(Runnable task) {
    Frame captured = CURRENT.get();
    if (captured == null || task == null) {
      return task;
    }
    return () -> {
      Frame previous = CURRENT.get();
      CURRENT.set(captured);
      try {
        task.run();
      } finally {
        restore(previous);
      }
    };
  }

  /** Sets the context window used for occupancy on accumulators that do not have one yet. */
  public static void setContextWindowTokens(long contextWindowTokens) {
    Frame frame = CURRENT.get();
    if (frame == null || contextWindowTokens <= 0) {
      return;
    }
    for (LlmUsageAccumulator accumulator : frame.accumulators()) {
      accumulator.setContextWindowTokensIfUnset(contextWindowTokens);
    }
  }

  private static void restore(Frame previous) {
    if (previous == null) {
      CURRENT.remove();
    } else {
      CURRENT.set(previous);
    }
  }

  public static final class Scope implements AutoCloseable {
    private final Frame previous;

    private Scope(Frame previous) {
      this.previous = previous;
    }

    @Override
    public void close() {
      restore(previous);
    }
  }
}
