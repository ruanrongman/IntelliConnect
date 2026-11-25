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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import top.rslly.iot.services.rodak.RodakMqttAccess;

@RestController
@RequestMapping("/internal/rodak/mqtt")
@ConditionalOnProperty(name = "rodak.mqtt.enabled", havingValue = "true")
public class RodakMqttCallbackController {
  private final RodakMqttAccess access;
  private final ObjectMapper json;

  public RodakMqttCallbackController(RodakMqttAccess access, ObjectMapper json) {
    this.access = access;
    this.json = json;
  }

  @PostMapping("/authenticate")
  public Map<String, Object> authenticate(HttpServletRequest request) throws IOException {
    return access.authenticate(body(request));
  }

  private JsonNode body(HttpServletRequest request) throws IOException {
    if (!access.broker(request.getHeader("X-Rodak-Broker-Key"))) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    byte[] bytes = request.getInputStream().readNBytes(8193);
    if (bytes.length > 8192) {
      throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);
    }
    try {
      JsonNode value = json.reader()
          .with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
          .readTree(bytes);
      if (value != null && value.isObject()) {
        return value;
      }
    } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
  }
}
