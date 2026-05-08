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
package top.rslly.iot.utility.ai.mcp;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.rslly.iot.models.AdminConfigEntity;
import top.rslly.iot.models.ProductSkillsEntity;
import top.rslly.iot.services.AdminConfigServiceImpl;
import top.rslly.iot.services.agent.McpServerServiceImpl;
import top.rslly.iot.services.agent.ProductSkillsServiceImpl;
import top.rslly.iot.utility.ai.LlmDiyUtility;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.ModelMessageRole;
import top.rslly.iot.utility.ai.llm.LLM;
import top.rslly.iot.utility.ai.llm.FunctionResult;
import top.rslly.iot.utility.ai.llm.FunctionStreamHandler;
import top.rslly.iot.utility.ai.llm.FunctionToolSpec;
import top.rslly.iot.utility.ai.prompts.ReactPrompt;
import top.rslly.iot.utility.ai.toolAgent.AgentEventSourceListener;
import top.rslly.iot.utility.ai.tools.BaseTool;
import top.rslly.iot.utility.ai.tools.JsExecuteTool;
import top.rslly.iot.utility.ai.tools.ToolPrefix;
import top.rslly.iot.utility.smartVoice.XiaoZhiWebsocket;

import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@Data
public class McpAgent implements BaseTool<String> {
  @Value("${ai.mcp.agent-llm}")
  private String llmName;

  @Value("${ai.mcp.agent-epoch-limit:8}")
  private int epochLimit;
  @Value("${ai.mcp.agent-speedUp:true}")
  private boolean speedUp;
  @Value("${ai.agent.showThinking:true}")
  private boolean showThinking;
  @Value("${ai.mcp.agent-include-thought:false}")
  private boolean includeThought;
  private String name = "mcpAgent";

  @Autowired
  private ReactPrompt reactPrompt;
  @Autowired
  private McpServerServiceImpl mcpServerService;
  @Autowired
  private McpWebsocket mcpWebsocket;
  @Autowired
  private LlmDiyUtility llmDiyUtility;
  @Autowired
  private ProductSkillsServiceImpl productSkillsService;
  @Autowired
  private JsExecuteTool jsExecuteTool;
  @Autowired
  private AdminConfigServiceImpl adminConfigService;
  @Value("${ai.router.mode:prompt}")
  private String routerMode;
  @Value("${ai.mcp.agent-mode:prompt}")
  private String mcpAgentMode;
  @Value("${ai.mcp.agent-function-timeout-seconds:60}")
  private long functionTimeoutSeconds;

  // 添加锁和条件变量映射（与ChatTool保持一致）
  private final Map<String, Lock> lockMap = new ConcurrentHashMap<>();
  private final Map<String, Condition> conditionMap = new ConcurrentHashMap<>();
  private final Map<String, String> dataMap = new ConcurrentHashMap<>();
  private final Map<String, FunctionResult> functionDataMap = new ConcurrentHashMap<>();

  private static final List<String> COMFORT_PHRASES = Arrays.asList(
      "嗯～",
      "哦～",
      "好～",
      "行～",
      "来啦～",
      "稍等～",
      "马上～",
      "正在～",
      "处理中～",
      "查询中～",
      "思考中～",
      "嗯哼～",
      "好的～",
      "收到～",
      "了解～");

  private String getRandomComfortPhrase() {
    return COMFORT_PHRASES.get(ThreadLocalRandom.current().nextInt(COMFORT_PHRASES.size()));
  }

  public String getDescription(int productId, String chatId) {
    var mcpServerEntities = mcpServerService.findAllByProductId(productId);
    var mcpWebSocketRunning = mcpWebsocket.isRunning(McpWebsocket.DEVICE_SERVER_NAME, chatId);
    var mcpEndpointRunning =
        mcpWebsocket.isRunning(McpWebsocket.ENDPOINT_SERVER_NAME, "mcp" + productId);
    var productSkillsEntities = productSkillsService.findAllByProductId(productId);
    if (mcpServerEntities.isEmpty() && productSkillsEntities.isEmpty() && !mcpWebSocketRunning
        && !mcpEndpointRunning) {
      return "工具无效，请勿调用";
    }
    StringBuilder toolDescriptions = new StringBuilder();
    for (var mcpServerEntity : mcpServerEntities) {
      toolDescriptions.append(mcpServerEntity.getDescription()).append("|");
    }
    for (var productSkillsEntity : productSkillsEntities) {
      toolDescriptions.append(productSkillsEntity.getName()).append("|");
    }
    toolDescriptions.append(mcpWebsocket.getIntention(McpWebsocket.DEVICE_SERVER_NAME, chatId));
    toolDescriptions
        .append(mcpWebsocket.getIntention(McpWebsocket.ENDPOINT_SERVER_NAME, "mcp" + productId));
    return toolDescriptions.toString();
  }

