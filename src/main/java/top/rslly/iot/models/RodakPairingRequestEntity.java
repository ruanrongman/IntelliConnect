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

@Entity
@Table(name = "rodak_pairing_request", indexes = {
    @Index(name = "idx_rodak_pairing_device", columnList = "device_key"),
    @Index(name = "idx_rodak_pairing_expires", columnList = "expires_at"),
    @Index(name = "idx_rodak_pairing_status", columnList = "status")
})
public class RodakPairingRequestEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "request_id", nullable = false, unique = true, length = 32)
  private String requestId;
  @Column(name = "device_key", nullable = false, length = 128)
  private String deviceKey;
  @Column(name = "credential_digest", nullable = false, length = 128)
  private String credentialDigest;
  @Column(name = "pairing_code_digest", nullable = false, length = 128)
  private String pairingCodeDigest;
  @Column(name = "request_token_digest", nullable = false, length = 128)
  private String requestTokenDigest;
  @Column(name = "product_id")
  private Integer productId;
  @Column(name = "source_ip", length = 64)
  private String sourceIp;
  @Column(name = "status", nullable = false, length = 32)
  private String status = "PENDING";
  @Column(name = "failed_attempts", nullable = false)
  private int failedAttempts;
  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;
  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  public Long getId() {
    return id;
  }

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String value) {
    requestId = value;
  }

  public String getDeviceKey() {
    return deviceKey;
  }

  public void setDeviceKey(String value) {
    deviceKey = value;
  }

  public String getCredentialDigest() {
    return credentialDigest;
  }

  public void setCredentialDigest(String value) {
    credentialDigest = value;
  }

  public String getPairingCodeDigest() {
    return pairingCodeDigest;
  }

  public void setPairingCodeDigest(String value) {
    pairingCodeDigest = value;
  }

  public String getRequestTokenDigest() {
    return requestTokenDigest;
  }

  public void setRequestTokenDigest(String value) {
    requestTokenDigest = value;
  }

  public Integer getProductId() {
    return productId;
  }

  public void setProductId(Integer value) {
    productId = value;
  }

  public String getSourceIp() {
    return sourceIp;
  }

  public void setSourceIp(String value) {
    sourceIp = value;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String value) {
    status = value;
  }

  public int getFailedAttempts() {
    return failedAttempts;
  }

  public void incrementFailedAttempts() {
    failedAttempts++;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant value) {
    expiresAt = value;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
