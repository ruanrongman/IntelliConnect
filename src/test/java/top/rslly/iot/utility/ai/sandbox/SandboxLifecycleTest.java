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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alibaba.opensandbox.sandbox.Sandbox;
import com.alibaba.opensandbox.sandbox.domain.exceptions.SandboxApiException;
import com.alibaba.opensandbox.sandbox.domain.exceptions.SandboxError;
import com.alibaba.opensandbox.sandbox.domain.exceptions.SandboxNotFoundException;
import com.alibaba.opensandbox.sandbox.domain.models.execd.executions.Execution;
import com.alibaba.opensandbox.sandbox.domain.models.execd.executions.RunCommandRequest;
import com.alibaba.opensandbox.sandbox.domain.pool.AcquirePolicy;
import com.alibaba.opensandbox.sandbox.domain.services.Commands;
import com.alibaba.opensandbox.sandbox.domain.services.Filesystem;
import com.alibaba.opensandbox.sandbox.pool.SandboxPool;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

@Timeout(20)
class SandboxLifecycleTest {
  private final ExecutorService executor = Executors.newFixedThreadPool(4);

  @AfterEach
  void stopWorkers() throws InterruptedException {
    executor.shutdownNow();
    assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
  }

  @Test
  void missingFileDoesNotDestroyOtherFiles() {
    Rig rig = new Rig(1);
    assertTrue(rig.service.writeFile("chat", "/workspace/snake.html", "saved"));
    when(rig.original.files().readFile("/workspace/typo.html"))
        .thenThrow(new IllegalArgumentException("404: file not found"));

    assertNull(rig.service.readFile("chat", "/workspace/typo.html"));
    assertEquals("saved", rig.service.readFile("chat", "/workspace/snake.html"));
    verify(rig.original, never()).kill();
    rig.verifyAcquisitions(1);
  }

  @Test
  void commandTransportFailureIsNotReplayedAndKeepsFiles() {
    Rig rig = new Rig(1);
    rig.service.writeFile("chat", "/workspace/snake.html", "saved");
    when(rig.original.commands().run(any(RunCommandRequest.class)))
        .thenThrow(new IllegalStateException("request timed out"));

    assertFalse(rig.service.runCommand("chat", "append data").ok());
    assertEquals("saved", rig.service.readFile("chat", "/workspace/snake.html"));
    verify(rig.original.commands()).run(any(RunCommandRequest.class));
    verify(rig.original, never()).kill();
    rig.verifyAcquisitions(1);
  }

  @Test
  void confirmedMissingSandboxRebuildsOnceAndExplainsFileLoss() {
    Rig rig = new Rig(1);
    when(rig.original.commands().run(any(RunCommandRequest.class)))
        .thenThrow(new SandboxNotFoundException("removed externally"));

    SandboxResult result = rig.service.runCommand("chat", "ls");

    assertTrue(result.ok());
    assertTrue(result.stderr().contains("previous sandbox no longer existed"));
    verify(rig.original).kill();
    verify(rig.replacement, never()).kill();
    rig.verifyAcquisitions(2);
  }

  @Test
  void errorMessageAloneDoesNotProveTheSandboxIsMissing() {
    Rig rig = new Rig(1);
    when(rig.original.commands().run(any(RunCommandRequest.class)))
        .thenThrow(new IllegalStateException("404 SANDBOX_NOT_FOUND in command output"));

    assertFalse(rig.service.runCommand("chat", "run").ok());

    verify(rig.original, never()).kill();
    rig.verifyAcquisitions(1);
  }

  @ParameterizedTest
  @CsvSource({"FILE_NOT_FOUND, 1", "SANDBOX_NOT_FOUND, 2", "DOCKER::SANDBOX_NOT_FOUND, 2"})
  void distinguishesStructuredSdkErrorsEvenWhenWrapped(String code, int acquisitions) {
    Rig rig = new Rig(1);
    rig.service.writeFile("chat", "/workspace/snake.html", "saved");
    SandboxApiException apiError = new SandboxApiException("not found", null, 404,
        new SandboxError(code, "not found"));
    when(rig.original.commands().run(any(RunCommandRequest.class)))
        .thenThrow(new IllegalStateException("SDK request failed", apiError));

    assertEquals(acquisitions == 2, rig.service.runCommand("chat", "ls").ok());

    rig.verifyAcquisitions(acquisitions);
    verify(rig.original, times(acquisitions - 1)).kill();
    if (acquisitions == 1) {
      assertEquals("saved", rig.service.readFile("chat", "/workspace/snake.html"));
    }
  }

