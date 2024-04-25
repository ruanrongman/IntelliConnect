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
package top.rslly.iot.utility.ai;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.Constants;
import com.zhipu.oapi.service.v4.model.*;
import io.reactivex.Flowable;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Glm {
  // 请自定义自己的业务id
  private static final String requestIdTemplate = "mycompany-%d";
  // 填写自己的API_KEY
  private static final String API_KEY = "XXXXXX";


  private static final ClientV4 client = new ClientV4.Builder(API_KEY).build();
  private static final ObjectMapper mapper = defaultObjectMapper();

  public static ObjectMapper defaultObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    mapper.addMixIn(ChatFunction.class, ChatFunctionMixIn.class);
    mapper.addMixIn(ChatCompletionRequest.class, ChatCompletionRequestMixIn.class);
    mapper.addMixIn(ChatFunctionCall.class, ChatFunctionCallMixIn.class);
    return mapper;
  }

  public void chat(String content) {
    System.setProperty("org.slf4j.simpleLogger.logFile", "System.out");
    List<ChatMessage> messages = new ArrayList<>();
    ChatMessage chatMessage = new ChatMessage(ChatMessageRole.USER.value(), content);
    ChatMessage chatMessage1 = new ChatMessage(ChatMessageRole.SYSTEM.value(),
        "作为一个智能音箱，你的名字叫肖总，是由创万联团队开发的，你擅长帮助人们操作各种电器，可以联网搜索，也可以给人们很好的帮助"
            + "你只可以控制下列电气:{\"lamp\",\"pump\",\"air_conditioner\"}"
            + "lamp可以控制的参数:{\"switch\",\"bright\",\"color\"}" + "pump可以控制的参数:{\"switch\"}"
            + "air_conditioner可以控制的参数:{\"switch\",\"temp\"}");
    // ChatMessage chatMessage = new ChatMessage(ChatMessageRole.USER.value(),
    // "你能帮我查询2024年1月1日从北京南站到上海的火车票吗？");
    messages.add(chatMessage1);
    messages.add(chatMessage);
    String requestId = String.format(requestIdTemplate, System.currentTimeMillis());
    // 函数调用参数构建部分
    List<ChatTool> chatToolList = new ArrayList<>();
    ChatTool searchTool = new ChatTool();
    searchTool.setType(ChatToolType.WEB_SEARCH.value());
    WebSearch webSearch = new WebSearch(true, content);
    searchTool.setWeb_search(webSearch);

    ChatTool chatTool = new ChatTool();
    chatTool.setType(ChatToolType.FUNCTION.value());
    ChatFunctionParameters chatFunctionParameters = new ChatFunctionParameters();
    chatFunctionParameters.setType("object");
    Map<String, Object> properties = new HashMap<>();
    properties.put("name", new HashMap<String, Object>() {
      {
        put("type", "string");
        put("description", "电气的名字");
      }
    });
    properties.put("properties", new HashMap<String, Object>() {
      {
        put("type", "list");
        put("description", "电器控制的参数（英文）");
      }
    });
    properties.put("value", new HashMap<String, Object>() {
      {
        put("type", "list");
        put("description", "电气控制的数值");
      }
    });

    List<String> required = new ArrayList<>();
    required.add("name");
    required.add("properties");
    required.add("value");
    chatFunctionParameters.setProperties(properties);
    ChatFunction chatFunction =
        ChatFunction.builder().name("switch_electrical").description("根据用户提供的信息，打开对应的电气")
            .parameters(chatFunctionParameters).required(required).build();
    chatTool.setFunction(chatFunction);
    chatToolList.add(chatTool);
    chatToolList.add(searchTool);

    ChatCompletionRequest chatCompletionRequest =
        ChatCompletionRequest.builder().model(Constants.ModelChatGLM4).stream(Boolean.TRUE)
            .messages(messages).requestId(requestId).tools(chatToolList).toolChoice("auto").build();
    ModelApiResponse sseModelApiResp = client.invokeModelApi(chatCompletionRequest);
    if (sseModelApiResp.isSuccess()) {
      AtomicBoolean isFirst = new AtomicBoolean(true);
      ChatMessageAccumulator chatMessageAccumulator =
          mapStreamToAccumulator(sseModelApiResp.getFlowable()).doOnNext(accumulator -> {
            {
              if (isFirst.getAndSet(false)) {
                System.out.print("Response: ");
              }
              if (accumulator.getDelta() != null
                  && accumulator.getDelta().getTool_calls() != null) {
                String jsonString =
                    mapper.writeValueAsString(accumulator.getDelta().getTool_calls());
                System.out.println("tool_calls: " + jsonString);
              }
              if (accumulator.getDelta() != null && accumulator.getDelta().getContent() != null) {
                System.out.print(accumulator.getDelta().getContent());
              }
            }
          }).doOnComplete(System.out::println).lastElement().blockingGet();

      Choice choice = new Choice(chatMessageAccumulator.getChoice().getFinishReason(), 0L,
          chatMessageAccumulator.getDelta());
      List<Choice> choices = new ArrayList<>();
      choices.add(choice);
      ModelData data = new ModelData();
      data.setChoices(choices);
      data.setUsage(chatMessageAccumulator.getUsage());
      data.setId(chatMessageAccumulator.getId());
      data.setCreated(chatMessageAccumulator.getCreated());
      data.setRequestId(chatCompletionRequest.getRequestId());
      sseModelApiResp.setFlowable(null);
      sseModelApiResp.setData(data);
    }
    System.out.println("model output:" + JSON.toJSONString(sseModelApiResp));
  }

  public static Flowable<ChatMessageAccumulator> mapStreamToAccumulator(
      Flowable<ModelData> flowable) {
    return flowable.map(chunk -> {
      return new ChatMessageAccumulator(chunk.getChoices().get(0).getDelta(), null,
          chunk.getChoices().get(0), chunk.getUsage(), chunk.getCreated(), chunk.getId());
    });
  }

  public JSONObject jsonChat(String content, List<ChatMessage> messages, boolean search) {


    List<ChatTool> chatToolList = new ArrayList<>();
    ChatTool searchTool = new ChatTool();
    searchTool.setType(ChatToolType.WEB_SEARCH.value());
    WebSearch webSearch = new WebSearch(search, content);
    searchTool.setWeb_search(webSearch);
    chatToolList.add(searchTool);
    String requestId = String.format(requestIdTemplate, System.currentTimeMillis());
    ChatCompletionRequest chatCompletionRequest =
        ChatCompletionRequest.builder()
            .model(Constants.ModelChatGLM4)
            .temperature(0.3F)
            .stream(Boolean.FALSE).tools(chatToolList)
            .invokeMethod(Constants.invokeMethod)
            .messages(messages)
            .requestId(requestId).build();
    ModelApiResponse invokeModelApiResp = client.invokeModelApi(chatCompletionRequest);
    try {
      var response =
          mapper.writeValueAsString(invokeModelApiResp.getData().getChoices().get(0).getMessage());
      System.out.println("model output:" + response);
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

  public String commonChat(String content, List<ChatMessage> messages, boolean search) {

    List<ChatTool> chatToolList = new ArrayList<>();
    ChatTool searchTool = new ChatTool();
    searchTool.setType(ChatToolType.WEB_SEARCH.value());
    WebSearch webSearch = new WebSearch(search, content);
    searchTool.setWeb_search(webSearch);
    chatToolList.add(searchTool);
    String requestId = String.format(requestIdTemplate, System.currentTimeMillis());
    ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
        .model(Constants.ModelChatGLM3TURBO)
        .stream(Boolean.FALSE)
        .tools(chatToolList)
        .invokeMethod(Constants.invokeMethod)
        .messages(messages)
        .requestId(requestId).build();
    ModelApiResponse invokeModelApiResp = client.invokeModelApi(chatCompletionRequest);
    try {
      var response = mapper.writeValueAsString(
          invokeModelApiResp.getData().getChoices().get(0).getMessage().getContent());
      System.out.println("model output:" + response);
      return response;
    } catch (JsonProcessingException e) {
      return "对不起你购买的产品尚不支持这个请求或者设备不在线，请检查你的小程序的设置";
    }
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
      System.out.println("model output:" + response);
      return response;
    } catch (JsonProcessingException e) {
      return "不起你购买的产品尚不支持这个请求或者设备不在线，请检查你的小程序的设置";
    }

  }

}
