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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.rslly.iot.dao.*;
import top.rslly.iot.models.OtaEntity;
import top.rslly.iot.models.OtaXiaozhiPassiveEntity;
import top.rslly.iot.models.WxUserEntity;
import top.rslly.iot.param.request.OtaXiaozhiPassive;
import top.rslly.iot.param.response.OtaXiaozhiPassiveListResponse;
import top.rslly.iot.utility.JwtTokenUtil;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class OtaXiaozhiPassiveServiceImpl implements OtaXiaozhiPassiveService {
  @Resource
  private WxProductBindRepository wxProductBindRepository;
  @Resource
  private UserProductBindRepository userProductBindRepository;
  @Resource
  private WxUserRepository wxUserRepository;
  @Resource
  private UserRepository userRepository;
  @Resource
  private OtaXiaozhiPassiveRepository otaXiaozhiPassiveRepository;
  @Resource
  private OtaRepository otaRepository;
  @Resource
  private ProductRepository productRepository;
  @Value("${ota.bin.url}")
  private String binUrl;

  @Override
  public List<OtaXiaozhiPassiveEntity> findAllById(int id) {
    return otaXiaozhiPassiveRepository.findAllById(id);
  }

  @Override
  public JsonResult<?> otaXiaozhiPassiveList(String token) {
    String token_deal = token.replace(JwtTokenUtil.TOKEN_PREFIX, "");
    String role = JwtTokenUtil.getUserRole(token_deal);
    String username = JwtTokenUtil.getUsername(token_deal);
    List<OtaXiaozhiPassiveEntity> result;
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
        List<OtaXiaozhiPassiveEntity> otaXiaozhiPassiveList =
            otaXiaozhiPassiveRepository.findAllByProductId(s.getProductId());
        result.addAll(otaXiaozhiPassiveList);
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
        List<OtaXiaozhiPassiveEntity> otaXiaozhiPassiveList =
            otaXiaozhiPassiveRepository.findAllByProductId(s.getProductId());
        result.addAll(otaXiaozhiPassiveList);
      }
    } else {
      result = otaXiaozhiPassiveRepository.findAll();
    }
    if (result.isEmpty()) {
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    }
    List<OtaXiaozhiPassiveListResponse> otaXiaozhiPassiveListResponses = new ArrayList<>();
    for (var s : result) {
      OtaXiaozhiPassiveListResponse otaXiaozhiPassiveListResponse =
          new OtaXiaozhiPassiveListResponse();
      otaXiaozhiPassiveListResponse.setId(s.getId());
      otaXiaozhiPassiveListResponse.setOtaId(s.getOtaId());
      otaXiaozhiPassiveListResponse.setProductId(s.getProductId());
      otaXiaozhiPassiveListResponse.setVersionName(s.getVersionName());
      otaXiaozhiPassiveListResponse
          .setOtaName(otaRepository.findAllById(s.getOtaId()).get(0).getName());
      otaXiaozhiPassiveListResponse
          .setProductName(productRepository.findAllById(s.getProductId()).get(0).getProductName());
      otaXiaozhiPassiveListResponses.add(otaXiaozhiPassiveListResponse);
    }
    return ResultTool.success(otaXiaozhiPassiveListResponses);
  }

  @Override
  public String otaXiaozhiPassiveEnable(int productId) throws IllegalArgumentException {
    List<OtaXiaozhiPassiveEntity> otaXiaozhiPassiveEntityList =
        otaXiaozhiPassiveRepository.findAllByProductId(productId);
    if (otaXiaozhiPassiveEntityList.isEmpty()) {
      throw new IllegalArgumentException("No OtaXiaozhiPassive found for productId: " + productId);
    }
    List<OtaEntity> otaEntityList =
        otaRepository.findAllById(otaXiaozhiPassiveEntityList.get(0).getOtaId());
    if (otaEntityList.isEmpty()) {
      throw new IllegalArgumentException(
          "No Ota found for id: " + otaXiaozhiPassiveEntityList.get(0).getOtaId());
    }
    String binName = otaEntityList.get(0).getPath();
    return this.binUrl + binName;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> otaXiaozhiPassivePost(OtaXiaozhiPassive otaXiaozhiPassive) {
    if (productRepository.findAllById(otaXiaozhiPassive.getProductId()).isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    List<OtaEntity> otaEntityList = otaRepository.findAllById(otaXiaozhiPassive.getOtaId());
    if (otaEntityList.isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    if (otaEntityList.get(0).getProductId() != otaXiaozhiPassive.getProductId()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    OtaXiaozhiPassiveEntity otaXiaozhiPassiveEntity = new OtaXiaozhiPassiveEntity();
    BeanUtils.copyProperties(otaXiaozhiPassive, otaXiaozhiPassiveEntity);
    var otaXiaozhiPassiveEntityList =
        otaXiaozhiPassiveRepository.findAllByProductId(otaXiaozhiPassive.getProductId());
    if (!otaXiaozhiPassiveEntityList.isEmpty()) {
      otaXiaozhiPassiveEntity.setId(otaXiaozhiPassiveEntityList.get(0).getId());
    }
    var result = otaXiaozhiPassiveRepository.save(otaXiaozhiPassiveEntity);
    return ResultTool.success(result);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> otaXiaozhiPassiveDelete(int id) {
    var otaXiaozhiPassiveEntityList = otaXiaozhiPassiveRepository.findAllById(id);
    if (otaXiaozhiPassiveEntityList.isEmpty()) {
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    } else {
      var result = otaXiaozhiPassiveRepository.deleteAllById(id);
      return ResultTool.success(result);
    }
  }
}
