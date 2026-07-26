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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.rslly.iot.param.request.AgentMemory;
import top.rslly.iot.services.agent.AgentMemoryServiceImpl;
import top.rslly.iot.utility.RedisUtil;
import top.rslly.iot.utility.ai.GlobalMessageContext;
import top.rslly.iot.utility.ai.LlmDiyUtility;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.ModelMessageRole;
import top.rslly.iot.utility.ai.llm.LLM;
import top.rslly.iot.utility.ai.prompts.MemoryToolPrompt;

@ExtendWith(MockitoExtension.class)
class MemoryToolTest {
  @Mock
  private MemoryToolPrompt memoryToolPrompt;
  @Mock
  private AgentMemoryServiceImpl agentMemoryService;
  @Mock
  private LlmDiyUtility llmDiyUtility;
  @Mock
  private RedisUtil redisUtil;
  @Mock
  private LLM llm;
  @InjectMocks
  private MemoryTool memoryTool;

  @BeforeEach
  void setUp() {
    memoryTool.setLlmName("memory-model");
    when(llmDiyUtility.getDiyLlm(1, "memory-model", "memory")).thenReturn(llm);
    when(agentMemoryService.findAllByChatId("chat-1")).thenReturn(List.of());
    when(memoryToolPrompt.getMemoryToolPrompt("")).thenReturn("system");
    when(llm.commonChat(anyString(), anyList(), eq(true))).thenReturn("summary");
  }

  @Test
  void skipsSummaryWriteWhenConversationWasDeleted() {
    Map<String, Object> context = context(0L);
    when(redisUtil.get(GlobalMessageContext.memoryRevisionKey("chat-1"))).thenReturn(1L);

    memoryTool.run(conversation(), context);

    verify(agentMemoryService, never()).insertAndUpdate(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void writesSummaryWhenRevisionStillMatches() {
    Map<String, Object> context = context(2L);
    when(redisUtil.get(GlobalMessageContext.memoryRevisionKey("chat-1"))).thenReturn(2L);

    memoryTool.run(conversation(), context);

    ArgumentCaptor<AgentMemory> memoryCaptor = ArgumentCaptor.forClass(AgentMemory.class);
    verify(agentMemoryService).insertAndUpdate(memoryCaptor.capture());
    assertEquals("chat-1", memoryCaptor.getValue().getChatId());
    assertEquals("summary", memoryCaptor.getValue().getContent());
  }

  private Map<String, Object> context(long revision) {
    Map<String, Object> context = new HashMap<>();
    context.put(GlobalMessageContext.PRODUCT_ID, 1);
    GlobalMessageContext.putChatIds(context, "stream-1", "chat-1");
    context.put(GlobalMessageContext.MEMORY_REVISION, revision);
    return context;
  }

  private List<ModelMessage> conversation() {
    return List.of(
        new ModelMessage(ModelMessageRole.USER.value(), "hello"),
        new ModelMessage(ModelMessageRole.ASSISTANT.value(), "hi"));
  }
}
