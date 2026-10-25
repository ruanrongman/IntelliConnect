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
import top.rslly.iot.utility.RedisUtil;
import top.rslly.iot.utility.ai.ConversationHistoryCodec;
import top.rslly.iot.utility.ai.ConversationMemoryPolicy;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.llm.FunctionResult;
import top.rslly.iot.utility.ai.llm.FunctionToolCallMessage;
import top.rslly.iot.utility.ai.llm.FunctionToolResultMessage;
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
  }

  @Test
  void memoryMaintenanceRunsBeforeRoutingAndNeverAfterToolExecution() {
    List<ModelMessage> previousTurns = new ArrayList<>();
    for (int index = 0; index < 5; index++) {
      previousTurns.add(new ModelMessage("user", "prior question", "prior-" + index));
      previousTurns.add(new ModelMessage("assistant", "prior answer", "prior-" + index));
    }
    when(redisUtil.get("memorychat")).thenReturn(previousTurns);
    ReflectionTestUtils.setField(router, "conversationMemoryPolicy",
        new ConversationMemoryPolicy(1, 0.5, 4));
    when(functionCallingRouterTool.run(anyString(), anyMap())).thenReturn(
        FunctionResult.toolCall("control-id", "route_control_electrical", "{}"));
    when(controlTool.run(anyString(), anyMap())).thenReturn("done");

    assertEquals(ToolPrefix.CONTROL.getPrefix() + "done", router.response("switch on", "chat", 1));

    var order = inOrder(longMemoryTool, memoryTool, functionCallingRouterTool, controlTool);
    order.verify(longMemoryTool).run(anyString(), anyMap());
    order.verify(memoryTool).run(anyList(), anyMap());
    order.verify(functionCallingRouterTool).run(anyString(), anyMap());
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
