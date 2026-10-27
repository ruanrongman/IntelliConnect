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
 * 参数格式: {@code {"action": "write|read|list|delete|str_replace|insert", "path": "..."}},
 * 相对路径自动落在沙箱工作目录 /workspace 下.
 *
 */
@Data
@Component
@Slf4j
public class EditTool implements BaseTool<String> {

  private static final String USAGE =
      """
          Invalid arguments. Expected JSON:
          {"action": "write|read|list|delete|str_replace|insert", "path": "target path", ...}
          str_replace requires old_str and new_str; insert requires insert_line and content.""";

  private static final String EDIT_SCRIPT =
      """
          import base64, fcntl, json, sys
          args = json.loads(base64.b64decode(sys.argv[1]))
          try:
              with open(args['path'], 'r+', encoding='utf-8', newline='') as file:
                  fcntl.flock(file, fcntl.LOCK_EX)
                  text = file.read()
                  if args['action'] == 'str_replace':
                      old = args['old_str']
                      count = text.count(old)
                      if count != 1:
                          print(f'Error: old_str must match exactly once; found {count} matches. Include more context.')
                          sys.exit(0)
                      updated = text.replace(old, args['new_str'], 1)
                  else:
                      lines = text.splitlines(keepends=True)
                      line = args['insert_line']
                      if line > len(lines):
                          print(f'Error: insert_line must be between 0 and {len(lines)}.')
                          sys.exit(0)
                      newline = '\\r\\n' if '\\r\\n' in text else '\\n'
                      before, after = ''.join(lines[:line]), ''.join(lines[line:])
                      content = args['content']
                      if before and not before.endswith(('\\n', '\\r')):
                          before += newline
                      if after and not content.endswith(('\\n', '\\r')):
                          content += newline
                      updated = before + content + after
                  file.seek(0)
                  file.write(updated)
                  file.truncate()
              print('Edit succeeded: ' + args['path'])
          except (OSError, UnicodeError) as error:
              print('Error: cannot edit file: ' + str(error))
              sys.exit(1)
          """;

  @Autowired
  private SandboxService sandboxService;

  @Autowired
  private ObjectMapper objectMapper;

  private String name = "editTool";

  private String description =
      """
          Read, write and precisely edit files in the chat session's remote Ubuntu sandbox.
          Args: a JSON string, e.g.
          {"action": "write|read|list|delete|str_replace|insert", "path": "target path", ...}
          - write: create/overwrite a file; read: read file content; list: list a directory;
            delete: remove a file or directory. write requires content (string).
          - str_replace: requires non-empty old_str and new_str (may be empty to delete text).
            Matches literal text including whitespace, exactly once; zero/multiple matches fail without changes.
            Example: {"action":"str_replace","path":"app.py","old_str":"return 1","new_str":"return 2"}
          - insert: requires insert_line (integer) and non-empty content. Insert AFTER that 1-based line;
            0 inserts at the beginning; the number of lines appends. Out-of-range positions fail.
            Example: {"action":"insert","path":"app.py","insert_line":0,"content":"import os\\n"}
          - Read the current file before editing; use grepTool to find the relevant code first.
          - Relative paths are resolved under /workspace, the sandbox's temporary working directory
          - This is not the persistent conversation workspace. Files persist between calls only while
            this sandbox instance exists and are lost when it shuts down, is recycled, or expires
          - For permanent storage, use workspaceTool (export_from_sandbox) before the sandbox is lost,
            or workspaceTool (write) to save generated text directly to the persistent workspace
          """;

  @Override
  public String run(String question) {
    return run(question, null);
  }

  @Override
  public String run(String question, Map<String, Object> globalMessage) {
    String chatId = SandboxService.resolveChatId(globalMessage);
    if (!sandboxService.isAvailable()) {
      return "Error: sandbox service is disabled, cannot operate on files.";
    }
    try {
      JsonNode args = parseArgs(question);
      if (args == null) {
        return USAGE;
      }
      String action = args.path("action").asText("");
      String path = SandboxFileSupport.resolvePath(args.path("path").isTextual()
          ? args.path("path").textValue()
          : null);
      if (path == null) {
        return "Error: missing required parameter \"path\".\n" + USAGE;
      }
      return switch (action) {
        case "write" -> doWrite(chatId, path, args.path("content").isTextual()
            ? args.path("content").textValue()
            : null);
        case "read" -> doRead(chatId, path);
        case "list" -> sandboxService.runCommand(chatId,
            "ls -la -- " + SandboxFileSupport.quote(path)).summary();
        case "delete" -> sandboxService.runCommand(chatId,
            "rm -rf -- " + SandboxFileSupport.quote(path)).summary();
        case "str_replace", "insert" -> doEdit(chatId, path, args);
        default -> "Error: unknown action \"" + action + "\".\n"
            + USAGE;
      };
    } catch (Exception e) {
      log.error("EditTool 执行异常, chatId: {}", chatId, e);
      return "Error: sandbox file operation failed unexpectedly: " + e.getMessage()
          + ". The sandbox instance may be broken, please retry or report this issue.";
    }
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

  private String doEdit(String chatId, String path, JsonNode args) throws Exception {
    if ("str_replace".equals(args.path("action").asText())) {
      if (!args.path("old_str").isTextual() || args.path("old_str").textValue().isEmpty()
          || !args.path("new_str").isTextual()) {
        return "Error: str_replace requires non-empty old_str and string new_str.";
      }
    } else if (!args.path("insert_line").isIntegralNumber()
        || !args.path("insert_line").canConvertToInt() || args.path("insert_line").intValue() < 0
        || !args.path("content").isTextual() || args.path("content").textValue().isEmpty()) {
      return "Error: insert requires insert_line (non-negative integer) and non-empty content.";
    }
    var parameters = args.deepCopy();
    ((com.fasterxml.jackson.databind.node.ObjectNode) parameters).put("path", path);
    var result = sandboxService.runCommand(chatId,
        SandboxFileSupport.pythonCommand(EDIT_SCRIPT, objectMapper.writeValueAsString(parameters)));
    return result.ok() ? result.summary() : "Error: " + result.summary();
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
