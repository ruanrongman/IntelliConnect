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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.rslly.iot.utility.HttpRequestUtils;
import top.rslly.iot.utility.ai.IcAiException;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.ModelMessageRole;
import top.rslly.iot.utility.ai.Prompt;
import top.rslly.iot.utility.ai.llm.LLM;
import top.rslly.iot.utility.ai.llm.LLMFactory;

import java.io.IOException;
import java.util.*;

@Data
@Component
public class WeatherTool implements BaseTool<String> {
  @Autowired
  private Prompt prompt;
  @Value("${weather.key}")
  private String AmapKey;
  @Autowired
  private HttpRequestUtils httpRequestUtils;
  @Value("${ai.weatherTool-llm}")
  private String llmName;
  private String name = "weatherTool";
  private String description = """
      Get live weather facts and forecasts
      Args: The weather in xx city at xx time(str)
      """;

  @Override
  public String run(String question) {
    LLM llm = LLMFactory.getLLM(llmName);
    List<ModelMessage> messages = new ArrayList<>();

    ModelMessage systemMessage =
        new ModelMessage(ModelMessageRole.SYSTEM.value(), prompt.getWeatherTool());
    ModelMessage userMessage = new ModelMessage(ModelMessageRole.USER.value(), question);
    messages.add(systemMessage);
    messages.add(userMessage);
    var obj = llm.jsonChat(question, messages, false).getJSONObject("action");
    try {
      Map<String, String> answer = process_llm_result(obj);
      ModelMessage responseSystemMessage =
          new ModelMessage(ModelMessageRole.SYSTEM.value(),
              prompt.getWeatherResponse(answer.get("lives"), answer.get("forecasts")));
      messages.clear();
      messages.add(responseSystemMessage);
      messages.add(userMessage);
      return llm.commonChat(question, messages, false);
    } catch (Exception e) {
      // e.printStackTrace();
    }
    return "查询天气失败，请检查是否输入具体城市名称。";
  }

  @Override
  public String run(String question, int productId) {
    return this.run(question);
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
