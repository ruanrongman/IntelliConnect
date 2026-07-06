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
import com.alibaba.fastjson.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.rslly.iot.services.UserConfigServiceImpl;
import top.rslly.iot.utility.RedisUtil;

import jakarta.websocket.Session;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
public class McpWebsocket {
  public static final String DEVICE_SERVER_NAME = "xiaozhi_device";
  public static final String ENDPOINT_SERVER_NAME = "xiaozhi_endpoint";
  public static final String ENDPOINT_SERVER_NAME_PREFIX = "xiaozhi_endpoint_";
  private static final String TimeOut_LIMIT_CONFIG_KEY = "mcp.time_out_limit";
  private static final int MAX_TimeOut_LIMIT = 3600;
  @Autowired
  private RedisUtil redisUtil;
  @Autowired
  private UserConfigServiceImpl userConfigService;


  // 工具调用锁的前缀
  private static final String TOOL_CALL_LOCK_PREFIX = "mcp:tool:call:";
  // 锁的过期时间（秒） - 比实际等待时间稍长
  private static final long LOCK_EXPIRE_TIME = 15L;
  private static final Map<String, Lock> SESSION_SEND_LOCKS = new ConcurrentHashMap<>();

  public static String getEndpointServerName(int endpointIndex) {
    return ENDPOINT_SERVER_NAME_PREFIX + endpointIndex;
  }

  public static String getEndpointChatId(int productId, int endpointIndex) {
    return "mcp" + productId + "_endpoint" + endpointIndex;
  }

  public static boolean isEndpointServerName(String serverName) {
    return serverName != null && serverName.startsWith(ENDPOINT_SERVER_NAME_PREFIX);
  }

  private int getMcpTimeOutLimit(int productId, String configKey, int defaultValue) {
    String value = userConfigService.getConfigValue(productId, configKey);
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    try {
      int limit = Integer.parseInt(value.trim());
      if (limit < 0) {
        return defaultValue;
      }
      return Math.min(MAX_TimeOut_LIMIT, limit);
    } catch (NumberFormatException e) {
      log.warn("Invalid mcp timeout limit config: productId={}, key={}, value={}",
          productId, configKey, value);
      return defaultValue;
    }
  }

  public static int parseEndpointIndexFromServerName(String serverName) {
    if (!isEndpointServerName(serverName)) {
      return -1;
    }
    try {
      return Integer.parseInt(serverName.substring(ENDPOINT_SERVER_NAME_PREFIX.length()));
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  public String combineToolDescription(String serverName, String chatId) {
    List<Map<String, Object>> toolDescriptionMap =
        (List<Map<String, Object>>) redisUtil.get(serverName + chatId);
    if (toolDescriptionMap == null)
      return "fail get TooList";
    StringBuilder sb = new StringBuilder();
    for (Map<String, Object> toolDescription : toolDescriptionMap) {
      sb.append("Tool: ").append(serverName).append(":").append(toolDescription.get("name"))
          .append("\n");
      sb.append("  Description: ").append(toolDescription.get("description")).append("\n");
      sb.append("  Input Schema: ").append(toolDescription.get("inputSchema")).append("\n");
      sb.append("\n");
    }
    // log.info("getTools: " + sb);
    return sb.toString();
  }

  public List<Map<String, Object>> getToolDescriptions(String serverName, String chatId) {
    List<Map<String, Object>> toolDescriptionMap =
        (List<Map<String, Object>>) redisUtil.get(serverName + chatId);
    return toolDescriptionMap == null ? List.of() : toolDescriptionMap;
  }

  public String getIntention(String serverName, String chatId) {
    List<Map<String, Object>> toolDescriptionMap =
        (List<Map<String, Object>>) redisUtil.get(serverName + chatId);
    if (toolDescriptionMap == null)
      return "no Intention";
    StringBuilder sb = new StringBuilder();
    for (Map<String, Object> toolDescription : toolDescriptionMap) {
      sb.append(serverName).append(":").append(toolDescription.get("name")).append("|");
    }
    // log.info("intent{}", sb);
    return sb.toString();
  }

  public String sendToolCall(String serverName, String chatId, String toolName,
      String jsonArguments, Session session, int productId, boolean endpoint) {
    if (jsonArguments == null || jsonArguments.trim().isEmpty()) {
      throw new IllegalArgumentException("jsonArguments cannot be null or empty");
    }
    // 为每个工具调用创建唯一的锁key
    String lockKey = TOOL_CALL_LOCK_PREFIX + serverName + ":" + chatId + ":" + toolName;
    String requestId = UUID.randomUUID().toString();

    // 尝试获取分布式锁
    boolean locked = redisUtil.tryLock(lockKey, requestId, LOCK_EXPIRE_TIME);

    if (!locked) {
      log.warn("工具调用正在进行中，serverName: {}, chatId: {}, toolName: {}",
          serverName, chatId, toolName);
      return "tool is being called by another request, please wait";
    }
    try {
      Map<String, Object> params = JSON.parseObject(
          jsonArguments,
          new TypeReference<>() {});
      String jsonStr = McpProtocolSend.callTool(toolName, params, endpoint);
      sendTextSerially(serverName, chatId, session, jsonStr);
      int timeOutLimit = getMcpTimeOutLimit(
          productId,
          TimeOut_LIMIT_CONFIG_KEY,
          12) * 10;
      for (int i = 0; i < timeOutLimit; i++) {
        try {
          if (redisUtil.hasKey(serverName + chatId + "toolResult")) {
            String result = redisUtil.get(serverName + chatId + "toolResult").toString();
            redisUtil.del(serverName + chatId + "toolResult");
            return result;
          }
          Thread.sleep(100);
        } catch (Exception e) {
          log.error(e.getMessage());
          return "calling this tool error";
        }
      }
      return "calling this tool timeout,please try again";
    } catch (Exception e) {
      e.printStackTrace();
      return "calling this tool error";
    } finally {
      // 确保释放锁
      boolean released = redisUtil.releaseLock(lockKey, requestId);
      if (!released) {
        log.error("释放工具调用锁失败，serverName: {}, chatId: {}, toolName: {}, requestId: {}",
            serverName, chatId, toolName, requestId);
      }
    }
  }

  public boolean isRunning(String serverName, String chatId) {
    String key = serverName + chatId;
    return redisUtil.hasKey(key);
  }

  private void sendTextSerially(String serverName, String chatId, Session session, String text)
      throws java.io.IOException {
    if (session == null || !session.isOpen()) {
      throw new java.io.IOException("websocket session is not open");
    }
    String sessionKey = serverName + ":" + chatId + ":" + session.getId();
    Lock lock = SESSION_SEND_LOCKS.computeIfAbsent(sessionKey, ignored -> new ReentrantLock());
    lock.lock();
    try {
      session.getBasicRemote().sendText(text);
    } finally {
      lock.unlock();
      if (!session.isOpen()) {
        SESSION_SEND_LOCKS.remove(sessionKey, lock);
      }
    }
  }
}
