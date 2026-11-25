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
package top.rslly.iot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import top.rslly.iot.services.rodak.RodakDescriptor;
import top.rslly.iot.services.rodak.RodakPairingService;
import top.rslly.iot.services.rodak.RodakPairingService.Identity;

@Configuration
public class RodakDeviceSecurity {
  public static final String IDENTITY = "top.rslly.iot.config.RodakDeviceSecurity.identity";

  @Bean
  @Order(1)
  SecurityFilterChain rodakDeviceChain(HttpSecurity http, RodakPairingService devices,
      ObjectMapper json) throws Exception {
    return http.securityMatcher(request -> route(request) != null)
        .csrf(AbstractHttpConfigurer::disable)
        .requestCache(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterBefore(new OncePerRequestFilter() {
          @Override
          protected void doFilterInternal(HttpServletRequest request,
              HttpServletResponse response, FilterChain chain)
              throws ServletException, IOException {
            String route = route(request);
            if ("voice".equals(route) || "device".equals(route)) {
              boolean voice = "voice".equals(route);
              if (voice && !validVoiceHeaders(request.getHeader("Device-Id"),
                  request.getHeader("Client-Id"), request.getHeader("Protocol-Version"))) {
                reject(response, json, 400, "invalid voice headers");
                return;
              }
              try {
                Identity identity = devices.authenticate(
                    bearer(request.getHeader("Authorization")));
                if (voice && !deviceMatches(identity, request.getHeader("Device-Id"))) {
                  throw new SecurityException("device identity mismatch");
                }
                request.setAttribute(IDENTITY, identity);
              } catch (SecurityException e) {
                reject(response, json, 401, "invalid device credentials");
                return;
              }
            }
            response.setHeader("Cache-Control", "no-store");
            chain.doFilter(request, response);
          }
        }, AuthorizationFilter.class)
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll()).build();
  }

  private static String route(HttpServletRequest request) {
        String uri = request.getRequestURI().substring(request.getContextPath().length());
        if ("POST".equals(request.getMethod()) && uri.equals("/internal/rodak/mqtt/authenticate")) {
            return "public";
        }
        if ("GET".equals(request.getMethod())) {
            if (uri.equals("/api/v1/ota/check")) {
                return "device";
            }
            if (uri.matches("/api/v1/aiot/products/[0-9]+/thing-model")) {
                return "public";
            }
        }
        if (!uri.startsWith(RodakDescriptor.DEVICES + "/")) {
            return null;
        }
        String path = uri.substring(RodakDescriptor.DEVICES.length());
        return switch (request.getMethod()) {
            case "GET" -> {
                if (path.equals("/realtime-voice")) {
                    yield "voice";
                }
                if (path.equals("/shadow/desired") || path.equals("/commands/pull")) {
                    yield "device";
                }
                yield path.equals("/bootstrap") || path.matches("/binding/requests/[0-9a-f]{32}/status")
                        ? "public" : null;
            }
            case "POST" -> switch (path) {
                case "/binding/unbind", "/heartbeat", "/telemetry", "/shadow/report" -> "device";
                case "/binding/request", "/auth/token", "/register", "/activate" -> "public";
                default -> path.matches("/commands/[0-9a-f]{32}/ack") ? "device" : null;
            };
            default -> null;
        };
    }

  public static boolean validVoiceHeaders(String device, String client, String version) {
    return device != null && !device.isBlank() && client != null && !client.isBlank()
        && client.length() <= 128 && "1".equals(version);
  }

  public static boolean deviceMatches(Identity identity, String device) {
    return device != null && device.trim().equalsIgnoreCase(identity.deviceKey());
  }

  public static String bearer(String header) {
    return header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)
        && header.length() > 7 ? header.substring(7) : null;
  }

  private static void reject(HttpServletResponse response, ObjectMapper json, int status,
      String message) throws IOException {
    response.setStatus(status);
    response.setContentType("application/json");
    response.setHeader("Cache-Control", "no-store");
    var error = json.createObjectNode().put("code", status).put("message", message).putNull("data");
    json.writeValue(response.getOutputStream(), error);
  }
}
