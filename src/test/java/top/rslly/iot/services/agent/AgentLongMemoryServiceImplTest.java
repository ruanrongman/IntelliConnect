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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.rslly.iot.dao.AgentLongMemoryRepository;
import top.rslly.iot.dao.ProductRepository;
import top.rslly.iot.models.AgentLongMemoryEntity;
import top.rslly.iot.models.ProductEntity;
import top.rslly.iot.param.request.AgentLongMemoryFastInitParam;
import top.rslly.iot.utility.result.ResultCode;

@ExtendWith(MockitoExtension.class)
class AgentLongMemoryServiceImplTest {
  @Mock
  private AgentLongMemoryRepository memoryRepository;
  @Mock
  private ProductRepository productRepository;
  @InjectMocks
  private AgentLongMemoryServiceImpl service;

  @Test
  void returnsEmptySuccessForExistingProductWithoutMemory() {
    when(productRepository.findAllById(12)).thenReturn(List.of(new ProductEntity()));
    when(memoryRepository.findAllByProductId(12)).thenReturn(List.of());

    var result = service.getLongMemoryByProductId(12);

    assertEquals(200, result.getErrorCode());
    assertEquals(List.of(), result.getData());
  }

  @Test
  void returnsOnlyRequestedProductsMemory() {
    AgentLongMemoryEntity memory = new AgentLongMemoryEntity();
    memory.setProductId(12);
    memory.setMemoryKey("city");
    memory.setMemoryValue("北京市");
    when(productRepository.findAllById(12)).thenReturn(List.of(new ProductEntity()));
    when(memoryRepository.findAllByProductId(12)).thenReturn(List.of(memory));

    var result = service.getLongMemoryByProductId(12);

    assertEquals(200, result.getErrorCode());
    assertEquals(List.of(memory), result.getData());
    verify(memoryRepository, never()).findAll();
  }

  @Test
  void rejectsMissingProductInsteadOfReportingEmptyMemory() {
    var result = service.getLongMemoryByProductId(404);

    assertEquals(ResultCode.PARAM_NOT_VALID.getCode(), result.getErrorCode());
    verifyNoInteractions(memoryRepository);
  }

  @Test
  void initializationCreatesTwoDistinctMemoriesAndRejectsSecondAttempt() {
    ProductEntity product = new ProductEntity();
    product.setId(12);
    when(productRepository.findAllById(12)).thenReturn(List.of(product));
    when(productRepository.findLockedById(12)).thenReturn(List.of(product));
    List<AgentLongMemoryEntity> saved = new ArrayList<>();
    when(memoryRepository.findAllByProductId(12)).thenAnswer(invocation -> List.copyOf(saved));
    when(memoryRepository.save(any(AgentLongMemoryEntity.class))).thenAnswer(invocation -> {
      AgentLongMemoryEntity entity = invocation.getArgument(0);
      entity.setId(saved.size() + 1);
      saved.add(entity);
      return entity;
    });
    AgentLongMemoryFastInitParam request = new AgentLongMemoryFastInitParam();
    request.setProductId(12);
    request.setCity(" 北京市 ");

    assertEquals(200, service.fastInitLongMemory(request).getErrorCode());
    assertEquals(2, saved.size());
    assertNotSame(saved.get(0), saved.get(1));
    assertTrue(saved.stream().allMatch(entity -> entity.getProductId() == 12));
    assertEquals("city", saved.get(0).getMemoryKey());
    assertEquals("北京市", saved.get(0).getMemoryValue());
    assertEquals("用户画像和偏好", saved.get(1).getMemoryKey());
    assertEquals("暂无", saved.get(1).getMemoryValue());
    assertEquals(saved, service.getLongMemoryByProductId(12).getData());

    request.setCity("上海市");
    assertEquals(ResultCode.ENTITY_EXIST.getCode(),
        service.fastInitLongMemory(request).getErrorCode());
    assertEquals(2, saved.size());
    assertEquals("北京市", saved.get(0).getMemoryValue());
    verify(memoryRepository, times(2)).save(any(AgentLongMemoryEntity.class));
  }

  @Test
  void rejectsInitializationWhenProductIsMissing() {
    AgentLongMemoryFastInitParam request = new AgentLongMemoryFastInitParam();
    request.setProductId(404);
    request.setCity("北京市");

    assertEquals(ResultCode.PARAM_NOT_VALID.getCode(),
        service.fastInitLongMemory(request).getErrorCode());
    verifyNoInteractions(memoryRepository);
  }
}
