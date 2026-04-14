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
package top.rslly.iot.utility.ai.chain;

import java.util.Arrays;
import java.util.Optional;

public enum FunctionRouterRoute {
  WEATHER("1", "route_weather", "Query weather"), CONTROL("2", "route_control",
      "Control and query electrical, excluding music and xiaozhi_device"), MUSIC("3", "route_music",
          "Play or recommend music"), AGENT("4", "route_agent",
              "Complex multi-step task"), WX_BOUND_PRODUCT("6", "route_bind_product",
                  "Bind or unbind products"), WX_PRODUCT_ACTIVE("7", "route_switch_product",
                      "Switch controlled products"), SCHEDULE("8", "route_schedule",
                          "Schedule or reminder"), PRODUCT_ROLE("9", "route_role",
                              "Role or voice configuration"), MCP("10", "route_mcp",
                                  "Use MCP capability"), GOODBYE("11", "route_step_back",
                                      "Say goodbye or step down");

  private final String taskId;
  private final String functionName;
  private final String defaultDescription;

  FunctionRouterRoute(String taskId, String functionName, String defaultDescription) {
    this.taskId = taskId;
    this.functionName = functionName;
    this.defaultDescription = defaultDescription;
  }

  public String getTaskId() {
    return taskId;
  }

  public String getFunctionName() {
    return functionName;
  }

  public String getDefaultDescription() {
    return defaultDescription;
  }

  public static Optional<FunctionRouterRoute> fromFunctionName(String functionName) {
    return Arrays.stream(values())
        .filter(route -> route.functionName.equals(functionName))
        .findFirst();
  }
}
