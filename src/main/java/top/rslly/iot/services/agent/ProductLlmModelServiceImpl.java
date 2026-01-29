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
import top.rslly.iot.dao.*;
import top.rslly.iot.models.ProductLlmModelEntity;
import top.rslly.iot.models.WxUserEntity;
import top.rslly.iot.param.request.ProductLlmModel;
import top.rslly.iot.utility.JwtTokenUtil;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class ProductLlmModelServiceImpl implements ProductLlmModelService {
  @Resource
  private ProductLlmModelRepository productLlmModelRepository;
  @Resource
  private WxProductBindRepository wxProductBindRepository;
  @Resource
  private UserProductBindRepository userProductBindRepository;
  @Resource
  private WxUserRepository wxUserRepository;
  @Resource
  private UserRepository userRepository;
  @Resource
  private LlmProviderInformationRepository llmProviderInformationRepository;
  @Resource
  private ProductRepository productRepository;

  @Override
  public List<ProductLlmModelEntity> findAllById(int id) {
    return productLlmModelRepository.findAllById(id);
  }

  @Override
  public List<ProductLlmModelEntity> findAllByProductId(int productId) {
    return productLlmModelRepository.findAllByProductId(productId);
  }

  @Override
  public List<ProductLlmModelEntity> findAllByProductIdAndToolsId(int productId, String toolsId) {
    return productLlmModelRepository.findAllByProductIdAndToolsId(productId, toolsId);
  }


  @Override
  public JsonResult<?> getProductLlmModel(String token) {
    String token_deal = token.replace(JwtTokenUtil.TOKEN_PREFIX, "");
    String role = JwtTokenUtil.getUserRole(token_deal);
    String username = JwtTokenUtil.getUsername(token_deal);
    List<ProductLlmModelEntity> result;
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
        List<ProductLlmModelEntity> productLlmModelEntities =
            productLlmModelRepository.findAllByProductId(s.getProductId());
        result.addAll(productLlmModelEntities);
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
        List<ProductLlmModelEntity> productLlmModelEntities =
            productLlmModelRepository.findAllByProductId(s.getProductId());
        result.addAll(productLlmModelEntities);
      }
    } else {
      result = productLlmModelRepository.findAll();
    }
    if (result.isEmpty()) {
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    } else
      return ResultTool.success(result);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> postProductLlmModel(ProductLlmModel productLlmModel) {
    if (productRepository.findAllById(productLlmModel.getProductId()).isEmpty()) {
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    }
    if (llmProviderInformationRepository.findAllById(productLlmModel.getProviderId()).isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    Set<String> allowed = Set.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
        "classifier", "longMemory", "memory");
    if (!allowed.contains(productLlmModel.getToolsId())) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    ProductLlmModelEntity productLlmModelEntity = new ProductLlmModelEntity();
    List<ProductLlmModelEntity> productLlmModelEntityList = productLlmModelRepository
        .findAllByProductIdAndToolsId(
            productLlmModel.getProductId(), productLlmModel.getToolsId());
    if (!productLlmModelEntityList.isEmpty()) {
      productLlmModelEntity.setId(productLlmModelEntityList.get(0).getId());
    }
    productLlmModelEntity.setModelName(productLlmModel.getModelName());
    productLlmModelEntity.setProductId(productLlmModel.getProductId());
    productLlmModelEntity.setProviderId(productLlmModel.getProviderId());
    productLlmModelEntity.setToolsId(productLlmModel.getToolsId());
    var result = productLlmModelRepository.save(productLlmModelEntity);
    return ResultTool.success(result);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> deleteProductLlmModel(int id) {
    if (productLlmModelRepository.findAllById(id).isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    } else {
      var productLlmModelEntityList = productLlmModelRepository.deleteAllById(id);
      if (productLlmModelEntityList.isEmpty()) {
        return ResultTool.fail(ResultCode.COMMON_FAIL);
      } else {
        return ResultTool.success(productLlmModelEntityList);
      }
    }
  }
}
