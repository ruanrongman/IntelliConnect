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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import okhttp3.HttpUrl;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.rslly.iot.utility.HttpRequestUtils;

/** Synchronous TypeSafe evaluation client; configuration is checked only when called. */
@Component
public class JevClient {
  private final JevProperties properties;
  private final HttpRequestUtils http;
  private final Sleeper sleeper;
  private final ObjectMapper mapper = new ObjectMapper()
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  @Autowired
  public JevClient(JevProperties properties, HttpRequestUtils http) {
    this(properties, http, Thread::sleep);
  }

  JevClient(JevProperties properties, HttpRequestUtils http, Sleeper sleeper) {
    this.properties = properties;
    this.http = http;
    this.sleeper = sleeper;
  }

  @FunctionalInterface
  interface Sleeper {
    void sleep(long millis) throws InterruptedException;
  }

  public JevResponse evaluate(Object state, Map<String, JevQuestion> questions) {
    checkInterrupted();
    String key = requiredConfig(properties.getKey(), "ai.decision-model.key");
    String model = requiredConfig(properties.getModel(), "ai.decision-model.model");
    String url = endpoint(requiredConfig(properties.getBaseUrl(), "ai.decision-model.base-url"));
    ObjectNode request = buildRequest(state, questions, model);
    String json = request.toString();
    for (int attempt = 0; attempt < 3; attempt++) {
      checkInterrupted();
      long delay;
      try (Response response = http.httpPostJson(url, json,
          Map.of("Authorization", "Bearer " + key, "Accept", "application/json"))) {
        String body = response.body() == null ? "" : response.body().string();
        checkInterrupted();
        if (response.isSuccessful()) {
          return parseResponse(body, response.code(), request.get("questions"));
        }
        if ((response.code() != 429 && response.code() != 529) || attempt == 2) {
          throw new JevException("Jev HTTP request failed: " + response.code(),
              response.code(), body, null);
        }
        delay = retryDelay(response.header("Retry-After"), 1000L << attempt);
      } catch (IOException e) {
        checkInterrupted();
        throw new JevException("Jev network request failed", null, null, e);
      }
      // Release the response/connection before backing off.
      try {
        sleeper.sleep(delay);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new JevException("Jev request interrupted", null, null, e);
      }
    }
    throw new IllegalStateException("Unreachable retry state");
  }

  private ObjectNode buildRequest(Object state, Map<String, JevQuestion> questions, String model) {
    require(questions != null && !questions.isEmpty(), "questions must not be empty");
    for (var entry : questions.entrySet()) {
      require(entry.getKey() != null && !entry.getKey().isBlank(), "Question id is required");
      require(entry.getValue() != null, "Question must not be null");
    }
    ObjectNode request = mapper.createObjectNode();
    request.set("state", mapper.valueToTree(state));
    request.put("model", model);
    request.set("questions", mapper.valueToTree(questions));
    validateStructured(request.get("state"), "state");
    request.get("questions").fields().forEachRemaining(entry -> {
      JsonNode question = entry.getValue();
      validateStructured(question.get("instructions"), "instructions");
      JsonNode criteria = question.get("criteria");
      switch (question.path("type").asText()) {
        case "noul" -> {
          if (criteria != null) {
            require(criteria.isObject(), "noul criteria must be an object");
            criteria.fields().forEachRemaining(item -> {
              require("true".equals(item.getKey()) || "false".equals(item.getKey()),
                  "noul criteria keys must be true or false");
              validateStructured(item.getValue(), "noul criterion");
            });
          }
        }
        case "choice" -> {
          require(criteria != null && criteria.isObject() && criteria.size() >= 1
              && criteria.size() <= 255, "choice requires 1 to 255 options");
          criteria.fields().forEachRemaining(item -> {
            if (!item.getValue().isNull()) {
              validateStructured(item.getValue(), "choice criterion");
            }
          });
        }
        case "score" -> {
          require(criteria != null && criteria.isArray() && criteria.size() >= 2
              && criteria.size() <= 10, "score requires 2 to 10 levels");
          criteria.forEach(item -> validateStructured(item, "score criterion"));
        }
        default -> throw new IllegalArgumentException("Unsupported question type");
      }
    });
    return request;
  }

