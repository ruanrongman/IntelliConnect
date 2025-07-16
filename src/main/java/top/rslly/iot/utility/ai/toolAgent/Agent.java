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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.rslly.iot.utility.ai.DescriptionUtil;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.ModelMessageRole;
import top.rslly.iot.utility.ai.Prompt;
import top.rslly.iot.utility.ai.llm.LLM;
import top.rslly.iot.utility.ai.llm.LLMFactory;
import top.rslly.iot.utility.ai.Manage;
import top.rslly.iot.utility.ai.prompts.ReactPrompt;

import java.util.*;

@Component
@Slf4j
public class Agent {
  @Autowired
  private Manage manage;
  @Value("${ai.agent-llm}")
  private String llmName;
  @Autowired
  private ReactPrompt reactPrompt;
  @Autowired
  private DescriptionUtil descriptionUtil;
  @Value("${ai.agent-epoch-limit}")
  private int epochLimit = 8;

  public String run(String question, Map<String, Object> globalMessage) {
    LLM llm = LLMFactory.getLLM(llmName);
    StringBuilder conversationPrompt = new StringBuilder();
    int productId = (int) globalMessage.get("productId");
    Map<String, Queue<String>> queueMap =
        (Map<String, Queue<String>>) globalMessage.get("queueMap");
    String chatId = (String) globalMessage.get("chatId");
    globalMessage.put("mcpIsTool", true);
    var queue = queueMap.get(chatId);
    if (queue != null) {
      queue.add("以下是智能体处理结果：");
    }
    String system = reactPrompt.getReact(descriptionUtil.getTools(productId, chatId), question);
    List<ModelMessage> messages = new ArrayList<>();
    String toolResult = "";
    conversationPrompt.append(system);
    // System.out.println("agent epoch limit" + epochLimit);
    log.info("agent epoch limit{}", epochLimit);
    int iteration = 0;
    while (iteration < epochLimit) {
      messages.clear();
      ModelMessage systemMessage =
          new ModelMessage(ModelMessageRole.SYSTEM.value(), conversationPrompt);
      ModelMessage userMessage = new ModelMessage(ModelMessageRole.USER.value(), question);
      messages.add(systemMessage);
      messages.add(userMessage);
      var obj = llm.jsonChat(question, messages, false);
      var answer = (String) obj.getJSONObject("action").get("answer");
      try {
        Map<String, String> res = process_llm_result(obj);
        if (res.get("action_name").equals("finish")) {
          String args = res.get("action_parameters");
          String content = JSON.parseObject(args).getString("content");
          if (queue != null) {
            queue.add(content);
            queue.add("[DONE]");
          }
          return content;
        }
        toolResult =
            manage.runTool(res.get("action_name"), res.get("action_parameters"), globalMessage);
        conversationPrompt.append(obj);
        conversationPrompt.append(String.format("Observation: %s\n", toolResult));
        if (queue != null) {
          queue.add(res.get("thought"));
          queue.add("成功调用工具:" + res.get("action_name") + "。");
        }
        log.info("Thought:{}", res.get("thought"));
        log.info("Observation:{}", toolResult);
      } catch (Exception e) {
        if (queue != null) {
          queue.add(answer);
          queue.add("[DONE]");
        }
        return answer;
      }
      iteration += 1;
    }
    if (queue != null) {
      queue.add(toolResult);
      queue.add("[DONE]");
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
