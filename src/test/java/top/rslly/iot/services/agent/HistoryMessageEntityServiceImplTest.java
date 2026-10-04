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
package top.rslly.iot.services.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import top.rslly.iot.dao.HistoryMessageRepository;
import top.rslly.iot.dao.ProductRepository;
import top.rslly.iot.models.HistoryMessageEntity;
import top.rslly.iot.models.ProductEntity;
import top.rslly.iot.services.UserConfigServiceImpl;

@ExtendWith(MockitoExtension.class)
class HistoryMessageEntityServiceImplTest {
  @Mock
  private HistoryMessageRepository historyMessageRepository;
  @Mock
  private ProductRepository productRepository;
  @Mock
  private UserConfigServiceImpl userConfigService;
  @InjectMocks
  private HistoryMessageEntityServiceImpl historyMessageService;

  @Test
  void blocksWhenRecentUserTalksReachConfiguredLimit() {
    when(productRepository.findAllById(1)).thenReturn(List.of(new ProductEntity()));
    when(userConfigService.getConfigValue(1, "youth-protection.enabled")).thenReturn("true");
    when(userConfigService.getConfigValue(1, "youth-protection-talk.maxLimit"))
        .thenReturn("4");
    when(historyMessageRepository.countByChatIdAndMessageTypeAndTimeBetween(
        eq("chat-1"), eq("user"), any(Long.class), any(Long.class))).thenReturn(4L);

    assertTrue(historyMessageService.youthProtectForTimeOut(1, "chat-1"));
  }

  @Test
  void allowsWhenRecentUserTalksAreBelowConfiguredLimit() {
    when(productRepository.findAllById(1)).thenReturn(List.of(new ProductEntity()));
    when(userConfigService.getConfigValue(1, "youth-protection.enabled")).thenReturn("true");
    when(userConfigService.getConfigValue(1, "youth-protection-talk.maxLimit"))
        .thenReturn("4");
    when(historyMessageRepository.countByChatIdAndMessageTypeAndTimeBetween(
        eq("chat-1"), eq("user"), any(Long.class), any(Long.class))).thenReturn(3L);

    assertFalse(historyMessageService.youthProtectForTimeOut(1, "chat-1"));
  }

  @Test
  void returnsLimitedRecentHistoryInConversationOrder() {
    HistoryMessageEntity firstUser = history(1, "user", 100L);
    HistoryMessageEntity firstAssistant = history(2, "assistant", 101L);
    HistoryMessageEntity secondUser = history(1, "user", 200L);
    HistoryMessageEntity secondAssistant = history(2, "assistant", 201L);
    when(historyMessageRepository.findAllByChatId(eq("chat-1"), any(Pageable.class)))
        .thenReturn(new PageImpl<>(
            List.of(secondAssistant, secondUser, firstAssistant, firstUser)));

    List<HistoryMessageEntity> result =
        historyMessageService.findRecentByChatId("chat-1", 4);

    assertEquals(List.of(firstUser, firstAssistant, secondUser, secondAssistant), result);
    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(historyMessageRepository).findAllByChatId(eq("chat-1"), pageableCaptor.capture());
    Pageable pageable = pageableCaptor.getValue();
    assertEquals(4, pageable.getPageSize());
    assertEquals(Sort.Direction.DESC, pageable.getSort().getOrderFor("time").getDirection());
  }

  @Test
  void deletesHistoryByExactChatId() {
    HistoryMessageEntity history = history(1, "user", 100L);
    when(historyMessageRepository.deleteAllByChatId("chat-1")).thenReturn(List.of(history));

    List<HistoryMessageEntity> result = historyMessageService.deleteAllByChatId("chat-1");

    assertEquals(1, result.size());
    assertSame(history, result.getFirst());
    verify(historyMessageRepository).deleteAllByChatId("chat-1");
  }

  private HistoryMessageEntity history(int sequence, String type, long time) {
    HistoryMessageEntity history = new HistoryMessageEntity();
    history.setChatId("chat-1");
    history.setRequestId("request-" + time);
    history.setSequenceNum(sequence);
    history.setMessageType(type);
    history.setContent(type + "-" + time);
    history.setTime(time);
    return history;
  }
}
