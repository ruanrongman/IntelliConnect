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
package top.rslly.iot.utility.ai;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.rslly.iot.services.ProductDataServiceImpl;
import top.rslly.iot.services.ProductDeviceServiceImpl;
import top.rslly.iot.services.ProductModelServiceImpl;
import top.rslly.iot.utility.ai.tools.ControlTool;
import top.rslly.iot.utility.ai.tools.MusicTool;
import top.rslly.iot.utility.ai.tools.WeatherTool;

@Component
public class DescriptionUtil {
  @Autowired
  private ProductModelServiceImpl productModelService;
  @Autowired
  private ProductDataServiceImpl productDataService;
  @Autowired
  private ProductDeviceServiceImpl productDeviceService;


  public String getElectricalName(int productId) {
    var result = productModelService.getDescription(productId);
    return JSON.toJSONString(result);
  }

  public String getPropertiesAndValue(int productId) {
    JSONObject jsonObject = new JSONObject();
    var result = productModelService.findAllByProductId(productId);
    for (var s : result) {
      var dataList = productDataService.getDescription(s.getId());
      jsonObject.put(s.getName(), dataList);
    }
    return jsonObject.toJSONString();
  }

  public String getCurrentValue(int productId) {
    JSONObject jsonObject = new JSONObject();
    var result = productModelService.findAllByProductId(productId);
    for (var s : result) {
      var dataList = productDeviceService.getDescription(s.getId());
      if (!dataList.isEmpty())
        jsonObject.put(s.getName(), dataList);
    }
    return jsonObject.toJSONString();
  }

  public String getTools() {
    ControlTool controlTool = new ControlTool();
    WeatherTool weatherTool = new WeatherTool();
    MusicTool musicTool = new MusicTool();
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(controlTool.getName(), controlTool.getDescription());
    jsonObject.put(weatherTool.getName(), weatherTool.getDescription());
    jsonObject.put(musicTool.getName(), musicTool.getDescription());
    return jsonObject.toJSONString();
  }
}
