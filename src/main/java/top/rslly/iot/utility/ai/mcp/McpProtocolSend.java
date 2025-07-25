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
import com.alibaba.fastjson.JSONObject;
import top.rslly.iot.utility.ai.mcp.protocol.Initialize;
import top.rslly.iot.utility.ai.mcp.protocol.ToolCall;
import top.rslly.iot.utility.ai.mcp.protocol.ToolList;

import java.util.HashMap;
import java.util.Map;

public class McpProtocolSend {
  public static String sendInitialize(String url, String token) {
    // {
    // "jsonrpc": "2.0",
    // "method": "initialize",
    // "params": {
    // "capabilities": {
    // // 客户端能力，可选
    //
    // // 摄像头视觉相关
    // "vision": {
    // "url": "...", //摄像头: 图片处理地址(必须是http地址, 不是websocket地址)
    // "token": "..." // url token
    // }
    //
    // // ... 其他客户端能力
    // }
    // },
    // "id": 1 // 请求 ID
    // }
    Initialize initialize = new Initialize();
    initialize.setId(1);
    Map<String, Object> initializeParams = initialize.getParams();
    Map<String, Object> initializeVision = new HashMap<>();
    Map<String, Object> capabilities = new HashMap<>();
    initializeVision.put("url", url);
    initializeVision.put("token", token);
    capabilities.put("vision", initializeVision);
    initializeParams.put("capabilities", capabilities);
    JSONObject responseObject = new JSONObject();
    responseObject.put("type", "mcp");
    responseObject.put("payload", JSON.parseObject(JSON.toJSONString(initialize)));
    return responseObject.toString();
  }

  public static String sendToolList(String cursor) {
    ToolList toolList = new ToolList();
    toolList.setId(1);
    toolList.params.put("cursor", cursor);
    JSONObject responseObject = new JSONObject();
    responseObject.put("type", "mcp");
    responseObject.put("payload", JSON.parseObject(JSON.toJSONString(toolList)));
    return responseObject.toString();
  }

  public static String callTool(String toolName, Map<String, Object> arguments) {
    ToolCall toolCall = new ToolCall();
    toolCall.params.put("name", toolName);
    toolCall.params.put("arguments", arguments);
    JSONObject responseObject = new JSONObject();
    responseObject.put("type", "mcp");
    responseObject.put("payload", JSON.parseObject(JSON.toJSONString(toolCall)));
    return responseObject.toString();
  }
}
