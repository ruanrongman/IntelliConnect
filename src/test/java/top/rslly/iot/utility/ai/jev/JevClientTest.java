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
package top.rslly.iot.utility.ai.jev;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ByteArrayResource;
import top.rslly.iot.utility.HttpRequestUtils;

class JevClientTest {
  private static final String NOUL_RESPONSE = """
      {"model":"jev-1.13.0","answers":{"urgent":{"type":"noul","noul":0.95}},
       "usage":{"input_tokens":296,"output_tokens":20}}
      """;
  private final ObjectMapper mapper = new ObjectMapper();
  private final BlockingQueue<Reply> replies = new LinkedBlockingQueue<>();
  private final BlockingQueue<CapturedRequest> requests = new LinkedBlockingQueue<>();
  private final List<Long> delays = new ArrayList<>();
  private HttpServer server;
  private String baseUrl;
  private JevProperties properties;
  private JevClient client;

  @BeforeEach
  void setUp() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", exchange -> {
      try (exchange) {
        requests.add(new CapturedRequest(exchange.getRequestURI().getPath(),
            exchange.getRequestMethod(), exchange.getRequestHeaders().getFirst("Authorization"),
            exchange.getRequestHeaders().getFirst("Content-Type"),
            new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
        Reply reply = replies.poll();
        if (reply == null) {
          reply = new Reply(500, "Unexpected extra request", null);
        }
        if (reply.retryAfter() != null) {
          exchange.getResponseHeaders().set("Retry-After", reply.retryAfter());
        }
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] bytes = reply.body().getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(reply.status(), bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
          exchange.getResponseBody().write(bytes);
        }
      }
    });
    server.start();
    baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    properties = new JevProperties();
    properties.setBaseUrl(baseUrl);
    properties.setKey("local-test-key");
    properties.setModel("jev-test-model");
    client = new JevClient(properties, new HttpRequestUtils(), delays::add);
  }

  @AfterEach
  void tearDown() {
    Thread.interrupted();
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void sendsMixedStructuredQuestionsAndReadsTypedAnswers() throws Exception {
    replies.add(new Reply(200, """
        {"model":"jev-1.13.0","answers":{
          "urgent":{"type":"noul","noul":0.95},
          "department":{"type":"choice","choice":"billing",
            "probabilities":{"billing":0.88,"technical":0.12},"confidence":0.81},
          "frustration":{"type":"score","score":1.05,
            "legend":{"0":"Calm","1":"Frustrated","2":"Very angry"},
            "probabilities":{"0":0.0,"1":0.95,"2":0.05},"confidence":0.92}},
          "usage":{"input_tokens":318,"output_tokens":34},"future_field":true}
        """, null));
    Map<String, Object> choices = new LinkedHashMap<>();
    choices.put("billing", Map.of("description", "账单、退款"));
    choices.put("technical", null);
    Object state = Map.of("messages", List.of(Map.of("role", "user", "content", "退款失败三天了")));
    Map<String, JevQuestion> questions = Map.of(
        "urgent", JevQuestion.noul(List.of("是否紧急？", Map.of("hint", "时间敏感")),
            Map.of("true", List.of("需要立即处理"), "false", "不紧急")),
        "department", JevQuestion.choice(Map.of("question", "由哪个部门处理？"), choices),
        "frustration", JevQuestion.score("愤怒程度？",
            List.of("Calm", Map.of("level", "Frustrated"), List.of("Very angry"))));

    JevResponse result = client.evaluate(state, questions);
    assertEquals("jev-1.13.0", result.model());
    assertEquals(0.95, ((JevResponse.NoulAnswer) result.answers().get("urgent")).noul());
    var choice = (JevResponse.ChoiceAnswer) result.answers().get("department");
    assertEquals("billing", choice.choice());
    assertEquals(0.12, choice.probabilities().get("technical"));
    assertEquals(0.81, choice.confidence());
    var score = (JevResponse.ScoreAnswer) result.answers().get("frustration");
    assertEquals(1.05, score.score());
    assertEquals("Frustrated", score.legend().get("1"));
    assertEquals(0.92, score.confidence());
    assertEquals(318L, result.usage().inputTokens());
    assertEquals(34L, result.usage().outputTokens());

    CapturedRequest captured = requests.remove();
    assertEquals("POST", captured.method());
    assertEquals("/v1/systemone", captured.path());
    assertEquals("Bearer local-test-key", captured.authorization());
    assertTrue(captured.contentType().startsWith("application/json"));
    JsonNode body = mapper.readTree(captured.body());
    assertEquals(3, body.size());
    assertEquals("jev-test-model", body.get("model").asText());
    assertEquals(mapper.valueToTree(state), body.get("state"));
    assertEquals(mapper.valueToTree(questions), body.get("questions"));
    assertTrue(body.at("/questions/department/criteria/technical").isNull());
  }

  @Test
  void treatsMissingOutputTokensAsZeroForDecisionOnlyProviders() {
    replies.add(new Reply(200, """
        {"model":"decision-model-preview","answers":{
          "urgent":{"type":"noul","noul":0.95}},
          "usage":{"input_tokens":296},"latency_ms":52.9}
        """, null));

    JevResponse result = evaluateNoul();

    assertEquals(296L, result.usage().inputTokens());
    assertEquals(0L, result.usage().outputTokens());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "/", "/v1", "/v1/", "/proxy/", "/proxy/v1/"})
  void normalizesBaseUrl(String suffix) {
    properties.setBaseUrl(baseUrl + suffix);
    replies.add(new Reply(200, NOUL_RESPONSE, null));
    evaluateNoul();
    String prefix = suffix.startsWith("/proxy") ? "/proxy" : "";
    assertEquals(prefix + "/v1/systemone", requests.remove().path());
  }

  @Test
  void bindsAllThreeSettingsFromYamlInSpring() {
    String yaml = """
        ai:
          decision-model:
            base-url: %s/v1/
            key: yaml-test-key
            model: jev-yaml-model
        """.formatted(baseUrl);
    replies.add(new Reply(200, NOUL_RESPONSE, null));
    context(yaml).run(ctx -> {
      assertNull(ctx.getStartupFailure());
      ctx.getBean(JevClient.class).evaluate("help", noulQuestions());
      CapturedRequest captured = requests.remove();
      assertEquals("/v1/systemone", captured.path());
      assertEquals("Bearer yaml-test-key", captured.authorization());
      assertEquals("jev-yaml-model", mapper.readTree(captured.body()).get("model").asText());
    });
  }

  @Test
  void propertiesDeclareDefaultsWithoutRequiringCredentials() {
    JevProperties defaults = new JevProperties();
    assertEquals("https://api.typesafe.ai", defaults.getBaseUrl());
    assertEquals("decision-model-preview", defaults.getModel());
    assertTrue(defaults.getKey().isEmpty());
  }

  @Test
  void emptyKeyAllowsStartupButFailsBeforeNetworkCall() {
    context("ai:\n  decision-model:\n    key: ''\n").run(ctx -> {
      assertNull(ctx.getStartupFailure());
      IllegalStateException error = assertThrows(IllegalStateException.class,
          () -> ctx.getBean(JevClient.class).evaluate("help", noulQuestions()));
      assertTrue(error.getMessage().contains("ai.decision-model.key"));
    });
    assertTrue(requests.isEmpty());
  }

  @Test
  void validatesRequestsBeforeSending() {
    assertThrows(IllegalArgumentException.class, () -> client.evaluate(null, noulQuestions()));
    assertThrows(IllegalArgumentException.class, () -> client.evaluate(42, noulQuestions()));
    assertThrows(IllegalArgumentException.class, () -> client.evaluate("text", null));
    assertThrows(IllegalArgumentException.class, () -> client.evaluate("text", Map.of()));
    assertThrows(IllegalArgumentException.class,
        () -> client.evaluate("text", Map.of(" ", JevQuestion.noul("Question"))));
    assertInvalid(JevQuestion.noul(" "));
    assertInvalid(JevQuestion.noul(true));
    assertInvalid(JevQuestion.noul("Question", Map.of("yes", "yes")));
    assertInvalid(JevQuestion.choice("Question", null));
    assertInvalid(JevQuestion.choice("Question", Map.of()));
    assertInvalid(JevQuestion.choice("Question", choiceCriteria(256)));
    assertInvalid(JevQuestion.score("Question", null));
    assertInvalid(JevQuestion.score("Question", List.of("one")));
    assertInvalid(JevQuestion.score("Question", Collections.nCopies(11, "level")));
    assertInvalid(JevQuestion.score("Question", List.of(1, 2)));
    assertTrue(requests.isEmpty());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 255})
  void acceptsChoiceOptionBoundaries(int count) throws Exception {
    // A deliberate HTTP failure is sufficient to confirm validation and actual serialization.
    replies.add(new Reply(422, "{}", null));
    assertThrows(JevException.class, () -> client.evaluate(List.of("text"),
        Map.of("choice", JevQuestion.choice("Question", choiceCriteria(count)))));
    assertEquals(count, mapper.readTree(requests.remove().body())
        .at("/questions/choice/criteria").size());
  }

  @ParameterizedTest
  @ValueSource(ints = {2, 10})
  void acceptsScoreLevelBoundaries(int count) throws Exception {
    replies.add(new Reply(422, "{}", null));
    assertThrows(JevException.class, () -> client.evaluate("text",
        Map.of("score", JevQuestion.score("Question", Collections.nCopies(count, "level")))));
    assertEquals(count, mapper.readTree(requests.remove().body())
        .at("/questions/score/criteria").size());
  }

  @ParameterizedTest
  @ValueSource(ints = {401, 422, 500})
  void reportsHttpErrorsWithoutRetryingOrPuttingBodyInMessage(int status) {
    replies.add(new Reply(status, "{\"detail\":\"private-business-data\"}", null));
    JevException error = assertThrows(JevException.class, this::evaluateNoul);
    assertEquals(status, error.getStatusCode());
    assertTrue(error.getResponseBody().contains("private-business-data"));
    assertFalse(error.toString().contains("private-business-data"));
    assertEquals(1, requests.size());
    assertTrue(delays.isEmpty());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "null", "not-json", "{}",
      "{\"model\":\"jev\",\"answers\":{\"urgent\":{\"type\":\"unknown\"}}}"})
  void rejectsInvalidSuccessResponses(String body) {
    replies.add(new Reply(200, body, null));
    JevException error = assertThrows(JevException.class, this::evaluateNoul);
    assertEquals(200, error.getStatusCode());
    assertEquals(body, error.getResponseBody());
    assertEquals(1, requests.size());
  }

  @Test
  void rejectsMissingOrMismatchedAnswersAndUsage() {
    List<String> invalid = List.of(
        NOUL_RESPONSE.replace("urgent", "other"),
        NOUL_RESPONSE.replace("\"noul\":0.95", "\"noul\":1.2"),
        NOUL_RESPONSE.replace("\"noul\":0.95", "\"noul\":null"),
        NOUL_RESPONSE.replace("\"type\":\"noul\"", "\"type\":\"choice\""),
        NOUL_RESPONSE.replace("\"input_tokens\":296", "\"input_tokens\":null"),
        NOUL_RESPONSE.replace("\"output_tokens\":20", "\"output_tokens\":null"),
        NOUL_RESPONSE.replace("\"output_tokens\":20", "\"output_tokens\":-1"));
    for (String body : invalid) {
      replies.add(new Reply(200, body, null));
      assertThrows(JevException.class, this::evaluateNoul);
    }
  }

  @Test
  void retries429And529WithExponentialBackoffAndSameRequest() {
    replies.add(new Reply(429, "{}", null));
    replies.add(new Reply(529, "{}", "invalid"));
    replies.add(new Reply(200, NOUL_RESPONSE, null));
    evaluateNoul();
    assertEquals(List.of(1000L, 2000L), delays);
    List<CapturedRequest> captured = new ArrayList<>(requests);
    assertEquals(3, captured.size());
    assertEquals(captured.get(0), captured.get(1));
    assertEquals(captured.get(1), captured.get(2));
  }

  @ParameterizedTest
  @ValueSource(ints = {429, 529})
  void stopsAfterTwoRetriesAndPreservesLastFailure(int status) {
    for (int i = 0; i < 3; i++) {
      replies.add(new Reply(status, "attempt-" + i, "0"));
    }
    JevException error = assertThrows(JevException.class, this::evaluateNoul);
    assertEquals(status, error.getStatusCode());
    assertEquals("attempt-2", error.getResponseBody());
    assertEquals(3, requests.size());
    assertEquals(List.of(0L, 0L), delays);
  }

  @Test
  void honorsRetryAfterSecondsAndHttpDates() {
    replies.add(new Reply(429, "{}", "3"));
    replies.add(new Reply(529, "{}", "Wed, 21 Oct 2015 07:28:00 GMT"));
    replies.add(new Reply(200, NOUL_RESPONSE, null));
    evaluateNoul();
    assertEquals(List.of(3000L, 0L), delays);
    String future = ZonedDateTime.now().plusMinutes(1)
        .format(DateTimeFormatter.RFC_1123_DATE_TIME);
    long wait = JevClient.retryDelay(future, 1000);
    assertTrue(wait > 50000 && wait <= 60000);
    assertEquals(1000, JevClient.retryDelay("-1", 1000));
    assertEquals(1000, JevClient.retryDelay("99999999999999999999999", 1000));
  }

  @Test
  void networkFailureIsNotRetried() throws IOException {
    HttpRequestUtils http = mock(HttpRequestUtils.class);
    IOException failure = new IOException("network unavailable");
    when(http.httpPostJson(anyString(), anyString(), anyMap())).thenThrow(failure);
    JevClient failingClient = new JevClient(properties, http, delays::add);
    JevException error = assertThrows(JevException.class,
        () -> failingClient.evaluate("text", noulQuestions()));
    assertNull(error.getStatusCode());
    assertSame(failure, error.getCause());
    verify(http, times(1)).httpPostJson(anyString(), anyString(), anyMap());
    assertTrue(delays.isEmpty());
  }

  @Test
  void interruptionDuringBackoffStopsAndPreservesFlag() throws Exception {
    replies.add(new Reply(429, "{}", null));
    CountDownLatch waiting = new CountDownLatch(1);
    AtomicReference<Throwable> error = new AtomicReference<>();
    AtomicReference<Boolean> interrupted = new AtomicReference<>(false);
    JevClient waitingClient = new JevClient(properties, new HttpRequestUtils(), millis -> {
      waiting.countDown();
      Thread.sleep(30000);
    });
    Thread worker = new Thread(() -> {
      try {
        waitingClient.evaluate("text", noulQuestions());
      } catch (Throwable e) {
        error.set(e);
        interrupted.set(Thread.currentThread().isInterrupted());
      }
    });
    worker.start();
    try {
      assertTrue(waiting.await(5, TimeUnit.SECONDS));
      worker.interrupt();
      worker.join(5000);
      assertFalse(worker.isAlive());
      assertInstanceOf(JevException.class, error.get());
      assertTrue(interrupted.get());
      assertEquals(1, requests.size());
    } finally {
      worker.interrupt();
      worker.join(5000);
    }
  }

  @Test
  void preexistingInterruptionPreventsRequest() {
    Thread.currentThread().interrupt();
    assertThrows(JevException.class, this::evaluateNoul);
    assertTrue(Thread.currentThread().isInterrupted());
    assertTrue(requests.isEmpty());
  }

  @ParameterizedTest
  @ValueSource(strings = {"not-a-url", "ftp://example.com", "http://user:pass@example.com",
      "http://example.com?key=x", "http://example.com#fragment"})
  void rejectsInvalidBaseUrl(String url) {
    properties.setBaseUrl(url);
    assertThrows(IllegalArgumentException.class, this::evaluateNoul);
    assertTrue(requests.isEmpty());
  }

  private ApplicationContextRunner context(String yaml) {
    return new ApplicationContextRunner()
        .withInitializer(ctx -> {
          try {
            new YamlPropertySourceLoader()
                .load("jev-test", new ByteArrayResource(yaml.getBytes(StandardCharsets.UTF_8)))
                .forEach(source -> ctx.getEnvironment().getPropertySources().addFirst(source));
          } catch (IOException e) {
            throw new IllegalStateException(e);
          }
        })
        .withUserConfiguration(BindingConfiguration.class);
  }

  @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
  @org.springframework.boot.context.properties.EnableConfigurationProperties
  @org.springframework.context.annotation.Import({JevProperties.class, JevClient.class,
      HttpRequestUtils.class})
  static class BindingConfiguration {
  }

  private JevResponse evaluateNoul() {
    return client.evaluate("Help!", noulQuestions());
  }

  private Map<String, JevQuestion> noulQuestions() {
    return Map.of("urgent", JevQuestion.noul("Is this urgent?"));
  }

  private Map<String, String> choiceCriteria(int count) {
    Map<String, String> choices = new LinkedHashMap<>();
    for (int i = 0; i < count; i++) {
      choices.put("option-" + i, "Description " + i);
    }
    return choices;
  }

  private void assertInvalid(JevQuestion question) {
    assertThrows(IllegalArgumentException.class,
        () -> client.evaluate("text", Map.of("question", question)));
  }

  private record Reply(int status, String body, String retryAfter) {}

  private record CapturedRequest(String path, String method, String authorization,
      String contentType, String body) {}
}
