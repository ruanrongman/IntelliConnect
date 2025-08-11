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
package top.rslly.iot.services.thingsModel;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.rslly.iot.dao.*;
import top.rslly.iot.models.EventDataEntity;
import top.rslly.iot.models.ProductDeviceEntity;
import top.rslly.iot.models.ProductModelEntity;
import top.rslly.iot.param.request.EventData;
import top.rslly.iot.services.storage.EventStorageServiceImpl;
import top.rslly.iot.utility.JwtTokenUtil;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
public class EventDataServiceImpl implements EventDataService {
  @Resource
  private ProductModelRepository productModelRepository;
  @Resource
  private EventDataRepository eventDataRepository;
  @Resource
  private WxProductBindRepository wxProductBindRepository;
  @Resource
  private UserProductBindRepository userProductBindRepository;
  @Resource
  private WxUserRepository wxUserRepository;
  @Resource
  private ProductDeviceRepository productDeviceRepository;
  @Resource
  private UserRepository userRepository;
  @Autowired
  private EventStorageServiceImpl eventStorageService;

  @Override
  public List<EventDataEntity> findAllById(int id) {
    return eventDataRepository.findAllById(id);
  }

  @Override
  public List<EventDataEntity> findAllByModelId(int modelId) {
    return eventDataRepository.findAllByModelId(modelId);
  }

  @Override
  public JsonResult<?> getEventData(String token) {
    String token_deal = token.replace(JwtTokenUtil.TOKEN_PREFIX, "");
    String role = JwtTokenUtil.getUserRole(token_deal);
    String username = JwtTokenUtil.getUsername(token_deal);
    List<EventDataEntity> result;
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
        List<ProductModelEntity> productModelEntities =
            productModelRepository.findAllByProductId(s.getProductId());
        for (var s1 : productModelEntities) {
          List<EventDataEntity> eventDataEntityList =
              eventDataRepository.findAllByModelId(s1.getId());
          result.addAll(eventDataEntityList);
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
        List<ProductModelEntity> productModelEntities =
            productModelRepository.findAllByProductId(s.getProductId());
        for (var s1 : productModelEntities) {
          List<EventDataEntity> eventDataEntityList =
              eventDataRepository.findAllByModelId(s1.getId());
          result.addAll(eventDataEntityList);
        }
      }
    } else {
      result = eventDataRepository.findAll();
    }
    if (result.isEmpty()) {
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    } else
      return ResultTool.success(result);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> postEventData(EventData eventData) {
    EventDataEntity eventDataEntity = new EventDataEntity();
    BeanUtils.copyProperties(eventData, eventDataEntity);
    List<ProductModelEntity> result =
        productModelRepository.findAllById(eventDataEntity.getModelId());
    List<EventDataEntity> p1 = eventDataRepository
        .findAllByModelIdAndJsonKey(eventDataEntity.getModelId(), eventDataEntity.getJsonKey());
    // List<ProductDataEntity> p2 = productDataRepository.findAllByType(productData.getType());
    HashMap<String, Integer> typeMap = new HashMap<>();
    typeMap.put("string", 1);
    typeMap.put("int", 2);
    typeMap.put("float", 3);
    var p2 = typeMap.get(eventDataEntity.getType());
    if (result.isEmpty() || !p1.isEmpty() || p2 == null)
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    else {
      EventDataEntity eventDataEntity1 = eventDataRepository.save(eventDataEntity);
      return ResultTool.success(eventDataEntity1);
    }
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> deleteEventData(int id) {
    List<EventDataEntity> result = eventDataRepository.deleteById(id);
    if (result.isEmpty())
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    else {
      List<ProductDeviceEntity> productDeviceList =
          productDeviceRepository.findAllByModelId(result.get(0).getModelId());
      for (var s : productDeviceList) {
        eventStorageService.deleteAllByDeviceIdAndJsonKey(s.getId(), result.get(0).getJsonKey());
      }
      return ResultTool.success(result);
    }

  }
}
