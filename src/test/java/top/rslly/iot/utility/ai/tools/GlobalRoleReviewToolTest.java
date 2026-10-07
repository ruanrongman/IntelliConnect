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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alibaba.fastjson.JSONObject;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import top.rslly.iot.models.AdminConfigEntity;
import top.rslly.iot.services.AdminConfigServiceImpl;
import top.rslly.iot.utility.ai.jev.JevClient;
import top.rslly.iot.utility.ai.jev.JevResponse;
import top.rslly.iot.utility.ai.prompts.GlobalRoleReviewToolPrompt;

class GlobalRoleReviewToolTest {
  @Test
  void jevAllowsSafeRoleAndIncludesCustomRequirements() {
    JevClient jevClient = mock(JevClient.class);
    AdminConfigServiceImpl configService = mock(AdminConfigServiceImpl.class);
    AdminConfigEntity config = new AdminConfigEntity();
    config.setSetValue("必须保持教育性和友善");
    when(configService.findAllBySetKey(AdminConfigServiceImpl.GLOBAL_ROLE_REVIEW_REQUIREMENTS))
        .thenReturn(List.of(config));
    when(jevClient.evaluate(any(), anyMap())).thenAnswer(invocation -> {
      Map<?, ?> state = invocation.getArgument(0);
      Map<?, ?> questions = invocation.getArgument(1);
      assertTrue(String.valueOf(state.get("requirements")).contains("必须保持教育性和友善"));
      assertTrue(questions.containsKey("roleReview"));
      return jevChoice("allow");
    });

    GlobalRoleReviewTool tool = new GlobalRoleReviewTool();
    tool.setJevClient(jevClient);
    tool.setAdminConfigService(configService);
    tool.setGlobalRoleReviewToolPrompt(new GlobalRoleReviewToolPrompt());
    tool.setLlmName("decision-model");

    assertTrue(tool.run("Role: helpful tutor\nRole introduction: safe and educational"));
  }

  @Test
  void jevBlocksUnsafeRole() {
    JevClient jevClient = mock(JevClient.class);
    AdminConfigServiceImpl configService = mock(AdminConfigServiceImpl.class);
    when(configService.findAllBySetKey(any())).thenReturn(List.of());
    when(jevClient.evaluate(any(), anyMap())).thenReturn(jevChoice("block"));

    GlobalRoleReviewTool tool = new GlobalRoleReviewTool();
    tool.setJevClient(jevClient);
    tool.setAdminConfigService(configService);
    tool.setGlobalRoleReviewToolPrompt(new GlobalRoleReviewToolPrompt());
    tool.setLlmName("decision-model");

    assertFalse(tool.run("Role: violent criminal\nRole introduction: instructions for crime"));
  }

  @Test
  void emptyInputAllowsAndLlmDecisionsAreStrictlyParsed() {
    GlobalRoleReviewTool tool = new GlobalRoleReviewTool();
    assertTrue(tool.run("  "));
    assertTrue(GlobalRoleReviewTool.parseDecision(
        JSONObject.parseObject("{\"action\":{\"decision\":\"allow\"}}")));
    assertFalse(GlobalRoleReviewTool.parseDecision(
        JSONObject.parseObject("{\"action\":{\"decision\":\"block\"}}")));
    assertThrows(IllegalArgumentException.class, () -> GlobalRoleReviewTool.parseDecision(
        JSONObject.parseObject("{\"action\":{\"decision\":\"unknown\"}}")));
  }

  private static JevResponse jevChoice(String choice) {
    return new JevResponse("decision-model-preview", Map.of("roleReview",
        new JevResponse.ChoiceAnswer(choice, Map.of(), 0.95)), null);
  }
}
