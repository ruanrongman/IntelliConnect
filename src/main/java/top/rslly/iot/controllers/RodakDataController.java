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

import static top.rslly.iot.config.RodakDeviceSecurity.IDENTITY;
import static top.rslly.iot.services.rodak.RodakDescriptor.DEVICES;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.rslly.iot.controllers.RodakDeviceController.Envelope;
import top.rslly.iot.services.rodak.RodakDataService;
import top.rslly.iot.services.rodak.RodakDataServiceImpl;
import top.rslly.iot.services.rodak.RodakPairingService.Identity;
import top.rslly.iot.services.rodak.RodakPairingService.Rejection;

@RestController
public class RodakDataController {
  private final RodakDataService data;
  private final ObjectMapper json;

  public RodakDataController(RodakDataService data, ObjectMapper json) {
    this.data = data;
    this.json = json;
  }

  @PostMapping(DEVICES + "/heartbeat")
  public Envelope heartbeat(@RequestAttribute(IDENTITY) Identity identity) {
    return ok(data.heartbeat(identity));
  }

  @PostMapping(DEVICES + "/telemetry")
  public Envelope telemetry(@RequestAttribute(IDENTITY) Identity identity,
      HttpServletRequest request) throws IOException {
    data.telemetry(identity, body(request));
    return ok(true);
  }

  @PostMapping(DEVICES + "/shadow/report")
  public Envelope report(@RequestAttribute(IDENTITY) Identity identity,
      HttpServletRequest request) throws IOException {
    JsonNode report = body(request);
    return ok(data.report(identity, report != null && report.has("reported")
        ? report.get("reported")
        : report).wire(identity.deviceKey()));
  }

  @GetMapping(DEVICES + "/shadow/desired")
  public Envelope desired(@RequestAttribute(IDENTITY) Identity identity) {
    return ok(data.desired(identity).wire(identity.deviceKey()));
  }

  @GetMapping(DEVICES + "/commands/pull")
  public Envelope pull(@RequestAttribute(IDENTITY) Identity identity,
      @RequestParam(defaultValue = "20") int limit) {
    return ok(data.pull(identity, limit).stream().map(command -> Map.of(
        "id", command.commandNo(), "deviceId", identity.id(), "deviceKey", identity.deviceKey(),
        "commandNo", command.commandNo(), "topic",
        "devices/" + identity.deviceKey() + "/commands/" + command.commandNo(),
        "payload", json.valueToTree(command.payload()).toString(), "status", command.status(),
        "createdAt", command.createdAt().toString())).toList());
  }

  @PostMapping(DEVICES + "/commands/{commandNo:[0-9a-f]{32}}/ack")
  public Envelope acknowledge(@RequestAttribute(IDENTITY) Identity identity,
      @PathVariable String commandNo, HttpServletRequest request) throws IOException {
    data.acknowledge(identity, commandNo, body(request));
    return ok(true);
  }

  @GetMapping("/api/v1/aiot/products/{productId:[0-9]+}/thing-model")
  public Envelope thingModel(@PathVariable int productId) {
    return ok(data.thingModel(productId));
  }

  @GetMapping("/api/v1/ota/check")
  public Envelope otaCheck(@RequestAttribute(IDENTITY) Identity identity) {
    return ok(Map.of("upgradeAvailable", false));
  }

  @ExceptionHandler(Rejection.class)
  public ResponseEntity<Envelope> rejected(Rejection rejection) {
    return ResponseEntity.status(rejection.code() == 401 ? 401 : 200)
        .body(new Envelope(rejection.code(), rejection.getMessage(), null));
  }

  private JsonNode body(HttpServletRequest request) throws IOException {
    int limit = RodakDataServiceImpl.MAX_PAYLOAD;
    byte[] bytes = request.getInputStream().readNBytes(limit + 1);
    if (bytes.length > limit) {
      throw new Rejection(413, "request too large");
    }
    try {
      return json.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(bytes);
    } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
      throw new Rejection(400, "invalid JSON request");
    }
  }

  private static Envelope ok(Object value) {
    return new Envelope(200, "ok", value);
  }

}
