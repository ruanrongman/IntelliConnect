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
import top.rslly.iot.dao.UserProductBindRepository;
import top.rslly.iot.dao.UserRepository;
import top.rslly.iot.dao.WxProductBindRepository;
import top.rslly.iot.dao.WxUserRepository;
import top.rslly.iot.models.AgentStatus;
import top.rslly.iot.models.CodingAgentType;
import top.rslly.iot.models.CodingAgentDevice;
import top.rslly.iot.models.CodingAgentSession;
import top.rslly.iot.models.WxUserEntity;
import top.rslly.iot.param.request.CodingAgentSessionParam;
import top.rslly.iot.param.request.CodingAgentSessionUploadParam;
import top.rslly.iot.param.response.CodingAgentSessionHardwareResponse;
import top.rslly.iot.utility.JwtTokenUtil;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class CodingAgentSessionServiceImpl implements CodingAgentSessionService {
  @Resource
  private CodingAgentSessionRepository codingAgentSessionRepository;

  @Resource
  private CodingAgentDeviceRepository codingAgentDeviceRepository;

  @Resource
  private WxProductBindRepository wxProductBindRepository;

  @Resource
  private UserProductBindRepository userProductBindRepository;

  @Resource
  private WxUserRepository wxUserRepository;

  @Resource
  private UserRepository userRepository;

  @Override
  public List<CodingAgentSession> findAll() {
    return codingAgentSessionRepository.findAll();
  }

  @Override
  public List<CodingAgentSession> findAllById(int id) {
    return codingAgentSessionRepository.findAllById(id);
  }

  @Override
  public List<CodingAgentSession> findAllByAgentId(int agentId) {
    return codingAgentSessionRepository.findAllByAgentId(agentId);
  }

  @Override
  public List<CodingAgentSession> findAllBySessionIdAndCwd(String sessionId, String cwd) {
    return codingAgentSessionRepository.findAllBySessionIdAndCwd(sessionId, cwd);
  }

  @Override
  public List<CodingAgentSession> findAllByAgentIdAndSessionIdAndCwd(int agentId, String sessionId,
      String cwd) {
    return codingAgentSessionRepository.findAllByAgentIdAndSessionIdAndCwd(agentId, sessionId,
        cwd);
  }

  @Override
  public JsonResult<?> getCodingAgentSession(String token) {
    String tokenDeal = token.replace(JwtTokenUtil.TOKEN_PREFIX, "");
    String role = JwtTokenUtil.getUserRole(tokenDeal);
    String username = JwtTokenUtil.getUsername(tokenDeal);
    List<CodingAgentSession> result;
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
        List<CodingAgentDevice> codingAgentDeviceList =
            codingAgentDeviceRepository.findAllByProductId(s.getProductId());
        for (var device : codingAgentDeviceList) {
          result.addAll(codingAgentSessionRepository.findAllByAgentId(device.getId()));
        }
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
        List<CodingAgentDevice> codingAgentDeviceList =
            codingAgentDeviceRepository.findAllByProductId(s.getProductId());
        for (var device : codingAgentDeviceList) {
          result.addAll(codingAgentSessionRepository.findAllByAgentId(device.getId()));
        }
      }
    } else {
      result = codingAgentSessionRepository.findAll();
    }
    if (result.isEmpty()) {
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    } else
      return ResultTool.success(result);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> postCodingAgentSession(CodingAgentSessionParam codingAgentSessionParam) {
    if (codingAgentDeviceRepository.findAllById(codingAgentSessionParam.getAgentId()).isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    String sessionId = codingAgentSessionParam.getSessionId().trim();
    String cwd = codingAgentSessionParam.getCwd().trim();
    String finalStatus = codingAgentSessionParam.getFinalStatus().trim();
    if (!AgentStatus.isValidStatus(finalStatus)) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    if (!codingAgentSessionRepository.findAllByAgentIdAndSessionIdAndCwd(
        codingAgentSessionParam.getAgentId(), sessionId, cwd).isEmpty()) {
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    }
    CodingAgentSession codingAgentSession = new CodingAgentSession();
    codingAgentSession.setAgentId(codingAgentSessionParam.getAgentId());
    codingAgentSession.setSessionId(sessionId);
    codingAgentSession.setCwd(cwd);
    codingAgentSession.setFinalStatus(AgentStatus.normalize(finalStatus));
    codingAgentSession.setLastUpdate(Instant.now());
    CodingAgentSession result = codingAgentSessionRepository.save(codingAgentSession);
    return ResultTool.success(result);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> updateCodingAgentSession(CodingAgentSessionParam codingAgentSessionParam) {
    if (codingAgentSessionParam.getId() == null) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    List<CodingAgentSession> codingAgentSessionList =
        codingAgentSessionRepository.findAllById(codingAgentSessionParam.getId());
    if (codingAgentSessionList.isEmpty()
        || codingAgentDeviceRepository.findAllById(codingAgentSessionParam.getAgentId())
            .isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    String sessionId = codingAgentSessionParam.getSessionId().trim();
    String cwd = codingAgentSessionParam.getCwd().trim();
    String finalStatus = codingAgentSessionParam.getFinalStatus().trim();
    if (!AgentStatus.isValidStatus(finalStatus)) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    List<CodingAgentSession> duplicateList =
        codingAgentSessionRepository.findAllByAgentIdAndSessionIdAndCwd(
            codingAgentSessionParam.getAgentId(), sessionId, cwd);
    for (var duplicate : duplicateList) {
      if (!duplicate.getId().equals(codingAgentSessionParam.getId())) {
        return ResultTool.fail(ResultCode.COMMON_FAIL);
      }
    }
    CodingAgentSession codingAgentSession = codingAgentSessionList.get(0);
    codingAgentSession.setAgentId(codingAgentSessionParam.getAgentId());
    codingAgentSession.setSessionId(sessionId);
    codingAgentSession.setCwd(cwd);
    codingAgentSession.setFinalStatus(AgentStatus.normalize(finalStatus));
    codingAgentSession.setLastUpdate(Instant.now());
    CodingAgentSession result = codingAgentSessionRepository.save(codingAgentSession);
    return ResultTool.success(result);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> uploadCodingAgentSession(
      CodingAgentSessionUploadParam codingAgentSessionUploadParam) {
    String pairCode = codingAgentSessionUploadParam.getPairCode().trim();
    if (codingAgentDeviceRepository.findAllByIdAndPairCode(
        codingAgentSessionUploadParam.getAgentId(), pairCode).isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    String sessionId = codingAgentSessionUploadParam.getSessionId().trim();
    String cwd = codingAgentSessionUploadParam.getCwd().trim();
    String finalStatus = codingAgentSessionUploadParam.getFinalStatus().trim();
    if (!AgentStatus.isValidStatus(finalStatus)) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    List<CodingAgentSession> codingAgentSessionList =
        codingAgentSessionRepository.findAllByAgentIdAndSessionIdAndCwd(
            codingAgentSessionUploadParam.getAgentId(), sessionId, cwd);
    CodingAgentSession codingAgentSession;
    if (codingAgentSessionList.isEmpty()) {
      codingAgentSession = new CodingAgentSession();
      codingAgentSession.setAgentId(codingAgentSessionUploadParam.getAgentId());
      codingAgentSession.setSessionId(sessionId);
      codingAgentSession.setCwd(cwd);
    } else {
      codingAgentSession = codingAgentSessionList.get(0);
    }
    codingAgentSession.setFinalStatus(AgentStatus.normalize(finalStatus));
    codingAgentSession.setLastUpdate(Instant.now());
    CodingAgentSession result = codingAgentSessionRepository.save(codingAgentSession);
    return ResultTool.success(result);
  }

  @Override
  public JsonResult<?> getCodingAgentSessionByDevice(int agentId, String pairCode) {
    String normalizedPairCode = pairCode.trim();
    List<CodingAgentDevice> codingAgentDeviceList =
        codingAgentDeviceRepository.findAllByIdAndPairCode(agentId, normalizedPairCode);
    if (codingAgentDeviceList.isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    CodingAgentDevice codingAgentDevice = codingAgentDeviceList.get(0);
    List<CodingAgentSessionHardwareResponse> result = new ArrayList<>();
    for (CodingAgentSession codingAgentSession : codingAgentSessionRepository
        .findAllByAgentId(agentId)) {
      CodingAgentSessionHardwareResponse response = new CodingAgentSessionHardwareResponse();
      response.setId(codingAgentSession.getId());
      response.setAgentId(codingAgentSession.getAgentId());
      response.setPairCode(normalizedPairCode);
      response.setAgentType(CodingAgentType.normalize(codingAgentDevice.getAgentType()));
      response.setSessionId(codingAgentSession.getSessionId());
      response.setCwd(codingAgentSession.getCwd());
      response.setFinalStatus(codingAgentSession.getFinalStatus());
      response.setLastUpdate(codingAgentSession.getLastUpdate().toString());
      result.add(response);
    }
    if (result.isEmpty()) {
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    } else
      return ResultTool.success(result);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> deleteCodingAgentSession(int id) {
    if (codingAgentSessionRepository.findAllById(id).isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    List<CodingAgentSession> result = codingAgentSessionRepository.deleteAllById(id);
    return ResultTool.success(result);
  }
}
