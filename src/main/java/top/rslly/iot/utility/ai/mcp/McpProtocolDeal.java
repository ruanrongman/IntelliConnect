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
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.rslly.iot.utility.RedisUtil;

import javax.websocket.Session;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class McpProtocolDeal {
  @Autowired
  private RedisUtil redisUtil;

  public void dealMcp(JSONObject resultObject, String serverName, String chatId, Session session,
      boolean endpoint) throws IOException {
    if (resultObject.containsKey("content")) {
      if (resultObject.containsKey("isError")) {
        if (resultObject.getBoolean("isError")) {
          redisUtil.set(serverName + chatId + "toolResult", "call tool error");
        } else {
          log.info("result{}", resultObject.getString("content"));
          redisUtil.set(serverName + chatId + "toolResult",
              resultObject.getString("content"));
        }
      } else {
        log.info("result{}", resultObject.getString("content"));
        redisUtil.set(serverName + chatId + "toolResult",
            resultObject.getString("content"));
      }
    }
    if (resultObject.containsKey("tools")) {
      if (resultObject.containsKey("error")) {
        redisUtil.set(serverName + chatId + "toolResult", "call tool error");
        log.error("tool调用错误:{}", resultObject.getJSONObject("error").getString("message"));
      }
    }
    if (resultObject.containsKey("tools")) {
      String tools = resultObject.getString("tools");
      String nextCursor = resultObject.getString("nextCursor");
      List<Map<String, Object>> toolList;
      if (redisUtil.hasKey(serverName + chatId)) {
        toolList = (List<Map<String, Object>>) redisUtil.get(serverName + chatId);
        if (toolList == null) {
          toolList = new ArrayList<>();
        }
      } else
        toolList = new ArrayList<>();
      JSONArray toolArray = JSON.parseArray(tools);
      for (int i = 0; i < toolArray.size(); i++) {
        Map<String, Object> tool = new HashMap<>();
        tool.put("name", toolArray.getJSONObject(i).getString("name"));
        tool.put("description", toolArray.getJSONObject(i).getString("description"));
        tool.put("inputSchema", toolArray.getJSONObject(i).getJSONObject("inputSchema"));
        toolList.add(tool);
      }
      // 限制工具数量
      if (toolList.size() > 50) {
        toolList = new ArrayList<>(toolList.subList(0, 50));
      }
      redisUtil.set(serverName + chatId, toolList);
      if (nextCursor != null && !nextCursor.equals("")) {
        session.getBasicRemote().sendText(McpProtocolSend.sendToolList(nextCursor, endpoint));
      }
    }
  }

  public void destroyMcp(String serverName, String chatId) {
    redisUtil.del(serverName + chatId);
    if (redisUtil.hasKey(serverName + chatId + "toolResult")) {
      redisUtil.del(serverName + chatId + "toolResult");
    }
  }
}
