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
public class WxBoundProductToolPrompt {
  private static final String wxProductBoundPrompt =
      """
          Identify the product name and key to be bind or unbind based on the user's request
          ## Output Format
               To answer the question, Use the following JSON format. JSON only, no explanation. Otherwise, you will be punished.
               The output should be formatted as a JSON instance that conforms to the format below. JSON only, no explanation.
               ```json
               {
               "thought": "The thought of what to do and why.(use Chinese)",
               "action": # the action to take
                   {
                   "code": "If this is related to bind product output 200,else output 400",
                   "answer": "Answer vivid,lively,kind and amiable(use Chinese)",
                   "bind" : "if user want to bound output true,else output false",
                   "productName": "The user needs to bind the product name",
                   "productKey": "The product key"
                   }
               }
               ```
               ## few shot
               if user input: help me bound the air product,the key is 12345
               ```json
               {
               "thought": "用户想要绑定空调产品",
               "action":
                   {
                   "code": "200",
                   "answer": "Ok, I've understood, your product name is air and the key is 12345",
                   "bind" : "true",
                   "productName": "air",
                   "productKey": "12345"
                   }
               }
               ```
               ## Attention
               - Your output is JSON only and no explanation.
          """;

  public String getWxProductBoundTool() {
    return wxProductBoundPrompt;
  }
}
