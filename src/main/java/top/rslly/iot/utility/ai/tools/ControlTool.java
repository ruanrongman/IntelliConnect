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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.rslly.iot.models.ProductModelEntity;
import top.rslly.iot.param.request.ControlParam;
import top.rslly.iot.services.HardWareServiceImpl;
import top.rslly.iot.services.ProductDeviceServiceImpl;
import top.rslly.iot.services.ProductModelServiceImpl;
import top.rslly.iot.services.ProductServiceImpl;
import top.rslly.iot.utility.ai.IcAiException;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.ModelMessageRole;
import top.rslly.iot.utility.ai.Prompt;
import top.rslly.iot.utility.ai.llm.LLM;
import top.rslly.iot.utility.ai.llm.LLMFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@Component
@Slf4j
public class ControlTool implements BaseTool<String> {
  @Autowired
  private Prompt prompt;
  @Autowired
  private ProductDeviceServiceImpl productDeviceService;
  @Autowired
  private ProductModelServiceImpl productModelService;
  @Autowired
  private ProductServiceImpl productService;
  @Autowired
  private HardWareServiceImpl hardWareService;

  @Value("${ai.controlTool-llm}")
  private String llmName;
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
  public String run(String question, Map<String, Object> globalMessage) {
    int productId = (int) globalMessage.get("productId");

    if (productService.findAllById(productId).isEmpty())
      return "产品设置错误，请检查相关设置！";
    if (productModelService.findAllByProductId(productId).isEmpty())
      return "产品下面没有任何设备，请先绑定设备";
    LLM llm = LLMFactory.getLLM(llmName);
    List<ModelMessage> messages = new ArrayList<>();

    ModelMessage systemMessage =
        new ModelMessage(ModelMessageRole.SYSTEM.value(), prompt.getControlTool(productId));
    // log.info("systemMessage: " + systemMessage.getContent());
    ModelMessage userMessage = new ModelMessage(ModelMessageRole.USER.value(), question);
    messages.add(systemMessage);
    messages.add(userMessage);
    var obj = llm.jsonChat(question, messages, false).getJSONObject("action");
    String answer = obj.getString("answer");
    JSONArray controlParameters = obj.getJSONArray("controlParameters");
    for (Object controlParameter : controlParameters) {
      process_llm_result(productId, (JSONObject) controlParameter);
    }
    return answer;
  }

  private void process_llm_result(int productId, JSONObject jsonObject) {
    try {
      if (jsonObject.get("code").equals("200") || jsonObject.get("code").equals(200)) {
        JSONArray propertyJson = jsonObject.getJSONArray("properties");
        JSONArray valueJson = jsonObject.getJSONArray("value");
        String taskType = jsonObject.get("taskType").toString();
        if (taskType == null)
          throw new NullPointerException("taskType is null");
        List<String> properties = JSONObject.parseArray(propertyJson.toJSONString(), String.class);
        List<String> value = JSONObject.parseArray(valueJson.toJSONString(), String.class);
        String deviceName = jsonObject.get("name").toString();
        List<Integer> modelList = new ArrayList<>();
        var productModelEntityList = productModelService.findAllByProductId(productId);
        for (var s : productModelEntityList) {
          modelList.add(s.getId());
        }
        if (modelList
            .contains(productDeviceService.findAllByName(deviceName).get(0).getModelId())) {
          if (!properties.isEmpty() && !value.isEmpty() && taskType.equals("control")) {
            ControlParam controlParam = new ControlParam(deviceName, 1, properties, value);
            var res = hardWareService.control(controlParam);
            if (res.getErrorCode() != 200)
              throw new IcAiException("platform not support");
          }
        } else
          throw new IcAiException("the deviceName is not in this product");

      } else
        throw new IcAiException("llm response error");
    } catch (Exception e) {
      log.error("control tools error:{}", e.getMessage());
    }
  }
}
