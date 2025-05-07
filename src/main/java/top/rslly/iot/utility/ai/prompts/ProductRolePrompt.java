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
import org.springframework.stereotype.Component;
import top.rslly.iot.utility.ai.DescriptionUtil;
import top.rslly.iot.utility.ai.promptTemplate.StringUtils;
import top.rslly.iot.utility.ai.voice.VoiceTimbre;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ProductRolePrompt {
  @Autowired
  private DescriptionUtil descriptionUtil;
  private static final String productRolePrompt =
      """
          Translate the user's requirements into detailed role and VoiceTimbre configurations.
          The following are the current user role settings:{role_set}
          The timbre list is as follows:{timbre_map}
          ## Output Format
          ```json
          {
          "thought": "The thought of what to do and why.(use Chinese)",
          "action":
              {
              "code": "If this is related to Configure roles output 200,else output 400",
              "answer": "Answer vivid,lively,kind and amiable(use Chinese),If the information queried by the user does not exist, please inform the user directly that it does not exist",
              "taskType":"set or cancel; otherwise, output 'query'."
              "assistant_name": "The name of the assistant is your name,if you not found,output null",
              "user_name":  "user name,if you not found,output null",
              "role":  "role name,if you not found,output null",
              "role_introduction":  "role introduction,if the user does not specify, it can be selected based on the role,can not be null",
              "voice_timbre":"Set the voice_timbre according to the user's request. If the user does not specify, it can be selected based on the role, but in any case, it must be one of the voice_timbre options from the above list"
              }
          }
          ```
          ## Attention
          - Your output is JSON only and no explanation.
          - If the above parameters contain null, please refuse and provide suggestions in the answer.
          """;

  public String getProductRoleTool(int productId) {
    Map<String, String> params = new HashMap<>();
    params.put("role_set", descriptionUtil.getProductRole(productId));
    params.put("timbre_map", JSON.toJSONString(
        VoiceTimbre.stream()
            .map(vt -> new HashMap<String, String>() {
              {
                put("timbre", vt.name());
                put("timbreDescription", vt.getTimbreDescription());
              }
            })
            .collect(Collectors.toList())));
    return StringUtils.formatString(productRolePrompt, params);
  }
}
