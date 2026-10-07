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

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import top.rslly.iot.utility.HttpRequestUtils;

/** Opt-in smoke test: makes one evaluation call using application.yaml and its overrides. */
@Tag("live")
@EnabledIfSystemProperty(named = "jev.live", matches = "true")
class JevClientLiveTest {

  @Test
  @Timeout(90)
  void evaluatesAllQuestionTypesAgainstConfiguredService() {
    // Load normal Spring configuration, but avoid starting databases, MQTT or other app services.
    new ApplicationContextRunner()
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .withUserConfiguration(LiveConfiguration.class)
        .run(context -> {
          assertNull(context.getStartupFailure(), "Jev test configuration failed to start");
          JevProperties properties = context.getBean(JevProperties.class);
          assertTrue(properties.getKey() != null && !properties.getKey().isBlank(),
              "Configure ai.decision-model.key in application.yaml or set JEV_API_KEY before the live test");

          JevResponse result = context.getBean(JevClient.class).evaluate(
              Map.of("ticket", "My payment has failed for three days. Please fix my billing "
                  + "issue urgently. I am very frustrated."),
              Map.of(
                  "urgent", JevQuestion.noul("Does this customer need urgent assistance?"),
                  "department", JevQuestion.choice("Which department should handle this ticket?",
                      Map.of("billing", "Payments, invoices and refunds",
                          "technical", "Software bugs and integration problems")),
                  "frustration", JevQuestion.score("How frustrated is the customer?",
                      List.of("Calm", "Frustrated", "Very angry"))));

          assertNotNull(result);
          assertFalse(result.model().isBlank());
          assertEquals(Set.of("urgent", "department", "frustration"), result.answers().keySet());
          var urgent = assertInstanceOf(JevResponse.NoulAnswer.class,
              result.answers().get("urgent"));
          assertTrue(urgent.noul() >= 0 && urgent.noul() <= 1);
          var department = assertInstanceOf(JevResponse.ChoiceAnswer.class,
              result.answers().get("department"));
          assertTrue(Set.of("billing", "technical").contains(department.choice()));
          assertEquals(Set.of("billing", "technical"), department.probabilities().keySet());
          var frustration = assertInstanceOf(JevResponse.ScoreAnswer.class,
              result.answers().get("frustration"));
          assertTrue(frustration.score() >= 0 && frustration.score() <= 2);
          assertEquals(Set.of("0", "1", "2"), frustration.legend().keySet());
          assertTrue(result.usage().inputTokens() > 0);
          assertTrue(result.usage().outputTokens() >= 0);

          // Only report model/usage metadata; never log credentials, requests or raw responses.
          System.out.printf("Jev live test passed: model=%s, answers=%d, inputTokens=%d, "
              + "outputTokens=%d%n", result.model(), result.answers().size(),
              result.usage().inputTokens(), result.usage().outputTokens());
        });
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(JevProperties.class)
  @Import({JevClient.class, HttpRequestUtils.class})
  static class LiveConfiguration {
  }
}
