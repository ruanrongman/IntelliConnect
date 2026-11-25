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

import static top.rslly.iot.services.rodak.RodakMqttAccess.*;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import top.rslly.iot.services.rodak.RodakPairingService.Identity;
import top.rslly.iot.services.rodak.RodakPairingService.Rejection;
import top.rslly.iot.services.rodak.RodakPairingService.Revoked;
import top.rslly.iot.utility.properties.EmqProperty;

@Component
@ConditionalOnProperty(name = "rodak.mqtt.enabled", havingValue = "true")
public class RodakMqttBridge {
  private static final Logger LOG = LoggerFactory.getLogger(RodakMqttBridge.class);
  private final RodakPairingService pairing;
  private final RodakDataService data;
  private final ObjectMapper json;
  private final MqttAsyncClient mqtt;
  private final MqttConnectOptions options = new MqttConnectOptions();
  private final Map<String, Connection> online = new ConcurrentHashMap<>();
  private final Map<String, Revoked> revocations = new ConcurrentHashMap<>();
  private final HttpClient management = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(2)).build();
  private final URI managementClients;
  private final String managementAuth;
  private boolean subscribed;

  public RodakMqttBridge(RodakPairingService pairing, RodakDataService data, ObjectMapper json,
      EmqProperty emq,
      @Value("${rodak.aiot.enabled:false}") boolean dataEnabled,
      @Value("${rodak.mqtt.service-password:}") String password,
      @Value("${rodak.mqtt.bridge-uri:${mqtt.Host:tcp://127.0.0.1:1883}}") String uri)
      throws MqttException {
    if (!dataEnabled || emq.getUrl() == null || emq.getUrl().isBlank()
        || emq.getKey() == null || emq.getKey().isBlank()
        || emq.getSecret() == null || emq.getSecret().isBlank()) {
      throw new IllegalArgumentException(
          "Rodak MQTT requires AIoT storage and emq.api.url, emq.api.key, and emq.api.secret");
    }
    this.pairing = pairing;
    this.data = data;
    this.json = json;
    URI clients = URI.create(emq.getUrl());
    this.managementClients = clients.resolve(clients.getRawPath().replaceFirst("/+$", "") + "/");
    this.managementAuth = "Basic " + Base64.getEncoder().encodeToString(
        (emq.getKey() + ":" + emq.getSecret()).getBytes(StandardCharsets.UTF_8));
    options.setUserName(BRIDGE);
    options.setPassword(password.toCharArray());
    options.setCleanSession(true);
    options.setConnectionTimeout(3);
    options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
    mqtt = new MqttAsyncClient(uri, BRIDGE, new MemoryPersistence());
    mqtt.setCallback(new MqttCallback() {
      @Override
      public void connectionLost(Throwable cause) {
        LOG.warn("Rodak broker bridge disconnected");
      }

      @Override
      public void messageArrived(String topic, MqttMessage message) {
        accept(topic, message);
      }

      @Override
      public void deliveryComplete(IMqttDeliveryToken token) {}
    });
  }

  public boolean isOnline(String key, long version) {
    Connection connection = online.get(key);
    return mqtt.isConnected() && connection != null && connection.identity.version() == version
        && Instant.now().isBefore(connection.identity.expiresAt());
  }

  public boolean canAnnounce(Identity identity) {
    return announcementConnection(identity) != null;
  }

  public boolean announce(Identity identity, long sequence, Instant expiresAt,
      Consumer<String> rejected) {
    Connection connection = announcementConnection(identity);
    if (connection == null || !pairing.isCurrent(identity)) {
      return false;
    }
    String commandNo = UUID.randomUUID().toString().replace("-", "");
    Announcement command = new Announcement(commandNo, expiresAt, rejected);
    connection.announcement.set(command);
    try {
      if (announcementConnection(identity) != connection || !Instant.now().isBefore(expiresAt)) {
        connection.announcement.compareAndSet(command, null);
        return false;
      }
      publish("devices/" + identity.deviceKey() + "/commands/" + commandNo,
          Map.of("command", "voice.announce", "announceSeq", sequence, "priority", "normal"));
      return true;
    } catch (MqttException e) {
      connection.announcement.compareAndSet(command, null);
      LOG.warn("Rodak announcement publish failed, reason={}", e.getReasonCode());
      return false;
    }
  }

  private Connection announcementConnection(Identity identity) {
    Connection connection = online.get(identity.deviceKey());
    return connection != null && connection.commands && mqtt.isConnected()
        && connection.identity.id().equals(identity.id())
        && connection.identity.productId() == identity.productId()
        && connection.identity.version() == identity.version()
        && Instant.now().isBefore(connection.identity.expiresAt())
        && !revocations.containsKey(identity.deviceKey()) ? connection : null;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void revoked(Revoked event) {
    revocations.put(event.key(), event);
  }

  @Scheduled(fixedDelay = 1500, scheduler = "rodakMqttTaskScheduler")
  public void exchange() {
    try {
      if (!mqtt.isConnected()) {
        subscribed = false;
        online.clear();
        mqtt.connect(options).waitForCompletion(3500);
      }
      if (!subscribed) {
        var subscription = mqtt.subscribe(EVENTS + "#", 1);
        subscription.waitForCompletion(2000);
        if (subscription.getGrantedQos()[0] == 0x80) {
          LOG.warn("Rodak broker denied the bridge event subscription");
          return;
        }
        subscribed = true;
      }
      for (var entry : revocations.entrySet()) {
        disconnect(entry.getValue());
      }
      for (Connection connection : online.values()) {
        exchange(connection);
      }
    } catch (MqttException ex) {
      LOG.warn("Rodak broker exchange failed, reason={}", ex.getReasonCode());
    }
  }

  private void accept(String eventTopic, MqttMessage message) {
    byte[] payload = message.getPayload();
    if (message.isRetained() || payload.length > 8 * RodakDataServiceImpl.MAX_PAYLOAD) {
      return;
    }
    try {
      JsonNode event = parse(payload);
      JsonNode attrs = event.path("client_attrs");
      Identity identity = identity(text(event, "username"), text(event, "clientid"), attrs);
      if (identity == null) {
        return;
      }
      String key = identity.deviceKey();
      String session = text(attrs, "rodak_session");
      String topic = text(event, "topic");
      if ((EVENTS + "client.disconnected").equals(eventTopic)) {
        online.computeIfPresent(key,
            (ignored, value) -> value.session.equals(session) ? null : value);
        return;
      }
      if (!pairing.isCurrent(identity)) {
        return;
      }
      switch (eventTopic.substring(EVENTS.length())) {
        case "client.connected" -> {
          connection(identity, session, event);
          data.heartbeat(identity);
        }
        case "session.subscribed" -> {
          Connection current = connection(identity, session, event);
          if (current.session.equals(session)) {
            current.desired |= ("devices/" + key + "/shadow/desired").equals(topic);
            current.commands |= ("devices/" + key + "/commands/+").equals(topic);
          }
        }
        case "session.unsubscribed" -> {
          Connection current = online.get(key);
          if (current != null && current.session.equals(session)) {
            if (("devices/" + key + "/shadow/desired").equals(topic)) {
              current.desired = false;
              current.sentDesired = null;
            }
            if (("devices/" + key + "/commands/+").equals(topic)) {
              current.commands = false;
            }
          }
        }
        case "message.publish" -> ingest(identity, session, topic, event);
        default -> {
        }
      }
    } catch (Rejection ex) {
      LOG.debug("Rodak broker message rejected, code={}", ex.code());
    } catch (Exception ex) {
      LOG.warn("Rodak broker message skipped, type={}", ex.getClass().getSimpleName());
    }
  }

  private void ingest(Identity identity, String session, String topic, JsonNode event)
      throws java.io.IOException {
    String key = identity.deviceKey();
    if (!allowsDevicePublish(key, topic)) {
      return;
    }
    byte[] bytes = text(event, "payload").getBytes(StandardCharsets.UTF_8);
    if (bytes.length > RodakDataServiceImpl.MAX_PAYLOAD) {
      return;
    }
    JsonNode body = parse(bytes);
    String suffix = topic.substring(("devices/" + key + "/").length());
    switch (suffix) {
      case "telemetry" -> {
        data.telemetry(identity, body);
        Connection current = connection(identity, session, event);
        if (current.session.equals(session) && !current.desired && !current.commands) {
          current.recoverSubscriptions = true;
        }
      }
      case "shadow/report" -> data.report(identity, body);
      default -> {
        String commandNo = suffix.substring(9, 41);
        if (!acknowledgeAnnouncement(key, session, commandNo, body)) {
          data.acknowledge(identity, commandNo, body);
        }
      }
    }
  }

  private boolean acknowledgeAnnouncement(String key, String session, String commandNo,
      JsonNode body) {
    Connection connection = online.get(key);
    if (connection == null || !connection.session.equals(session)) {
      return false;
    }
    Announcement command = connection.announcement.get();
    if (command == null || !command.commandNo().equals(commandNo)) {
      return false;
    }
    if (connection.announcement.compareAndSet(command, null)
        && Instant.now().isBefore(command.expiresAt()) && !"ok".equals(text(body, "status"))) {
      command.rejected().accept(text(body, "errorCode"));
    }
    return true;
  }

  private Connection connection(Identity identity, String session, JsonNode event) {
    long since = event.path("timestamp").asLong();
    return online.compute(identity.deviceKey(),
        (key, prior) -> prior != null && (prior.session.equals(session) || prior.eventTime > since)
            ? prior
            : new Connection(identity, session, since));
  }

  private void exchange(Connection connection) throws MqttException {
    Identity identity = connection.identity;
    String key = identity.deviceKey();
    if (online.get(key) != connection) {
      return;
    }
    try {
      if (revocations.containsKey(key)) {
        return;
      }
      if (!pairing.isCurrent(identity)) {
        online.remove(key, connection);
        if (Instant.now().isBefore(identity.expiresAt())) {
          revocations.putIfAbsent(key, new Revoked(identity.id(), key, identity.version()));
        }
        return;
      }
      String prefix = "devices/" + key + "/";
      Announcement announcement = connection.announcement.get();
      if (announcement != null && !Instant.now().isBefore(announcement.expiresAt())) {
        connection.announcement.compareAndSet(announcement, null);
      }
      if (connection.recoverSubscriptions) {
        recoverSubscriptions(connection, prefix);
      }
      if (connection.desired) {
        var shadow = data.desired(identity);
        if (!shadow.desired().equals(connection.sentDesired)
            && online.get(key) == connection && !revocations.containsKey(key)) {
          var wire = shadow.wire(key);
          wire.put("_meta", Map.of("rodak/shadowWrite",
              Map.of("dispatchId", java.util.UUID.randomUUID().toString())));
          publish(prefix + "shadow/desired", wire);
          connection.sentDesired = shadow.desired();
        }
      }
      if (connection.commands) {
        var pending = data.pull(identity, 100);
        var stillPending = new HashSet<String>();
        for (var command : pending) {
          if (online.get(key) != connection || revocations.containsKey(key)) {
            break;
          }
          stillPending.add(command.commandNo());
          if (Instant.now().isBefore(command.expiresAt())
              && !connection.sent.contains(command.commandNo())) {
            publish(prefix + "commands/" + command.commandNo(), command.payload());
            connection.sent.add(command.commandNo());
          }
        }
        connection.sent.retainAll(stillPending);
      }
    } catch (Rejection ex) {
      online.remove(key, connection);
    }
  }

  private void disconnect(Revoked revoked) {
    Connection current = online.get(revoked.key());
    if (current != null && current.identity.version() != revoked.version()) {
      revocations.remove(revoked.key(), revoked);
      return;
    }
    try {
      String suffix = clientId(revoked.key());
      var lookup = management.send(managementRequest(suffix).GET().build(),
          HttpResponse.BodyHandlers.ofByteArray());
      if (lookup.statusCode() != 404 && lookup.statusCode() != 200) {
        LOG.warn("Rodak broker client lookup failed, status={}", lookup.statusCode());
        return;
      }
      if (lookup.statusCode() == 200) {
        var attrs = parse(lookup.body()).path("client_attrs");
        if (text(attrs, "rodak_id").isEmpty() || text(attrs, "rodak_version").isEmpty()) {
          LOG.warn("Rodak broker client lookup did not return binding attributes");
          return;
        }
        if (!revoked.id().equals(text(attrs, "rodak_id"))
            || !Long.toString(revoked.version()).equals(text(attrs, "rodak_version"))) {
          revocations.remove(revoked.key(), revoked);
          return;
        }
      }
      if (lookup.statusCode() == 404) {
        if (current != null) {
          online.remove(revoked.key(), current);
        }
        revocations.remove(revoked.key(), revoked);
        return;
      }
      var request = managementRequest(suffix).DELETE().build();
      int status = management.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
      if (status == 204 || status == 404) {
        if (current != null) {
          online.remove(revoked.key(), current);
        }
        revocations.remove(revoked.key(), revoked);
      } else {
        LOG.warn("Rodak broker disconnect failed, status={}", status);
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    } catch (java.io.IOException ex) {
      LOG.warn("Rodak broker disconnect request failed, type={}", ex.getClass().getSimpleName());
    }
  }

  private void recoverSubscriptions(Connection connection, String prefix) {
    connection.recoverSubscriptions = false;
    try {
      var response = management.send(managementRequest(
          clientId(connection.identity.deviceKey()) + "/subscriptions").GET().build(),
          HttpResponse.BodyHandlers.ofByteArray());
      if (response.statusCode() == 200) {
        for (JsonNode subscription : parse(response.body())) {
          String topic = text(subscription, "topic");
          connection.desired |= topic.equals(prefix + "shadow/desired");
          connection.commands |= topic.equals(prefix + "commands/+");
        }
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    } catch (java.io.IOException ex) {
      LOG.debug("Rodak broker subscription lookup failed, type={}", ex.getClass().getSimpleName());
    }
  }

  private HttpRequest.Builder managementRequest(String path) {
    return HttpRequest.newBuilder(managementClients.resolve(path))
        .header("Authorization", managementAuth).timeout(Duration.ofSeconds(2));
  }

  private void publish(String topic, Map<String, ?> body) throws MqttException {
    try {
      mqtt.publish(topic, json.writeValueAsBytes(body), 1, false).waitForCompletion(2000);
    } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
      throw new IllegalStateException("cannot encode Rodak broker response", ex);
    }
  }

  private JsonNode parse(byte[] bytes) throws java.io.IOException {
    return json.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(bytes);
  }

  @PreDestroy
  public void close() throws MqttException {
    if (mqtt.isConnected()) {
      mqtt.disconnectForcibly(500, 500);
    }
    mqtt.close(true);
    management.close();
  }

  private static final class Connection {
    final Identity identity;
    final String session;
    final long eventTime;
    final Set<String> sent = new HashSet<>();
    final AtomicReference<Announcement> announcement = new AtomicReference<>();
    volatile boolean desired;
    volatile boolean commands;
    volatile boolean recoverSubscriptions;
    volatile JsonNode sentDesired;

    Connection(Identity identity, String session, long eventTime) {
      this.identity = identity;
      this.session = session;
      this.eventTime = eventTime;
    }
  }

  private record Announcement(String commandNo, Instant expiresAt, Consumer<String> rejected) {}
}
