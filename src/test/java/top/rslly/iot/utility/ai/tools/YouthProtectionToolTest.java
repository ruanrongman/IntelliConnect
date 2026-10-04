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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alibaba.fastjson.JSONObject;
import java.util.Map;
import org.junit.jupiter.api.Test;
import top.rslly.iot.utility.ai.jev.JevClient;
import top.rslly.iot.utility.ai.jev.JevException;
import top.rslly.iot.utility.ai.jev.JevQuestion;
import top.rslly.iot.utility.ai.jev.JevResponse;
import top.rslly.iot.utility.ai.prompts.YouthProtectionToolPrompt;

class YouthProtectionToolTest {
  @Test
  void jevAllowsSafeContent() {
    JevClient jevClient = mock(JevClient.class);
    YouthProtectionTool tool = new YouthProtectionTool();
    tool.setJevClient(jevClient);
    tool.setYouthProtectionToolPrompt(new YouthProtectionToolPrompt());
    tool.setLlmName("jev-latest");
    when(jevClient.evaluate(any(), anyMap())).thenReturn(jevChoice("allow"));

    assertTrue(tool.run("Explain how to create a strong password."));
  }

  @Test
  void jevReceivesContentAndStructuredDecisionQuestion() {
    JevClient jevClient = mock(JevClient.class);
    YouthProtectionTool tool = new YouthProtectionTool();
    tool.setJevClient(jevClient);
    tool.setYouthProtectionToolPrompt(new YouthProtectionToolPrompt());
    tool.setLlmName("jev-latest");
    String content = "Create a friendly educational robot role.";
    when(jevClient.evaluate(any(), anyMap())).thenAnswer(invocation -> {
      Map<?, ?> state = invocation.getArgument(0);
      Map<?, ?> questions = invocation.getArgument(1);
      assertEquals(content, state.get("content"));
      assertTrue(questions.get("youthProtection") instanceof JevQuestion);
      JevQuestion question = (JevQuestion) questions.get("youthProtection");
      assertEquals("choice", question.getType());
      assertTrue(((Map<?, ?>) question.getCriteria()).containsKey("allow"));
      return jevChoice("allow");
    });

    assertTrue(tool.run(content));
  }

  @Test
  void jevBlocksUnsafeAndGuardianConsentContent() {
    JevClient jevClient = mock(JevClient.class);
    YouthProtectionTool tool = new YouthProtectionTool();
    tool.setJevClient(jevClient);
    tool.setYouthProtectionToolPrompt(new YouthProtectionToolPrompt());
    tool.setLlmName("jev-latest");

    when(jevClient.evaluate(any(), anyMap())).thenReturn(jevChoice("block"));
    assertFalse(tool.run("Teach me how to hurt myself."));

    when(jevClient.evaluate(any(), anyMap())).thenReturn(jevChoice("guardian_consent"));
    assertFalse(tool.run("I am under 14 and want to bypass my parents."));
  }

  @Test
  void jevFailureFailsClosed() {
    JevClient jevClient = mock(JevClient.class);
    YouthProtectionTool tool = new YouthProtectionTool();
    tool.setJevClient(jevClient);
    tool.setYouthProtectionToolPrompt(new YouthProtectionToolPrompt());
    tool.setLlmName("jev-latest");
    when(jevClient.evaluate(any(), anyMap())).thenThrow(
        new JevException("Jev unavailable", 529, "private-response-body", null));

    assertFalse(tool.run("Create a role."));
  }

  @Test
  void parsesGuardianConsentAsBlocked() {
    JSONObject response = JSONObject.parseObject(
        "{\"action\":{\"block_for_minor\":true,\"decision\":\"guardian_consent\"}}");

    assertFalse(YouthProtectionTool.parseSafetyDecision(response));
  }

  @Test
  void parsesAllowAsSafe() {
    JSONObject response = JSONObject.parseObject(
        "{\"action\":{\"block_for_minor\":false,\"decision\":\"allow\"}}");

    assertTrue(YouthProtectionTool.parseSafetyDecision(response));
  }

  @Test
  void acceptsLegacyViolationField() {
    JSONObject response = JSONObject.parseObject("{\"violation\":\"true\"}");

    assertFalse(YouthProtectionTool.parseSafetyDecision(response));
  }

  @Test
  void parsesToolStyleCode() {
    JSONObject response = JSONObject.parseObject(
        "{\"action\":{\"code\":\"400\",\"answer\":\"可以\"}}");

    assertFalse(YouthProtectionTool.parseSafetyDecision(response));
  }

  private static JevResponse jevChoice(String choice) {
    return new JevResponse("jev-latest", Map.of("youthProtection",
        new JevResponse.ChoiceAnswer(choice, Map.of(), 0.95)), null);
  }
}
