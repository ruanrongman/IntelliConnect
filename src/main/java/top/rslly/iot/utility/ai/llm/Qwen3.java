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
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import top.rslly.iot.utility.ai.ModelMessage;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Qwen3 implements LLM {
  private String URL = "https://api.siliconflow.cn/v1/chat/completions";
  private final String apiKey;
  private final String modelName;
  private final boolean enableThinking;
  private final int thinkingBudget;
  private static final OkHttpClient okHttpClient = new OkHttpClient.Builder()
      .connectTimeout(3000, TimeUnit.SECONDS)
      .writeTimeout(3000, TimeUnit.SECONDS)
      .readTimeout(3000, TimeUnit.SECONDS)
      .build();

  public Qwen3(String apiKey, String modelName, boolean enableThinking, int thinkingBudget) {
    this.apiKey = apiKey;
    this.modelName = modelName;
    this.enableThinking = enableThinking;
    this.thinkingBudget = thinkingBudget;
  }

  public Qwen3(String apiKey, String modelName, boolean enableThinking, int thinkingBudget,
      String url) {
    this.URL = url;
    this.apiKey = apiKey;
    this.modelName = modelName;
    this.enableThinking = enableThinking;
    this.thinkingBudget = thinkingBudget;
  }

  @Override
  public JSONObject jsonChat(String content, List<ModelMessage> messages, boolean search) {
    try {
      String response = sendRequest(buildMessages(messages, content), false, true);
      var responseArray = JSONObject.parseObject(response).getJSONArray("choices");
      String reasoningContent =
          responseArray.getJSONObject(0).getJSONObject("message").getString("reasoning_content");
      response = responseArray.getJSONObject(0).getJSONObject("message").getString("content");
      log.info("model reasoningContent:{} ", reasoningContent);
      log.info("model output:{} ", response);
      if (response == null || response.equals(""))
        throw new Exception("model error: 模型返回结果为空");
      var temp = response.replace("```json", "").replace("```JSON", "").replace("```", "")
          .replace("json", "");
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
      String response = sendRequest(buildMessages(messages, content), false, false);
      var responseArray = JSONObject.parseObject(response).getJSONArray("choices");
      response = responseArray.getJSONObject(0).getJSONObject("message").getString("content");
      String reasoningContent =
          responseArray.getJSONObject(0).getJSONObject("message").getString("reasoning_content");
      log.info("model reasoningContent:{} ", reasoningContent);
      log.info("model output:{} ", response);
      if (response == null || response.equals(""))
        throw new Exception("model error: 模型返回结果为空");
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
      Request request = buildRequest(buildMessages(messages, content), true, true);
      EventSources.createFactory(okHttpClient).newEventSource(request, listener);
    } catch (Exception e) {
      log.error("流式请求失败: {}", e.getMessage());
    }
  }

  private List<Map<String, String>> buildMessages(List<ModelMessage> messages, String newContent) {
    List<Map<String, String>> messageList = new ArrayList<>();
    for (ModelMessage msg : messages) {
      Map<String, String> message = new HashMap<>();
      message.put("role", msg.getRole());
      message.put("content", msg.getContent().toString());
      messageList.add(message);
    }

    return messageList;
  }

  private Request buildRequest(List<Map<String, String>> messages, boolean stream,
      boolean jsonOutput) throws IOException {
    // 构建请求参数
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("model", modelName);
    requestBody.put("messages", messages);
    requestBody.put("stream", stream);
    requestBody.put("max_tokens", 512);
    requestBody.put("enable_thinking", enableThinking);
    requestBody.put("thinking_budget", thinkingBudget);
    requestBody.put("min_p", 0.05);
    requestBody.put("temperature", 0.7);
    requestBody.put("top_p", 0.7);
    requestBody.put("top_k", 50);
    requestBody.put("frequency_penalty", 0.5);
    requestBody.put("n", 1);
    if (jsonOutput) {
      requestBody.put("output_format", Collections.singletonMap("type", "json"));
    } else {
      requestBody.put("response_format", Collections.singletonMap("type", "text"));
    }

    // 修正 tools 字段结构
    Map<String, Object> functionSpec = new HashMap<>();
    functionSpec.put("description", "<string>");
    functionSpec.put("name", "<string>");
    functionSpec.put("parameters", Collections.emptyMap());
    functionSpec.put("strict", false);

    Map<String, Object> tool = new HashMap<>();
    tool.put("type", "function");
    tool.put("function", functionSpec);

    requestBody.put("tools", Collections.singletonList(tool));

    // log.info("requestBody:{}", JSON.toJSONString(requestBody));

    RequestBody body = RequestBody.create(
        MediaType.get("application/json"),
        JSON.toJSONString(requestBody));

    return new Request.Builder()
        .url(URL)
        .post(body)
        .addHeader("Authorization", "Bearer " + apiKey)
        .addHeader("Content-Type", "application/json")
        .build();
  }

  private String sendRequest(List<Map<String, String>> messages, boolean stream, boolean jsonOutput)
      throws IOException {
    Request request = buildRequest(messages, stream, jsonOutput);
    try (Response response = okHttpClient.newCall(request).execute()) {
      // log.info("Response: {}", response.body());
      if (!response.isSuccessful())
        throw new IOException("Unexpected code " + response);

      ResponseBody responseBody = response.body();
      if (responseBody == null)
        throw new IOException("Empty response body");
      return responseBody.string();
    }
  }
}
