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
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import javax.sql.DataSource;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;
import top.rslly.iot.controllers.RodakDeviceController;
import top.rslly.iot.controllers.RodakDataController;
import top.rslly.iot.dao.ProductRepository;
import top.rslly.iot.dao.RodakDeviceRepository;
import top.rslly.iot.dao.RodakPairingRequestRepository;
import top.rslly.iot.dao.RodakCommandRepository;
import top.rslly.iot.dao.RodakShadowRepository;
import top.rslly.iot.models.ProductEntity;
import top.rslly.iot.services.thingsModel.ProductServiceImpl;
import top.rslly.iot.services.rodak.RodakPairingService.Rejection;
import top.rslly.iot.utility.result.ResultCode;

@EnabledIfSystemProperty(named = "rodak.test.mysql", matches = "true")
@ActiveProfiles("rodak-mysql-test")
@SpringBootTest(classes = RodakMySqlIntegrationTest.Config.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {"rodak.security.secret=integration-test-key-for-rollback-only",
        "rodak.aiot.enabled=true",
        "spring.datasource.type=com.zaxxer.hikari.HikariDataSource",
        "spring.datasource.hikari.maximum-pool-size=2",
        "spring.datasource.hikari.connection-timeout=5000",
        "spring.jpa.hibernate.ddl-auto=none"})
@Transactional
class RodakMySqlIntegrationTest {
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final String BASE = "http://127.0.0.1:8080";
  private static final Path FIXTURE = Path.of("rodak", "rodakos-main", "contracts",
      "rodak-aiot", "v1", "examples", "valid", "binding-request.rodakos.json");

  @Autowired
  RodakPairingService service;
  @Autowired
  RodakDescriptor descriptor;
  @Autowired
  RodakDataService data;
  @Autowired
  RodakDeviceRepository devices;
  @Autowired
  RodakPairingRequestRepository requests;
  @Autowired
  RodakCommandRepository commands;
  @Autowired
  RodakShadowRepository shadows;
  @Autowired
  ProductRepository products;
  @Autowired
  EntityManager em;
  @Autowired
  DataSource source;

  @BeforeAll
  static void applySql(@Autowired DataSource source) throws Exception {
    try (var connection = source.getConnection()) {
      for (int retry = 0; retry < 2; retry++) {
        ScriptUtils.executeSqlScript(connection, new FileSystemResource("docs/sql/rodak_p1.sql"));
        ScriptUtils.executeSqlScript(connection, new FileSystemResource("docs/sql/rodak_p3.sql"));
      }
    }
  }

  @Test
  void sharedBrokerKeepsLegacyAclAndRequiresCurrentRodakBinding() throws Exception {
    var jdbc = new JdbcTemplate(source);
    String legacy = "rodak-test-" + UUID.randomUUID().toString().substring(0, 12);
    jdbc.update(
        "INSERT INTO mqtt_user(username, password, is_superuser) VALUES (?, SHA2(?, 256), 0)",
        legacy, "rollback-only-password");
    jdbc.update("INSERT INTO mqtt_acl(username, permission, action, topic) VALUES (?, ?, ?, ?)",
        legacy, "allow", "all", "/oc/devices/" + legacy + "/#");

    String authn = Files.readString(Path.of("docker/rodak/shared-mysql-authn.sql"))
        .replaceAll("\\$\\{[^}]+}", "?");
    assertEquals(1, jdbc.queryForList(authn, legacy, "legacy-client", "legacy-client").size());
    assertTrue(
        jdbc.queryForList(authn, legacy, "rodakos_aabbccddeeff", "rodakos_aabbccddeeff").isEmpty());
    assertTrue(jdbc.queryForList(authn, legacy, "rodak-service", "rodak-service").isEmpty());

    String authz = Files.readString(Path.of("docker/rodak/shared-mysql-authz.sql"))
        .replaceAll("\\$\\{[^}]+}", "?");
    var legacyAcl = jdbc.queryForList(authz, legacy, "legacy-client", "", "", "", "", "");
    assertEquals(List.of("deny", "deny", "allow"),
        legacyAcl.stream().map(row -> row.get("permission")).toList());
    assertEquals("/oc/devices/" + legacy + "/#", legacyAcl.getLast().get("topic"));

    int product = testProduct();
    var identity = bind("acl", product);
    em.flush();
    String client = RodakMqttAccess.clientId(identity.deviceKey());
    long expiry = identity.expiresAt().getEpochSecond();
    Object[] credential = {identity.deviceKey(), client, identity.id(),
        Long.toString(identity.version()), Integer.toString(product), Long.toString(expiry), ""};
    var acl = jdbc.queryForList(authz, credential);
    assertEquals(8, acl.size());
    assertTrue(acl.stream().anyMatch(row -> "subscribe".equals(row.get("action"))
        && ("eq devices/" + identity.deviceKey() + "/commands/+").equals(row.get("topic"))));
    var bridge = jdbc.queryForList(authz, "rodak-service", "rodak-service", "", "", "", "", "true");
    assertEquals("eq _rodak/events/#", bridge.getFirst().get("topic"));
    assertEquals(1,
        jdbc.queryForList(authz, "rodak-service", "rodak-service", "", "", "", "", "").size());

    Object[] expired = credential.clone();
    expired[5] = "1";
    assertEquals(1, jdbc.queryForList(authz, expired).size());
    service.unbind(identity.id(), product, identity.version());
    em.flush();
    assertEquals(List.of("deny"), jdbc.queryForList(authz, credential)
        .stream().map(row -> row.get("permission")).toList());
  }

