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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.llm.FunctionToolSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LlmUsageTrackingTest {
  private final List<LlmCallRecord> sunk = Collections.synchronizedList(new ArrayList<>());

  @BeforeEach
  void setUp() {
    LlmUsageTracker.configure(true, true, 0.2D, 1.25D, 1024L);
    LlmUsageTracker.setSink(sunk::add);
  }

  @AfterEach
  void tearDown() {
    LlmUsageTracker.setSink(null);
  }

  @Test
  void parsesDashScopeCacheFields() {
    CompletionUsage usage = CompletionUsage.builder()
        .promptTokens(3000)
        .completionTokens(120)
        .totalTokens(3120)
        .promptTokensDetails(CompletionUsage.PromptTokensDetails.builder()
            .cachedTokens(2048)
            .putAdditionalProperty("cache_creation_input_tokens", JsonValue.from(512))
            .build())
        .completionTokensDetails(CompletionUsage.CompletionTokensDetails.builder()
            .reasoningTokens(40)
            .build())
        .build();

    LlmUsage parsed = LlmUsage.fromOpenAi(usage);

    assertEquals(new LlmUsage(3000, 2048, 512, 120, 40), parsed);
    assertEquals(440, parsed.uncachedPromptTokens());
  }

  @Test
  void fallsBackToDeepSeekCacheHitField() {
    CompletionUsage usage = CompletionUsage.builder()
        .promptTokens(2000)
        .completionTokens(10)
        .totalTokens(2010)
        .putAdditionalProperty("prompt_cache_hit_tokens", JsonValue.from(1280))
        .build();

    LlmUsage parsed = LlmUsage.fromOpenAi(usage);

    assertEquals(1280, parsed.cachedTokens());
    assertEquals(0, parsed.cacheCreationTokens());
    assertNull(LlmUsage.fromOpenAi(null));
  }

  @Test
  void normalizesScenesFromStackFrames() {
    assertEquals("ChatTool.run",
        LlmUsageTracker.normalizeScene("top.rslly.iot.utility.ai.tools.ChatTool", "run"));
    assertEquals("Agent.streamStep",
        LlmUsageTracker.normalizeScene("top.rslly.iot.utility.ai.toolAgent.Agent$1",
            "lambda$streamStep$3"));
    assertEquals("MemoryTool.summarize", LlmUsageTracker.normalizeScene(
        "top.rslly.iot.utility.ai.tools.MemoryTool$$SpringCGLIB$$0", "summarize"));
  }

  @Test
  void skipsLlmClientPackageWhenDetectingScene() {
    // This test lives in the LLM package, which is skipped like the clients themselves.
    LlmUsageTracker.begin("host", "qwen-plus", List.of(), false)
        .finish(new LlmUsage(10, 0, 0, 1, 0));

    assertEquals("unknown", sunk.getFirst().scene());
  }

  @Test
  void attributesCallsToNestedScopesAndOtherThreads() throws Exception {
    LlmUsageAccumulator turn = new LlmUsageAccumulator();
    LlmUsageAccumulator task = new LlmUsageAccumulator();
    try (LlmUsageContext.Scope outer = LlmUsageContext.open(7, "chat-1", turn)) {
      LlmUsageTracker.begin("host", "m", List.of(), false)
          .finish(new LlmUsage(2000, 1500, 0, 50, 0));
      try (LlmUsageContext.Scope inner = LlmUsageContext.open(null, null, task)) {
        Thread worker = Thread.ofVirtual().start(LlmUsageContext.wrap(
            () -> LlmUsageTracker.begin("host", "m", List.of(), true)
                .finish(new LlmUsage(500, 0, 0, 20, 0))));
        worker.join();
      }
    }
    assertNull(LlmUsageContext.current());

    assertEquals(2, turn.summary().getCalls());
    assertEquals(1, task.summary().getCalls());
    LlmCallRecord threaded = sunk.get(1);
    assertEquals(7, threaded.productId());
    assertEquals("chat-1", threaded.chatId());
    assertTrue(threaded.stream());
  }

  @Test
  void finishIsIdempotentAndCountsMissingUsage() {
    LlmUsageAccumulator turn = new LlmUsageAccumulator();
    try (LlmUsageContext.Scope ignored = LlmUsageContext.open(1, "c", turn)) {
      LlmUsageTracker.Call call = LlmUsageTracker.begin("host", "m",
          List.of(new ModelMessage("user", "hello world")), true);
      call.finish(null);
      call.finish(new LlmUsage(1, 1, 0, 1, 0));
    }

    LlmUsageSummary summary = turn.summary();
    assertEquals(1, summary.getCalls());
    assertEquals(0, summary.getReportedCalls());
    assertEquals(1, sunk.size());
    assertTrue(sunk.getFirst().estimatedPromptTokens() > 0);
  }

  @Test
  void estimateIncludesToolDefinitions() {
    List<ModelMessage> messages = List.of(new ModelMessage("user", "hello world"));
    LlmUsageTracker.begin("host", "m", messages, false).finish(null);
    LlmUsageTracker.begin("host", "m", messages,
        List.of(new FunctionToolSpec("route_weather", "query weather for a city",
            FunctionToolSpec.defaultRouterArgsSchema())),
        false).finish(null);

    assertTrue(sunk.get(1).estimatedPromptTokens() > sunk.get(0).estimatedPromptTokens() + 20);
  }

  @Test
  void summarizesRatesThresholdsAndEffectiveTokens() {
    LlmUsageAccumulator accumulator = new LlmUsageAccumulator(100_000);
    accumulator.add(record("ChatTool.run", new LlmUsage(4000, 3000, 0, 100, 0), 3600));
    accumulator.add(record("ChatTool.run", new LlmUsage(6000, 4000, 1000, 200, 0), 0));
    accumulator.add(record("EmotionTool.run", new LlmUsage(500, 0, 0, 10, 0), 450));
    accumulator.add(record("ClassifierTool.run", null, 0));

    LlmUsageSummary summary = accumulator.summary();

    assertEquals(4, summary.getCalls());
    assertEquals(3, summary.getReportedCalls());
    assertEquals(10500, summary.getPromptTokens());
    assertEquals(7000, summary.getCachedTokens());
    assertEquals(2500, summary.getUncachedPromptTokens());
    assertEquals(0.6667, summary.getCacheHitRate(), 1e-4);
    // The 500-token call is below the 1024 minimum and excluded from the eligible rate.
    assertEquals(2, summary.getEligibleCalls());
    assertEquals(0.7, summary.getEligibleCacheHitRate(), 1e-4);
    assertEquals(6000, summary.getMaxPromptTokens());
    assertEquals(0.06, summary.getContextUsage(), 1e-4);
    // 2500 uncached + 7000 * 0.2 + 1000 * 1.25
    assertEquals(5150, summary.getEffectiveInputTokens(), 1e-6);
    // Estimates only count calls that had one: (3600 + 450) / (4000 + 500).
    assertEquals(0.9, summary.getEstimateRatio(), 1e-4);
    assertEquals("ChatTool.run", summary.getScenes().getFirst().getScene());
    assertEquals(3, summary.getScenes().size());
  }

  private static LlmCallRecord record(String scene, LlmUsage usage, long estimate) {
    return new LlmCallRecord(0L, "host", "qwen-plus", scene, 1, "chat", usage, estimate, 100L,
        false);
  }
}
