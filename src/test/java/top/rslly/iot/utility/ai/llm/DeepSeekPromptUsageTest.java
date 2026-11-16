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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.openai.client.OpenAIClient;
import com.openai.core.http.StreamResponse;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.completions.CompletionUsage;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.llm.usage.LlmUsageContext;

class DeepSeekPromptUsageTest {
  @Test
  void synchronousResponseFeedsNextTurnAndFailureResetsBaseline() {
    DeepSeek llm = new DeepSeek("test-key");
    OpenAIClient client = mock(OpenAIClient.class, RETURNS_DEEP_STUBS);
    ReflectionTestUtils.setField(llm, "client", client);
    ChatCompletion completion = mock(ChatCompletion.class);
    when(completion.usage()).thenReturn(Optional.of(usage()));
    when(client.chat().completions().create(any(ChatCompletionCreateParams.class)))
        .thenReturn(completion).thenThrow(new IllegalStateException("provider unavailable"));
    try (var scope = LlmUsageContext.open(1, "chat", null)) {
      llm.commonChat("question", PromptTokenBudgetTest.prompt(1), false);
      assertNextBudget(llm, 1, 2);
      llm.commonChat("question", PromptTokenBudgetTest.prompt(2), false);
      List<ModelMessage> third = PromptTokenBudgetTest.prompt(3);
      assertEquals(PromptTokenBudget.localEstimate(third, List.of()),
          llm.estimatePromptTokens(third, List.of()));
    }
    // A new model/provider client never inherits the old client's real input count.
    try (var scope = LlmUsageContext.open(1, "chat", null)) {
      List<ModelMessage> next = PromptTokenBudgetTest.prompt(2);
      assertEquals(PromptTokenBudget.localEstimate(next, List.of()),
          new DeepSeek("https://example.com", "other-model", "key")
              .estimatePromptTokens(next, List.of()));
    }
  }

  @Test
  void streamedUsageIsAvailableBeforeCompletionCallback() throws Exception {
    DeepSeek llm = new DeepSeek("test-key");
    OpenAIClient client = mock(OpenAIClient.class, RETURNS_DEEP_STUBS);
    ReflectionTestUtils.setField(llm, "client", client);
    StreamResponse<ChatCompletionChunk> response = mock(StreamResponse.class);
    ChatCompletionChunk chunk = mock(ChatCompletionChunk.class);
    when(chunk.usage()).thenReturn(Optional.of(usage()));
    when(response.stream()).thenReturn(Stream.of(chunk));
    when(client.chat().completions().createStreaming(any(ChatCompletionCreateParams.class)))
        .thenReturn(response);
    CountDownLatch completed = new CountDownLatch(1);
    try (var scope = LlmUsageContext.open(1, "chat", null)) {
      llm.streamFunctionChat("question", PromptTokenBudgetTest.prompt(1), List.of(), false,
          new FunctionStreamHandler() {
            @Override
            public void onDirectReplyComplete(String text) {
              completed.countDown();
            }
          });
      assertTrue(completed.await(5, TimeUnit.SECONDS));
      assertNextBudget(llm, 1, 2);
    }
  }

  private void assertNextBudget(DeepSeek llm, int previousTurn, int nextTurn) {
    long previousEstimate =
        PromptTokenBudget.localEstimate(PromptTokenBudgetTest.prompt(previousTurn), List.of());
    List<ModelMessage> next = PromptTokenBudgetTest.prompt(nextTurn);
    assertEquals(2000 + PromptTokenBudget.localEstimate(next, List.of()) - previousEstimate,
        llm.estimatePromptTokens(next, List.of()));
  }

  private CompletionUsage usage() {
    return CompletionUsage.builder().promptTokens(2000).completionTokens(10).totalTokens(2010)
        .build();
  }
}
