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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.CriteriaBuilder;
import top.rslly.iot.utility.JwtTokenUtil;
import top.rslly.iot.dao.HistoryMessageRepository;
import top.rslly.iot.dao.ProductRepository;
import top.rslly.iot.dao.UserRepository;
import top.rslly.iot.dao.UserProductBindRepository;
import top.rslly.iot.models.HistoryMessageEntity;
import top.rslly.iot.models.ProductEntity;
import top.rslly.iot.models.UserEntity;
import top.rslly.iot.services.UserConfigServiceImpl;

@ExtendWith(MockitoExtension.class)
class HistoryMessageEntityServiceImplTest {
  @Mock
  private HistoryMessageRepository historyMessageRepository;
  @Mock
  private ProductRepository productRepository;
  @Mock
  private UserConfigServiceImpl userConfigService;
  @Mock
  private UserRepository userRepository;
  @Mock
  private UserProductBindRepository userProductBindRepository;
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
  void loadsWholeRequestsInsteadOfLimitingEventRows() {
    HistoryMessageEntity firstUser = history(1, "user", 100L);
    HistoryMessageEntity firstAssistant = history(2, "assistant", 101L);
    HistoryMessageEntity secondUser = history(1, "user", 200L);
    HistoryMessageEntity secondAssistant = history(2, "assistant", 201L);
    List<String> requestIds = List.of("second", "first");
    when(historyMessageRepository.findRecentRequestIds(eq("chat-1"), any(Pageable.class)))
        .thenReturn(requestIds);
    when(historyMessageRepository.findAllByChatIdAndRequestIdIn(eq("chat-1"), eq(requestIds),
        any(Sort.class)))
            .thenReturn(List.of(firstUser, firstAssistant, secondUser, secondAssistant));

    List<HistoryMessageEntity> result =
        historyMessageService.findRecentByChatId("chat-1", 4);

    assertEquals(List.of(firstUser, firstAssistant, secondUser, secondAssistant), result);
    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(historyMessageRepository).findRecentRequestIds(eq("chat-1"), pageableCaptor.capture());
    Pageable pageable = pageableCaptor.getValue();
    assertEquals(2, pageable.getPageSize());
    ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
    verify(historyMessageRepository).findAllByChatIdAndRequestIdIn(eq("chat-1"), eq(requestIds),
        sortCaptor.capture());
    assertEquals(Sort.Direction.ASC, sortCaptor.getValue().getOrderFor("time").getDirection());
    assertEquals(Sort.Direction.ASC,
        sortCaptor.getValue().getOrderFor("sequenceNum").getDirection());
  }

  @Test
  void storesLongProtocolEventsWithoutTruncatingTheirJson() {
    String content = "{\"arguments\":\"" + "完整参数😀".repeat(3000) + "\"}";

    historyMessageService.recordHistoryEvent("chat", "request", 2, "assistant_tool_call", content,
        100L);

    ArgumentCaptor<HistoryMessageEntity> event =
        ArgumentCaptor.forClass(HistoryMessageEntity.class);
    verify(historyMessageRepository).saveAndFlush(event.capture());
    assertEquals(content, event.getValue().getContent());
    assertEquals("request", event.getValue().getRequestId());
    assertEquals(2, event.getValue().getSequenceNum());
  }

  @Test
  void historyApiFiltersInternalEventsBeforePaginationAndSortsBySequence() {
    try (var jwt = mockStatic(JwtTokenUtil.class)) {
      jwt.when(() -> JwtTokenUtil.getUserRole("token")).thenReturn("[ROLE_admin]");
      jwt.when(() -> JwtTokenUtil.getUsername("token")).thenReturn("admin");
      when(historyMessageRepository.findAll(any(Specification.class), any(Pageable.class)))
          .thenReturn(new PageImpl<>(List.of(history(1, "user", 100L))));

      historyMessageService.getHistoryMessage("token", 1, 20, null, false);

      ArgumentCaptor<Specification<HistoryMessageEntity>> specification =
          ArgumentCaptor.forClass(Specification.class);
      ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
      verify(historyMessageRepository).findAll(specification.capture(), pageable.capture());
      Root<HistoryMessageEntity> root = mock(Root.class);
      Path<Object> messageType = mock(Path.class);
      when(root.get("messageType")).thenReturn(messageType);
      specification.getValue().toPredicate(root, null, mock(CriteriaBuilder.class));
      verify(messageType).in("user", "assistant");
      assertEquals(Sort.Direction.DESC,
          pageable.getValue().getSort().getOrderFor("time").getDirection());
      assertEquals(Sort.Direction.DESC,
          pageable.getValue().getSort().getOrderFor("sequenceNum").getDirection());
    }
  }

  @Test
  void managementHistoryIncludesToolEventsWithoutRemovingChatScope() {
    try (var jwt = mockStatic(JwtTokenUtil.class)) {
      jwt.when(() -> JwtTokenUtil.getUserRole("token")).thenReturn("[ROLE_admin]");
      jwt.when(() -> JwtTokenUtil.getUsername("token")).thenReturn("admin");
      HistoryMessageEntity call = history(2, "assistant_tool_call", 100L);
      HistoryMessageEntity result = history(3, "tool_result", 101L);
      when(historyMessageRepository.findAll(any(Specification.class), any(Pageable.class)))
          .thenReturn(new PageImpl<>(List.of(call, result)));

      var response = historyMessageService.getHistoryMessage("token", 1, 20, "chat-1", true);

      assertEquals(200, response.getErrorCode());
      assertEquals(List.of(call, result),
          ((org.springframework.data.domain.Page<?>) response.getData())
              .getContent());
      ArgumentCaptor<Specification<HistoryMessageEntity>> specification =
          ArgumentCaptor.forClass(Specification.class);
      verify(historyMessageRepository).findAll(specification.capture(), any(Pageable.class));
      Root<HistoryMessageEntity> root = mock(Root.class);
      Path<Object> chatId = mock(Path.class);
      when(root.get("chatId")).thenReturn(chatId);
      CriteriaBuilder cb = mock(CriteriaBuilder.class);
      specification.getValue().toPredicate(root, null, cb);
      verify(cb).equal(chatId, "chat-1");
      org.mockito.Mockito.verify(root, org.mockito.Mockito.never()).get("messageType");
    }
  }

  @Test
  void includingToolsDoesNotAllowUsersWithoutBoundProductsToReadHistory() {
    try (var jwt = mockStatic(JwtTokenUtil.class)) {
      jwt.when(() -> JwtTokenUtil.getUserRole("token")).thenReturn("ROLE_user");
      jwt.when(() -> JwtTokenUtil.getUsername("token")).thenReturn("user");
      var user = new UserEntity();
      user.setId(7);
      when(userRepository.findAllByUsername("user")).thenReturn(List.of(user));
      when(userProductBindRepository.findAllByUserId(7)).thenReturn(List.of());

      var response = historyMessageService.getHistoryMessage("token", 1, 20, "chat-1", true);

      assertTrue(response.getErrorCode() != 200);
      org.mockito.Mockito.verifyNoInteractions(historyMessageRepository);
    }
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
