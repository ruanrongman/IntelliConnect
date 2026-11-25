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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import top.rslly.iot.services.rodak.RodakPairingService.Identity;

class RodakMqttAccessTest {
  private static final String KEY = "44:1b:f6:c3:b4:30";
  private static final String CLIENT = "rodakos_441bf6c3b430";
  private static final String CALLBACK = "callback-key-for-testing-only";
  private static final String PASSWORD = "service-password-for-testing-only";
  private final ObjectMapper json = new ObjectMapper();
  private final RodakPairingService pairing = mock(RodakPairingService.class);
  private final RodakMqttAccess access =
      new RodakMqttAccess(pairing, true, CALLBACK, PASSWORD);

  @Test
  void sharedBrokerDefersLegacyClientsButReservesRodakIdentities() {
    var login = json.createObjectNode().put("username", "legacy").put("clientid", "sensor-01");
    assertEquals("ignore", access.authenticate(login).get("result"));
    assertEquals("deny", access.authenticate(login.put("clientid", CLIENT)).get("result"));
    assertEquals("deny", access.authenticate(login.put("clientid", "rodak-service")).get("result"));
    assertEquals("deny", access.authenticate(login.put("username", "rodak-service")
        .put("clientid", "sensor-01")).get("result"));
    verifyNoInteractions(pairing);
  }

  @Test
  void authenticatesOnlyMatchingFirmwareIdentityAndBindsTheAclToItsVersion() {
    Instant expiry = Instant.ofEpochSecond(Instant.now().getEpochSecond() + 300);
    var identity = new Identity("942783dd-b0fa-407f-b937-77311ae757b2", KEY, 42, 7, expiry);
    when(pairing.authenticate("token")).thenReturn(identity);
    var connect = json.createObjectNode().put("username", KEY).put("clientid", CLIENT)
        .put("password", "token");
    var result = access.authenticate(connect);
    assertEquals("allow", result.get("result"));
    assertEquals(expiry.getEpochSecond(), result.get("expire_at"));
    var attrs = json.valueToTree(result.get("client_attrs"));
    assertEquals(identity, RodakMqttAccess.identity(KEY, CLIENT, attrs));
    assertNull(RodakMqttAccess.identity(KEY, "other", attrs));
    assertEquals("ignore", access.authenticate(connect.put("clientid", "other")).get("result"));
    assertEquals("deny", access.authenticate(connect.put("clientid", CLIENT + "0")).get("result"));
    assertEquals("deny", access
        .authenticate(connect.put("clientid", CLIENT).put("username", "other")).get("result"));
  }

  @Test
  void authenticatesTheBridgeAndFiltersDevicePublications() {
    assertTrue(access.broker(CALLBACK));
    assertFalse(access.broker(PASSWORD));
    String topic = "devices/" + KEY + "/";
    assertTrue(RodakMqttAccess.allowsDevicePublish(KEY, topic + "telemetry"));
    assertTrue(RodakMqttAccess.allowsDevicePublish(KEY, topic + "shadow/report"));
    assertFalse(RodakMqttAccess.allowsDevicePublish(KEY, topic + "shadow/desired"));
    assertFalse(RodakMqttAccess.allowsDevicePublish(KEY, "devices/other/telemetry"));
    var bridge = json.createObjectNode().put("username", "rodak-service")
        .put("clientid", "rodak-service").put("password", PASSWORD);
    var authenticated = access.authenticate(bridge);
    assertEquals("allow", authenticated.get("result"));
    assertEquals("true", json.valueToTree(authenticated.get("client_attrs"))
        .path("rodak_bridge").asText());
    assertEquals("deny", access.authenticate(bridge.put("password", CALLBACK)).get("result"));
  }
}
