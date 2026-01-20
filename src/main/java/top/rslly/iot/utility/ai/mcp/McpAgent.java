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
import top.rslly.iot.services.agent.LlmProviderInformationServiceImpl;
import top.rslly.iot.services.agent.McpServerServiceImpl;
import top.rslly.iot.services.agent.ProductLlmModelServiceImpl;
import top.rslly.iot.utility.ai.LlmDiyUtility;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.ModelMessageRole;
import top.rslly.iot.utility.ai.llm.LLM;
import top.rslly.iot.utility.ai.llm.LLMFactory;
import top.rslly.iot.utility.ai.prompts.ReactPrompt;
import top.rslly.iot.utility.ai.toolAgent.AgentEventSourceListener;
import top.rslly.iot.utility.ai.tools.BaseTool;
import top.rslly.iot.utility.ai.tools.ToolPrefix;
import top.rslly.iot.utility.smartVoice.XiaoZhiWebsocket;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
  private String name = "mcpAgent";

  @Autowired
  private ReactPrompt reactPrompt;
  @Autowired
  private McpServerServiceImpl mcpServerService;
  @Autowired
  private McpWebsocket mcpWebsocket;
  @Autowired
  private LlmDiyUtility llmDiyUtility;

  // 添加锁和条件变量映射（与ChatTool保持一致）
  private final Map<String, Lock> lockMap = new ConcurrentHashMap<>();
  private final Map<String, Condition> conditionMap = new ConcurrentHashMap<>();
  private final Map<String, String> dataMap = new ConcurrentHashMap<>();

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
    if (mcpServerEntities.isEmpty() && !mcpWebSocketRunning && !mcpEndpointRunning) {
      return "工具无效，请勿调用";
    }
    StringBuilder toolDescriptions = new StringBuilder();
    for (var mcpServerEntity : mcpServerEntities) {
      toolDescriptions.append(mcpServerEntity.getDescription()).append("|");
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
        && !mcpWebsocket.isRunning(McpWebsocket.DEVICE_SERVER_NAME, chatId)
        && !mcpWebsocket.isRunning(McpWebsocket.ENDPOINT_SERVER_NAME, "mcp" + productId))
      return "产品下面没有任何mcp服务器，请先绑定mcp服务器";
    if (!mcpServerService.findAllByProductId(productId).isEmpty()) {
      try {
        initClients(productId, clientMap);
      } catch (Exception e) {
        return "连接mcp服务器失败";
      }
    }

    // 初始化当前会话的锁和条件变量
    lockMap.putIfAbsent(chatId, new ReentrantLock());
    conditionMap.putIfAbsent(chatId, lockMap.get(chatId).newCondition());

    // 构建 system prompt，包括所有服务器的工具描述
    String system;
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
      system = reactPrompt.getReact(toolDescriptions, question, productId);
    } catch (Exception e) {
      return "获取工具描述失败";
    }
    Map<String, Queue<String>> queueMap =
        (Map<String, Queue<String>>) globalMessage.get("queueMap");
    var queue = queueMap.get(chatId);
    if (queue != null) {
      queue.add(ToolPrefix.MCP_AGENT.getPrefix());
      queue.add(getRandomComfortPhrase());
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
      messages.clear();
      messages
          .add(new ModelMessage(ModelMessageRole.SYSTEM.value(), conversationPrompt.toString()));
      messages.add(new ModelMessage(ModelMessageRole.USER.value(), question));

      var obj = callLLMForThought(question, messages, queueMap, chatId, productId);
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
          conversationPrompt.append("\nObservation: ").append(toolResult).append("\n");
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
        conversationPrompt.append(obj.toJSONString());
        conversationPrompt.append("\nObservation: ").append(toolResult).append("\n");
        if (queue != null) {
          queue.add(ToolPrefix.ToolCall.getPrefix());
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
      Map<String, Queue<String>> queueMap, String chatId, int productId) {
    LLM llm = llmDiyUtility.getDiyLlm(productId, llmName, "10");
    if (speedUp) {
      // 使用流式调用，实时获取thought内容
      dataMap.remove(chatId);

      try {
        llm.streamJsonChat(question, messages, false,
            new AgentEventSourceListener(queueMap, chatId, this));

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
}
