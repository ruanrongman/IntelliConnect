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
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.rslly.iot.utility.ai.IcAiException;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.ModelMessageRole;
import top.rslly.iot.utility.ai.Prompt;
import top.rslly.iot.utility.ai.llm.LLM;
import top.rslly.iot.utility.ai.llm.LLMFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Component
public class WxProductActiveTool {
  @Autowired
  private Prompt prompt;
  @Value("${ai.wxProductActiveTool-llm}")
  private String llmName;
  private String name = "wxProductActiveTool";
  private String description = """
      A tool for wechat to set the agent enable control the product.
      Args: product name .(str)
      """;

  public Map<String, String> run(String question) {
    LLM llm = LLMFactory.getLLM(llmName);
    List<ModelMessage> messages = new ArrayList<>();
    Map<String, String> responseMap = new HashMap<>();
    ModelMessage systemMessage =
        new ModelMessage(ModelMessageRole.SYSTEM.value(), prompt.getWxProductActiveTool());
    ModelMessage userMessage = new ModelMessage(ModelMessageRole.USER.value(), question);
    messages.add(systemMessage);
    messages.add(userMessage);
    var obj = llm.jsonChat(question, messages, true).getJSONObject("action");
    var answer = (String) obj.get("answer");
    responseMap.put("answer", answer);
    responseMap.put("productName", "");
    try {
      var productMetaData = process_llm_result(obj);
      responseMap.put("productName", productMetaData.get("productName"));
      return responseMap;
    } catch (Exception e) {
      // e.printStackTrace();
      return responseMap;
    }
  }

  private Map<String, String> process_llm_result(JSONObject jsonObject) throws IcAiException {
    Map<String, String> result = new HashMap<>();
    if (jsonObject.get("code").equals("200") || jsonObject.get("code").equals(200)) {
      var productName = jsonObject.getString("productName");
      result.put("productName", productName);
      return result;
    } else {
      throw new IcAiException(jsonObject.getString("platform not support"));
    }
  }
}
