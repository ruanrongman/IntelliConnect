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

import org.springframework.stereotype.Component;
import top.rslly.iot.utility.ai.promptTemplate.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Component
public class EmotionToolPrompt {
  private static final String emotionToolPrompt =
      """
          Analyze the conversation context and select the most appropriate emotion name from the given emotion library. Follow the rules strictly.
          ## emotion Library
          [
             "neutral", "happy", "laughing", "funny", "sad",
             "angry", "crying", "loving", "embarrassed", "surprised",
             "shocked", "thinking", "winking", "cool", "relaxed",
             "delicious", "kissy", "confident", "sleepy", "silly", "confused"
          ]
          ## Output Format
          ```json
          {
          "thought": "The thought of what to do and why.(use Chinese)",
          "action": # the action to take
              {
              "emotion":  "Select an emoji"
              }
          }
          ```
          ## Attention
          - Your output is JSON only and no explanation.
          ## Current Memory
             {current_memory}
          ## Current Conversation
             Below is the current conversation consisting of interleaving human and assistant history.
          """;

  public String getEmotionTool(String memory) {
    Map<String, String> params = new HashMap<>();
    params.put("current_memory", memory);
    return StringUtils.formatString(emotionToolPrompt, params);
  }
}
