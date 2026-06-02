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
package top.rslly.iot.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.rslly.iot.dao.AdminConfigRepository;

import jakarta.annotation.Resource;

@Component
public class McpEndpointConfigService {
  public static final String CONFIG_KEY = "ai_mcp_endpoint_count";
  public static final int MIN_ENDPOINT_COUNT = 1;
  public static final int MAX_ENDPOINT_COUNT = 20;

  @Value("${ai.mcp.endpoint-count:5}")
  private int defaultEndpointCount;

  @Resource
  private AdminConfigRepository adminConfigRepository;

  public int getEndpointCount() {
    var configs = adminConfigRepository.findAllBySetKey(CONFIG_KEY);
    if (!configs.isEmpty()) {
      Integer configValue = parseEndpointCount(configs.get(0).getSetValue());
      if (configValue != null) {
        return configValue;
      }
    }
    return normalizeEndpointCount(defaultEndpointCount);
  }

  public int getToolsLimit(int defaultLimit) {
    var configs = adminConfigRepository.findAllBySetKey("mcp_tools_limit");
    if (!configs.isEmpty()) {
      try {
        return Integer.parseInt(configs.get(0).getSetValue());
      } catch (NumberFormatException e) {
        return defaultLimit;
      }
    }
    return defaultLimit;
  }

  public boolean isValidEndpointIndex(int endpointIndex) {
    return endpointIndex >= MIN_ENDPOINT_COUNT && endpointIndex <= getEndpointCount();
  }

  public static Integer parseEndpointCount(String value) {
    try {
      int endpointCount = Integer.parseInt(value);
      if (endpointCount < MIN_ENDPOINT_COUNT || endpointCount > MAX_ENDPOINT_COUNT) {
        return null;
      }
      return endpointCount;
    } catch (Exception e) {
      return null;
    }
  }

  private int normalizeEndpointCount(int endpointCount) {
    if (endpointCount < MIN_ENDPOINT_COUNT) {
      return MIN_ENDPOINT_COUNT;
    }
    if (endpointCount > MAX_ENDPOINT_COUNT) {
      return MAX_ENDPOINT_COUNT;
    }
    return endpointCount;
  }
}
