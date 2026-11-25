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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.rslly.iot.dao.AgentMemoryRepository;
import top.rslly.iot.dao.OtaXiaozhiRepository;
import top.rslly.iot.dao.ProductRepository;
import top.rslly.iot.dao.RodakDeviceRepository;
import top.rslly.iot.models.AgentMemoryEntity;
import top.rslly.iot.models.OtaXiaozhiEntity;
import top.rslly.iot.models.ProductEntity;
import top.rslly.iot.models.RodakDeviceEntity;
import top.rslly.iot.param.response.AgentMemoryResponse;
import top.rslly.iot.services.rodak.RodakConversation;
import top.rslly.iot.utility.RedisUtil;
import top.rslly.iot.utility.ai.GlobalMessageContext;
import top.rslly.iot.utility.result.JsonResult;

@ExtendWith(MockitoExtension.class)
class AgentMemoryServiceImplTest {
  private static final String UUID = "550e8400-e29b-41d4-a716-446655440000";

  @Mock
  private AgentMemoryRepository agentMemoryRepository;
  @Mock
  private OtaXiaozhiRepository otaXiaozhiRepository;
  @Mock
  private RodakDeviceRepository rodakDeviceRepository;
  @Mock
  private ProductRepository productRepository;
  @Mock
  private HistoryMessageEntityService historyMessageEntityService;
  @Mock
  private RedisUtil redisUtil;
  @InjectMocks
  private AgentMemoryServiceImpl agentMemoryService;

  @Test
  void validatesDefaultDebugAndProductDeviceChatIds() {
    OtaXiaozhiEntity device = device("76:56:26:c6:66:65", "客厅音箱");
    when(otaXiaozhiRepository.findAllByProductIdAndDeviceId(
        1, device.getDeviceId())).thenReturn(List.of(device));

    assertTrue(agentMemoryService.isChatIdValidForProduct(1, "chatProduct1"));
    assertTrue(agentMemoryService.isChatIdValidForProduct(
        1, "chatProduct1debug" + UUID));
    assertTrue(agentMemoryService.isChatIdValidForProduct(
        1, "chatProduct1" + device.getDeviceId()));

    assertFalse(agentMemoryService.isChatIdValidForProduct(
        1, "chatProduct1" + UUID));
    assertFalse(agentMemoryService.isChatIdValidForProduct(
        1, "chatProduct1debug-not-a-uuid"));
    assertFalse(agentMemoryService.isChatIdValidForProduct(
        1, "chatProduct10debug" + UUID));
  }

  @Test
  void returnsOnlyExactProductConversationsWithDeviceMetadata() {
    String defaultChatId = "chatProduct1";
    String debugChatId = defaultChatId + "debug" + UUID;
    String deviceChatId = defaultChatId + "76:56:26:c6:66:65";
    OtaXiaozhiEntity device = device("76:56:26:c6:66:65", "客厅音箱");
    when(productRepository.findAllById(1)).thenReturn(List.of(mock(ProductEntity.class)));
    when(otaXiaozhiRepository.findAllByProductId(1)).thenReturn(List.of(device));
    when(agentMemoryRepository.findAllByChatIdStartingWith(defaultChatId)).thenReturn(List.of(
        memory(1, defaultChatId, "默认记忆。后续内容"),
        memory(2, debugChatId, "调试记忆。后续内容"),
        memory(3, "chatProduct10debug" + UUID, "其他产品"),
        memory(4, deviceChatId, "设备记忆。后续内容")));
    when(agentMemoryRepository.findAllByChatIdIn(anyList())).thenReturn(
        List.of(memory(4, deviceChatId, "设备记忆。后续内容")));

    JsonResult<?> result = agentMemoryService.getMemoryByNickName(1, null);
    List<AgentMemoryResponse> responses = ((List<?>) result.getData()).stream()
        .map(AgentMemoryResponse.class::cast)
        .toList();

    assertEquals(200, result.getErrorCode());
    assertEquals(3, responses.size());
    assertFalse(responses.stream()
        .anyMatch(response -> response.getChatId().startsWith("chatProduct10")));
    AgentMemoryResponse defaultResponse = responses.stream()
        .filter(response -> response.getChatId().equals(defaultChatId))
        .findFirst()
        .orElseThrow();
    assertNull(defaultResponse.getDeviceName());
    assertNull(defaultResponse.getNickName());
    AgentMemoryResponse deviceResponse = responses.stream()
        .filter(response -> response.getChatId().equals(deviceChatId))
        .findFirst()
        .orElseThrow();
    assertEquals("76:56:26:c6:66:65", deviceResponse.getDeviceName());
    assertEquals("客厅音箱", deviceResponse.getNickName());
  }

