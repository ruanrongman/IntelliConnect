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
import top.rslly.iot.utility.HttpRequestUtils;
import top.rslly.iot.utility.ai.IcAiException;
import top.rslly.iot.utility.ai.LlmDiyUtility;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.ModelMessageRole;
import top.rslly.iot.utility.ai.llm.LLM;
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
  @Value("${weather.amap-key}")
  private String amapKey;
  @Value("${ai.weatherTool-speedUp}")
  private boolean speedUp;
  @Autowired
  private HttpRequestUtils httpRequestUtils;
  @Autowired
  private LlmDiyUtility llmDiyUtility;
  @Autowired
  private QWeatherService qWeatherService;
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

    // 只有在speedUp模式下才初始化锁和条件变量
    if (speedUp && !mcpIsTool) {
      lockMap.putIfAbsent(chatId, new ReentrantLock());
      conditionMap.putIfAbsent(chatId, lockMap.get(chatId).newCondition());
    }

    ModelMessage systemMessage =
        new ModelMessage(ModelMessageRole.SYSTEM.value(), weatherToolPrompt.getWeatherTool());
    ModelMessage userMessage = new ModelMessage(ModelMessageRole.USER.value(), question);
    messages.add(systemMessage);
    messages.add(userMessage);
    if (speedUp && !mcpIsTool) {
      queueMap.get(chatId).add(ToolPrefix.ToolCall.getPrefix());
    }
    var obj = llm.jsonChat(question, messages, false).getJSONObject("action");
    // 获取客户端IP用于定位
    String clientIp = (String) globalMessage.get("clientIp");
    try {
      Map<String, String> answer = process_llm_result(obj, clientIp);
      ModelMessage responseSystemMessage =
          new ModelMessage(ModelMessageRole.SYSTEM.value(),
              weatherToolPrompt.getWeatherResponse(answer.get("city"),
                  answer.get("lives"), answer.get("forecasts")));
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
        return data;
      } else {
        return llm.commonChat(question, messages, false);
      }
    } catch (Exception e) {
      if (speedUp && !mcpIsTool) {
        queueMap.get(chatId).add("查询天气失败，可能是所在地区不支持");
      }
      log.error("ChatTool error{}", e.getMessage());
    } finally {
      cleanupResources(chatId);
    }
    return "查询天气失败，请检查是否输入具体城市名称。";
  }

  private void cleanupResources(String chatId) {
    dataMap.remove(chatId);
    conditionMap.remove(chatId);
    lockMap.remove(chatId);
  }

  private Map<String, String> process_llm_result(JSONObject llmObject, String clientIp)
      throws IcAiException, IOException, NullPointerException {
    Map<String, String> result = new HashMap<>();
    String address = llmObject.getString("address");

    // 如果没有指定城市（code为400或address为空），尝试使用IP定位
    if ((address == null || address.equals("")) && clientIp != null
        && !clientIp.equals("unknown")) {
      address = ipLocate(clientIp);
      log.info("IP定位结果: ip={}, city={}", clientIp, address);
    }

    // 无论 code 是 200 还是 400，只要有地址就查询天气
    if (address != null && !address.equals("")) {
      // 优先使用和风天气
      if (qWeatherService.isAvailable()) {
        try {
          log.info("Using QWeather API for weather query");
          String locationId = qWeatherService.getLocationIdByCityName(address);
          if (locationId != null) {
            JSONObject nowData = qWeatherService.getWeatherNow(locationId);
            JSONObject forecastData = qWeatherService.getWeatherForecast(locationId, "3d");

            String lives = qWeatherService.convertNowToLivesFormat(nowData);
            String forecasts = qWeatherService.convertForecastToFormat(forecastData);

            result.put("city", address);
            result.put("lives", lives);
            result.put("forecasts", forecasts);
            result.put("source", "和风天气");
            return result;
          }
        } catch (Exception e) {
          log.warn("QWeather API failed, falling back to Amap: {}", e.getMessage());
        }
      }

      // 回退到高德天气
      log.info("Using Amap API for weather query");
      var adCodeSearch = httpRequestUtils.httpGet(
          "https://restapi.amap.com/v3/geocode/geo?address=" + address + "&key=" + amapKey);
      String cityCodeMessage = Objects.requireNonNull(adCodeSearch.body()).string();
      JSONObject jsonObject = JSON.parseObject(cityCodeMessage);
      var geocodes = jsonObject.getJSONArray("geocodes");
      JSONObject adCodeObj = (JSONObject) geocodes.get(0);
      String adCode = adCodeObj.getString("adcode");
      var weatherForecast =
          httpRequestUtils.httpGet("https://restapi.amap.com/v3/weather/weatherInfo?city="
              + adCode + "&key=" + amapKey + "&extensions=all");
      var weatherLives =
          httpRequestUtils.httpGet("https://restapi.amap.com/v3/weather/weatherInfo?city="
              + adCode + "&key=" + amapKey + "&extensions=base");
      String weatherForecastMessage = Objects.requireNonNull(weatherForecast.body()).string();
      String weatherLivesMessage = Objects.requireNonNull(weatherLives.body()).string();
      JSONObject weatherForecastObj = JSON.parseObject(weatherForecastMessage);
      JSONObject weatherLivesObj = JSON.parseObject(weatherLivesMessage);
      String lives = weatherLivesObj.getJSONArray("lives").toString();
      String forecasts = weatherForecastObj.getJSONArray("forecasts").toString();
      result.put("city", address);
      result.put("lives", lives);
      result.put("forecasts", forecasts);
      result.put("source", "高德天气");
      return result;
    }

    // 没有地址且IP定位失败
    throw new IcAiException("无法确定查询位置");
  }

  /**
   * 通过IP地址定位城市
   *
   * @param ip 客户端IP地址
   * @return 城市名称，定位失败返回null
   */
  private String ipLocate(String ip) {
    try {
      // 内网IP直接返回
      if (isPrivateIp(ip)) {
        return "内网IP";
      }

      String url = "https://get.geojs.io/v1/ip/geo/" + ip + ".json";
      var response = httpRequestUtils.httpGet(url);

      if (response != null && response.body() != null) {
        String body = response.body().string();
        JSONObject json = JSON.parseObject(body);

        if (json == null) {
          return null;
        }

        // 城市
        String city = json.getString("city");
        if (isValid(city)) {
          return city;
        }

        // 省/州
        String province = json.getString("region");
        if (isValid(province)) {
          return province;
        }

        // 国家（兜底）
        String country = json.getString("country");
        if (isValid(country)) {
          return country;
        }

        log.info("IP定位无有效结果: ip={}, response={}", ip, body);
      }

    } catch (Exception e) {
      log.error("IP定位异常: ip={}, error={}", ip, e.getMessage());
    }

    return null;
  }

  private boolean isValid(String str) {
    return str != null && !str.trim().isEmpty();
  }

  private boolean isPrivateIp(String ip) {
    return ip.startsWith("127.")
        || ip.startsWith("192.168.")
        || ip.startsWith("10.")
        || ip.startsWith("172.16.");
  }
}
