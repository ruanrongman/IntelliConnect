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
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import top.rslly.iot.dao.ProductDataRepository;
import top.rslly.iot.dao.ProductModelRepository;
import top.rslly.iot.models.ProductDataEntity;
import top.rslly.iot.models.ProductModelEntity;
import top.rslly.iot.param.prompt.ProductDataDescription;
import top.rslly.iot.param.request.ProductData;
import top.rslly.iot.utility.DataSave;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

@Service
public class ProductDataServiceImpl implements ProductDataService {

  @Resource
  private ProductDataRepository productDataRepository;
  @Resource
  private ProductModelRepository productModelRepository;

  @Override
  public List<ProductDataEntity> findAllByModelId(int modelId) {
    return productDataRepository.findAllByModelId(modelId);
  }

  @Override
  public List<ProductDataEntity> findAllByStorageType(String storageType) {
    return productDataRepository.findAllByStorageType(storageType);
  }

  @Override
  public List<ProductDataDescription> getDescription(int modelId) {
    var result = productDataRepository.findAllByModelId(modelId);
    List<ProductDataDescription> productDataDescriptionList = new LinkedList<>();
    if (!result.isEmpty()) {
      for (var s : result) {
        if (s.getrRw() == 1) {
          ProductDataDescription productDataDescription = new ProductDataDescription();
          productDataDescription.setValueName(s.getJsonKey());
          productDataDescription.setDescription(s.getDescription());
          productDataDescription.setType(s.getType());
          productDataDescriptionList.add(productDataDescription);
        }
      }
    }
    return productDataDescriptionList;
  }

  @Override
  public JsonResult<?> getProductData() {
    var result = productDataRepository.findAll();
    if (result.isEmpty()) {
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    } else
      return ResultTool.success(result);
  }

  @Override
  public JsonResult<?> postProductData(ProductData productData) {
    ProductDataEntity productDataEntity = new ProductDataEntity();
    BeanUtils.copyProperties(productData, productDataEntity);
    List<ProductModelEntity> result =
        productModelRepository.findAllById(productDataEntity.getModelId());
    List<ProductDataEntity> p1 = productDataRepository
        .findAllByModelIdAndJsonKey(productDataEntity.getModelId(), productDataEntity.getJsonKey());
    // List<ProductDataEntity> p2 = productDataRepository.findAllByType(productData.getType());
    HashMap<String, Integer> typeMap = new HashMap<>();
    typeMap.put("string", 1);
    typeMap.put("int", 2);
    typeMap.put("float", 3);
    HashMap<String, Integer> storageMap = new HashMap<>();
    storageMap.put(DataSave.never.getStorageType(), 1);
    storageMap.put(DataSave.week.getStorageType(), 2);
    storageMap.put(DataSave.permanent.getStorageType(), 3);
    var p2 = typeMap.get(productDataEntity.getType());
    var p3 = storageMap.get(productDataEntity.getStorageType());
    boolean isPermit = productDataEntity.getrRw() == 0 || productDataEntity.getrRw() == 1;
    if (result.isEmpty() || !p1.isEmpty() || p2 == null || p3 == null || !isPermit)
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    else {
      ProductDataEntity productDataEntity1 = productDataRepository.save(productDataEntity);
      return ResultTool.success(productDataEntity1);
    }
  }

  @Override
  public JsonResult<?> deleteProductData(int id) {
    List<ProductDataEntity> result = productDataRepository.deleteById(id);
    if (result.isEmpty())
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    else {
      return ResultTool.success(result);
    }
  }
}
