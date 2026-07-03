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

import java.lang.reflect.Field;

class ReactPromptTest {

  @Test
  void reactPromptRemovesFinalAnswerWordLimit() throws Exception {
    String reactSystem = readPromptConstant("ReactSystem");

    Assertions.assertFalse(reactSystem.contains("100 words"));
    Assertions.assertFalse(reactSystem.contains("≤ 100 words"));
    Assertions.assertTrue(reactSystem.contains("Final answer should match your role's style."));
  }

  @Test
  void functionCallingPromptKeepsProgressSeparateFromFinalAnswer() throws Exception {
    String functionCallingSystem = readPromptConstant("FunctionCallingSystem");

    Assertions.assertFalse(functionCallingSystem.contains("100 words"));
    Assertions.assertTrue(functionCallingSystem.contains("progress text and not the final answer"));
    Assertions.assertTrue(functionCallingSystem.contains("must still call the function"));
    Assertions.assertTrue(
        functionCallingSystem.contains("Final answers must not include internal tool traces"));
  }

  private String readPromptConstant(String fieldName) throws Exception {
    Field field = ReactPrompt.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    return (String) field.get(null);
  }
}
