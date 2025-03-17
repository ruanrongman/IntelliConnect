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

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import top.rslly.iot.models.AgentMemoryEntity;
import top.rslly.iot.param.request.AgentMemory;
import top.rslly.iot.services.agent.AgentMemoryServiceImpl;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.ModelMessageRole;
import top.rslly.iot.utility.ai.llm.LLM;
import top.rslly.iot.utility.ai.llm.LLMFactory;
import top.rslly.iot.utility.ai.prompts.MemoryToolPrompt;

import java.util.*;

@Data
@Component
@Slf4j
public class MemoryTool {
  @Autowired
  private MemoryToolPrompt memoryToolPrompt;
  @Autowired
  private AgentMemoryServiceImpl agentMemoryService;
  @Value("${ai.memoryTool-llm}")
  private String llmName;
  private String name = "memoryTool";
  private String description = """
      This tool is used to summarize the conversations
      Args: user question(str)
      """;

  @Async("taskExecutor")
  public void run(String question, Map<String, Object> globalMessage) {
    LLM llm = LLMFactory.getLLM(llmName);
    List<ModelMessage> messages = new ArrayList<>();
    String chatId = (String) globalMessage.get("chatId");
    List<AgentMemoryEntity> agentMemoryEntities = agentMemoryService.findAllByChatId(chatId);
    String currentMemory = "";
    if (!agentMemoryEntities.isEmpty()) {
      currentMemory = agentMemoryEntities.get(0).getContent();
    }
    // 使用 Optional 进行类型安全的转换
    List<ModelMessage> memory =
        Optional.ofNullable((List<ModelMessage>) globalMessage.get("memory"))
            .orElse(Collections.emptyList());

    ModelMessage systemMessage =
        new ModelMessage(ModelMessageRole.SYSTEM.value(),
            memoryToolPrompt.getMemoryToolPrompt(currentMemory));
    ModelMessage userMessage = new ModelMessage(ModelMessageRole.USER.value(), question);
    if (!memory.isEmpty()) {
      // messages.addAll(memory);
      systemMessage.setContent(
          memoryToolPrompt.getMemoryToolPrompt(currentMemory) + memory);
    } else {
      return;
    }
    messages.add(systemMessage);
    messages.add(userMessage);
    // log.info("systemMessage: " + systemMessage.getContent());
    String answer = llm.commonChat(question, messages, true);
    if (answer.length() > 254)
      answer = answer.substring(0, 254);
    AgentMemory agentMemory = new AgentMemory(chatId, answer);
    agentMemoryService.insertAndUpdate(agentMemory);
    // log.info("chatTool: " + messages);
  }
}
