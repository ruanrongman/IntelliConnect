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
package top.rslly.iot.utility.ai.tools;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;
import top.rslly.iot.services.agent.AgentMemoryServiceImpl;
import top.rslly.iot.utility.EmotionManager;
import top.rslly.iot.utility.ai.jev.JevClient;
import top.rslly.iot.utility.ai.jev.JevException;
import top.rslly.iot.utility.ai.jev.JevResponse;
import top.rslly.iot.utility.ai.prompts.EmotionToolPrompt;

class EmotionToolTest {
  private final JevClient jevClient = mock(JevClient.class);
  private final EmotionTool tool = new EmotionTool();
  private final Logger logger = (Logger) LoggerFactory.getLogger(EmotionTool.class);
  private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
  private boolean previousAdditive;

  @BeforeEach
  void setUp() {
    AgentMemoryServiceImpl memoryService = mock(AgentMemoryServiceImpl.class);
    when(memoryService.findAllByChatId("test-chat")).thenReturn(List.of());
    tool.setAgentMemoryService(memoryService);
    tool.setJevClient(jevClient);
    tool.setEmotionToolPrompt(new EmotionToolPrompt());
    tool.setLlmName("decision-model");
    previousAdditive = logger.isAdditive();
    logger.setAdditive(false);
    logs.start();
    logger.addAppender(logs);
  }

  @AfterEach
  void tearDown() {
    Thread.interrupted();
    logger.detachAppender(logs);
    logger.setAdditive(previousAdditive);
    logs.stop();
  }

  @Test
  void validChoiceReturnsMatchingEmotionAndEmoji() {
    when(jevClient.evaluate(any(), anyMap())).thenReturn(choiceResponse("happy"));
    assertEquals(Map.of("text", "happy", "emoji", EmotionManager.getCurrentEmotion("happy")),
        run());
    assertTrue(logs.list.stream().noneMatch(event -> event.getLevel() == Level.ERROR));
  }

  @Test
  void networkFailureReturnsNeutralAndLogsCause() {
    when(jevClient.evaluate(any(), anyMap())).thenThrow(new JevException(
        "Jev network request failed", null, null, new SocketTimeoutException("Read timed out")));
    assertNeutralAndError();
    var error = lastError();
    assertTrue(error.getFormattedMessage().contains("test-chat"));
    assertEquals(SocketTimeoutException.class.getName(), error.getThrowableProxy().getCause()
        .getClassName());
    verify(jevClient, times(1)).evaluate(any(), anyMap());
  }

  @Test
  void serviceFailureLogsStatusWithoutRawResponseBody() {
    when(jevClient.evaluate(any(), anyMap())).thenThrow(new JevException(
        "Jev HTTP request failed: 429", 429, "private-response-body", null));
    assertNeutralAndError();
    assertTrue(lastError().getFormattedMessage().contains("statusCode=429"));
    assertFalse(lastError().getFormattedMessage().contains("private-response-body"));
    assertFalse(lastError().getThrowableProxy().getMessage().contains("private-response-body"));
  }

  @Test
  void configurationFailureReturnsNeutral() {
    when(jevClient.evaluate(any(), anyMap())).thenThrow(
        new IllegalStateException("ai.decision-model.key must be configured before calling Jev"));
    assertNeutralAndError();
  }

  @Test
  void interruptedRequestKeepsInterruptFlagAndReturnsNeutral() {
    when(jevClient.evaluate(any(), anyMap())).thenAnswer(invocation -> {
      Thread.currentThread().interrupt();
      throw new JevException("Jev request interrupted", null, null, null);
    });
    assertNeutralAndError();
    assertTrue(Thread.currentThread().isInterrupted());
  }

  @ParameterizedTest
  @MethodSource("invalidResponses")
  void invalidResponseNeverOverwritesNeutralFallback(JevResponse response) {
    when(jevClient.evaluate(any(), anyMap())).thenReturn(response);
    assertNeutralAndError();
  }

  static Stream<JevResponse> invalidResponses() {
    return Stream.of(null,
        new JevResponse("jev", null, null),
        new JevResponse("jev", Map.of(), null),
        new JevResponse("jev", Map.of("emotion", new JevResponse.NoulAnswer(0.5)), null),
        choiceResponse(null), choiceResponse(""), choiceResponse(" "), choiceResponse("unknown"));
  }

  private static JevResponse choiceResponse(String choice) {
    return new JevResponse("jev", Map.of("emotion",
        new JevResponse.ChoiceAnswer(choice, Map.of(), 0.9)), null);
  }

  private Map<String, String> run() {
    return tool.run("讲个开心的故事", Map.of("chatId", "test-chat"));
  }

  private void assertNeutralAndError() {
    assertEquals(Map.of("text", "neutral", "emoji", EmotionManager.getCurrentEmotion("neutral")),
        run());
    assertNotNull(lastError().getThrowableProxy());
  }

  private ILoggingEvent lastError() {
    return logs.list.stream().filter(event -> event.getLevel() == Level.ERROR)
        .reduce((first, second) -> second).orElseThrow();
  }
}
