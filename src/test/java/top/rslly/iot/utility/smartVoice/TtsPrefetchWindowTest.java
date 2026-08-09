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
package top.rslly.iot.utility.smartVoice;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class TtsPrefetchWindowTest {

  @Test
  void startsThirdTaskOnlyAfterCurrentPlaybackReleasesItsSlot() throws Exception {
    TtsPrefetchWindow window = new TtsPrefetchWindow();
    TtsPrefetchWindow.Permit current = window.acquire(() -> true);
    TtsPrefetchWindow.Permit prefetched = window.acquire(() -> true);
    AtomicBoolean active = new AtomicBoolean(true);
    AtomicReference<TtsPrefetchWindow.Permit> thirdPermit = new AtomicReference<>();
    AtomicReference<TtsPrefetchWindow.Permit> fourthPermit = new AtomicReference<>();
    AtomicReference<Throwable> failure = new AtomicReference<>();
    CountDownLatch thirdWaiting = new CountDownLatch(1);
    CountDownLatch thirdAcquired = new CountDownLatch(1);
    CountDownLatch fourthWaiting = new CountDownLatch(1);
    CountDownLatch fourthAcquired = new CountDownLatch(1);
    Thread thirdThread = null;
    Thread fourthThread = null;

    try {
      thirdThread = acquireOnVirtualThread(window, active, thirdPermit, failure, thirdWaiting,
          thirdAcquired);
      assertTrue(thirdWaiting.await(1, TimeUnit.SECONDS));
      assertFalse(thirdAcquired.await(200, TimeUnit.MILLISECONDS));

      current.close();
      assertTrue(thirdAcquired.await(1, TimeUnit.SECONDS));

      // A second close must not create a third slot.
      current.close();
      fourthThread = acquireOnVirtualThread(window, active, fourthPermit, failure, fourthWaiting,
          fourthAcquired);
      assertTrue(fourthWaiting.await(1, TimeUnit.SECONDS));
      assertFalse(fourthAcquired.await(200, TimeUnit.MILLISECONDS));

      prefetched.close();
      assertTrue(fourthAcquired.await(1, TimeUnit.SECONDS));
      assertNull(failure.get());
    } finally {
      active.set(false);
      current.close();
      prefetched.close();
      close(thirdPermit.get());
      close(fourthPermit.get());
      join(thirdThread);
      join(fourthThread);
    }
  }

  @Test
  void cancelledWaitReturnsWithoutStartingAnotherTask() throws Exception {
    TtsPrefetchWindow window = new TtsPrefetchWindow();
    TtsPrefetchWindow.Permit current = window.acquire(() -> true);
    TtsPrefetchWindow.Permit prefetched = window.acquire(() -> true);
    AtomicBoolean active = new AtomicBoolean(true);
    AtomicReference<TtsPrefetchWindow.Permit> result = new AtomicReference<>();
    AtomicReference<Throwable> failure = new AtomicReference<>();
    CountDownLatch waiting = new CountDownLatch(1);
    CountDownLatch completed = new CountDownLatch(1);
    Thread waitingThread = null;

    try {
      waitingThread = acquireOnVirtualThread(window, active, result, failure, waiting, completed);
      assertTrue(waiting.await(1, TimeUnit.SECONDS));
      assertFalse(completed.await(200, TimeUnit.MILLISECONDS));

      active.set(false);
      assertTrue(completed.await(1, TimeUnit.SECONDS));
      assertNull(result.get());
      assertNull(failure.get());
    } finally {
      active.set(false);
      current.close();
      prefetched.close();
      join(waitingThread);
    }
  }

  @Test
  void rejectsNonPositiveWindowSize() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> new TtsPrefetchWindow(0));

    assertEquals("maxInFlight must be positive", exception.getMessage());
  }

  private Thread acquireOnVirtualThread(TtsPrefetchWindow window, AtomicBoolean active,
      AtomicReference<TtsPrefetchWindow.Permit> result, AtomicReference<Throwable> failure,
      CountDownLatch waiting, CountDownLatch completed) {
    return Thread.ofVirtual().start(() -> {
      waiting.countDown();
      try {
        result.set(window.acquire(active::get));
      } catch (Throwable throwable) {
        failure.set(throwable);
      } finally {
        completed.countDown();
      }
    });
  }

  private void close(TtsPrefetchWindow.Permit permit) {
    if (permit != null) {
      permit.close();
    }
  }

  private void join(Thread thread) throws InterruptedException {
    if (thread != null) {
      thread.join(TimeUnit.SECONDS.toMillis(1));
      assertFalse(thread.isAlive());
    }
  }
}