  @Test
  void lateInvalidationOfOldLeaseCannotInvalidateReplacement() {
    Rig rig = new Rig(1);
    Object oldLease = ReflectionTestUtils.invokeMethod(rig.service, "lease", "chat");
    ReflectionTestUtils.invokeMethod(rig.service, "release", oldLease);
    rig.expire("chat");
    rig.service.sweepIdle();
    assertTrue(rig.service.writeFile("chat", "/workspace/snake.html", "replacement data"));

    ReflectionTestUtils.invokeMethod(rig.service, "invalidate", oldLease);

    assertEquals("replacement data", rig.service.readFile("chat", "/workspace/snake.html"));
    verify(rig.replacement, never()).kill();
    rig.verifyAcquisitions(2);
  }

  @Test
  void invalidGenerationDrainsLeasesAndFinishesKillBeforeCreatingReplacement() throws Exception {
    Rig rig = new Rig(1);
    CountDownLatch running = new CountDownLatch(1);
    CountDownLatch finishCommand = new CountDownLatch(1);
    CountDownLatch killing = new CountDownLatch(1);
    CountDownLatch finishKill = new CountDownLatch(1);
    when(rig.original.commands().run(any(RunCommandRequest.class))).thenAnswer(call -> {
      RunCommandRequest request = call.getArgument(0);
      if (request.getCommand().equals("long")) {
        running.countDown();
        await(finishCommand);
        return new Execution();
      }
      throw new SandboxNotFoundException("removed externally");
    });
    doAnswer(call -> {
      killing.countDown();
      await(finishKill);
      return null;
    }).when(rig.original).kill();
    Future<SandboxResult> longCommand =
        executor.submit(() -> rig.service.runCommand("chat", "long"));
    try {
      await(running);
      Future<SandboxResult> failed =
          executor.submit(() -> rig.service.runCommand("chat", "missing"));
      eventually(
          () -> Boolean.TRUE.equals(ReflectionTestUtils.getField(rig.state("chat"), "invalid")));
      Future<SandboxResult> follower = executor.submit(() -> rig.service.runCommand("chat", "new"));
      eventually(() -> rig.waiters("chat") == 2);
      verify(rig.original, never()).kill();
      finishCommand.countDown();
      await(killing);
      assertFalse(failed.isDone());
      assertFalse(follower.isDone());
      rig.verifyAcquisitions(1);
      finishKill.countDown();

      assertTrue(longCommand.get(5, TimeUnit.SECONDS).ok());
      assertTrue(failed.get(5, TimeUnit.SECONDS).ok());
      assertTrue(follower.get(5, TimeUnit.SECONDS).ok());
      rig.verifyAcquisitions(2);
      verify(rig.original).kill();
      verify(rig.replacement, never()).kill();
    } finally {
      finishCommand.countDown();
      finishKill.countDown();
    }
  }

  @Test
  void sweeperRechecksIdleDeadlineAfterAcquiringTheLock() throws Exception {
    Rig rig = new Rig(1);
    rig.service.writeFile("chat", "/workspace/snake.html", "saved");
    rig.expire("chat");
    ReentrantLock lock = rig.lock("chat");
    Thread sweeper = new Thread(rig.service::sweepIdle);
    lock.lock();
    try {
      sweeper.start();
      eventually(() -> lock.hasQueuedThread(sweeper));
      ReflectionTestUtils.setField(rig.state("chat"), "lastUsedNanos", System.nanoTime());
    } finally {
      lock.unlock();
    }
    sweeper.join(5000);
    assertFalse(sweeper.isAlive());

    assertEquals("saved", rig.service.readFile("chat", "/workspace/snake.html"));
    verify(rig.original, never()).kill();
  }