  @Test
  void aiotStoresScopedShadowAndFreezesTheFirstAcknowledgement() {
    int product = testProduct();
    var a = bind("a", product);
    var b = bind("b", product);
    assertNotNull(data.heartbeat(a).get("timestamp"));
    data.telemetry(a, JSON.createObjectNode().put("battery", 85));
    var desired = data.volume(a.id(), product, 37);
    assertEquals(1, desired.shadowVersion());
    assertEquals(1, data.volume(a.id(), product, 37).shadowVersion());
    data.report(a, JSON.createObjectNode().put("volume", 37));
    var command = data.ping(a.id(), product);
    assertEquals(command.commandNo(), data.pull(a, 20).getFirst().commandNo());
    var controller = new RodakDataController(data, JSON);
    var pulled = JSON.valueToTree(controller.pull(a, 20).data()).path(0);
    assertEquals(command.commandNo(), pulled.path("id").asText());
    assertEquals("{\"command\":\"ping\"}", pulled.path("payload").asText());
    assertEquals("devices/" + a.deviceKey() + "/commands/" + command.commandNo(),
        pulled.path("topic").asText());
    var wireShadow = JSON.valueToTree(controller.desired(a).data());
    assertEquals(wireShadow.path("shadowVersion"), wireShadow.path("version"));
    assertEquals(37, wireShadow.path("state").path("reported").path("volume").asInt());
    assertTrue(data.pull(b, 20).isEmpty());
    var ack = JSON.createObjectNode().put("status", "ok");
    ack.putObject("result").put("pong", true);
    assertEquals(404, assertThrows(Rejection.class,
        () -> data.acknowledge(b, command.commandNo(), ack)).code());
    data.acknowledge(a, command.commandNo(), ack);
    data.acknowledge(a, command.commandNo(), JSON.createObjectNode().put("status", "error"));
    em.flush();
    em.clear();
    assertEquals(85, data.desired(a).telemetry().path("battery").asInt());
    assertEquals(37, data.desired(a).reported().path("volume").asInt());
    var snapshots = data.snapshots(service.devicesForProduct(product));
    assertEquals(85, snapshots.get(a.id()).telemetry().path("battery").asInt());
    assertFalse(snapshots.containsKey(b.id()));
    assertTrue(data.desired(b).desired().isEmpty());
    assertTrue(data.pull(a, 20).isEmpty());
    assertEquals("ok", data.command(a.id(), product, command.commandNo()).status());
    service.unbind(a.id(), product, a.version());
    assertEquals(401, assertThrows(Rejection.class, () -> data.desired(a)).code());
  }

  private RodakPairingService.Identity bind(String prefix, int product) {
    String key = "rodak-test-" + prefix + UUID.randomUUID().toString().substring(0, 12);
    var created = service.createRequest(key, "rollback-only-device-secret", "192.0.2.4");
    service.confirm((String) created.get("requestId"), (String) created.get("pairingCode"),
        product);
    var credentials = service.refresh(key, "rollback-only-device-secret", BASE);
    return service.authenticate((String) credentials.get("token"));
  }

