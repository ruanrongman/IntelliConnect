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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import top.rslly.iot.models.ProductRoleEntity;
import top.rslly.iot.services.agent.ProductRoleServiceImpl;
import top.rslly.iot.utility.ai.DescriptionUtil;
import top.rslly.iot.utility.ai.promptTemplate.StringUtils;

import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class ReactPrompt {
  @Value("${ai.robot-name}")
  private String robotName;
  @Value("${ai.team-name}")
  private String teamName;
  @Autowired
  @Lazy
  private DescriptionUtil descriptionUtil;
  @Autowired
  private ProductRoleServiceImpl productRoleService;
  private static final String ReactSystem =
      """
          ## Identity
          You are {agent_name}, developed by the {team_name} team.
          Your role: {role}. {role_introduction}
          The user's name: {user_name}.
          Current time: {time} (Lunar: {lunar_date}).
          Long-term memory: {memory_map}

          ## Tools
          You have access to the following tools:
          {tool_descriptions}

          ## ReAct Protocol
          You are on step **{current_step}** of **{max_steps}**.

          On every step, output exactly ONE JSON object (no extra text):

          ```json
          {
            "thought": "<your reasoning in the user's language>",
            "action": {
              "name": "<tool_name>",
              "args": {}
            }
          }
          ```

          - `action.name` must be one of the provided tool names, or `"finish"`.
          - `action.args` must be a JSON object matching the tool's parameter schema.

          After each tool call, the system will return:
          ```
          Observation: <tool response>
          ```

          When you have enough information to answer, or when this is the final step, you MUST call `finish`:

          ```json
          {
            "thought": "<brief summary of reasoning>",
            "action": {
              "name": "finish",
              "args": {"content": "<your final answer>"}
            }
          }
          ```

          ## Termination Rules
          1. Default: complete the task in **1 step**. Use additional steps only when the first result is genuinely insufficient.
          2. If `current_step == max_steps`, you **MUST** call `finish` regardless.
          3. If a tool returns an error or empty result, either retry with different arguments or call `finish` with an explanation.
          4. Choose exactly ONE tool per step.

          ## Constraints
          - Output ONLY the JSON object. No markdown fences, no comments, no extra text.
          - The "thought" field and final answer language must match the user's language.
          - "thought" is visible to the user — stay in character.
          - Final answer should be ≤ 100 words and match your role's style.
          - No emojis in output.

          ## User Question
          {question}

          ## Current Conversation
          Below is the current conversation consisting of interleaving human and assistant messages.
          """;

  public String getReact(String toolDescriptions, String question, int productId,
      int currentStep, int maxSteps) {
    Map<String, String> params = new HashMap<>();
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Date date = new Date();
    String formattedDate = formatter.format(date);
    params.put("time", formattedDate);
    // ========== 农历时间 ==========
    String lunarDate = getLunarDateString(date);
    params.put("lunar_date", lunarDate);
    params.put("memory_map", descriptionUtil.getAgentLongMemory(productId));
    params.put("tool_descriptions", toolDescriptions);
    params.put("question", question);
    params.put("current_step", String.valueOf(currentStep));
    params.put("max_steps", String.valueOf(maxSteps));
    List<ProductRoleEntity> productRole = productRoleService.findAllByProductId(productId);
    String assistantName = null;
    String userName = null;
    String role = null;
    String roleIntroduction = null;
    if (!productRole.isEmpty()) {
      assistantName = productRole.get(0).getAssistantName();
      userName = productRole.get(0).getUserName();
      role = productRole.get(0).getRole();
      roleIntroduction = productRole.get(0).getRoleIntroduction();
    }
    if (assistantName != null)
      params.put("agent_name", assistantName);
    else
      params.put("agent_name", robotName);
    params.put("team_name", teamName);
    params.put("user_name", Objects.requireNonNullElse(userName, "user"));
    params.put("role", Objects.requireNonNullElse(role, "smart speaker"));
    params.put("role_introduction", Objects.requireNonNullElse(roleIntroduction,
        "你可以回答新闻内容和用户的各种合法请求，你回答的每句话都尽量口语化、简短"));
    return StringUtils.formatString(ReactSystem, params);
  }

  /**
   * 使用 Hutool 获取农历日期字符串
   *
   * @param date 公历日期
   * @return 农历日期字符串 例如：甲辰年腊月初五
   */
  private String getLunarDateString(Date date) {
    try {
      ChineseDate chineseDate = new ChineseDate(date);
      // 天干地支年份 + 生肖 + 月 + 日
      // 例如：甲辰年（龙年）腊月初五
      return chineseDate.getCyclical() // 甲辰年
          + "(" + chineseDate.getChineseZodiac() + "年)" // (龙年)
          + chineseDate.getChineseMonthName() // 腊月
          + chineseDate.getChineseDay() // 初五
          + chineseDate.getFestivals(); // 节日
    } catch (Exception e) {
      return "";
    }
  }
}
