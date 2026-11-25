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
package top.rslly.iot.controllers;

import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import top.rslly.iot.services.SafetyService;
import top.rslly.iot.param.request.RodakVolume;
import top.rslly.iot.services.rodak.RodakDataService;
import top.rslly.iot.services.rodak.RodakDataService.Shadow;
import top.rslly.iot.services.rodak.RodakDataServiceImpl;
import top.rslly.iot.services.rodak.RodakMqttBridge;
import top.rslly.iot.services.rodak.RodakPairingService;
import top.rslly.iot.services.rodak.RodakPairingService.Rejection;
import top.rslly.iot.utility.smartVoice.rodak.RodakVoiceRuntime;

@RestController
@RequestMapping("/api/v2/rodak")
public class RodakAdminController {
  private final RodakPairingService service;
  private final SafetyService safety;
  private final RodakVoiceRuntime voice;
  private final RodakDataService data;
  private final ObjectProvider<RodakMqttBridge> mqtt;

  public RodakAdminController(RodakPairingService service, SafetyService safety,
      RodakVoiceRuntime voice, RodakDataService data, ObjectProvider<RodakMqttBridge> mqtt) {
    this.service = service;
    this.safety = safety;
    this.voice = voice;
    this.data = data;
    this.mqtt = mqtt;
  }

  @GetMapping("/devices")
  public Map<String, Object> devices(@RequestHeader("Authorization") String token,
      @RequestParam int productId) {
    authorize(token, productId);
    var devices = service.devicesForProduct(productId);
    var snapshots = data.snapshots(devices);
    var bridge = mqtt.getIfAvailable();
    Instant onlineAfter = Instant.now().minusSeconds(RodakDataServiceImpl.ONLINE_SECONDS);
    return ok(devices.stream().map(device -> new DeviceView(
        device.getDeviceUuid(), device.getDeviceKey(), device.getProductId(), device.getStatus(),
        device.getTokenVersion(), device.getLastSeenAt(),
        voice.isConnected(device.getDeviceKey()),
        bridge != null && bridge.isOnline(device.getDeviceKey(), device.getTokenVersion()),
        "ACTIVE".equals(device.getStatus()) && device.getLastSeenAt() != null
            && device.getLastSeenAt().isAfter(onlineAfter),
        snapshots.get(device.getDeviceUuid()))).toList());
  }

  @PostMapping("/devices/{id}/ping")
  public Map<String, Object> ping(@RequestHeader("Authorization") String token,
      @PathVariable String id, @RequestParam int productId) {
    authorize(token, productId);
    return ok(data.ping(id, productId));
  }

  @GetMapping("/devices/{id}/commands/{commandNo:[0-9a-f]{32}}")
  public Map<String, Object> command(@RequestHeader("Authorization") String token,
      @PathVariable String id, @PathVariable String commandNo, @RequestParam int productId) {
    authorize(token, productId);
    return ok(data.command(id, productId, commandNo));
  }

  @PutMapping("/devices/{id}/volume")
  public Map<String, Object> volume(@RequestHeader("Authorization") String token,
      @PathVariable String id, @RequestParam int productId, @RequestBody RodakVolume volume) {
    authorize(token, productId);
    return ok(data.volume(id, productId, volume.volume()));
  }

  @GetMapping("/pairing-requests")
  @PreAuthorize("hasAuthority('ROLE_admin')")
  public Map<String, Object> pairingRequests() {
    return ok(service.pendingRequests());
  }

  @PostMapping("/pairing-requests/confirm")
  public Map<String, Object> confirm(@RequestHeader("Authorization") String token,
      @RequestBody top.rslly.iot.param.request.RodakClaim claim,
      jakarta.servlet.http.HttpServletRequest request,
      java.security.Principal operator) {
    authorize(token, claim.productId());
    service.checkConfirmationRate(operator.getName(), request.getRemoteAddr(), claim.productId());
    return ok(Map.of("deviceId",
        service.confirm(claim.requestId(), claim.pairingCode(), claim.productId())));
  }

  @PostMapping("/devices/{id}/unbind")
  public Map<String, Object> unbind(@RequestHeader("Authorization") String token,
      @PathVariable String id, @RequestParam int productId) {
    authorize(token, productId);
    return ok(Map.of("status", "unbound", "deviceId", service.unbind(id, productId, null)));
  }

  private void authorize(String token, int productId) {
    if (!safety.controlAuthorizeProduct(token, productId)) {
      throw new Rejection(403, "forbidden");
    }
  }

  @ExceptionHandler(Rejection.class)
  public Map<String, Object> rejected(Rejection rejection) {
    return Map.of("success", false, "errorCode", rejection.code(), "errorMsg",
        rejection.getMessage());
  }

  private static Map<String, Object> ok(Object data) {
    return Map.of("success", true, "errorCode", 200, "data", data);
  }

  public record DeviceView(String id, String deviceKey, int productId, String status,
      long tokenVersion, Instant lastSeenAt, boolean voiceConnected, boolean mqttConnected,
      boolean recentlySeen, Shadow shadow) {}
}