  @Test
  void pairsFromOriginalFirmwareRequestRefreshesAndRevokes() throws Exception {
    int product = testProduct();
    var controller = new RodakDeviceController(service, descriptor, JSON);
    String key = "rodak-test-" + UUID.randomUUID().toString().substring(0, 12);
    var requestBody =
        (com.fasterxml.jackson.databind.node.ObjectNode) JSON.readTree(FIXTURE.toFile());
    requestBody.put("deviceKey", key.toUpperCase(java.util.Locale.ROOT));
    requestBody.put("deviceCode", key);
    var request = new MockHttpServletRequest();
    request.setContent(JSON.writeValueAsBytes(requestBody));
    request.setRemoteAddr("192.0.2.1");
    JsonNode created = JSON.valueToTree(controller.bindingRequest(request).data());
    String id = created.path("requestId").asText();
    String requestToken = created.path("requestToken").asText();
    assertTrue(created.path("pairingCode").asText().matches("[0-9]{8}"));
    assertEquals("pending", created.path("status").asText());
    assertEquals(2, created.path("pollIntervalSeconds").asInt());
    assertEquals("pending", service.status(id, requestToken, BASE).get("status"));
    assertEquals(401,
        assertThrows(Rejection.class, () -> service.status(id, "wrong", BASE)).code());

    String deviceId = service.confirm(null, created.path("pairingCode").asText(), product);
    em.flush();
    em.clear();
    var confirmed = service.status(id, requestToken, BASE);
    assertEquals("confirmed", confirmed.get("status"));
    assertEquals(deviceId, confirmed.get("deviceId"));
    assertEquals(key, confirmed.get("deviceKey"));
    assertEquals(confirmed.get("deviceToken"), confirmed.get("accessToken"));
    assertEquals("ws://127.0.0.1:8080/api/v1/aiot/devices/realtime-voice",
        ((Map<?, ?>) confirmed.get("realtimeVoice")).get("endpoint"));
    assertEquals(key, ((Map<?, ?>) confirmed.get("unifiedMqtt")).get("username"));

    String plainSecret = requestBody.path("credentialSecret").asText();
    var device = devices.findByDeviceUuid(deviceId).orElseThrow();
    assertNotNull(device.getId());
    assertNotEquals(plainSecret, device.getCredentialDigest());
    var refreshed = service.refresh(key, plainSecret, BASE);
    String jwt = (String) refreshed.get("deviceToken");
    assertTrue(jwt.length() <= 255);
    var identity = service.authenticate(jwt);
    assertEquals(product, identity.productId());
    assertEquals(deviceId, service.unbind(deviceId, product, identity.version()));
    em.flush();
    em.clear();
    assertFalse(service.isCurrent(identity));
    assertThrows(SecurityException.class, () -> service.authenticate(jwt));
    assertEquals(403, assertThrows(Rejection.class,
        () -> service.refresh(key, plainSecret, BASE)).code());
    assertEquals("revoked", service.status(id, requestToken, BASE).get("status"));
  }

  @Test
  void recoversAnExistingBindingAfterTheDeviceChangesBootstrapAddress() {
    int product = testProduct();
    var identity = bind("recovery", product);
    String origin = "http://192.0.2.5:8080";
    assertEquals(409, assertThrows(Rejection.class,
        () -> service.createRequest(identity.deviceKey(), "wrong-secret", "192.0.2.6")).code());

    var result = service.createRequest(identity.deviceKey(), "rollback-only-device-secret",
        "192.0.2.6");
    String requestId = (String) result.get("requestId");
    String requestToken = (String) result.get("requestToken");
    assertEquals("pending", result.get("status"));
    assertTrue(((String) result.get("pairingCode")).matches("[0-9]{8}"));
    assertThrows(Rejection.class,
        () -> service.confirm(requestId, (String) result.get("pairingCode"), product));
    assertEquals(401, assertThrows(Rejection.class,
        () -> service.status(requestId, "wrong-token", origin)).code());

    var recovered = service.status(requestId, requestToken, origin);
    assertEquals("confirmed", recovered.get("status"));
    assertEquals(identity.id(), recovered.get("deviceId"));
    var recoveredIdentity = service.authenticate((String) recovered.get("deviceToken"));
    assertEquals(product, recoveredIdentity.productId());
    assertEquals(identity.version(), recoveredIdentity.version());
    assertEquals(origin, ((Map<?, ?>) recovered.get("unifiedMqtt")).get("http_base_url"));

    service.unbind(identity.id(), product, identity.version());
    assertEquals("revoked", service.status(requestId, requestToken, origin).get("status"));
  }

