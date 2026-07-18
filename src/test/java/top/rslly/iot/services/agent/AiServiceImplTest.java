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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.rslly.iot.models.ProductEntity;
import top.rslly.iot.param.request.AgentMemory;
import top.rslly.iot.param.request.AiControl;
import top.rslly.iot.services.SafetyServiceImpl;
import top.rslly.iot.services.thingsModel.ProductServiceImpl;
import top.rslly.iot.utility.ai.chain.Router;
import top.rslly.iot.utility.result.JsonResult;

@ExtendWith(MockitoExtension.class)
class AiServiceImplTest {
  private static final String CHAT_ID =
      "chatProduct1debug550e8400-e29b-41d4-a716-446655440000";

  @Mock
  private Router router;
  @Mock
  private ProductServiceImpl productService;
  @Mock
  private SafetyServiceImpl safetyService;
  @Mock
  private AgentMemoryServiceImpl agentMemoryService;
  @InjectMocks
  private AiServiceImpl aiService;

  @Test
  void routesDebugConversationAndInitializesItsMemory() {
    AiControl aiControl = new AiControl();
    aiControl.setProductId(1);
    aiControl.setChatId(CHAT_ID);
    aiControl.setContent("第一句话");

    when(safetyService.controlAuthorizeProduct("token", 1)).thenReturn(true);
    when(productService.findAllById(1)).thenReturn(List.of(new ProductEntity()));
    when(agentMemoryService.isChatIdValidForProduct(1, CHAT_ID)).thenReturn(true);
    when(agentMemoryService.findAllByChatId(CHAT_ID)).thenReturn(List.of());
    when(router.response("第一句话", CHAT_ID, 1)).thenReturn("完成");

    JsonResult<?> result = aiService.getAiResponse(aiControl, "token");

    assertEquals(200, result.getErrorCode());
    assertEquals("完成", result.getData());
    ArgumentCaptor<AgentMemory> memoryCaptor = ArgumentCaptor.forClass(AgentMemory.class);
    verify(agentMemoryService).insertAndUpdate(memoryCaptor.capture());
    assertEquals(CHAT_ID, memoryCaptor.getValue().getChatId());
    assertEquals("第一句话", memoryCaptor.getValue().getContent());
    verify(router).response("第一句话", CHAT_ID, 1);
  }
}
