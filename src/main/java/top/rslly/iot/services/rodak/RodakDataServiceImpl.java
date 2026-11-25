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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.rslly.iot.dao.ProductRepository;
import top.rslly.iot.dao.RodakCommandRepository;
import top.rslly.iot.dao.RodakDeviceRepository;
import top.rslly.iot.dao.RodakShadowRepository;
import top.rslly.iot.models.RodakCommandEntity;
import top.rslly.iot.models.RodakDeviceEntity;
import top.rslly.iot.models.RodakShadowEntity;
import top.rslly.iot.services.rodak.RodakPairingService.Identity;
import top.rslly.iot.services.rodak.RodakPairingService.Rejection;

@Service
@Transactional
public class RodakDataServiceImpl implements RodakDataService {
  public static final int MAX_PAYLOAD = 65536;
  public static final int ONLINE_SECONDS = 180;
  private static final Map<String, String> PING = Map.of("command", "ping");
  private final RodakDeviceRepository devices;
  private final RodakShadowRepository shadows;
  private final RodakCommandRepository commands;
  private final ProductRepository products;
  private final ObjectMapper json;
  private final boolean enabled;

  public RodakDataServiceImpl(RodakDeviceRepository devices, RodakShadowRepository shadows,
      RodakCommandRepository commands, ProductRepository products, ObjectMapper json,
      @Value("${rodak.aiot.enabled:false}") boolean enabled) {
    this.devices = devices;
    this.shadows = shadows;
    this.commands = commands;
    this.products = products;
    this.json = json;
    this.enabled = enabled;
  }

  @Override
  public Map<String, Object> heartbeat(Identity identity) {
    Instant now = Instant.now();
    device(identity).setLastSeenAt(now);
    return Map.of("deviceId", identity.id(), "deviceKey", identity.deviceKey(), "status", "online",
        "timestamp", now.toEpochMilli());
  }

  @Override
  public void telemetry(Identity identity, JsonNode payload) {
    RodakDeviceEntity device = device(identity);
    Instant now = Instant.now();
    RodakShadowEntity row = shadow(device, true);
    row.setTelemetry(validObject(payload));
    row.setTelemetryAt(now);
    device.setLastSeenAt(now);
  }

  @Override
  public Shadow report(Identity identity, JsonNode payload) {
    RodakDeviceEntity device = device(identity);
    RodakShadowEntity row = shadow(device, true);
    String next = validObject(payload);
    if (!read(row.getReported()).equals(payload)) {
      row.setReported(next);
      row.setShadowVersion(Math.incrementExact(row.getShadowVersion()));
    }
    device.setLastSeenAt(Instant.now());
    return view(row);
  }

  @Override
  public Shadow desired(Identity identity) {
    return view(shadow(device(identity), false));
  }

  @Override
  public List<Command> pull(Identity identity, int limit) {
    RodakDeviceEntity device = device(identity);
    Instant now = Instant.now();
    return commands
        .findAllByDeviceIdAndBindingVersionAndAcknowledgementIsNullAndExpiresAtAfterOrderByCreatedAtAsc(
            device.getId(), identity.version(), now, PageRequest.of(0, Math.clamp(limit, 1, 100)))
        .stream().map(row -> view(row, now)).toList();
  }

  @Override
  public void acknowledge(Identity identity, String commandNo, JsonNode payload) {
    RodakDeviceEntity device = device(identity);
    RodakCommandEntity row = command(device, commandNo);
    if (row.getAcknowledgement() != null) {
      return;
    }
    Instant now = Instant.now();
    if (!now.isBefore(row.getExpiresAt())) {
      throw new Rejection(410, "command expired");
    }
    String encoded = validObject(payload);
    String status = payload.path("status").asText();
    if (!"error".equals(status) &&
        !("ok".equals(status) && payload.path("result").path("pong").isBoolean()
            && payload.path("result").path("pong").booleanValue())) {
      throw new Rejection(400, "invalid ping acknowledgement");
    }
    row.setAcknowledgement(encoded);
    device.setLastSeenAt(now);
  }

  @Override
  public Command ping(String id, int productId) {
    RodakDeviceEntity device = locked(id, productId, null);
    Instant now = Instant.now();
    if (commands.countByDeviceIdAndBindingVersionAndAcknowledgementIsNullAndExpiresAtAfter(
        device.getId(), device.getTokenVersion(), now) >= 100) {
      throw new Rejection(429, "too many pending commands");
    }
    var row = new RodakCommandEntity();
    row.setCommandNo(UUID.randomUUID().toString().replace("-", ""));
    row.setDeviceId(device.getId());
    row.setBindingVersion(device.getTokenVersion());
    row.setCreatedAt(now);
    row.setExpiresAt(now.plusSeconds(120));
    commands.save(row);
    return view(row, now);
  }

  @Override
  public Command command(String id, int productId, String commandNo) {
    return view(command(locked(id, productId, null), commandNo), Instant.now());
  }

  @Override
  public Shadow volume(String id, int productId, Integer volume) {
    if (volume == null || volume < 0 || volume > 100) {
      throw new Rejection(400, "volume must be an integer from 0 to 100");
    }
    RodakDeviceEntity device = locked(id, productId, null);
    RodakShadowEntity row = shadow(device, true);
    if (read(row.getDesired()).path("volume").asInt(-1) != volume) {
      row.setDesired(json.createObjectNode().put("volume", volume).toString());
      row.setShadowVersion(Math.incrementExact(row.getShadowVersion()));
    }
    return view(row);
  }

