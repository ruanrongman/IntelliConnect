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
package top.rslly.iot.utility.ai.chain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.LongConsumer;
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
import top.rslly.iot.models.HistoryMessageEntity;
import top.rslly.iot.services.UserConfigServiceImpl;
import top.rslly.iot.services.agent.AgentMemoryServiceImpl;
import top.rslly.iot.services.agent.HistoryMessageEntityService;
import top.rslly.iot.services.agent.LlmTurnUsageService;
import top.rslly.iot.utility.RedisUtil;
import top.rslly.iot.utility.ai.ConversationHistoryCodec;
import top.rslly.iot.utility.ai.ConversationMemoryPolicy;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.GlobalMessageContext;
import top.rslly.iot.utility.ai.llm.FunctionResult;
import top.rslly.iot.utility.ai.llm.FunctionToolCallMessage;
import top.rslly.iot.utility.ai.llm.FunctionToolResultMessage;
import top.rslly.iot.utility.ai.llm.usage.LlmUsage;
import top.rslly.iot.utility.ai.llm.usage.LlmUsageAccumulator;
import top.rslly.iot.utility.ai.llm.usage.LlmUsageContext;
import top.rslly.iot.utility.ai.llm.usage.LlmUsageSummary;
import top.rslly.iot.utility.ai.llm.usage.LlmUsageTracker;
import top.rslly.iot.utility.ai.tools.*;

