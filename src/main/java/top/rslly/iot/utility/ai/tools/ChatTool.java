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
import top.rslly.iot.utility.ai.prompts.ChatToolPrompt;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
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
  @Value("${ai.chatTool-timeout-seconds:60}")
  private long responseTimeoutSeconds;
  private String name = "chatTool";
  private String description = """
      This tool is used to chat with people
      Args: user question(str)
      """;
  private static final String ERROR_MESSAGE = "对不起你购买的产品尚不支持这个请求或者设备不在线，请检查你的小程序的设置";

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

    // 仅在 speedUp 模式下初始化同步资源
    if (speedUp) {
      lockMap.computeIfAbsent(chatId, k -> new ReentrantLock());
      conditionMap.computeIfAbsent(chatId, k -> lockMap.get(k).newCondition());
    }
    String memoryChatId = chatId;
    if (chatId.endsWith("_prediction")) {
      memoryChatId = chatId.substring(0, chatId.length() - "_prediction".length());
    }
    List<AgentMemoryEntity> agentMemoryEntities = agentMemoryService.findAllByChatId(memoryChatId);
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
    String voice = null;
    if (!productRole.isEmpty()) {
      assistantName = productRole.get(0).getAssistantName();
      userName = productRole.get(0).getUserName();
      role = productRole.get(0).getRole();
      roleIntroduction = productRole.get(0).getRoleIntroduction();
      voice = productRole.get(0).getVoice();
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
                currentMemory, information, memoryMap, knowledgeGraphic, voice));
    log.info(llmName);
    ModelMessage userMessage = new ModelMessage(ModelMessageRole.USER.value(), question);
    messages.addAll(buildConversationMessages(memory, systemMessage, userMessage));
    if (speedUp) {
      return runWithStreaming(question, llm, messages, queueMap, chatId);
    }
    return runWithoutStreaming(question, llm, messages);
  }

  private String runWithStreaming(String question, LLM llm, List<ModelMessage> messages,
      Map<String, Queue<String>> queueMap, String chatId) {
    ChatToolEventSourceListener listener = new ChatToolEventSourceListener(queueMap, chatId, this);
    Lock chatLock = lockMap.get(chatId);
    Condition chatCondition = conditionMap.get(chatId);
    dataMap.remove(chatId);
    try {
      llm.streamJsonChat(question, messages, true, listener);
      chatLock.lock();
      try {
        while (dataMap.get(chatId) == null) {
          if (!chatCondition.await(responseTimeoutSeconds, TimeUnit.SECONDS)) {
            log.warn("ChatTool流式响应超时, chatId={}", chatId);
            listener.cancelCurrentStream();
            signalQueueDone(queueMap, chatId);
            dataMap.put(chatId, ERROR_MESSAGE);
            break;
          }
        }
      } finally {
        chatLock.unlock();
      }
      return Optional.ofNullable(dataMap.get(chatId)).filter(result -> !result.isBlank())
          .orElse(ERROR_MESSAGE);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      listener.cancelCurrentStream();
      signalQueueDone(queueMap, chatId);
      log.error("ChatTool等待流式响应时被中断, chatId={}", chatId, e);
      return ERROR_MESSAGE;
    } catch (Exception e) {
      listener.cancelCurrentStream();
      signalQueueDone(queueMap, chatId);
      log.error("ChatTool流式调用失败, chatId={}", chatId, e);
      return ERROR_MESSAGE;
    } finally {
      cleanupResources(chatId);
    }
  }

  private String runWithoutStreaming(String question, LLM llm, List<ModelMessage> messages) {
    try {
      String result = llm.commonChat(question, messages, true);
      if (result == null || result.isBlank()) {
        return ERROR_MESSAGE;
      }
      return result;
    } catch (Exception e) {
      log.error("ChatTool普通调用失败", e);
      return ERROR_MESSAGE;
    }
  }

  static List<ModelMessage> buildConversationMessages(List<ModelMessage> memory,
      ModelMessage systemMessage, ModelMessage userMessage) {
    List<ModelMessage> messages = new ArrayList<>();
    messages.add(systemMessage);
    if (memory != null) {
      for (ModelMessage message : memory) {
        if (message != null && message.getRole() != null && message.getContent() != null) {
          messages.add(message);
        }
      }
    }
    messages.add(userMessage);
    return messages;
  }

  private void signalQueueDone(Map<String, Queue<String>> queueMap, String chatId) {
    if (queueMap == null) {
      return;
    }
    Queue<String> queue = queueMap.get(chatId);
    if (queue != null) {
      queue.add("[DONE]");
    }
  }

  private void cleanupResources(String chatId) {
    dataMap.remove(chatId);
    conditionMap.remove(chatId);
    lockMap.remove(chatId);
  }
}
