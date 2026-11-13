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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record FunctionToolSpec(
    String name,
    String description,
    Map<String, Object> parameters,
    boolean strict) {

  public FunctionToolSpec(String name, String description, Map<String, Object> parameters) {
    this(name, description, parameters, false);
  }

  /**
   * JSON of the function definitions as sent to the model, for token estimates. fastjson 1.x
   * serializes records as {@code {}}, so {@code JSON.toJSONString(specs)} must not be used.
   */
  public static String toEstimateJson(List<FunctionToolSpec> specs) {
    if (specs == null || specs.isEmpty()) {
      return "";
    }
    List<Map<String, Object>> functions = new ArrayList<>();
    for (FunctionToolSpec spec : specs) {
      if (spec == null) {
        continue;
      }
      Map<String, Object> function = new LinkedHashMap<>();
      function.put("name", spec.name());
      function.put("description", spec.description());
      function.put("parameters", spec.parameters());
      Map<String, Object> tool = new LinkedHashMap<>();
      tool.put("type", "function");
      tool.put("function", function);
      functions.add(tool);
    }
    return JSON.toJSONString(functions);
  }

  public static Map<String, Object> defaultRouterArgsSchema() {
    Map<String, Object> argsProperty = new LinkedHashMap<>();
    argsProperty.put("type", "string");
    argsProperty.put("description",
        "Normalized arguments for the selected route. Use empty string when no args are needed.");

    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put("args", argsProperty);

    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("type", "object");
    schema.put("properties", properties);
    schema.put("required", List.of("args"));
    schema.put("additionalProperties", false);
    return schema;
  }

  public static Map<String, Object> defaultStringArgsSchema() {
    Map<String, Object> argsProperty = new LinkedHashMap<>();
    argsProperty.put("type", "string");
    argsProperty.put("description",
        "Input argument for the function. Use empty string when no args are needed.");

    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put("args", argsProperty);

    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("type", "object");
    schema.put("properties", properties);
    schema.put("required", List.of("args"));
    schema.put("additionalProperties", false);
    return schema;
  }
}
