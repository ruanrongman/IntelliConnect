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
package top.rslly.iot.utility.ai.toolAgent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.zhipu.oapi.service.v4.model.ChatMessage;
import com.zhipu.oapi.service.v4.model.ChatMessageRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.rslly.iot.utility.ai.DescriptionUtil;
import top.rslly.iot.utility.ai.Glm;
import top.rslly.iot.utility.ai.Prompt;
import top.rslly.iot.utility.ai.tools.Manage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class Agent {
  @Autowired
  private Manage manage;
  @Autowired
  private Prompt prompt;
  @Autowired
  private DescriptionUtil descriptionUtil;
  private final StringBuffer conversationPrompt = new StringBuffer();

  public String run(String question, int productId) {
    Glm glm = new Glm();
    String system = prompt.getReact(descriptionUtil.getTools(), question);
    List<ChatMessage> messages = new ArrayList<>();
    String toolResult = "";
    conversationPrompt.append(system);
    int iteration = 0;
    while (iteration < 8) {
      messages.clear();
      ChatMessage systemMessage =
          new ChatMessage(ChatMessageRole.SYSTEM.value(), conversationPrompt);
      ChatMessage userMessage = new ChatMessage(ChatMessageRole.USER.value(), question);
      messages.add(systemMessage);
      messages.add(userMessage);
      var obj = glm.jsonChat(question, messages, false);
      var answer = (String) obj.getJSONObject("action").get("answer");
      try {
        Map<String, String> res = process_llm_result(obj);
        // System.out.println(res);
        if (res.get("action_name").equals("finish")) {
          String args = res.get("action_parameters");
          return JSON.parseObject(args).getString("content");
        }
        toolResult =
            manage.runTool(res.get("action_name"), res.get("action_parameters"), productId);
        conversationPrompt.append(obj);
        conversationPrompt.append(String.format("Observation: %s\n", toolResult));
        log.info("Thought{}", res.get("thought"));
        log.info("Observation:{}", toolResult);
      } catch (Exception e) {
        return answer;
      }
      iteration += 1;
    }
    return toolResult;
  }

  private Map<String, String> process_llm_result(JSONObject obj) {
    Map<String, String> resultMap = new HashMap<>();
    resultMap.put("thought", obj.getString("thought"));
    resultMap.put("action_name", obj.getJSONObject("action").getString("name"));
    resultMap.put("action_parameters", obj.getJSONObject("action").getString("args"));
    return resultMap;
  }

}
