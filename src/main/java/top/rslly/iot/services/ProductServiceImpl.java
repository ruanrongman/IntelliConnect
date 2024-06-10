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

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import top.rslly.iot.dao.MqttUserRepository;
import top.rslly.iot.dao.ProductRepository;
import top.rslly.iot.models.MqttUserEntity;
import top.rslly.iot.models.ProductEntity;
import top.rslly.iot.param.request.Product;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;


import javax.annotation.Resource;
import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {
  @Resource
  private ProductRepository productRepository;
  @Resource
  private MqttUserRepository mqttUserRepository;

  @Override
  public JsonResult<?> getProduct() {
    var result = productRepository.findAll();
    if (result.isEmpty()) {
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    } else
      return ResultTool.success(result);
  }

  @Override
  public JsonResult<?> postProduct(Product product) {
    ProductEntity productEntity = new ProductEntity();
    BeanUtils.copyProperties(product, productEntity);
    List<MqttUserEntity> result = mqttUserRepository.findALLById(productEntity.getMqttUser());
    List<ProductEntity> p1 = productRepository.findAllByProductName(productEntity.getProductName());
    if (result.isEmpty() || !p1.isEmpty())
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    else {
      ProductEntity productEntity1 = productRepository.save(productEntity);
      return ResultTool.success(productEntity1);
    }
  }

  @Override
  public JsonResult<?> deleteProduct(int id) {
    List<ProductEntity> result = productRepository.deleteById(id);
    if (result.isEmpty())
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    else {
      return ResultTool.success(result);
    }
  }

  @Override
  public List<ProductEntity> findAllByProductName(String productName) {
    return productRepository.findAllByProductName(productName);
  }

  @Override
  public List<ProductEntity> findAllById(int productId) {
    return productRepository.findAllById(productId);
  }
}
