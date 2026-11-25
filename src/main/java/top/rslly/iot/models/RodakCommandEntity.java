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
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "rodak_command")
@Getter
@Setter
public class RodakCommandEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Setter(lombok.AccessLevel.NONE)
  private Long id;

  @Column(name = "command_no", nullable = false, unique = true, length = 32)
  private String commandNo;
  @Column(name = "device_id", nullable = false)
  private Long deviceId;
  private long bindingVersion;
  private Instant createdAt;
  private Instant expiresAt;
  private String acknowledgement;
}
