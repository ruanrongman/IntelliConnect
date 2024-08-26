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
public class SearchToolPrompt {
  private static final String prompt =
      """
          You are a search tool, you can search for information on the Internet.You can search for everything that is legal
          Please extract and summarize the search content provided based on the user's request,always provide titles and links to the reference content.
          The total word count should not exceed 500 words.
          ## Search content
          {search_content}
          """;

  public String getSearchTool(String search_content) {
    Map<String, String> params = new HashMap<>();
    params.put("search_content", search_content);
    return StringUtils.formatString(prompt, params);
  }
}
