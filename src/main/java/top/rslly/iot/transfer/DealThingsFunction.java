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
package top.rslly.iot.transfer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.rslly.iot.models.ProductFunctionEntity;
import top.rslly.iot.services.ProductDeviceServiceImpl;
import top.rslly.iot.services.ProductFunctionServiceImpl;
import top.rslly.iot.transfer.mqtt.MqttConnectionUtils;
import top.rslly.iot.utility.RedisUtil;

import java.util.List;

@Component
@Slf4j
public class DealThingsFunction {

  @Autowired
  private ProductDeviceServiceImpl productDeviceService;
  @Autowired
  private ProductFunctionServiceImpl productFunctionService;
  @Autowired
  private RedisUtil redisUtil;

  // private final Lock lock = new ReentrantLock();
  public boolean deal(String clientId, String topic, String message) {
    // this clientId is delivered id ,not the sender
    log.info("clientId:{},topic:{},message:{}", clientId, topic, message);
    var deviceEntityList = productDeviceService.findAllByClientId(clientId);
    if (deviceEntityList.isEmpty())
      return false;
    String deviceName = deviceEntityList.get(0).getName();
    String function_topic = "/oc/devices/" + deviceName
        + "/sys/" + "services/report";
    if (!function_topic.equals(topic))
      return false;
    int modelId = deviceEntityList.get(0).getModelId();
    List<ProductFunctionEntity> functionReplyEntities;
    JSONObject mes;
    try {
      mes = JSON.parseObject(message);
      functionReplyEntities =
          productFunctionService.findAllByModelIdAndFunctionNameAndDataType(modelId,
              mes.get("functionName").toString(), "output");
    } catch (JSONException e) {
      log.error("json error{}", e.getMessage());
      return false;
    }
    String reply_topic = "/oc/devices/" + deviceName
        + "/sys/" + "services/report_reply";
    String async_outputData_topic = "/oc/devices/" + deviceName
        + "/sys/" + "services/async_outputData";
    JSONObject outputDataObject = new JSONObject();
    outputDataObject.put("functionName", mes.get("functionName"));
    for (var s : functionReplyEntities) {
      var result = mes.get(s.getJsonKey());
      if (result == null)
        continue;// Automatically ignore values
      try {
        outputDataObject.put(s.getJsonKey(), result);
        // redisUtil.set("service"+deviceEntityList.get(0).getId() + s.getJsonKey(), result, 120);
        // log.info("热点过期时间{}",
        // redisUtil.getExpire(deviceEntityList.get(0).getId()+s.getJsonKey()));
        log.info("key:{},value:{}", s.getJsonKey(), result);
      } catch (Exception e) {
        log.error("DealThingsModel save error:{}", e.getMessage());
      }
    }
    redisUtil.set("service" + deviceName, outputDataObject.toJSONString(), 120);
    try {
      MqttConnectionUtils.publish(reply_topic, "{\"code\":\"" + 200 + "\",\"status\":\"ok\"}", 0);
      MqttConnectionUtils.publish(async_outputData_topic, outputDataObject.toJSONString(), 0);
    } catch (MqttException e) {
      log.error("DealThingsModel error:{}", e.getMessage());
    }
    return true;
  }
}
