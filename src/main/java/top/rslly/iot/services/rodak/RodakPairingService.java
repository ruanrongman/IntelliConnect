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

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import top.rslly.iot.dao.ProductRepository;
import top.rslly.iot.dao.RodakDeviceRepository;
import top.rslly.iot.dao.RodakPairingRequestRepository;
import top.rslly.iot.models.RodakDeviceEntity;
import top.rslly.iot.models.RodakPairingRequestEntity;

@Service
public class RodakPairingService {
  static final String PRODUCT_KEY = "rymcu-bigsmart";
  private static final String PENDING = "PENDING";
  private static final String CONFIRMED = "CONFIRMED";
  private static final String REVOKED = "REVOKED";
  private static final String ACTIVE = "ACTIVE";
  private static final SecureRandom RANDOM = new SecureRandom();
  private final RodakDeviceRepository devices;
  private final RodakPairingRequestRepository requests;
  private final ProductRepository products;
  private final RodakTokenService tokens;
  private final RodakDescriptor descriptor;
  private final ApplicationEventPublisher events;
  private final TransactionTemplate transactions;
  private final Map<String, Integer> confirmationAttempts = new LinkedHashMap<>();
  private long confirmationMinute = -1;

  public RodakPairingService(RodakDeviceRepository devices, RodakPairingRequestRepository requests,
      ProductRepository products, RodakTokenService tokens, RodakDescriptor descriptor,
      ApplicationEventPublisher events, PlatformTransactionManager transactionManager) {
    this.devices = devices;
    this.requests = requests;
    this.products = products;
    this.tokens = tokens;
    this.descriptor = descriptor;
    this.events = events;
    this.transactions = new TransactionTemplate(transactionManager);
  }

  public static String normalizeDeviceKey(String value) {
    if (value == null) {
      throw new Rejection(400, "device key is required");
    }
    String key = value.trim().toLowerCase(Locale.ROOT);
    if (!key.matches("[a-z0-9:_.-]{1,96}")) {
      throw new Rejection(400, "invalid device key");
    }
    return key;
  }

  public Map<String, Object> createRequest(String deviceKey, String secret, String source) {
    return mutate(() -> {
      Instant now = Instant.now();
      if (requests.countBySourceIpAndCreatedAtAfter(source, now.minusSeconds(60)) >= 20
          || requests.countByStatusAndExpiresAtAfter(PENDING, now) >= 256
          || requests.countByDeviceKeyAndStatusAndExpiresAtAfter(deviceKey, PENDING, now) >= 3) {
        throw new Rejection(429, "pairing limit reached");
      }
      var existing = devices.findByDeviceKey(deviceKey).filter(this::active).orElse(null);
      if (existing != null && !tokens.matches(existing.getCredentialDigest(), "device", secret)) {
        throw new Rejection(409, "device already bound");
      }
      tokens.issue(deviceKey, Long.MAX_VALUE, now.plusSeconds(tokens.ttlSeconds()));
      String code;
      String codeDigest;
      do {
        code = String.format(Locale.ROOT, "%08d", RANDOM.nextInt(100_000_000));
        codeDigest = tokens.digest("pairing", code);
      } while (requests.findByPairingCodeDigestAndStatusAndExpiresAtAfter(
          codeDigest, PENDING, now).isPresent());

      byte[] random = new byte[32];
      RANDOM.nextBytes(random);
      String requestToken = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
      RodakPairingRequestEntity request = new RodakPairingRequestEntity();
      request.setRequestId(UUID.randomUUID().toString().replace("-", ""));
      request.setDeviceKey(deviceKey);
      request.setCredentialDigest(existing == null ? tokens.digest("device", secret)
          : existing.getCredentialDigest());
      request.setPairingCodeDigest(codeDigest);
      request.setRequestTokenDigest(tokens.digest("request", requestToken));
      request.setSourceIp(source);
      request.setExpiresAt(now.plusSeconds(600));
      if (existing != null) {
        request.setProductId(existing.getProductId());
        request.setStatus(CONFIRMED);
      }
      requests.save(request);
      // Stock firmware reads credentials from the status poll, so creation still returns pending.
      return Map.of("requestId", request.getRequestId(), "pairingCode", code,
          "requestToken", requestToken, "status", "pending",
          "expiresAt", request.getExpiresAt().toString(), "pollIntervalSeconds", 2);
    });
  }

