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

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.rslly.iot.dao.*;
import top.rslly.iot.models.*;
import top.rslly.iot.param.request.AlarmEvent;
import top.rslly.iot.utility.JwtTokenUtil;
import top.rslly.iot.utility.SendEmail;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;
import top.rslly.iot.utility.wx.DealWx;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AlarmEventServiceImpl implements AlarmEventService {
  @Resource
  private WxUserRepository wxUserRepository;
  @Resource
  private WxProductBindRepository wxProductBindRepository;
  @Resource
  private UserProductBindRepository userProductBindRepository;
  @Resource
  private ProductModelRepository productModelRepository;
  @Resource
  private ProductEventRepository productEventRepository;
  @Resource
  private AlarmEventRepository alarmEventRepository;
  @Resource
  private UserRepository userRepository;
  @Autowired
  private SendEmail sendEmail;
  @Autowired
  private DealWx dealWx;

  @Value("${wx.alarmTemplateId}")
  private String templateId;
  @Value("${wx.micro.appid}")
  private String microappid;
  @Value("${wx.micro.appid2}")
  private String microappid2;

  @Override
  public List<AlarmEventEntity> findAllById(int id) {
    return alarmEventRepository.findAllById(id);
  }

  @Override
  public JsonResult<?> getAlarmEvent(String token) {
    String token_deal = token.replace(JwtTokenUtil.TOKEN_PREFIX, "");
    String role = JwtTokenUtil.getUserRole(token_deal);
    String username = JwtTokenUtil.getUsername(token_deal);
    List<AlarmEventEntity> result;
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
        List<ProductModelEntity> productModelEntities =
            productModelRepository.findAllByProductId(s.getProductId());
        for (var s1 : productModelEntities) {
          List<ProductEventEntity> productEventEntityList =
              productEventRepository.findAllByModelId(s1.getId());
          for (var s2 : productEventEntityList) {
            List<AlarmEventEntity> alarmEventEntityList =
                alarmEventRepository.findAllByEventId(s2.getId());
            result.addAll(alarmEventEntityList);
          }
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
          List<ProductEventEntity> productEventEntityList =
              productEventRepository.findAllByModelId(s1.getId());
          for (var s2 : productEventEntityList) {
            List<AlarmEventEntity> alarmEventEntityList =
                alarmEventRepository.findAllByEventId(s2.getId());
            result.addAll(alarmEventEntityList);
          }
        }
      }
    } else {
      result = alarmEventRepository.findAll();
    }
    if (result.isEmpty()) {
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    } else
      return ResultTool.success(result);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> postAlarmEvent(AlarmEvent alarmEvent) {
    AlarmEventEntity alarmEventEntity = new AlarmEventEntity();
    List<ProductEventEntity> productEventEntityList =
        productEventRepository.findAllByModelIdAndName(alarmEvent.getModelId(),
            alarmEvent.getName());
    List<AlarmEventEntity> alarmEventEntityList =
        alarmEventRepository.findAllByEventId(productEventEntityList.get(0).getId());
    if (productEventEntityList.isEmpty() || !alarmEventEntityList.isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    } else {
      alarmEventEntity.setEventId(productEventEntityList.get(0).getId());
      AlarmEventEntity alarmEventEntity1 = alarmEventRepository.save(alarmEventEntity);
      return ResultTool.success(alarmEventEntity1);
    }
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> deleteAlarmEvent(int id) {
    List<AlarmEventEntity> result = alarmEventRepository.deleteById(id);
    if (result.isEmpty())
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    else {
      return ResultTool.success(result);
    }

  }

  @Override
  public void alarmEvent(String deviceName, int eventId) {
    List<AlarmEventEntity> alarmEventEntityList = alarmEventRepository.findAllByEventId(eventId);
    if (alarmEventEntityList.isEmpty())
      return;
    List<ProductEventEntity> productEventEntityList =
        productEventRepository.findAllById(eventId);
    if (productEventEntityList.isEmpty())
      return;
    List<ProductModelEntity> productModelEntityList =
        productModelRepository.findAllById(productEventEntityList.get(0).getModelId());
    if (productModelEntityList.isEmpty())
      return;
    List<UserProductBindEntity> userProductBindEntityList =
        userProductBindRepository.findAllByProductId(productModelEntityList.get(0).getProductId());
    List<WxProductBindEntity> wxProductBindEntityList =
        wxProductBindRepository.findAllByProductId(productModelEntityList.get(0).getProductId());
    for (var rootUser : userRepository.findAllByRole("admin")) {
      alarmEmailSend(deviceName, productEventEntityList.get(0).getName(), rootUser);
    }
    for (var s1 : userProductBindEntityList) {
      for (var s2 : userRepository.findAllById(s1.getUserId())) {
        alarmEmailSend(deviceName, productEventEntityList.get(0).getName(), s2);
      }
    }
    for (var wxProductBindEntity : wxProductBindEntityList) {
      try {
        if (wxProductBindEntity.getAppid().equals(microappid)
            || wxProductBindEntity.getAppid().equals(microappid2)) {
          dealWx.sendContent(wxProductBindEntity.getOpenid(),
              "设备：" + deviceName + "发生事件：" + productEventEntityList.get(0).getName(),
              wxProductBindEntity.getAppid());
        } else {
          // String reqdata = "{\"key\":{\"value\":\"" + key + "\", \"color\":\"#0000CD\"},
          // \"value\":{\"value\":\"" + value + "\"}}";
          // 优化 reqbody, 不用拼接
          JSONObject jsonObject = new JSONObject();
          JSONObject contentObject = new JSONObject();
          JSONObject eventObject = new JSONObject();
          contentObject.put("value", deviceName);
          contentObject.put("color", "#0000CD");
          jsonObject.put("thing23", contentObject);
          eventObject.put("value", productEventEntityList.get(0).getName());
          jsonObject.put("thing8", eventObject);
          dealWx.templatePost(jsonObject, templateId, wxProductBindEntity.getOpenid());
        }
      } catch (IOException e) {
        log.error("小程序发送失败{}", e.getMessage());
      }
    }

  }

  private void alarmEmailSend(String deviceName, String eventName, UserEntity userEntity) {
    Map<String, Object> map = new HashMap<>();
    map.put("deviceName", deviceName);
    map.put("event", eventName);
    map.put("username", userEntity.getUsername());
    sendEmail.contextLoads(new String[] {userEntity.getEmail()}, "事件告警", "alarm", map);
  }
}
