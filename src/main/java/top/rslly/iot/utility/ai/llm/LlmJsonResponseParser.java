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
package top.rslly.iot.utility.ai.llm;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses JSON responses and normalizes leaked DeepSeek DSML tool calls. */
public final class LlmJsonResponseParser {
  private static final Pattern MARKDOWN_FENCE =
      Pattern.compile("```(?:json|xml)?\\s*\\R?(.*?)\\s*```",
          Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
  private static final Pattern DSML_CALL = Pattern.compile(
      "<｜DSML｜\\s*calls>\\s*<｜DSML｜\\s*invoke\\s+name=\"([^\"]+)\"\\s*>"
          + "(.*?)</｜DSML｜\\s*invoke>\\s*</｜DSML｜\\s*calls>",
      Pattern.DOTALL);
  private static final Pattern DSML_PARAMETER = Pattern.compile(
      "\\s*<｜DSML｜\\s*parameter\\s+name=\"([^\"]+)\"\\s+string=\"(true|false)\"\\s*>"
          + "(.*?)</｜DSML｜\\s*parameter>",
      Pattern.DOTALL);

  private LlmJsonResponseParser() {}

  public static JSONObject parse(String response) {
    if (response == null || response.isBlank()) {
      throw new IllegalArgumentException("Empty LLM response");
    }
    String content = response.trim();
    Matcher fence = MARKDOWN_FENCE.matcher(content);
    if (fence.matches()) {
      content = fence.group(1).trim();
    }
    if (content.startsWith("<｜DSML｜")) {
      return parseDsmlCall(content);
    }
    JSONObject result = JSON.parseObject(content);
    if (result == null) {
      throw new IllegalArgumentException("Empty LLM JSON object");
    }
    return result;
  }

  private static JSONObject parseDsmlCall(String content) {
    Matcher call = DSML_CALL.matcher(content);
    if (!call.matches() || call.group(1).isBlank()) {
      throw new IllegalArgumentException("Expected one complete DSML tool call");
    }
    String body = call.group(2).trim();
    JSONObject parameters = new JSONObject(true);
    Matcher parameter = DSML_PARAMETER.matcher(body);
    int offset = 0;
    while (offset < body.length()) {
      parameter.region(offset, body.length());
      if (!parameter.lookingAt()) {
        throw new IllegalArgumentException("Invalid DSML parameters or multiple tool calls");
      }
      String name = parameter.group(1);
      if (name.isBlank() || parameters.containsKey(name)) {
        throw new IllegalArgumentException("Invalid or duplicate DSML parameter name");
      }
      String value = parameter.group(3);
      parameters.put(name, Boolean.parseBoolean(parameter.group(2)) ? value : JSON.parse(value));
      offset = parameter.end();
    }
    // Agent function schemas wrap the tool input in a single `args` parameter.
    Object args = parameters.size() == 1 && parameters.containsKey("args")
        ? parameters.get("args")
        : parameters;
    if (!(args instanceof JSONObject) && !(args instanceof String)) {
      throw new IllegalArgumentException("DSML tool arguments must be an object or string");
    }
    JSONObject action = new JSONObject(true);
    action.put("name", call.group(1));
    action.put("args", args);
    JSONObject result = new JSONObject(true);
    result.put("action", action);
    return result;
  }
}
