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

import com.zhipu.oapi.service.v4.model.ChatMessage;
import com.zhipu.oapi.service.v4.model.ChatMessageRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.rslly.iot.utility.Cast;
import top.rslly.iot.utility.RedisUtil;
import top.rslly.iot.utility.ai.toolAgent.Agent;
import top.rslly.iot.utility.ai.tools.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@Component
public class Router {
  @Autowired
  private ClassifierTool classifierTool;
  @Autowired
  private MusicTool musicTool;
  @Autowired
  private ControlTool controlTool;
  @Autowired
  private ChatTool chatTool;
  @Autowired
  private WeatherTool weatherTool;
  @Autowired
  private RedisUtil redisUtil;
  @Autowired
  private Agent agent;

  public String response(String content, String chatId, int productId) {
    List<ChatMessage> memory;
    String answer;
    Map<String, String> musicMap = null;
    boolean isMusicTool = false;
    var memory_cache = redisUtil.get("memory" + chatId);
    if (memory_cache != null)
      memory = Cast.castList(memory_cache, ChatMessage.class);
    else
      memory = new ArrayList<>();
    var resultMap = classifierTool.run(content, memory);
    String args;
    Object argsObject = resultMap.get("args");
    if (argsObject != null) {
      args = argsObject.toString();
    } else {
      args = "";
    }
    if (resultMap.containsKey("value") && !args.equals("")) {
      List<String> value = Cast.castList(resultMap.get("value"), String.class);
      if (!value.isEmpty()) {
        switch (value.get(0)) {
          case "4" -> {
            musicMap = musicTool.run(args);
            isMusicTool = true;
            answer = "以下是网易云音乐插件结果：" + musicMap.get("answer") + musicMap.get("url");
          }
          case "1" -> answer = "以下是高德天气插件结果：" + weatherTool.run(args);
          case "3" -> answer = "以下是智能控制插件结果：" + controlTool.run(args, productId);
          case "6" -> answer = chatTool.run(content, memory);
          case "5" -> answer = "以下是智能体处理结果：" + agent.run(content, productId);
          default -> answer = resultMap.get("answer").toString();
        }
      } else
        answer = chatTool.run(content, memory);
    } else
      answer = chatTool.run(content, memory);
    ChatMessage chatMessage;
    if (isMusicTool)
      chatMessage = new ChatMessage(ChatMessageRole.ASSISTANT.value(), musicMap.get("answer"));
    else
      chatMessage = new ChatMessage(ChatMessageRole.ASSISTANT.value(), answer);
    ChatMessage userContent = new ChatMessage(ChatMessageRole.USER.value(), content);
    memory.add(userContent);
    memory.add(chatMessage);
    System.out.println(memory.size());
    if (memory.size() > 6) {
      memory.subList(0, memory.size() - 6).clear();
    }
    redisUtil.set("memory" + chatId, memory);
    return answer;
  }


}
