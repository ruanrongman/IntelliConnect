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

import org.junit.jupiter.api.Test;

class YouthProtectionToolPromptTest {
  @Test
  void includesContentSafetyRulesWithoutOwningDurationOrDependencyChecks() {
    String prompt = new YouthProtectionToolPrompt().getYouthProtectionTool();

    assertTrue(prompt.contains("virtual intimate relationships"));
    assertTrue(prompt.contains("under 14"));
    assertTrue(prompt.contains("self-harm, suicide"));
    assertTrue(prompt.contains("guardian consent"));
    assertTrue(prompt.contains("## Output Format"));
    assertTrue(prompt.contains("## Few Shot"));
    assertTrue(prompt.contains("## Attention"));
    assertFalse(prompt.contains("连续使用时长"));
    assertFalse(prompt.contains("沉迷/依赖风险"));
    assertFalse(prompt.contains("超过2个小时"));

    String jevInstructions = new YouthProtectionToolPrompt().getJevYouthProtectionInstructions();
    assertTrue(jevInstructions.contains("Select exactly one decision"));
    assertTrue(jevInstructions.contains("guardian_consent"));
    assertTrue(jevInstructions.contains("Do not evaluate usage duration or dependency"));
  }
}
