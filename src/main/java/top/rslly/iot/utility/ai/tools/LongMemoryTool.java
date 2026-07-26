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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import top.rslly.iot.param.request.AgentLongMemoryToolParam;
import top.rslly.iot.services.agent.AgentLongMemoryServiceImpl;
import top.rslly.iot.services.agent.LlmProviderInformationServiceImpl;
import top.rslly.iot.services.agent.ProductLlmModelServiceImpl;
import top.rslly.iot.services.thingsModel.ProductServiceImpl;
import top.rslly.iot.utility.ai.IcAiException;
import top.rslly.iot.utility.ai.LlmDiyUtility;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.ModelMessageRole;
import top.rslly.iot.utility.ai.llm.LLM;
import top.rslly.iot.utility.ai.llm.LLMFactory;
import top.rslly.iot.utility.ai.prompts.AgentLongMemoryPrompt;

import java.util.*;

@Component
@Data
@Slf4j
public class LongMemoryTool {
  @Autowired
  private AgentLongMemoryServiceImpl agentMemoryService;
  @Autowired
  private AgentLongMemoryPrompt agentLongMemoryPrompt;
  @Autowired
  private ProductServiceImpl productService;
  @Autowired
  private AgentLongMemoryServiceImpl agentLongMemoryService;
  @Autowired
  private LlmDiyUtility llmDiyUtility;
  @Value("${ai.longMemoryTool-llm}")
  private String llmName;
  private String name = "longMemoryTool";
  private String description = """
      This tool is used to summarize the conversations for longMemory
      Args: user question(str)
      """;

  @Async("taskExecutor")
  public void run(String question, Map<String, Object> globalMessage) {
    int productId = (int) globalMessage.get("productId");

    if (productService.findAllById(productId).isEmpty()
        || agentLongMemoryService.findAllByProductId(productId).isEmpty())
      return;
    LLM llm = llmDiyUtility.getDiyLlm(productId, llmName, "longMemory");
    List<ModelMessage> messages = new ArrayList<>();
    List<ModelMessage> memory = copyMemory(globalMessage.get("memory"));
    if (memory.isEmpty()) {
      return;
    }

    ModelMessage systemMessage =
        new ModelMessage(ModelMessageRole.SYSTEM.value(),
            agentLongMemoryPrompt.getAgentLongMemory(productId));
    ModelMessage userMessage = new ModelMessage(ModelMessageRole.USER.value(),
        "start memory!\n" + formatConversationHistory(memory));
    messages.add(systemMessage);
    messages.add(userMessage);
    var response = llm.jsonChat(question, messages, true);
    if (response == null || response.getJSONObject("action") == null) {
      return;
    }
    var obj = response.getJSONObject("action");
    try {
      process_llm_result(obj, productId);
    } catch (Exception e) {
      // e.printStackTrace();
      log.info("LongMemoryTool{}", e.getMessage());
    }
  }

  private List<ModelMessage> copyMemory(Object value) {
    if (!(value instanceof List<?> rawList)) {
      return List.of();
    }
    List<ModelMessage> memory = new ArrayList<>();
    for (Object item : rawList) {
      if (item instanceof ModelMessage modelMessage) {
        memory.add(modelMessage);
      }
    }
    return Collections.unmodifiableList(new ArrayList<>(memory));
  }

  private String formatConversationHistory(List<ModelMessage> memory) {
    StringBuilder builder = new StringBuilder();
    for (ModelMessage message : memory) {
      if (message == null || message.getRole() == null || message.getContent() == null) {
        continue;
      }
      String content = String.valueOf(message.getContent()).trim();
      if (content.isBlank()) {
        continue;
      }
      builder.append(message.getRole()).append(": ").append(content).append("\n");
    }
    return builder.toString();
  }

  private void process_llm_result(JSONObject jsonObject, int productId) throws IcAiException {
    JSONArray memoryKeyJson = jsonObject.getJSONArray("memory_Key");
    JSONArray memoryValueJson = jsonObject.getJSONArray("memory_value");
    List<String> memoryKey = JSONObject.parseArray(memoryKeyJson.toJSONString(), String.class);
    List<String> memoryValue =
        JSONObject.parseArray(memoryValueJson.toJSONString(), String.class);
    for (var s : memoryKey) {
      AgentLongMemoryToolParam param = new AgentLongMemoryToolParam();
      param.setProductId(productId);
      if (s != null) {
        param.setMemoryKey(s);
        if (memoryValue.get(memoryKey.indexOf(s)) != null) {
          String memoryValueAnswer = memoryValue.get(memoryKey.indexOf(s));
          if (memoryValueAnswer.length() > 1000) {
            memoryValueAnswer = memoryValueAnswer.substring(0, 1000);
          }
          param.setMemoryValue(memoryValueAnswer);
          var result = agentLongMemoryService.updateLongMemory(param);
          if (!result)
            throw new IcAiException("update long memory error!!");
        }
      }
    }
  }
}
