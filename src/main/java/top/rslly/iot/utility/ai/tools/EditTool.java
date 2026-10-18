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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.rslly.iot.utility.ai.sandbox.SandboxService;

/**
 * 沙箱文件编辑工具, 最小 harness 模式: 传入的 question 即 JSON 参数, 纯 Jackson 解析, 不做 LLM 参数提取. 所有返回给大模型的内容均为英文,
 * 便于模型理解与自纠错.
 *
 * <p>
 * 参数格式: {@code {"action": "write|read|list|delete", "path": "...", "content": "..."}},
 * 相对路径自动落在沙箱工作目录 /workspace 下.
 *
 * <p>
 * 暂未接入 FunctionRouterRoute 路由, 由后续功能按需调用.
 */
@Data
@Component
@Slf4j
public class EditTool implements BaseTool<String> {

  private static final String USAGE =
      """
          Invalid arguments. Expected JSON:
          {"action": "write|read|list|delete", "path": "target path", "content": "content to write (required for write)"}""";

  @Autowired
  private SandboxService sandboxService;

  @Autowired
  private ObjectMapper objectMapper;

  private String name = "editTool";

  private String description =
      """
          Read and write files in the chat session's remote Ubuntu sandbox.
          Args: a JSON string, e.g.
          {"action": "write|read|list|delete", "path": "target path", "content": "content to write (required for write)"}
          - write: create/overwrite a file; read: read file content; list: list a directory;
            delete: remove a file or directory
          - Relative paths are resolved under the /workspace working directory
          - Filesystem state persists between calls within the same session
          """;

  @Override
  public String run(String question) {
    return run(question, null);
  }

  @Override
  public String run(String question, Map<String, Object> globalMessage) {
    String chatId = extractChatId(globalMessage);
    if (!sandboxService.isAvailable()) {
      return "Error: sandbox service is disabled, cannot operate on files.";
    }
    try {
      JsonNode args = parseArgs(question);
      if (args == null) {
        return USAGE;
      }
      String action = args.path("action").asText("");
      String path = resolvePath(args.path("path").asText(null));
      if (path == null) {
        return "Error: missing required parameter \"path\".\n" + USAGE;
      }
      return switch (action) {
        case "write" -> doWrite(chatId, path, args.path("content").asText(null));
        case "read" -> doRead(chatId, path);
        case "list" -> sandboxService.runCommand(chatId, "ls -la '" + path + "'").summary();
        case "delete" -> sandboxService.runCommand(chatId, "rm -rf '" + path + "'").summary();
        default -> "Error: unknown action \"" + action + "\". Allowed: write|read|list|delete.\n"
            + USAGE;
      };
    } catch (Exception e) {
      log.error("EditTool 执行异常, chatId: {}", chatId, e);
      return "Error: sandbox file operation failed unexpectedly: " + e.getMessage()
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

  private JsonNode parseArgs(String question) {
    if (question == null || question.isBlank()) {
      return null;
    }
    try {
      JsonNode node = objectMapper.readTree(question);
      return node.isObject() ? node : null;
    } catch (Exception e) {
      log.info("EditTool 参数解析失败: {}", e.getMessage());
      return null;
    }
  }

  /** 相对路径落到 /workspace 下, 绝对路径原样使用 */
  private String resolvePath(String path) {
    if (path == null || path.isBlank()) {
      return null;
    }
    String trimmed = path.trim();
    return trimmed.startsWith("/") ? trimmed : SandboxService.WORKSPACE_DIR + "/" + trimmed;
  }

  private String doWrite(String chatId, String path, String content) {
    if (content == null) {
      return "Error: the \"write\" action requires the \"content\" parameter.";
    }
    boolean success = sandboxService.writeFile(chatId, path, content);
    return success ? "Write succeeded: " + path : "Error: failed to write " + path;
  }

  private String doRead(String chatId, String path) {
    String content = sandboxService.readFile(chatId, path);
    return content == null ? "Error: file not found or read failed: " + path : content;
  }
}
