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
package top.rslly.iot.services.thingsModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.rslly.iot.dao.AlarmEventRepository;
import top.rslly.iot.dao.ProductEventRepository;
import top.rslly.iot.dao.ProductModelRepository;
import top.rslly.iot.dao.UserProductBindRepository;
import top.rslly.iot.dao.UserRepository;
import top.rslly.iot.dao.WxProductBindRepository;
import top.rslly.iot.dao.WxUserRepository;
import top.rslly.iot.models.ProductEventEntity;
import top.rslly.iot.models.ProductModelEntity;
import top.rslly.iot.param.request.ProductEvent;
import top.rslly.iot.utility.result.ResultCode;

@ExtendWith(MockitoExtension.class)
class ProductEventServiceImplTest {
  @Mock
  private ProductModelRepository productModelRepository;
  @Mock
  private ProductEventRepository productEventRepository;
  @Mock
  private WxProductBindRepository wxProductBindRepository;
  @Mock
  private UserProductBindRepository userProductBindRepository;
  @Mock
  private WxUserRepository wxUserRepository;
  @Mock
  private UserRepository userRepository;
  @Mock
  private AlarmEventRepository alarmEventRepository;
  @InjectMocks
  private ProductEventServiceImpl productEventService;

  @Test
  void createsEventWhenRequestIdIsNull() {
    ProductEvent request = new ProductEvent();
    request.setName("灯泡不匹配");
    request.setDescription("关联事件 灯泡不匹配");
    request.setModelId(7);

    ProductModelEntity model = new ProductModelEntity();
    model.setId(7);
    when(productModelRepository.findAllById(7)).thenReturn(List.of(model));
    when(productEventRepository.findAllByModelIdAndName(7, request.getName()))
        .thenReturn(List.of());
    ProductEventEntity saved = new ProductEventEntity();
    saved.setName(request.getName());
    saved.setDescription(request.getDescription());
    saved.setModelId(request.getModelId());
    when(productEventRepository.save(any(ProductEventEntity.class))).thenReturn(saved);

    var response = productEventService.postProductEvent(request);

    assertTrue(response.getSuccess());
    assertEquals(ResultCode.SUCCESS.getCode(), response.getErrorCode());
    ArgumentCaptor<ProductEventEntity> captor = ArgumentCaptor.forClass(ProductEventEntity.class);
    verify(productEventRepository).save(captor.capture());
    ProductEventEntity entity = captor.getValue();
    assertEquals(0, entity.getId());
    assertEquals(request.getName(), entity.getName());
    assertEquals(request.getDescription(), entity.getDescription());
    assertEquals(request.getModelId(), entity.getModelId());
  }
}
