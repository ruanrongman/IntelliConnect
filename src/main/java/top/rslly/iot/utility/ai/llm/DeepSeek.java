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
import com.unfbx.chatgpt.OpenAiStreamClient;
import com.unfbx.chatgpt.entity.chat.*;
import com.unfbx.chatgpt.interceptor.OpenAILogger;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.sse.EventSourceListener;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.ModelMessageRole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DeepSeek implements LLM {
  private String URL = "https://api.deepseek.com";
  private String model = "deepseek-chat";
  // private static final String glmUrl = "https://open.bigmodel.cn/api/paas/v4/";
  private final OpenAiClient openAiClient;
  private final OpenAiStreamClient openAiStreamClient;
  private final static OkHttpClient okHttpClient = new OkHttpClient.Builder()
      .connectTimeout(3000, TimeUnit.SECONDS)// 自定义超时时间
      .writeTimeout(3000, TimeUnit.SECONDS)// 自定义超时时间
      .readTimeout(3000, TimeUnit.SECONDS)// 自定义超时时间
      .build();

  public DeepSeek(String apiKey) {
    openAiClient = OpenAiClient.builder()
        .apiKey(List.of(apiKey))
        .okHttpClient(okHttpClient)
        .apiHost(URL)
        .build();
    openAiStreamClient = OpenAiStreamClient.builder()
        .apiKey(List.of(apiKey))
        .okHttpClient(okHttpClient)
        .apiHost(URL)
        .build();
  }

  public DeepSeek(String url, String model, String apiKey) {
    this.URL = url;
    this.model = model;
    openAiClient = OpenAiClient.builder()
        .apiKey(List.of(apiKey))
        .okHttpClient(okHttpClient)
        .apiHost(URL + "/")
        .build();
    openAiStreamClient = OpenAiStreamClient.builder()
        .apiKey(List.of(apiKey))
        .okHttpClient(okHttpClient)
        .apiHost(URL + "/")
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
      var temp = response.replace("```json", "").replace("```JSON", "").replace("```", "");
      JSONObject jsonResponse = JSON.parseObject(temp);
      if (jsonResponse == null)
        throw new Exception("json parse error");
      return jsonResponse;
    } catch (Exception e) {
      log.error("model error:{} ", e.getMessage());
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
      log.error("model error:{} ", e.getMessage());
      return "对不起你购买的产品尚不支持这个请求或者设备不在线，请检查你的小程序的设置";
    }
  }

  @Override
  public void streamJsonChat(String content, List<ModelMessage> messages, boolean search,
      EventSourceListener listener) {
    try {
      var messageList = this.dealMsg(messages);
      ChatCompletion chatCompletion = ChatCompletion
          .builder()
          .model(model)
          .stream(Boolean.TRUE)
          .messages(messageList).build();
      openAiStreamClient.streamChatCompletion(chatCompletion, listener);
    } catch (Exception e) {
      log.error("model error:{} ", e.getMessage());
    }
  }

  public String imageToWord(String question, String url) {
    try {
      Content textContent =
          Content.builder().text(question).type(Content.Type.TEXT.getName()).build();
      ImageUrl imageUrl = ImageUrl.builder().url(url).build();
      Content imageContent =
          Content.builder().imageUrl(imageUrl).type(Content.Type.IMAGE_URL.getName()).build();
      List<Content> contentList = new ArrayList<>();
      contentList.add(textContent);
      contentList.add(imageContent);
      // log.info("contentList:{}", contentList);
      MessagePicture message =
          MessagePicture.builder().role(Message.Role.USER).content(contentList).build();
      // #####请求参数使用ChatCompletionWithPicture类
      ChatCompletionWithPicture chatCompletion = ChatCompletionWithPicture
          .builder()
          .messages(Collections.singletonList(message))
          .model(model)
          .build();
      ChatCompletionResponse chatCompletionResponse = openAiClient.chatCompletion(chatCompletion);
      var response = chatCompletionResponse.getChoices().get(0).getMessage().getContent();
      log.info("model output:{} ", response);
      return response;
    } catch (Exception e) {
      log.error("model error:{} ", e.getMessage());
      return "对不起你购买的产品尚不支持这个请求或者设备不在线，请检查你的小程序的设置";
    }
  }

  private List<Message> dealMsg(List<ModelMessage> messages) {
    List<Message> messageList = new ArrayList<>();
    for (var s : messages) {
      if (s.getRole().equals(ModelMessageRole.SYSTEM.value()))
        messageList.add(
            Message.builder().role(Message.Role.SYSTEM).content(s.getContent().toString()).build());
      else if (s.getRole().equals(ModelMessageRole.USER.value()))
        messageList.add(
            Message.builder().role(Message.Role.USER).content(s.getContent().toString()).build());
      else if (s.getRole().equals(ModelMessageRole.ASSISTANT.value()))
        messageList.add(Message.builder().role(Message.Role.ASSISTANT)
            .content(s.getContent().toString()).build());
    }
    return messageList;
  }


}
