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

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.rslly.iot.dao.UserConfigRepository;
import top.rslly.iot.models.UserConfigEntity;
import top.rslly.iot.param.request.UserConfig;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import java.util.List;

@Service
@Slf4j
public class UserConfigServiceImpl implements UserConfigService {
  @Autowired
  private UserConfigRepository userConfigRepository;

  @Override
  public JsonResult<?> addUserConfig(UserConfig userConfig) {
    UserConfigEntity entity =
        userConfigRepository.getTopByProductIdAndName(userConfig.productId, userConfig.name);
    if (entity == null) {
      // Use MapStruct!!!
      entity = UserConfigEntity.builder()
          .userId(userConfig.userId)
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
    } else {
      entity.setName(userConfig.name);
      entity.setType(userConfig.type);
      entity.setValue(userConfig.value);
      entity.setDefaultValue(userConfig.defaultValue);
      entity.setMin(userConfig.min);
      entity.setMax(userConfig.max);
      entity.setRequired(userConfig.required);
      entity.setParent(userConfig.parent);
      entity.setDes(userConfig.des);
    }
    userConfigRepository.save(entity);
    return ResultTool.success();
  }

  @Override
  public JsonResult<?> deleteUserConfig(UserConfig userConfig) {
    UserConfigEntity entity =
        userConfigRepository.getTopByProductIdAndName(userConfig.productId, userConfig.name);
    if (entity == null) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    userConfigRepository.delete(entity);
    return ResultTool.success(entity.getId());
  }

  @Override
  public JsonResult<?> deleteAllByProductId(int productId) {
    userConfigRepository.deleteAllByProductId(productId);
    return ResultTool.success();
  }

  @Override
  public JsonResult<?> updateUserConfig(UserConfig userConfig) {
    return addUserConfig(userConfig);
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

  public String getConfigValue(int productId, String name){
    UserConfigEntity entity = userConfigRepository.getTopByProductIdAndName(productId, name);
    if(entity == null || entity.getValue() == null) return "";
    return entity.getValue();
  }
}
