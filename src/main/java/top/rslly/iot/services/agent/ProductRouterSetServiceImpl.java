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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.rslly.iot.dao.*;
import top.rslly.iot.models.ProductRouterSetEntity;
import top.rslly.iot.param.request.ProductRouterSet;
import top.rslly.iot.services.thingsModel.ProductServiceImpl;
import top.rslly.iot.utility.JwtTokenUtil;
import top.rslly.iot.utility.RedisUtil;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductRouterSetServiceImpl implements ProductRouterSetService {
  @Resource
  private ProductRouterSetRepository productRouterSetRepository;
  @Resource
  private WxProductBindRepository wxProductBindRepository;
  @Resource
  private UserProductBindRepository userProductBindRepository;
  @Resource
  private WxUserRepository wxUserRepository;
  @Resource
  private UserRepository userRepository;
  @Resource
  private ProductServiceImpl productService;
  @Resource
  private WxProductActiveRepository wxProductActiveRepository;
  @Autowired
  private RedisUtil redisUtil;

  @Override
  public List<ProductRouterSetEntity> findAllById(int id) {
    return productRouterSetRepository.findAllById(id);
  }

  @Override
  public List<ProductRouterSetEntity> findAllByProductId(int productId) {
    return productRouterSetRepository.findAllByProductId(productId);
  }

  @Override
  public JsonResult<?> getProductRouterSet(String token) {
    String token_deal = token.replace(JwtTokenUtil.TOKEN_PREFIX, "");
    String role = JwtTokenUtil.getUserRole(token_deal);
    String username = JwtTokenUtil.getUsername(token_deal);
    List<ProductRouterSetEntity> result;
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
        List<ProductRouterSetEntity> productRouterSetEntities =
            productRouterSetRepository.deleteAllByProductId(s.getProductId());
        result.addAll(productRouterSetEntities);
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
        List<ProductRouterSetEntity> productRouterSetEntities =
            productRouterSetRepository.findAllByProductId(s.getProductId());
        result.addAll(productRouterSetEntities);
      }
    } else {
      result = productRouterSetRepository.findAll();
    }
    if (result.isEmpty()) {
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    } else
      return ResultTool.success(result);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> postProductRouterSet(ProductRouterSet productRouterSet) {
    if (productService.findAllById(productRouterSet.getProductId()).isEmpty())
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    ProductRouterSetEntity productRouterSetEntity = new ProductRouterSetEntity();
    var productRouterSetEntityList =
        productRouterSetRepository.findAllByProductId(productRouterSet.getProductId());
    if (!productRouterSetEntityList.isEmpty()) {
      productRouterSetEntity.setId(productRouterSetEntityList.get(0).getId());
    }
    productRouterSetEntity.setProductId(productRouterSet.getProductId());
    productRouterSetEntity.setPrompt(productRouterSet.getPrompt());
    if (redisUtil.hasKey("memory" + "chatProduct" + productRouterSet.getProductId())) {
      redisUtil.del("memory" + "chatProduct" + productRouterSet.getProductId());
    }
    var wxProductActiveEntityList =
        wxProductActiveRepository.findAllByProductId(productRouterSet.getProductId());
    for (var s : wxProductActiveEntityList) {
      if (redisUtil.hasKey("memory" + s.getOpenid())) {
        redisUtil.del("memory" + s.getOpenid());
      }
    }
    var result = productRouterSetRepository.save(productRouterSetEntity);
    return ResultTool.success(result);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> deleteProductRouterSet(int id) {
    var productRouterSetEntityList = productRouterSetRepository.findAllById(id);
    if (productRouterSetEntityList.isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    } else {
      List<ProductRouterSetEntity> result = productRouterSetRepository.deleteAllById(id);
      return ResultTool.success(result);
    }
  }
}
