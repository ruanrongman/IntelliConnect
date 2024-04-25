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
package top.rslly.iot.utility.ai.tools;

import com.alibaba.fastjson.JSONObject;
import com.zhipu.oapi.service.v4.model.ChatMessage;
import com.zhipu.oapi.service.v4.model.ChatMessageRole;
import lombok.Data;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import top.rslly.iot.param.request.ControlParam;
import top.rslly.iot.services.HardWareServiceImpl;
import top.rslly.iot.services.ProductDeviceServiceImpl;
import top.rslly.iot.services.ProductModelServiceImpl;
import top.rslly.iot.utility.ai.Glm;
import top.rslly.iot.utility.ai.IcAiException;
import top.rslly.iot.utility.ai.Prompt;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
public class ControlTool implements BaseTool<String> {
  @Autowired
  private Prompt prompt;
  @Autowired
  private ProductDeviceServiceImpl productDeviceService;
  @Autowired
  private ProductModelServiceImpl productModelService;
  @Autowired
  private HardWareServiceImpl hardWareService;

  private String name = "controlTool";
  private String description = """
      A tool for controlling and querying electrical status
      Args: Xx operation xx electrical(str)
      """;

  @Override
  public String run(String question) {
    return null;
  }

  @Override
  public String run(String question, int productId) {
    Glm glm = new Glm();
    List<ChatMessage> messages = new ArrayList<>();

    ChatMessage systemMessage =
        new ChatMessage(ChatMessageRole.SYSTEM.value(), prompt.getControlTool(productId));
    ChatMessage userMessage = new ChatMessage(ChatMessageRole.USER.value(), question);
    messages.add(systemMessage);
    messages.add(userMessage);
    var obj = glm.jsonChat(question, messages, false).getJSONObject("action");
    var answer = obj.getString("answer");
    try {
      this.process_llm_result(obj);
    } catch (Exception e) {
      return answer;
    }
    return answer;
  }

  private void process_llm_result(JSONObject jsonObject) throws MqttException, IcAiException {
    if (jsonObject.get("code").equals("200") || jsonObject.get("code").equals(200)) {
      var propertyJson = jsonObject.getJSONArray("properties");
      var valueJson = jsonObject.getJSONArray("value");
      String taskType = jsonObject.get("taskType").toString();
      if (taskType == null)
        throw new NullPointerException("taskType is null");
      List<String> properties = JSONObject.parseArray(propertyJson.toJSONString(), String.class);
      List<String> value = JSONObject.parseArray(valueJson.toJSONString(), String.class);
      var productModels = productModelService.findAllByName(jsonObject.get("name").toString());
      if (productModels.isEmpty())
        throw new IcAiException("platform not support");
      for (var s : productModels) {
        var deviceNames = productDeviceService.findAllByModelId(s.getId());
        if (deviceNames.isEmpty())
          throw new IcAiException("platform not support");
        for (var device : deviceNames) {
          if (!properties.isEmpty() && !value.isEmpty() && taskType.equals("control")) {
            ControlParam controlParam = new ControlParam(device.getName(), properties, value);
            var res = hardWareService.control(controlParam);
            if (res.getErrorCode() != 200)
              throw new IcAiException("platform not support");
          }
        }
      }
    } else
      throw new IcAiException("llm response error");
  }
}
