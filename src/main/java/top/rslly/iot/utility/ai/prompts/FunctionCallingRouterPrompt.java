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
          1. CRITICAL: When the user wants to end the conversation or asks you to step down (e.g., "退下", "你退下吧", "再见", "拜拜", "不用陪了"), you MUST CALL the route_step_back function. DO NOT reply directly with text. DO NOT output the function name "route_step_back" in your reply - actually CALL the function.
          2. EXTREMELY IMPORTANT: All function names like route_step_back, route_weather, route_mcp are INTERNAL ONLY. NEVER output or mention these route_* names in your text reply.
             - WRONG: Replying "route_step_back" as text
             - WRONG: Saying "I will call route_step_back"
             - CORRECT: Just actually CALL the route_step_back function directly
          3. Decide from the latest user message first.
          4. If the latest message is a clear standalone request, treat it as a new topic.
          5. Only use recent conversation when the latest message is ambiguous or clearly continues the previous topic.
          6. If a direct reply is enough, do not call any function.
          7. If a tool is clearly needed, call exactly one function.
          8. Do not choose a route only because the previous turn used it.
          9. Keep function arguments short and normalized.
          10. Do not route xiaozhi_device operations to control.
          11. If MCP is available and the request is about xiaozhi_device or seeing what is in front of the user, prefer MCP.
          12. Common triggers for route_step_back: goodbye, bye, see you, stop陪伴, leave, step down, end chat, end session, 退下, 你退下吧, 退下吧, 你可以退下了, 再见, 拜拜, 拜拜了, 再见了, 告别, 结束对话, 不用陪我了, 不用了, 先这样吧, 今天就到这里, 今天就到这, 不聊了, 先不聊了, 我先走了, 我下了, 下次再聊.
          13. In direct replies, do not echo the user's wording, do not repeat your own phrasing, avoid filler, keep chat concise, and use memory only when it clearly helps.
          14. Direct replies must be one paragraph, no markdown, no line breaks.

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
          Available routes:
          {available_routes}
          Recent conversation:
          {recent_conversation}
          Memory categories: {memory_map}
          Related memory: {current_memory}
          {knowledge_graphic}
          {tts_control}
          """;

  public String build(String assistantName, String userName, String role, String roleIntroduction,
      String currentMemory, String information, String memoryMap, String knowledgeGraphic,
      String voice, String routerRules, String availableRoutes, String recentConversation) {
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
    params.put("available_routes", defaultText(availableRoutes, "none"));
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