  @Test
  void concurrentSameChatCreationUsesOneCapacitySlotAndOneInstance() throws Exception {
    Rig rig = new Rig(1);
    CountDownLatch creating = new CountDownLatch(1);
    CountDownLatch finish = new CountDownLatch(1);
    when(rig.pool.acquire(any(Duration.class), eq(AcquirePolicy.RETRY_NEXT_IDLE_THEN_CREATE)))
        .thenAnswer(call -> {
          creating.countDown();
          await(finish);
          return rig.original;
        });
    Future<SandboxResult> first = executor.submit(() -> rig.service.runCommand("chat", "first"));
    try {
      await(creating);
      Future<SandboxResult> second =
          executor.submit(() -> rig.service.runCommand("chat", "second"));
      eventually(() -> rig.waiters("chat") == 1);
      finish.countDown();

      assertTrue(first.get(5, TimeUnit.SECONDS).ok());
      assertTrue(second.get(5, TimeUnit.SECONDS).ok());
      rig.verifyAcquisitions(1);
      verify(rig.original, never()).kill();
    } finally {
      finish.countDown();
    }
  }

  @Test
  void networkCreationDoesNotHoldStripedLock() throws Exception {
    Rig rig = new Rig(2);
    assertEquals("Aa".hashCode(), "BB".hashCode());
    CountDownLatch creating = new CountDownLatch(1);
    CountDownLatch finish = new CountDownLatch(1);
    AtomicInteger calls = new AtomicInteger();
    when(rig.pool.acquire(any(Duration.class), eq(AcquirePolicy.RETRY_NEXT_IDLE_THEN_CREATE)))
        .thenAnswer(call -> {
          if (calls.getAndIncrement() == 0) {
            creating.countDown();
            await(finish);
            return rig.original;
          }
          return rig.replacement;
        });
    Future<SandboxResult> first = executor.submit(() -> rig.service.runCommand("Aa", "first"));
    try {
      await(creating);
      Future<SandboxResult> second = executor.submit(() -> rig.service.runCommand("BB", "second"));
      assertTrue(second.get(5, TimeUnit.SECONDS).ok());
      assertFalse(first.isDone());
      finish.countDown();
      assertTrue(first.get(5, TimeUnit.SECONDS).ok());
    } finally {
      finish.countDown();
    }
  }

  @Test
  void creationFailureReturnsCapacityAndAllowsTheNextAttempt() {
    Rig rig = new Rig(1);
    when(rig.pool.acquire(any(Duration.class), eq(AcquirePolicy.RETRY_NEXT_IDLE_THEN_CREATE)))
        .thenThrow(new IllegalStateException("pool unavailable"));
    try (MockedStatic<Sandbox> sdk = mockStatic(Sandbox.class)) {
      sdk.when(Sandbox::builder).thenThrow(new IllegalStateException("creation unavailable"));

      assertFalse(rig.service.runCommand("chat", "first").ok());

      assertEquals(1, rig.permits());
      assertEquals(false, ReflectionTestUtils.getField(rig.state("chat"), "creating"));
      doReturn(rig.replacement).when(rig.pool)
          .acquire(any(Duration.class), eq(AcquirePolicy.RETRY_NEXT_IDLE_THEN_CREATE));
      assertTrue(rig.service.runCommand("chat", "second").ok());
      assertEquals(0, rig.permits());
    }
  }

  @Test
  void expiredSkillIsUploadedAgainToTheNewInstance() {
    Rig rig = new Rig(1);
    AtomicInteger uploads = new AtomicInteger();
    assertTrue(rig.service.prepareSkill("chat", "test.zip", "/workspace/skills/test", () -> {
      uploads.incrementAndGet();
      return new byte[] {1};
    }).ok());
    rig.expire("chat");

    assertTrue(rig.service.prepareSkill("chat", "test.zip", "/workspace/skills/test", () -> {
      uploads.incrementAndGet();
      return new byte[] {2};
    }).ok());

    assertEquals(2, uploads.get());
    verify(rig.original).kill();
    verify(rig.replacement.files()).writeFile(anyString(), any());
    assertTrue(rig.service.isSkillExtracted("chat", "test.zip"));
  }

  @Test
  void extractionRetriesUploadAsWellAsCommandOnMissingSandbox() {
    Rig rig = new Rig(1);
    when(rig.original.commands().run(any(RunCommandRequest.class)))
        .thenThrow(new SandboxNotFoundException("removed between upload and extraction"));

    assertTrue(
        rig.service.uploadAndExtract("chat", new byte[] {1}, "test.zip", "/workspace/skill").ok());

    verify(rig.original.files()).writeFile(anyString(), any());
    verify(rig.replacement.files()).writeFile(anyString(), any());
    verify(rig.replacement.commands()).run(any(RunCommandRequest.class));
    rig.verifyAcquisitions(2);
  }

