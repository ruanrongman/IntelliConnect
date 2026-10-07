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
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.ModelMessageRole;
import top.rslly.iot.utility.ai.jev.JevClient;
import top.rslly.iot.utility.ai.jev.JevException;
import top.rslly.iot.utility.ai.jev.JevQuestion;
import top.rslly.iot.utility.ai.jev.JevResponse;
import top.rslly.iot.utility.ai.llm.LLM;
import top.rslly.iot.utility.ai.llm.LLMFactory;
import top.rslly.iot.utility.ai.prompts.YouthProtectionToolPrompt;

/**
 * Classifies whether content needs to be blocked or gated before it is shown to a minor.
 */
@Data
@Component
@Slf4j
public class YouthProtectionTool implements BaseTool<Boolean> {
  private static final String DECISION_MODEL = "decision-model";
  private static final String JEV_QUESTION_ID = "youthProtection";
  private static final String ALLOW_DECISION = "allow";

  @Autowired
  private YouthProtectionToolPrompt youthProtectionToolPrompt;
  @Autowired
  private JevClient jevClient;

  @Value("${ai.youthProtectionTool-llm:decision-model}")
  private String llmName;

  private String name = "youthProtectionTool";
  private String description =
      """
          Check whether content must be blocked or gated before being provided to a minor.
          Return true when content is safe to provide; return false for prohibited content or content requiring guardian consent.
          Args: content to review (str)
          """;

  @Override
  public Boolean run(String question) {
    return run(question, Map.of());
  }

  @Override
  public Boolean run(String question, Map<String, Object> globalMessage) {
    if (question == null || question.isBlank()) {
      return false;
    }

    try {
      if (DECISION_MODEL.equalsIgnoreCase(llmName)) {
        return evaluateWithJev(question);
      }
      LLM llm = LLMFactory.getLLM(llmName);
      List<ModelMessage> messages = new ArrayList<>();
      messages.add(new ModelMessage(ModelMessageRole.SYSTEM.value(),
          youthProtectionToolPrompt.getYouthProtectionTool()));
      messages.add(new ModelMessage(ModelMessageRole.USER.value(), question));
      JSONObject response = llm.jsonChat(question, messages, false);
      return parseSafetyDecision(response);
    } catch (JevException e) {
      if (e.getStatusCode() != null && e.getStatusCode() == 529
          && e.getResponseBody() != null && e.getResponseBody().contains("capacity is busy")) {
        log.warn("Jev 未成年人保护审核服务繁忙，按保护性策略拦截");
      } else {
        log.error("Jev 未成年人保护审核失败，按保护性策略拦截，statusCode={}",
            e.getStatusCode(), e);
      }
      return false;
    } catch (Exception e) {
      // A safety guard should fail closed when the model is unavailable or malformed.
      log.error("未成年人保护审核失败，按保护性策略拦截", e);
      return false;
    }
  }

  private boolean evaluateWithJev(String content) {
    JevResponse response = jevClient.evaluate(
        Map.of("content", content),
        Map.of(JEV_QUESTION_ID,
            JevQuestion.choice(youthProtectionToolPrompt.getJevYouthProtectionInstructions(),
                youthProtectionToolPrompt.getJevYouthProtectionCriteria())));
    if (response == null || response.answers() == null
        || !(response.answers()
            .get(JEV_QUESTION_ID)instanceof JevResponse.ChoiceAnswer choiceAnswer)) {
      throw new IllegalStateException("Missing or invalid Jev youth protection answer");
    }
    String decision = choiceAnswer.choice();
    if (decision == null
        || !youthProtectionToolPrompt.getJevYouthProtectionCriteria().containsKey(decision)) {
      throw new IllegalStateException("Jev returned an unsupported youth protection decision");
    }
    log.debug("Jev youth protection decision={}, confidence={}", decision,
        choiceAnswer.confidence());
    return ALLOW_DECISION.equals(decision);
  }

  static boolean parseSafetyDecision(JSONObject response) {
    if (response == null) {
      throw new IllegalArgumentException("Youth protection response is null");
    }
    JSONObject action = response.getJSONObject("action");
    if (action == null) {
      action = response;
    }

    Object decision = action.get("decision");
    if (decision != null) {
      String normalizedDecision = String.valueOf(decision).trim().toLowerCase(Locale.ROOT);
      if (normalizedDecision.equals("block")
          || normalizedDecision.equals("guardian_consent")
          || normalizedDecision.equals("guardian-consent")
          || normalizedDecision.equals("unsafe")) {
        return false;
      }
      if (normalizedDecision.equals("allow") || normalizedDecision.equals("safe")) {
        return true;
      }
    }

    Object code = action.get("code");
    if (code != null) {
      String normalizedCode = String.valueOf(code).trim();
      if (normalizedCode.equals("200")) {
        return true;
      }
      if (normalizedCode.equals("400")) {
        return false;
      }
    }

    for (String field : List.of("block_for_minor", "is_violation", "violation", "unsafe")) {
      Object value = action.get(field);
      if (value instanceof Boolean booleanValue) {
        return !booleanValue;
      }
      if (value instanceof String stringValue) {
        String normalizedValue = stringValue.trim().toLowerCase(Locale.ROOT);
        if (normalizedValue.equals("true") || normalizedValue.equals("yes")
            || normalizedValue.equals("block") || normalizedValue.equals("unsafe")) {
          return false;
        }
        if (normalizedValue.equals("false") || normalizedValue.equals("no")
            || normalizedValue.equals("allow") || normalizedValue.equals("safe")) {
          return true;
        }
      }
    }
    throw new IllegalArgumentException("Youth protection response has no valid decision");
  }
}