  public String confirm(String requestId, String code, int productId) {
    if (code == null || !code.matches("[0-9]{8}")) {
      throw new Rejection(400, "invalid or expired pairing code");
    }
    String deviceId = mutate(() -> {
      Instant now = Instant.now();
      var match = requestId == null
          ? requests.findByPairingCodeDigestAndStatusAndExpiresAtAfter(
              tokens.digest("pairing", code), PENDING, now)
          : requests.findLockedByRequestId(requestId);
      if (match.isEmpty() || !pending(match.get(), now)) {
        return null;
      }
      var request = match.get();
      if (!tokens.matches(request.getPairingCodeDigest(), "pairing", code)) {
        request.incrementFailedAttempts();
        if (request.getFailedAttempts() >= 5) {
          request.setStatus(REVOKED);
        }
        return null;
      }
      if (products.findLockedById(productId).isEmpty()) {
        throw new Rejection(404, "product not found");
      }
      var device = devices.findLockedByDeviceKey(request.getDeviceKey())
          .orElseGet(RodakDeviceEntity::new);
      if (active(device)) {
        throw new Rejection(409, "device already bound");
      }
      device.setDeviceKey(request.getDeviceKey());
      device.setProductId(productId);
      device.setCredentialDigest(request.getCredentialDigest());
      device.setStatus(ACTIVE);
      device.touch();
      devices.save(device);
      revokeRequests(device.getDeviceKey(), request.getRequestId());
      request.setProductId(productId);
      request.setStatus(CONFIRMED);
      return device.getDeviceUuid();
    });
    if (deviceId == null) {
      throw new Rejection(400, "invalid or expired pairing code");
    }
    return deviceId;
  }

  public synchronized void checkConfirmationRate(String operator, String source, int productId) {
    long minute = Instant.now().getEpochSecond() / 60;
    if (minute != confirmationMinute) {
      confirmationAttempts.clear();
      confirmationMinute = minute;
    }
    var keys = List.of("account:" + operator, "source:" + source, "product:" + productId);
    if (confirmationAttempts.size() >= 4096
        || keys.stream().anyMatch(key -> confirmationAttempts.getOrDefault(key, 0) >= 20)) {
      throw new Rejection(429, "too many confirmation attempts");
    }
    keys.forEach(key -> confirmationAttempts.merge(key, 1, Integer::sum));
  }

  public Map<String, Object> status(String id, String requestToken, String origin) {
    return mutate(() -> {
      var request = requests.findByRequestId(id).orElseThrow(
          () -> new Rejection(404, "pairing request not found"));
      if (!tokens.matches(request.getRequestTokenDigest(), "request", requestToken)) {
        throw new Rejection(401, "invalid request token");
      }
      String status = request.getExpiresAt().isAfter(Instant.now())
          ? request.getStatus().toLowerCase(Locale.ROOT)
          : "expired";
      var data = new LinkedHashMap<>(pairingSummary(request));
      data.put("status", status);
      if ("confirmed".equals(status)) {
        var device = devices.findByDeviceKey(request.getDeviceKey())
            .filter(this::active).orElseThrow(
                () -> new Rejection(403, "device no longer bound"));
        if (device.getProductId() != request.getProductId()
            || !device.getCredentialDigest().equals(request.getCredentialDigest())) {
          throw new Rejection(403, "device no longer bound");
        }
        data.putAll(tokenResponse(device, origin));
      }
      return data;
    });
  }

  public Map<String, Object> refresh(String deviceKey, String secret, String origin) {
    return mutate(() -> {
      var device = devices.findByDeviceKey(deviceKey)
          .orElseThrow(() -> new Rejection(404, "device not found"));
      if (!active(device) || !tokens.matches(device.getCredentialDigest(), "device", secret)) {
        throw new Rejection(403, "device not authorized");
      }
      return tokenResponse(device, origin);
    });
  }

  public Identity authenticate(String bearer) {
    try {
      var claims = tokens.parse(bearer);
      Object version = claims.get("tv");
      if (!(version instanceof Number number) || claims.getExpiration() == null) {
        throw new SecurityException("invalid device token");
      }
      String key = normalizeDeviceKey(claims.get("dk", String.class));
      var device = devices.findByDeviceKey(key).filter(this::active)
          .filter(row -> row.getTokenVersion() == number.longValue())
          .orElseThrow(() -> new SecurityException("device token revoked"));
      return new Identity(device.getDeviceUuid(), key, device.getProductId(),
          device.getTokenVersion(),
          claims.getExpiration().toInstant());
    } catch (io.jsonwebtoken.JwtException | IllegalArgumentException | Rejection e) {
      throw new SecurityException("invalid device token", e);
    }
  }

