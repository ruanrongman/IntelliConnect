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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.rslly.iot.dao.ProductToolsBanRepository;
import top.rslly.iot.models.ProductToolsBanEntity;
import top.rslly.iot.param.request.ProductToolsBan;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class ProductToolsBanServiceImpl implements ProductToolsBanService {
  @Resource
  private ProductToolsBanRepository productToolsBanRepository;

  @Override
  public List<String> getProductToolsBanList(int productId) {
    List<String> result = new ArrayList<>();
    if (productToolsBanRepository.findAllByProductId(productId).isEmpty())
      return result;
    for (var s : productToolsBanRepository.findAllByProductId(productId)) {
      result.add(s.getToolsName());
    }
    return result;
  }

  @Override
  public JsonResult<?> getProductToolsBan(int productId) {
    List<String> result = new ArrayList<>();
    if (productToolsBanRepository.findAllByProductId(productId).isEmpty())
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    var productToolsBanEntityList = productToolsBanRepository.findAllByProductId(productId);
    for (var s : productToolsBanEntityList) {
      result.add(s.getToolsName());
    }
    return ResultTool.success(result);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> postProductToolsBan(ProductToolsBan productToolsBan) {
    // 允许值列表（不包含 "5"）
    Set<String> allowed = Set.of("1", "2", "3", "4", "6", "7", "8", "9", "10", "knowledge");

    for (var s : productToolsBan.getToolsName()) {
      if (!allowed.contains(s))
        return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    if (!productToolsBanRepository.findAllByProductId(productToolsBan.getProductId()).isEmpty()) {
      productToolsBanRepository.deleteAllByProductId(productToolsBan.getProductId());
    }
    List<ProductToolsBanEntity> productToolsBanEntityList = new ArrayList<>();
    for (var s : productToolsBan.getToolsName()) {
      ProductToolsBanEntity entity = new ProductToolsBanEntity();
      entity.setProductId(productToolsBan.getProductId());
      entity.setToolsName(s);
      productToolsBanEntityList.add(entity);
    }
    var result = productToolsBanRepository.saveAll(productToolsBanEntityList);
    return ResultTool.success(result);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> deleteProductToolsBan(int productId) {
    if (productToolsBanRepository.findAllByProductId(productId).isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    var result = productToolsBanRepository.deleteAllByProductId(productId);
    return ResultTool.success(result);
  }
}
