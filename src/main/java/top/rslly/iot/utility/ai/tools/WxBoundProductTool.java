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
import top.rslly.iot.services.*;
import top.rslly.iot.utility.ai.IcAiException;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.ModelMessageRole;
import top.rslly.iot.utility.ai.Prompt;
import top.rslly.iot.utility.ai.llm.LLM;
import top.rslly.iot.utility.ai.llm.LLMFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Component
public class WxBoundProductTool implements BaseTool<String> {
  @Autowired
  private Prompt prompt;
  @Autowired
  private WxUserServiceImpl wxUserService;
  @Autowired
  private WxProductBindServiceImpl wxProductBindService;
  @Autowired
  private ProductServiceImpl productService;
  @Value("${ai.wxBoundProductTool-llm}")
  private String llmName;
  private String name = "wxBoundProductTool";
  private String description = """
      A tool for wechat bound product.
      Args: product name and product key.(str)
      """;

  public String run(String question) {
    return null;
  }

  @Override
  public String run(String question, Map<String, Object> globalMessage) {
    String chatId = (String) globalMessage.get("chatId");
    if (wxUserService.findAllByOpenid(chatId).isEmpty())
      return "检测到当前不在微信客服对话环境，该功能无法使用";
    LLM llm = LLMFactory.getLLM(llmName);
    List<ModelMessage> messages = new ArrayList<>();
    ModelMessage systemMessage =
        new ModelMessage(ModelMessageRole.SYSTEM.value(), prompt.getWxProductBoundTool());
    ModelMessage userMessage = new ModelMessage(ModelMessageRole.USER.value(), question);
    messages.add(systemMessage);
    messages.add(userMessage);
    var obj = llm.jsonChat(question, messages, true).getJSONObject("action");
    // var answer = (String) obj.get("answer");
    try {
      return process_llm_result(obj, chatId);
    } catch (Exception e) {
      // e.printStackTrace();
      return "对不起小主人,绑定产品操作发生了异常，请重新尝试";
    }
  }

  private String process_llm_result(JSONObject jsonObject, String chatId) throws IcAiException {
    if (jsonObject.get("code").equals("200") || jsonObject.get("code").equals(200)) {
      var productName = jsonObject.getString("productName");
      var productKey = jsonObject.getString("productKey");
      var bind = jsonObject.getString("bind");
      var answer = jsonObject.getString("answer");
      String toolResult;
      var productList = productService.findAllByProductName(productName);
      if (productList.isEmpty()) {
        toolResult = "对不起小主人，没有找到" + productName + "产品";
      } else {
        if (!wxProductBindService
            .findByOpenidAndProductId(chatId, productList.get(0).getId()).isEmpty()) {
          if (bind.equals("true")) {
            toolResult = "对不起小主人，你已经绑定过该产品了，无需重复绑定";
          } else {
            boolean result =
                wxProductBindService.wxUnBindProduct(chatId, productName, productKey);
            if (result)
              toolResult = answer;
            else
              toolResult = "对不起小主人，我解除绑定失败了，请再试一次吧,参考输入格式为解除绑定XXX产品，密钥为XXX";
          }
        } else {
          boolean result =
              wxProductBindService.wxBindProduct(chatId, productName, productKey);
          if (result)
            toolResult = answer;
          else
            toolResult = "对不起小主人，我绑定失败了，请再试一次吧,参考输入格式为绑定XXX产品，密钥为XXX";
        }
      }
      return toolResult;

    } else {
      throw new IcAiException(jsonObject.getString("platform not support"));
    }
  }
}
