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
import top.rslly.iot.dao.*;
import top.rslly.iot.models.ProductEventEntity;
import top.rslly.iot.models.ProductFunctionEntity;
import top.rslly.iot.models.ProductModelEntity;
import top.rslly.iot.param.request.ProductEvent;
import top.rslly.iot.utility.JwtTokenUtil;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductEventServiceImpl implements ProductEventService {
  @Resource
  private ProductModelRepository productModelRepository;
  @Resource
  private ProductEventRepository productEventRepository;
  @Resource
  private WxProductBindRepository wxProductBindRepository;
  @Resource
  private UserProductBindRepository userProductBindRepository;
  @Resource
  private WxUserRepository wxUserRepository;
  @Resource
  private UserRepository userRepository;

  @Override
  public List<ProductEventEntity> findAllByModelIdAndName(int modelId, String name) {
    return productEventRepository.findAllByModelIdAndName(modelId, name);
  }

  @Override
  public JsonResult<?> getProductEvent(String token) {
    String token_deal = token.replace(JwtTokenUtil.TOKEN_PREFIX, "");
    String role = JwtTokenUtil.getUserRole(token_deal);
    String username = JwtTokenUtil.getUsername(token_deal);
    List<ProductEventEntity> result;
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
          List<ProductEventEntity> productEventEntityList =
              productEventRepository.findAllByModelId(s1.getId());
          result.addAll(productEventEntityList);
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
          List<ProductEventEntity> productEventEntityList =
              productEventRepository.findAllByModelId(s1.getId());
          result.addAll(productEventEntityList);
        }
      }
    } else {
      result = productEventRepository.findAll();
    }
    if (result.isEmpty()) {
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    } else
      return ResultTool.success(result);
  }

  @Override
  public JsonResult<?> postProductEvent(ProductEvent productEvent) {
    ProductEventEntity productEventEntity = new ProductEventEntity();
    BeanUtils.copyProperties(productEvent, productEventEntity);
    List<ProductModelEntity> result = productModelRepository.findAllById(productEvent.getModelId());
    List<ProductEventEntity> p1 = productEventRepository
        .findAllByModelIdAndName(productEvent.getModelId(), productEvent.getName());
    if (result.isEmpty() || !p1.isEmpty())
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    else {
      ProductEventEntity productEventEntity1 = productEventRepository.save(productEventEntity);
      return ResultTool.success(productEventEntity1);
    }
  }

  @Override
  public JsonResult<?> deleteProductEvent(int id) {

    List<ProductEventEntity> result = productEventRepository.deleteById(id);
    if (result.isEmpty())
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    else {
      return ResultTool.success(result);
    }

  }
}
