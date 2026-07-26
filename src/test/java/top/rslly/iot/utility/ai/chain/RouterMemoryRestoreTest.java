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
package top.rslly.iot.utility.ai.chain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import top.rslly.iot.models.HistoryMessageEntity;
import top.rslly.iot.services.agent.HistoryMessageEntityService;
import top.rslly.iot.utility.RedisUtil;
import top.rslly.iot.utility.ai.ConversationMemoryPolicy;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.ModelMessageRole;

class RouterMemoryRestoreTest {
  private RedisUtil redisUtil;
  private HistoryMessageEntityService historyMessageService;
  private Router router;

  @BeforeEach
  void setUp() {
    redisUtil = org.mockito.Mockito.mock(RedisUtil.class);
    historyMessageService = org.mockito.Mockito.mock(HistoryMessageEntityService.class);
    router = new Router();
    ReflectionTestUtils.setField(router, "redisUtil", redisUtil);
    ReflectionTestUtils.setField(router, "historyMessageEntityService", historyMessageService);
    ReflectionTestUtils.setField(
        router, "conversationMemoryPolicy", new ConversationMemoryPolicy(128000, 0.5, 4));
  }

  @Test
  void usesCachedMemoryWithoutQueryingHistory() {
    List<ModelMessage> cached = List.of(
        new ModelMessage(ModelMessageRole.USER.value(), "cached-user"),
        new ModelMessage(ModelMessageRole.ASSISTANT.value(), "cached-assistant"));
    when(redisUtil.get("memorychat-1")).thenReturn(cached);

    List<ModelMessage> result =
        ReflectionTestUtils.invokeMethod(router, "loadConversationMemory", "chat-1");

    assertEquals("cached-user", result.getFirst().getContent());
    assertEquals("cached-assistant", result.getLast().getContent());
    verifyNoInteractions(historyMessageService);
  }

  @Test
  void restoresRecentHistoryWhenCacheIsEmpty() {
    when(redisUtil.get("memorychat-1")).thenReturn(null);
    when(historyMessageService.findRecentByChatId("chat-1", 8)).thenReturn(List.of(
        history("user", "history-user", 100L),
        history("assistant", "history-assistant", 101L)));

    List<ModelMessage> result =
        ReflectionTestUtils.invokeMethod(router, "loadConversationMemory", "chat-1");

    assertEquals(2, result.size());
    assertEquals(ModelMessageRole.USER.value(), result.getFirst().getRole());
    assertEquals("history-user", result.getFirst().getContent());
    assertEquals(ModelMessageRole.ASSISTANT.value(), result.getLast().getRole());
    verify(historyMessageService).findRecentByChatId("chat-1", 8);
  }

  private HistoryMessageEntity history(String type, String content, long time) {
    HistoryMessageEntity history = new HistoryMessageEntity();
    history.setChatId("chat-1");
    history.setRequestId("request-" + time);
    history.setSequenceNum(ModelMessageRole.USER.value().equals(type) ? 1 : 2);
    history.setMessageType(type);
    history.setContent(content);
    history.setTime(time);
    return history;
  }
}
