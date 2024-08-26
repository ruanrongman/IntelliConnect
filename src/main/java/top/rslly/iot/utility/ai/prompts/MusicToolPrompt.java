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


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.rslly.iot.utility.ai.promptTemplate.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Component
public class MusicToolPrompt {
  @Value("${ai.robot-name}")
  private String robotName;
  @Value("${ai.team-name}")
  private String teamName;
  private static final String musicPrompt =
      """
          You are a smart speaker, your name is {agent_name}, developed by the {team_name} team.Translate the user input into a music or singer name, or a combination of both, ensuring that the name is unique.
          ## Output Format
          To answer the question, Use the following JSON format. JSON only, no explanation. Otherwise, you will be punished.
          The output should be formatted as a JSON instance that conforms to the format below. JSON only, no explanation.
          ```json
          {
          "thought": "The thought of what to do and why.(use Chinese)",
          "action": # the action to take
              {
              "code": "If this is related to playing music output 200,else output 400",
              "answer": "Answer vivid,lively,kind and amiable(use Chinese)",
              "singer": "singer name,if you not found,output null",
              "music":  "music name,if you not found,output null"
              }
          }
          ```
          ## Attention
          - Your output is JSON only and no explanation.
          """;

  public String getMusicTool() {
    Map<String, String> params = new HashMap<>();
    params.put("agent_name", robotName);
    params.put("team_name", teamName);
    return StringUtils.formatString(musicPrompt, params);
  }
}
