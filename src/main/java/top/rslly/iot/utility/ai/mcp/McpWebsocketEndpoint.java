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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.rslly.iot.config.WebSocketConfig;
import top.rslly.iot.services.McpEndpointConfigService;
import top.rslly.iot.utility.JwtTokenUtil;

import jakarta.annotation.PreDestroy;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint(value = "/mcp", configurator = WebSocketConfig.class)
@Component
@Slf4j
public class McpWebsocketEndpoint {
  public static final Map<String, Session> clients = new ConcurrentHashMap<>();
  private String username;
  private String serverName;
  private Session session;
  private static volatile McpProtocolDeal mcpProtocolDeal;
  private static volatile McpEndpointConfigService mcpEndpointConfigService;

  @Autowired
  public void setMcpProtocalDeal(McpProtocolDeal mcpProtocolDeal) {
    if (McpWebsocketEndpoint.mcpProtocolDeal == null) {
      McpWebsocketEndpoint.mcpProtocolDeal = mcpProtocolDeal;
    }
  }

  @Autowired
  public void setMcpEndpointConfigService(McpEndpointConfigService mcpEndpointConfigService) {
    if (McpWebsocketEndpoint.mcpEndpointConfigService == null) {
      McpWebsocketEndpoint.mcpEndpointConfigService = mcpEndpointConfigService;
    }
  }

  public static void closeEndpointsAbove(int maxEndpointIndex) {
    if (mcpProtocolDeal == null) {
      return;
    }
    for (Map.Entry<String, Session> entry : new HashMap<>(clients).entrySet()) {
      String username = entry.getKey();
      int endpointIndex = parseEndpointIndex(username);
      if (endpointIndex <= maxEndpointIndex) {
        continue;
      }
      Session session = entry.getValue();
      clients.remove(username, session);
      closeSession(session, "MCP endpoint count was reduced");
      mcpProtocolDeal.destroyMcp(McpWebsocket.getEndpointServerName(endpointIndex), username);
    }
  }

  @PreDestroy
  public void destroy() {
    log.info("开始销毁 MCP WebSocket 资源");

    if (mcpProtocolDeal != null) {
      for (Map.Entry<String, Session> entry : new HashMap<>(clients).entrySet()) {
        String username = entry.getKey();
        Session session = entry.getValue();
        if (session.isOpen()) {
          try {
            session.close(new CloseReason(
                CloseReason.CloseCodes.NORMAL_CLOSURE, "Server shutdown"));
          } catch (IOException e) {
            log.error("关闭会话失败: {}", e.getMessage());
          }
        }
        mcpProtocolDeal.destroyMcp(resolveServerName(username), username);
      }
    }

    clients.clear();
    log.info("MCP WebSocket 资源销毁完成");
  }