  public void initClients(int productId, Map<String, McpSyncClient> clientMap)
      throws IllegalArgumentException {
    // 解析配置
    var mcpServerEntities = mcpServerService.findAllByProductId(productId);
    for (var mcpServerEntity : mcpServerEntities) {
      String key = mcpServerEntity.getDescription();
      String url = mcpServerEntity.getUrl();
      String sseEndpoint = mcpServerEntity.getSseEndpoint();
      try {
        // 创建 transport 和 client
        McpClientTransport transport = HttpClientSseClientTransport
            .builder(url)
            .sseEndpoint(sseEndpoint)
            .build();
        McpSyncClient client = McpClient.sync(transport)
            .requestTimeout(Duration.ofSeconds(10))
            .capabilities(McpSchema.ClientCapabilities.builder()
                .roots(true)
                .sampling()
                .build())
            .build();
        client.initialize();
        client.ping();
        clientMap.put(client.getServerInfo().name(), client);
        log.info("Initialized MCP server [{}] at {}", key, url);
      } catch (Exception e) {
        log.error("Failed to initialize MCP server [{}] at {}", key, url);
      }
    }
    if (clientMap.isEmpty()) {
      throw new IllegalArgumentException("No MCP server found for productId: " + productId);
    }
  }

  private boolean isQueueRemoved(String chatId, Map<String, Queue<String>> queueMap) {
    if (queueMap == null) {
      return false;
    }
    boolean removed = !queueMap.containsKey(chatId);
    if (removed) {
      log.info("检测到队列被移除，Agent 中断执行，chatId: {}", chatId);
    }
    return removed;
  }

  @Override
  public String run(String question) {
    return null;
  }