  private JevResponse parseResponse(String body, int status, JsonNode questions) {
    try {
      JsonNode responseJson = mapper.readTree(body);
      JsonNode usageJson = responseJson == null ? null : responseJson.get("usage");
      if (usageJson instanceof ObjectNode usage && !usage.has("output_tokens")) {
        // Decision-only providers such as Bailian do not generate text and omit this field.
        usage.put("output_tokens", 0);
      }
      JevResponse result = mapper.treeToValue(responseJson, JevResponse.class);
      require(result != null && result.model() != null && !result.model().isBlank(),
          "Missing response model");
      require(result.answers() != null && result.answers().size() == questions.size(),
          "Missing answers");
      require(result.usage() != null && result.usage().inputTokens() != null
          && result.usage().outputTokens() != null && result.usage().inputTokens() >= 0
          && result.usage().outputTokens() >= 0, "Missing or invalid usage");
      questions.fields().forEachRemaining(entry -> {
        JevResponse.Answer answer = result.answers().get(entry.getKey());
        JsonNode question = entry.getValue();
        switch (question.path("type").asText()) {
          case "noul" -> require(answer instanceof JevResponse.NoulAnswer value
              && probability(value.noul()), "Invalid noul answer");
          case "choice" -> {
            require(answer instanceof JevResponse.ChoiceAnswer, "Invalid choice answer type");
            var value = (JevResponse.ChoiceAnswer) answer;
            require(value.choice() != null && question.get("criteria").has(value.choice())
                && probability(value.confidence()), "Invalid choice answer");
            validateProbabilities(value.probabilities(), question.get("criteria").size());
            question.get("criteria").fieldNames()
                .forEachRemaining(option -> require(value.probabilities().containsKey(option),
                    "Missing choice probability"));
          }
          case "score" -> {
            require(answer instanceof JevResponse.ScoreAnswer, "Invalid score answer type");
            var value = (JevResponse.ScoreAnswer) answer;
            int levels = question.get("criteria").size();
            require(value.score() != null && Double.isFinite(value.score())
                && value.score() >= 0 && value.score() <= levels - 1
                && probability(value.confidence()) && value.legend() != null
                && value.legend().size() == levels, "Invalid score answer");
            validateProbabilities(value.probabilities(), levels);
            for (int i = 0; i < levels; i++) {
              String index = Integer.toString(i);
              require(value.probabilities().containsKey(index) && value.legend().get(index) != null,
                  "Missing score level");
            }
          }
          default -> throw new IllegalArgumentException("Unsupported answer type");
        }
      });
      return result;
    } catch (IOException | IllegalArgumentException e) {
      // Parser exceptions may embed response text; expose it only through the explicit accessor.
      throw new JevException("Invalid Jev response", status, body, null);
    }
  }

  private static void validateProbabilities(Map<String, Double> values, int size) {
    require(values != null && values.size() == size
        && values.values().stream().allMatch(JevClient::probability), "Invalid probabilities");
  }

  private static boolean probability(Double value) {
    return value != null && Double.isFinite(value) && value >= 0 && value <= 1;
  }

  private static void validateStructured(JsonNode value, String field) {
    require(value != null && (value.isObject() || value.isArray()
        || (value.isTextual() && !value.textValue().isBlank())),
        field + " must be a string, object or array");
  }

  private static void require(boolean condition, String message) {
    if (!condition) {
      throw new IllegalArgumentException(message);
    }
  }

  private static String requiredConfig(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalStateException(name + " must be configured before calling Jev");
    }
    return value.trim();
  }

  private static String endpoint(String baseUrl) {
    HttpUrl url = HttpUrl.parse(baseUrl);
    require(url != null && url.username().isEmpty() && url.password().isEmpty()
        && url.query() == null && url.fragment() == null, "Invalid ai.decision-model.base-url");
    String path = url.encodedPath().replaceAll("/+$", "");
    if (!path.endsWith("/v1")) {
      path += "/v1";
    }
    return url.newBuilder().encodedPath(path + "/systemone").build().toString();
  }

  static long retryDelay(String header, long fallback) {
    if (header == null || header.isBlank()) {
      return fallback;
    }
    String value = header.trim();
    try {
      if (value.matches("[0-9]+")) {
        return Math.multiplyExact(Long.parseLong(value), 1000);
      }
      return Math.max(0, Duration.between(ZonedDateTime.now(),
          ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME)).toMillis());
    } catch (DateTimeParseException | ArithmeticException | NumberFormatException e) {
      return fallback;
    }
  }


  private static void checkInterrupted() {
    if (Thread.currentThread().isInterrupted()) {
      throw new JevException("Jev request interrupted", null, null, null);
    }
  }
}
