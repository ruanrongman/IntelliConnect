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
import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rodak_device", indexes = {
    @Index(name = "idx_rodak_device_key", columnList = "device_key", unique = true),
    @Index(name = "idx_rodak_device_product", columnList = "product_id")
})
public class RodakDeviceEntity {
  private static final SecureRandom RANDOM = new SecureRandom();

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "device_uuid", nullable = false, unique = true, length = 36)
  private String deviceUuid = UUID.randomUUID().toString();

  @Column(name = "device_key", nullable = false, unique = true, length = 128)
  private String deviceKey;

  @Column(name = "product_id", nullable = false)
  private int productId;

  @Column(name = "product_key", nullable = false, length = 64)
  private String productKey = "rymcu-bigsmart";

  @Column(name = "status", nullable = false, length = 32)
  private String status = "PENDING";

  @Column(name = "credential_digest", nullable = false, length = 128)
  private String credentialDigest;

  @Column(name = "token_version", nullable = false)
  // A device can pair again after physical deletion while its previous JWT is still unexpired.
  private long tokenVersion = RANDOM.nextLong(1, 1L << 52);

  @Column(name = "last_seen_at")
  private Instant lastSeenAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  public Long getId() {
    return id;
  }

  public String getDeviceUuid() {
    return deviceUuid;
  }

  public String getDeviceKey() {
    return deviceKey;
  }

  public void setDeviceKey(String value) {
    deviceKey = value;
  }

  public int getProductId() {
    return productId;
  }

  public void setProductId(int value) {
    productId = value;
  }

  public String getProductKey() {
    return productKey;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String value) {
    status = value;
  }

  public String getCredentialDigest() {
    return credentialDigest;
  }

  public void setCredentialDigest(String value) {
    credentialDigest = value;
  }

  public long getTokenVersion() {
    return tokenVersion;
  }

  public void setTokenVersion(long value) {
    tokenVersion = value;
  }

  public Instant getLastSeenAt() {
    return lastSeenAt;
  }

  public void setLastSeenAt(Instant value) {
    lastSeenAt = value;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void touch() {
    updatedAt = Instant.now();
  }
}