  @Override
  public String run(String question, Map<String, Object> globalMessage) {
    Map<String, McpSyncClient> clientMap = new LinkedHashMap<>();
    StringBuilder conversationPrompt = new StringBuilder();
    String chatId = (String) globalMessage.get("chatId");
    int productId = (int) globalMessage.get("productId");
    boolean mcpIsTool = false;
    if (globalMessage.containsKey("mcpIsTool"))
      mcpIsTool = (boolean) globalMessage.get("mcpIsTool");
    if (mcpServerService.findAllByProductId(productId).isEmpty()
        && productSkillsService.findAllByProductId(productId).isEmpty()
        && !mcpWebsocket.isRunning(McpWebsocket.DEVICE_SERVER_NAME, chatId)
        && !mcpWebsocket.isRunning(McpWebsocket.ENDPOINT_SERVER_NAME, "mcp" + productId))
      return "产品下面没有任何mcp服务器或者skills，请先绑定mcp服务器或者skills";
    if (!mcpServerService.findAllByProductId(productId).isEmpty()) {
      try {
        initClients(productId, clientMap);
      } catch (Exception e) {
        return "连接mcp服务器失败";
      }
    }

    if (isFunctionMcpAgentMode()) {
      String functionResult = runFunctionCalling(question, globalMessage, clientMap, mcpIsTool);
      if (functionResult != null) {
        return functionResult;
      }
      log.info("MCP Agent function calling unavailable, fallback to ReAct prompt mode");
    }

    // 仅在 speedUp 模式下初始化同步资源
    if (speedUp) {
      lockMap.computeIfAbsent(chatId, k -> new ReentrantLock());
      conditionMap.computeIfAbsent(chatId, k -> lockMap.get(k).newCondition());
    }

    // 构建 system prompt，包括所有服务器的工具描述
    String system;
    boolean includeThoughtEnabled = getIncludeThoughtConfig();
    try {
      String toolDescriptions = combineToolDescriptions(clientMap);
      if (mcpWebsocket.isRunning(McpWebsocket.DEVICE_SERVER_NAME, chatId)) {
        toolDescriptions +=
            mcpWebsocket.combineToolDescription(McpWebsocket.DEVICE_SERVER_NAME, chatId);
      }
      if (mcpWebsocket.isRunning(McpWebsocket.ENDPOINT_SERVER_NAME, "mcp" + productId)) {
        toolDescriptions += mcpWebsocket.combineToolDescription(McpWebsocket.ENDPOINT_SERVER_NAME,
            "mcp" + productId);
      }
      if (!productSkillsService.findAllByProductId(productId).isEmpty()) {
        toolDescriptions += productSkillsService.combineToolDescription(productId);
      }
      system = reactPrompt.getReact(toolDescriptions, question, productId, 1, epochLimit,
          includeThoughtEnabled);
    } catch (Exception e) {
      return "获取工具描述失败";
    }
    Map<String, Queue<String>> queueMap =
        (Map<String, Queue<String>>) globalMessage.get("queueMap");
    var queue = queueMap.get(chatId);
    if (queue != null) {
      queue.add(ToolPrefix.MCP_AGENT.getPrefix());
      if (!isFunctionRouterMode()) {
        queue.add(getRandomComfortPhrase());
      }
    }
    // log.info("system: {}", system);

    List<ModelMessage> messages = new ArrayList<>();
    String toolResult = "";
    conversationPrompt.setLength(0);
    conversationPrompt.append(system);

    int iteration = 0;
    while (iteration < epochLimit) {
      if (isQueueRemoved(chatId, queueMap)) {
        cleanupResources(clientMap, chatId);
        return "操作已取消";
      }
      // 更新当前轮次信息
      if (iteration > 0) {
        conversationPrompt
            .append(String.format("\n## Current Step: %d of %d\n", iteration + 1, epochLimit));
      }
      messages.clear();
      messages
          .add(new ModelMessage(ModelMessageRole.SYSTEM.value(), conversationPrompt.toString()));
      messages.add(new ModelMessage(ModelMessageRole.USER.value(), question));

      var obj =
          callLLMForThought(question, messages, queueMap, chatId, productId, includeThoughtEnabled);
      if (obj == null) {
        cleanupResources(clientMap, chatId);
        return "小主人抱歉哦，服务器现在繁忙。";
      }
      String thought;
      String answer;
      try {
        thought = obj.getString("thought");
        answer = obj.getJSONObject("action").getString("answer");
      } catch (Exception e) {
        log.error("解析LLM响应失败: {}", e.getMessage());
        cleanupResources(clientMap, chatId);
        return "解析LLM响应失败";
      }

      try {
        var action = obj.getJSONObject("action");
        String fullName = action.getString("name");
        String args = action.getString("args");

        // 格式: serverKey:toolName
        if (!fullName.contains(":") && !fullName.equals("finish")) {
          toolResult = "Error calling tool, must use format 'serverKey:toolName'";
          conversationPrompt.append(String.format(
              "%s\nAction: %s\nObservation: %s\n",
              buildThoughtLine(thought), fullName, toolResult));
          if (queue != null) {
            queue.add("调用工具出现小错误");
          }
          log.warn("Invalid tool name format: {}", fullName); // 添加日志记录
          iteration++;
          continue; // 继续下一轮迭代或根据需要跳出
        }
        String[] parts = fullName.split(":", 2);
        if (parts.length != 2 || parts[0].equalsIgnoreCase("finish")) {
          // 释放mcp客户端
          cleanupResources(clientMap, chatId);
          // finish or invalid format
          String content = JSON.parseObject(args).getString("content");
          if (queue != null) {
            queue.add(content);
            if (!mcpIsTool)
              queue.add("[DONE]");
          }
          return content;
        }
        String serverKey = parts[0];
        String toolName = parts[1];
        // 调用指定服务器的工具
        try {
          if (serverKey.equals(McpWebsocket.DEVICE_SERVER_NAME)) {
            toolResult = mcpWebsocket.sendToolCall(McpWebsocket.DEVICE_SERVER_NAME, chatId,
                toolName, args, XiaoZhiWebsocket.clients.get(chatId), false);
          } else if (serverKey.equals(McpWebsocket.ENDPOINT_SERVER_NAME)) {
            toolResult =
                mcpWebsocket.sendToolCall(McpWebsocket.ENDPOINT_SERVER_NAME, "mcp" + productId,
                    toolName, args, McpWebsocketEndpoint.clients.get("mcp" + productId), true);
          } else if (serverKey.equals("skills")) {
            if (toolName.equals(jsExecuteTool.getName())) {
              toolResult = jsExecuteTool.run(args, globalMessage);
            } else {
              List<ProductSkillsEntity> skillsEntity =
                  productSkillsService.findAllByProductIdAndName(productId, toolName);
              if (skillsEntity.isEmpty()) {
                toolResult = "调用skills失败";
              } else {
                toolResult = productSkillsService.readSkills(skillsEntity.get(0).getFilePath());
              }
            }
          } else {
            McpSyncClient client = clientMap.get(serverKey);
            if (client == null) {
              throw new IllegalArgumentException("Unknown server key: " + serverKey);
            }
            McpSchema.CallToolResult result = client.callTool(
                new McpSchema.CallToolRequest(toolName, args));
            toolResult = result.content().get(0).toString();
          }
        } catch (Exception e) {
          log.error("Error during tool call{}", e.getMessage());
          toolResult = "Error calling tool, please try again";
        }
        // 更新对话
        conversationPrompt.append(String.format(
            "%s\nAction: %s(%s)\nObservation: %s\n",
            buildThoughtLine(thought), fullName, args, toolResult));
        if (queue != null) {
          queue.add(ToolPrefix.getToolCallPrefix(fullName));
        }
        log.info("Thought: {}", thought);
        log.info("Observation ({}): {}", fullName, toolResult);

      } catch (Exception e) {
        log.error("Error during tool call{}", e.getMessage());
        if (answer == null)
          answer = "mcp服务器调用发生严重错误，请检查mcp服务器";
        if (queue != null) {
          queue.add(answer);
          if (!mcpIsTool)
            queue.add("[DONE]");
        }
        cleanupResources(clientMap, chatId);
        return answer;
      }
      iteration++;
    }
    // 释放mcp客户端
    cleanupResources(clientMap, chatId);
    // 超出迭代轮次，调用模型进行总结
    String summaryPrompt = "请根据以上对话历史和最终观察结果，总结回答最初的问题: " + question;
    messages.clear();
    messages.add(new ModelMessage(ModelMessageRole.SYSTEM.value(), summaryPrompt));
    messages.add(new ModelMessage(ModelMessageRole.USER.value(), conversationPrompt.toString()));

    LLM summaryLlm = llmDiyUtility.getDiyLlm(productId, llmName, "10");
    try {
      toolResult = summaryLlm.commonChat(summaryPrompt, messages, false);
      try {
        var temp = toolResult.replace("```json", "").replace("```JSON", "").replace("```", "");
        JSONObject jsonResponse = JSON.parseObject(temp);
        if (jsonResponse == null)
          throw new Exception("json parse error");
        try {
          toolResult =
              jsonResponse.getJSONObject("action").getJSONObject("args").getString("content");
        } catch (Exception ignore) {
          toolResult = jsonResponse.getJSONObject("action").getJSONObject("args").toString();
        }
      } catch (Exception e) {
        log.error("Error during summary generation: ", e);
      }
    } catch (Exception e) {
      log.error("Error generating summary after max iterations: ", e);
      toolResult = "经过多次尝试仍未找到完整答案，请重新提问或提供更多细节。";
    }
    if (toolResult == null) {
      toolResult = "经过多次尝试仍未找到完整答案，请重新提问或提供更多细节。";
    }
    if (queue != null) {
      queue.add(toolResult);
      if (!mcpIsTool)
        queue.add("[DONE]");
    }
    return toolResult;
  }

