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

/**
 * Token usage summary of one chat turn, keyed by the turn's {@code request_id} in
 * {@code history_message}. One row per answered turn, so it can be shown under the reply again when
 * the history is reloaded.
 */
@Entity
@Table(name = "llm_turn_usage", indexes = {
    @Index(name = "idx_llm_turn_usage_request_id", columnList = "request_id", unique = true),
    @Index(name = "idx_llm_turn_usage_chat_id", columnList = "chat_id")})
@Getter
@Setter
public class LlmTurnUsageEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(name = "request_id", nullable = false, length = 64)
  private String requestId;
  @Column(name = "chat_id", nullable = false, length = 255)
  private String chatId;
  @Column(name = "product_id")
  private Integer productId;
  @Column(nullable = false)
  private long time;
  /** Serialized {@code LlmUsageSummary}. */
  @Column(name = "usage_json", nullable = false, columnDefinition = "TEXT")
  private String usageJson;
}
