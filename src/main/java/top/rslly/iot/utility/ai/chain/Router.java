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


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhipu.oapi.service.v4.model.ChatMessage;
import com.zhipu.oapi.service.v4.model.ChatMessageRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import top.rslly.iot.models.WxProductActiveEntity;
import top.rslly.iot.services.*;
import top.rslly.iot.utility.Cast;
import top.rslly.iot.utility.RedisUtil;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.toolAgent.Agent;
import top.rslly.iot.utility.ai.tools.*;

import java.util.*;

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
  private WxBoundProductTool wxBoundProductTool;
  @Autowired
  private RedisUtil redisUtil;
  @Autowired
  private Agent agent;
  @Autowired
  private WxUserServiceImpl wxUserService;
  @Autowired
  private WxProductActiveTool wxProductActiveTool;
  @Autowired
  private ScheduleTool scheduleTool;
  @Autowired
  private SearchTool searchTool;
  @Autowired
  private ProductRoleTool productRoleTool;

  // chatId in wechat module need to use openid
  public String response(String content, String chatId, int productId, String... microappid) {
    List<ModelMessage> memory;
    String answer;
    String toolResult = "";
    Map<String, Object> globalMessage = new HashMap<>();
    globalMessage.put("productId", productId);
    globalMessage.put("chatId", chatId);
    if (microappid.length > 0)
      globalMessage.put("microappid", microappid[0]);
    var memory_cache = redisUtil.get("memory" + chatId);
    if (memory_cache != null)
      try {
        memory = Cast.castList(memory_cache, ModelMessage.class);
      } catch (Exception e) {
        e.printStackTrace();
        memory = new ArrayList<>();
      }
    else
      memory = new ArrayList<>();
    globalMessage.put("memory", memory);
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
          case "1" -> {
            toolResult = weatherTool.run(args);
            answer = "以下是高德天气插件结果：" + toolResult;
          }
          case "3" -> {
            toolResult = controlTool.run(args, globalMessage);
            answer = "以下是智能控制插件结果：" + toolResult;
          }
          case "4" -> {
            var musicMap = musicTool.run(args);
            toolResult = musicMap.get("answer");
            answer = "以下是网易云音乐插件结果：" + toolResult + musicMap.get("url");
          }
          case "5" -> {

            toolResult = agent.run(content, globalMessage);
            answer = "以下是智能体处理结果：" + toolResult;
          }
          case "6" -> answer = chatTool.run(content, globalMessage);
          case "7" -> {
            toolResult = wxBoundProductTool.run(args, globalMessage);
            answer = "以下是微信绑定产品插件结果：" + toolResult;
          }
          case "8" -> {
            toolResult = wxProductActiveTool.run(args, globalMessage);
            answer = "以下是微信切换产品插件结果：" + toolResult;
          }
          case "9" -> {
            if (wxUserService.findAllByOpenid(chatId).isEmpty())
              toolResult = "检测到当前不在微信客服对话环境，该功能无法使用";
            else {
              toolResult = scheduleTool.run(args, globalMessage);
            }
            answer = "以下是定时任务插件的结果：" + toolResult;
          }
          case "10" -> {
            toolResult = searchTool.run(args);
            answer = "以下是必应搜索插件的结果：" + toolResult;
          }
          case "11" -> {
            toolResult = productRoleTool.run(args, globalMessage);
            answer = "以下是产品角色插件的结果：" + toolResult;
          }
          default -> answer = resultMap.get("answer").toString();
        }
      } else
        answer = chatTool.run(content, globalMessage);
    } else
      answer = chatTool.run(content, globalMessage);
    if (toolResult.equals(""))
      toolResult = answer;
    ModelMessage userContent = new ModelMessage(ChatMessageRole.USER.value(), content);
    ModelMessage chatMessage = new ModelMessage(ChatMessageRole.ASSISTANT.value(), toolResult);
    memory.add(userContent);
    memory.add(chatMessage);
    // System.out.println(memory.size());
    // slide memory window
    if (memory.size() > 6) {
      memory.subList(0, memory.size() - 6).clear();
    }
    redisUtil.set("memory" + chatId, memory, 24 * 3600);
    return answer;
  }


}