  @Test
  void concurrentSkillPreparationUploadsOnceAndCannotBeEvictedMidway() throws Exception {
    Rig rig = new Rig(1);
    CountDownLatch readingArchive = new CountDownLatch(1);
    CountDownLatch finish = new CountDownLatch(1);
    AtomicInteger uploads = new AtomicInteger();
    Future<SandboxResult> first = executor.submit(() -> rig.service.prepareSkill("chat", "test.zip",
        "/workspace/skills/test", () -> {
          uploads.incrementAndGet();
          readingArchive.countDown();
          await(finish);
          return new byte[] {1};
        }));
    try {
      await(readingArchive);
      Future<SandboxResult> second =
          executor.submit(() -> rig.service.prepareSkill("chat", "test.zip",
              "/workspace/skills/test", () -> {
                uploads.incrementAndGet();
                return new byte[] {2};
              }));
      eventually(() -> ((AtomicInteger) ReflectionTestUtils.getField(rig.state("chat"), "inFlight"))
          .get() == 2);
      assertFalse(rig.service.runCommand("other-chat", "ls").ok());
      verify(rig.original, never()).kill();
      finish.countDown();

      assertTrue(first.get(5, TimeUnit.SECONDS).ok());
      assertTrue(second.get(5, TimeUnit.SECONDS).ok());
      assertEquals(1, uploads.get());
      verify(rig.original.files()).writeFile(anyString(), any());
      rig.verifyAcquisitions(1);
    } finally {
      finish.countDown();
    }
  }

  @Test
  void failedExtractionDoesNotSetSkillMarker() {
    Rig rig = new Rig(1);
    Execution failed = new Execution();
    failed.setExitCode(1);
    when(rig.original.commands().run(any(RunCommandRequest.class)))
        .thenReturn(failed, new Execution(), new Execution());

    assertFalse(rig.service
        .prepareSkill("chat", "test.zip", "/workspace/skill", () -> new byte[] {1}).ok());
    assertFalse(rig.service.isSkillExtracted("chat", "test.zip"));
    assertTrue(rig.service
        .prepareSkill("chat", "test.zip", "/workspace/skill", () -> new byte[] {1}).ok());
    assertTrue(rig.service.isSkillExtracted("chat", "test.zip"));
    verify(rig.original.files(), times(2)).writeFile(anyString(), any());
  }

  @Test
  void shutdownRejectsNewLeasesAndKillsAfterLastOperation() throws Exception {
    Rig rig = new Rig(1);
    CountDownLatch running = new CountDownLatch(1);
    CountDownLatch finish = new CountDownLatch(1);
    when(rig.original.commands().run(any(RunCommandRequest.class))).thenAnswer(call -> {
      running.countDown();
      await(finish);
      return new Execution();
    });
    Future<SandboxResult> active = executor.submit(() -> rig.service.runCommand("chat", "long"));
    try {
      await(running);
      rig.service.shutdown();
      assertFalse(rig.service.isAvailable());
      assertFalse(rig.service.runCommand("chat", "new").ok());
      assertFalse(rig.service.writeFile("other-chat", "/workspace/new", "data"));
      verify(rig.original, never()).kill();
      finish.countDown();

      assertTrue(active.get(5, TimeUnit.SECONDS).ok());
      verify(rig.original).kill();
      assertNull(ReflectionTestUtils.getField(rig.state("chat"), "sandbox"));
      assertEquals(1, rig.permits());
      rig.service.shutdown();
      verify(rig.original).kill();
      rig.verifyAcquisitions(1);
    } finally {
      finish.countDown();
    }
  }