  @OnOpen
  public void onOpen(Session session) {
    // 从URL查询参数中获取token
    String token = session.getRequestParameterMap().get("token") != null
        ? session.getRequestParameterMap().get("token").get(0)
        : null;
    // log.info("token {}", token);
    if (token == null) {
      try {
        session.getBasicRemote().sendText("token为null");
        session.close();
        return;
      } catch (IOException e) {
        log.info("发送失败");
        return;
      }
    }
    if (JwtTokenUtil.checkJWT(token) == null) {
      try {
        session.getBasicRemote().sendText("token无效");
        session.close();
        return;
      } catch (IOException e) {
        log.info("发送失败");
        return;
      }
    }
    String rawUsername = JwtTokenUtil.getUsername(token);
    if (rawUsername == null) {
      try {
        session.getBasicRemote().sendText("用户不存在");
        session.close();
        return;
      } catch (IOException e) {
        log.info("发送失败");
        return;
      }
    }
    String username = normalizeUsername(rawUsername);
    int endpointIndex = parseEndpointIndex(username);
    if (username == null || endpointIndex < 1) {
      try {
        session.getBasicRemote().sendText("产品不存在");
        session.close();
        return;
      } catch (IOException e) {
        log.info("发送失败");
        return;
      }
    }
    if (mcpEndpointConfigService != null
        && !mcpEndpointConfigService.isValidEndpointIndex(endpointIndex)) {
      try {
        session.getBasicRemote().sendText("MCP端点不存在");
        session.close();
        return;
      } catch (IOException e) {
        log.info("发送失败");
        return;
      }
    }
    log.info("username {}", username);
    this.username = username;
    this.serverName = McpWebsocket.getEndpointServerName(endpointIndex);
    this.session = session;
    Session oldSession = clients.put(username, session);
    if (oldSession != null && oldSession != session) {
      closeSession(oldSession, "Replaced by new MCP endpoint connection");
      if (mcpProtocolDeal != null) {
        mcpProtocolDeal.destroyMcp(this.serverName, username);
      }
      log.info("MCP endpoint reconnect replaced old session, username={}", username);
    }
    try {
      session.getBasicRemote().sendText(McpProtocolSend.sendInitialize("", "", true));
    } catch (IOException e) {
      cleanupSession(session);
      log.error("发送失败{}", e.getMessage());
    }
    log.info("token {}", token);
  }

  @OnClose
  public void onClose(Session session) {
    log.info("close");
    cleanupSession(session);
  }

  /**
   * socket message
   */
  @OnMessage
  public void onMessage(String message) {
    // log.info("message {}", message);
    try {
      var payloadObject = JSON.parseObject(message);
      if (!payloadObject.containsKey("jsonrpc")) {
        cleanupSession(session);
        return;
      }
      if (payloadObject.containsKey("result")) {
        String result = payloadObject.getString("result");
        var resultJson = JSON.parseObject(result);
        if (resultJson.containsKey("serverInfo")) {
          // log.info(McpProtocolSend.sendToolList("", true));
          session.getBasicRemote().sendText("""
              {"jsonrpc": "2.0","method": "notifications/initialized"}
              """);
          session.getBasicRemote().sendText(McpProtocolSend.sendToolList("", true));
        }
        mcpProtocolDeal.dealMcp(resultJson, serverName, username, session, true);
      }
    } catch (Exception e) {
      log.error("解析失败{}", e.getMessage());
    }

  }

  @OnError
  public void onError(Session session, Throwable error) {
    cleanupSession(session);
    log.error("Error in onError: {}", error.getMessage());
  }

  private void cleanupSession(Session closedSession) {
    if (this.username == null) {
      return;
    }
    Session sessionToRemove = closedSession != null ? closedSession : this.session;
    boolean removed = clients.remove(this.username, sessionToRemove);
    if (removed && mcpProtocolDeal != null) {
      mcpProtocolDeal.destroyMcp(this.serverName, this.username);
    }
  }

  private static void closeSession(Session session, String reason) {
    if (session == null || !session.isOpen()) {
      return;
    }
    try {
      session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, reason));
    } catch (IOException e) {
      log.error("关闭旧MCP endpoint会话失败: {}", e.getMessage());
    }
  }

  private static String normalizeUsername(String username) {
    if (username == null) {
      return null;
    }
    if (username.matches("mcp\\d+")) {
      return username + "_endpoint1";
    }
    if (username.matches("mcp\\d+_endpoint\\d+")) {
      return username;
    }
    return null;
  }

  private static int parseEndpointIndex(String username) {
    String normalized = normalizeUsername(username);
    if (normalized == null) {
      return -1;
    }
    try {
      return Integer.parseInt(normalized.substring(normalized.lastIndexOf("_endpoint") + 9));
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  private static String resolveServerName(String username) {
    int endpointIndex = parseEndpointIndex(username);
    if (endpointIndex < 1) {
      return McpWebsocket.getEndpointServerName(1);
    }
    return McpWebsocket.getEndpointServerName(endpointIndex);
  }
}
