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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.rslly.iot.utility.HttpRequestUtils;
import top.rslly.iot.utility.ai.Glm;
import top.rslly.iot.utility.ai.IcAiException;
import top.rslly.iot.utility.ai.Prompt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Component
public class ClassifierTool {
  @Autowired
  private Prompt prompt;
  @Autowired
  private HttpRequestUtils httpRequestUtils;
  private String name = "ClassifierTool";
  private String description = """
      This tool is used to classify users' intentions
      Args: user question(str)
      """;

  public Map<String, Object> run(String question, List<ChatMessage> memory) {
    Glm glm = new Glm();
    List<ChatMessage> messages = new ArrayList<>();
    Map<String, Object> resultMap = new HashMap<>();

    ChatMessage systemMessage =
        new ChatMessage(ChatMessageRole.SYSTEM.value(), prompt.getClassifierTool());
    ChatMessage userMessage = new ChatMessage(ChatMessageRole.USER.value(), question);
    messages.add(systemMessage);
    if (!memory.isEmpty())
      messages.addAll(memory);
    messages.add(userMessage);
    var obj = glm.jsonChat(question, messages, true).getJSONObject("action");
    var answer = (String) obj.get("answer");
    try {
      var value = process_llm_value(obj);
      resultMap.put("value", value.get("value"));
      resultMap.put("args", value.get("args"));
      resultMap.put("answer", answer);

    } catch (Exception e) {
      resultMap.put("answer", answer);
      return resultMap;
    }
    return resultMap;
  }

  private Map<String, Object> process_llm_value(JSONObject jsonObject) throws IcAiException {
    Map<String, Object> resultMap = new HashMap<>();
    if (jsonObject.get("code").equals("200") || jsonObject.get("code").equals(200)) {
      var valueJson = jsonObject.getJSONArray("value");
      var argsJson = jsonObject.getString("args");
      resultMap.put("value", JSONObject.parseArray(valueJson.toJSONString(), String.class));
      resultMap.put("args", argsJson);
      return resultMap;
    } else
      throw new IcAiException("llm response error");
  }
}
