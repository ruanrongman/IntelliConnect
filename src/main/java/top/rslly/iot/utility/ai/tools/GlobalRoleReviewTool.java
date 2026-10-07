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

import com.alibaba.fastjson.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.rslly.iot.services.AdminConfigServiceImpl;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.ModelMessageRole;
import top.rslly.iot.utility.ai.jev.JevClient;
import top.rslly.iot.utility.ai.jev.JevException;
import top.rslly.iot.utility.ai.jev.JevQuestion;
import top.rslly.iot.utility.ai.jev.JevResponse;
import top.rslly.iot.utility.ai.llm.LLM;
import top.rslly.iot.utility.ai.llm.LLMFactory;
import top.rslly.iot.utility.ai.prompts.GlobalRoleReviewToolPrompt;

/** Reviews product role text against global administrator-defined safety rules. */
@Data
@Component
@Slf4j
public class GlobalRoleReviewTool implements BaseTool<Boolean> {
  private static final String DECISION_MODEL = "decision-model";
  private static final String JEV_QUESTION_ID = "roleReview";
  private static final String ALLOW = "allow";

  @Autowired
  private JevClient jevClient;
  @Autowired
  private AdminConfigServiceImpl adminConfigService;
  @Autowired
  private GlobalRoleReviewToolPrompt globalRoleReviewToolPrompt;

  @Value("${ai.globalRoleReview-llm:decision-model}")
  private String llmName;

  private String name = "globalRoleReviewTool";
  private String description =
      """
          Review a product role name and introduction against global safety rules.
          Return true when the role is safe to save; return false only for an explicit block.
          Args: role name and role introduction to review (str)
          """;

  @Override
  public Boolean run(String question) {
    if (question == null || question.isBlank()) {
      log.warn("全局角色审核输入为空，按审核异常放行");
      return true;
    }
    String requirements = loadRequirements();
    try {
      if (DECISION_MODEL.equalsIgnoreCase(llmName)) {
        return evaluateWithJev(question, requirements);
      }
      return evaluateWithLlm(question, requirements);
    } catch (JevException e) {
      log.warn("全局角色 Jev 审核失败，按配置放行，statusCode={}", e.getStatusCode(), e);
      return true;
    } catch (Exception e) {
      log.warn("全局角色审核失败，按配置放行，model={}", llmName, e);
      return true;
    }
  }

  @Override
  public Boolean run(String question, Map<String, Object> globalMessage) {
    return run(question);
  }

  private boolean evaluateWithJev(String content, String requirements) {
    JevResponse response = jevClient.evaluate(
        Map.of("role_content", content, "requirements", requirements),
        Map.of(JEV_QUESTION_ID, JevQuestion.choice(
            globalRoleReviewToolPrompt.getJevGlobalRoleReviewInstructions(defaultRules(),
                requirements),
            globalRoleReviewToolPrompt.getJevGlobalRoleReviewCriteria())));
    if (response == null || response.answers() == null
        || !(response.answers().get(JEV_QUESTION_ID)instanceof JevResponse.ChoiceAnswer answer)) {
      throw new IllegalStateException("Missing global role review answer");
    }
    String decision = answer.choice();
    if (!globalRoleReviewToolPrompt.getJevGlobalRoleReviewCriteria().containsKey(decision)) {
      throw new IllegalStateException("Unsupported global role review decision");
    }
    return ALLOW.equals(decision);
  }

  private boolean evaluateWithLlm(String content, String requirements) {
    LLM llm = LLMFactory.getLLM(llmName);
    List<ModelMessage> messages = new ArrayList<>();
    messages.add(new ModelMessage(ModelMessageRole.SYSTEM.value(),
        globalRoleReviewToolPrompt.getGlobalRoleReviewTool(defaultRules(), requirements)));
    messages.add(new ModelMessage(ModelMessageRole.USER.value(), content));
    return parseDecision(llm.jsonChat(content, messages, false));
  }

  private String loadRequirements() {
    try {
      var configs = adminConfigService.findAllBySetKey(
          AdminConfigServiceImpl.GLOBAL_ROLE_REVIEW_REQUIREMENTS);
      if (!configs.isEmpty() && configs.get(0).getSetValue() != null
          && !configs.get(0).getSetValue().isBlank()) {
        String configured = configs.get(0).getSetValue().trim();
        if (!configured.equals(AdminConfigServiceImpl.DEFAULT_GLOBAL_ROLE_REVIEW_REQUIREMENTS)) {
          return configured;
        }
      }
    } catch (Exception e) {
      log.warn("读取全局角色审核要求失败，使用默认要求", e);
    }
    return AdminConfigServiceImpl.DEFAULT_GLOBAL_ROLE_REVIEW_REQUIREMENTS;
  }

  private String defaultRules() {
    return AdminConfigServiceImpl.DEFAULT_GLOBAL_ROLE_REVIEW_REQUIREMENTS;
  }

  static boolean parseDecision(JSONObject response) {
    if (response == null) {
      throw new IllegalArgumentException("Global role review response is null");
    }
    JSONObject action = response.getJSONObject("action");
    if (action == null) {
      action = response;
    }
    Object decision = action.get("decision");
    if (decision != null) {
      String normalized = String.valueOf(decision).trim().toLowerCase(Locale.ROOT);
      if (ALLOW.equals(normalized) || "safe".equals(normalized)) {
        return true;
      }
      if ("block".equals(normalized) || "unsafe".equals(normalized)
          || "reject".equals(normalized)) {
        return false;
      }
    }
    Object code = action.get("code");
    if (code != null) {
      String normalized = String.valueOf(code).trim();
      if ("200".equals(normalized)) {
        return true;
      }
      if ("400".equals(normalized)) {
        return false;
      }
    }
    throw new IllegalArgumentException("Global role review response has no valid decision");
  }
}
