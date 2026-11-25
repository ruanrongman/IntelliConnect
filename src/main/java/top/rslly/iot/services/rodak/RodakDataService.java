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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import top.rslly.iot.models.RodakDeviceEntity;
import top.rslly.iot.services.rodak.RodakPairingService.Identity;

public interface RodakDataService {
  Map<String, Object> heartbeat(Identity identity);

  void telemetry(Identity identity, JsonNode payload);

  Shadow report(Identity identity, JsonNode payload);

  Shadow desired(Identity identity);

  List<Command> pull(Identity identity, int limit);

  void acknowledge(Identity identity, String commandNo, JsonNode payload);

  Command ping(String id, int productId);

  Command command(String id, int productId, String commandNo);

  Shadow volume(String id, int productId, Integer volume);

  Map<String, Shadow> snapshots(List<RodakDeviceEntity> devices);

  Map<String, Object> thingModel(int productId);

  record Shadow(long shadowVersion, JsonNode desired, JsonNode reported, JsonNode telemetry,
      Instant telemetryAt) {
    public Map<String, Object> wire(String deviceKey) {
      return new java.util.HashMap<>(Map.of("deviceKey", deviceKey, "shadowVersion", shadowVersion,
          "version", shadowVersion, "desired", desired, "reported", reported,
          "state", Map.of("desired", desired, "reported", reported)));
    }
  }

  record Command(String commandNo, Map<String, String> payload, Instant createdAt,
      Instant expiresAt, String status, JsonNode acknowledgement) {}
}
