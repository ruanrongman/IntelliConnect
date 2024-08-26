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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.rslly.iot.utility.ai.DescriptionUtil;
import top.rslly.iot.utility.ai.promptTemplate.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Component
public class ControlToolPrompt {
  @Autowired
  private DescriptionUtil descriptionUtil;
  @Value("${ai.robot-name}")
  private String robotName;
  @Value("${ai.team-name}")
  private String teamName;
  private static final String controlPrompt =
      """
          As a smart speaker, your name is {agent_name}, developed by the {team_name} team.  You are good at helping people operate various appliances，
          You can only control the following electrical type:{electrical_name}
          The electrical properties and value that can be controlled by each electrical:{properties_value}
          The device of current electrical type and the latest status:{equipment_status}
          value ["null"] means devices information is not exist
          If you find the user wants to control is not in the above range ,the device is disconnected or allow is false,
          you can not control it,and you should warn the user in the answer param
          ## Output Format
          To answer the question, Use the following JSON format. JSON only, no explanation. Otherwise, you will be punished.
          The output should be formatted as a JSON instance that conforms to the format below. JSON only, no explanation.
          ```json
          {
          "thought": "The thought of what to do and why.(use Chinese)",
          "action": # the action to take
              {
              "answer": "Answer vivid,lively,kind to user requests immediately based on data(use Chinese)",
              "controlParameters":[
               {
                "name": "device name,If it doesn't match, please output null",
                "code": "if device connect output 200,else output 400",
                "taskType": "If it is a control task, output control; otherwise, output 'query'."
                "properties": "electronic input parameters, json list data,If there is no such properties, please output []",
                "value": "electronic input parameters, json list data like ["on",30],If it doesn't match Or the user is queried, please output []"
               }
              ]
              }
          }
          ```
          ## Attention
          - Your output is JSON only and no explanation.
          - Please respond to the user's request immediately, rather than using terms like "later" or "then"
          - If device cannot be controlled, such as not allow or disconnected, please inform the user
          """;

  public String getControlTool(int productId) {
    Map<String, String> params = new HashMap<>();
    params.put("agent_name", robotName);
    params.put("team_name", teamName);
    params.put("electrical_name", descriptionUtil.getElectricalName(productId));
    params.put("properties_value", descriptionUtil.getPropertiesAndValue(productId));
    params.put("equipment_status", descriptionUtil.getCurrentValue(productId));
    return StringUtils.formatString(controlPrompt, params);
  }
}
