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
package top.rslly.iot.utility.ai.sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alibaba.opensandbox.sandbox.Sandbox;
import com.alibaba.opensandbox.sandbox.domain.models.execd.executions.Execution;
import com.alibaba.opensandbox.sandbox.domain.models.execd.executions.RunCommandRequest;
import com.alibaba.opensandbox.sandbox.domain.pool.AcquirePolicy;
import com.alibaba.opensandbox.sandbox.domain.services.Commands;
import com.alibaba.opensandbox.sandbox.domain.services.Filesystem;
import com.alibaba.opensandbox.sandbox.pool.SandboxPool;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import top.rslly.iot.models.ProductSkillsEntity;
import top.rslly.iot.services.agent.ProductSkillsServiceImpl;
import top.rslly.iot.utility.ai.GlobalMessageContext;
import top.rslly.iot.utility.ai.mcp.McpAgent;
import top.rslly.iot.utility.ai.tools.BashTool;
import top.rslly.iot.utility.ai.tools.EditTool;

class SandboxServiceTest {
  private SandboxProperty property;
  private SandboxService service;
  private SandboxPool pool;
  private Sandbox sandbox;
  private BashTool bashTool;
  private EditTool editTool;

  @BeforeEach
  void setUp() {
    property = new SandboxProperty();
    property.setEnabled(true);
    property.setPoolEnabled(false);
    service = new SandboxService(property);
    pool = mock(SandboxPool.class);
    ReflectionTestUtils.setField(service, "pool", pool);
    sandbox = sandboxWithFiles("sandbox-1");
    when(pool.acquire(any(Duration.class), eq(AcquirePolicy.RETRY_NEXT_IDLE_THEN_CREATE)))
        .thenReturn(sandbox);
    bashTool = new BashTool();
    ReflectionTestUtils.setField(bashTool, "sandboxService", service);
    editTool = new EditTool();
    ReflectionTestUtils.setField(editTool, "sandboxService", service);
    ReflectionTestUtils.setField(editTool, "objectMapper", new ObjectMapper());
  }

  @Test
  void keepsFilesAcrossStreamRequestsAndToolsInTheSameConversation() {
    assertTrue(editTool.run(
        "{\"action\":\"write\",\"path\":\"snake.html\",\"content\":\"snake game\"}",
        context("request-1", "conversation-1")).startsWith("Write succeeded"));

    bashTool.run("ls -la", context("request-2", "conversation-1"));
    assertEquals("snake game", editTool.run(
        "{\"action\":\"read\",\"path\":\"snake.html\"}",
        context("request-3", "conversation-1")));

    verify(pool).acquire(any(Duration.class), eq(AcquirePolicy.RETRY_NEXT_IDLE_THEN_CREATE));
    verify(sandbox, times(3)).renew(any(Duration.class));
    ArgumentCaptor<RunCommandRequest> command = ArgumentCaptor.forClass(RunCommandRequest.class);
    verify(sandbox.commands()).run(command.capture());
    assertEquals("/workspace", command.getValue().getWorkingDirectory());
    verify(sandbox, never()).kill();
  }

  @Test
  void isolatesDifferentConversations() {
    Sandbox other = sandboxWithFiles("sandbox-2");
    when(pool.acquire(any(Duration.class), eq(AcquirePolicy.RETRY_NEXT_IDLE_THEN_CREATE)))
        .thenReturn(sandbox, other);

    editTool.run("{\"action\":\"write\",\"path\":\"snake.html\",\"content\":\"snake game\"}",
        context("request-1", "conversation-1"));
    String result = editTool.run("{\"action\":\"read\",\"path\":\"snake.html\"}",
        context("request-2", "conversation-2"));

    assertTrue(result.startsWith("Error: file not found"));
    verify(pool, times(2)).acquire(any(Duration.class),
        eq(AcquirePolicy.RETRY_NEXT_IDLE_THEN_CREATE));
  }

  @Test
  void reusesSkillExtractionAcrossStreamRequests() throws IOException {
    ProductSkillsServiceImpl skills = mock(ProductSkillsServiceImpl.class);
    ProductSkillsEntity skill = new ProductSkillsEntity();
    skill.setName("test-skill");
    skill.setFilePath("test-skill.zip");
    when(skills.readSkillFileBytes("test-skill.zip")).thenReturn(new byte[] {1, 2, 3});
    McpAgent agent = new McpAgent();
    ReflectionTestUtils.setField(agent, "sandboxService", service);
    ReflectionTestUtils.setField(agent, "productSkillsService", skills);

    String first = ReflectionTestUtils.invokeMethod(agent, "invokeSkill", skill,
        context("request-1", "conversation-1"));
    String second = ReflectionTestUtils.invokeMethod(agent, "invokeSkill", skill,
        context("request-2", "conversation-1"));

    assertTrue(first.contains("extracted into the sandbox"));
    assertTrue(second.contains("extracted into the sandbox"));
    assertTrue(service.isSkillExtracted("conversation-1", "test-skill.zip"));
    verify(skills).readSkillFileBytes("test-skill.zip");
    verify(pool).acquire(any(Duration.class), eq(AcquirePolicy.RETRY_NEXT_IDLE_THEN_CREATE));
  }

