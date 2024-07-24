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
package top.rslly.iot.utility.ai.llm;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.unfbx.chatgpt.OpenAiClient;
import com.unfbx.chatgpt.entity.chat.ChatCompletion;
import com.unfbx.chatgpt.entity.chat.ChatCompletionResponse;
import com.unfbx.chatgpt.entity.chat.Message;
import com.zhipu.oapi.service.v4.model.ChatMessageRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.rslly.iot.utility.ai.ModelMessage;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class DeepSeek implements LLM {
  private static final String URL = "https://api.deepseek.com";
  private static final String model = "deepseek-chat";
  // private static final String glmUrl = "https://open.bigmodel.cn/api/paas/v4/";
  private static OpenAiClient openAiClient;
  private static final ObjectMapper mapper = defaultObjectMapper();

  @Value("${ai.deepSeek-key}")
  public void setApiKey(String apiKey) {
    // 填写自己的api key
    openAiClient = OpenAiClient.builder()
        .apiKey(List.of(apiKey))
        .apiHost(URL)
        .build();
  }

  public static ObjectMapper defaultObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    return mapper;
  }

  @Override
  public JSONObject jsonChat(String content, List<ModelMessage> messages, boolean search) {
    // Message system =
    // Message.builder().role(Message.Role.SYSTEM).content(wxProductBoundPrompt).build();
    // Message message =
    // Message.builder().role(Message.Role.USER).content("绑定lamp设备，密钥是12345").build();
    try {
      var messageList = this.dealMsg(messages);
      ChatCompletion chatCompletion = ChatCompletion
          .builder()
          .model(model)
          .messages(messageList).build();
      ChatCompletionResponse chatCompletionResponse = openAiClient.chatCompletion(chatCompletion);
      // chatCompletionResponse.getChoices().forEach(e -> {
      // System.out.println(e.getMessage().getContent());
      // });
      var response = chatCompletionResponse.getChoices().get(0).getMessage().getContent();
      log.info("model output:{} ", response);
      var temp = response.replace("```json", "").replace("```JSON", "").replace("```", "")
          .replace("json", "");
      return JSON.parseObject(temp);
    } catch (Exception e) {
      // e.printStackTrace();
      JSONObject action = new JSONObject();
      JSONObject answer = new JSONObject();
      answer.put("answer", "对不起你购买的产品尚不支持这个请求或者设备不在线，请检查你的小程序的设置");
      action.put("action", answer);

      return action;
    }
  }

  @Override
  public String commonChat(String content, List<ModelMessage> messages, boolean search) {
    try {
      var messageList = this.dealMsg(messages);
      ChatCompletion chatCompletion = ChatCompletion
          .builder()
          .model(model)
          .messages(messageList).build();
      ChatCompletionResponse chatCompletionResponse = openAiClient.chatCompletion(chatCompletion);
      // chatCompletionResponse.getChoices().forEach(e -> {
      // System.out.println(e.getMessage().getContent());
      // });
      var response = chatCompletionResponse.getChoices().get(0).getMessage().getContent();
      log.info("model output:{} ", response);
      return response;
    } catch (Exception e) {
      e.printStackTrace();
      return "对不起你购买的产品尚不支持这个请求或者设备不在线，请检查你的小程序的设置";
    }
  }

  private List<Message> dealMsg(List<ModelMessage> messages) {
    List<Message> messageList = new ArrayList<>();
    for (var s : messages) {
      if (s.getRole().equals(ChatMessageRole.SYSTEM.value()))
        messageList.add(
            Message.builder().role(Message.Role.SYSTEM).content(s.getContent().toString()).build());
      else if (s.getRole().equals(ChatMessageRole.USER.value()))
        messageList.add(
            Message.builder().role(Message.Role.USER).content(s.getContent().toString()).build());
      else if (s.getRole().equals(ChatMessageRole.ASSISTANT.value()))
        messageList.add(Message.builder().role(Message.Role.ASSISTANT)
            .content(s.getContent().toString()).build());
    }
    return messageList;
  }


}
