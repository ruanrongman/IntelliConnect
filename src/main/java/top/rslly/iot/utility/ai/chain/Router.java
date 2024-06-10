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
  private WxBoundProductTool wxBoundProductTool;
  @Autowired
  private RedisUtil redisUtil;
  @Autowired
  private Agent agent;
  @Autowired
  private WxUserServiceImpl wxUserService;
  @Autowired
  private WxProductBindServiceImpl wxProductBindService;
  @Autowired
  private WxProductActiveTool wxProductActiveTool;
  @Autowired
  private WxProductActiveServiceImpl wxProductActiveService;
  @Autowired
  private ProductServiceImpl productService;
  @Autowired
  private ScheduleTool scheduleTool;

  // chatId in wechat module need to use openid
  public String response(String content, String chatId, int productId, String... microappid) {
    List<ModelMessage> memory;
    String answer;
    String toolResult = "";
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
            toolResult = controlTool.run(args, productId);
            answer = "以下是智能控制插件结果：" + toolResult;
          }
          case "4" -> {
            var musicMap = musicTool.run(args);
            toolResult = musicMap.get("answer");
            answer = "以下是网易云音乐插件结果：" + toolResult + musicMap.get("url");
          }
          case "5" -> {
            toolResult = agent.run(content, productId);
            answer = "以下是智能体处理结果：" + toolResult;
          }
          case "6" -> answer = chatTool.run(content, memory);
          case "7" -> {
            if (wxUserService.findAllByOpenid(chatId).isEmpty())
              toolResult = "检测到当前不在微信客服对话环境，该功能无法使用";
            else {
              var wxBoundProductToolMap = wxBoundProductTool.run(args);
              String productName = wxBoundProductToolMap.get("productName");
              String productKey = wxBoundProductToolMap.get("productKey");
              var productList = productService.findAllByProductName(productName);
              if (productList.isEmpty())
                toolResult = "对不起小主人，没有找到" + productName + "产品";
              else {
                if (!wxProductBindService
                    .findByOpenidAndProductId(chatId, productList.get(0).getId()).isEmpty())
                  toolResult = "对不起小主人，你已经绑定过该产品了，无需重复绑定";
                else {
                  boolean result =
                      wxProductBindService.wxBindProduct(chatId, productName, productKey);
                  if (result)
                    toolResult = wxBoundProductToolMap.get("answer");
                  else
                    toolResult = "对不起小主人，我绑定失败了，请再试一次吧,参考输入格式为绑定XXX产品，密钥为XXX";
                }
              }

            }
            answer = "以下是微信绑定产品插件结果：" + toolResult;
          }
          case "8" -> {
            if (wxUserService.findAllByOpenid(chatId).isEmpty())
              toolResult = "检测到当前不在微信客服对话环境，该功能无法使用";
            else {

              var wxProductActiveToolMap = wxProductActiveTool.run(args);
              String productName = wxProductActiveToolMap.get("productName");
              WxProductActiveEntity wxProductActiveEntity = new WxProductActiveEntity();
              wxProductActiveEntity.setOpenid(chatId);
              var productList = productService.findAllByProductName(productName);
              if (productList.isEmpty())
                toolResult = "对不起小主人，没有找到" + productName + "产品";
              else {
                if (wxProductBindService
                    .findByOpenidAndProductId(chatId, productList.get(0).getId()).isEmpty())
                  toolResult = "对不起小主人，你还没绑定该产品，请先绑定该产品再来设置吧";
                else {
                  wxProductActiveEntity.setProductId(productList.get(0).getId());
                  if (wxProductActiveService
                      .findAllByProductIdAndOpenid(wxProductActiveEntity.getProductId(),
                          wxProductActiveEntity.getOpenid())
                      .isEmpty()) {
                    wxProductActiveService.setUp(wxProductActiveEntity);
                    toolResult = wxProductActiveToolMap.get("answer");
                  } else {
                    toolResult = "对不起小主人，你当前已经能控制该产品了，无需进行切换";
                  }
                }
              }
            }
            answer = "以下是微信切换产品插件结果：" + toolResult;
          }
          case "9" -> {
            if (wxUserService.findAllByOpenid(chatId).isEmpty())
              toolResult = "检测到当前不在微信客服对话环境，该功能无法使用";
            else {
              toolResult = scheduleTool.run(args, chatId, microappid[0]);
            }
            answer = "以下是定时任务插件的结果：" + toolResult;
          }
          default -> answer = resultMap.get("answer").toString();
        }
      } else
        answer = chatTool.run(content, memory);
    } else
      answer = chatTool.run(content, memory);
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
    redisUtil.set("memory" + chatId, memory);
    return answer;
  }


}