  /**
   * 调用LLM获取thought，支持流式和非流式
   */
  private JSONObject callLLMForThought(String question, List<ModelMessage> messages,
      Map<String, Queue<String>> queueMap, String chatId, int productId,
      boolean includeThoughtEnabled) {
    LLM llm = llmDiyUtility.getDiyLlm(productId, llmName, "10");
    if (speedUp) {
      // 使用流式调用，实时获取thought内容
      dataMap.remove(chatId);

      try {
        llm.streamJsonChat(question, messages, false,
            new AgentEventSourceListener(queueMap, chatId, this, "thought",
                showThinking && includeThoughtEnabled));

        Lock chatLock = lockMap.get(chatId);
        Condition chatCondition = conditionMap.get(chatId);

        chatLock.lock();
        try {
          // 等待流式响应完成
          while (dataMap.get(chatId) == null) {
            chatCondition.await();
          }
        } catch (InterruptedException e) {
          log.error("等待LLM响应时被中断: {}", e.getMessage());
          Thread.currentThread().interrupt();
          return null;
        } finally {
          chatLock.unlock();
        }

        String data = dataMap.get(chatId);
        if (data == null || data.isEmpty()) {
          log.error("LLM返回空数据");
          return null;
        }

        try {
          return JSON.parseObject(data.replace("```json", "")
              .replace("```JSON", "").replace("```", ""));
        } catch (Exception e) {
          log.error("解析LLM响应JSON失败: {}, data: {}", e.getMessage(), data);
          return null;
        }
      } catch (Exception e) {
        log.error("流式调用LLM失败: {}", e.getMessage());
        return null;
      }
    } else {
      // 非流式调用
      try {
        return llm.jsonChat(question, messages, false);
      } catch (Exception e) {
        log.error("调用LLM失败: {}", e.getMessage());
        return null;
      }
    }
  }