  public boolean isCurrent(Identity identity) {
    return Instant.now().isBefore(identity.expiresAt()) && devices.findByDeviceUuid(identity.id())
        .filter(this::active)
        .filter(row -> row.getTokenVersion() == identity.version()
            && row.getProductId() == identity.productId())
        .isPresent();
  }

  public void seen(Identity identity) {
    mutate(() -> {
      devices.findLockedByDeviceUuid(identity.id())
          .filter(row -> row.getTokenVersion() == identity.version() && active(row))
          .ifPresent(row -> row.setLastSeenAt(Instant.now()));
      return null;
    });
  }

  public String unbind(String id, int productId, Long version) {
    var revoked = mutate(() -> {
      var device = devices.findLockedByDeviceUuid(id)
          .filter(row -> active(row) && row.getProductId() == productId
              && (version == null || row.getTokenVersion() == version))
          .orElseThrow(() -> new Rejection(version == null ? 404 : 401, "device not bound"));
      long oldVersion = device.getTokenVersion();
      device.setStatus(REVOKED);
      device.setTokenVersion(Math.incrementExact(oldVersion));
      device.touch();
      revokeRequests(device.getDeviceKey(), null);
      return new Revoked(device.getDeviceUuid(), device.getDeviceKey(), oldVersion);
    });
    events.publishEvent(revoked);
    return revoked.id();
  }

  public List<RodakDeviceEntity> devicesForProduct(int productId) {
    return devices.findAllByProductId(productId);
  }

  public List<Map<String, Object>> pendingRequests() {
    return requests.findAllByStatusAndExpiresAtAfterOrderByCreatedAtDesc(PENDING, Instant.now())
        .stream().map(RodakPairingService::pairingSummary).toList();
  }

  @Scheduled(fixedDelay = 3600000, initialDelay = 3600000)
  public void purgeRequests() {
    if (tokens.configured()) {
      mutate(() -> {
        requests.deleteAllByExpiresAtBefore(Instant.now().minusSeconds(3600));
        return null;
      });
    }
  }

  private void revokeRequests(String key, String retainedId) {
    for (var request : requests.findAllByDeviceKeyAndStatusIn(key, List.of(PENDING, CONFIRMED))) {
      if (!request.getRequestId().equals(retainedId)) {
        request.setStatus(REVOKED);
      }
    }
  }

  private boolean active(RodakDeviceEntity device) {
    return ACTIVE.equals(device.getStatus())
        && !products.findAllById(device.getProductId()).isEmpty();
  }

  private static boolean pending(RodakPairingRequestEntity request, Instant now) {
    return PENDING.equals(request.getStatus()) && request.getExpiresAt().isAfter(now);
  }

  private static Map<String, Object> pairingSummary(RodakPairingRequestEntity request) {
    return Map.of("requestId", request.getRequestId(), "deviceKey", request.getDeviceKey(),
        "deviceName", "RodakOS RYMCU BigSmart", "productKey", PRODUCT_KEY,
        "requestedAt", request.getCreatedAt().toString(), "expiresAt",
        request.getExpiresAt().toString(), "status", "pending");
  }

  private Map<String, Object> tokenResponse(RodakDeviceEntity device, String origin) {
    Instant expiresAt = Instant.ofEpochSecond(Instant.now().getEpochSecond() + tokens.ttlSeconds());
    String token = tokens.issue(device.getDeviceKey(), device.getTokenVersion(), expiresAt);
    return descriptor.credentials(device, token, tokens.ttlSeconds(), expiresAt.getEpochSecond(),
        origin);
  }

  // Serialize the request quotas and claims through commit on the USB deployment.
  private synchronized <T> T mutate(Supplier<T> action) {
    return transactions.execute(status -> action.get());
  }

  public record Identity(String id, String deviceKey, int productId, long version,
      Instant expiresAt) {}

  public record Revoked(String id, String key, long version) {}

  public static final class Rejection extends RuntimeException {
    private final int code;

    public Rejection(int code, String message) {
      super(message);
      this.code = code;
    }

    public int code() {
      return code;
    }
  }
}
