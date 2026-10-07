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

import com.alibaba.dashscope.utils.Constants;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Applies a custom DashScope endpoint to the native DashScope SDK (TTS/ASR/multimodal voice). The
 * SDK exposes mutable static base URLs, so this must run before the first SDK call.
 */
@Component
@Slf4j
public class DashScopeEndpointConfig {

  @Value("${ai.dashscope-sdk-base-url:}")
  private String sdkBaseUrl;

  @PostConstruct
  public void apply() {
    if (sdkBaseUrl == null || sdkBaseUrl.isBlank()) {
      return;
    }
    String root = sdkBaseUrl.trim().replaceAll("/+$", "");
    String version = Constants.apiVersion == null || Constants.apiVersion.isBlank()
        ? "v1"
        : Constants.apiVersion;
    String websocketRoot = root.startsWith("https://")
        ? "wss://" + root.substring("https://".length())
        : root;
    Constants.baseHttpApiUrl = root + "/api/" + version;
    Constants.baseWebsocketApiUrl = websocketRoot + "/api-ws/" + version + "/inference/";
    log.info("DashScope SDK 自定义端点: http={}, websocket={}",
        Constants.baseHttpApiUrl, Constants.baseWebsocketApiUrl);
  }
}
