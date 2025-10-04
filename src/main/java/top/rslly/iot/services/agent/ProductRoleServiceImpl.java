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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.rslly.iot.dao.*;
import top.rslly.iot.models.ProductEntity;
import top.rslly.iot.models.ProductRoleEntity;
import top.rslly.iot.models.WxUserEntity;
import top.rslly.iot.param.prompt.ProductRoleDescription;
import top.rslly.iot.param.request.ProductRole;
import top.rslly.iot.services.agent.ProductRoleService;
import top.rslly.iot.utility.JwtTokenUtil;
import top.rslly.iot.utility.ai.voice.VoiceTimbre;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Service
@Slf4j
public class ProductRoleServiceImpl implements ProductRoleService {
  @Resource
  private ProductRepository productRepository;
  @Resource
  private ProductRoleRepository productRoleRepository;
  @Resource
  private WxProductBindRepository wxProductBindRepository;
  @Resource
  private UserProductBindRepository userProductBindRepository;
  @Resource
  private WxUserRepository wxUserRepository;
  @Resource
  private UserRepository userRepository;

  @Override
  public List<ProductRoleEntity> findAllById(int id) {
    return productRoleRepository.findAllById(id);
  }

  @Override
  public List<ProductRoleEntity> findAllByProductId(int productId) {
    return productRoleRepository.findAllByProductId(productId);
  }

  @Override
  public List<ProductRoleEntity> deleteAllByProductId(int productId) {
    return productRoleRepository.deleteAllByProductId(productId);
  }

  @Override
  public List<ProductRoleDescription> getDescription(int productId) {
    var result = productRoleRepository.findAllByProductId(productId);
    List<ProductRoleDescription> productRoleDescriptionList = new LinkedList<>();
    if (!result.isEmpty()) {
      for (var s : result) {
        ProductRoleDescription productRoleDescription = new ProductRoleDescription();
        productRoleDescription.setRole(s.getRole());
        productRoleDescription.setRoleIntroduction(s.getRoleIntroduction());
        productRoleDescription.setAssistantName(s.getAssistantName());
        productRoleDescription.setUserName(s.getUserName());
        productRoleDescription.setVoice(s.getVoice());
        productRoleDescriptionList.add(productRoleDescription);
      }
    }
    return productRoleDescriptionList;
  }

  @Override
  public JsonResult<?> getProductRole(String token) {
    String token_deal = token.replace(JwtTokenUtil.TOKEN_PREFIX, "");
    String role = JwtTokenUtil.getUserRole(token_deal);
    String username = JwtTokenUtil.getUsername(token_deal);
    List<ProductRoleEntity> result;
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
        List<ProductRoleEntity> productRoleEntities =
            productRoleRepository.findAllByProductId(s.getProductId());
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
        List<ProductRoleEntity> productRoleEntities =
            productRoleRepository.findAllByProductId(s.getProductId());
        result.addAll(productRoleEntities);
      }
    } else {
      result = productRoleRepository.findAll();
    }
    if (result.isEmpty()) {
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    } else
      return ResultTool.success(result);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> postProductRole(ProductRole productRole) {
    ProductRoleEntity productRoleEntity = new ProductRoleEntity();
    BeanUtils.copyProperties(productRole, productRoleEntity);
    List<ProductEntity> result = productRepository.findAllById(productRole.getProductId());
    List<ProductRoleEntity> p1 = productRoleRepository
        .findAllByProductId(productRole.getProductId());
    // 校验声音voice是否在VoiceTimbre enum中
    if (!VoiceTimbre.isValidVoice(productRole.getVoice())) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    if (result.isEmpty() || !p1.isEmpty())
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    else {
      ProductRoleEntity productRoleEntity1 = productRoleRepository.save(productRoleEntity);
      return ResultTool.success(productRoleEntity1);
    }
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> putProductRole(ProductRole productRole) {
    List<ProductRoleEntity> productRoleEntityList =
        productRoleRepository.findAllByProductId(productRole.getProductId());
    // 校验声音voice是否在VoiceTimbre enum中
    if (!VoiceTimbre.isValidVoice(productRole.getVoice())) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    if (productRoleEntityList.isEmpty())
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    else {
      ProductRoleEntity productRoleEntity = new ProductRoleEntity();
      BeanUtils.copyProperties(productRole, productRoleEntity);
      productRoleEntity.setId(productRoleEntityList.get(0).getId());
      ProductRoleEntity result = productRoleRepository.save(productRoleEntity);
      return ResultTool.success(result);
    }
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> deleteProductRole(int id) {
    List<ProductRoleEntity> productRoleEntityList = productRoleRepository.findAllById(id);
    if (productRoleEntityList.isEmpty())
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    else {
      List<ProductRoleEntity> result = productRoleRepository.deleteById(id);
      return ResultTool.success(result);
    }
  }
}
