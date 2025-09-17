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

import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
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
