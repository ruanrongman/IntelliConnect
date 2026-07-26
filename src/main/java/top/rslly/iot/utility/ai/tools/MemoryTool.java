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
import top.rslly.iot.utility.RedisUtil;
import top.rslly.iot.utility.ai.GlobalMessageContext;
import top.rslly.iot.utility.ai.LlmDiyUtility;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.ModelMessageRole;
import top.rslly.iot.utility.ai.llm.LLM;
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
  @Autowired
  private LlmDiyUtility llmDiyUtility;
  @Autowired
  private RedisUtil redisUtil;
  @Value("${ai.memoryTool-llm}")
  private String llmName;
  private String name = "memoryTool";
  private String description = """
      This tool is used to summarize the conversations
      Args: user question(str)
      """;

  @Async("taskExecutor")
  public void run(String question, Map<String, Object> globalMessage) {
    Object memoryValue = globalMessage.get(GlobalMessageContext.MEMORY);
    List<ModelMessage> memory = copyMemory(memoryValue);
    summarize(memory, question, globalMessage);
  }

  @Async("taskExecutor")
  public void run(List<ModelMessage> conversation, Map<String, Object> globalMessage) {
    summarize(conversation == null ? List.of()
        : Collections.unmodifiableList(new ArrayList<>(conversation)), null, globalMessage);
  }

  private void summarize(List<ModelMessage> memory, String question,
      Map<String, Object> globalMessage) {
    if (memory.isEmpty()) {
      return;
    }
    int productId = (int) globalMessage.get("productId");
    LLM llm = llmDiyUtility.getDiyLlm(productId, llmName, "memory");
    String memoryChatId = GlobalMessageContext.memoryChatId(globalMessage);
    List<AgentMemoryEntity> agentMemoryEntities =
        agentMemoryService.findAllByChatId(memoryChatId);
    String currentMemory = agentMemoryEntities.isEmpty()
        ? ""
        : Optional.ofNullable(agentMemoryEntities.get(0).getContent()).orElse("");

    String conversation = formatConversationHistory(memory);
    if (question != null && !question.isBlank()) {
      conversation += "\nlatest user input: " + question.trim();
    }
    List<ModelMessage> messages = new ArrayList<>();
    messages.add(new ModelMessage(ModelMessageRole.SYSTEM.value(),
        memoryToolPrompt.getMemoryToolPrompt(currentMemory)));
    messages.add(new ModelMessage(ModelMessageRole.USER.value(),
        conversation.isBlank() ? "No useful conversation to compact." : conversation));

    String answer = llm.commonChat(conversation, messages, true);
    if (answer == null || answer.isBlank()) {
      return;
    }
    if (answer.length() > 1000) {
      answer = answer.substring(0, 1000);
    }
    if (!memoryRevisionMatches(memoryChatId, globalMessage)) {
      log.info("skip stale memory summary, chatId={}", memoryChatId);
      return;
    }
    agentMemoryService.insertAndUpdate(new AgentMemory(memoryChatId, answer));
  }

  private boolean memoryRevisionMatches(String chatId, Map<String, Object> globalMessage) {
    Object expectedRevision = globalMessage.get(GlobalMessageContext.MEMORY_REVISION);
    if (expectedRevision == null) {
      return true;
    }
    try {
      Object currentRevision = redisUtil.get(GlobalMessageContext.memoryRevisionKey(chatId));
      return revisionValue(expectedRevision) == revisionValue(currentRevision);
    } catch (NumberFormatException e) {
      log.warn("invalid memory revision, chatId={}", chatId, e);
      return false;
    }
  }

  private long revisionValue(Object value) {
    if (value == null) {
      return 0L;
    }
    return value instanceof Number number
        ? number.longValue()
        : Long.parseLong(value.toString());
  }

  private List<ModelMessage> copyMemory(Object value) {
    if (!(value instanceof List<?> rawList)) {
      return List.of();
    }
    List<ModelMessage> result = new ArrayList<>();
    for (Object item : rawList) {
      if (item instanceof ModelMessage modelMessage) {
        result.add(modelMessage);
      }
    }
    return List.copyOf(result);
  }

  private String formatConversationHistory(List<ModelMessage> memory) {
    StringBuilder builder = new StringBuilder("\n");
    for (ModelMessage message : memory) {
      if (message == null || message.getRole() == null || message.getContent() == null) {
        continue;
      }
      String content = message.getContent().toString().trim();
      if (content.isBlank()) {
        continue;
      }
      if (ModelMessageRole.USER.value().equals(message.getRole())) {
        builder.append("user: ").append(content).append("\n");
      } else if (ModelMessageRole.ASSISTANT.value().equals(message.getRole())) {
        builder.append("assistant: ").append(content).append("\n");
      }
    }
    return builder.toString();
  }
}
