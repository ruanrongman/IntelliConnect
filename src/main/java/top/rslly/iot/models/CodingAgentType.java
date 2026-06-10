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
package top.rslly.iot.models;

import java.util.Locale;

public enum CodingAgentType {
  CODEX, CLAUDE_CODE;

  public static String normalize(String agentType) {
    if (agentType == null) {
      throw new IllegalArgumentException("agentType is null");
    }
    String normalized = agentType.trim().toUpperCase(Locale.ROOT)
        .replace(' ', '_')
        .replace('-', '_');
    if ("CLAUDECODE".equals(normalized)) {
      return CLAUDE_CODE.name();
    }
    if (CODEX.name().equals(normalized) || CLAUDE_CODE.name().equals(normalized)) {
      return normalized;
    }
    throw new IllegalArgumentException("Unsupported agentType: " + agentType);
  }
}
