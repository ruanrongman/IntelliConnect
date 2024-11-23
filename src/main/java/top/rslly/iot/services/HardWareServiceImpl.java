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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.rslly.iot.dao.ProductDataRepository;
import top.rslly.iot.dao.ProductDeviceRepository;
import top.rslly.iot.dao.ProductFunctionRepository;
import top.rslly.iot.models.ProductDataEntity;
import top.rslly.iot.models.ProductFunctionEntity;
import top.rslly.iot.param.request.ControlParam;
import top.rslly.iot.param.request.ProductFunction;
import top.rslly.iot.transfer.mqtt.MqttConnectionUtils;
import top.rslly.iot.utility.JsonCreate;
import top.rslly.iot.utility.RedisUtil;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class HardWareServiceImpl implements HardWareService {
  @Resource
  private ProductDataRepository productDataRepository;
  @Resource
  private ProductDeviceRepository productDeviceRepository;
  @Resource
  private ProductFunctionRepository productFunctionRepository;
  @Autowired
  private RedisUtil redisUtil;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> control(ControlParam controlParam) throws MqttException {
    var deviceEntityList = productDeviceRepository.findAllByName(controlParam.getName());
    if (deviceEntityList.isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    if (controlParam.getQos() < 0 || controlParam.getQos() > 2)
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    if (!controlParam.getMode().equals("service") && !controlParam.getMode().equals("attribute"))
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    int allow = deviceEntityList.get(0).getAllow();
    if (allow == 0) {
      return ResultTool.fail(ResultCode.DEVICE_ABANDON);
    }
    int modelId = deviceEntityList.get(0).getModelId();
    List<ProductDataEntity> productDataEntities = productDataRepository.findAllByModelId(modelId);
    List<ProductFunctionEntity> productFunctionEntities =
        productFunctionRepository.findAllByModelIdAndDataType(modelId, "input");
    List<String> productDataKey = new ArrayList<>();
    Map<String, String> productTypeMap = new HashMap<>();
    Map<String, String> dataMaxMap = new HashMap<>();
    Map<String, String> dataMinMap = new HashMap<>();
    List<String> productType = new ArrayList<>();
    List<String> controlKey = new ArrayList<>();
    List<String> controlValue = new ArrayList<>();
    List<String> dataMax = new ArrayList<>();
    List<String> dataMin = new ArrayList<>();
    if (controlParam.getMode().equals("attribute")) {
      if (productDataEntities.isEmpty()) {
        return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
      }
      for (var s : productDataEntities) {
        if (s.getrRw() == 1) {
          productDataKey.add(s.getJsonKey());
          productTypeMap.put(s.getJsonKey(), s.getType());
          if (s.getMax() != null || s.getMin() != null) {
            dataMaxMap.put(s.getJsonKey(), s.getMax());
            dataMinMap.put(s.getJsonKey(), s.getMin());
          }
        }
      }
    } else { // service
      if (productFunctionEntities.isEmpty()) {
        return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
      }
      if (!controlParam.getStatus().equals("sync") && !controlParam.getStatus().equals("async")) {
        return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
      }
      for (var s : productFunctionEntities) {
        if (s.getDataType().equals("input")) {
          productDataKey.add(s.getJsonKey());
          productTypeMap.put(s.getJsonKey(), s.getType());
          if (s.getMax() != null || s.getMin() != null) {
            dataMaxMap.put(s.getJsonKey(), s.getMax());
            dataMinMap.put(s.getJsonKey(), s.getMin());
          }
        }
      }
    }
    if (!productDataKey.containsAll(controlParam.getKey())
        || controlParam.getValue().size() != controlParam.getKey().size()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    for (int i = 0; i < controlParam.getKey().size(); i++) {
      if (!controlKey.contains(controlParam.getKey().get(i))) {
        controlKey.add(controlParam.getKey().get(i));
        controlValue.add(controlParam.getValue().get(i));
        productType.add(productTypeMap.get(controlParam.getKey().get(i)));
        dataMax.add(dataMaxMap.get(controlParam.getKey().get(i)));
        dataMin.add(dataMinMap.get(controlParam.getKey().get(i)));
      }
    }
    StringBuffer res = null;
    try {
      res = JsonCreate.create(controlKey, controlValue, productType, dataMax,
          dataMin);
    } catch (IOException | IllegalArgumentException e) {
      log.error(e.getMessage());
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    String prefix_topic;
    if (controlParam.getMode().equals("attribute"))
      prefix_topic = "properties/update";
    else
      prefix_topic = "services/invoke";
    MqttConnectionUtils.publish(
        "/oc/devices/" + controlParam.getName() + "/sys/" + prefix_topic, res.toString(),
        controlParam.getQos());
    if (controlParam.getStatus().equals("sync")) {
      for (int i = 0; i < 80; i++) {
        try {
          if (redisUtil.hasKey("service" + controlParam.getName())) {
            String res1 = (String) redisUtil.get("service" + controlParam.getName());
            redisUtil.del("service" + controlParam.getName());
            JSONObject output = JSONObject.parseObject(res1);
            JSONObject input = JSONObject.parseObject(res.toString());
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("input", input);
            jsonObject.put("output", output);
            return ResultTool.success(jsonObject.toJSONString());
          }
          Thread.sleep(100);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      return ResultTool.fail(ResultCode.DEVICE_TIMEOUT);
    }
    return ResultTool.success(res.toString());
  }
}
