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
package top.rslly.iot.utility.ai.tools;

public enum ToolPrefix {
  WEATHER("以下是高德天气插件结果："), CONTROL("以下是智能控制插件结果："), MUSIC("以下是网易云音乐插件结果："), AGENT(
      "以下是智能体处理结果："), WX_BOUND_PRODUCT("以下是微信绑定产品插件结果："), WX_PRODUCT_ACTIVE(
          "以下是微信切换产品插件结果："), SCHEDULE("以下是定时任务插件的结果："), PRODUCT_ROLE(
              "以下是产品角色插件的结果："), MCP_AGENT("以下是mcp智能体的结果："), GOODBYE("以下是byebye插件的结果：");

  private final String prefix;

  ToolPrefix(String prefix) {
    this.prefix = prefix;
  }

  public String getPrefix() {
    return prefix;
  }

  // 检查文本是否以任何插件前缀开头
  public static boolean startsWithAnyPrefix(String text) {
    for (ToolPrefix toolPrefix : values()) {
      if (text.startsWith(toolPrefix.getPrefix())) {
        return true;
      }
    }
    return false;
  }
}