  @Override
  @Transactional(readOnly = true)
  public Map<String, Shadow> snapshots(List<RodakDeviceEntity> scoped) {
    if (!enabled || scoped.isEmpty()) {
      return Map.of();
    }
    var result = new HashMap<String, Shadow>();
    var devicesById = new HashMap<Long, RodakDeviceEntity>();
    scoped.forEach(device -> devicesById.put(device.getId(), device));
    for (var row : shadows.findAllByDeviceIdIn(devicesById.keySet())) {
      var device = devicesById.get(row.getDeviceId());
      if (device.getTokenVersion() == row.getBindingVersion()) {
        result.put(device.getDeviceUuid(), view(row));
      }
    }
    return result;
  }

  @Override
  @Transactional(readOnly = true)
  public Map<String, Object> thingModel(int productId) {
    requireEnabled();
    if (products.findAllById(productId).isEmpty()
        || devices.findAllByProductId(productId).stream()
            .noneMatch(device -> "ACTIVE".equals(device.getStatus()))) {
      throw new Rejection(404, "Rodak product not found");
    }
    return Map.of("productId", productId, "productKey", RodakPairingService.PRODUCT_KEY,
        "properties", List.of(property("firmware", "string"), property("wifi_rssi", "integer"),
            property("volume", "integer"), property("battery", "integer"),
            property("charging", "boolean"), property("free_heap", "integer"),
            property("minimum_free_heap_size", "integer"), property("uptime_ms", "integer"),
            property("ota_slot", "string")));
  }

  @Scheduled(fixedDelay = 3600000, initialDelay = 3600000)
  public void purgeCommands() {
    if (enabled) {
      commands.deleteByExpiresAtBefore(Instant.now().minusSeconds(86400));
    }
  }

  private RodakDeviceEntity device(Identity identity) {
    if (!Instant.now().isBefore(identity.expiresAt())) {
      throw new Rejection(401, "device token expired");
    }
    return locked(identity.id(), identity.productId(), identity.version());
  }

  private RodakDeviceEntity locked(String id, int productId, Long version) {
    requireEnabled();
    return devices.findLockedByDeviceUuid(id)
        .filter(row -> "ACTIVE".equals(row.getStatus()) && row.getProductId() == productId
            && (version == null || row.getTokenVersion() == version))
        .filter(row -> !products.findAllById(productId).isEmpty())
        .orElseThrow(() -> new Rejection(version == null ? 404 : 401, "device not bound"));
  }

  private void requireEnabled() {
    if (!enabled) {
      throw new Rejection(503, "Rodak AIoT data is unavailable");
    }
  }

  private RodakShadowEntity shadow(RodakDeviceEntity device, boolean create) {
    var row = shadows.findById(device.getId()).orElse(null);
    if (row != null && row.getBindingVersion() == device.getTokenVersion()) {
      return row;
    }
    if (!create) {
      return null;
    }
    if (row == null) {
      row = new RodakShadowEntity();
      row.setDeviceId(device.getId());
    }
    row.setBindingVersion(device.getTokenVersion());
    row.setDesired(null);
    row.setReported(null);
    row.setTelemetry(null);
    row.setTelemetryAt(null);
    return shadows.save(row);
  }

  private RodakCommandEntity command(RodakDeviceEntity device, String commandNo) {
    return commands.findByCommandNoAndDeviceIdAndBindingVersion(commandNo, device.getId(),
        device.getTokenVersion()).orElseThrow(() -> new Rejection(404, "command not found"));
  }

  private Shadow view(RodakShadowEntity row) {
    if (row == null) {
      return new Shadow(0, json.createObjectNode(), json.createObjectNode(), null, null);
    }
    return new Shadow(row.getShadowVersion(), read(row.getDesired()), read(row.getReported()),
        row.getTelemetry() == null ? null : read(row.getTelemetry()), row.getTelemetryAt());
  }

  private Command view(RodakCommandEntity row, Instant now) {
    JsonNode ack = row.getAcknowledgement() == null ? null : read(row.getAcknowledgement());
    String status = ack == null
        ? (now.isBefore(row.getExpiresAt()) ? "pending" : "expired")
        : ack.path("status").asText();
    return new Command(row.getCommandNo(), PING, row.getCreatedAt(), row.getExpiresAt(), status,
        ack);
  }

  private JsonNode read(String encoded) {
    if (encoded == null) {
      return json.createObjectNode();
    }
    try {
      return json.readTree(encoded);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("invalid persisted Rodak JSON", e);
    }
  }

  private static String validObject(JsonNode payload) {
    if (payload == null || !payload.isObject()) {
      throw new Rejection(400, "expected JSON object");
    }
    String encoded = payload.toString();
    if (encoded.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_PAYLOAD) {
      throw new Rejection(413, "request too large");
    }
    return encoded;
  }

  private static Map<String, String> property(String key, String type) {
    return Map.of("identifier", key, "dataType", type, "accessMode", "r");
  }
}
