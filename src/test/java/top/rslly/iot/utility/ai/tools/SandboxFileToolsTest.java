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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import top.rslly.iot.services.McpEndpointConfigService;
import top.rslly.iot.services.agent.ProductSkillsServiceImpl;
import top.rslly.iot.utility.ai.llm.FunctionToolSpec;
import top.rslly.iot.utility.ai.mcp.McpAgent;
import top.rslly.iot.utility.ai.mcp.McpWebsocket;
import top.rslly.iot.utility.ai.sandbox.SandboxResult;
import top.rslly.iot.utility.ai.sandbox.SandboxService;

class SandboxFileToolsTest {
  @TempDir
  Path directory;
  private final ObjectMapper mapper = new ObjectMapper();
  private final Map<String, Object> context = Map.of("chatId", "request-1",
      "conversationChatId", "conversation-1");
  private SandboxService sandbox;
  private EditTool edit;
  private GrepTool grep;

  @BeforeEach
  void setUp() {
    sandbox = mock(SandboxService.class);
    when(sandbox.isAvailable()).thenReturn(true);
    edit = new EditTool();
    grep = new GrepTool();
    for (Object tool : new Object[] {edit, grep}) {
      ReflectionTestUtils.setField(tool, "sandboxService", sandbox);
      ReflectionTestUtils.setField(tool, "objectMapper", mapper);
    }
  }

  @Test
  void replacesOnlyUniqueLiteralMatchAndPreservesUnicodeAndCrLf() throws Exception {
    executeScriptsLocally();
    Path file = directory.resolve("source ' quoted.txt");
    Files.writeString(file, "你好\r\nreturn '$HOME';\r\n");
    String result = edit.run(mapper.writeValueAsString(Map.of("action", "str_replace",
        "path", file.toString().replace('\\', '/'), "old_str", "return '$HOME';",
        "new_str", "return '$(echo text)';")), context);
    assertTrue(result.contains("Edit succeeded"), result);
    assertEquals("你好\r\nreturn '$(echo text)';\r\n", Files.readString(file));
    verify(sandbox).runCommand(eq("conversation-1"), anyString());
  }

  @ParameterizedTest
  @ValueSource(strings = {"missing", "same"})
  void missingOrAmbiguousReplacementLeavesFileUntouched(String old) throws Exception {
    executeScriptsLocally();
    Path file = directory.resolve("source.txt");
    Files.writeString(file, "same\nsame\n");
    String result = edit.run(mapper.writeValueAsString(Map.of("action", "str_replace",
        "path", file.toString().replace('\\', '/'), "old_str", old, "new_str", "changed")),
        context);
    assertTrue(result.contains("must match exactly once"), result);
    assertEquals("same\nsame\n", Files.readString(file));
  }

