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
package top.rslly.iot.services.rodak;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RodakTokenService {
  private final SecretKey key;
  private final SecretKey digestKey;
  private final long ttlSeconds;

  public RodakTokenService(
      @Value("${rodak.security.secret:${jwt.secret:}}") String secret,
      @Value("${rodak.security.token-ttl-seconds:600}") long ttlSeconds) {
    if (secret.isBlank()) {
      key = null;
      digestKey = null;
    } else {
      SecretKey root = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
      key = Keys.hmacShaKeyFor(mac(root, "rodak-device-jwt"));
      digestKey = new SecretKeySpec(mac(root, "rodak-credential-digest"), "HmacSHA256");
    }
    this.ttlSeconds = ttlSeconds;
  }

  public String issue(String deviceKey, long tokenVersion, Instant expiresAt) {
    requireConfigured();
    String token = Jwts.builder().claim("dk", deviceKey).claim("tv", tokenVersion)
        .expiration(Date.from(expiresAt)).signWith(key, Jwts.SIG.HS256).compact();
    if (token.length() > 255) {
      throw new IllegalArgumentException("device key exceeds firmware token budget");
    }
    return token;
  }

  public Claims parse(String token) {
    requireConfigured();
    if (token == null || token.length() > 255) {
      throw new SecurityException("invalid device token");
    }
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }

  public String digest(String purpose, String value) {
    requireConfigured();
    return HexFormat.of().formatHex(mac(digestKey, purpose + "\n" + value));
  }

  public boolean matches(String digest, String purpose, String value) {
    return digest != null && value != null && MessageDigest.isEqual(
        digest.getBytes(StandardCharsets.US_ASCII),
        digest(purpose, value).getBytes(StandardCharsets.US_ASCII));
  }

  private void requireConfigured() {
    if (!configured()) {
      throw new RodakPairingService.Rejection(503, "Rodak credentials are not configured");
    }
  }

  public boolean configured() {
    return key != null;
  }

  private static byte[] mac(SecretKey key, String value) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(key);
      return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Cannot initialize Rodak credentials", e);
    }
  }

  public long ttlSeconds() {
    return ttlSeconds;
  }
}
