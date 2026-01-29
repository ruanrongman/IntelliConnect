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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.rslly.iot.services.agent.LlmProviderInformationServiceImpl;
import top.rslly.iot.services.agent.ProductLlmModelServiceImpl;
import top.rslly.iot.utility.HttpRequestUtils;
import top.rslly.iot.utility.ai.IcAiException;
import top.rslly.iot.utility.ai.LlmDiyUtility;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.ModelMessageRole;
import top.rslly.iot.utility.ai.llm.LLM;
import top.rslly.iot.utility.ai.llm.LLMFactory;
import top.rslly.iot.utility.ai.prompts.WeatherToolPrompt;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Data
@Component
@Slf4j
public class WeatherTool implements BaseTool<String> {
  @Autowired
  private WeatherToolPrompt weatherToolPrompt;
  @Value("${weather.key}")
  private String AmapKey;
  @Value("${ai.weatherTool-speedUp}")
  private boolean speedUp;
  @Autowired
  private HttpRequestUtils httpRequestUtils;
  @Autowired
  private LlmDiyUtility llmDiyUtility;
  // 将锁和条件变量改为每个 chatId 独立
  private final Map<String, Lock> lockMap = new ConcurrentHashMap<>();
  private final Map<String, Condition> conditionMap = new ConcurrentHashMap<>();
  private final Map<String, String> dataMap = new ConcurrentHashMap<>();

  @Value("${ai.weatherTool-llm}")
  private String llmName;
  private String name = "weatherTool";
  private String description = """
      Get live weather facts and forecasts
      Args: The weather in xx city at xx time(str)
      """;

  @Override
  public String run(String question) {
    return null;
  }

  @Override
  public String run(String question, Map<String, Object> globalMessage) {
    int productId = (int) globalMessage.get("productId");
    LLM llm = llmDiyUtility.getDiyLlm(productId, llmName, "1");
    List<ModelMessage> messages = new ArrayList<>();
    Map<String, Queue<String>> queueMap =
        (Map<String, Queue<String>>) globalMessage.get("queueMap");
    String chatId = (String) globalMessage.get("chatId");
    boolean mcpIsTool = false;
    if (globalMessage.containsKey("mcpIsTool"))
      mcpIsTool = (boolean) globalMessage.get("mcpIsTool");

    // 初始化当前会话的锁和条件变量
    lockMap.putIfAbsent(chatId, new ReentrantLock());
    conditionMap.putIfAbsent(chatId, lockMap.get(chatId).newCondition());

    ModelMessage systemMessage =
        new ModelMessage(ModelMessageRole.SYSTEM.value(), weatherToolPrompt.getWeatherTool());
    ModelMessage userMessage = new ModelMessage(ModelMessageRole.USER.value(), question);
    messages.add(systemMessage);
    messages.add(userMessage);
    if (speedUp && !mcpIsTool) {
      queueMap.get(chatId).add("我来给你查天气啦!");
    }
    var obj = llm.jsonChat(question, messages, false).getJSONObject("action");
    try {
      Map<String, String> answer = process_llm_result(obj);
      ModelMessage responseSystemMessage =
          new ModelMessage(ModelMessageRole.SYSTEM.value(),
              weatherToolPrompt.getWeatherResponse(answer.get("lives"), answer.get("forecasts")));
      messages.clear();
      messages.add(responseSystemMessage);
      messages.add(userMessage);
      if (speedUp && !mcpIsTool) {
        dataMap.remove(chatId); // 使用 chatId 作为 key
        llm.streamJsonChat(question, messages, true,
            new ChatToolEventSourceListener(queueMap, chatId, this));
        Lock chatLock = lockMap.get(chatId);
        Condition chatCondition = conditionMap.get(chatId);
        chatLock.lock();
        try {
          while (dataMap.get(chatId) == null) {
            chatCondition.await();
          }
        } catch (Exception e) {
          log.error("ChatTool error{}", e.getMessage());
        } finally {
          chatLock.unlock();
        }
        String data = dataMap.get(chatId);
        dataMap.remove(chatId);
        conditionMap.remove(chatId);
        lockMap.remove(chatId);
        return data;
      } else {
        return llm.commonChat(question, messages, false);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return "查询天气失败，请检查是否输入具体城市名称。";
  }

  private Map<String, String> process_llm_result(JSONObject llmObject)
      throws IcAiException, IOException, NullPointerException {
    if (llmObject.get("code").equals("200") || llmObject.get("code").equals(200)) {
      Map<String, String> result = new HashMap<>();
      String address = llmObject.getString("address");
      if (address != null && !address.equals("")) {
        var adCodeSearch = httpRequestUtils.httpGet(
            "https://restapi.amap.com/v3/geocode/geo?address=" + address + "&key=" + AmapKey);
        String cityCodeMessage = Objects.requireNonNull(adCodeSearch.body()).string();
        JSONObject jsonObject = JSON.parseObject(cityCodeMessage);
        var geocodes = jsonObject.getJSONArray("geocodes");
        JSONObject adCodeObj = (JSONObject) geocodes.get(0);
        String adCode = adCodeObj.getString("adcode");
        var weatherForecast =
            httpRequestUtils.httpGet("https://restapi.amap.com/v3/weather/weatherInfo?city="
                + adCode + "&key=" + AmapKey + "&extensions=all");
        var weatherLives =
            httpRequestUtils.httpGet("https://restapi.amap.com/v3/weather/weatherInfo?city="
                + adCode + "&key=" + AmapKey + "&extensions=base");
        String weatherForecastMessage = Objects.requireNonNull(weatherForecast.body()).string();
        String weatherLivesMessage = Objects.requireNonNull(weatherLives.body()).string();
        JSONObject weatherForecastObj = JSON.parseObject(weatherForecastMessage);
        JSONObject weatherLivesObj = JSON.parseObject(weatherLivesMessage);
        String lives = weatherLivesObj.getJSONArray("lives").toString();
        String forecasts = weatherForecastObj.getJSONArray("forecasts").toString();
        result.put("lives", lives);
        result.put("forecasts", forecasts);
        return result;
      } else
        throw new IcAiException("llm response error");
    } else
      throw new IcAiException("llm response error");
  }
}
