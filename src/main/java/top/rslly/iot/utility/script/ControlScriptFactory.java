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
package top.rslly.iot.utility.script;

public class ControlScriptFactory {
  // public static final String Time_SLEEP_FUNCTION_NAME = "timeSleepFunc";
  public static final String Control_NODE_FUNCTION_NAME = "controlNodeFunc";
  private static final String JS_WRAPPER_PREFIX_TEMPLATE = """
      function %s() {
      return %s();
      function %s() {""";
  private static final String JS_WRAPPER_SUFFIX = "\n}" +
      "\n}";

  public static String generateControlNodeScript(String functionName, String scriptBody,
      String... argNames) {

    String jsWrapperPrefix = String.format(JS_WRAPPER_PREFIX_TEMPLATE, functionName,
        Control_NODE_FUNCTION_NAME, Control_NODE_FUNCTION_NAME);
    return jsWrapperPrefix + scriptBody + JS_WRAPPER_SUFFIX;
  }
}
