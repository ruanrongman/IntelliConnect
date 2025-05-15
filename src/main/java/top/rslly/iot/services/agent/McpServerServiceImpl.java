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

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import top.rslly.iot.dao.*;
import top.rslly.iot.models.McpServerEntity;
import top.rslly.iot.models.ProductEntity;
import top.rslly.iot.param.request.McpServerParam;
import top.rslly.iot.utility.JwtTokenUtil;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Component
public class McpServerServiceImpl implements McpServerService {
  @Resource
  private McpServerRepository mcpServerRepository;
  @Resource
  private ProductRepository productRepository;
  @Resource
  private WxProductBindRepository wxProductBindRepository;
  @Resource
  private UserProductBindRepository userProductBindRepository;
  @Resource
  private WxUserRepository wxUserRepository;
  @Resource
  private UserRepository userRepository;

  @Override
  public List<McpServerEntity> findALLById(int id) {
    return mcpServerRepository.findALLById(id);
  }

  @Override
  public List<McpServerEntity> findAllByProductId(int productId) {
    return mcpServerRepository.findAllByProductId(productId);
  }

  @Override
  public JsonResult<?> getMcpServerList(String token) {
    String token_deal = token.replace(JwtTokenUtil.TOKEN_PREFIX, "");
    String role = JwtTokenUtil.getUserRole(token_deal);
    String username = JwtTokenUtil.getUsername(token_deal);
    List<McpServerEntity> result;
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
        List<McpServerEntity> mcpServerEntityList =
            mcpServerRepository.findAllByProductId(s.getProductId());
        result.addAll(mcpServerEntityList);
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
        List<McpServerEntity> mcpServerEntityList =
            mcpServerRepository.findAllByProductId(s.getProductId());
        result.addAll(mcpServerEntityList);
      }
    } else {
      result = mcpServerRepository.findAll();
    }
    if (result.isEmpty()) {
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    } else
      return ResultTool.success(result);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> postMcpServer(McpServerParam mcpServerParam) {
    McpServerEntity mcpServerEntity = new McpServerEntity();
    BeanUtils.copyProperties(mcpServerParam, mcpServerEntity);
    if (mcpServerParam.getUrl() == null || mcpServerParam.getDescription() == null
        || mcpServerParam.getProductId() == 0)
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    List<McpServerEntity> p1 = mcpServerRepository.findAllByUrl(mcpServerParam.getUrl());
    List<McpServerEntity> p2 =
        mcpServerRepository.findAllByDescription(mcpServerParam.getDescription());
    List<ProductEntity> result =
        productRepository.findAllById(mcpServerParam.getProductId());
    if (result.isEmpty() || !p1.isEmpty() || !p2.isEmpty())
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    else {
      McpServerEntity mcpServerEntity1 = mcpServerRepository.save(mcpServerEntity);
      return ResultTool.success(mcpServerEntity1);
    }
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> deleteMcpServer(int id) {
    List<McpServerEntity> result = mcpServerRepository.findALLById(id);
    if (result.isEmpty())
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    else {
      List<McpServerEntity> mcpServerEntityList = mcpServerRepository.deleteById(id);
      return ResultTool.success(mcpServerEntityList);
    }
  }
}
