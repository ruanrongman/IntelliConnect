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
import org.springframework.stereotype.Component;
import top.rslly.iot.models.AgentMemoryEntity;
import top.rslly.iot.models.ProductRoleEntity;
import top.rslly.iot.services.agent.*;
import top.rslly.iot.services.knowledgeGraphic.KnowledgeGraphicServiceImpl;
import top.rslly.iot.utility.HttpRequestUtils;
import top.rslly.iot.utility.ai.DescriptionUtil;
import top.rslly.iot.utility.ai.LlmDiyUtility;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.ModelMessageRole;
import top.rslly.iot.utility.ai.llm.LLM;
import top.rslly.iot.utility.ai.llm.LLMFactory;
import top.rslly.iot.utility.ai.prompts.ChatToolPrompt;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Data
@Component
@Slf4j
public class ChatTool implements BaseTool<String> {
  @Autowired
  private ChatToolPrompt chatToolPrompt;
  @Autowired
  private AgentMemoryServiceImpl agentMemoryService;
  @Autowired
  private HttpRequestUtils httpRequestUtils;
  @Autowired
  private ProductRoleServiceImpl productRoleService;
  @Autowired
  private KnowledgeChatServiceImpl knowledgeChatService;
  @Autowired
  private ProductToolsBanServiceImpl productToolsBanService;
  @Autowired
  private DescriptionUtil descriptionUtil;
  @Autowired
  private LlmDiyUtility llmDiyUtility;
  @Autowired
  private KnowledgeGraphicServiceImpl knowledgeGraphicService;

  // 将锁和条件变量改为每个 chatId 独立
  private final Map<String, Lock> lockMap = new ConcurrentHashMap<>();
  private final Map<String, Condition> conditionMap = new ConcurrentHashMap<>();
  private final Map<String, String> dataMap = new ConcurrentHashMap<>();

  @Value("${ai.chatTool-llm}")
  private String llmName;
  @Value("${ai.chatTool-speedUp}")
  private boolean speedUp;
  private String name = "chatTool";
  private String description = """
      This tool is used to chat with people
      Args: user question(str)
      """;

  @Override
  public String run(String question) {
    return null;
  }

  public String run(String question, Map<String, Object> globalMessage) {
    int productId = (int) globalMessage.get("productId");
    LLM llm = llmDiyUtility.getDiyLlm(productId, llmName, "5");
    Map<String, Queue<String>> queueMap =
        (Map<String, Queue<String>>) globalMessage.get("queueMap");
    String chatId = (String) globalMessage.get("chatId");

    // 初始化当前会话的锁和条件变量
    lockMap.putIfAbsent(chatId, new ReentrantLock());
    conditionMap.putIfAbsent(chatId, lockMap.get(chatId).newCondition());

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
    List<ProductRoleEntity> productRole = productRoleService.findAllByProductId(productId);
    String assistantName = null;
    String userName = null;
    String role = null;
    String roleIntroduction = null;
    if (!productRole.isEmpty()) {
      assistantName = productRole.get(0).getAssistantName();
      userName = productRole.get(0).getUserName();
      role = productRole.get(0).getRole();
      roleIntroduction = productRole.get(0).getRoleIntroduction();
    }
    String information = "";
    var productToolsBanList = productToolsBanService.getProductToolsBanList(productId);
    if (productToolsBanList.isEmpty() || !productToolsBanList.contains("knowledge")) {
      information = knowledgeChatService.searchByProductId(String.valueOf(productId), question);
    }
    String memoryMap = descriptionUtil.getAgentLongMemory(productId);
    String knowledgeGraphic = knowledgeGraphicService.queryKnowledgeGraphic(question, productId);
    ModelMessage systemMessage =
        new ModelMessage(ModelMessageRole.SYSTEM.value(),
            chatToolPrompt.getChatTool(assistantName, userName, role, roleIntroduction,
                currentMemory, information, memoryMap, knowledgeGraphic));
    log.info(llmName);
    log.info("systemMessage: " + systemMessage.getContent());
    ModelMessage userMessage = new ModelMessage(ModelMessageRole.USER.value(), question);
    if (!memory.isEmpty()) {
      // messages.addAll(memory);
      systemMessage.setContent(
          chatToolPrompt.getChatTool(assistantName, userName, role, roleIntroduction, currentMemory,
              information, memoryMap, knowledgeGraphic)
              + memory);
    }
    messages.add(systemMessage);
    messages.add(userMessage);
    // log.info("chatTool: " + messages);
    if (speedUp) {
      dataMap.remove(chatId); // 使用 chatId 作为 key
      llm.streamJsonChat(question, messages, true,
          new ChatToolEventSourceListener(queueMap, chatId, this));
      Lock chatLock = lockMap.get(chatId);
      Condition chatCondition = conditionMap.get(chatId);
      chatLock.lock();
      try {
        while (dataMap.get(chatId) == null) {
          chatCondition.await();
        }
      } catch (Exception e) {
        log.error("ChatTool error{}", e.getMessage());
      } finally {
        chatLock.unlock();
      }
      String data = dataMap.get(chatId);
      dataMap.remove(chatId);
      conditionMap.remove(chatId);
      lockMap.remove(chatId);
      return data;
    } else {
      return llm.commonChat(question, messages, true);
    }
  }
}