  @Test
  void exposesBoundRodakConversationsWithAndWithoutSavedMemory() {
    RodakDeviceEntity used = rodakDevice("76:56:26:c6:66:65");
    RodakDeviceEntity newDevice = rodakDevice("76:56:26:c6:66:66");
    String usedChatId = RodakConversation.chatId(used);
    String newChatId = RodakConversation.chatId(newDevice);
    when(productRepository.findAllById(1)).thenReturn(List.of(mock(ProductEntity.class)));
    when(rodakDeviceRepository.findAllByProductIdAndStatus(1, "ACTIVE"))
        .thenReturn(List.of(used, newDevice));
    when(agentMemoryRepository.findAllByChatIdIn(List.of(usedChatId, newChatId)))
        .thenReturn(List.of(memory(9, usedChatId, "设备历史")));

    JsonResult<?> result = agentMemoryService.getMemoryByNickName(1, null);

    assertEquals(200, result.getErrorCode());
    List<?> rows = (List<?>) result.getData();
    assertEquals(2, rows.size());
    var saved = (AgentMemoryResponse) rows.get(0);
    assertEquals(usedChatId, saved.getChatId());
    assertEquals(9, saved.getId());
    assertEquals("设备历史", saved.getContent());
    assertEquals(used.getDeviceKey(), saved.getDeviceName());
    var empty = (AgentMemoryResponse) rows.get(1);
    assertEquals(newChatId, empty.getChatId());
    assertEquals("Rodak", empty.getNickName());
    assertEquals(0, empty.getId());
  }

  @Test
  void validatesOnlyTheCurrentRodakBindingForTheProduct() {
    RodakDeviceEntity rodak = rodakDevice("76:56:26:c6:66:65");
    when(rodakDeviceRepository.findByDeviceUuid(rodak.getDeviceUuid()))
        .thenReturn(java.util.Optional.of(rodak));

    assertTrue(agentMemoryService.isChatIdValidForProduct(1, RodakConversation.chatId(rodak)));
    assertFalse(agentMemoryService.isChatIdValidForProduct(10, RodakConversation.chatId(rodak)));
    assertFalse(agentMemoryService.isChatIdValidForProduct(1,
        RodakConversation.chatId(1, rodak.getDeviceUuid(), rodak.getTokenVersion() - 1)));
    rodak.setStatus("UNBOUND");
    assertFalse(agentMemoryService.isChatIdValidForProduct(1, RodakConversation.chatId(rodak)));
  }

  @Test
  void deletingMemoryDeletesExactConversationDataAndInvalidatesSummaries() {
    String chatId = "chatProduct1debug" + UUID;
    AgentMemoryEntity memory = memory(7, chatId, "summary");
    when(agentMemoryRepository.findAllById(7)).thenReturn(List.of(memory));
    when(agentMemoryRepository.deleteAllById(7)).thenReturn(List.of(memory));
    when(redisUtil.incr(GlobalMessageContext.memoryRevisionKey(chatId), 1)).thenReturn(1L);

    JsonResult<?> result = agentMemoryService.deleteMemory(7);

    assertEquals(200, result.getErrorCode());
    verify(redisUtil).incr(GlobalMessageContext.memoryRevisionKey(chatId), 1);
    verify(redisUtil).expire(GlobalMessageContext.memoryRevisionKey(chatId), 48 * 3600);
    verify(redisUtil).del(
        "memory" + chatId, GlobalMessageContext.memoryBootstrapKey(chatId));
    verify(historyMessageEntityService).deleteAllByChatId(chatId);
    verify(agentMemoryRepository).deleteAllById(7);
  }

  private AgentMemoryEntity memory(int id, String chatId, String content) {
    AgentMemoryEntity memory = new AgentMemoryEntity();
    memory.setId(id);
    memory.setChatId(chatId);
    memory.setContent(content);
    return memory;
  }

  private OtaXiaozhiEntity device(String deviceId, String nickName) {
    OtaXiaozhiEntity device = new OtaXiaozhiEntity();
    device.setProductId(1);
    device.setDeviceId(deviceId);
    device.setNickName(nickName);
    return device;
  }

  private RodakDeviceEntity rodakDevice(String deviceKey) {
    var device = new RodakDeviceEntity();
    device.setProductId(1);
    device.setDeviceKey(deviceKey);
    device.setStatus("ACTIVE");
    device.setTokenVersion(3);
    return device;
  }
}
