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

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.rslly.iot.dao.CodingAgentDeviceRepository;
import top.rslly.iot.dao.CodingAgentSessionRepository;
import top.rslly.iot.dao.ProductRepository;
import top.rslly.iot.dao.UserProductBindRepository;
import top.rslly.iot.dao.UserRepository;
import top.rslly.iot.dao.WxProductBindRepository;
import top.rslly.iot.dao.WxUserRepository;
import top.rslly.iot.models.CodingAgentType;
import top.rslly.iot.models.CodingAgentDevice;
import top.rslly.iot.models.WxUserEntity;
import top.rslly.iot.param.request.CodingAgentDeviceParam;
import top.rslly.iot.utility.JwtTokenUtil;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import java.util.ArrayList;
import java.util.List;

@Service
public class CodingAgentDeviceServiceImpl implements CodingAgentDeviceService {
  @Resource
  private CodingAgentDeviceRepository codingAgentDeviceRepository;

  @Resource
  private CodingAgentSessionRepository codingAgentSessionRepository;

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
  public List<CodingAgentDevice> findAll() {
    return codingAgentDeviceRepository.findAll();
  }

  @Override
  public List<CodingAgentDevice> findAllById(int id) {
    return codingAgentDeviceRepository.findAllById(id);
  }

  @Override
  public List<CodingAgentDevice> findAllByProductId(int productId) {
    return codingAgentDeviceRepository.findAllByProductId(productId);
  }

  @Override
  public List<CodingAgentDevice> findAllByProductIdAndPairCodeAndAgentType(int productId,
      String pairCode, String agentType) {
    return codingAgentDeviceRepository.findAllByProductIdAndPairCodeAndAgentType(productId,
        pairCode, agentType);
  }

  @Override
  public JsonResult<?> getCodingAgentDevice(String token) {
    String tokenDeal = token.replace(JwtTokenUtil.TOKEN_PREFIX, "");
    String role = JwtTokenUtil.getUserRole(tokenDeal);
    String username = JwtTokenUtil.getUsername(tokenDeal);
    List<CodingAgentDevice> result;
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
        result.addAll(codingAgentDeviceRepository.findAllByProductId(s.getProductId()));
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
        result.addAll(codingAgentDeviceRepository.findAllByProductId(s.getProductId()));
      }
    } else {
      result = codingAgentDeviceRepository.findAll();
    }
    if (result.isEmpty()) {
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    } else
      return ResultTool.success(result);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> postCodingAgentDevice(CodingAgentDeviceParam codingAgentDeviceParam) {
    if (productRepository.findAllById(codingAgentDeviceParam.getProductId()).isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    String pairCode = codingAgentDeviceParam.getPairCode().trim();
    String agentType;
    try {
      agentType = CodingAgentType.normalize(codingAgentDeviceParam.getAgentType());
    } catch (IllegalArgumentException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    if (!codingAgentDeviceRepository.findAllByProductIdAndPairCodeAndAgentType(
        codingAgentDeviceParam.getProductId(), pairCode, agentType).isEmpty()) {
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    }
    CodingAgentDevice codingAgentDevice = new CodingAgentDevice();
    codingAgentDevice.setPairCode(pairCode);
    codingAgentDevice.setAgentType(agentType);
    codingAgentDevice.setProductId(codingAgentDeviceParam.getProductId());
    CodingAgentDevice result = codingAgentDeviceRepository.save(codingAgentDevice);
    return ResultTool.success(result);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> updateCodingAgentDevice(CodingAgentDeviceParam codingAgentDeviceParam) {
    if (codingAgentDeviceParam.getId() == null) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    List<CodingAgentDevice> codingAgentDeviceList =
        codingAgentDeviceRepository.findAllById(codingAgentDeviceParam.getId());
    if (codingAgentDeviceList.isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    CodingAgentDevice codingAgentDevice = codingAgentDeviceList.get(0);
    if (productRepository.findAllById(codingAgentDeviceParam.getProductId()).isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    String pairCode = codingAgentDeviceParam.getPairCode().trim();
    String agentType;
    try {
      agentType = CodingAgentType.normalize(codingAgentDeviceParam.getAgentType());
    } catch (IllegalArgumentException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    List<CodingAgentDevice> duplicateList =
        codingAgentDeviceRepository.findAllByProductIdAndPairCodeAndAgentType(
            codingAgentDeviceParam.getProductId(), pairCode, agentType);
    for (CodingAgentDevice duplicate : duplicateList) {
      if (!duplicate.getId().equals(codingAgentDeviceParam.getId())) {
        return ResultTool.fail(ResultCode.COMMON_FAIL);
      }
    }
    codingAgentDevice.setPairCode(pairCode);
    codingAgentDevice.setAgentType(agentType);
    codingAgentDevice.setProductId(codingAgentDeviceParam.getProductId());
    CodingAgentDevice result = codingAgentDeviceRepository.save(codingAgentDevice);
    return ResultTool.success(result);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> deleteCodingAgentDevice(int id) {
    List<CodingAgentDevice> codingAgentDeviceList = codingAgentDeviceRepository.findAllById(id);
    if (codingAgentDeviceList.isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    if (!codingAgentSessionRepository.findAllByAgentId(id).isEmpty()) {
      return ResultTool.fail(ResultCode.HAS_DEPENDENCIES);
    }
    List<CodingAgentDevice> result = codingAgentDeviceRepository.deleteAllById(id);
    return ResultTool.success(result);
  }
}
