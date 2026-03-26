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

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.rslly.iot.dao.*;
import top.rslly.iot.models.ProductAsrEntity;
import top.rslly.iot.models.WxUserEntity;
import top.rslly.iot.param.request.ProductAsrParam;
import top.rslly.iot.utility.JwtTokenUtil;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductAsrServiceImpl implements ProductAsrService {
  @Resource
  private ProductAsrRepository productAsrRepository;
  @Resource
  private WxProductBindRepository wxProductBindRepository;
  @Resource
  private UserProductBindRepository userProductBindRepository;
  @Resource
  private WxUserRepository wxUserRepository;
  @Resource
  private UserRepository userRepository;
  @Resource
  private ProductRepository productRepository;

  @Override
  public List<ProductAsrEntity> findAllById(int id) {
    return productAsrRepository.findAllById(id);
  }

  @Override
  public JsonResult<?> getProductAsr(String token) {
    String token_deal = token.replace(JwtTokenUtil.TOKEN_PREFIX, "");
    String role = JwtTokenUtil.getUserRole(token_deal);
    String username = JwtTokenUtil.getUsername(token_deal);
    List<ProductAsrEntity> result;
    if (role.equals("ROLE_" + "wx_user")) {
      if (wxUserRepository.findAllByName(username).isEmpty()) {
        return ResultTool.fail(ResultCode.COMMON_FAIL);
      }
      List<WxUserEntity> wxUserEntityList = wxUserRepository.findAllByName(username);
      String appid = wxUserEntityList.get(0).getAppid();
      String openid = wxUserEntityList.get(0).getOpenid();
      result = new ArrayList<>();
      var wxBindProductResponseList =
          wxProductBindRepository.findAllByAppidAndOpenid(appid, openid);
      if (wxBindProductResponseList.isEmpty()) {
        return ResultTool.fail(ResultCode.COMMON_FAIL);
      }
      for (var s : wxBindProductResponseList) {
        List<ProductAsrEntity> productRoleEntities =
            productAsrRepository.findAllByProductId(s.getProductId());
        result.addAll(productRoleEntities);
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
        List<ProductAsrEntity> productRoleEntities =
            productAsrRepository.findAllByProductId(s.getProductId());
        result.addAll(productRoleEntities);
      }
    } else {
      result = productAsrRepository.findAll();
    }
    if (result.isEmpty()) {
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    } else
      return ResultTool.success(result);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> addProductAsr(ProductAsrParam param) {
    if (productRepository.findAllById(param.getProductId()).isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    if (!productAsrRepository.findAllByProductId(param.getProductId()).isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    if (!param.getAsrName().equals("dashscope") && !param.getAsrName().equals("funasr")) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    if (!param.getProviderName().equals("default")) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    ProductAsrEntity productAsrEntity = new ProductAsrEntity();
    productAsrEntity.setAsrName(param.getAsrName());
    productAsrEntity.setProviderName(param.getProviderName());
    productAsrEntity.setProductId(param.getProductId());
    var result = productAsrRepository.save(productAsrEntity);
    return ResultTool.success(result);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> updateProductAsr(ProductAsrParam param) {
    var productAsrEntityList = productAsrRepository.findAllByProductId(param.getProductId());
    if (productAsrEntityList.isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    if (!param.getAsrName().equals("dashscope") && !param.getAsrName().equals("funasr")) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    if (!param.getProviderName().equals("default")) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    ProductAsrEntity productAsrEntity = new ProductAsrEntity();
    productAsrEntity.setId(productAsrEntityList.getFirst().getId());
    productAsrEntity.setAsrName(param.getAsrName());
    productAsrEntity.setProviderName(param.getProviderName());
    productAsrEntity.setProductId(param.getProductId());
    var result = productAsrRepository.save(productAsrEntity);
    return ResultTool.success(result);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> deleteProductAsr(int id) {
    List<ProductAsrEntity> productAsrEntityList = productAsrRepository.findAllById(id);
    if (productAsrEntityList.isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    var result = productAsrRepository.deleteAllById(id);
    return ResultTool.success(result);
  }
}
