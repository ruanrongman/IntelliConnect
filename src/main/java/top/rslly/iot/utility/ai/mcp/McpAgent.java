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
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.rslly.iot.services.agent.McpServerServiceImpl;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.ModelMessageRole;
import top.rslly.iot.utility.ai.llm.LLMFactory;
import top.rslly.iot.utility.ai.prompts.ReactPrompt;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class McpAgent {
  @Value("${ai.mcp.agent-llm}")
  private String llmName;

  @Value("${ai.mcp.agent-epoch-limit:8}")
  private int epochLimit;

  @Autowired
  private ReactPrompt reactPrompt;
  @Autowired
  private McpServerServiceImpl mcpServerService;

  /**
   * Map<serverKey, McpSyncClient>
   */
  private final Map<String, McpSyncClient> clientMap = new LinkedHashMap<>();

  private final StringBuffer conversationPrompt = new StringBuffer();

  public void initClients(int productId) throws Exception {
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
        clientMap.put(key, client);
        log.info("Initialized MCP server [{}] at {}", key, url);
      } catch (Exception e) {
        log.error("Failed to initialize MCP server [{}] at {}", key, url);
      }
    }
    if (clientMap.isEmpty()) {
      throw new IllegalArgumentException("No MCP server found for productId: " + productId);
    }
  }

  public String run(String question, Map<String, Object> globalMessage) {
    int productId = (int) globalMessage.get("productId");
    if (mcpServerService.findAllByProductId(productId).isEmpty())
      return "产品下面没有任何mcp服务器，请先绑定mcp服务器";
    try {
      initClients(productId);
    } catch (Exception e) {
      return "连接mcp服务器失败";
    }
    // 构建 system prompt，包括所有服务器的工具描述
    String system = reactPrompt.getReact(combineToolDescriptions(), question);
    // log.info("system: {}", system);

    List<ModelMessage> messages = new ArrayList<>();
    String toolResult = "";
    conversationPrompt.setLength(0);
    conversationPrompt.append(system);

    int iteration = 0;
    while (iteration < epochLimit) {
      messages.clear();
      messages
          .add(new ModelMessage(ModelMessageRole.SYSTEM.value(), conversationPrompt.toString()));
      messages.add(new ModelMessage(ModelMessageRole.USER.value(), question));

      var obj = LLMFactory.getLLM(llmName).jsonChat(question, messages, false);
      String thought = obj.getString("thought");
      String answer = obj.getJSONObject("action").getString("answer");

      try {
        var action = obj.getJSONObject("action");
        String fullName = action.getString("name");
        String args = action.getString("args");

        // 格式: serverKey:toolName
        String[] parts = fullName.split(":", 2);
        if (parts.length != 2 || parts[0].equalsIgnoreCase("finish")) {
          // 释放mcp客户端
          for (var client : clientMap.values()) {
            client.close();
          }
          // finish or invalid format
          return JSON.parseObject(args).getString("content");
        }
        String serverKey = parts[0];
        String toolName = parts[1];
        McpSyncClient client = clientMap.get(serverKey);
        if (client == null) {
          throw new IllegalArgumentException("Unknown server key: " + serverKey);
        }

        // 调用指定服务器的工具
        McpSchema.CallToolResult result = client.callTool(
            new McpSchema.CallToolRequest(toolName, args));
        toolResult = result.content().get(0).toString();

        // 更新对话
        conversationPrompt.append(obj.toJSONString());
        conversationPrompt.append("\nObservation: ").append(toolResult).append("\n");
        log.info("Thought: {}", thought);
        log.info("Observation ({}): {}", fullName, toolResult);

      } catch (Exception e) {
        log.error("Error during tool call", e);
        return answer;
      }
      iteration++;
    }
    // 释放mcp客户端
    for (var client : clientMap.values()) {
      client.close();
    }
    return toolResult;
  }

  /**
   * 汇总所有服务器的工具描述，名称前添加 serverKey 前缀
   */
  private String combineToolDescriptions() {
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
