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
package top.rslly.iot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;
import java.util.List;
import java.util.Map;

@Configuration
public class WebSocketConfig extends ServerEndpointConfig.Configurator {
  @Bean
  public ServerEndpointExporter serverEndpointExporter() {

    return new ServerEndpointExporter();
  }

  /**
   * 建立握手时，连接前的操作
   */
  @Override
  public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request,
      HandshakeResponse response) {
    // 这个userProperties 可以通过 session.getUserProperties()获取
    final Map<String, Object> userProperties = sec.getUserProperties();
    Map<String, List<String>> headers = request.getHeaders();
    if (headers.containsKey("Authorization")) {
      List<String> Authorization = headers.get("Authorization");
      userProperties.put("Authorization", Authorization.get(0));
    }
    if (headers.containsKey("Protocol-Version")) {
      List<String> header1 = headers.get("Protocol-Version");
      userProperties.put("Protocol-Version", header1.get(0));
    }
    if (headers.containsKey("Device-Id")) {
      List<String> header2 = headers.get("Device-Id");
      userProperties.put("Device-Id", header2.get(0));
    }
    if (headers.containsKey("Client-Id")) {
      List<String> header3 = headers.get("Client-Id");
      userProperties.put("Client-Id", header3.get(0));
    }
    // 获取客户端真实IP地址
    String clientIp = getRemoteAddr(request);
    userProperties.put("Client-Ip", clientIp);
  }

  /**
   * 获取客户端真实IP地址，支持反向代理场景
   */
  private String getRemoteAddr(HandshakeRequest request) {
    Map<String, List<String>> headers = request.getHeaders();

    // 1. 优先检查 X-Forwarded-For（反向代理常用）
    String ip = getHeaderFirst(headers, "X-Forwarded-For");
    if (isValidIp(ip)) {
      // X-Forwarded-For 格式: client1, proxy1, proxy2，取第一个
      int index = ip.indexOf(',');
      if (index > 0) {
        ip = ip.substring(0, index).trim();
      }
      return ip;
    }

    // 2. 检查 X-Real-IP（Nginx 常用）
    ip = getHeaderFirst(headers, "X-Real-IP");
    if (isValidIp(ip)) {
      return ip;
    }

    // 3. 检查 Proxy-Client-IP（Apache 服务器常用）
    ip = getHeaderFirst(headers, "Proxy-Client-IP");
    if (isValidIp(ip)) {
      return ip;
    }

    // 4. 检查 WL-Proxy-Client-IP（WebLogic 常用）
    ip = getHeaderFirst(headers, "WL-Proxy-Client-IP");
    if (isValidIp(ip)) {
      return ip;
    }

    // 5. 最后尝试从 Host 获取（开发环境直连）
    ip = getHeaderFirst(headers, "Host");
    if (ip != null && ip.contains(":")) {
      ip = ip.substring(0, ip.indexOf(":"));
    }

    return ip != null ? ip : "unknown";
  }

  private String getHeaderFirst(Map<String, List<String>> headers, String headerName) {
    List<String> values = headers.get(headerName);
    return (values != null && !values.isEmpty()) ? values.get(0) : null;
  }

  private boolean isValidIp(String ip) {
    return ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)
        && !"null".equalsIgnoreCase(ip);
  }

  @Bean
  public ServletServerContainerFactoryBean createWebSocketContainer() {
    ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
    // 在此处设置bufferSize
    container.setMaxTextMessageBufferSize(5120000);
    container.setMaxBinaryMessageBufferSize(5120000);
    container.setMaxSessionIdleTimeout(15 * 600000L);
    return container;
  }

}
