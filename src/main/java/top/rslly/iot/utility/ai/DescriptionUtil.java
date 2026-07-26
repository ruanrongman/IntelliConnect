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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.rslly.iot.param.prompt.AgentLongMemoryDescription;
import top.rslly.iot.services.agent.AgentLongMemoryServiceImpl;
import top.rslly.iot.services.agent.ProductRoleServiceImpl;
import top.rslly.iot.services.agent.TimeScheduleServiceImpl;
import top.rslly.iot.services.thingsModel.ProductDataServiceImpl;
import top.rslly.iot.services.thingsModel.ProductDeviceServiceImpl;
import top.rslly.iot.services.thingsModel.ProductFunctionServiceImpl;
import top.rslly.iot.services.thingsModel.ProductModelServiceImpl;
import top.rslly.iot.utility.ai.mcp.McpAgent;
import top.rslly.iot.utility.ai.tools.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DescriptionUtil {
  @Autowired
  private ProductModelServiceImpl productModelService;
  @Autowired
  private ProductDataServiceImpl productDataService;
  @Autowired
  private ProductDeviceServiceImpl productDeviceService;
  @Autowired
  private ProductFunctionServiceImpl productFunctionService;
  @Autowired
  private ProductRoleServiceImpl productRoleService;
  @Autowired
  private TimeScheduleServiceImpl timeScheduleService;
  @Autowired
  private AgentLongMemoryServiceImpl agentLongMemoryService;
  @Autowired
  private McpAgent mcpAgent;
  @Autowired
  private KnowledgeTool knowledgeTool;

  public String getElectricalName(int productId) {
    var result = productModelService.getDescription(productId);
    return JSON.toJSONString(result);
  }

  public String getAgentLongMemory(int productId) {
    var result = agentLongMemoryService.getDescription(productId);
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

  public String getThingsFunction(int productId) {
    JSONObject jsonObject = new JSONObject();
    var result = productModelService.findAllByProductId(productId);
    for (var s : result) {
      var dataList = productFunctionService.getDescription(s.getId());
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

  public String getProductRole(int productId) {
    JSONObject jsonObject = new JSONObject();
    var result = productRoleService.getDescription(productId);
    if (!result.isEmpty())
      jsonObject.put("productRole", result);
    return jsonObject.toJSONString();
  }

  public String getSchedule(String appid, String openid) {
    JSONObject jsonObject = new JSONObject();
    var result = timeScheduleService.findAllByAppidAndOpenid(appid, openid);
    for (var s : result) {
      jsonObject.put(s.getTaskName(), s.getCron());
    }
    return jsonObject.toJSONString();
  }

  public String getTools(int productId, String chatId) {
    return JSON.toJSONString(getToolDescriptions(productId, chatId));
  }

  public Map<String, String> getToolDescriptions(int productId, String chatId) {
    ControlTool controlTool = new ControlTool();
    WeatherTool weatherTool = new WeatherTool();
    MusicTool musicTool = new MusicTool();
    Map<String, String> toolDescriptions = new LinkedHashMap<>();
    toolDescriptions.put(controlTool.getName(), controlTool.getDescription());
    toolDescriptions.put(weatherTool.getName(), weatherTool.getDescription());
    toolDescriptions.put(musicTool.getName(), musicTool.getDescription());
    String knowledgeDescription = knowledgeTool.getDescription(productId);
    if (!knowledgeDescription.isBlank()) {
      toolDescriptions.put(knowledgeTool.getName(), knowledgeDescription);
    }
    toolDescriptions.put(mcpAgent.getName(), mcpAgent.getDescription(productId, chatId));
    return toolDescriptions;
  }
}