@ExtendWith(MockitoExtension.class)
class RouterFunctionHistoryTest {
  @Mock
  private FunctionCallingRouterTool functionCallingRouterTool;
  @Mock
  private HistoryMessageEntityService historyMessageEntityService;
  @Mock
  private RedisUtil redisUtil;
  @Mock
  private UserConfigServiceImpl userConfigService;
  @Mock
  private AgentMemoryServiceImpl agentMemoryService;
  @Mock
  private LlmTurnUsageService llmTurnUsageService;
  @Mock
  private MusicTool musicTool;
  @Mock
  private ControlTool controlTool;
  @Mock
  private ScheduleTool scheduleTool;
  @Mock
  private ClassifierTool classifierTool;
  @Mock
  private ChatTool chatTool;
  @Mock
  private MemoryTool memoryTool;
  @Mock
  private LongMemoryTool longMemoryTool;
  @Mock
  private KnowledgeGraphicTool knowledgeGraphicTool;
  @InjectMocks
  private Router router;
  private final List<HistoryMessageEntity> events = new ArrayList<>();

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(router, "routerMode", "function");
    ReflectionTestUtils.setField(router, "conversationMemoryPolicy",
        new ConversationMemoryPolicy(128000, 0.5, 4));
    lenient().doAnswer(invocation -> {
      HistoryMessageEntity event = new HistoryMessageEntity();
      event.setRequestId(invocation.getArgument(1));
      event.setSequenceNum(invocation.getArgument(2));
      event.setMessageType(invocation.getArgument(3));
      event.setContent(invocation.getArgument(4));
      event.setTime(invocation.getArgument(5));
      events.add(event);
      return null;
    }).when(historyMessageEntityService).recordHistoryEvent(anyString(), anyString(), anyInt(),
        anyString(), nullable(String.class), anyLong());
  }

  @Test
  void persistsOrderedExchangeAndKeepsMusicAnswerInRedisAndDatabase() {
    String arguments = "{\"args\":\"play song\",\"original\":\" untouched \"}";
    when(functionCallingRouterTool.run(anyString(), anyMap())).thenReturn(
        FunctionResult.toolCall("real-call-1", "route_music", arguments, "assistant content",
            "reason"));
    when(musicTool.run(anyString(), anyMap())).thenAnswer(invocation -> {
      assertEquals(List.of("user", "assistant_tool_call"),
          events.stream().map(HistoryMessageEntity::getMessageType).toList());
      return Map.of("answer", "playing song", "url", "https://music.example/song.mp3");
    });

    String answer = router.response("enriched question", "original question", "chat", 1);

    assertEquals(ToolPrefix.MUSIC.getPrefix() + "playing songhttps://music.example/song.mp3",
        answer);
    assertEquals(List.of("user", "assistant_tool_call", "tool_result", "assistant"),
        events.stream().map(HistoryMessageEntity::getMessageType).toList());
    assertEquals(List.of(1, 2, 3, 4),
        events.stream().map(HistoryMessageEntity::getSequenceNum).toList());
    assertEquals(1, events.stream().map(HistoryMessageEntity::getRequestId).distinct().count());
    assertEquals("original question", events.getFirst().getContent());
    assertEquals(answer, events.getLast().getContent());
    List<ModelMessage> restored = ConversationHistoryCodec.restore(events);
    FunctionToolCallMessage call = assertInstanceOf(FunctionToolCallMessage.class,
        restored.get(1).getContent());
    FunctionToolResultMessage result = assertInstanceOf(FunctionToolResultMessage.class,
        restored.get(2).getContent());
    assertEquals("real-call-1", call.toolCallId());
    assertEquals(call.toolCallId(), result.toolCallId());
    assertEquals(arguments, call.arguments());
    assertEquals("assistant content", call.content());
    assertEquals("reason", call.reasoningContent());
    assertEquals("playing song", result.content());
    ArgumentCaptor<Object> cache = ArgumentCaptor.forClass(Object.class);
    verify(redisUtil).set(eq("memorychat"), cache.capture(), eq(48 * 3600L));
    assertEquals(restored, cache.getValue());
    verify(musicTool, times(1)).run(anyString(), anyMap());
    verify(functionCallingRouterTool, times(1)).run(anyString(), anyMap());
    verifyNoInteractions(classifierTool, chatTool, memoryTool, longMemoryTool,
        knowledgeGraphicTool);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void fullPromptBudgetControlsCompactionBeforeModelCall(boolean aboveThreshold) {
    List<ModelMessage> history = new ArrayList<>();
    // Local history estimate disagrees with the model-aware budget in both directions.
    String text = aboveThreshold ? "short" : "字".repeat(6000);
    for (int turn = 1; turn <= 6; turn++) {
      history.add(new ModelMessage("user", text, "r" + turn));
      history.add(new ModelMessage("assistant", text, "r" + turn));
    }
    when(redisUtil.get("memorychat")).thenReturn(history);
    when(functionCallingRouterTool.run(anyString(), anyMap())).thenAnswer(invocation -> {
      Map<String, Object> context = invocation.getArgument(1);
      ((LongConsumer) context.get(GlobalMessageContext.PROMPT_BUDGET))
          .accept(aboveThreshold ? 100000 : 10);
      List<ModelMessage> retained = (List<ModelMessage>) context.get(GlobalMessageContext.MEMORY);
      assertEquals(aboveThreshold ? 8 : 12, retained.size());
      return FunctionResult.directReply("answer");
    });

    assertEquals("answer", router.response("next question", "chat", 1));
    if (aboveThreshold) {
      verify(memoryTool).run(anyList(), anyMap());
    } else {
      verifyNoInteractions(memoryTool);
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4})
  void historyFailureDoesNotRetryExecutedRoute(int failedSequence) {
    doAnswer(invocation -> {
      if ((int) invocation.getArgument(2) == failedSequence) {
        throw new IllegalStateException("history unavailable");
      }
      return null;
    }).when(historyMessageEntityService).recordHistoryEvent(anyString(), anyString(), anyInt(),
        anyString(), nullable(String.class), anyLong());
    when(functionCallingRouterTool.run(anyString(), anyMap())).thenReturn(
        FunctionResult.toolCall("real-control-id", "route_control_electrical", "{}"));
    when(controlTool.run(anyString(), anyMap())).thenReturn("switched on");
    when(redisUtil.set(anyString(), any(), anyLong()))
        .thenThrow(new IllegalStateException("redis"));

    assertEquals(ToolPrefix.CONTROL.getPrefix() + "switched on",
        router.response("switch on", "chat", 1));

    verify(controlTool, times(1)).run(anyString(), anyMap());
    verify(functionCallingRouterTool, times(1)).run(anyString(), anyMap());
    verifyNoInteractions(classifierTool, chatTool, memoryTool, longMemoryTool);
  }

  @Test
  void executionExceptionRecordsActualErrorWithoutFallbackOrRetry() {
    when(functionCallingRouterTool.run(anyString(), anyMap())).thenReturn(
        FunctionResult.toolCall("failed-call", "route_control_electrical", "{}"));
    when(controlTool.run(anyString(), anyMap()))
        .thenThrow(new IllegalStateException("device rejected command"));

    String answer = router.response("switch on", "chat", 1);

    assertTrue(answer.contains("device rejected command"));
    assertEquals(4, events.size());
    List<ModelMessage> restored = ConversationHistoryCodec.restore(events);
    FunctionToolResultMessage result = (FunctionToolResultMessage) restored.get(2).getContent();
    assertEquals("failed-call", result.toolCallId());
    assertTrue(result.content().contains("java.lang.IllegalStateException"));
    assertTrue(result.content().contains("device rejected command"));
    assertEquals(answer, restored.getLast().getContent());
    verify(controlTool, times(1)).run(anyString(), anyMap());
    verifyNoInteractions(classifierTool, chatTool);
  }

  @Test
  void directReplyOnlySavesOrdinaryConversation() {
    when(functionCallingRouterTool.run(anyString(), anyMap()))
        .thenReturn(FunctionResult.directReply("hello"));

    assertEquals("hello", router.response("hi", "chat", 1));

    assertTrue(events.isEmpty());
    verify(historyMessageEntityService).recordHistoryMessage(eq("chat"), anyString(), eq("hi"),
        eq("hello"));
    verifyNoInteractions(controlTool, musicTool, classifierTool, chatTool);
    verifyNoInteractions(llmTurnUsageService);
  }

  @ParameterizedTest
  @ValueSource(strings = {"chatProduct1device", "openid"})
  void recordsVoiceAndWechatUsageOnTheRouterWorkerWithHistoryRequestId(String chatId)
      throws Exception {
    String[] args = "openid".equals(chatId) ? new String[] {"wx-app"} : new String[0];
    String storedChatId = "openid".equals(chatId) ? "wx-appopenid" : chatId;
    when(functionCallingRouterTool.run(anyString(), anyMap())).thenAnswer(invocation -> {
      assertEquals(storedChatId, LlmUsageContext.current().chatId());
      assertEquals(1, LlmUsageContext.current().productId());
      reportUsage(new LlmUsage(1200, 500, 0, 80, 0));
      return FunctionResult.directReply("answer");
    });

    assertEquals("answer", CompletableFuture.supplyAsync(
        () -> router.response("hello", chatId, 1, args)).get(5, TimeUnit.SECONDS));

    ArgumentCaptor<String> requestId = ArgumentCaptor.forClass(String.class);
    verify(historyMessageEntityService).recordHistoryMessage(eq(storedChatId), requestId.capture(),
        eq("hello"), eq("answer"));
    ArgumentCaptor<LlmUsageSummary> summary = ArgumentCaptor.forClass(LlmUsageSummary.class);
    verify(llmTurnUsageService).record(eq(1), eq(storedChatId), eq(requestId.getValue()),
        summary.capture());
    assertEquals(1, summary.getValue().getCalls());
    assertEquals(1200, summary.getValue().getPromptTokens());
    assertEquals(500, summary.getValue().getCachedTokens());
    assertEquals(80, summary.getValue().getCompletionTokens());
    assertEquals(128000, summary.getValue().getContextWindowTokens());
    assertNull(LlmUsageContext.current());
  }

  @Test
  void persistsOnceWhileOuterSseAccumulatorStillReceivesUsage() {
    LlmUsageAccumulator sse = new LlmUsageAccumulator();
    when(functionCallingRouterTool.run(anyString(), anyMap())).thenAnswer(invocation -> {
      reportUsage(new LlmUsage(900, 0, 0, 30, 0));
      return FunctionResult.directReply("answer");
    });

    try (LlmUsageContext.Scope ignored = LlmUsageContext.open(1, "conversation", sse)) {
      assertEquals("answer", router.responseDebugStream("hello", "hello", "stream",
          "conversation", 1, new java.util.concurrent.ConcurrentLinkedQueue<>()));
    }

    assertEquals(1, sse.summary().getCalls());
    assertEquals(900, sse.summary().getPromptTokens());
    verify(llmTurnUsageService, times(1)).record(eq(1), eq("conversation"), eq(sse.requestId()),
        any(LlmUsageSummary.class));
    assertNull(LlmUsageContext.current());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void recordsSpentCallsEvenWhenRouterFailsOrProviderOmitsUsage(boolean missingUsage) {
    when(functionCallingRouterTool.run(anyString(), anyMap())).thenAnswer(invocation -> {
      reportUsage(missingUsage ? null : new LlmUsage(700, 0, 0, 20, 0));
      throw new IllegalStateException("model failed after spending tokens");
    });

    assertThrows(IllegalStateException.class, () -> router.response("hello", "chat", 1));

    ArgumentCaptor<LlmUsageSummary> summary = ArgumentCaptor.forClass(LlmUsageSummary.class);
    verify(llmTurnUsageService).record(eq(1), eq("chat"), anyString(), summary.capture());
    assertEquals(1, summary.getValue().getCalls());
    assertEquals(missingUsage ? 0 : 1, summary.getValue().getReportedCalls());
    assertNull(LlmUsageContext.current());
  }

  private static void reportUsage(LlmUsage usage) {
    LlmUsageTracker.begin("test", "model", List.of(new ModelMessage("user", "hello")), false)
        .finish(usage);
  }

  @Test
  void memoryMaintenanceRunsAtPromptBudgetCheckAndNeverAfterToolExecution() {
    List<ModelMessage> previousTurns = new ArrayList<>();
    for (int index = 0; index < 5; index++) {
      previousTurns.add(new ModelMessage("user", "prior question", "prior-" + index));
      previousTurns.add(new ModelMessage("assistant", "prior answer", "prior-" + index));
    }
    when(redisUtil.get("memorychat")).thenReturn(previousTurns);
    ReflectionTestUtils.setField(router, "conversationMemoryPolicy",
        new ConversationMemoryPolicy(1, 0.5, 4));
    when(functionCallingRouterTool.run(anyString(), anyMap())).thenAnswer(invocation -> {
      Map<String, Object> context = invocation.getArgument(1);
      ((LongConsumer) context.get(GlobalMessageContext.PROMPT_BUDGET)).accept(100000);
      return FunctionResult.toolCall("control-id", "route_control_electrical", "{}");
    });
    when(controlTool.run(anyString(), anyMap())).thenReturn("done");

    assertEquals(ToolPrefix.CONTROL.getPrefix() + "done", router.response("switch on", "chat", 1));

    var order = inOrder(longMemoryTool, memoryTool, functionCallingRouterTool, controlTool);
    order.verify(longMemoryTool).run(anyString(), anyMap());
    order.verify(functionCallingRouterTool).run(anyString(), anyMap());
    order.verify(memoryTool).run(anyList(), anyMap());
    order.verify(controlTool).run(anyString(), anyMap());
    order.verifyNoMoreInteractions();
    verifyNoInteractions(chatTool, classifierTool);
  }

  @ParameterizedTest
  @ValueSource(strings = {"unsupported", "error", "unknown"})
  void fallbackKeepsLegacySemanticsWithoutInventingProtocolEvents(String type) {
    FunctionResult result = switch (type) {
      case "unsupported" -> FunctionResult.unsupported();
      case "error" -> FunctionResult.error("provider failed");
      default -> FunctionResult.toolCall("unknown-id", "unknown_function", "{}");
    };
    when(functionCallingRouterTool.run(anyString(), anyMap())).thenReturn(result);
    when(classifierTool.run(anyString(), anyMap()))
        .thenReturn(Map.of("value", List.of("2"), "args", ""));
    when(controlTool.run(anyString(), anyMap())).thenReturn("legacy result");

    String answer = router.response("switch on", "chat", 1);

    assertEquals(ToolPrefix.CONTROL.getPrefix() + "legacy result", answer);
    assertTrue(events.isEmpty());
    verify(historyMessageEntityService).recordHistoryMessage(eq("chat"), anyString(),
        eq("switch on"), eq(answer));
    verify(controlTool, times(1)).run(anyString(), anyMap());
  }

  @Test
  void blockedScheduleDoesNotInventAnExecutedCall() {
    when(functionCallingRouterTool.run(anyString(), anyMap())).thenReturn(
        FunctionResult.toolCall("schedule-id", "route_schedule", "{}"));

    String answer = router.response("schedule", "chat", 1, "false");

    assertTrue(answer.contains("定时任务禁止递归调用"));
    assertTrue(events.isEmpty());
    verifyNoInteractions(scheduleTool);
    verify(historyMessageEntityService).recordHistoryMessage(eq("chat"), anyString(),
        eq("schedule"), eq(answer));
  }
}
