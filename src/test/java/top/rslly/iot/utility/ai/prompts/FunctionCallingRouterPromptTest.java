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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class FunctionCallingRouterPromptTest {
  private FunctionCallingRouterPrompt prompt;

  @BeforeEach
  void setUp() {
    prompt = new FunctionCallingRouterPrompt();
    ReflectionTestUtils.setField(prompt, "robotName", "robot");
    ReflectionTestUtils.setField(prompt, "teamName", "team");
  }

  @Test
  void keepsSystemPromptStable() {
    String systemPrompt = prompt.build(
        "assistant", "user", "role", "role introduction",
        "memory categories", null, "router rules");

    assertTrue(systemPrompt.contains("Custom routing rules: router rules"));
    assertTrue(systemPrompt.contains("Memory categories: memory categories"));
    assertTrue(systemPrompt.contains("Only <current_user_request> is the user's latest message"));
    assertTrue(systemPrompt.contains("NEVER infer the user's topic, intent, route"));
    assertTrue(systemPrompt.contains("only from native conversation history"));
    assertTrue(systemPrompt.contains("native tool_calls protocol"));
    assertTrue(systemPrompt.contains("Plain text never invokes, represents, or confirms"));
    assertTrue(systemPrompt.contains("Requests that change or verify external state MUST call"));
    assertTrue(systemPrompt.contains("Without a native function call, NEVER claim"));
    assertTrue(systemPrompt.contains("silently verify assumptions, constraints"));
    assertTrue(systemPrompt.contains("Concise does not mean superficial"));
    assertFalse(systemPrompt.contains("short acknowledgment sentence"));
    assertFalse(systemPrompt.contains("Related memory:"));
    assertFalse(systemPrompt.contains("Current time:"));
    assertFalse(systemPrompt.contains("Reference information:"));
    assertFalse(systemPrompt.contains("Knowledge graph context:"));
  }

  @Test
  void placesDynamicContextAndQuestionInFinalUserMessage() {
    String userContext = prompt.buildUserContext(
        "current question", "related memory", "graph result");

    assertTrue(userContext.contains("Related memory: related memory"));
    assertTrue(userContext.contains("time zone: Asia/Shanghai"));
    assertFalse(userContext.contains("Reference information:"));
    assertTrue(userContext.contains("Knowledge graph context: graph result"));
    assertTrue(userContext.indexOf("<reference_context>") < userContext
        .indexOf("Knowledge graph context:"));
    assertTrue(userContext.indexOf("Knowledge graph context:") < userContext
        .indexOf("</reference_context>"));
    assertTrue(
        userContext.indexOf("<current_user_request>") < userContext.indexOf("current question"));
    assertTrue(
        userContext.indexOf("</reference_context>") < userContext
            .indexOf("<current_user_request>"));
    assertTrue(
        userContext.indexOf("current question") < userContext.indexOf("</current_user_request>"));
  }
}
