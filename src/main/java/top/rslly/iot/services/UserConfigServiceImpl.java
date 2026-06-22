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

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.rslly.iot.dao.UserConfigRepository;
import top.rslly.iot.dao.UserRepository;
import top.rslly.iot.dao.WxUserRepository;
import top.rslly.iot.models.UserConfigEntity;
import top.rslly.iot.param.request.UserConfig;
import top.rslly.iot.utility.JwtTokenUtil;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import java.util.List;

@Service
@Slf4j
public class UserConfigServiceImpl implements UserConfigService {
  @Resource
  private UserConfigRepository userConfigRepository;
  @Resource
  private UserRepository userRepository;
  @Resource
  private WxUserRepository wxUserRepository;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> addUserConfig(UserConfig userConfig, String header) {
    String token_deal = header.replace(JwtTokenUtil.TOKEN_PREFIX, "");
    String role = JwtTokenUtil.getUserRole(token_deal);
    String username = JwtTokenUtil.getUsername(token_deal);
    int userId = 0;
    String isWechatUser = "false";
    if (role.equals("ROLE_" + "wx_user")) {
      isWechatUser = "true";
      userId = wxUserRepository.findAllByName(username).get(0).getId();
    } else {
      userId = userRepository.findAllByUsername(username).get(0).getId();
    }
    boolean exists = userConfigRepository.existsByUserIdAndIsWechatUserAndProductIdAndName(userId,
        isWechatUser, userConfig.productId, userConfig.name);
    userConfigRepository.updateAllByProductIdAndName(userConfig.productId,
        userConfig.name, userConfig.type, userConfig.value, userConfig.defaultValue,
        userConfig.min, userConfig.max, userConfig.required, userConfig.parent, userConfig.des);
    if (!exists) {
      // Use MapStruct!!!
      UserConfigEntity entity = UserConfigEntity.builder()
          .userId(userId)
          .isWechatUser(isWechatUser)
          .productId(userConfig.productId)
          .name(userConfig.name)
          .type(userConfig.type)
          .value(userConfig.value)
          .defaultValue(userConfig.defaultValue)
          .min(userConfig.min)
          .max(userConfig.max)
          .required(userConfig.required)
          .parent(userConfig.parent)
          .des(userConfig.des)
          .build();
      userConfigRepository.save(entity);
    }
    return ResultTool.success();
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> deleteUserConfig(UserConfig userConfig) {
    List<UserConfigEntity> entities =
        userConfigRepository.getAllByProductIdAndName(userConfig.productId, userConfig.name);
    if (entities.isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    userConfigRepository.deleteAll(entities);
    return ResultTool.success();
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> deleteAllByProductId(int productId) {
    userConfigRepository.deleteAllByProductId(productId);
    return ResultTool.success();
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> updateUserConfig(UserConfig userConfig, String header) {
    return addUserConfig(userConfig, header);
  }

  @Override
  public JsonResult<?> getAllUserConfig(int productId) {
    List<UserConfigEntity> configs = userConfigRepository.getAllByProductId(productId);
    return ResultTool.success(configs);
  }

  @Override
  public JsonResult<?> getUserConfigByName(int productId, String name) {
    UserConfigEntity entity = userConfigRepository.getTopByProductIdAndName(productId, name);
    return ResultTool.success(entity);
  }

  public String getConfigValue(int productId, String name) {
    UserConfigEntity entity = userConfigRepository.getTopByProductIdAndName(productId, name);
    if (entity == null || entity.getValue() == null)
      return "";
    return entity.getValue();
  }
}
