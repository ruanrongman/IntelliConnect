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
import top.rslly.iot.models.AgentMemoryEntity;
import top.rslly.iot.models.OtaXiaozhiEntity;
import top.rslly.iot.models.ProductEntity;
import top.rslly.iot.param.response.AgentMemoryResponse;
import top.rslly.iot.utility.result.JsonResult;

@ExtendWith(MockitoExtension.class)
class AgentMemoryServiceImplTest {
  private static final String UUID = "550e8400-e29b-41d4-a716-446655440000";

  @Mock
  private AgentMemoryRepository agentMemoryRepository;
  @Mock
  private OtaXiaozhiRepository otaXiaozhiRepository;
  @Mock
  private ProductRepository productRepository;
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
}
