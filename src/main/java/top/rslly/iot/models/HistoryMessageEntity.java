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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "history_message")
public class HistoryMessageEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Integer id;

  @Size(max = 255)
  @NotNull
  @Column(name = "chat_id", nullable = false)
  private String chatId;

  @Size(max = 255)
  @NotNull
  @Column(name = "request_id", nullable = false)
  private String requestId;

  @NotNull
  @Column(name = "sequence_num", nullable = false)
  private Integer sequenceNum;

  @Size(max = 255)
  @NotNull
  @Column(name = "message_type", nullable = false)
  private String messageType;

  @Size(max = 6144)
  @NotNull
  @Column(name = "content", nullable = false)
  private String content;

  @NotNull
  @Column(name = "time", nullable = false)
  private Long time;


}