  @Test
  void insertsAtStartMiddleAndUnterminatedEndAndRejectsInvalidLine() throws Exception {
    executeScriptsLocally();
    Path file = directory.resolve("source.txt");
    Files.writeString(file, "a\r\nb");
    assertTrue(insert(file, 0, "first").contains("Edit succeeded"));
    assertTrue(insert(file, 2, "middle").contains("Edit succeeded"));
    assertTrue(insert(file, 4, "last").contains("Edit succeeded"));
    assertEquals("first\r\na\r\nmiddle\r\nb\r\nlast", Files.readString(file));
    assertTrue(insert(file, 99, "invalid").contains("must be between"));
    assertEquals("first\r\na\r\nmiddle\r\nb\r\nlast", Files.readString(file));
    Files.writeString(file, "");
    assertTrue(insert(file, 0, "empty file").contains("Edit succeeded"));
    assertEquals("empty file", Files.readString(file));
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "{\"action\":\"str_replace\",\"path\":\"a\",\"old_str\":\"\",\"new_str\":\"x\"}",
      "{\"action\":\"insert\",\"path\":\"a\",\"insert_line\":-1,\"content\":\"x\"}",
      "{\"action\":\"insert\",\"path\":\"a\",\"insert_line\":1.5,\"content\":\"x\"}"
  })
  void rejectsInvalidEditsBeforeExecutingCommands(String input) {
    assertTrue(edit.run(input, context).startsWith("Error:"));
    verify(sandbox, never()).runCommand(anyString(), anyString());
  }

  @Test
  void searchesRecursivelyWithRegexGlobContextAndLiteralOptions() throws Exception {
    requireGrep();
    executeScriptsLocally();
    Path nested = Files.createDirectory(directory.resolve("nested"));
    Files.writeString(nested.resolve("Example.java"),
        "before\nclass MyService {}\na.b\naXb\nafter\n");
    Files.writeString(nested.resolve("ignore.txt"), "class IgnoredService {}\n");
    String result = search(Map.of("pattern", "class .*Service", "include", "*.java",
        "context_lines", 1));
    assertTrue(result.contains("Example.java:2:class MyService"), result);
    assertTrue(result.contains("before"), result);
    assertFalse(result.contains("IgnoredService"), result);
    result = search(Map.of("pattern", "A.B", "literal", true, "ignore_case", true));
    assertTrue(result.contains(":3:a.b"), result);
    assertFalse(result.contains("aXb"), result);
    assertTrue(search(Map.of("pattern", "absent")).contains("No matches found"));
  }

  @Test
  void reportsTruncationAndInvalidRegexAndMissingPaths() throws Exception {
    requireGrep();
    executeScriptsLocally();
    Files.writeString(directory.resolve("many.txt"), "match\n".repeat(30));
    String result = search(Map.of("pattern", "match", "max_results", 2));
    assertTrue(result.contains("truncated"), result);
    assertFalse(result.contains(":3:match"), result);
    assertTrue(search(Map.of("pattern", "[")).startsWith("Error:"));
    assertTrue(grep.run(mapper.writeValueAsString(Map.of("pattern", "x", "path",
        directory.resolve("missing").toString().replace('\\', '/'))), context)
        .startsWith("Error:"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"{}", "[]", "bad json", "{\"pattern\":\"x\",\"max_results\":0}",
      "{\"pattern\":\"x\",\"ignore_case\":\"true\"}",
      "{\"pattern\":\"x\",\"context_lines\":101}"})
  void rejectsInvalidSearchArguments(String input) {
    assertTrue(grep.run(input, context).startsWith("Error:"));
    verify(sandbox, never()).runCommand(anyString(), anyString());
  }

  @Test
  void disabledSandboxDoesNotExecuteFileTools() {
    when(sandbox.isAvailable()).thenReturn(false);
    assertTrue(edit.run("{}", context).contains("disabled"));
    assertTrue(grep.run("{}", context).contains("disabled"));
    verify(sandbox, never()).runCommand(anyString(), anyString());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void mcpRegistersGrepInBothProtocolsAndDispatchesWithConversationContext(boolean available) {
    when(sandbox.isAvailable()).thenReturn(available);
    McpAgent agent = new McpAgent();
    ReflectionTestUtils.setField(agent, "sandboxService", sandbox);
    ReflectionTestUtils.setField(agent, "bashTool", new BashTool());
    ReflectionTestUtils.setField(agent, "editTool", edit);
    ReflectionTestUtils.setField(agent, "grepTool", grep);
    ReflectionTestUtils.setField(agent, "imageTool", new ImageTool());
    ReflectionTestUtils.setField(agent, "workspaceTool", new WorkspaceTool(
        mock(top.rslly.iot.services.agent.WorkspaceService.class), mapper));
    ReflectionTestUtils.setField(agent, "jsExecuteTool", new JsExecuteTool());
    ReflectionTestUtils.setField(agent, "mcpWebsocket", mock(McpWebsocket.class));
    ReflectionTestUtils.setField(agent, "mcpEndpointConfigService",
        mock(McpEndpointConfigService.class));
    ReflectionTestUtils.setField(agent, "productSkillsService",
        mock(ProductSkillsServiceImpl.class));
    String react = ReflectionTestUtils.invokeMethod(agent, "sandboxToolDescription");
    assertEquals(available, react.contains("skills:grepTool"));
    Object registry = ReflectionTestUtils.invokeMethod(agent, "buildMcpFunctionToolRegistry",
        12, "request-1", Map.of());
    List<FunctionToolSpec> specs = ReflectionTestUtils.invokeMethod(registry, "toolSpecs");
    var spec =
        specs.stream().filter(item -> item.description().equals(grep.getDescription())).findFirst();
    assertEquals(available, spec.isPresent());
    if (available) {
      assertEquals(FunctionToolSpec.defaultStringArgsSchema(), spec.orElseThrow().parameters());
      Map<String, ?> refs = ReflectionTestUtils.invokeMethod(registry, "refMap");
      when(sandbox.runCommand(eq("conversation-1"), anyString()))
          .thenReturn(new SandboxResult(0, "found", "", null, 0));
      var callContext = new java.util.HashMap<>(context);
      callContext.put("productId", 12);
      String result = ReflectionTestUtils.invokeMethod(agent, "callMcpFunction",
          refs.get(spec.orElseThrow().name()), "{\"pattern\":\"test\"}", callContext, Map.of());
      assertEquals("found", result);
      verify(sandbox).runCommand(eq("conversation-1"), anyString());
    }
  }

  private String insert(Path file, int line, String content) throws Exception {
    return edit.run(mapper.writeValueAsString(Map.of("action", "insert", "path",
        file.toString().replace('\\', '/'), "insert_line", line, "content", content)), context);
  }

  private String search(Map<String, Object> input) throws Exception {
    var args = new java.util.HashMap<>(input);
    args.put("path", directory.toString().replace('\\', '/'));
    return grep.run(mapper.writeValueAsString(args), context);
  }

  /** Run the production scripts against real files without requiring a remote sandbox. */
  private void executeScriptsLocally() throws Exception {
    String python = System.getProperty("sandbox.test.python", "python3");
    try {
      Process probe = new ProcessBuilder(python, "--version").start();
      org.junit.jupiter.api.Assumptions.assumeTrue(probe.waitFor(10, TimeUnit.SECONDS)
          && probe.exitValue() == 0, "Python 3 is required for sandbox script integration tests");
    } catch (java.io.IOException e) {
      org.junit.jupiter.api.Assumptions.abort("Python 3 unavailable: " + e.getMessage());
    }
    when(sandbox.runCommand(anyString(), anyString())).thenAnswer(invocation -> {
      String command = invocation.getArgument(1);
      int end = command.lastIndexOf("' '");
      String script = command.substring("python3 -c '".length(), end).replace("'\"'\"'", "'");
      String payload = command.substring(end + 3, command.length() - 1);
      // Translate the simulated Linux workspace path to the Windows fixture directory.
      if (System.getProperty("os.name").startsWith("Windows")) {
        var args = mapper.readTree(Base64.getDecoder().decode(payload));
        String path = args.path("path").asText();
        if (path.startsWith("/workspace/")) {
          ((com.fasterxml.jackson.databind.node.ObjectNode) args).put("path",
              path.substring("/workspace/".length()));
        }
        payload = Base64.getEncoder().encodeToString(mapper.writeValueAsBytes(args));
      }
      // The production sandbox is Linux. Windows tests emulate only its advisory-lock API.
      String bootstrap = "import sys, types\nif sys.platform == 'win32':\n"
          + "    sys.modules['fcntl'] = types.SimpleNamespace(LOCK_EX=2, flock=lambda *a: None)\n";
      Process process = new ProcessBuilder(python, "-c", bootstrap + script, payload)
          .redirectErrorStream(true).start();
      String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      assertTrue(process.waitFor(10, TimeUnit.SECONDS));
      return new SandboxResult(process.exitValue(), output, "", null, 0);
    });
  }

  private void requireGrep() throws Exception {
    try {
      Process probe = new ProcessBuilder("grep", "--version").start();
      org.junit.jupiter.api.Assumptions.assumeTrue(probe.waitFor(10, TimeUnit.SECONDS)
          && probe.exitValue() == 0, "GNU grep is required for sandbox search integration tests");
    } catch (java.io.IOException e) {
      org.junit.jupiter.api.Assumptions.abort("GNU grep unavailable: " + e.getMessage());
    }
  }
}
