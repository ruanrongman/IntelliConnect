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

import org.springframework.stereotype.Component;
import top.rslly.iot.utility.ai.promptTemplate.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Component
public class MemoryToolPrompt {
  private static final String memoryToolPrompt =
      """
          You are maintaining long-term chat memory for future conversations.
          Merge the existing memory with the latest conversation and produce a concise memory summary.
          Keep only information that is likely to matter in later turns.
          ## Current Memory
          {current_memory}
          ## Memory Rules
          1. Preserve stable facts about the user, assistant, devices, relationships, preferences, habits, goals, and unresolved tasks.
          2. Preserve short-lived context only when it is likely to matter soon, such as recent plans, current issues, or follow-up items.
          3. Remove stale, trivial, repetitive, or low-value content.
          4. Do not copy the conversation verbatim.
          5. Do not store filler phrases, tone habits, catchphrases, politeness formulas, or response style preferences inferred only from one reply.
          6. Do not write as an inner monologue, diary, or narrative.
          7. Prefer compact factual statements over explanation.
          8. If the latest conversation does not add useful memory, keep the existing memory but clean up duplication.
          ## Output Format
          Output a single-paragraph memory summary in Chinese.
          No line breaks.
          Keep it under 500 Chinese characters when possible, and never exceed 1000 characters.
          ## Current Conversation
          Below is the current conversation consisting of interleaving human and assistant history.
          """;

  public String getMemoryToolPrompt(String memory) {
    Map<String, String> params = new HashMap<>();
    params.put("current_memory", memory);
    return StringUtils.formatString(memoryToolPrompt, params);
  }
}
