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
import top.rslly.iot.services.agent.AgentMemoryServiceImpl;
import top.rslly.iot.services.agent.McpServerServiceImpl;
import top.rslly.iot.services.McpEndpointConfigService;
import top.rslly.iot.services.agent.ProductRoleServiceImpl;
import top.rslly.iot.services.agent.ProductRouterSetServiceImpl;
import top.rslly.iot.services.agent.ProductSkillsServiceImpl;
import top.rslly.iot.services.agent.ProductToolsBanServiceImpl;
import top.rslly.iot.services.knowledgeGraphic.KnowledgeGraphicServiceImpl;
import top.rslly.iot.utility.ai.DescriptionUtil;
import top.rslly.iot.utility.ai.GlobalMessageContext;
import top.rslly.iot.utility.ai.LlmDiyUtility;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.ModelMessageRole;
import top.rslly.iot.utility.ai.chain.FunctionRouterRoute;
import top.rslly.iot.utility.ai.llm.FunctionResult;
import top.rslly.iot.utility.ai.llm.FunctionStreamHandler;
import top.rslly.iot.utility.ai.llm.FunctionToolSpec;
import top.rslly.iot.utility.ai.llm.LLM;
import top.rslly.iot.utility.ai.mcp.McpWebsocket;
import top.rslly.iot.utility.ai.prompts.FunctionCallingRouterPrompt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Data
@Component
@Slf4j
public class FunctionCallingRouterTool {
  /*
   * private static final String ERROR_MESSAGE = "抱歉，当前服务繁忙";
   *
   * private static final String ERROR_MESSAGE = "抱歉，当前服务繁忙";
   */
  private static final String ERROR_MESSAGE = "Service is busy right now";
  private static final String DONE_SIGNAL = "[DONE]";
  private static final int MAX_ROUTER_RULES_CHARS = 1200;
  private static final int MAX_MCP_DESCRIPTION_CHARS = 6000;

  @Autowired
  private FunctionCallingRouterPrompt functionCallingRouterPrompt;
  @Autowired
  private AgentMemoryServiceImpl agentMemoryService;
  @Autowired
  private ProductRoleServiceImpl productRoleService;
  @Autowired
  private ProductToolsBanServiceImpl productToolsBanService;
  @Autowired
  private DescriptionUtil descriptionUtil;
  @Autowired
  private LlmDiyUtility llmDiyUtility;
  @Autowired
  private KnowledgeGraphicServiceImpl knowledgeGraphicService;
  @Autowired
  private KnowledgeTool knowledgeTool;
  @Autowired
  private ProductRouterSetServiceImpl productRouterSetService;
  @Autowired
  private McpServerServiceImpl mcpServerService;
  @Autowired
  private ProductSkillsServiceImpl productSkillsService;
  @Autowired
  private McpWebsocket mcpWebsocket;
  @Autowired
  private McpEndpointConfigService mcpEndpointConfigService;

  private final Map<String, Lock> lockMap = new ConcurrentHashMap<>();
  private final Map<String, Condition> conditionMap = new ConcurrentHashMap<>();
  private final Map<String, FunctionResult> dataMap = new ConcurrentHashMap<>();

  @Value("${ai.classifierTool-llm}")
  private String llmName;
  @Value("${ai.classifierTool-speedUp}")
  private boolean speedUp;
  @Value("${ai.classifierTool-timeout-seconds:60}")
  private long responseTimeoutSeconds;

  public FunctionResult run(String question, Map<String, Object> globalMessage) {
    int productId = (int) globalMessage.get("productId");
    String chatId = (String) globalMessage.get("chatId");
    LLM llm = llmDiyUtility.getDiyLlm(productId, llmName, "classifier");
    if (!llm.supportsFunctionCalling()) {
      return FunctionResult.unsupported();
    }

    List<FunctionToolSpec> toolSpecs = buildToolSpecs(productId, chatId);
    List<ModelMessage> messages =
        buildPromptMessages(question, productId, chatId, globalMessage, toolSpecs);
    if (!speedUp) {
      return normalizeResult(llm.functionChat(question, messages, toolSpecs));
    }
    return runWithStreaming(question, chatId, llm, messages, toolSpecs,
        (Map<String, Queue<String>>) globalMessage.get("queueMap"));
  }

