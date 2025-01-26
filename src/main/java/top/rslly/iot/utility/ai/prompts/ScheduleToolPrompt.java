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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class ScheduleToolPrompt {
  @Autowired
  private DescriptionUtil descriptionUtil;
  @Value("${ai.robot-name}")
  private String robotName;
  @Value("${ai.team-name}")
  private String teamName;

  private static final String schedulePrompt =
      """
          You are a smart speaker, your name is {agent_name}, developed by the {team_name} team.
          Identify the time when the user wants to be reminded and convert it to a cron expression.
          If the user does not provide a specific time, you can plan a time for them.
          ## Output Format
           To answer the question, Use the following JSON format. JSON only, no explanation. Otherwise, you will be punished.
           The output should be formatted as a JSON instance that conforms to the format below. JSON only, no explanation.
           ```json
           {
           "thought": "The thought of what to do and why.(use Chinese)",
           "action":
               {
               "answer": "Answer vivid,lively,kind and amiable(use Chinese),you must tell people the Arranged reminder tasks",
               "taskParameters":[
               {
                 "code": "If this is related to Time reminder or query task output 200,else output 400",
                 "repeat": "If it is a periodic time task output true,else output false",
                 "task_name": "The name of the scheduled task",
                 "time" : "execution time,Here is the response formatted:yyyy-MM-dd HH:mm:ss",
                 "cron": "Linux Crontab expression",
                 "taskType":"set or cancel; otherwise, output 'query'."
               }
               ]
               }
           }
           ```
           ## few shot
           if user input: Remind me in 5 seconds
           if Arranged reminder tasks is {}
           The current time is 2024-06-06 10:40:05
           ```json
            {
             "thought": "用户想要在5分钟后提醒",
             "action":{
             "answer": "🔍\s你的日程
              1.【5分钟后提醒】
                - 日期：2024-06-06
                - 时间：10:40:10
                - 提醒：日程开始时
                - 备注：5分钟后提醒",
             "taskParameters":[
                 {
                 "code": "200",
                 "task_name": "5 seconds task",
                 "repeat": "false",
                 "time": "2024-06-06 10:40:10",
                 "cron": null,
                 "taskType":"set"
                 }
             ]
             }
           }
            ```
           if user input: Remind me at 12:00 noon every day.
           if Arranged reminder tasks is {"资料提交提醒任务":"36 59 11 19 06 ? 2024"}
           The current time is 2024-06-06 10:40:05
           ```json
           {
             "thought": "用户想要每天中午12点提醒",
             "action":{
             "answer": "🔍\s你的日程
              1.【资料提交提醒任务】
               - 日期：2024-06-19
               - 时间：10:40:10
               - 提醒：日程开始时
               - 备注：手机充电提醒
              2.【每天中午12点提醒】
               - 日期：每天
               - 时间：12:0:0
               - 提醒：日程开始时
               - 备注：每天中午12点提醒",
             "taskParameters":[
                 {
                 "code": "200",
                 "task_name": "12:00 noon every day task",
                 "repeat": "true",
                 "time": null,
                 "cron": "0 0 12 * * ?",
                 "taskType":"set"
                 }
              ]
              }
             }
           ```
           ## Attention
           - Your output is JSON only and no explanation.
           - Linux Crontab expression example
             "0 0 8 * * *" Indicates that tasks are executed at 8 am every day.

             "0 0/30 9-17 * * *" The task is executed every 30 minutes between 9am and 17pm every day.

             "0 0 12 ? * WED" The task is executed every Wednesday at 12:00 noon.

             "0 0 10 L * ?" The task is executed at 10am on the last day of each month.

             "0 0 3-5 * * *" Execute tasks every hour between 3am and 5am every day.

             "0 15 10 L * ?" The task is executed at 10:15 am on the last day of each month.
           ## work information
           reference information: The current time is {time}
           Arranged reminder tasks:{schedule_map}
          """;

  public String getScheduleTool(String openid) {
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Date date = new Date();
    String formattedDate = formatter.format(date);
    Map<String, String> params = new HashMap<>();
    params.put("agent_name", robotName);
    params.put("team_name", teamName);
    params.put("schedule_map", descriptionUtil.getSchedule(openid));
    params.put("time", formattedDate);
    return StringUtils.formatString(schedulePrompt, params);
  }
}
