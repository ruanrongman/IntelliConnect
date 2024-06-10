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
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.unfbx.chatgpt.entity.chat.Message;
import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.Constants;
import com.zhipu.oapi.service.v4.model.*;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.rslly.iot.utility.ai.ModelMessage;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Slf4j
public class Glm implements LLM {
  // 请自定义自己的业务id
  private static final String requestIdTemplate = "mycompany-%d";


  private static ClientV4 client;
  private static final ObjectMapper mapper = defaultObjectMapper();

  @Value("${ai.glm-key}")
  public void setApiKey(String apiKey) {
    // 填写自己的api key
    client = new ClientV4.Builder(apiKey).build();
  }

  public static ObjectMapper defaultObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    return mapper;
  }


  public static Flowable<ChatMessageAccumulator> mapStreamToAccumulator(
      Flowable<ModelData> flowable) {
    return flowable.map(chunk -> {
      return new ChatMessageAccumulator(chunk.getChoices().get(0).getDelta(), null,
          chunk.getChoices().get(0), chunk.getUsage(), chunk.getCreated(), chunk.getId());
    });
  }

  public JSONObject jsonChat(String content, List<ModelMessage> messages, boolean search) {
    try {
      var messageList = this.dealMsg(messages);
      List<ChatTool> chatToolList = new ArrayList<>();
      ChatTool searchTool = new ChatTool();
      searchTool.setType(ChatToolType.WEB_SEARCH.value());
      WebSearch webSearch = new WebSearch();
      webSearch.setSearch_query(content);
      webSearch.setEnable(search);
      searchTool.setWeb_search(webSearch);
      chatToolList.add(searchTool);
      String requestId = String.format(requestIdTemplate, System.currentTimeMillis());
      ChatCompletionRequest chatCompletionRequest =
          ChatCompletionRequest.builder()
              .model(Constants.ModelChatGLM4)
              .temperature(0.3F)
              .stream(Boolean.FALSE).tools(chatToolList)
              .invokeMethod(Constants.invokeMethod)
              .messages(messageList)
              .requestId(requestId).build();
      ModelApiResponse invokeModelApiResp = client.invokeModelApi(chatCompletionRequest);
      var response =
          mapper.writeValueAsString(invokeModelApiResp.getData().getChoices().get(0).getMessage());
      log.info("model output:{} ", response);
      var temp = response.replace("```json", "").replace("```JSON", "").replace("```", "")
          .replace("json", "");
      JSONObject obj = JSON.parseObject(temp);
      var contentObj = obj.getJSONObject("content");
      if (contentObj == null)
        throw new NullPointerException("content jsonObject is null");
      return contentObj;
    } catch (Exception e) {
      JSONObject action = new JSONObject();
      JSONObject answer = new JSONObject();
      answer.put("answer", "对不起你购买的产品尚不支持这个请求或者设备不在线，请检查你的小程序的设置");
      action.put("action", answer);

      return action;
    }
  }

  public String commonChat(String content, List<ModelMessage> messages, boolean search) {
    try {
      var messageList = this.dealMsg(messages);
      List<ChatTool> chatToolList = new ArrayList<>();
      ChatTool searchTool = new ChatTool();
      searchTool.setType(ChatToolType.WEB_SEARCH.value());
      WebSearch webSearch = new WebSearch();
      webSearch.setSearch_query(content);
      webSearch.setEnable(search);
      searchTool.setWeb_search(webSearch);
      chatToolList.add(searchTool);
      String requestId = String.format(requestIdTemplate, System.currentTimeMillis());
      ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
          .model("glm-4-flash")
          .stream(Boolean.FALSE)
          .tools(chatToolList)
          .invokeMethod(Constants.invokeMethod)
          .messages(messageList)
          .requestId(requestId).build();
      ModelApiResponse invokeModelApiResp = client.invokeModelApi(chatCompletionRequest);

      var response = mapper.writeValueAsString(
          invokeModelApiResp.getData().getChoices().get(0).getMessage().getContent());
      log.info("model output:{} ", response);
      return response;
    } catch (Exception e) {
      return "对不起你购买的产品尚不支持这个请求或者设备不在线，请检查你的小程序的设置";
    }
  }

  private List<ChatMessage> dealMsg(List<ModelMessage> messages) {
    List<ChatMessage> messageList = new ArrayList<>();
    for (var s : messages) {
      if (s.getRole().equals(ChatMessageRole.SYSTEM.value()))
        messageList.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), s.getContent().toString()));
      else if (s.getRole().equals(ChatMessageRole.USER.value()))
        messageList.add(new ChatMessage(ChatMessageRole.USER.value(), s.getContent().toString()));
      else if (s.getRole().equals(ChatMessageRole.ASSISTANT.value()))
        messageList
            .add(new ChatMessage(ChatMessageRole.ASSISTANT.value(), s.getContent().toString()));
    }
    return messageList;
  }

  /**
   * 图生文
   *
   * @return
   */
  public static String testImageToWord(String url) {
    List<ChatMessage> messages = new ArrayList<>();
    List<Map<String, Object>> contentList = new ArrayList<>();
    Map<String, Object> textMap = new HashMap<>();
    textMap.put("type", "text");
    textMap.put("text", "图里有什么");
    Map<String, Object> typeMap = new HashMap<>();
    typeMap.put("type", "image_url");
    Map<String, Object> urlMap = new HashMap<>();
    urlMap.put("url", url);
    typeMap.put("image_url", urlMap);
    contentList.add(textMap);
    contentList.add(typeMap);
    ChatMessage chatMessage = new ChatMessage(ChatMessageRole.USER.value(), contentList);
    messages.add(chatMessage);
    String requestId = String.format(requestIdTemplate, System.currentTimeMillis());


    ChatCompletionRequest chatCompletionRequest =
        ChatCompletionRequest.builder()
            .model(Constants.ModelChatGLM4V)
            .stream(Boolean.FALSE)
            .invokeMethod(Constants.invokeMethod)
            .messages(messages)
            .requestId(requestId).build();
    ModelApiResponse modelApiResponse = client.invokeModelApi(chatCompletionRequest);
    try {
      var response = mapper.writeValueAsString(
          modelApiResponse.getData().getChoices().get(0).getMessage().getContent());
      // System.out.println("model output:" + response);
      log.info("model output:{}", response);
      return response;
    } catch (JsonProcessingException e) {
      return "不起你购买的产品尚不支持这个请求或者设备不在线，请检查你的小程序的设置";
    }

  }

}