  private FunctionResult runWithStreaming(String question, String chatId, LLM llm,
      List<ModelMessage> messages, List<FunctionToolSpec> toolSpecs,
      Map<String, Queue<String>> queueMap) {
    lockMap.computeIfAbsent(chatId, k -> new ReentrantLock());
    conditionMap.computeIfAbsent(chatId, k -> lockMap.get(k).newCondition());
    dataMap.remove(chatId);
    Lock lock = lockMap.get(chatId);
    Condition condition = conditionMap.get(chatId);
    AtomicBoolean responseStarted = new AtomicBoolean(false);
    StringBuilder replyBuffer = new StringBuilder();

    FunctionStreamHandler handler = new FunctionStreamHandler() {
      @Override
      public void onTextDelta(String text) {
        responseStarted.set(true);
        if (text == null || text.isBlank()) {
          return;
        }
        replyBuffer.append(text);
        Queue<String> queue = queueMap == null ? null : queueMap.get(chatId);
        if (queue != null) {
          queue.add(text.replace("\n", "").replace("\r", ""));
        }
      }

      @Override
      public void onDirectReplyComplete(String reply) {
        finish(FunctionResult.directReply(reply == null || reply.isBlank()
            ? replyBuffer.toString()
            : reply), true);
      }

      @Override
      public void onToolCall(String functionName, String arguments) {
        responseStarted.set(true);
        finish(FunctionResult.toolCall(functionName, arguments), false);
      }

      @Override
      public void onFailure(Throwable throwable) {
        log.error("Function router streaming failed, chatId={}", chatId, throwable);
        if (responseStarted.get() && replyBuffer.length() > 0) {
          finish(FunctionResult.directReply(replyBuffer.toString()), true);
          return;
        }
        finish(FunctionResult.error(ERROR_MESSAGE), false);
      }

      @Override
      public void onUnsupported() {
        finish(FunctionResult.unsupported(), false);
      }

      private void finish(FunctionResult result, boolean signalDone) {
        lock.lock();
        try {
          if (!dataMap.containsKey(chatId)) {
            dataMap.put(chatId, normalizeResult(result));
            if (signalDone) {
              signalQueueDone(queueMap, chatId);
            }
            condition.signalAll();
          }
        } finally {
          lock.unlock();
        }
      }
    };

    llm.streamFunctionChat(question, messages, toolSpecs, handler);
    lock.lock();
    try {
      while (!dataMap.containsKey(chatId)) {
        if (!condition.await(responseTimeoutSeconds, TimeUnit.SECONDS)) {
          dataMap.put(chatId, FunctionResult.error(ERROR_MESSAGE));
          break;
        }
      }
      return normalizeResult(
          dataMap.getOrDefault(chatId, FunctionResult.error(ERROR_MESSAGE)));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return FunctionResult.error(ERROR_MESSAGE);
    } finally {
      lock.unlock();
      cleanup(chatId);
    }
  }

  private List<ModelMessage> buildPromptMessages(String question, int productId, String chatId,
      Map<String, Object> globalMessage, List<FunctionToolSpec> toolSpecs) {
    String memoryChatId = GlobalMessageContext.memoryChatId(globalMessage);
    List<AgentMemoryEntity> agentMemoryEntities = agentMemoryService.findAllByChatId(memoryChatId);
    String currentMemory =
        agentMemoryEntities.isEmpty() ? "" : agentMemoryEntities.get(0).getContent();
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
    String memoryMap = descriptionUtil.getAgentLongMemory(productId);
    String knowledgeGraphic = knowledgeGraphicService.queryKnowledgeGraphic(question, productId);
    String prompt = functionCallingRouterPrompt.build(assistantName, userName, role,
        roleIntroduction, memoryMap, voice,
        limitText(getRouterSet(productId), MAX_ROUTER_RULES_CHARS));
    String userContext = functionCallingRouterPrompt.buildUserContext(
        question, currentMemory, knowledgeGraphic);
    ModelMessage systemMessage = new ModelMessage(ModelMessageRole.SYSTEM.value(), prompt);
    ModelMessage userMessage = new ModelMessage(ModelMessageRole.USER.value(), userContext);
    return ChatTool.buildConversationMessages(memory, systemMessage, userMessage);
  }

  private List<FunctionToolSpec> buildToolSpecs(int productId, String chatId) {
    List<String> banTools = productToolsBanService.getProductToolsBanList(productId);
    Map<FunctionRouterRoute, String> availableRoutes = new LinkedHashMap<>();
    availableRoutes.put(FunctionRouterRoute.WEATHER,
        FunctionRouterRoute.WEATHER.getDefaultDescription());
    availableRoutes.put(FunctionRouterRoute.CONTROL,
        FunctionRouterRoute.CONTROL.getDefaultDescription());
    availableRoutes.put(FunctionRouterRoute.MUSIC,
        FunctionRouterRoute.MUSIC.getDefaultDescription());
    String agentDescription = FunctionRouterRoute.AGENT.getDefaultDescription();
    String knowledgeDescription = knowledgeTool.getRoutingDescription(productId);
    if (!knowledgeDescription.isBlank()) {
      agentDescription += " | " + knowledgeDescription;
    }
    availableRoutes.put(FunctionRouterRoute.AGENT, agentDescription);
    if (!chatId.startsWith("chatProduct")) {
      availableRoutes.put(FunctionRouterRoute.WX_BOUND_PRODUCT,
          FunctionRouterRoute.WX_BOUND_PRODUCT.getDefaultDescription());
      availableRoutes.put(FunctionRouterRoute.WX_PRODUCT_ACTIVE,
          FunctionRouterRoute.WX_PRODUCT_ACTIVE.getDefaultDescription());
    }
    availableRoutes.put(FunctionRouterRoute.SCHEDULE,
        FunctionRouterRoute.SCHEDULE.getDefaultDescription());
    availableRoutes.put(FunctionRouterRoute.PRODUCT_ROLE,
        FunctionRouterRoute.PRODUCT_ROLE.getDefaultDescription());
    String mcpDescription = buildMcpDescription(productId, chatId);
    if (!mcpDescription.isBlank()) {
      availableRoutes.put(FunctionRouterRoute.MCP, mcpDescription);
    }
    if (chatId.startsWith("chatProduct")) {
      availableRoutes.put(FunctionRouterRoute.GOODBYE,
          FunctionRouterRoute.GOODBYE.getDefaultDescription());
    }
    banTools.forEach(banTool -> availableRoutes.entrySet()
        .removeIf(entry -> Objects.equals(entry.getKey().getTaskId(), banTool)));

    List<FunctionToolSpec> toolSpecs = new ArrayList<>();
    for (Map.Entry<FunctionRouterRoute, String> entry : availableRoutes.entrySet()) {
      toolSpecs.add(new FunctionToolSpec(
          entry.getKey().getFunctionName(),
          entry.getValue(),
          FunctionToolSpec.defaultRouterArgsSchema(),
          false));
    }
    return toolSpecs;
  }

