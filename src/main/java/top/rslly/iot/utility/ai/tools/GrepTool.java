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
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.rslly.iot.utility.ai.sandbox.SandboxService;

/** Bounded, read-only code search in the current chat's sandbox. */
@Data
@Component
@Slf4j
public class GrepTool implements BaseTool<String> {
  private static final String SEARCH_SCRIPT =
      """
          import base64, json, subprocess, sys
          args = json.loads(base64.b64decode(sys.argv[1]))
          command = ['grep', '-r', '-n', '-H', '-I', '-F' if args.get('literal', False) else '-E']
          if args.get('ignore_case', False):
              command.append('-i')
          if args.get('include'):
              command.append('--include=' + args['include'])
          command += ['-C', str(args.get('context_lines', 0)), '-e', args['pattern'], '--', args['path']]
          limit = args.get('max_results', 200)
          try:
              with subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                    text=True, encoding='utf-8', errors='replace') as process:
                  for count, line in enumerate(process.stdout):
                      if count >= limit:
                          process.terminate()
                          process.wait()
                          print('[truncated: narrow pattern/path or increase max_results]')
                          sys.exit(0)
                      value = line.rstrip('\\n')
                      print(value[:2000] + (' [line truncated]' if len(value) > 2000 else ''))
                  code = process.wait()
              if code == 1:
                  print('No matches found.')
              elif code != 0:
                  print('Error: grep search failed (exit code ' + str(code) + ').')
                  sys.exit(code)
          except OSError as error:
              print('Error: cannot search files: ' + str(error))
              sys.exit(1)
          """;

  @Autowired
  private SandboxService sandboxService;
  @Autowired
  private ObjectMapper objectMapper;

  private String name = "grepTool";
  private String description = """
      Search file contents in the chat session's remote Ubuntu sandbox (read-only).
      Args: a JSON string, e.g. {"pattern":"class .*Service","path":"src","include":"*.java"}
      - pattern: required non-empty string; extended grep regular expression by default
      - path: file or directory, defaults to /workspace; relative paths resolve under /workspace
      - include: optional filename glob, e.g. *.java; directories are searched recursively
      - literal: optional boolean, match exact text instead of regex (default false)
      - ignore_case: optional boolean (default false)
      - context_lines: surrounding lines per match, integer 0..100 (default 0)
      - max_results: maximum output lines including context, integer 1..2000 (default 200)
      Returns filenames, 1-based line numbers and matching text. Binary files are skipped.
      Output is bounded and truncation is reported. No matches is a successful empty search.
      Files belong to the temporary sandbox, not the persistent conversation workspace.
      """;

  @Override
  public String run(String question) {
    return run(question, null);
  }

  @Override
  public String run(String question, Map<String, Object> globalMessage) {
    if (!sandboxService.isAvailable()) {
      return "Error: sandbox service is disabled, cannot search files.";
    }
    try {
      JsonNode parsed = question == null ? null : objectMapper.readTree(question);
      if (parsed == null || !parsed.isObject() || !parsed.path("pattern").isTextual()
          || parsed.path("pattern").textValue().isEmpty()) {
        return "Error: expected JSON with a non-empty string pattern and optional path.";
      }
      ObjectNode args = (ObjectNode) parsed;
      for (String key : new String[] {"path", "include"}) {
        if (args.has(key)
            && (!args.path(key).isTextual() || args.path(key).textValue().isBlank())) {
          return "Error: " + key + " must be a non-empty string.";
        }
      }
      for (String key : new String[] {"literal", "ignore_case"}) {
        if (args.has(key) && !args.path(key).isBoolean()) {
          return "Error: " + key + " must be a boolean.";
        }
      }
      if (!validRange(args, "context_lines", 0, 100) || !validRange(args, "max_results", 1, 2000)) {
        return "Error: context_lines must be an integer 0..100; max_results must be an integer 1..2000.";
      }
      args.put("path", SandboxFileSupport.resolvePath(args.path("path").asText("/workspace")));
      var result = sandboxService.runCommand(SandboxService.resolveChatId(globalMessage),
          SandboxFileSupport.pythonCommand(SEARCH_SCRIPT, objectMapper.writeValueAsString(args)));
      return result.ok() ? result.summary() : "Error: " + result.summary();
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      return "Error: grepTool arguments must be a JSON object.";
    } catch (Exception e) {
      log.error("GrepTool execution failed", e);
      return "Error: sandbox search failed: " + e.getMessage();
    }
  }

  private boolean validRange(JsonNode args, String key, int min, int max) {
    JsonNode value = args.get(key);
    return value == null || (value.isIntegralNumber() && value.canConvertToInt()
        && value.intValue() >= min && value.intValue() <= max);
  }
}
