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
import top.rslly.iot.utility.JwtTokenUtil;

import javax.annotation.PreDestroy;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint(value = "/mcp", configurator = WebSocketConfig.class)
@Component
@Slf4j
public class McpWebsocketEndpoint {
  public static final Map<String, Session> clients = new ConcurrentHashMap<>();
  private String username;
  private static volatile McpProtocolDeal mcpProtocolDeal;

  @Autowired
  public void setMcpProtocalDeal(McpProtocolDeal mcpProtocolDeal) {
    if (McpWebsocketEndpoint.mcpProtocolDeal == null) {
      McpWebsocketEndpoint.mcpProtocolDeal = mcpProtocolDeal;
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
        mcpProtocolDeal.destroyMcp(McpWebsocket.ENDPOINT_SERVER_NAME, username);
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
    log.info("token {}", token);
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
    String username = JwtTokenUtil.getUsername(token);
    if (username == null) {
      try {
        session.getBasicRemote().sendText("用户不存在");
        session.close();
        return;
      } catch (IOException e) {
        log.info("发送失败");
        return;
      }
    }
    if (!username.startsWith("mcp")) {
      try {
        session.getBasicRemote().sendText("产品不存在");
        session.close();
        return;
      } catch (IOException e) {
        log.info("发送失败");
        return;
      }
    }
    log.info("username {}", username);
    if (clients.get(username) == null) {
      this.username = username;
      clients.put(username, session);
      try {
        session.getBasicRemote().sendText(McpProtocolSend.sendInitialize("", "", true));
      } catch (IOException e) {
        e.printStackTrace();
        log.error("发送失败{}", e.getMessage());
      }
      log.info("token {}", token);
    } else {
      log.info("冲突，无法连接");
      try {
        session.getBasicRemote().sendText("冲突，无法连接");
      } catch (IOException e) {
        log.info("发送失败");
      }
      try {
        session.close();
      } catch (IOException e) {
        log.error("关闭失败{}", e.getMessage());
      }
    }
  }

  @OnClose
  public void onClose() {
    log.info("close");
    if (this.username != null) {
      clients.remove(this.username);
      mcpProtocolDeal.destroyMcp(McpWebsocket.ENDPOINT_SERVER_NAME, this.username);
    }
  }

  /**
   * socket message
   */
  @OnMessage
  public void onMessage(String message) {
    log.info("message {}", message);
    try {
      var payloadObject = JSON.parseObject(message);
      if (!payloadObject.containsKey("jsonrpc")) {
        onClose();
      }
      if (payloadObject.containsKey("result")) {
        String result = payloadObject.getString("result");
        var resultJson = JSON.parseObject(result);
        if (resultJson.containsKey("serverInfo")) {
          // log.info(McpProtocolSend.sendToolList("", true));
          clients.get(username).getBasicRemote().sendText("""
              {"jsonrpc": "2.0","method": "notifications/initialized"}
              """);
          clients.get(username).getBasicRemote().sendText(McpProtocolSend.sendToolList("", true));
        }
        mcpProtocolDeal.dealMcp(resultJson, McpWebsocket.ENDPOINT_SERVER_NAME, username,
            clients.get(username), true);
      }
    } catch (Exception e) {
      log.error("解析失败{}", e.getMessage());
    }

  }

  @OnError
  public void onError(Session session, Throwable error) {
    onClose();
    log.error("Error in onError: {}", error.getMessage());
  }
}
