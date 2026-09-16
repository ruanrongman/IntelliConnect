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
package top.rslly.iot.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import top.rslly.iot.services.SafetyServiceImpl;
import top.rslly.iot.services.agent.AgentLongMemoryServiceImpl;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

@ExtendWith(MockitoExtension.class)
class ToolLongMemoryTest {
  @Mock
  private SafetyServiceImpl safetyService;
  @Mock
  private AgentLongMemoryServiceImpl memoryService;
  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    Tool controller = new Tool();
    ReflectionTestUtils.setField(controller, "safetyService", safetyService);
    ReflectionTestUtils.setField(controller, "agentLongMemoryService", memoryService);
    mvc = MockMvcBuilders.standaloneSetup(controller).build();
  }

  @Test
  void authorizedProductWithNoMemoryReturnsEmptyArray() throws Exception {
    when(safetyService.controlAuthorizeProduct("test-token", 12)).thenReturn(true);
    doReturn(ResultTool.success(List.of())).when(memoryService).getLongMemoryByProductId(12);

    mvc.perform(get("/api/v2/longMemoryByProductId").param("productId", "12")
        .header("Authorization", "test-token"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.errorCode").value(200))
        .andExpect(jsonPath("$.data").isEmpty());
  }

  @Test
  void deniesReadingAnotherProductsMemory() throws Exception {
    mvc.perform(get("/api/v2/longMemoryByProductId").param("productId", "13")
        .header("Authorization", "test-token"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.errorCode").value(3001));
    verify(safetyService).controlAuthorizeProduct("test-token", 13);
    verifyNoInteractions(memoryService);
  }

  @Test
  void rejectsMissingAuthorizationHeader() throws Exception {
    mvc.perform(get("/api/v2/longMemoryByProductId").param("productId", "12"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(memoryService);
  }

  @Test
  void deniesInitializingAnotherProductsMemory() throws Exception {
    mvc.perform(post("/api/v2/longMemoryFastInit").header("Authorization", "test-token")
        .contentType(MediaType.APPLICATION_JSON).content("{\"productId\":13,\"city\":\"北京市\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.errorCode").value(3001));
    verifyNoInteractions(memoryService);
  }

  @Test
  void preservesDuplicateResponseForTheButtonToDisableItself() throws Exception {
    when(safetyService.controlAuthorizeProduct("test-token", 12)).thenReturn(true);
    doReturn(ResultTool.fail(ResultCode.ENTITY_EXIST)).when(memoryService)
        .fastInitLongMemory(any());
    mvc.perform(post("/api/v2/longMemoryFastInit").header("Authorization", "test-token")
        .contentType(MediaType.APPLICATION_JSON).content("{\"productId\":12,\"city\":\"北京市\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.errorCode").value(3005));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   "})
  void rejectsBlankCity(String city) throws Exception {
    mvc.perform(post("/api/v2/longMemoryFastInit").header("Authorization", "test-token")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"productId\":12,\"city\":\"" + city + "\"}"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(memoryService);
  }

  @Test
  void rejectsCityThatExceedsTheStorageLimit() throws Exception {
    mvc.perform(post("/api/v2/longMemoryFastInit").header("Authorization", "test-token")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"productId\":12,\"city\":\"" + "a".repeat(256) + "\"}"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(memoryService);
  }

  @Test
  void keepsTheExistingFullListEndpoint() throws Exception {
    doReturn(ResultTool.success(List.of())).when(memoryService).getLongMemory("test-token");
    mvc.perform(get("/api/v2/longMemory").header("Authorization", "test-token"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.errorCode").value(200));
    verify(memoryService).getLongMemory("test-token");
  }
}
