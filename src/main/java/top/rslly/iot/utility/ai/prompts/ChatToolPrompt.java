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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class ChatToolPrompt {
  @Value("${ai.robot-name}")
  private String robotName;
  @Value("${ai.team-name}")
  private String teamName;
  private static final String chatPrompt =
      """
          Your role is {role}, {role_introduction},your name is {agent_name},developed by the {team_name} team.
          The user's name is {user_name}
          reference information: The current time is {time}
          ## Current Memory
            {current_memory}
          ## Output Format
          Please do not output \\n and try to limit the word count to 100 words or less
          ## Current Conversation
             Below is the current conversation consisting of interleaving human and assistant history.
          """;

  public String getChatTool(String assistantName, String userName, String role,
      String roleIntroduction, String memory) {
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Date date = new Date();
    String formattedDate = formatter.format(date);
    Map<String, String> params = new HashMap<>();
    if (assistantName != null)
      params.put("agent_name", assistantName);
    else
      params.put("agent_name", robotName);
    params.put("team_name", teamName);
    params.put("time", formattedDate);
    params.put("current_memory", memory);
    params.put("user_name", Objects.requireNonNullElse(userName, "user"));
    params.put("role", Objects.requireNonNullElse(role, "smart speaker"));
    params.put("role_introduction", Objects.requireNonNullElse(roleIntroduction,
        "你可以回答新闻内容和用户的各种合法请求，你回答的每句话都尽量口语化、简短,总是喜欢使用表情符号"));
    return StringUtils.formatString(chatPrompt, params);
  }
}
