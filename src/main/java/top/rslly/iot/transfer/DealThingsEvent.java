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
import top.rslly.iot.models.EventStorageEntity;
import top.rslly.iot.services.EventDataServiceImpl;
import top.rslly.iot.services.EventStorageServiceImpl;
import top.rslly.iot.services.ProductDeviceServiceImpl;
import top.rslly.iot.services.ProductEventServiceImpl;
import top.rslly.iot.transfer.mqtt.MqttConnectionUtils;

import java.util.UUID;

@Component
@Slf4j
public class DealThingsEvent {
  @Autowired
  private ProductDeviceServiceImpl productDeviceService;
  @Autowired
  private ProductEventServiceImpl productEventService;
  @Autowired
  private EventDataServiceImpl eventDataService;
  @Autowired
  private EventStorageServiceImpl eventStorageService;

  public boolean deal(String clientId, String topic, String message) {
    // this clientId is delivered id ,not the sender
    log.info("clientId:{},topic:{},message:{}", clientId, topic, message);
    String characteristic = UUID.randomUUID().toString();
    // var deviceEntityList = productDeviceService.findAllBySubscribeTopic(topic);
    // if (deviceEntityList.isEmpty()||!clientId.equals(MqttConnectionUtils.clientId))
    // return;
    var deviceEntityList = productDeviceService.findAllByClientId(clientId);
    if (deviceEntityList.isEmpty())
      return false;
    String event_topic = "/oc/devices/" + deviceEntityList.get(0).getName()
        + "/sys/" + "properties/event";
    if (!event_topic.equals(topic))
      return false;
    int allow = deviceEntityList.get(0).getAllow();
    if (allow == 0) {
      return false;
    }
    int modelId = deviceEntityList.get(0).getModelId();
    var eventDataEntities = eventDataService.findAllByModelId(modelId);
    JSONObject mes;
    int event_id;
    try {
      mes = JSON.parseObject(message);
      var eventEntities =
          productEventService.findAllByModelIdAndName(modelId, mes.get("event").toString());
      event_id = eventEntities.get(0).getId();// event_id is the id of event
    } catch (Exception e) {
      log.error("json error{}", e.getMessage());
      return false;
    }
    String reply_topic = "/oc/devices/" + deviceEntityList.get(0).getName()
        + "/sys/" + "properties/event_reply";
    for (var s : eventDataEntities) {
      var result = mes.get(s.getJsonKey());
      if (result == null)
        continue;// Automatically ignore values
      EventStorageEntity eventStorageEntity = new EventStorageEntity();
      eventStorageEntity.setCharacteristic(characteristic);
      eventStorageEntity.setEventId(event_id);
      eventStorageEntity.setJsonKey(s.getJsonKey());
      eventStorageEntity.setValue(result.toString());
      eventStorageEntity.setDeviceId(deviceEntityList.get(0).getId());
      try {
        long time = System.currentTimeMillis();
        eventStorageEntity.setTime(time);
        log.info("{}", eventStorageEntity);
        eventStorageService.insert(eventStorageEntity);
      } catch (Exception e) {
        log.error("DealThingsEvent save error:{}", e.getMessage());
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
