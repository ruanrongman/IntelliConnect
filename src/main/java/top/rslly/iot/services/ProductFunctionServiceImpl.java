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
import org.springframework.transaction.annotation.Transactional;
import top.rslly.iot.dao.*;
import top.rslly.iot.models.ProductDataEntity;
import top.rslly.iot.models.ProductFunctionEntity;
import top.rslly.iot.models.ProductModelEntity;
import top.rslly.iot.param.prompt.ProductFunctionDescription;
import top.rslly.iot.param.prompt.ProductModelDescription;
import top.rslly.iot.param.request.ProductFunction;
import top.rslly.iot.utility.JwtTokenUtil;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

@Service
public class ProductFunctionServiceImpl implements ProductFunctionService {

  @Resource
  private ProductFunctionRepository productFunctionRepository;
  @Resource
  private ProductModelRepository productModelRepository;
  @Resource
  private WxProductBindRepository wxProductBindRepository;
  @Resource
  private UserProductBindRepository userProductBindRepository;
  @Resource
  private WxUserRepository wxUserRepository;
  @Resource
  private UserRepository userRepository;


  @Override
  public List<ProductFunctionEntity> findAllById(int id) {
    return productFunctionRepository.findAllById(id);
  }

  @Override
  public List<ProductFunctionEntity> findAllByModelIdAndDataType(int modelId, String dataType) {
    return productFunctionRepository.findAllByModelIdAndDataType(modelId, dataType);
  }

  @Override
  public List<ProductFunctionEntity> findAllByModelIdAndFunctionNameAndDataType(int modelId,
      String functionName, String dataType) {
    return productFunctionRepository.findAllByModelIdAndFunctionNameAndDataType(modelId,
        functionName, dataType);
  }

  @Override
  public List<ProductFunctionDescription> getDescription(int modelId) {
    var result = productFunctionRepository.findAllByModelIdAndDataType(modelId, "input");
    List<ProductFunctionDescription> productFunctionDescriptionList = new LinkedList<>();
    if (!result.isEmpty()) {
      for (var s : result) {
        ProductFunctionDescription productFunctionDescription = new ProductFunctionDescription();
        productFunctionDescription.setServiceName(s.getFunctionName());
        productFunctionDescription.setParamName(s.getJsonKey());
        productFunctionDescription.setDescription(s.getDescription());
        productFunctionDescription.setMax(s.getMax());
        productFunctionDescription.setMin(s.getMin());
        productFunctionDescription.setStep(s.getStep());
        productFunctionDescription.setUnit(s.getUnit());
        productFunctionDescription.setType(s.getType());

        productFunctionDescriptionList.add(productFunctionDescription);
      }
    }
    return productFunctionDescriptionList;
  }

  @Override
  public JsonResult<?> getProductFunction(String token) {
    String token_deal = token.replace(JwtTokenUtil.TOKEN_PREFIX, "");
    String role = JwtTokenUtil.getUserRole(token_deal);
    String username = JwtTokenUtil.getUsername(token_deal);
    List<ProductFunctionEntity> result;
    if (role.equals("ROLE_" + "wx_user")) {
      if (wxUserRepository.findAllByName(username).isEmpty()) {
        return ResultTool.fail(ResultCode.COMMON_FAIL);
      }
      String openid = wxUserRepository.findAllByName(username).get(0).getOpenid();
      result = new ArrayList<>();
      var wxBindProductResponseList = wxProductBindRepository.findProductIdByOpenid(openid);
      if (wxBindProductResponseList.isEmpty()) {
        return ResultTool.fail(ResultCode.COMMON_FAIL);
      }
      for (var s : wxBindProductResponseList) {
        List<ProductModelEntity> productModelEntities =
            productModelRepository.findAllByProductId(s.getProductId());
        for (var s1 : productModelEntities) {
          List<ProductFunctionEntity> productFunctionEntityList =
              productFunctionRepository.findAllByModelId(s1.getId());
          result.addAll(productFunctionEntityList);
        }
      }
    } else if (!role.equals("[ROLE_admin]")) {
      var userList = userRepository.findAllByUsername(username);
      if (userList.isEmpty()) {
        return ResultTool.fail(ResultCode.COMMON_FAIL);
      }
      int userId = userList.get(0).getId();
      result = new ArrayList<>();
      var userProductBindEntityList = userProductBindRepository.findAllByUserId(userId);
      if (userProductBindEntityList.isEmpty()) {
        return ResultTool.fail(ResultCode.COMMON_FAIL);
      }
      for (var s : userProductBindEntityList) {
        List<ProductModelEntity> productModelEntities =
            productModelRepository.findAllByProductId(s.getProductId());
        for (var s1 : productModelEntities) {
          List<ProductFunctionEntity> productFunctionEntityList =
              productFunctionRepository.findAllByModelId(s1.getId());
          result.addAll(productFunctionEntityList);
        }
      }
    } else {
      result = productFunctionRepository.findAll();
    }
    if (result.isEmpty()) {
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    } else
      return ResultTool.success(result);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> postProductFunction(ProductFunction productFunction) {
    ProductFunctionEntity productFunctionEntity = new ProductFunctionEntity();
    BeanUtils.copyProperties(productFunction, productFunctionEntity);
    List<ProductModelEntity> result =
        productModelRepository.findAllById(productFunctionEntity.getModelId());
    List<ProductFunctionEntity> p1 = productFunctionRepository
        .findAllByModelIdAndFunctionNameAndJsonKeyAndDataType(productFunctionEntity.getModelId(),
            productFunctionEntity.getFunctionName(),
            productFunctionEntity.getJsonKey(), productFunctionEntity.getDataType());
    // List<ProductDataEntity> p2 = productDataRepository.findAllByType(productData.getType());
    HashMap<String, Integer> typeMap = new HashMap<>();
    typeMap.put("string", 1);
    typeMap.put("int", 2);
    typeMap.put("float", 3);
    HashMap<String, Integer> dataTypeMap = new HashMap<>();
    dataTypeMap.put("input", 1);
    dataTypeMap.put("output", 2);
    var p2 = typeMap.get(productFunctionEntity.getType());
    var p3 = dataTypeMap.get(productFunctionEntity.getDataType());
    if (result.isEmpty() || !p1.isEmpty() || p2 == null || p3 == null)
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    else {
      try {
        String max = productFunction.getMax();
        String min = productFunction.getMin();
        if (max != null && min != null
            && Double.parseDouble(max) - Double.parseDouble(min) < 1e-8) {
          return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
        }
      } catch (Exception e) {
        return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
      }
      ProductFunctionEntity productFunctionEntity1 =
          productFunctionRepository.save(productFunctionEntity);
      return ResultTool.success(productFunctionEntity1);
    }
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> deleteProductFunction(int id) {
    List<ProductFunctionEntity> result = productFunctionRepository.deleteById(id);
    if (result.isEmpty())
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    else {
      return ResultTool.success(result);
    }
  }
}
