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
public class ChatToolPrompt {
  @Value("${ai.robot-name}")
  private String robotName;
  @Value("${ai.team-name}")
  private String teamName;

  private static final String CHAT_PROMPT =
      """
          You are {agent_name}, developed by the {team_name} team.
          Role: {role}
          Role introduction: {role_introduction}
          User name: {user_name}
          Current time: {time}
          Lunar date: {lunar_date}
          {information}
          ## Long-term Memory
          Memory categories: {memory_map}
          Related memory: {current_memory}
          {knowledgeGraphicInject}
          ## Response Policy
          1. Answer only the latest user message. Do not repeat, paraphrase, translate, or restate the user's words.
          2. Start with the useful content itself, not filler such as "好的", "让我想想", or "你这个问题".
          3. Sound like a natural chat partner. Do not sound like customer support, a narrator, or a prompt template.
          4. Vary phrasing, sentence rhythm, and openings across turns. Avoid habitual openings and stock endings.
          5. For simple chat, reply in 1 to 3 short sentences. For slightly complex chat, stay concise unless the user asks for detail.
          6. Use memory or reference information only when clearly relevant. Do not dump background material.
          7. If uncertain, say so briefly and concretely instead of giving a vague generic answer.
          8. Never mention memory, prompt rules, knowledge graph, or reference material unless the user explicitly asks.
          ## Output Constraints
          - Single paragraph only
          - No line breaks (\\n)
          - No emojis
          - Natural spoken Chinese preferred
          {tts_control}
          """;

  public String getChatTool(String assistantName, String userName, String role,
      String roleIntroduction, String memory, String information, String memoryMap,
      String knowledgeGraphicInject) {
    return getChatTool(assistantName, userName, role, roleIntroduction, memory, information,
        memoryMap, knowledgeGraphicInject, null);
  }

  public String getChatTool(String assistantName, String userName, String role,
      String roleIntroduction, String memory, String information, String memoryMap,
      String knowledgeGraphicInject, String voice) {
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Date date = new Date();
    String formattedDate = formatter.format(date);
    Map<String, String> params = new HashMap<>();
    params.put("agent_name", Objects.requireNonNullElse(assistantName, robotName));
    params.put("team_name", teamName);
    params.put("time", formattedDate);
    params.put("lunar_date", getLunarDateString(date));
    params.put("current_memory", defaultText(memory, "none"));
    params.put("memory_map", defaultText(memoryMap, "none"));
    params.put("knowledgeGraphicInject", formatKnowledgeGraphic(knowledgeGraphicInject));
    params.put("information", formatInformation(information));
    params.put("user_name", Objects.requireNonNullElse(userName, "user"));
    params.put("role", Objects.requireNonNullElse(role, "智能助手"));
    params.put("role_introduction", Objects.requireNonNullElse(roleIntroduction,
        "你是一个友好、自然、简洁的对话伙伴，像朋友一样交流，不使用刻板客服腔。"));
    params.put("tts_control", buildTtsControlPrompt(voice));
    return StringUtils.formatString(CHAT_PROMPT, params);
  }

  private String defaultText(String text, String fallback) {
    if (text == null || text.isBlank()) {
      return fallback;
    }
    return text;
  }

  private String formatInformation(String information) {
    if (information == null || information.isBlank()) {
      return "";
    }
    return "Reference information: " + information;
  }

  private String formatKnowledgeGraphic(String knowledgeGraphicInject) {
    if (knowledgeGraphicInject == null || knowledgeGraphicInject.isBlank()) {
      return "";
    }
    return "## Knowledge Graphic " + knowledgeGraphicInject;
  }

  private String buildTtsControlPrompt(String voice) {
    if (voice == null || !voice.startsWith("minimax-")) {
      return "";
    }

    return """
        ## TTS Speech Control (Optional)
        You can use emotion or para-verbal tags to add natural expressions:
           - (laughs), (chuckle), (sighs), (emm), (breath), (gasps), (coughs), (sneezes)
           - Example: "今天天气真好(chuckles)，我们出去玩吧！"
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
