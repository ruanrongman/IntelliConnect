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
package top.rslly.iot.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** One model call's token usage; used when {@code storage.database} is mysql. */
@Entity
@Table(name = "llm_usage", indexes = @Index(columnList = "product_id,time"))
@Getter
@Setter
public class LlmUsageEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(nullable = false)
  private long time;
  @Column(name = "product_id")
  private Integer productId;
  @Column(name = "chat_id", length = 255)
  private String chatId;
  @Column(length = 128)
  private String scene;
  @Column(length = 128)
  private String model;
  @Column(length = 255)
  private String provider;
  @Column(name = "prompt_tokens")
  private long promptTokens;
  @Column(name = "cached_tokens")
  private long cachedTokens;
  @Column(name = "cache_creation_tokens")
  private long cacheCreationTokens;
  @Column(name = "completion_tokens")
  private long completionTokens;
  @Column(name = "reasoning_tokens")
  private long reasoningTokens;
  @Column(name = "estimated_prompt_tokens")
  private long estimatedPromptTokens;
  @Column(name = "latency_ms")
  private long latencyMs;
  @Column(name = "usage_reported")
  private boolean usageReported;
  private boolean stream;
}
