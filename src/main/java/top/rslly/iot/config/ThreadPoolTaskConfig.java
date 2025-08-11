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

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 线程池配置
 * 
 * @author SpringRoot
 *
 */
@Configuration
@EnableAsync
public class ThreadPoolTaskConfig {

  /**
   * 默认情况下，在创建了线程池后，线程池中的线程数为0，当有任务来之后，就会创建一个线程去执行任务， 当线程池中的线程数目达到corePoolSize后，就会把到达的任务放到缓存队列当中；
   * 当队列满了，就继续创建线程，当线程数量大于等于maxPoolSize后，开始使用拒绝策略拒绝
   */

  /** 核心线程数（默认线程数） */
  private static final int corePoolSize = 20;
  /** 最大线程数 */
  private static final int maxPoolSize = 300;
  /** 允许线程空闲时间（单位：默认为秒） */
  private static final int keepAliveTime = 60;
  /** 缓冲队列大小 */
  private static final int queueCapacity = 100;
  /** 线程池名前缀 */
  private static final String threadNamePrefix = "Async-Service-";

  @Bean("taskExecutor") // bean的名称，默认为首字母小写的方法名
  public ThreadPoolTaskExecutor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(corePoolSize);
    executor.setMaxPoolSize(maxPoolSize);
    executor.setQueueCapacity(queueCapacity);
    executor.setKeepAliveSeconds(keepAliveTime);
    executor.setThreadNamePrefix(threadNamePrefix);

    // 线程池对拒绝任务的处理策略
    // CallerRunsPolicy：由调用线程（提交任务的线程）处理该任务
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    // 初始化
    executor.initialize();
    return executor;
  }
}
