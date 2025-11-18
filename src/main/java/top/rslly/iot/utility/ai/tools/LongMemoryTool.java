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
import top.rslly.iot.services.thingsModel.ProductServiceImpl;
import top.rslly.iot.utility.ai.IcAiException;
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
    LLM llm = LLMFactory.getLLM(llmName);
    List<ModelMessage> messages = new ArrayList<>();
    // 使用 Optional 进行类型安全的转换
    List<ModelMessage> memory =
        Optional.ofNullable((List<ModelMessage>) globalMessage.get("memory"))
            .orElse(Collections.emptyList());

    ModelMessage systemMessage =
        new ModelMessage(ModelMessageRole.SYSTEM.value(),
            agentLongMemoryPrompt.getAgentLongMemory(productId));
    ModelMessage userMessage = new ModelMessage(ModelMessageRole.USER.value(), "start memory!");
    if (!memory.isEmpty()) {
      // messages.addAll(memory);
      systemMessage.setContent(
          agentLongMemoryPrompt.getAgentLongMemory(productId) + memory);
    } else {
      return;
    }
    messages.add(systemMessage);
    messages.add(userMessage);
    // log.info("LongMemoryTool{}", messages);
    var obj = llm.jsonChat(question, messages, true).getJSONObject("action");
    try {
      process_llm_result(obj, productId);
    } catch (Exception e) {
      // e.printStackTrace();
      log.info("LongMemoryTool{}", e.getMessage());
    }
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
