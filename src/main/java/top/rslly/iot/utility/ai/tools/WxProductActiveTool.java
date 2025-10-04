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

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.rslly.iot.models.WxProductActiveEntity;
import top.rslly.iot.services.thingsModel.ProductServiceImpl;
import top.rslly.iot.services.wechat.WxProductActiveServiceImpl;
import top.rslly.iot.services.wechat.WxProductBindServiceImpl;
import top.rslly.iot.services.wechat.WxUserServiceImpl;
import top.rslly.iot.utility.ai.IcAiException;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.ModelMessageRole;
import top.rslly.iot.utility.ai.llm.LLM;
import top.rslly.iot.utility.ai.llm.LLMFactory;
import top.rslly.iot.utility.ai.prompts.WxProductActiveToolPrompt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Component
public class WxProductActiveTool implements BaseTool<String> {
  @Autowired
  private WxProductActiveToolPrompt wxProductActiveToolPrompt;
  @Autowired
  private ProductServiceImpl productService;
  @Autowired
  private WxProductBindServiceImpl wxProductBindService;
  @Autowired
  private WxProductActiveServiceImpl wxProductActiveService;
  @Autowired
  private WxUserServiceImpl wxUserService;
  @Value("${ai.wxProductActiveTool-llm}")
  private String llmName;
  private String name = "wxProductActiveTool";
  private String description = """
      A tool for wechat to set the agent enable control the product.
      Args: product name .(str)
      """;

  public String run(String question) {
    return null;
  }

  @Override
  public String run(String question, Map<String, Object> globalMessage) {
    String chatId = (String) globalMessage.get("chatId");
    String openid = (String) globalMessage.get("openId");
    if (!globalMessage.containsKey("microappid"))
      return "检测到当前不在微信客服对话环境，该功能无法使用";
    String appid = (String) globalMessage.get("microappid");
    if (wxUserService.findAllByAppidAndOpenid(appid, openid).isEmpty())
      return "检测到当前不在微信客服对话环境，该功能无法使用";
    LLM llm = LLMFactory.getLLM(llmName);
    List<ModelMessage> messages = new ArrayList<>();
    ModelMessage systemMessage =
        new ModelMessage(ModelMessageRole.SYSTEM.value(),
            wxProductActiveToolPrompt.getWxProductActiveTool());
    ModelMessage userMessage = new ModelMessage(ModelMessageRole.USER.value(), question);
    messages.add(systemMessage);
    messages.add(userMessage);
    var obj = llm.jsonChat(question, messages, true).getJSONObject("action");
    // var answer = (String) obj.get("answer");
    try {
      return process_llm_result(obj, appid, openid);
    } catch (Exception e) {
      // e.printStackTrace();
      return "对不起小主人,切换产品操作发生了异常，请重新尝试";
    }
  }

  private String process_llm_result(JSONObject jsonObject, String appid, String openId)
      throws IcAiException {
    if (jsonObject.get("code").equals("200") || jsonObject.get("code").equals(200)) {
      var productName = jsonObject.getString("productName");
      var answer = jsonObject.getString("answer");
      var productList = productService.findAllByProductName(productName);
      String toolResult;
      if (productList.isEmpty())
        toolResult = "对不起小主人，没有找到" + productName + "产品";
      else {
        if (wxProductBindService
            .findByAppidAndOpenidAndProductId(appid, openId, productList.get(0).getId()).isEmpty())
          toolResult = "对不起小主人，你还没绑定该产品，请先绑定该产品再来设置吧";
        else {
          WxProductActiveEntity wxProductActiveEntity = new WxProductActiveEntity();
          wxProductActiveEntity.setOpenid(openId);
          wxProductActiveEntity.setAppid(appid);
          wxProductActiveEntity.setProductId(productList.get(0).getId());
          if (wxProductActiveService
              .findAllByProductIdAndAppidAndOpenid(wxProductActiveEntity.getProductId(), appid,
                  wxProductActiveEntity.getOpenid())
              .isEmpty()) {
            wxProductActiveService.setUp(wxProductActiveEntity);
            toolResult = answer;
          } else {
            toolResult = "对不起小主人，你当前已经能控制该产品了，无需进行切换";
          }
        }
      }
      return toolResult;
    } else {
      throw new IcAiException(jsonObject.getString("platform not support"));
    }
  }
}