  @Test
  void fiveWrongCodesRevokePairingAndAreFlushedToMySql() {
    int product = testProduct();
    String key = "rodak-test-" + UUID.randomUUID().toString().substring(0, 12);
    var request = service.createRequest(key, "just-a-rollback-test-secret", "192.0.2.2");
    String id = (String) request.get("requestId");
    String wrong = "99999999".equals(request.get("pairingCode")) ? "00000000" : "99999999";
    for (int index = 0; index < 5; index++) {
      assertThrows(Rejection.class, () -> service.confirm(id, wrong, product));
    }
    em.flush();
    em.clear();
    assertEquals("revoked",
        service.status(id, (String) request.get("requestToken"), BASE).get("status"));
    assertTrue(devices.findByDeviceKey(key).isEmpty());
  }

  @Test
  void productDeletionRequiresUnbindingAndRemovesRodakDependents() {
    var product = new ProductEntity();
    product.setProductName("Rodak deletion " + UUID.randomUUID());
    product.setKeyvalue(UUID.randomUUID().toString());
    em.persist(product);
    int productId = product.getId();
    var identity = bind("deletion", productId);
    String key = identity.deviceKey();
    long deviceId = devices.findByDeviceUuid(identity.id()).orElseThrow().getId();
    String commandNo = data.ping(identity.id(), productId).commandNo();
    data.volume(identity.id(), productId, 30);
    var jwt = (String) service.refresh(key, "rollback-only-device-secret", BASE).get("deviceToken");
    assertEquals(1, requests.findAllByProductId(productId).size());

    var productService = new ProductServiceImpl();
    ReflectionUtils.doWithFields(ProductServiceImpl.class,
        field -> ReflectionTestUtils.setField(productService, field.getName(),
            mock(field.getType())),
        field -> field.isAnnotationPresent(jakarta.annotation.Resource.class));
    ReflectionTestUtils.setField(productService, "productRepository", products);
    ReflectionTestUtils.setField(productService, "rodakDeviceRepository", devices);
    ReflectionTestUtils.setField(productService, "rodakPairingRequestRepository", requests);
    ReflectionTestUtils.setField(productService, "rodakCommandRepository", commands);
    ReflectionTestUtils.setField(productService, "rodakShadowRepository", shadows);

    var blocked = productService.deleteProduct(productId);
    assertEquals(ResultCode.HAS_DEPENDENCIES.getCode(), blocked.getErrorCode());
    assertTrue(blocked.getErrorMsg().contains("请先解绑"));
    assertTrue(shadows.existsById(deviceId));
    assertEquals(commandNo, data.pull(identity, 1).getFirst().commandNo());

    service.unbind(identity.id(), productId, identity.version());
    var pending = service.createRequest(key, "next-binding-device-secret", "192.0.2.9");
    String pendingId = (String) pending.get("requestId");
    assertTrue(productService.deleteProduct(productId).getSuccess());
    em.flush();
    em.clear();
    assertTrue(products.findAllById(productId).isEmpty());
    assertFalse(devices.existsById(deviceId));
    assertFalse(shadows.existsById(deviceId));
    assertTrue(commands.findByCommandNoAndDeviceIdAndBindingVersion(
        commandNo, deviceId, identity.version()).isEmpty());
    assertTrue(requests.findAllByProductId(productId).isEmpty());
    assertTrue(requests.findByRequestId(pendingId).isPresent());

    service.confirm(pendingId, (String) pending.get("pairingCode"), testProduct());
    assertThrows(SecurityException.class, () -> service.authenticate(jwt));
  }

  private int testProduct() {
    // Use a rolled-back row only if the configured test database has no products.
    var existing = em.createQuery("select p.id from ProductEntity p order by p.id", Integer.class)
        .setMaxResults(1).getResultList();
    if (!existing.isEmpty())
      return existing.getFirst();
    var row = new ProductEntity();
    row.setProductName("Rodak integration test");
    row.setKeyvalue(UUID.randomUUID().toString());
    em.persist(row);
    return row.getId();
  }

  @Configuration(proxyBeanMethods = false)
  @Profile("rodak-mysql-test")
  @ImportAutoConfiguration({DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
  @EntityScan(basePackageClasses = ProductEntity.class)
  @EnableJpaRepositories(basePackageClasses = ProductRepository.class,
      includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
          classes = {ProductRepository.class, RodakDeviceRepository.class,
              RodakPairingRequestRepository.class, RodakCommandRepository.class,
              RodakShadowRepository.class}))
  @Import({RodakPairingService.class, RodakTokenService.class, RodakDescriptor.class,
      RodakDataServiceImpl.class})
  static class Config {
    @Bean
    ObjectMapper rodakJson() {
      return new ObjectMapper();
    }
  }
}
