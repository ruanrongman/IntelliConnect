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

import static java.util.Map.entry;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.rslly.iot.models.RodakDeviceEntity;

@Component
public class RodakDescriptor {
  public static final String DEVICES = "/api/v1/aiot/devices";
  public static final String VOICE = DEVICES + "/realtime-voice";
  private static final Set<Integer> VOICE_SAMPLE_RATES = Set.of(16000, 24000, 48000);
  private static final Set<Integer> VOICE_FRAME_DURATIONS = Set.of(5, 10, 20, 40, 60);
  public static final VoiceFormat VOICE_UPLINK = new VoiceFormat(16000, 60);
  public static final String SERVER_VAD = "server-authoritative";
  public static final String HYBRID_VAD = "hybrid-fallback";
  private final String httpBaseUrl;
  private final String mqttHost;
  private final int mqttPort;
  private final boolean mqttEnabled;
  private final boolean dataEnabled;
  private final boolean hybridVadEnabled;
  private final VoiceFormat downlink;
  private final RodakTokenService tokens;

  public RodakDescriptor(@Value("${rodak.http-base-url:}") String httpBaseUrl,
      @Value("${rodak.mqtt.host:}") String mqttHost,
      @Value("${rodak.mqtt.port:1883}") int mqttPort,
      @Value("${rodak.mqtt.enabled:false}") boolean mqttEnabled,
      @Value("${rodak.aiot.enabled:false}") boolean dataEnabled,
      @Value("${rodak.voice.hybrid-vad-enabled:false}") boolean hybridVadEnabled,
      @Value("${rodak.voice.downlink-sample-rate-hz:16000}") int downlinkSampleRate,
      @Value("${rodak.voice.downlink-frame-duration-ms:60}") int downlinkFrameDuration,
      RodakTokenService tokens) {
    this.httpBaseUrl = httpBaseUrl.replaceFirst("/$", "");
    this.mqttHost = mqttHost;
    this.mqttPort = mqttPort;
    this.mqttEnabled = mqttEnabled;
    this.dataEnabled = dataEnabled;
    this.hybridVadEnabled = hybridVadEnabled;
    downlink = new VoiceFormat(downlinkSampleRate, downlinkFrameDuration);
    this.tokens = tokens;
  }

  public String preferredVadStrategy() {
    return hybridVadEnabled ? HYBRID_VAD : SERVER_VAD;
  }

  public Map<String, Object> bootstrap(String requestOrigin) {
    String base = origin(requestOrigin);
    String broker = broker(base);
    var capabilities = new java.util.ArrayList<>(List.of("manual-pairing",
        "device-jwt-after-pairing", "realtime-voice"));
    var endpoints = new LinkedHashMap<String, Object>(Map.of(
        "httpBaseUrl", base, "mqttEndpoint", "mqtt://" + broker + ":" + mqttPort,
        "bindingRequest", DEVICES + "/binding/request",
        "bindingStatus", DEVICES + "/binding/requests/{requestId}/status",
        "bindingUnbind", DEVICES + "/binding/unbind",
        "token", DEVICES + "/auth/token",
        "otaCheck", "/api/v1/ota/check"));
    if (dataEnabled) {
      capabilities
          .addAll(List.of("shadow", "telemetry", "command-pull", "command-ack", "thing-model"));
      endpoints.putAll(Map.of("heartbeat", DEVICES + "/heartbeat",
          "telemetry", DEVICES + "/telemetry", "shadowReport", DEVICES + "/shadow/report",
          "shadowDesired", DEVICES + "/shadow/desired", "commandsPull",
          DEVICES + "/commands/pull"));
    }
    return Map.ofEntries(entry("module", "aiot"), entry("protocol", "rodak-aiot"),
        entry("protocolVersion", 1), entry("schema", "rodak-aiot/1"),
        entry("productKey", RodakPairingService.PRODUCT_KEY),
        entry("productKeys", List.of(RodakPairingService.PRODUCT_KEY)),
        entry("protocols", List.of("http", "mqtt")),
        entry("productTypes", List.of("device_model")),
        entry("capabilities", List.copyOf(capabilities)),
        entry("heartbeatTimeout", 180), entry("deviceTokenTtl", tokens.ttlSeconds()),
        entry("mqttPort", mqttPort), entry("mqttGatewayEnabled", mqttEnabled),
        entry("realtimeVoice", voice(base)),
        entry("endpoints", endpoints));
  }

