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
import top.rslly.iot.dao.LlmProviderInformationRepository;
import top.rslly.iot.dao.ProductLlmModelRepository;
import top.rslly.iot.models.LlmProviderInformationEntity;
import top.rslly.iot.models.ProductLlmModelEntity;
import top.rslly.iot.param.request.LlmProviderInformation;
import top.rslly.iot.utility.JwtTokenUtil;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import jakarta.annotation.Resource;
import java.util.List;

@Service
public class LlmProviderInformationServiceImpl implements LlmProviderInformationService {
  @Resource
  private LlmProviderInformationRepository llmProviderInformationRepository;

  @Resource
  private ProductLlmModelRepository productLlmModelRepository;

  @Override
  public List<LlmProviderInformationEntity> findAllById(int id) {
    return llmProviderInformationRepository.findAllById(id);
  }

  @Override
  public JsonResult<?> getLLmProviderInformation(String token) {
    try {
      String token_deal = token.replace(JwtTokenUtil.TOKEN_PREFIX, "");
      String role = JwtTokenUtil.getUserRole(token_deal);
      String username = JwtTokenUtil.getUsername(token_deal);

      // Check if user is admin
      boolean isAdmin = role.equals("[ROLE_admin]");

      List<LlmProviderInformationEntity> result;
      if (isAdmin) {
        // Admin can see all records
        result = llmProviderInformationRepository.findAll();
      } else {
        // Regular user can only see their own records
        result = llmProviderInformationRepository.findAllByUserName(username);
      }

      return ResultTool.success(result);
    } catch (Exception e) {
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    }
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> postLLmProviderInformation(String token,
      LlmProviderInformation llmProviderInformation) {
    try {
      String token_deal = token.replace(JwtTokenUtil.TOKEN_PREFIX, "");
      String role = JwtTokenUtil.getUserRole(token_deal);
      String username = JwtTokenUtil.getUsername(token_deal);

      // Check if user is admin
      boolean isAdmin = role.equals("[ROLE_admin]");
      if (!llmProviderInformation.getType().equals("openai")) {
        return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
      }
      // Convert the request object to entity
      LlmProviderInformationEntity incomingEntity = new LlmProviderInformationEntity();
      incomingEntity.setProviderName(llmProviderInformation.getProviderName());
      incomingEntity.setBaseUrl(llmProviderInformation.getBaseUrl());
      incomingEntity.setAppKey(llmProviderInformation.getAppKey());
      incomingEntity.setType(llmProviderInformation.getType());
      incomingEntity.setUserName(username);

      // First, check for existing record owned by the current user
      List<LlmProviderInformationEntity> existingList =
          llmProviderInformationRepository.findAllByProviderNameAndUserName(
              incomingEntity.getProviderName(), username);

      // If no record found for current user and user is admin, check for any user's record with
      // this provider name
      if (isAdmin && existingList.isEmpty()) {
        // Admin can update any record with this provider name, regardless of owner
        List<LlmProviderInformationEntity> providerRecords =
            llmProviderInformationRepository
                .findAllByProviderName(incomingEntity.getProviderName());
        if (!providerRecords.isEmpty()) {
          // Use the first record with this provider name
          existingList = providerRecords;
        }
      }

      if (!existingList.isEmpty()) {
        // This is an update - get the existing entity to preserve protected fields
        LlmProviderInformationEntity existingEntity = existingList.get(0);

        // For updates, we don't allow changing providerName and userName (as required)
        // Only other fields (baseUrl, appKey, type) can be updated
        existingEntity.setBaseUrl(incomingEntity.getBaseUrl());
        existingEntity.setAppKey(incomingEntity.getAppKey());
        existingEntity.setType(incomingEntity.getType());

        // Save and return the updated entity
        LlmProviderInformationEntity savedEntity =
            llmProviderInformationRepository.save(existingEntity);
        return ResultTool.success(savedEntity);
      } else {
        // This is a new record - save it
        LlmProviderInformationEntity savedEntity =
            llmProviderInformationRepository.save(incomingEntity);
        return ResultTool.success(savedEntity);
      }
    } catch (Exception e) {
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    }
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> deleteLLmProviderInformation(int id) {
    // Check if there are any ProductLlmModelEntity records that depend on this provider
    List<ProductLlmModelEntity> productLlmModelEntityList =
        productLlmModelRepository.findAllByProviderId(id);

    if (productLlmModelEntityList.isEmpty()) {
      // Get the entity to check if it exists
      List<LlmProviderInformationEntity> llmProviderInformationEntities =
          llmProviderInformationRepository.findAllById(id);
      if (llmProviderInformationEntities.isEmpty()) {
        return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
      } else {
        // Delete the entity
        List<LlmProviderInformationEntity> result =
            llmProviderInformationRepository.deleteAllById(id);

        return ResultTool.success(result);
      }
    } else {
      return ResultTool.fail(ResultCode.HAS_DEPENDENCIES);
    }
  }
}
