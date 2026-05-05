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

import cn.hutool.core.date.ChineseDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.rslly.iot.utility.ai.promptTemplate.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class FunctionCallingRouterPrompt {
  @Value("${ai.robot-name}")
  private String robotName;
  @Value("${ai.team-name}")
  private String teamName;

  private static final String PROMPT =
      """
          Reply directly or call exactly one route function.

          Rules:
          1. When user ends conversation ("退下","再见","拜拜","不用陪了" etc), CALL route_step_back immediately, output NO text before or after the call.
          2. In direct replies, first sentence must be ≤7 chars for fast response; no echo, no filler, concise, one paragraph, no markdown/emoji/line breaks, use memory only when clearly helpful.
          3. NEVER mention any route_* name in text — just call the function.
          4. Decide from latest message first; treat clear standalone requests as new topics; use history only when ambiguous or continuing prior topic.
          5. Direct reply enough → no function. Tool clearly needed → call exactly one. Never reuse a route just because previous turn used it.
          6. Keep function arguments short and normalized.
          7. Do not route xiaozhi_device to control. If MCP available and request involves xiaozhi_device or seeing what's ahead, prefer MCP.
          8. route_step_back triggers: goodbye, bye, 退下, 再见, 拜拜, 不用陪了, 结束对话, 不聊了, 先这样吧, 今天就到这, 我下了.

          Profile:
          You are {agent_name}, developed by the {team_name} team.
          Role: {role}
          Role introduction: {role_introduction}
          User name: {user_name}

          Context:
          Current time: {time}
          Lunar date: {lunar_date}
          {information}
          {router_rules}
          Recent conversation:
          {recent_conversation}
          Memory categories: {memory_map}
          Related memory: {current_memory}
          {knowledge_graphic}
          {tts_control}
          """;

  public String build(String assistantName, String userName, String role, String roleIntroduction,
      String currentMemory, String information, String memoryMap, String knowledgeGraphic,
      String voice, String routerRules, String recentConversation) {
    Date date = new Date();
    Map<String, String> params = new HashMap<>();
    params.put("agent_name", Objects.requireNonNullElse(assistantName, robotName));
    params.put("team_name", teamName);
    params.put("role", Objects.requireNonNullElse(role, "智能助手"));
    params.put("role_introduction",
        Objects.requireNonNullElse(roleIntroduction, "你是一个友好、自然、简洁的对话伙伴。"));
    params.put("user_name", Objects.requireNonNullElse(userName, "user"));
    params.put("time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date));
    params.put("lunar_date", getLunarDateString(date));
    params.put("information", formatInformation(information));
    params.put("router_rules", formatRouterRules(routerRules));
    params.put("recent_conversation", defaultText(recentConversation, "none"));
    params.put("memory_map", defaultText(memoryMap, "none"));
    params.put("current_memory", defaultText(currentMemory, "none"));
    params.put("knowledge_graphic", formatKnowledgeGraphic(knowledgeGraphic));
    params.put("tts_control", buildTtsControlPrompt(voice));
    return StringUtils.formatString(PROMPT, params);
  }

  private String defaultText(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  private String formatInformation(String information) {
    if (information == null || information.isBlank()) {
      return "";
    }
    return "Reference information: " + information;
  }

  private String formatRouterRules(String routerRules) {
    if (routerRules == null || routerRules.isBlank()) {
      return "";
    }
    return "Custom routing rules: " + routerRules;
  }

  private String formatKnowledgeGraphic(String knowledgeGraphic) {
    if (knowledgeGraphic == null || knowledgeGraphic.isBlank()) {
      return "";
    }
    return "Knowledge graph context: " + knowledgeGraphic;
  }

  private String buildTtsControlPrompt(String voice) {
    if (voice == null || !voice.startsWith("minimax-")) {
      return "";
    }
    return """
        TTS guidance: you may use light para-verbal tags such as (laughs), (chuckle), (sighs), (emm), (breath) when they fit naturally.
        """;
  }

  private String getLunarDateString(Date date) {
    try {
      ChineseDate chineseDate = new ChineseDate(date);
      return chineseDate.getCyclical()
          + "(" + chineseDate.getChineseZodiac() + "年)"
          + chineseDate.getChineseMonthName()
          + chineseDate.getChineseDay()
          + chineseDate.getFestivals();
    } catch (Exception e) {
      return "";
    }
  }
}