  Map<String, Object> credentials(RodakDeviceEntity device, String token, long ttl,
      long expiry, String requestOrigin) {
    String base = origin(requestOrigin);
    String key = device.getDeviceKey();
    String prefix = "devices/" + key + "/";
    var topics = Map.of("telemetry", prefix + "telemetry",
        "shadow_report", prefix + "shadow/report", "shadow_desired", prefix + "shadow/desired",
        "ota_notify", prefix + "ota/notify", "ota_progress", prefix + "ota/progress",
        "commands", prefix + "commands/+", "pc_status", prefix + "pc_status",
        "home_prefix", "rymcu/home/v1");
    var mqtt = Map.ofEntries(entry("protocol_version", 2), entry("broker_address", broker(base)),
        entry("broker_port", mqttPort), entry("username", key), entry("password", token),
        entry("http_base_url", base), entry("http_bearer_token", token),
        entry("keepalive", 240), entry("device_key", key), entry("home_enabled", 0),
        entry("topics", topics));
    return Map.ofEntries(entry("deviceId", device.getDeviceUuid()), entry("deviceCode", key),
        entry("deviceKey", key), entry("productKey", device.getProductKey()),
        entry("credentialKey", device.getProductKey()), entry("deviceToken", token),
        entry("token", token), entry("accessToken", token), entry("tokenType", "Bearer"),
        entry("expiresIn", ttl), entry("tokenTtlSeconds", ttl), entry("expiresAt", expiry),
        entry("expires_at", expiry), entry("activationStatus", 1),
        entry("activationRequired", false), entry("unifiedMqtt", mqtt),
        entry("realtimeVoice", voice(base)));
  }

  private Map<String, Object> voice(String base) {
    var result = new LinkedHashMap<String, Object>();
    result.put("schema", "rodak-realtime-voice/v1");
    result.put("protocol", "rodak-realtime-voice");
    result.put("protocolVersion", 1);
    result.put("transport", "websocket");
    result.put("endpoint", base.replaceFirst("^http", "ws") + VOICE);
    result.put("authMode", "device-token");
    result.put("uplink", VOICE_UPLINK.wire());
    result.put("downlink", downlink.wire());
    result.put("limits", Map.of("maxAudioFrameBytes", 8192, "maxControlBytes", 65536));
    result.put("features", List.of());
    result.put("capabilities", List.of("session", "audio-input", "audio-output",
        "speech-boundaries", "interrupt", "assistant-events"));
    result.put("events", List.of("session.open", "session.ready", "input.start", "input.stop",
        "wake.detected", "playback.abort", "vad", "output.start", "output.stop",
        "session.end", "error"));
    String preferredVad = preferredVadStrategy();
    result.put("vadStrategies", HYBRID_VAD.equals(preferredVad) ? List.of(SERVER_VAD, HYBRID_VAD)
        : List.of(SERVER_VAD));
    result.put("preferredVadStrategy", preferredVad);
    return result;
  }

  public record VoiceFormat(int sampleRate, int frameDurationMs) {
    public VoiceFormat {
      if (!VOICE_SAMPLE_RATES.contains(sampleRate)
          || !VOICE_FRAME_DURATIONS.contains(frameDurationMs)) {
        throw new IllegalArgumentException("unsupported Rodak Opus format");
      }
    }

    public Map<String, Object> wire() {
      return Map.of("codec", "opus", "sampleRateHz", sampleRate, "channels", 1,
          "frameDurationMs", frameDurationMs);
    }
  }

  private String origin(String requestOrigin) {
    return httpBaseUrl.isEmpty() ? requestOrigin : httpBaseUrl;
  }

  private String broker(String base) {
    return mqttHost.isEmpty() ? URI.create(base).getHost() : mqttHost;
  }
}