  @Test
  void shutdownWakesWaitingCallersAndDisposesSandboxCreatedAfterClosing() throws Exception {
    Rig rig = new Rig(1);
    CountDownLatch creating = new CountDownLatch(1);
    CountDownLatch finish = new CountDownLatch(1);
    when(rig.pool.acquire(any(Duration.class), eq(AcquirePolicy.RETRY_NEXT_IDLE_THEN_CREATE)))
        .thenAnswer(call -> {
          creating.countDown();
          await(finish);
          return rig.original;
        });
    Future<SandboxResult> first = executor.submit(() -> rig.service.runCommand("chat", "first"));
    try {
      await(creating);
      Future<SandboxResult> second =
          executor.submit(() -> rig.service.runCommand("chat", "second"));
      eventually(() -> rig.waiters("chat") == 1);
      rig.service.shutdown();
      assertFalse(second.get(5, TimeUnit.SECONDS).ok());
      finish.countDown();
      assertFalse(first.get(5, TimeUnit.SECONDS).ok());

      verify(rig.original).kill();
      verify(rig.original.commands(), never()).run(any(RunCommandRequest.class));
      assertNull(ReflectionTestUtils.getField(rig.state("chat"), "sandbox"));
      assertEquals(1, rig.permits());
    } finally {
      finish.countDown();
    }
  }

  @Test
  void failedKillHoldsCapacityUntilRetryConfirmsTermination() {
    Rig rig = new Rig(1);
    rig.service.runCommand("chat", "true");
    doThrow(new IllegalStateException("HTTP 404 from proxy")).when(rig.original).kill();
    rig.expire("chat");
    rig.service.sweepIdle();

    assertEquals(0, rig.permits());
    assertFalse(rig.service.runCommand("other-chat", "true").ok());
    doNothing().when(rig.original).kill();
    rig.service.retryPendingKills();
    rig.service.retryPendingKills();
    assertEquals(1, rig.permits());
    assertTrue(rig.service.runCommand("other-chat", "true").ok());
    assertEquals(0, rig.permits());
    verify(rig.original).close();
  }

  private static void await(CountDownLatch latch) throws InterruptedException {
    assertTrue(latch.await(5, TimeUnit.SECONDS), "Timed out waiting for worker");
  }

  private static void eventually(BooleanSupplier condition) {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
    while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
      LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
    }
    assertTrue(condition.getAsBoolean(), "Worker did not reach the expected state");
  }

  private static class Rig {
    final SandboxService service;
    final SandboxPool pool = mock(SandboxPool.class);
    final Sandbox original = sandbox("original");
    final Sandbox replacement = sandbox("replacement");

    Rig(int limit) {
      SandboxProperty property = new SandboxProperty();
      property.setEnabled(true);
      property.setPoolEnabled(false);
      property.setMaxInstances(limit);
      service = new SandboxService(property);
      ReflectionTestUtils.setField(service, "pool", pool);
      when(pool.acquire(any(Duration.class), eq(AcquirePolicy.RETRY_NEXT_IDLE_THEN_CREATE)))
          .thenReturn(original, replacement);
    }

    Object state(String key) {
      return ((Map<?, ?>) ReflectionTestUtils.getField(service, "sessions")).get(key);
    }

    void expire(String key) {
      ReflectionTestUtils.setField(state(key), "lastUsedNanos",
          System.nanoTime() - Duration.ofMinutes(11).toNanos());
    }

    ReentrantLock lock(String key) {
      return ReflectionTestUtils.invokeMethod(service, "lockFor", key);
    }

    int waiters(String key) {
      ReentrantLock lock = lock(key);
      lock.lock();
      try {
        return lock
            .getWaitQueueLength((Condition) ReflectionTestUtils.getField(state(key), "changed"));
      } finally {
        lock.unlock();
      }
    }

    int permits() {
      return ((Semaphore) ReflectionTestUtils.getField(service, "capacity")).availablePermits();
    }

    void verifyAcquisitions(int count) {
      verify(pool, times(count)).acquire(any(Duration.class),
          eq(AcquirePolicy.RETRY_NEXT_IDLE_THEN_CREATE));
    }
  }

  private static Sandbox sandbox(String id) {
    Sandbox sandbox = mock(Sandbox.class);
    Filesystem files = mock(Filesystem.class);
    Commands commands = mock(Commands.class);
    Map<String, Object> contents = new ConcurrentHashMap<>();
    when(sandbox.getId()).thenReturn(id);
    when(sandbox.files()).thenReturn(files);
    when(sandbox.commands()).thenReturn(commands);
    when(commands.run(any(RunCommandRequest.class))).thenReturn(new Execution());
    doAnswer(call -> {
      contents.put(call.getArgument(0), call.getArgument(1));
      return null;
    }).when(files).writeFile(anyString(), any());
    when(files.readFile(anyString())).thenAnswer(call -> contents.get(call.getArgument(0)));
    return sandbox;
  }
}
