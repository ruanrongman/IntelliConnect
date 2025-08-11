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
package top.rslly.iot.services.iot;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.rslly.iot.dao.*;
import top.rslly.iot.models.OtaEntity;
import top.rslly.iot.models.OtaPassiveEntity;
import top.rslly.iot.models.ProductDeviceEntity;
import top.rslly.iot.models.ProductModelEntity;
import top.rslly.iot.param.request.OtaPassive;
import top.rslly.iot.param.response.OtaPassiveEnableResponse;
import top.rslly.iot.utility.JwtTokenUtil;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import javax.annotation.Resource;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class OtaPassiveServiceImpl implements OtaPassiveService {
  @Resource
  private WxProductBindRepository wxProductBindRepository;
  @Resource
  private UserProductBindRepository userProductBindRepository;
  @Resource
  private WxUserRepository wxUserRepository;
  @Resource
  private UserRepository userRepository;
  @Resource
  private OtaRepository otaRepository;
  @Resource
  private OtaPassiveRepository otaPassiveRepository;
  @Resource
  private ProductModelRepository productModelRepository;
  @Resource
  private ProductDeviceRepository productDeviceRepository;
  @Value("${ota.bin.path}")
  private String binPath;

  @Override
  public List<OtaPassiveEntity> findAllById(int id) {
    return otaPassiveRepository.findAllById(id);
  }

  @Override
  public JsonResult<?> otaPassiveList(String token) {
    String token_deal = token.replace(JwtTokenUtil.TOKEN_PREFIX, "");
    String role = JwtTokenUtil.getUserRole(token_deal);
    String username = JwtTokenUtil.getUsername(token_deal);
    List<OtaPassiveEntity> result;
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
        List<OtaEntity> otaEntities = otaRepository.findAllByProductId(s.getProductId());
        for (var s1 : otaEntities) {
          List<OtaPassiveEntity> otaPassiveEntities =
              otaPassiveRepository.findAllByOtaId(s1.getId());
          result.addAll(otaPassiveEntities);
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
        List<OtaEntity> otaEntities = otaRepository.findAllByProductId(s.getProductId());
        for (var s1 : otaEntities) {
          List<OtaPassiveEntity> otaPassiveEntities =
              otaPassiveRepository.findAllByOtaId(s1.getId());
          result.addAll(otaPassiveEntities);
        }
      }
    } else {
      result = otaPassiveRepository.findAll();
    }
    if (result.isEmpty()) {
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    } else
      return ResultTool.success(result);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> otaPassiveEnable(String deviceName) {
    List<ProductDeviceEntity> productDeviceEntityList =
        productDeviceRepository.findAllByName(deviceName);
    if (productDeviceEntityList.isEmpty())
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    List<OtaPassiveEntity> otaPassiveEntityList =
        otaPassiveRepository.findAllByDeviceId(productDeviceEntityList.get(0).getId());
    if (otaPassiveEntityList.isEmpty())
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    var otaList = otaRepository.findAllById(otaPassiveEntityList.get(0).getOtaId());
    if (otaList.isEmpty())
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    OtaPassiveEnableResponse otaPassiveEnableResponse = new OtaPassiveEnableResponse();
    // Map<String, String> map = new HashMap<>();
    otaPassiveEnableResponse.setFileName(otaList.get(0).getPath());
    otaPassiveEnableResponse.setVersion(otaPassiveEntityList.get(0).getVersionName());
    try {
      otaPassiveEnableResponse
          .setMd5(DigestUtils.md5Hex(new FileInputStream(binPath + otaList.get(0).getPath())));
    } catch (IOException e) {
      log.error(e.getMessage());
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    }
    return ResultTool.success(otaPassiveEnableResponse);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> otaPassivePost(OtaPassive otaPassive) {
    var productDeviceEntityList = productDeviceRepository.findAllByName(otaPassive.getDeviceName());
    if (productDeviceEntityList.isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    var otaList = otaRepository.findAllByName(otaPassive.getName());
    if (otaList.isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    int modelId = productDeviceEntityList.get(0).getModelId();
    List<ProductModelEntity> productModels = productModelRepository.findAllById(modelId);
    if (productModels.isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    int deviceProductId = productModels.get(0).getProductId();
    int otaProductId = otaList.get(0).getProductId();
    if (deviceProductId != otaProductId) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    OtaPassiveEntity otaPassiveEntity = new OtaPassiveEntity();
    if (!otaPassiveRepository.findAllByDeviceId(productDeviceEntityList.get(0).getId()).isEmpty()) {
      otaPassiveEntity.setId(otaPassiveRepository
          .findAllByDeviceId(productDeviceEntityList.get(0).getId()).get(0).getId());
    }
    otaPassiveEntity.setDeviceId(productDeviceEntityList.get(0).getId());
    otaPassiveEntity.setOtaId(otaList.get(0).getId());
    otaPassiveEntity.setVersionName(otaPassive.getVersionName());
    var result = otaPassiveRepository.save(otaPassiveEntity);
    return ResultTool.success(result);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> otaPassiveDelete(int id) {
    var otaPassiveEntityList = otaPassiveRepository.findAllById(id);
    if (otaPassiveEntityList.isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return ResultTool.success(otaPassiveRepository.deleteAllById(id));
  }
}
