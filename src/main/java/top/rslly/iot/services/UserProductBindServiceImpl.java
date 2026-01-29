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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.rslly.iot.dao.ProductRepository;
import top.rslly.iot.dao.UserProductBindRepository;
import top.rslly.iot.dao.UserRepository;
import top.rslly.iot.models.UserEntity;
import top.rslly.iot.models.UserProductBindEntity;
import top.rslly.iot.param.request.UserBindProduct;
import top.rslly.iot.utility.JwtTokenUtil;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import jakarta.annotation.Resource;
import java.util.List;

@Service
public class UserProductBindServiceImpl implements UserProductBindService {
  @Resource
  private UserProductBindRepository userProductBindRepository;
  @Resource
  private UserRepository userRepository;
  @Resource
  private ProductRepository productRepository;

  @Override
  public List<UserProductBindEntity> findAllByUserId(int userId) {
    return userProductBindRepository.findAllByUserId(userId);
  }

  @Override
  public List<UserProductBindEntity> findAllByUserIdAndProductId(int userId, int productId) {
    return userProductBindRepository.findAllByUserIdAndProductId(userId, productId);
  }

  @Override
  public JsonResult<?> userProductBindList(String token) {
    String token_deal = token.replace(JwtTokenUtil.TOKEN_PREFIX, "");
    String role = JwtTokenUtil.getUserRole(token_deal);
    String username = JwtTokenUtil.getUsername(token_deal);
    if (role.equals("ROLE_" + "wx_user") || role.equals("[ROLE_admin]")) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    List<UserEntity> user = userRepository.findAllByUsername(username);
    var res = userProductBindRepository.findAllByUserId(user.get(0).getId());
    if (res.isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return ResultTool.success(res);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> userProductBind(UserBindProduct userBindProduct, String token) {
    String token_deal = token.replace(JwtTokenUtil.TOKEN_PREFIX, "");
    String role = JwtTokenUtil.getUserRole(token_deal);
    String username = JwtTokenUtil.getUsername(token_deal);
    if (role.equals("ROLE_" + "wx_user") || role.equals("[ROLE_admin]")) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    var productList = productRepository.findAllByProductNameAndKeyvalue(
        userBindProduct.getProductName(), userBindProduct.getProductKey());
    if (productList.isEmpty())
      return ResultTool.fail(ResultCode.NO_PERMISSION);

    UserProductBindEntity userProductBindEntity = new UserProductBindEntity();
    userProductBindEntity.setProductId(productList.get(0).getId());
    userProductBindEntity.setUserId(userRepository.findAllByUsername(username).get(0).getId());
    if (!userProductBindRepository.findAllByUserIdAndProductId(userProductBindEntity.getUserId(),
        userProductBindEntity.getProductId()).isEmpty())
      return ResultTool.fail(ResultCode.USER_ACCOUNT_ALREADY_EXIST);
    var res = userProductBindRepository.save(userProductBindEntity);
    return ResultTool.success(res);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> userProductUnbind(UserBindProduct userBindProduct, String token) {
    String token_deal = token.replace(JwtTokenUtil.TOKEN_PREFIX, "");
    String role = JwtTokenUtil.getUserRole(token_deal);
    String username = JwtTokenUtil.getUsername(token_deal);
    if (role.equals("ROLE_" + "wx_user") || role.equals("[ROLE_admin]")) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    var productList = productRepository.findAllByProductNameAndKeyvalue(
        userBindProduct.getProductName(), userBindProduct.getProductKey());
    if (productList.isEmpty())
      return ResultTool.fail(ResultCode.NO_PERMISSION);
    var res = userProductBindRepository.findAllByUserIdAndProductId(
        userRepository.findAllByUsername(username).get(0).getId(),
        productList.get(0).getId());
    if (res.isEmpty())
      return ResultTool.fail(ResultCode.NO_PERMISSION);
    userProductBindRepository.deleteById(res.get(0).getId());
    return ResultTool.success();
  }
}
