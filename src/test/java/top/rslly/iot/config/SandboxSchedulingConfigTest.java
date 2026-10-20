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
package top.rslly.iot.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import top.rslly.iot.utility.ai.sandbox.SandboxService;

class SandboxSchedulingConfigTest {

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void blockingMaintenanceKeepsBootSchedulerAndHeartbeatRunning(boolean virtualThreads) {
    new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class,
            TaskSchedulingAutoConfiguration.class))
        .withUserConfiguration(SandboxSchedulingConfig.class, SchedulingTestConfig.class)
        .withPropertyValues("spring.threads.virtual.enabled=" + virtualThreads)
        .run(context -> {
          assertThat(context).hasNotFailed();
          TaskScheduler defaultScheduler = context.getBean("taskScheduler", TaskScheduler.class);
          TaskScheduler sandboxScheduler =
              context.getBean("sandboxTaskScheduler", TaskScheduler.class);
          assertThat(defaultScheduler).isNotSameAs(sandboxScheduler);
          assertThat(defaultScheduler).isInstanceOf(virtualThreads
              ? SimpleAsyncTaskScheduler.class
              : ThreadPoolTaskScheduler.class);
          Jobs jobs = context.getBean(Jobs.class);
          try {
            assertTrue(jobs.cleanupStarted.await(5, TimeUnit.SECONDS));
            CountDownLatch nextHeartbeat = new CountDownLatch(1);
            jobs.nextHeartbeat.set(nextHeartbeat);

            assertTrue(nextHeartbeat.await(5, TimeUnit.SECONDS), "Maintenance blocked heartbeat");
            assertTrue(jobs.cleanupThread.get().startsWith("sandbox-maintenance-"));
            assertThat(jobs.heartbeatThread.get()).doesNotStartWith("sandbox-maintenance-");
          } finally {
            jobs.finishCleanup.countDown();
          }
        });
  }

  @Test
  void bothSandboxMaintenanceJobsUseDedicatedScheduler() throws NoSuchMethodException {
    for (String name : new String[] {"sweepIdle", "retryPendingKills"}) {
      Scheduled scheduled = SandboxService.class.getMethod(name).getAnnotation(Scheduled.class);
      assertEquals("sandboxTaskScheduler", scheduled.scheduler());
    }
  }

  @Configuration(proxyBeanMethods = false)
  @EnableScheduling
  static class SchedulingTestConfig {
    @Bean
    Jobs jobs() {
      return new Jobs();
    }
  }

  static class Jobs {
    final CountDownLatch cleanupStarted = new CountDownLatch(1);
    final CountDownLatch finishCleanup = new CountDownLatch(1);
    final AtomicReference<CountDownLatch> nextHeartbeat = new AtomicReference<>();
    final AtomicReference<String> cleanupThread = new AtomicReference<>();
    final AtomicReference<String> heartbeatThread = new AtomicReference<>();

    @Scheduled(fixedDelay = 10, scheduler = "sandboxTaskScheduler")
    public void cleanup() throws InterruptedException {
      cleanupThread.set(Thread.currentThread().getName());
      cleanupStarted.countDown();
      assertTrue(finishCleanup.await(10, TimeUnit.SECONDS));
    }

    @Scheduled(fixedDelay = 10)
    public void heartbeat() {
      heartbeatThread.set(Thread.currentThread().getName());
      CountDownLatch latch = nextHeartbeat.get();
      if (latch != null) {
        latch.countDown();
      }
    }
  }
}
