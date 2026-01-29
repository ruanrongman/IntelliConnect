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
import top.rslly.iot.models.AgentLongMemoryEntity;
import top.rslly.iot.models.WxUserEntity;
import top.rslly.iot.param.prompt.AgentLongMemoryDescription;
import top.rslly.iot.param.request.AgentLongMemory;
import top.rslly.iot.param.request.AgentLongMemoryToolParam;
import top.rslly.iot.utility.JwtTokenUtil;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Service
public class AgentLongMemoryServiceImpl implements AgentLongMemoryService {
  @Resource
  private ProductRepository productRepository;
  @Resource
  private AgentLongMemoryRepository agentLongMemoryRepository;
  @Resource
  private WxProductBindRepository wxProductBindRepository;
  @Resource
  private UserProductBindRepository userProductBindRepository;
  @Resource
  private WxUserRepository wxUserRepository;
  @Resource
  private UserRepository userRepository;

  @Override
  public List<AgentLongMemoryEntity> findAllById(int id) {
    return agentLongMemoryRepository.findAllById(id);
  }

  @Override
  public List<AgentLongMemoryEntity> findAllByProductId(int productId) {
    return agentLongMemoryRepository.findAllByProductId(productId);
  }

  @Override
  public JsonResult<?> getLongMemory(String token) {
    String token_deal = token.replace(JwtTokenUtil.TOKEN_PREFIX, "");
    String role = JwtTokenUtil.getUserRole(token_deal);
    String username = JwtTokenUtil.getUsername(token_deal);
    List<AgentLongMemoryEntity> result;
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
        List<AgentLongMemoryEntity> agentLongMemoryEntities =
            agentLongMemoryRepository.findAllByProductId(s.getProductId());
        result.addAll(agentLongMemoryEntities);
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
        List<AgentLongMemoryEntity> agentLongMemoryEntities =
            agentLongMemoryRepository.findAllByProductId(s.getProductId());
        result.addAll(agentLongMemoryEntities);
      }
    } else {
      result = agentLongMemoryRepository.findAll();
    }
    if (result.isEmpty()) {
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    } else
      return ResultTool.success(result);
  }

  @Override
  public List<AgentLongMemoryDescription> getDescription(int productId) {
    var result = agentLongMemoryRepository.findAllByProductId(productId);
    List<AgentLongMemoryDescription> agentLongMemoryDescriptionList = new LinkedList<>();
    if (!result.isEmpty()) {
      for (var s : result) {
        AgentLongMemoryDescription agentLongMemoryDescription = new AgentLongMemoryDescription();
        agentLongMemoryDescription.setMemoryKey(s.getMemoryKey());
        agentLongMemoryDescription.setDescription(s.getDescription());
        agentLongMemoryDescription.setMemoryValue(s.getMemoryValue());
        agentLongMemoryDescriptionList.add(agentLongMemoryDescription);
      }
    }
    return agentLongMemoryDescriptionList;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> postLongMemory(AgentLongMemory agentLongMemory) {
    if (productRepository.findAllById(agentLongMemory.getProductId()).isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    AgentLongMemoryEntity agentLongMemoryEntity = new AgentLongMemoryEntity();
    List<AgentLongMemoryEntity> agentLongMemoryEntities =
        agentLongMemoryRepository.findAllByProductIdAndMemoryKey(agentLongMemory.getProductId(),
            agentLongMemory.getMemoryKey());
    if (!agentLongMemoryEntities.isEmpty()) {
      agentLongMemoryEntity.setId(agentLongMemoryEntities.get(0).getId());
    }
    agentLongMemoryEntity.setProductId(agentLongMemory.getProductId());
    agentLongMemoryEntity.setMemoryKey(agentLongMemory.getMemoryKey());
    agentLongMemoryEntity.setDescription(agentLongMemory.getDescription());
    agentLongMemoryEntity.setMemoryValue(agentLongMemory.getMemoryValue());
    var result = agentLongMemoryRepository.save(agentLongMemoryEntity);
    return ResultTool.success(result);
  }

  @Override
  public Boolean updateLongMemory(AgentLongMemoryToolParam agentLongMemoryToolParam) {
    if (productRepository.findAllById(agentLongMemoryToolParam.getProductId()).isEmpty()) {
      return false;
    }
    AgentLongMemoryEntity agentLongMemoryEntity = new AgentLongMemoryEntity();
    List<AgentLongMemoryEntity> agentLongMemoryEntities =
        agentLongMemoryRepository.findAllByProductIdAndMemoryKey(
            agentLongMemoryToolParam.getProductId(), agentLongMemoryToolParam.getMemoryKey());
    if (agentLongMemoryEntities.isEmpty()) {
      return false;
    }
    agentLongMemoryEntity.setId(agentLongMemoryEntities.get(0).getId());
    agentLongMemoryEntity.setProductId(agentLongMemoryToolParam.getProductId());
    agentLongMemoryEntity.setMemoryKey(agentLongMemoryToolParam.getMemoryKey());
    agentLongMemoryEntity.setDescription(agentLongMemoryEntities.get(0).getDescription());
    agentLongMemoryEntity.setMemoryValue(agentLongMemoryToolParam.getMemoryValue());
    agentLongMemoryRepository.save(agentLongMemoryEntity);
    return true;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> deleteLongMemory(int id) {
    if (agentLongMemoryRepository.findAllById(id).isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    } else {
      var result = agentLongMemoryRepository.deleteAllById(id);
      return ResultTool.success(result);
    }
  }
}
