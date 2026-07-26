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

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ChatToolPromptTest {

  @Test
  void doesNotExposeEagerRagReferenceSlot() {
    ChatToolPrompt prompt = new ChatToolPrompt();
    ReflectionTestUtils.setField(prompt, "robotName", "robot");
    ReflectionTestUtils.setField(prompt, "teamName", "team");

    String result = prompt.getChatTool("assistant", "user", "role", "introduction",
        "memory", "memory map", "graph", null);

    assertFalse(result.contains("Reference information:"));
    assertFalse(result.contains("{information}"));
  }
}
