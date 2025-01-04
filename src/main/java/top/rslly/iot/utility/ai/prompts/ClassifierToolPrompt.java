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
  @Value("${ai.robot-name}")
  private String robotName;
  @Value("${ai.team-name}")
  private String teamName;
  private static final String classifierPrompt =
      """
           As a smart speaker, your name is {agent_name}, developed by the {team_name} team. You are good at helping people doing the following task
           {task_map}
           You now need to classify based on user input
           ## Output Format
           To answer the question, Use the following JSON format. JSON only, no explanation. Otherwise, you will be punished.
           The output should be formatted as a JSON instance that conforms to the format below. JSON only, no explanation.
           ```json
           {
           "thought": "The thought of what to do and why.(use Chinese)",
           "action": # the action to take, must be one of provided tools
               {
               "code": "if success output 200,If it doesn't match any task,output 400",
               "answer": "Task 1,3,4,5,6 just need to answer "yes",no explanation.other Answer vivid,lively,kind and amiable",
               "value": "one of task No., json list data like [1],If it doesn't match, please output []",
               "args": "task input parameters(Combined with Current Conversation,Summarize the context,not null)"
               }
           }
           ```
           ## few shot
           if user input: What's the weather like today ?
           ```json
           {
           "thought": "用户想要查询天气",
           "action":
               {
               "code": "200",
               "answer": "yes",
               "value": "[1]",
               "args": "What's the weather like today"
               }
           }
           ```
           if user input: what time is it ?
           ```json
           {
           "thought": "用户希望查询时间",
           "action":
               {
               "code": "200",
               "answer": "现在时间是 XXX",
               "value": "[2]",
               "args": "ok"
               }
           }
           ```
           if user input: Bundle the lamp product,key is XXX
           ```json
           {
           "thought": "用户想要绑定产品",
           "action":
               {
               "code": "200",
               "answer": "yes",
               "value": "[7]",
               "args": "Bundle the lamp product,key is XXX"
               }
           }
           ```
           reference information: The current time is {time}
           ## Attention
           - Your output is JSON only and no explanation.
           ## Current Conversation
           Below is the current conversation consisting of interleaving human and assistant history.
          """;

  public String getClassifierTool() {
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Date date = new Date();
    String formattedDate = formatter.format(date);
    Map<String, String> classifierMap = new HashMap<>();
    classifierMap.put("1", "Query weather");
    classifierMap.put("2", "Get the current time");
    classifierMap.put("3",
        "Operate electrical and query electrical status(Excluding playing music.)");
    classifierMap.put("4", "Request a song or play music.(including Recommend music.)");
    classifierMap.put("5",
        "Complex tasks that require in-depth planning and thinking(like according to weather control electrical)");
    classifierMap.put("6",
        "Other tasks (including chatting,coding,write paper,Get news or unknown events,translate and etc.)");
    classifierMap.put("7", "Bind or Unbinding the product");
    classifierMap.put("8", "handoff controlled products");
    classifierMap.put("9", "Schedule management and reminder tasks");
    classifierMap.put("10",
        "Search for online(Please simplify user input to meet the needs of search engines)");
    String classifierJson = JSON.toJSONString(classifierMap);
    Map<String, String> params = new HashMap<>();
    params.put("agent_name", robotName);
    params.put("team_name", teamName);
    params.put("task_map", classifierJson);
    params.put("time", formattedDate);
    return StringUtils.formatString(classifierPrompt, params);
  }
}
