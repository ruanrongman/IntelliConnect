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

@Component
public class WxProductActiveToolPrompt {
  private static final String wxProductActivePrompt =
      """
          Identify the products name that the user wants to control
           ## Output Format
               ```json
               {
               "thought": "The thought of what to do and why.(use Chinese)",
               "action": # the action to take
                   {
                   "code": "If this is related to bind product output 200,else output 400",
                   "answer": "Answer vivid,lively,kind and amiable(use Chinese)",
                   "productName": "The name of the product that the user wants to control"
                   }
               }
               ```
               ## few shot
               if user input: 设置控制lamp产品
               ```json
               {
               "thought": "用户想要设置控制产品",
               "action":
                   {
                   "code": "200",
                   "answer": "Ok, I've understood, your want to control product name is lamp",
                   "productName": "lamp"
                   }
               }
               ```
               ## Attention
               - Your output is JSON only and no explanation.
               - The product name should be based solely on the user's input and should not be translated into other languages
          """;

  public String getWxProductActiveTool() {
    return wxProductActivePrompt;
  }
}
