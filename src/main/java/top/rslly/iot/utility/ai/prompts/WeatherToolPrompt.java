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
package top.rslly.iot.utility.ai.prompts;

import com.zhipu.oapi.utils.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class WeatherToolPrompt {
  @Value("${ai.robot-name}")
  private String robotName;
  @Value("${ai.team-name}")
  private String teamName;
  private static final String weatherPrompt =
      """
          You are a smart speaker, your name is {agent_name}, developed by the {team_name} team.Simplify user input to city names.
           ## Output Format
           ```json
           {
           "thought": "The thought of what to do and why.(use Chinese)",
           "action":
               {
               "code": "If this is related to weather output 200,else output 400",
               "answer": "Answer vivid,lively,kind and amiable(use Chinese)",
               "address": "Structured addresses"
               }
           }
           ```
           ## Attention
           - Your output is JSON only and no explanation.
           - First of all, the address must be a string of characters, containing the names of buildings such as country, province, city, district, town, village, street, house number, housing estate, building, etc.
             Characters grouped together from the name of the large region to the name of the small region. A valid address should be unique.
             Note: The country information can be selectively ignored during the geocoding conversion for the mainland, Hong Kong, and Macao, but the address composition at the provincial, municipal,
             and urban levels cannot be ignored.
           ## Current Conversation
           Below is the current conversation consisting of interleaving human and assistant history.
          """;
  private static final String weatherArrangePrompt = """
      请根据用户的请求，合理使用下列信息，使用人类友善的语言来回答，并推荐用户穿衣以及出行建议,句子尽量不要出现markdown等符号以方便tts系统。
      ## weather information
      当前天气信息{lives}
      未来几天的天气信息{forecast}
      """;

  public String getWeatherTool() {
    Map<String, String> params = new HashMap<>();
    params.put("agent_name", robotName);
    params.put("team_name", teamName);
    return StringUtils.formatString(weatherPrompt, params);
  }

  public String getWeatherResponse(String lives, String forecast) {
    Map<String, String> params = new HashMap<>();
    params.put("lives", lives);
    params.put("forecast", forecast);
    return StringUtils.formatString(weatherArrangePrompt, params);
  }

}
