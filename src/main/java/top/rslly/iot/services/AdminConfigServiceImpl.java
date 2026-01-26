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

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import top.rslly.iot.dao.AdminConfigRepository;
import top.rslly.iot.dao.ProductRepository;
import top.rslly.iot.models.AdminConfigEntity;
import top.rslly.iot.param.request.AdminConfig;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Set;

@Component
public class AdminConfigServiceImpl implements AdminConfigService {
  @Resource
  private AdminConfigRepository adminConfigRepository;
  @Resource
  private ProductRepository productRepository;

  @Override
  public List<AdminConfigEntity> findAllBySetKey(String setKey) {
    return adminConfigRepository.findAllBySetKey(setKey);
  }

  @Override
  public JsonResult<?> getAdminConfig() {
    List<AdminConfigEntity> adminConfigEntityList = adminConfigRepository.findAll();
    return adminConfigEntityList.isEmpty() ? ResultTool.fail(ResultCode.COMMON_FAIL)
        : ResultTool.success(adminConfigEntityList);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> postAdminConfig(AdminConfig adminConfig) {
    JsonResult<?> validationResult = validateAdminConfig(adminConfig);
    if (validationResult != null) {
      return validationResult;
    }
    if (!adminConfigRepository.findAllBySetKey(adminConfig.getSetKey()).isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    AdminConfigEntity adminConfigEntity = new AdminConfigEntity();
    adminConfigEntity.setSetKey(adminConfig.getSetKey());
    adminConfigEntity.setSetValue(adminConfig.getSetValue());
    var result = adminConfigRepository.save(adminConfigEntity);
    return ResultTool.success(result);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> putAdminConfig(AdminConfig adminConfig) {
    JsonResult<?> validationResult = validateAdminConfig(adminConfig);
    if (validationResult != null) {
      return validationResult;
    }
    if (adminConfigRepository.findAllBySetKey(adminConfig.getSetKey()).isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    adminConfigRepository.updateConfig(adminConfig.getSetKey(), adminConfig.getSetValue());
    return ResultTool.success();
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> deleteAdminConfig(int id) {
    if (adminConfigRepository.findAllById(id).isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    } else {
      var result = adminConfigRepository.deleteAllById(id);
      return ResultTool.success(result);
    }
  }

  private JsonResult<?> validateAdminConfig(AdminConfig adminConfig) {
    Set<String> allowed = Set.of("wx_default_product", "wx_trigger-keyword", "wx_success-message",
        "wx_unregistered-message", "ai_classifier_include_thought", "ai_detect_random");
    if (!allowed.contains(adminConfig.getSetKey())) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    if (adminConfig.getSetKey().equals("wx_default_product")) {
      try {
        int productId = Integer.parseInt(adminConfig.getSetValue());
        if (productId <= 0) {
          return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
        }
        if (productRepository.findAllById(productId).isEmpty()) {
          return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
        }
      } catch (NumberFormatException e) {
        return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
      }
    }
    // 其他配置项的验证
    if (adminConfig.getSetKey().equals("wx_trigger-keyword")) {
      // 关键词不能为空
      if (adminConfig.getSetValue() == null || adminConfig.getSetValue().trim().isEmpty()) {
        return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
      }
    }

    if (adminConfig.getSetKey().equals("wx_success-message")) {
      // 成功消息不能为空
      if (adminConfig.getSetValue() == null || adminConfig.getSetValue().trim().isEmpty()) {
        return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
      }
    }
    if (adminConfig.getSetKey().equals("wx_unregistered-message")) {
      // 未注册消息不能为空
      if (adminConfig.getSetValue() == null || adminConfig.getSetValue().trim().isEmpty()) {
        return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
      }
    }
    if (adminConfig.getSetKey().equals("ai_classifier_include_thought")) {
      if (adminConfig.getSetValue() == null || adminConfig.getSetValue().trim().isEmpty()) {
        return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
      }
      // 必须是"true"或者"false"
      if (!adminConfig.getSetValue().equals("true") && !adminConfig.getSetValue().equals("false")) {
        return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
      }
    }
    if (adminConfig.getSetKey().equals("ai_detect_random")) {
      if (adminConfig.getSetValue() == null || adminConfig.getSetValue().trim().isEmpty()) {
        return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
      }
      // 必须是"true"或者"false"
      if (!adminConfig.getSetValue().equals("true") && !adminConfig.getSetValue().equals("false")) {
        return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
      }
    }
    return null;
  }

}
