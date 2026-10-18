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
package top.rslly.iot.utility.ai.tools;

import java.util.Map;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.rslly.iot.utility.ai.sandbox.SandboxResult;
import top.rslly.iot.utility.ai.sandbox.SandboxService;

/**
 * 沙箱 bash 执行工具, 最小 harness 模式: 传入的 question 即完整 shell 命令, 原样执行不做任何参数提取, 返回原始的 stdout / stderr /
 * exit code. 所有返回给大模型的内容均为英文, 便于模型理解与自纠错.
 *
 * <p>
 * 暂未接入 FunctionRouterRoute 路由, 由后续功能按需调用.
 */
@Data
@Component
@Slf4j
public class BashTool implements BaseTool<String> {

  @Autowired
  private SandboxService sandboxService;

  private String name = "bashTool";
  // 预装运行时清单来自 2026-10-09 对 code-interpreter:v1.1.0 镜像的实际探测
  private String description =
      """
          Run a shell command in a remote Ubuntu 24.04 sandbox and return its stdout/stderr/exit code.
          Environment notes:
          - Each chat session has its own isolated sandbox instance; other sessions cannot see its files
          - Full dev runtimes are preinstalled, do NOT reinstall basics: Python 3.12 (no pip),
            Node.js 22 (npm 10), Go 1.25, OpenJDK 21 (javac available), gcc/g++ 13.3,
            plus common tools like git/curl/vim/unzip/tar
          - Filesystem state persists between calls within the same session; working directory is /workspace
          - Commands run in an isolated container with a per-command timeout
          Args: the full shell command to execute (str)
          """;

  @Override
  public String run(String question) {
    return run(question, null);
  }

  @Override
  public String run(String question, Map<String, Object> globalMessage) {
    String chatId = extractChatId(globalMessage);
    if (!sandboxService.isAvailable()) {
      return "Error: sandbox service is disabled, cannot execute commands.";
    }
    if (question == null || question.isBlank()) {
      return "Error: empty command. Provide the full shell command to execute.";
    }
    try {
      SandboxResult result = sandboxService.runCommand(chatId, question.trim());
      log.info("沙箱命令执行完成, chatId: {}, exitCode: {}, 耗时: {}ms", chatId, result.exitCode(),
          result.durationMillis());
      return "exit code: " + result.exitCode() + "\n" + result.summary();
    } catch (Exception e) {
      log.error("BashTool 执行异常, chatId: {}", chatId, e);
      return "Error: sandbox execution failed unexpectedly: " + e.getMessage()
          + ". The sandbox instance may be broken, please retry or report this issue.";
    }
  }

  /** 从会话上下文取 chatId, 缺省时落到共享默认实例 */
  private String extractChatId(Map<String, Object> globalMessage) {
    if (globalMessage == null) {
      return SandboxService.DEFAULT_CHAT_ID;
    }
    Object chatId = globalMessage.get("chatId");
    return chatId == null ? SandboxService.DEFAULT_CHAT_ID : chatId.toString();
  }
}
