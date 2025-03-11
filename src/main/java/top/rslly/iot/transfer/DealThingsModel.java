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
import top.rslly.iot.models.DataEntity;
import top.rslly.iot.services.storage.DataServiceImpl;
import top.rslly.iot.services.thingsModel.ProductDataServiceImpl;
import top.rslly.iot.services.thingsModel.ProductDeviceServiceImpl;
import top.rslly.iot.transfer.mqtt.MqttConnectionUtils;
import top.rslly.iot.utility.DataSave;
import top.rslly.iot.utility.RedisUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class DealThingsModel {
  @Autowired
  private ProductDeviceServiceImpl productDeviceService;
  @Autowired
  private ProductDataServiceImpl productDataService;
  @Autowired
  private DataServiceImpl dataService;
  @Autowired
  private RedisUtil redisUtil;

  // private final Lock lock = new ReentrantLock();
  public boolean deal(String clientId, String topic, String message) {
    // this clientId is delivered id ,not the sender
    log.info("clientId:{},topic:{},message:{}", clientId, topic, message);
    String characteristic = UUID.randomUUID().toString();
    // var deviceEntityList = productDeviceService.findAllBySubscribeTopic(topic);
    // if (deviceEntityList.isEmpty()||!clientId.equals(MqttConnectionUtils.clientId))
    // return;
    var deviceEntityList = productDeviceService.findAllByClientId(clientId);
    if (deviceEntityList.isEmpty() || !deviceEntityList.get(0).getSubscribeTopic().equals(topic))
      return false;
    int allow = deviceEntityList.get(0).getAllow();
    if (allow == 0) {
      return false;
    }
    int modelId = deviceEntityList.get(0).getModelId();
    var productDataEntities = productDataService.findAllByModelId(modelId);
    JSONObject mes;
    try {
      mes = JSON.parseObject(message);
    } catch (JSONException e) {
      log.error("json error{}", e.getMessage());
      return false;
    }
    String reply_topic = "/oc/devices/" + deviceEntityList.get(0).getName()
        + "/sys/" + "properties/report_reply";
    for (var s : productDataEntities) {
      var result = mes.get(s.getJsonKey());
      if (result == null || s.getStorageType().equals(DataSave.never.getStorageType()))
        continue;// Automatically ignore values
      DataEntity dataEntity = new DataEntity();
      dataEntity.setCharacteristic(characteristic);
      dataEntity.setJsonKey(s.getJsonKey());
      dataEntity.setValue(result.toString());
      dataEntity.setDeviceId(deviceEntityList.get(0).getId());
      try {
        long time = System.currentTimeMillis();
        dataEntity.setTime(time);
        var memory = redisUtil.get("property" + deviceEntityList.get(0).getId() + s.getJsonKey());
        if (memory != null) {
          List<DataEntity> dataEntityList = new ArrayList<>();
          dataEntityList.add(dataEntity);
          redisUtil.set("property" + deviceEntityList.get(0).getId() + s.getJsonKey(),
              dataEntityList, 120);
          // log.info("热点过期时间{}",
          // redisUtil.getExpire(deviceEntityList.get(0).getId()+s.getJsonKey()));
        }
        log.info("{}", dataEntity);
        dataService.insert(dataEntity);
      } catch (Exception e) {
        log.error("DealThingsModel save error:{}", e.getMessage());
      }
    }
    try {
      MqttConnectionUtils.publish(reply_topic, "{\"code\":\"" + 200 + "\",\"status\":\"ok\"}", 0);
    } catch (MqttException e) {
      log.error("DealThingsModel error:{}", e.getMessage());
    }
    return true;
  }
}
