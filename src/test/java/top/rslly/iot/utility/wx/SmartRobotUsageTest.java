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
package top.rslly.iot.utility.wx;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import top.rslly.iot.models.WxProductActiveEntity;
import top.rslly.iot.models.WxProductBindEntity;
import top.rslly.iot.models.WxUserEntity;
import top.rslly.iot.services.AdminConfigServiceImpl;
import top.rslly.iot.services.agent.LlmTurnUsageService;
import top.rslly.iot.services.wechat.WxProductActiveServiceImpl;
import top.rslly.iot.services.wechat.WxProductBindServiceImpl;
import top.rslly.iot.services.wechat.WxUserServiceImpl;
import top.rslly.iot.utility.RedisUtil;
import top.rslly.iot.utility.ai.ConversationMemoryPolicy;
import top.rslly.iot.utility.ai.llm.LLM;
import top.rslly.iot.utility.ai.llm.LLMFactory;
import top.rslly.iot.utility.ai.llm.usage.LlmUsage;
import top.rslly.iot.utility.ai.llm.usage.LlmUsageContext;
import top.rslly.iot.utility.ai.llm.usage.LlmUsageSummary;
import top.rslly.iot.utility.ai.llm.usage.LlmUsageTracker;

@ExtendWith(MockitoExtension.class)
class SmartRobotUsageTest {
  @Mock
  private DealWx dealWx;
  @Mock
  private WxUserServiceImpl wxUserService;
  @Mock
  private WxProductActiveServiceImpl productActiveService;
  @Mock
  private WxProductBindServiceImpl wxProductBindService;
  @Mock
  private AdminConfigServiceImpl adminConfigService;
  @Mock
  private LlmTurnUsageService llmTurnUsageService;
  @Mock
  private RedisUtil redisUtil;
  @Mock
  private LLM llm;
  @InjectMocks
  private SmartRobot smartRobot;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(smartRobot, "smartRobotLLm", "vision-model");
    ReflectionTestUtils.setField(smartRobot, "conversationMemoryPolicy",
        new ConversationMemoryPolicy(128000, 0.5, 4));
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void recordsImageUsageForTheWechatProductAndAppScopedConversation(boolean boundFallback)
      throws Exception {
    when(wxUserService.findAllByAppidAndOpenid("app", "user"))
        .thenReturn(List.of(new WxUserEntity()));
    if (boundFallback) {
      WxProductBindEntity bound = new WxProductBindEntity();
      bound.setProductId(7);
      when(wxProductBindService.findAllByAppidAndOpenid("app", "user"))
          .thenReturn(List.of(bound));
    } else {
      WxProductActiveEntity active = new WxProductActiveEntity();
      active.setProductId(7);
      when(productActiveService.findAllByAppidAndOpenid("app", "user"))
          .thenReturn(List.of(active));
    }
    when(llm.imageToWord("图里有什么", "https://example.test/image.png"))
        .thenAnswer(invocation -> {
          assertEquals(7, LlmUsageContext.current().productId());
          assertEquals("appuser", LlmUsageContext.current().chatId());
          LlmUsageTracker.begin("test", "vision-model", null, false)
              .finish(new LlmUsage(800, 100, 0, 60, 0));
          return "a picture";
        });
    try (var factory = mockStatic(LLMFactory.class)) {
      factory.when(() -> LLMFactory.getLLM("vision-model")).thenReturn(llm);

      smartRobot.smartImageSendContent("user", "https://example.test/image.png", "app");
    }

    ArgumentCaptor<String> requestId = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<LlmUsageSummary> summary = ArgumentCaptor.forClass(LlmUsageSummary.class);
    verify(llmTurnUsageService).record(eq(7), eq("appuser"), requestId.capture(),
        summary.capture());
    assertNotNull(UUID.fromString(requestId.getValue()));
    assertEquals(1, summary.getValue().getCalls());
    assertEquals(800, summary.getValue().getPromptTokens());
    assertEquals(100, summary.getValue().getCachedTokens());
    assertEquals(60, summary.getValue().getCompletionTokens());
    assertEquals(128000, summary.getValue().getContextWindowTokens());
    verify(dealWx).sendContent("user", "a picture", "app");
    assertNull(LlmUsageContext.current());
  }

  @Test
  void retainsUsageWhenImageCallFailsAfterSpendingTokens() {
    when(wxUserService.findAllByAppidAndOpenid("app", "user"))
        .thenReturn(List.of(new WxUserEntity()));
    WxProductActiveEntity active = new WxProductActiveEntity();
    active.setProductId(7);
    when(productActiveService.findAllByAppidAndOpenid("app", "user"))
        .thenReturn(List.of(active));
    when(llm.imageToWord(anyString(), anyString())).thenAnswer(invocation -> {
      LlmUsageTracker.begin("test", "vision-model", null, false)
          .finish(new LlmUsage(800, 0, 0, 0, 0));
      throw new IllegalStateException("image failed");
    });
    try (var factory = mockStatic(LLMFactory.class)) {
      factory.when(() -> LLMFactory.getLLM("vision-model")).thenReturn(llm);

      assertThrows(IllegalStateException.class,
          () -> smartRobot.smartImageSendContent("user", "image", "app"));
    }

    verify(llmTurnUsageService).record(eq(7), eq("appuser"), anyString(),
        any(LlmUsageSummary.class));
    verifyNoInteractions(dealWx, redisUtil);
    assertNull(LlmUsageContext.current());
  }

  @Test
  void unregisteredImageDoesNotInvokeTheModelOrRecordUsage() throws Exception {
    smartRobot.smartImageSendContent("user", "image", "app");

    verifyNoInteractions(llmTurnUsageService, llm, productActiveService, wxProductBindService);
    verify(dealWx).sendContent(eq("user"), anyString(), eq("app"));
  }
}
