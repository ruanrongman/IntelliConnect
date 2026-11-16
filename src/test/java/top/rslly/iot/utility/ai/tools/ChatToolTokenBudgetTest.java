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
package top.rslly.iot.utility.ai.tools;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.completions.CompletionUsage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongConsumer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import top.rslly.iot.services.agent.AgentMemoryServiceImpl;
import top.rslly.iot.services.agent.ProductRoleServiceImpl;
import top.rslly.iot.services.knowledgeGraphic.KnowledgeGraphicServiceImpl;
import top.rslly.iot.utility.ai.ConversationMemoryPolicy;
import top.rslly.iot.utility.ai.DescriptionUtil;
import top.rslly.iot.utility.ai.GlobalMessageContext;
import top.rslly.iot.utility.ai.LlmDiyUtility;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.llm.DeepSeek;
import top.rslly.iot.utility.ai.llm.usage.LlmUsageContext;
import top.rslly.iot.utility.ai.prompts.ChatToolPrompt;

class ChatToolTokenBudgetTest {
  @Test
  void realUsageSurvivesRuntimeChangesAndCompactsTheActualOutgoingRequest() {
    ChatTool tool = new ChatTool();
    DeepSeek llm = new DeepSeek("test-key");
    OpenAIClient client = mock(OpenAIClient.class, RETURNS_DEEP_STUBS);
    ReflectionTestUtils.setField(llm, "client", client);
    ChatCompletion completion = mock(ChatCompletion.class);
    when(completion.usage()).thenReturn(java.util.Optional.of(CompletionUsage.builder()
        .promptTokens(2000).completionTokens(0).totalTokens(2000).build()));
    when(client.chat().completions().create(any(ChatCompletionCreateParams.class)))
        .thenReturn(completion);
    LlmDiyUtility diy = mock(LlmDiyUtility.class);
    when(diy.getDiyLlm(eq(1), nullable(String.class), eq("5"), eq(true))).thenReturn(llm);
    ChatToolPrompt prompt = mock(ChatToolPrompt.class);
    when(prompt.getChatTool(any(), any(), any(), any(), any(), any(), any(), any(), anyBoolean()))
        .thenReturn("runtime 1", "runtime changed 2", "runtime changed again 3");
    ReflectionTestUtils.setField(tool, "llmDiyUtility", diy);
    ReflectionTestUtils.setField(tool, "chatToolPrompt", prompt);
    ReflectionTestUtils.setField(tool, "agentMemoryService", mock(AgentMemoryServiceImpl.class));
    ReflectionTestUtils.setField(tool, "productRoleService", mock(ProductRoleServiceImpl.class));
    ReflectionTestUtils.setField(tool, "descriptionUtil", mock(DescriptionUtil.class));
    ReflectionTestUtils.setField(tool, "knowledgeGraphicService",
        mock(KnowledgeGraphicServiceImpl.class));

    List<ModelMessage> history = new ArrayList<>();
    List<Long> budgets = new ArrayList<>();
    ConversationMemoryPolicy policy = new ConversationMemoryPolicy(1000, 0.5, 1);
    Map<String, Object> context = new HashMap<>();
    GlobalMessageContext.putChatIds(context, "chat", "chat");
    context.put(GlobalMessageContext.PRODUCT_ID, 1);
    context.put(GlobalMessageContext.MEMORY, history);
    context.put(GlobalMessageContext.PROMPT_BUDGET, (LongConsumer) tokens -> {
      budgets.add(tokens);
      if (policy.shouldSummarize(history, tokens)) {
        policy.retainRecentTurns(history);
      }
    });
    try (var scope = LlmUsageContext.open(1, "chat", null)) {
      for (int turn = 1; turn <= 3; turn++) {
        tool.run("question " + turn, context);
        history.add(new ModelMessage("user", "question " + turn, "r" + turn));
        history.add(new ModelMessage("assistant", "answer " + turn, "r" + turn));
      }
    }
    assertTrue(budgets.get(0) < 100);
    assertTrue(budgets.get(1) > 1900);
    assertTrue(budgets.get(2) > 1900);
    var requests = ArgumentCaptor.forClass(ChatCompletionCreateParams.class);
    verify(client.chat().completions(), times(3)).create(requests.capture());
    // Third request contains system + retained turn 2 + question 3; turn 1 was removed.
    assertEquals(4, requests.getAllValues().getLast().messages().size());
    assertEquals("question 2", history.getFirst().getContent());
  }
}
