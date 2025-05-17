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

import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.rslly.iot.utility.ai.DescriptionUtil;
import top.rslly.iot.utility.ai.promptTemplate.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class ClassifierToolPrompt {
  private static final String classifierPrompt =
      """
           {task_map}
           You now need to classify based on user input
           ## Output Format
           ```json
           {
           "thought": "The thought of what to do and why.(use Chinese)",
           "action": # the action to take, must be one of provided tools
               {
               "code": "if success output 200,If it doesn't match any task,output 400",
               "value": "one of task No., json list data like [1],If it doesn't match, please output []",
               "args": "task input parameters(Combined with Current Conversation,Summarize the context,not null)"
               }
           }
           ```
           ## few shot
           if user input: Bundle the lamp product,key is XXX
           ```json
           {
           "thought": "用户想要绑定产品",
           "action":
               {
               "code": "200",
               "value": "[7]",
               "args": "Bundle the lamp product,key is XXX"
               }
           }
           ```
           ## Attention
           - Your output is JSON only and no explanation.
           ## Current Conversation
           Below is the current conversation consisting of interleaving human and assistant history.
          """;

  public String getClassifierTool() {
    Map<String, String> classifierMap = new HashMap<>();
    classifierMap.put("1", "Query weather");
    classifierMap.put("2", "Get the current time");
    classifierMap.put("3",
        "Operate and query electrical (including various intelligent devices such as robots, excluding playing music)");
    classifierMap.put("4", "Request a song or play music.(including Recommend music.)");
    classifierMap.put("5",
        "Complex tasks that require in-depth planning and thinking(like according to weather control electrical)");
    classifierMap.put("6",
        "Common chat");
    classifierMap.put("7", "Bind or Unbinding the product");
    classifierMap.put("8", "handoff controlled products");
    classifierMap.put("9", "Schedule management and reminder tasks");
    classifierMap.put("10",
        "Search for online(simplified to keywords)");
    classifierMap.put("11", "All about role and voice");
    classifierMap.put("12", "All use mcp server to do anything");
    String classifierJson = JSON.toJSONString(classifierMap);
    Map<String, String> params = new HashMap<>();
    params.put("task_map", classifierJson);
    return StringUtils.formatString(classifierPrompt, params);
  }
}
