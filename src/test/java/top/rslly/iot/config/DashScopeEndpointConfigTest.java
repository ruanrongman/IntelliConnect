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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.alibaba.dashscope.utils.Constants;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DashScopeEndpointConfigTest {

  @Test
  void appliesCustomEndpointToSdkConstants() {
    String originalHttp = Constants.baseHttpApiUrl;
    String originalWebsocket = Constants.baseWebsocketApiUrl;
    try {
      DashScopeEndpointConfig config = new DashScopeEndpointConfig();
      ReflectionTestUtils.setField(config, "sdkBaseUrl", "https://dashscope-intl.aliyuncs.com/");
      config.apply();

      String version = Constants.apiVersion == null || Constants.apiVersion.isBlank()
          ? "v1"
          : Constants.apiVersion;
      assertEquals("https://dashscope-intl.aliyuncs.com/api/" + version,
          Constants.baseHttpApiUrl);
      assertEquals("wss://dashscope-intl.aliyuncs.com/api-ws/" + version + "/inference/",
          Constants.baseWebsocketApiUrl);
    } finally {
      Constants.baseHttpApiUrl = originalHttp;
      Constants.baseWebsocketApiUrl = originalWebsocket;
    }
  }

  @Test
  void blankBaseUrlKeepsSdkDefaults() {
    String originalHttp = Constants.baseHttpApiUrl;
    String originalWebsocket = Constants.baseWebsocketApiUrl;
    try {
      DashScopeEndpointConfig config = new DashScopeEndpointConfig();
      ReflectionTestUtils.setField(config, "sdkBaseUrl", "  ");
      config.apply();

      assertEquals(originalHttp, Constants.baseHttpApiUrl);
      assertEquals(originalWebsocket, Constants.baseWebsocketApiUrl);
    } finally {
      Constants.baseHttpApiUrl = originalHttp;
      Constants.baseWebsocketApiUrl = originalWebsocket;
    }
  }
}