  /**
   * 清理资源（MCP客户端、锁、条件变量等）
   */
  private void cleanupResources(Map<String, McpSyncClient> clientMap, String chatId) {
    // 释放mcp客户端
    for (var client : clientMap.values()) {
      try {
        client.close();
      } catch (Exception e) {
        log.error("关闭MCP客户端失败: {}", e.getMessage());
      }
    }
    clientMap.clear();

    // 清理锁和条件变量
    dataMap.remove(chatId);
    conditionMap.remove(chatId);
    lockMap.remove(chatId);
  }

  /**
   * 汇总所有服务器的工具描述，名称前添加 serverKey 前缀
   */
  private String combineToolDescriptions(Map<String, McpSyncClient> clientMap) {
    StringBuilder sb = new StringBuilder();
    for (var entry : clientMap.entrySet()) {
      String key = entry.getKey();
      McpSchema.ListToolsResult tools = entry.getValue().listTools();
      for (McpSchema.Tool tool : tools.tools()) {
        sb.append("Tool: ").append(key).append(":").append(tool.name()).append("\n");
        sb.append("  Description: ").append(tool.description()).append("\n");
        sb.append("  Input Schema: ").append(tool.inputSchema()).append("\n");
        sb.append("\n");
      }
    }
    return sb.toString();
  }

  private String buildThoughtLine(String thought) {
    if (thought == null || thought.isBlank()) {
      return "";
    }
    return "\nThought: " + thought;
  }

  private boolean getIncludeThoughtConfig() {
    try {
      List<AdminConfigEntity> configs =
          adminConfigService.findAllBySetKey("ai_mcp_agent_include_thought");
      if (!configs.isEmpty() && configs.get(0).getSetValue() != null) {
        return Boolean.parseBoolean(configs.get(0).getSetValue());
      }
    } catch (Exception e) {
      log.error("从数据库获取MCP Agent thought配置失败", e);
    }
    return includeThought;
  }

  private boolean isFunctionRouterMode() {
    return "function".equalsIgnoreCase(routerMode);
  }

  private boolean isFunctionMcpAgentMode() {
    return "function".equalsIgnoreCase(mcpAgentMode);
  }

  private String runFunctionCalling(String question, Map<String, Object> globalMessage,
      Map<String, McpSyncClient> clientMap, boolean mcpIsTool) {
    String chatId = (String) globalMessage.get("chatId");
    int productId = (int) globalMessage.get("productId");
    Map<String, Queue<String>> queueMap =
        (Map<String, Queue<String>>) globalMessage.get("queueMap");
    Queue<String> queue = queueMap == null ? null : queueMap.get(chatId);
    LLM llm = llmDiyUtility.getDiyLlm(productId, llmName, "10");
    if (!llm.supportsFunctionCalling()) {
      return null;
    }
    if (queue != null) {
      queue.add(ToolPrefix.MCP_AGENT.getPrefix());
    }

    FunctionToolRegistry registry = buildMcpFunctionToolRegistry(productId, chatId, clientMap);
    if (registry.toolSpecs().isEmpty()) {
      cleanupResources(clientMap, chatId);
      return "产品下面没有任何可调用的mcp工具或者skills";
    }
    List<ModelMessage> messages = new ArrayList<>();
    messages.add(new ModelMessage(ModelMessageRole.SYSTEM.value(),
        reactPrompt.getFunctionCalling(question, productId)));
    messages.add(new ModelMessage(ModelMessageRole.USER.value(), question));

    String finalAnswer = null;
    for (int iteration = 0; iteration < epochLimit; iteration++) {
      if (isQueueRemoved(chatId, queueMap)) {
        cleanupResources(clientMap, chatId);
        return "操作已取消";
      }
      FunctionResult result =
          callFunctionLlm(question, messages, registry.toolSpecs(), queueMap, chatId, llm);
      if (result == null || result.isUnsupported() || result.isError()) {
        return null;
      }
      if (result.isDirectReply()) {
        cleanupResources(clientMap, chatId);
        if (queue != null) {
          if (!speedUp) {
            queue.add(result.getReply());
          }
          if (!mcpIsTool) {
            queue.add("[DONE]");
          }
        }
        return result.getReply();
      }
      if (!result.isToolCall()) {
        return null;
      }
      McpFunctionRef functionRef = registry.refMap().get(result.getFunctionName());
      if (functionRef == null) {
        log.warn("Unknown MCP function call: {}", result.getFunctionName());
        return null;
      }
      String assistantText = extractAssistantText(result.getArguments());
      String arguments = normalizeToolArguments(result.getArguments());
      if (queue != null) {
        queue.add(ToolPrefix.getToolCallPrefix(functionRef.fullName()));
      }
      String observation = callMcpFunction(functionRef, arguments, globalMessage, clientMap);
      messages.add(new ModelMessage(ModelMessageRole.ASSISTANT.value(),
          (assistantText.isBlank() ? "" : assistantText + "\n")
              + "Called function " + result.getFunctionName() + " for " + functionRef.fullName()
              + " with arguments: " + arguments));
      messages.add(new ModelMessage(ModelMessageRole.USER.value(),
          "Observation: " + observation));
      finalAnswer = observation;
    }

    cleanupResources(clientMap, chatId);
    String summary = summarizeFunctionConversation(question, productId, messages, finalAnswer);
    if (queue != null) {
      queue.add(summary);
      if (!mcpIsTool) {
        queue.add("[DONE]");
      }
    }
    return summary;
  }

