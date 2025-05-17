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
package top.rslly.iot.services.thingsModel;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.rslly.iot.dao.*;
import top.rslly.iot.models.ProductEntity;
import top.rslly.iot.models.ProductModelEntity;
import top.rslly.iot.models.UserProductBindEntity;
import top.rslly.iot.models.WxProductBindEntity;
import top.rslly.iot.param.request.Product;
import top.rslly.iot.utility.JwtTokenUtil;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {
  @Resource
  private ProductRepository productRepository;
  @Resource
  private ProductModelRepository productModelRepository;
  @Resource
  private WxProductBindRepository wxProductBindRepository;
  @Resource
  private UserProductBindRepository userProductBindRepository;
  @Resource
  private MqttUserRepository mqttUserRepository;
  @Resource
  private WxUserRepository wxUserRepository;
  @Resource
  private UserRepository userRepository;


  @Override
  public JsonResult<?> getProduct(String token) {
    String token_deal = token.replace(JwtTokenUtil.TOKEN_PREFIX, "");
    String role = JwtTokenUtil.getUserRole(token_deal);
    String username = JwtTokenUtil.getUsername(token_deal);
    List<ProductEntity> result;
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
        List<ProductEntity> productEntities = productRepository.findAllById(s.getProductId());
        result.addAll(productEntities);
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
        List<ProductEntity> productEntities = productRepository.findAllById(s.getProductId());
        result.addAll(productEntities);
      }
    } else {
      result = productRepository.findAll();
    }
    if (result.isEmpty()) {
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    } else
      return ResultTool.success(result);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> postProduct(Product product, String token) {
    String token_deal = token.replace(JwtTokenUtil.TOKEN_PREFIX, "");
    String role = JwtTokenUtil.getUserRole(token_deal);
    String username = JwtTokenUtil.getUsername(token_deal);
    ProductEntity productEntity = new ProductEntity();
    BeanUtils.copyProperties(product, productEntity);
    // List<MqttUserEntity> result = mqttUserRepository.findALLById(productEntity.getMqttUser());
    List<ProductEntity> result =
        productRepository.findAllByProductName(productEntity.getProductName());
    if (!result.isEmpty())
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    else {
      ProductEntity productEntity1 = productRepository.save(productEntity);
      if (role.equals("ROLE_" + "wx_user")) {
        WxProductBindEntity wxProductBindEntity = new WxProductBindEntity();
        var userList = wxUserRepository.findAllByName(username);
        if (userList.isEmpty()) {
          return ResultTool.fail(ResultCode.COMMON_FAIL);
        }
        wxProductBindEntity.setOpenid(userList.get(0).getOpenid());
        wxProductBindEntity.setProductId(productEntity1.getId());
        wxProductBindRepository.save(wxProductBindEntity);
      } else if (!role.equals("[ROLE_admin]")) {
        UserProductBindEntity userProductBindEntity = new UserProductBindEntity();
        var userList = userRepository.findAllByUsername(username);
        if (userList.isEmpty()) {
          return ResultTool.fail(ResultCode.COMMON_FAIL);
        }
        userProductBindEntity.setUserId(userList.get(0).getId());
        userProductBindEntity.setProductId(productEntity1.getId());
        userProductBindRepository.save(userProductBindEntity);
      }
      return ResultTool.success(productEntity1);
    }
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> deleteProduct(int id) {
    List<ProductModelEntity> productModelEntityList = productModelRepository.findAllByProductId(id);
    List<WxProductBindEntity> wxProductBindEntityList =
        wxProductBindRepository.findAllByProductId(id);
    List<UserProductBindEntity> userProductBindEntityList =
        userProductBindRepository.findAllByProductId(id);
    if (productModelEntityList.isEmpty() && wxProductBindEntityList.isEmpty()) {
      List<ProductEntity> result = productRepository.deleteById(id);
      if (result.isEmpty())
        return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
      else {
        if (userProductBindEntityList.isEmpty()) {
          userProductBindRepository.deleteByProductId(id);
        }
        return ResultTool.success(result);
      }
    } else {
      return ResultTool.fail(ResultCode.HAS_DEPENDENCIES);
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
