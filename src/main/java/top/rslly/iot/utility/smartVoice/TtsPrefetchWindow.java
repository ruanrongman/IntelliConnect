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

import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

final class TtsPrefetchWindow {
  static final int CURRENT_AND_ONE_PREFETCHED = 2;
  private static final long ACTIVE_CHECK_INTERVAL_MS = 100;

  private final Semaphore slots;

  TtsPrefetchWindow() {
    this(CURRENT_AND_ONE_PREFETCHED);
  }

  TtsPrefetchWindow(int maxInFlight) {
    if (maxInFlight < 1) {
      throw new IllegalArgumentException("maxInFlight must be positive");
    }
    this.slots = new Semaphore(maxInFlight, true);
  }

  Permit acquire(BooleanSupplier isActive) throws InterruptedException {
    Objects.requireNonNull(isActive, "isActive");
    while (isActive.getAsBoolean()) {
      if (!slots.tryAcquire(ACTIVE_CHECK_INTERVAL_MS, TimeUnit.MILLISECONDS)) {
        continue;
      }
      try {
        if (isActive.getAsBoolean()) {
          return new Permit(slots);
        }
      } catch (RuntimeException | Error e) {
        slots.release();
        throw e;
      }
      slots.release();
      return null;
    }
    return null;
  }

  static final class Permit implements AutoCloseable {
    private final Semaphore slots;
    private final AtomicBoolean released = new AtomicBoolean();

    private Permit(Semaphore slots) {
      this.slots = slots;
    }

    @Override
    public void close() {
      if (released.compareAndSet(false, true)) {
        slots.release();
      }
    }
  }
}