  private FunctionResult callFunctionLlm(String question, List<ModelMessage> messages,
      List<FunctionToolSpec> toolSpecs, Map<String, Queue<String>> queueMap, String chatId,
      LLM llm) {
    if (!speedUp) {
      return llm.functionChat(question, messages, toolSpecs);
    }
    lockMap.computeIfAbsent(chatId, k -> new ReentrantLock());
    conditionMap.computeIfAbsent(chatId, k -> lockMap.get(k).newCondition());
    functionDataMap.remove(chatId);
    Lock lock = lockMap.get(chatId);
    Condition condition = conditionMap.get(chatId);
    StringBuilder replyBuffer = new StringBuilder();

    llm.streamFunctionChat(question, messages, toolSpecs, new FunctionStreamHandler() {
      @Override
      public void onTextDelta(String text) {
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
            : reply));
      }

      @Override
      public void onToolCall(String functionName, String arguments) {
        FunctionResult result = FunctionResult.toolCall(functionName, arguments);
        if (!replyBuffer.isEmpty()) {
          result = FunctionResult.toolCall(functionName,
              mergeAssistantText(arguments, replyBuffer.toString()));
        }
        finish(result);
      }

      @Override
      public void onFailure(Throwable throwable) {
        log.error("MCP Agent function calling stream failed, chatId={}", chatId, throwable);
        if (!replyBuffer.isEmpty()) {
          finish(FunctionResult.directReply(replyBuffer.toString()));
          return;
        }
        finish(FunctionResult.error("function calling stream failed"));
      }

      @Override
      public void onUnsupported() {
        finish(FunctionResult.unsupported());
      }

      private void finish(FunctionResult result) {
        lock.lock();
        try {
          if (!functionDataMap.containsKey(chatId)) {
            functionDataMap.put(chatId, result);
            condition.signalAll();
          }
        } finally {
          lock.unlock();
        }
      }
    });

    lock.lock();
    try {
      while (!functionDataMap.containsKey(chatId)) {
        if (!condition.await(functionTimeoutSeconds, TimeUnit.SECONDS)) {
          functionDataMap.put(chatId, FunctionResult.error("function calling timeout"));
          break;
        }
      }
      return functionDataMap.get(chatId);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return FunctionResult.error("function calling interrupted");
    } finally {
      lock.unlock();
      functionDataMap.remove(chatId);
    }
  }

  private FunctionToolRegistry buildMcpFunctionToolRegistry(int productId, String chatId,
      Map<String, McpSyncClient> clientMap) {
    List<FunctionToolSpec> toolSpecs = new ArrayList<>();
    Map<String, McpFunctionRef> refMap = new LinkedHashMap<>();
    Set<String> usedNames = new HashSet<>();
    for (var entry : clientMap.entrySet()) {
      String serverKey = entry.getKey();
      McpSchema.ListToolsResult tools = entry.getValue().listTools();
      for (McpSchema.Tool tool : tools.tools()) {
        addMcpFunctionTool(toolSpecs, refMap, usedNames, serverKey, tool.name(),
            tool.description(), normalizeSchema(tool.inputSchema()));
      }
    }
    addWebsocketFunctionTools(toolSpecs, refMap, usedNames, McpWebsocket.DEVICE_SERVER_NAME,
        chatId);
    addWebsocketFunctionTools(toolSpecs, refMap, usedNames, McpWebsocket.ENDPOINT_SERVER_NAME,
        "mcp" + productId);
    for (ProductSkillsEntity skill : productSkillsService.findAllByProductId(productId)) {
      addMcpFunctionTool(toolSpecs, refMap, usedNames, "skills", skill.getName(),
          skill.getDescription(), FunctionToolSpec.defaultStringArgsSchema());
    }
    addMcpFunctionTool(toolSpecs, refMap, usedNames, "skills", jsExecuteTool.getName(),
        jsExecuteTool.getDescription(), FunctionToolSpec.defaultStringArgsSchema());
    return new FunctionToolRegistry(toolSpecs, refMap);
  }

  private void addWebsocketFunctionTools(List<FunctionToolSpec> toolSpecs,
      Map<String, McpFunctionRef> refMap, Set<String> usedNames, String serverName,
      String chatId) {
    for (Map<String, Object> tool : mcpWebsocket.getToolDescriptions(serverName, chatId)) {
      String toolName = Objects.toString(tool.get("name"), "");
      if (toolName.isBlank()) {
        continue;
      }
      addMcpFunctionTool(toolSpecs, refMap, usedNames, serverName, toolName,
          Objects.toString(tool.get("description"), ""),
          normalizeSchema(tool.get("inputSchema")));
    }
  }

  private void addMcpFunctionTool(List<FunctionToolSpec> toolSpecs,
      Map<String, McpFunctionRef> refMap, Set<String> usedNames, String serverKey,
      String toolName, String description, Map<String, Object> parameters) {
    String functionName = toFunctionName(serverKey, toolName, usedNames);
    McpFunctionRef functionRef = new McpFunctionRef(serverKey, toolName);
    refMap.put(functionName, functionRef);
    toolSpecs.add(new FunctionToolSpec(functionName,
        (description == null || description.isBlank() ? functionRef.fullName() : description),
        parameters == null || parameters.isEmpty()
            ? FunctionToolSpec.defaultStringArgsSchema()
            : parameters));
  }

  private Map<String, Object> normalizeSchema(Object inputSchema) {
    if (inputSchema instanceof McpSchema.JsonSchema jsonSchema) {
      Map<String, Object> schema = new LinkedHashMap<>();
      schema.put("type", jsonSchema.type() == null ? "object" : jsonSchema.type());
      schema.put("properties",
          jsonSchema.properties() == null ? Map.of() : jsonSchema.properties());
      if (jsonSchema.required() != null) {
        schema.put("required", jsonSchema.required());
      }
      if (jsonSchema.additionalProperties() != null) {
        schema.put("additionalProperties", jsonSchema.additionalProperties());
      }
      return schema;
    }
    if (inputSchema instanceof Map<?, ?> inputMap) {
      Map<String, Object> schema = new LinkedHashMap<>();
      inputMap.forEach((key, value) -> {
        if (key != null) {
          schema.put(String.valueOf(key), value);
        }
      });
      return schema;
    }
    if (inputSchema instanceof String schemaText && !schemaText.isBlank()) {
      try {
        JSONObject jsonObject = JSON.parseObject(schemaText);
        if (jsonObject != null && !jsonObject.isEmpty()) {
          Map<String, Object> schema = new LinkedHashMap<>();
          jsonObject.forEach(schema::put);
          return schema;
        }
      } catch (Exception ignored) {
        // Some websocket clients send display-only schema strings.
      }
    }
    return FunctionToolSpec.defaultStringArgsSchema();
  }

  private String toFunctionName(String serverKey, String toolName, Set<String> usedNames) {
    String raw = "mcp_" + serverKey + "_" + toolName;
    String normalized = raw.replaceAll("[^a-zA-Z0-9_]", "_");
    if (normalized.isBlank() || !Character.isLetter(normalized.charAt(0))) {
      normalized = "mcp_" + normalized;
    }
    if (normalized.length() > 58) {
      normalized = normalized.substring(0, 58);
    }
    String candidate = normalized;
    if (usedNames.contains(candidate)) {
      candidate = normalized + "_" + shortHash(serverKey + ":" + toolName);
    }
    int suffix = 1;
    while (usedNames.contains(candidate)) {
      candidate = normalized + "_" + shortHash(serverKey + ":" + toolName) + "_" + suffix++;
    }
    usedNames.add(candidate);
    return candidate;
  }

  private String shortHash(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < 3; i++) {
        builder.append(String.format("%02x", bytes[i]));
      }
      return builder.toString();
    } catch (Exception e) {
      return Integer.toHexString(value.hashCode()).replace("-", "");
    }
  }

  private String normalizeToolArguments(String arguments) {
    if (arguments == null || arguments.isBlank()) {
      return "{}";
    }
    try {
      JSONObject object = JSON.parseObject(arguments);
      object.remove("_assistant_text");
      String args = object.getString("args");
      if (args != null && object.size() == 1) {
        return args;
      }
      return object.toJSONString();
    } catch (Exception ignored) {
      return arguments;
    }
  }

  private String extractAssistantText(String arguments) {
    if (arguments == null || arguments.isBlank()) {
      return "";
    }
    try {
      JSONObject object = JSON.parseObject(arguments);
      return Objects.toString(object.getString("_assistant_text"), "");
    } catch (Exception ignored) {
      return "";
    }
  }

  private String mergeAssistantText(String arguments, String assistantText) {
    try {
      JSONObject object = arguments == null || arguments.isBlank()
          ? new JSONObject()
          : JSON.parseObject(arguments);
      object.put("_assistant_text", assistantText);
      return object.toJSONString();
    } catch (Exception e) {
      JSONObject object = new JSONObject();
      object.put("args", arguments == null ? "" : arguments);
      object.put("_assistant_text", assistantText);
      return object.toJSONString();
    }
  }

  private String callMcpFunction(McpFunctionRef functionRef, String args,
      Map<String, Object> globalMessage, Map<String, McpSyncClient> clientMap) {
    String chatId = (String) globalMessage.get("chatId");
    int productId = (int) globalMessage.get("productId");
    String serverKey = functionRef.serverKey();
    String toolName = functionRef.toolName();
    try {
      if (serverKey.equals(McpWebsocket.DEVICE_SERVER_NAME)) {
        return mcpWebsocket.sendToolCall(McpWebsocket.DEVICE_SERVER_NAME, chatId,
            toolName, args, XiaoZhiWebsocket.clients.get(chatId), false);
      } else if (serverKey.equals(McpWebsocket.ENDPOINT_SERVER_NAME)) {
        return mcpWebsocket.sendToolCall(McpWebsocket.ENDPOINT_SERVER_NAME, "mcp" + productId,
            toolName, args, McpWebsocketEndpoint.clients.get("mcp" + productId), true);
      } else if (serverKey.equals("skills")) {
        if (toolName.equals(jsExecuteTool.getName())) {
          return jsExecuteTool.run(args, globalMessage);
        }
        List<ProductSkillsEntity> skillsEntity =
            productSkillsService.findAllByProductIdAndName(productId, toolName);
        if (skillsEntity.isEmpty()) {
          return "调用skills失败";
        }
        return productSkillsService.readSkills(skillsEntity.get(0).getFilePath());
      }
      McpSyncClient client = clientMap.get(serverKey);
      if (client == null) {
        return "Unknown server key: " + serverKey;
      }
      McpSchema.CallToolResult result =
          client.callTool(new McpSchema.CallToolRequest(toolName, args));
      return result.content().isEmpty() ? "" : result.content().get(0).toString();
    } catch (Exception e) {
      log.error("MCP function tool call failed: {}", functionRef.fullName(), e);
      return "Error calling tool, please try again";
    }
  }

  private String summarizeFunctionConversation(String question, int productId,
      List<ModelMessage> messages, String fallback) {
    String summaryPrompt = "请根据以上工具调用过程和最终观察结果，总结回答最初的问题: " + question;
    List<ModelMessage> summaryMessages = new ArrayList<>();
    summaryMessages.add(new ModelMessage(ModelMessageRole.SYSTEM.value(), summaryPrompt));
    summaryMessages.add(new ModelMessage(ModelMessageRole.USER.value(), messages.toString()));
    try {
      return llmDiyUtility.getDiyLlm(productId, llmName, "10")
          .commonChat(summaryPrompt, summaryMessages, false);
    } catch (Exception e) {
      log.error("MCP Agent function calling summary failed", e);
      return fallback == null || fallback.isBlank()
          ? "经过多次尝试仍未找到完整答案，请重新提问或提供更多细节。"
          : fallback;
    }
  }

  private record FunctionToolRegistry(List<FunctionToolSpec> toolSpecs,
      Map<String, McpFunctionRef> refMap) {}

  private record McpFunctionRef(String serverKey, String toolName) {
    private String fullName() {
      return serverKey + ":" + toolName;
    }
  }
}
