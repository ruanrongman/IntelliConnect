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
package top.rslly.iot.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import top.rslly.iot.dao.AdminConfigRepository;
import top.rslly.iot.dao.ProductRepository;
import top.rslly.iot.models.AdminConfigEntity;
import top.rslly.iot.param.request.AdminConfig;
import top.rslly.iot.utility.ai.mcp.McpProtocolDeal;
import top.rslly.iot.utility.result.ResultCode;

class AdminConfigServiceImplTest {
  private AdminConfigRepository repository;
  private AdminConfigServiceImpl service;

  @BeforeEach
  void setUp() {
    repository = mock(AdminConfigRepository.class);
    service = new AdminConfigServiceImpl();
    ReflectionTestUtils.setField(service, "adminConfigRepository", repository);
    ReflectionTestUtils.setField(service, "productRepository", mock(ProductRepository.class));
    ReflectionTestUtils.setField(service, "mcpProtocolDeal", mock(McpProtocolDeal.class));
    when(repository.findAllBySetKey(any())).thenReturn(List.of());
  }

  @Test
  void globalReviewConfigurationsAreAccepted() {
    AdminConfig enabled = config(AdminConfigServiceImpl.GLOBAL_ROLE_REVIEW_ENABLED, "true");
    AdminConfig requirements = config(AdminConfigServiceImpl.GLOBAL_ROLE_REVIEW_REQUIREMENTS,
        "必须保持教育性和友善");
    when(repository.save(any(AdminConfigEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    assertTrue(service.postAdminConfig(enabled).getSuccess());
    assertTrue(service.postAdminConfig(requirements).getSuccess());
  }

  @Test
  void globalReviewEnabledRequiresBoolean() {
    var result = service.postAdminConfig(
        config(AdminConfigServiceImpl.GLOBAL_ROLE_REVIEW_ENABLED, "yes"));

    assertEquals(ResultCode.PARAM_NOT_VALID.getCode(), result.getErrorCode());
  }

  @Test
  void globalReviewRequirementsMustBeNonBlankAndAtMost2000Characters() {
    var blank = service.postAdminConfig(
        config(AdminConfigServiceImpl.GLOBAL_ROLE_REVIEW_REQUIREMENTS, "  "));
    var overLimit = service.postAdminConfig(config(
        AdminConfigServiceImpl.GLOBAL_ROLE_REVIEW_REQUIREMENTS, "x".repeat(2001)));

    assertEquals(ResultCode.PARAM_NOT_VALID.getCode(), blank.getErrorCode());
    assertEquals(ResultCode.PARAM_NOT_VALID.getCode(), overLimit.getErrorCode());
  }

  private static AdminConfig config(String key, String value) {
    AdminConfig config = new AdminConfig();
    config.setSetKey(key);
    config.setSetValue(value);
    return config;
  }
}
