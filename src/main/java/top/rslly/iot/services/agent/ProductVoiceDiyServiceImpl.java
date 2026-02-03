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
import top.rslly.iot.dao.ProductRepository;
import top.rslly.iot.dao.ProductVoiceDiyRepository;
import top.rslly.iot.models.ProductVoiceDiyEntity;
import top.rslly.iot.param.request.ProductVoiceDiy;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import jakarta.annotation.Resource;
import java.util.List;

@Service
public class ProductVoiceDiyServiceImpl implements ProductVoiceDiyService {
  @Resource
  private ProductVoiceDiyRepository productVoiceDiyRepository;
  @Resource
  private ProductRepository productRepository;

  @Override
  public List<ProductVoiceDiyEntity> findAllById(int id) {
    return productVoiceDiyRepository.findAllById(id);
  }

  @Override
  public List<ProductVoiceDiyEntity> findAllByProductId(int productId) {
    return productVoiceDiyRepository.findAllByProductId(productId);
  }

  @Override
  public JsonResult<?> getProductVoiceDiy(int productId) {
    if (productVoiceDiyRepository.findAllByProductId(productId).isEmpty())
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    var productVoiceDiyEntityList = productVoiceDiyRepository.findAllByProductId(productId);
    if (productVoiceDiyEntityList.isEmpty()) {
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    } else {
      return ResultTool.success(productVoiceDiyEntityList);
    }
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> postProductVoiceDiy(ProductVoiceDiy productVoiceDiy) {
    if (productRepository.findAllById(productVoiceDiy.getProductId()).isEmpty())
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    ProductVoiceDiyEntity productVoiceDiyEntity = new ProductVoiceDiyEntity();
    var productVoiceDiyEntityList =
        productVoiceDiyRepository.findAllByProductId(productVoiceDiy.getProductId());
    try {
      var pitch = productVoiceDiy.getPitch();
      var speed = productVoiceDiy.getSpeed();
      if (pitch < 0.5 || pitch > 2.0 || speed < 0.5 || speed > 2.0)
        return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    } catch (Exception e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    if (!productVoiceDiyEntityList.isEmpty()) {
      productVoiceDiyEntity.setId(productVoiceDiyEntityList.get(0).getId());
    }
    productVoiceDiyEntity.setProductId(productVoiceDiy.getProductId());
    productVoiceDiyEntity.setPitch(productVoiceDiy.getPitch().toString());
    productVoiceDiyEntity.setSpeed(productVoiceDiy.getSpeed().toString());
    var res = productVoiceDiyRepository.save(productVoiceDiyEntity);
    return ResultTool.success(res);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> deleteProductVoiceDiy(int id) {
    if (productVoiceDiyRepository.findAllById(id).isEmpty())
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    else {
      var result = productVoiceDiyRepository.deleteAllById(id);
      return ResultTool.success(result);
    }
  }
}