  @Test
  void preservesLegacyAndDefaultSessionIds() {
    assertEquals("legacy-chat", SandboxService.resolveChatId(Map.of("chatId", "legacy-chat")));
    assertEquals("legacy-chat", SandboxService.resolveChatId(
        Map.of("chatId", "legacy-chat", "conversationChatId", " ")));
    assertEquals(SandboxService.DEFAULT_CHAT_ID, SandboxService.resolveChatId(null));
    assertEquals(SandboxService.DEFAULT_CHAT_ID, SandboxService.resolveChatId(Map.of()));
  }

  @Test
  void remoteLifetimeCoversOperationAndIdleWindowInsteadOfHttpTimeout() {
    property.setRequestTimeout(Duration.ofSeconds(1));
    assertTrue(service.runCommand("conversation-1", "true").ok());

    ArgumentCaptor<Duration> lifetime = ArgumentCaptor.forClass(Duration.class);
    verify(pool).acquire(lifetime.capture(), eq(AcquirePolicy.RETRY_NEXT_IDLE_THEN_CREATE));
    assertTrue(lifetime.getValue().compareTo(
        property.getIdleTimeout().plus(property.getCommandTimeout())) > 0);
    verify(sandbox).renew(lifetime.getValue());
  }

  @Test
  void renewsReusedInstancePastTheOldFiveMinuteLifetime() {
    assertTrue(service.writeFile("conversation-1", "/workspace/snake.html", "snake game"));
    ReflectionTestUtils.setField(state("conversation-1"), "lastUsedNanos",
        System.nanoTime() - TimeUnit.MINUTES.toNanos(6));

    assertEquals("snake game", service.readFile("conversation-1", "/workspace/snake.html"));

    verify(sandbox, times(2)).renew(any(Duration.class));
    verify(pool).acquire(any(Duration.class), eq(AcquirePolicy.RETRY_NEXT_IDLE_THEN_CREATE));
    verify(sandbox, never()).kill();
  }

  @Test
  void temporaryRenewalFailureKeepsExistingFiles() {
    assertTrue(service.writeFile("conversation-1", "/workspace/snake.html", "snake game"));
    when(sandbox.renew(any(Duration.class))).thenThrow(new RuntimeException("request timed out"));

    assertEquals("snake game", service.readFile("conversation-1", "/workspace/snake.html"));

    verify(sandbox, never()).kill();
    verify(pool).acquire(any(Duration.class), eq(AcquirePolicy.RETRY_NEXT_IDLE_THEN_CREATE));
  }

  @Test
  void idleWindowStartsWhenLongRunningOperationCompletes() {
    when(sandbox.commands().run(any(RunCommandRequest.class))).thenAnswer(call -> {
      ReflectionTestUtils.setField(state("conversation-1"), "lastUsedNanos",
          System.nanoTime() - TimeUnit.MINUTES.toNanos(11));
      service.sweepIdle();
      verify(sandbox, never()).kill();
      return new Execution();
    });

    assertTrue(service.runCommand("conversation-1", "long command").ok());
    service.sweepIdle();

    long lastUsed = (long) ReflectionTestUtils.getField(state("conversation-1"), "lastUsedNanos");
    assertTrue(System.nanoTime() - lastUsed < TimeUnit.SECONDS.toNanos(5));
    verify(sandbox, never()).kill();
  }

  @Test
  void stillReclaimsIdleInstances() {
    assertTrue(service.runCommand("conversation-1", "true").ok());
    ReflectionTestUtils.setField(state("conversation-1"), "lastUsedNanos",
        System.nanoTime() - TimeUnit.MINUTES.toNanos(11));

    service.sweepIdle();

    verify(sandbox).kill();
    assertFalse(service.isSkillExtracted("conversation-1", "test-skill.zip"));
  }

  private Object state(String chatId) {
    Map<?, ?> sessions = (Map<?, ?>) ReflectionTestUtils.getField(service, "sessions");
    return sessions.get(chatId);
  }

  @Test
  void exportingMissingOrExpiredInstanceNeverCreatesReplacement() {
    assertNull(service.exportWorkspaceFile("conversation-1", "a.bin", 100));
    verify(pool, never()).acquire(any(Duration.class), any(AcquirePolicy.class));
    service.writeFile("conversation-1", "/workspace/a.bin", new byte[] {1});
    ReflectionTestUtils.setField(state("conversation-1"), "lastUsedNanos",
        System.nanoTime() - TimeUnit.MINUTES.toNanos(11));
    assertNull(service.exportWorkspaceFile("conversation-1", "a.bin", 100));
    verify(pool).acquire(any(Duration.class), any(AcquirePolicy.class));
  }

