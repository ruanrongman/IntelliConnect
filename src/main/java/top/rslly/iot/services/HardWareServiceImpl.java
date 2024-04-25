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

import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.stereotype.Service;
import top.rslly.iot.dao.ProductDataRepository;
import top.rslly.iot.dao.ProductDeviceRepository;
import top.rslly.iot.param.request.ControlParam;
import top.rslly.iot.transfer.mqtt.MqttConnectionUtils;
import top.rslly.iot.utility.JsonCreate;
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
public class HardWareServiceImpl implements HardWareService {
  @Resource
  private ProductDataRepository productDataRepository;
  @Resource
  private ProductDeviceRepository productDeviceRepository;

  @Override
  public JsonResult<?> control(ControlParam controlParam) throws MqttException {
    var deviceEntityList = productDeviceRepository.findAllByName(controlParam.getName());
    if (deviceEntityList.isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    int modelId = deviceEntityList.get(0).getModelId();
    var productDataEntities = productDataRepository.findAllByModelId(modelId);
    if (productDataEntities.isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    List<String> productDataKey = new ArrayList<>();
    Map<String, String> productTypeMap = new HashMap<>();
    List<String> productType = new ArrayList<>();
    for (var s : productDataEntities) {
      if (s.getrRw() == 1) {
        productDataKey.add(s.getJsonKey());
        productTypeMap.put(s.getJsonKey(), s.getType());
      }
    }
    if (!productDataKey.containsAll(controlParam.getKey())) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    for (var s : controlParam.getKey()) {
      productType.add(productTypeMap.get(s));
    }
    StringBuffer res = null;
    try {
      res = JsonCreate.create(controlParam.getKey(), controlParam.getValue(), productType);
    } catch (IOException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    MqttConnectionUtils.publish(
        "/oc/devices/" + controlParam.getName() + "/sys/" + "properties/update", res.toString(), 0);
    return ResultTool.success();
  }
}
