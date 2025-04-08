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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.rslly.iot.models.AgentMemoryEntity;
import top.rslly.iot.services.agent.AgentMemoryServiceImpl;
import top.rslly.iot.utility.EmotionManager;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.ModelMessageRole;
import top.rslly.iot.utility.ai.llm.LLM;
import top.rslly.iot.utility.ai.llm.LLMFactory;
import top.rslly.iot.utility.ai.prompts.EmotionToolPrompt;

import java.util.*;

@Data
@Component
@Slf4j
public class EmotionTool implements BaseTool<Map<String, String>> {
  @Autowired
  private AgentMemoryServiceImpl agentMemoryService;
  @Autowired
  private EmotionToolPrompt emotionToolPrompt;
  @Value("${ai.emotionTool-llm}")
  private String llmName;
  private String name = "emotionTool";
  private String description = """
      Automatically detect the most appropriate emotional state from conversation context.
      Args: All user requests for emoji generate(str)
      """;

  @Override
  public Map<String, String> run(String question) {
    return null;
  }

  @Override
  public Map<String, String> run(String question, Map<String, Object> globalMessage) {
    LLM llm = LLMFactory.getLLM(llmName);
    Map<String, String> responseMap = new HashMap<>();
    String chatId = (String) globalMessage.get("chatId");
    List<AgentMemoryEntity> agentMemoryEntities = agentMemoryService.findAllByChatId(chatId);
    String currentMemory = "";
    if (!agentMemoryEntities.isEmpty()) {
      currentMemory = agentMemoryEntities.get(0).getContent();
    }
    List<ModelMessage> messages = new ArrayList<>();
    // 使用 Optional 进行类型安全的转换
    List<ModelMessage> memory =
        Optional.ofNullable((List<ModelMessage>) globalMessage.get("memory"))
            .orElse(Collections.emptyList());
    ModelMessage systemMessage =
        new ModelMessage(ModelMessageRole.SYSTEM.value(),
            emotionToolPrompt.getEmotionTool(currentMemory));
    // log.info("systemMessage: " + systemMessage.getContent());
    ModelMessage userMessage = new ModelMessage(ModelMessageRole.USER.value(), question);
    if (!memory.isEmpty()) {
      // messages.addAll(memory);
      systemMessage.setContent(
          emotionToolPrompt.getEmotionTool(currentMemory)
              + memory);
    }
    messages.add(systemMessage);
    messages.add(userMessage);
    var obj = llm.jsonChat(question, messages, true).getJSONObject("action");
    responseMap.put("text", "neutral");
    responseMap.put("emoji", EmotionManager.getCurrentEmotion("neutral"));
    try {
      return process_llm_result(obj);
    } catch (Exception e) {
      log.error(e.getMessage());
      return responseMap;
    }
  }

  private Map<String, String> process_llm_result(JSONObject llmObject) {
    Map<String, String> result = new HashMap<>();
    String emoji = llmObject.getString("emotion");
    if (emoji == null) {
      result.put("text", "neutral");
      result.put("emoji", EmotionManager.getCurrentEmotion("neutral"));
    } else {
      result.put("text", emoji);
      result.put("emoji", EmotionManager.getCurrentEmotion(emoji));
    }
    return result;
  }
}
