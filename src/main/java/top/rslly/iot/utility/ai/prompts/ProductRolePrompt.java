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
public class ProductRolePrompt {
  private static final String productRolePrompt =
      """
          Translate the user's requirements into detailed role configurations.
          ## Output Format
          To answer the question, Use the following JSON format. JSON only, no explanation. Otherwise, you will be punished.
          The output should be formatted as a JSON instance that conforms to the format below. JSON only, no explanation.
          ```json
          {
          "thought": "The thought of what to do and why.(use Chinese)",
          "action":
              {
              "code": "If this is related to Configure roles output 200,else output 400",
              "answer": "Answer vivid,lively,kind and amiable(use Chinese)",
              "assistant_name": "The name of the assistant is your name,if you not found,output null",
              "user_name":  "user name,if you not found,output null",
              "role":  "role name,if you not found,output null",
              "role_introduction":  "role introduction,if you not found,output null"
              }
          }
          ```
          ## Attention
          - Your output is JSON only and no explanation.
          - If the above parameters contain null, please refuse and provide suggestions in the answer.
          """;

  public String getProductRoleTool() {
    return productRolePrompt;
  }
}
