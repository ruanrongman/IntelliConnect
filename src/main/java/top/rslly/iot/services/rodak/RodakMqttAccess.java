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

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.rslly.iot.services.rodak.RodakPairingService.Identity;

@Component
public class RodakMqttAccess {
  public static final String BRIDGE = "rodak-service";
  public static final String EVENTS = "_rodak/events/";
  private final RodakPairingService pairing;
  private final boolean enabled;
  private final String callbackKey;
  private final String password;

  public RodakMqttAccess(RodakPairingService pairing,
      @Value("${rodak.mqtt.enabled:false}") boolean enabled,
      @Value("${rodak.mqtt.callback-key:}") String callbackKey,
      @Value("${rodak.mqtt.service-password:}") String password) {
    this.pairing = pairing;
    this.enabled = enabled;
    this.callbackKey = callbackKey;
    this.password = password;
    if (enabled && (callbackKey.length() < 24 || password.length() < 24
        || equal(callbackKey, password))) {
      throw new IllegalArgumentException(
          "Rodak MQTT requires separate callback and service secrets");
    }
  }

  public boolean broker(String header) {
    return enabled && equal(callbackKey, header);
  }

  public Map<String, Object> authenticate(JsonNode body) {
    String username = text(body, "username");
    String client = text(body, "clientid");
    String token = text(body, "password");
    if (BRIDGE.equals(username)) {
      return BRIDGE.equals(client) && equal(password, token)
          ? Map.of("result", "allow", "is_superuser", false,
              "client_attrs", Map.of("rodak_bridge", "true"))
          : deny();
    }
    if (!BRIDGE.equals(client) && !client.startsWith("rodakos_")) {
      return Map.of("result", "ignore");
    }
    // Stock BigSmart identifies itself with its MAC. The firmware client ID removes separators.
    if (!username.matches("(?:[0-9a-f]{2}:){5}[0-9a-f]{2}")) {
      return deny();
    }
    try {
      Identity identity = pairing.authenticate(token);
      if (!identity.deviceKey().equals(username) || !clientId(username).equals(client)) {
        return deny();
      }
      return Map.of("result", "allow", "is_superuser", false,
          "expire_at", identity.expiresAt().getEpochSecond(), "client_attrs", Map.of(
              "rodak_id", identity.id(), "rodak_version", Long.toString(identity.version()),
              "rodak_product", Integer.toString(identity.productId()),
              "rodak_exp", Long.toString(identity.expiresAt().getEpochSecond()),
              "rodak_session", UUID.randomUUID().toString()));
    } catch (SecurityException ex) {
      return deny();
    }
  }

  static Identity identity(String user, String client, JsonNode attrs) {
    if (user.isEmpty() || !clientId(user).equals(client)
        || text(attrs, "rodak_session").isEmpty()) {
      return null;
    }
    try {
      String id = UUID.fromString(text(attrs, "rodak_id")).toString();
      int product = Integer.parseInt(text(attrs, "rodak_product"));
      long version = Long.parseLong(text(attrs, "rodak_version"));
      Instant expiry = Instant.ofEpochSecond(Long.parseLong(text(attrs, "rodak_exp")));
      return new Identity(id, user, product, version, expiry);
    } catch (IllegalArgumentException | java.time.DateTimeException ex) {
      return null;
    }
  }

  static boolean allowsDevicePublish(String key, String topic) {
    String prefix = "devices/" + key + "/";
    if (!topic.startsWith(prefix)) {
      return false;
    }
    String suffix = topic.substring(prefix.length());
    return suffix.equals("telemetry") || suffix.equals("shadow/report")
        || suffix.matches("commands/[0-9a-f]{32}/ack");
  }

  static String clientId(String key) {
    String suffix = key.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    return "rodakos_" + suffix.substring(Math.max(0, suffix.length() - 20));
  }

  private static Map<String, Object> deny() {
    return Map.of("result", "deny");
  }

  private static boolean equal(String expected, String supplied) {
    return supplied != null && supplied.length() <= 255
        && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
            supplied.getBytes(StandardCharsets.UTF_8));
  }

  static String text(JsonNode node, String key) {
    return node.path(key).asText("");
  }
}