  private String buildMcpDescription(int productId, String chatId) {
    var mcpServerList = mcpServerService.findAllByProductId(productId);
    var productSkillsEntities = productSkillsService.findAllByProductId(productId);
    boolean hasMcp =
        !productSkillsEntities.isEmpty()
            || !mcpServerList.isEmpty()
            || mcpWebsocket.isRunning(McpWebsocket.DEVICE_SERVER_NAME, chatId)
            || hasRunningEndpoint(productId);
    if (!hasMcp) {
      return "";
    }

    StringBuilder builder = new StringBuilder();
    mcpServerList.forEach(server -> appendWithSeparator(builder, server.getDescription()));
    productSkillsEntities.forEach(skill -> appendWithSeparator(builder, skill.getName()));
    if (mcpWebsocket.isRunning(McpWebsocket.DEVICE_SERVER_NAME, chatId)) {
      appendWithSeparator(builder,
          mcpWebsocket.getIntention(McpWebsocket.DEVICE_SERVER_NAME, chatId));
    }
    appendEndpointIntentions(builder, productId);
    return limitText(builder.toString(), MAX_MCP_DESCRIPTION_CHARS);
  }

  private boolean hasRunningEndpoint(int productId) {
    for (int endpointIndex = 1; endpointIndex <= mcpEndpointConfigService
        .getEndpointCount(); endpointIndex++) {
      String serverName = McpWebsocket.getEndpointServerName(endpointIndex);
      String endpointChatId = McpWebsocket.getEndpointChatId(productId, endpointIndex);
      if (mcpWebsocket.isRunning(serverName, endpointChatId)) {
        return true;
      }
    }
    return false;
  }

  private void appendEndpointIntentions(StringBuilder builder, int productId) {
    for (int endpointIndex = 1; endpointIndex <= mcpEndpointConfigService
        .getEndpointCount(); endpointIndex++) {
      String serverName = McpWebsocket.getEndpointServerName(endpointIndex);
      String endpointChatId = McpWebsocket.getEndpointChatId(productId, endpointIndex);
      if (mcpWebsocket.isRunning(serverName, endpointChatId)) {
        appendWithSeparator(builder, mcpWebsocket.getIntention(serverName, endpointChatId));
      }
    }
  }

  private void appendWithSeparator(StringBuilder builder, String text) {
    if (text == null || text.isBlank()) {
      return;
    }
    if (!builder.isEmpty()) {
      builder.append(" | ");
    }
    builder.append(text.trim());
  }

  private String getRouterSet(int productId) {
    var productRouterSetList = productRouterSetService.findAllByProductId(productId);
    if (productRouterSetList.isEmpty()) {
      return "";
    }
    return productRouterSetList.get(0).getPrompt();
  }

  private String limitText(String value, int maxChars) {
    if (value == null) {
      return "";
    }
    String normalized = value.replace("\r", " ").replace("\n", " ").trim();
    if (normalized.length() <= maxChars) {
      return normalized;
    }
    return normalized.substring(0, maxChars) + "...";
  }

  private FunctionResult normalizeResult(FunctionResult result) {
    if (result == null) {
      return FunctionResult.error(ERROR_MESSAGE);
    }
    if (result.isDirectReply() && (result.getReply() == null || result.getReply().isBlank())) {
      return FunctionResult.error(ERROR_MESSAGE);
    }
    return result;
  }

  private void signalQueueDone(Map<String, Queue<String>> queueMap, String chatId) {
    if (queueMap == null) {
      return;
    }
    Queue<String> queue = queueMap.get(chatId);
    if (queue != null) {
      queue.add(DONE_SIGNAL);
    }
  }

  private void cleanup(String chatId) {
    dataMap.remove(chatId);
    conditionMap.remove(chatId);
    lockMap.remove(chatId);
  }
}
