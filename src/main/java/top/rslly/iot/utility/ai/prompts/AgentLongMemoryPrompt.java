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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.rslly.iot.utility.ai.DescriptionUtil;
import top.rslly.iot.utility.ai.promptTemplate.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Component
public class AgentLongMemoryPrompt {
  @Autowired
  private DescriptionUtil descriptionUtil;
  private static final String agentLongMemoryPrompt =
      """
          Please refer to the following user conversation to see if it involves any of the following memory concepts.
          If the conversation contains or updates information related to these memory concepts, please update them accordingly.
          Otherwise, do not add to the memory_Key and memory_value lists.
          If none of the concepts match or are updated, you may set both lists to [].

          The current concept of memory and its content: {memory_map}
          ## Output Format
            ```json
             {
              "thought": "The thought of what to do and why.(use Chinese)",
              "action":
                  {
                  "memory_Key": [],
                  "memory_value": [],
                  }
             }
            ```
          ## Attention
            - Your output is JSON only and no explanation.
            - Each memory-value must be less than 800 words
          ## Current Conversation
            Below is the current conversation consisting of interleaving human and assistant history.
          """;

  public String getAgentLongMemory(int productId) {
    Map<String, String> params = new HashMap<>();
    params.put("memory_map", descriptionUtil.getAgentLongMemory(productId));
    return StringUtils.formatString(agentLongMemoryPrompt, params);
  }
}
