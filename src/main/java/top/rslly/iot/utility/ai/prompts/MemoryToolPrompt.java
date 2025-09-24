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
          Please summarize the current memory content and the following user conversation into an inner monologue of no more than 1000 words
           ## Current Memory
             {current_memory}
           ## Output Format
           Please do not output \\n and try to limit the word count to 1000 words or less
           ## Current Conversation
              Below is the current conversation consisting of interleaving human and assistant history.
           """;

  public String getMemoryToolPrompt(String memory) {
    Map<String, String> params = new HashMap<>();
    params.put("current_memory", memory);
    return StringUtils.formatString(memoryToolPrompt, params);
  }
}