  @Test
  void rejectsTraversalAndOutsidePathsBeforeCallingSandbox() {
    for (String path : java.util.List.of("../secret", "/etc/passwd", "a/../../secret",
        "/workspace/../secret", "C:\\secret", "a//b")) {
      assertNull(service.exportWorkspaceFile("conversation-1", path, 100));
      assertFalse(service.importWorkspaceFile("conversation-1", path,
          new ByteArrayInputStream(new byte[] {1}), 100));
    }
    verify(pool, never()).acquire(any(Duration.class), any(AcquirePolicy.class));
  }

  @Test
  void holdsLeaseUntilExportStreamIsClosedAndReturnsExactBytes() {
    service.writeFile("conversation-1", "/workspace/a.bin", new byte[] {1});
    byte[] data = {0, 1, (byte) 255};
    java.util.concurrent.atomic.AtomicBoolean closed =
        new java.util.concurrent.atomic.AtomicBoolean();
    when(sandbox.files().readStream(anyString()))
        .thenAnswer(call -> new ByteArrayInputStream(data) {
          @Override
          public void close() throws IOException {
            assertEquals(1, ((java.util.concurrent.atomic.AtomicInteger) ReflectionTestUtils
                .getField(state("conversation-1"), "inFlight")).get());
            ReflectionTestUtils.setField(state("conversation-1"), "lastUsedNanos",
                System.nanoTime() - TimeUnit.MINUTES.toNanos(11));
            service.sweepIdle();
            verify(sandbox, never()).kill();
            closed.set(true);
            super.close();
          }
        });
    assertArrayEquals(data, service.exportWorkspaceFile("conversation-1", "reports/a.bin", 100));
    assertTrue(closed.get());
    assertEquals(0, ((java.util.concurrent.atomic.AtomicInteger) ReflectionTestUtils
        .getField(state("conversation-1"), "inFlight")).get());
    ArgumentCaptor<RunCommandRequest> command = ArgumentCaptor.forClass(RunCommandRequest.class);
    verify(sandbox.commands()).run(command.capture());
    assertTrue(command.getValue().getCommand().contains("os.O_NOFOLLOW"));
    assertTrue(command.getValue().getCommand().contains("dir_fd=descriptor"));
    verify(sandbox.files()).deleteFiles(any());
  }

  @Test
  void closesOversizedExportAndDoesNotRetryAnotherInstance() {
    service.writeFile("conversation-1", "/workspace/a.bin", new byte[] {1});
    when(sandbox.files().readStream(anyString()))
        .thenReturn(new ByteArrayInputStream(new byte[10]));
    assertNull(service.exportWorkspaceFile("conversation-1", "a.bin", 5));
    verify(pool).acquire(any(Duration.class), any(AcquirePolicy.class));
    assertEquals(0, ((java.util.concurrent.atomic.AtomicInteger) ReflectionTestUtils
        .getField(state("conversation-1"), "inFlight")).get());
  }

  @Test
  void importsBinaryWithinOneLeaseAndRejectsOversizedInput() {
    assertTrue(service.importWorkspaceFile("conversation-1", "/workspace/imports/file/a.bin",
        new ByteArrayInputStream(new byte[] {0, 1, (byte) 255}), 10));
    assertFalse(service.importWorkspaceFile("conversation-1", "/workspace/imports/file/b.bin",
        new ByteArrayInputStream(new byte[11]), 10));
    verify(pool).acquire(any(Duration.class), any(AcquirePolicy.class));
    ArgumentCaptor<RunCommandRequest> command = ArgumentCaptor.forClass(RunCommandRequest.class);
    verify(sandbox.commands()).run(command.capture());
    assertTrue(command.getValue().getCommand().contains("os.O_EXCL"));
    verify(sandbox.files(), times(2)).deleteFiles(any());
  }

  private Map<String, Object> context(String streamId, String conversationId) {
    Map<String, Object> context = new HashMap<>();
    GlobalMessageContext.putChatIds(context, "chatProduct12:stream:" + streamId, conversationId);
    return context;
  }

  private Sandbox sandboxWithFiles(String id) {
    Sandbox instance = mock(Sandbox.class);
    Filesystem files = mock(Filesystem.class);
    Commands commands = mock(Commands.class);
    Map<String, Object> contents = new HashMap<>();
    when(instance.getId()).thenReturn(id);
    when(instance.files()).thenReturn(files);
    when(instance.commands()).thenReturn(commands);
    when(commands.run(any(RunCommandRequest.class))).thenReturn(new Execution());
    doAnswer(call -> {
      contents.put(call.getArgument(0), call.getArgument(1));
      return null;
    }).when(files).writeFile(anyString(), any());
    when(files.readFile(anyString())).thenAnswer(call -> contents.get(call.getArgument(0)));
    return instance;
  }
}
