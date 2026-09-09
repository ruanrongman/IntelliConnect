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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.rslly.iot.dao.ProductToolsBanRepository;
import top.rslly.iot.models.ProductToolsBanEntity;
import top.rslly.iot.param.request.ProductToolsBan;
import top.rslly.iot.utility.result.ResultCode;

@ExtendWith(MockitoExtension.class)
class ProductToolsBanServiceImplTest {
  @Mock
  private ProductToolsBanRepository productToolsBanRepository;
  @InjectMocks
  private ProductToolsBanServiceImpl productToolsBanService;

  @Test
  void rejectsWebSearchBan() {
    ProductToolsBan request = new ProductToolsBan();
    request.setProductId(12);
    request.setToolsName(List.of("webSearch"));

    var response = productToolsBanService.postProductToolsBan(request);

    assertEquals(ResultCode.PARAM_NOT_VALID.getCode(), response.getErrorCode());
    verify(productToolsBanRepository, never()).saveAll(anyList());
  }

  @Test
  void rejectsWebSearchSingleToolBan() {
    var response = productToolsBanService.addProductToolBan("webSearch", 12);

    assertEquals(ResultCode.PARAM_NOT_VALID.getCode(), response.getErrorCode());
    verify(productToolsBanRepository, never()).save(any());
  }

  @Test
  void rejectsUnknownToolBan() {
    ProductToolsBan request = new ProductToolsBan();
    request.setProductId(12);
    request.setToolsName(List.of("unknownTool"));

    var response = productToolsBanService.postProductToolsBan(request);

    assertEquals(ResultCode.PARAM_NOT_VALID.getCode(), response.getErrorCode());
    verify(productToolsBanRepository, never()).saveAll(anyList());
  }

  @Test
  void returnsWebSearchBanFromProductList() {
    ProductToolsBanEntity entity = new ProductToolsBanEntity();
    entity.setProductId(12);
    entity.setToolsName("webSearch");
    when(productToolsBanRepository.findAllByProductId(12)).thenReturn(List.of(entity));

    assertEquals(List.of("webSearch"), productToolsBanService.getProductToolsBanList(12));
  }
}
