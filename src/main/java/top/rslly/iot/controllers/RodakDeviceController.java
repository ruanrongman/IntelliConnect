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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import top.rslly.iot.config.RodakDeviceSecurity;
import top.rslly.iot.services.rodak.RodakDescriptor;
import top.rslly.iot.services.rodak.RodakPairingService;
import top.rslly.iot.services.rodak.RodakPairingService.Identity;
import top.rslly.iot.services.rodak.RodakPairingService.Rejection;

@RestController
@RequestMapping(RodakDescriptor.DEVICES)
public class RodakDeviceController {
  private final RodakPairingService service;
  private final RodakDescriptor descriptor;
  private final ObjectMapper json;

  public RodakDeviceController(RodakPairingService service, RodakDescriptor descriptor,
      ObjectMapper json) {
    this.service = service;
    this.descriptor = descriptor;
    this.json = json;
  }

  @GetMapping("/bootstrap")
  public Envelope bootstrap(HttpServletRequest request) {
    return ok(descriptor.bootstrap(origin(request)));
  }

  @PostMapping("/binding/request")
  public Envelope bindingRequest(HttpServletRequest request) throws IOException {
    JsonNode body = body(request);
    if (body.has("trustSetup")) {
      throw new Rejection(400, "LAN trust setup is unavailable");
    }
    return ok(service.createRequest(deviceKey(body), secret(body), request.getRemoteAddr()));
  }

  @GetMapping("/binding/requests/{requestId:[0-9a-f]{32}}/status")
  public Envelope status(@PathVariable String requestId,
      @RequestHeader(value = "Authorization", required = false) String authorization,
      HttpServletRequest request) {
    String token = RodakDeviceSecurity.bearer(authorization);
    if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) {
      throw new Rejection(401, "invalid request token");
    }
    return ok(service.status(requestId, token, origin(request)));
  }

  @PostMapping("/auth/token")
  public Envelope token(HttpServletRequest request) throws IOException {
    JsonNode body = body(request);
    return ok(service.refresh(deviceKey(body), secret(body), origin(request)));
  }

  @PostMapping("/binding/unbind")
  public org.springframework.http.ResponseEntity<Envelope> unbind(
      @RequestAttribute(RodakDeviceSecurity.IDENTITY) Identity identity) {
    try {
      String id = service.unbind(identity.id(), identity.productId(), identity.version());
      return org.springframework.http.ResponseEntity
          .ok(ok(Map.of("status", "unbound", "deviceId", id)));
    } catch (Rejection rejection) {
      return org.springframework.http.ResponseEntity.status(rejection.code())
          .body(rejected(rejection));
    }
  }

  @PostMapping({"/register", "/activate"})
  public Envelope legacyRegistration() {
    return new Envelope(403, "manual pairing is required", null);
  }

  @ExceptionHandler(Rejection.class)
  public Envelope rejected(Rejection rejection) {
    return new Envelope(rejection.code(), rejection.getMessage(), null);
  }

  private JsonNode body(HttpServletRequest request) throws IOException {
    byte[] bytes = request.getInputStream().readNBytes(65537);
    if (bytes.length > 65536) {
      throw new Rejection(413, "request too large");
    }
    JsonNode body;
    try {
      body = json.readTree(bytes);
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new Rejection(400, "invalid JSON request");
    }
    if (body == null || !body.isObject()) {
      throw new Rejection(400, "invalid JSON request");
    }
    String product = aliases(body, false, "productKey", "product_key", "credentialKey");
    if (product != null && !"rymcu-bigsmart".equals(product)) {
      throw new Rejection(403, "unsupported product key");
    }
    return body;
  }

  private static String deviceKey(JsonNode body) {
    return RodakPairingService.normalizeDeviceKey(
        aliases(body, true, "deviceKey", "deviceCode", "device_key"));
  }

  private static String secret(JsonNode body) {
    String secret = aliases(body, false, "deviceSecret", "credentialSecret", "device_secret");
    if (secret == null || secret.isBlank() || secret.length() > 512) {
      throw new Rejection(400, "invalid device secret");
    }
    return secret;
  }

  private static String aliases(JsonNode body, boolean normalize, String... fields) {
    String selected = null;
    for (String field : fields) {
      if (!body.has(field)) {
        continue;
      }
      JsonNode value = body.get(field);
      if (!value.isTextual()) {
        throw new Rejection(400, "invalid " + field);
      }
      String candidate = normalize ? RodakPairingService.normalizeDeviceKey(value.asText())
          : value.asText();
      if (selected != null && !selected.equals(candidate)) {
        throw new Rejection(400, "conflicting credential fields");
      }
      selected = candidate;
    }
    return selected;
  }

  private static String origin(HttpServletRequest request) {
    String host = request.getServerName();
    if (host.contains(":") && !host.startsWith("[")) {
      host = "[" + host + "]";
    }
    return request.getScheme() + "://" + host + ":" + request.getServerPort();
  }

  private static Envelope ok(Object data) {
    return new Envelope(200, "ok", data);
  }

  @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS)
  public record Envelope(int code, String message, Object data) {}
}
