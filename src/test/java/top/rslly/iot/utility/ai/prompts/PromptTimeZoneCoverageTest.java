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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import top.rslly.iot.utility.ai.Prompt;

import java.lang.reflect.Field;

class PromptTimeZoneCoverageTest {

  @Test
  @SuppressWarnings("deprecation")
  void everyCurrentTimePromptIncludesTimeZone() throws Exception {
    assertContainsTimeZone(ReactPrompt.class, "ReactSystem");
    assertContainsTimeZone(ReactPrompt.class, "FunctionCallingSystem");
    assertContainsTimeZone(ChatToolPrompt.class, "CHAT_PROMPT");
    assertContainsTimeZone(FunctionCallingRouterPrompt.class, "PROMPT");
    assertContainsTimeZone(ScheduleToolPrompt.class, "schedulePrompt");
    assertContainsTimeZone(Prompt.class, "classifierPrompt");
    assertContainsTimeZone(Prompt.class, "schedulePrompt");
  }

  private void assertContainsTimeZone(Class<?> promptClass, String fieldName) throws Exception {
    Field field = promptClass.getDeclaredField(fieldName);
    field.setAccessible(true);
    String prompt = (String) field.get(null);
    Assertions.assertTrue(prompt.contains("{time_zone}"),
        () -> promptClass.getSimpleName() + "." + fieldName + " must include {time_zone}");
  }
}
