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

import lombok.Getter;

import java.util.UUID;

@Getter
public final class FunctionResult {
  public enum Type {
    UNSUPPORTED, DIRECT_REPLY, TOOL_CALL, ERROR
  }

  private final Type type;
  private final String toolCallId;
  private final String functionName;
  private final String arguments;
  private final String reply;
  private final String errorMessage;
  private final String reasoningContent;

  private FunctionResult(Type type, String toolCallId, String functionName, String arguments,
      String reply, String errorMessage, String reasoningContent) {
    this.type = type;
    this.toolCallId = toolCallId;
    this.functionName = functionName == null ? "" : functionName;
    this.arguments = arguments == null ? "" : arguments;
    this.reply = reply == null ? "" : reply;
    this.errorMessage = errorMessage == null ? "" : errorMessage;
    this.reasoningContent = reasoningContent == null ? "" : reasoningContent;
  }

  public static FunctionResult unsupported() {
    return new FunctionResult(Type.UNSUPPORTED, "", "", "", "", "", "");
  }

  public static FunctionResult directReply(String reply) {
    return new FunctionResult(Type.DIRECT_REPLY, "", "", "", reply, "", "");
  }

  public static FunctionResult toolCall(String functionName, String arguments) {
    return toolCall("", functionName, arguments);
  }

  public static FunctionResult toolCall(String toolCallId, String functionName, String arguments) {
    return toolCall(toolCallId, functionName, arguments, "");
  }

  public static FunctionResult toolCall(String toolCallId, String functionName, String arguments,
      String reasoningContent) {
    return new FunctionResult(Type.TOOL_CALL,
        normalizeToolCallId(toolCallId),
        functionName,
        arguments,
        "",
        "",
        reasoningContent);
  }

  public static FunctionResult error(String errorMessage) {
    return new FunctionResult(Type.ERROR, "", "", "", "", errorMessage, "");
  }

  public boolean isUnsupported() {
    return type == Type.UNSUPPORTED;
  }

  public boolean isDirectReply() {
    return type == Type.DIRECT_REPLY;
  }

  public boolean isToolCall() {
    return type == Type.TOOL_CALL;
  }

  public boolean isError() {
    return type == Type.ERROR;
  }

  private static String normalizeToolCallId(String toolCallId) {
    if (toolCallId != null && !toolCallId.isBlank()) {
      return toolCallId;
    }
    return "call_" + UUID.randomUUID().toString().replace("-", "");
  }
}
