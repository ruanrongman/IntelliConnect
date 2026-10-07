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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.rslly.iot.dao.ProductRepository;
import top.rslly.iot.dao.ProductRoleRepository;
import top.rslly.iot.models.AdminConfigEntity;
import top.rslly.iot.models.ProductEntity;
import top.rslly.iot.models.ProductRoleEntity;
import top.rslly.iot.param.request.ProductRole;
import top.rslly.iot.services.AdminConfigServiceImpl;
import top.rslly.iot.services.UserConfigServiceImpl;
import top.rslly.iot.utility.ai.tools.GlobalRoleReviewTool;
import top.rslly.iot.utility.ai.tools.YouthProtectionTool;
import top.rslly.iot.utility.ai.voice.VoiceTimbre;
import top.rslly.iot.utility.result.ResultCode;

@ExtendWith(MockitoExtension.class)
class ProductRoleServiceImplTest {
  @Mock
  private ProductRepository productRepository;
  @Mock
  private ProductRoleRepository productRoleRepository;
  @Mock
  private GlobalRoleReviewTool globalRoleReviewTool;
  @Mock
  private YouthProtectionTool youthProtectionTool;
  @Mock
  private AdminConfigServiceImpl adminConfigService;
  @Mock
  private UserConfigServiceImpl userConfigService;
  @InjectMocks
  private ProductRoleServiceImpl productRoleService;

  @Test
  void rejectsCreateWhenGlobalReviewBlocks() {
    ProductRole request = request();
    AdminConfigEntity enabled = new AdminConfigEntity();
    enabled.setSetValue("true");
    when(adminConfigService.findAllBySetKey(AdminConfigServiceImpl.GLOBAL_ROLE_REVIEW_ENABLED))
        .thenReturn(List.of(enabled));
    when(globalRoleReviewTool.run(any(String.class))).thenReturn(false);
    when(productRepository.findAllById(1)).thenReturn(List.of(new ProductEntity()));
    when(productRoleRepository.findAllByProductId(1)).thenReturn(List.of());

    var response = productRoleService.postProductRole(request);

    assertEquals(ResultCode.ROLE_REVIEW_REJECTED.getCode(), response.getErrorCode());
    verify(productRoleRepository, never()).save(any(ProductRoleEntity.class));
  }

  @Test
  void skipsGlobalReviewWhenDisabled() {
    ProductRole request = request();
    ProductRoleEntity saved = new ProductRoleEntity();
    when(adminConfigService.findAllBySetKey(AdminConfigServiceImpl.GLOBAL_ROLE_REVIEW_ENABLED))
        .thenReturn(List.of());
    when(productRepository.findAllById(1)).thenReturn(List.of(new ProductEntity()));
    when(productRoleRepository.findAllByProductId(1)).thenReturn(List.of());
    when(productRoleRepository.save(any(ProductRoleEntity.class))).thenReturn(saved);

    var response = productRoleService.postProductRole(request);

    assertTrue(response.getSuccess());
    verify(globalRoleReviewTool, never()).run(any(String.class));
    verify(productRoleRepository).save(any(ProductRoleEntity.class));
  }

  private static ProductRole request() {
    ProductRole request = new ProductRole();
    request.setProductId(1);
    request.setAssistantName("助手");
    request.setUserName("用户");
    request.setRole("学习助手");
    request.setRoleIntroduction("提供安全、友善的学习帮助");
    request.setVoice(VoiceTimbre.values()[0].getTimbre());
    return request;
  }
}
