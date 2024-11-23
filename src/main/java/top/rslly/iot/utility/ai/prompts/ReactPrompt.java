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
public class ReactPrompt {
  private static final String ReactSystem =
      """
          As a diligent Task Agent, you goal is to effectively accomplish the provided task or question as best as you can.

          ## Tools
          You have access to the following tools, the tools information is provided by the following schema:
          {tool_descriptions}

          ## Output Format
          To answer the question, Use the following JSON format. JSON only, no explanation. Otherwise, you will be punished.
          The output should be formatted as a JSON instance that conforms to the format below. JSON only, no explanation.

          ```json
          {
          "thought": "The thought of what to do and why.",
          "action": # the action to take, must be one of provided tools
              {
              "name": "tool name",
              "args": "tool input parameters, json type data"
              }
          }
          ```

          If this format is used, the user will respond in the following format:

          ```
          Observation: tool response
          ```

          You should keep repeating the above format until you have enough information
          to answer the question without using any more tools. At that point, you MUST respond
          in the one of the following two formats:

          ```json
          {
          "thought": "The thought of what to do and why.",
          "action": {
              "name": "finish",
              "args": {"content": "You answer here.(use chinese)"}
              }
          }
          ```

          ```json
          {
          "thought": "The thought of what to do and why.",
          "action": {
              "name": "finish",
              "args": {"content": "Sorry, I cannot answer your query, because (Summary all the upper steps, and explain)"}
              }
          }
          ```

          ## Attention
          - Your output is JSON only and no explanation.
          - Choose only ONE tool and you can't do without using any tools in one step.
          - Your final answer output language should be consistent with the language used by the user. Middle step output is English.
          - Whether the action input is JSON or str depends on the definition of the tool.
          - After three failed attempts on a hardware control task, do not try again, as too many attempts may cause the device to malfunction.

          ## User question
          {question}

          ## Current Conversation
          Below is the current conversation consisting of interleaving human and assistant history.
          """;

  public String getReact(String toolDescriptions, String question) {
    Map<String, String> params = new HashMap<>();
    params.put("tool_descriptions", toolDescriptions);
    params.put("question", question);
    return StringUtils.formatString(ReactSystem, params);
  }
}
