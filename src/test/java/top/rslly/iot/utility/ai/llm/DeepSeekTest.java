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
package top.rslly.iot.utility.ai.llm;

import com.openai.core.JsonValue;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.ModelMessageRole;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

class DeepSeekTest {

  @Test
  void officialDeepSeekDisablesThinkingWithOfficialParameter() throws Exception {
    ChatCompletionCreateParams params = buildTextParams(
        new DeepSeek("https://api.deepseek.com", "deepseek-v3.1", "test-key", false, 128));

    Map<String, JsonValue> additionalBody = params._additionalBodyProperties();
    Assertions.assertFalse(additionalBody.containsKey("enable_thinking"));
    Assertions.assertEquals("disabled",
        additionalBody.get("thinking").convert(Map.class).get("type"));
  }

  @Test
  void compatibleProviderKeepsLegacyDisableThinkingParameter() throws Exception {
    ChatCompletionCreateParams params = buildTextParams(
        new DeepSeek("https://dashscope.aliyuncs.com/compatible-mode", "deepseek-v4-flash",
            "test-key", false, 128));

    Map<String, JsonValue> additionalBody = params._additionalBodyProperties();
    Assertions.assertEquals(Boolean.FALSE,
        additionalBody.get("enable_thinking").convert(Boolean.class));
    Assertions.assertFalse(additionalBody.containsKey("thinking"));
  }

  @Test
  void officialDeepSeekEnablesThinkingWithOfficialParameter() throws Exception {
    ChatCompletionCreateParams params = buildTextParams(
        new DeepSeek("https://api.deepseek.com", "deepseek-chat", "test-key", true, 256));

    Map<String, JsonValue> additionalBody = params._additionalBodyProperties();
    Assertions.assertFalse(additionalBody.containsKey("enable_thinking"));
    Assertions.assertFalse(additionalBody.containsKey("thinking_budget"));
    Assertions.assertEquals("enabled",
        additionalBody.get("thinking").convert(Map.class).get("type"));
  }

  @Test
  void compatibleProviderEnableThinkingKeepsExistingThinkingBudgetParameters() throws Exception {
    ChatCompletionCreateParams params = buildTextParams(
        new DeepSeek("https://dashscope.aliyuncs.com/compatible-mode", "deepseek-chat",
            "test-key", true, 256));

    Map<String, JsonValue> additionalBody = params._additionalBodyProperties();
    Assertions.assertEquals(Boolean.TRUE,
        additionalBody.get("enable_thinking").convert(Boolean.class));
    Assertions.assertEquals(256,
        additionalBody.get("thinking_budget").convert(Integer.class));
    Assertions.assertFalse(additionalBody.containsKey("thinking"));
  }

  private ChatCompletionCreateParams buildTextParams(DeepSeek deepSeek) throws Exception {
    Method method = DeepSeek.class.getDeclaredMethod("buildTextParams", List.class);
    method.setAccessible(true);
    return (ChatCompletionCreateParams) method.invoke(deepSeek, List.of(
        new ModelMessage(ModelMessageRole.USER.value(), "hello")));
  }
}
