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
package top.rslly.iot.services.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import top.rslly.iot.models.ProductEntity;
import top.rslly.iot.param.request.AgentMemory;
import top.rslly.iot.param.request.AiControl;
import top.rslly.iot.services.SafetyServiceImpl;
import top.rslly.iot.services.thingsModel.ProductServiceImpl;
import top.rslly.iot.utility.ai.chain.Router;
import top.rslly.iot.utility.result.JsonResult;

@ExtendWith(MockitoExtension.class)
class AiServiceImplTest {
  private static final String CHAT_ID =
      "chatProduct1debug550e8400-e29b-41d4-a716-446655440000";

  @Mock
  private Router router;
  @Mock
  private ProductServiceImpl productService;
  @Mock
  private SafetyServiceImpl safetyService;
  @Mock
  private AgentMemoryServiceImpl agentMemoryService;
  @InjectMocks
  private AiServiceImpl aiService;

  @Test
  void routesDebugConversationAndInitializesItsMemory() {
    AiControl aiControl = new AiControl();
    aiControl.setProductId(1);
    aiControl.setChatId(CHAT_ID);
    aiControl.setContent("第一句话");

    when(safetyService.controlAuthorizeProduct("token", 1)).thenReturn(true);
    when(productService.findAllById(1)).thenReturn(List.of(new ProductEntity()));
    when(agentMemoryService.isChatIdValidForProduct(1, CHAT_ID)).thenReturn(true);
    when(agentMemoryService.findAllByChatId(CHAT_ID)).thenReturn(List.of());
    when(router.response("第一句话", CHAT_ID, 1)).thenReturn("完成");

    JsonResult<?> result = aiService.getAiResponse(aiControl, "token");

    assertEquals(200, result.getErrorCode());
    assertEquals("完成", result.getData());
    ArgumentCaptor<AgentMemory> memoryCaptor = ArgumentCaptor.forClass(AgentMemory.class);
    verify(agentMemoryService).insertAndUpdate(memoryCaptor.capture());
    assertEquals(CHAT_ID, memoryCaptor.getValue().getChatId());
    assertEquals("第一句话", memoryCaptor.getValue().getContent());
    verify(router).response("第一句话", CHAT_ID, 1);
  }

  @Test
  void streamsReasoningSeparatelyAndCleansQueuesOnCompletion() throws Exception {
    AiControl aiControl = validStreamControl();
    String streamId = "completion";
    String streamChatId = streamChatId(streamId);
    SseEmitter emitter = org.mockito.Mockito.mock(SseEmitter.class);

    doAnswer(invocation -> {
      Queue<String> reasoningQueue = invocation.getArgument(5);
      reasoningQueue.add("模型思考");
      Router.queueMap.put(streamChatId,
          new ConcurrentLinkedQueue<>(List.of("正文", "[DONE]")));
      return "正文";
    }).when(router).responseDebugStream(any(), any(), any(), any(), anyInt(), any());

    invokeStream(aiControl, streamId, emitter);

    ArgumentCaptor<SseEmitter.SseEventBuilder> eventCaptor =
        ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
    verify(emitter, atLeastOnce()).send(eventCaptor.capture());
    List<List<Object>> events = eventCaptor.getAllValues().stream()
        .map(builder -> builder.build().stream().map(item -> item.getData()).toList())
        .toList();
    List<Object> reasoningEvent = events.stream()
        .filter(event -> isEvent(event, "reasoning"))
        .findFirst()
        .orElseThrow();
    List<Object> messageEvent = events.stream()
        .filter(event -> isEvent(event, "message"))
        .findFirst()
        .orElseThrow();

    assertTrue(reasoningEvent.contains("模型思考"));
    assertFalse(reasoningEvent.contains("正文"));
    assertTrue(messageEvent.contains("正文"));
    assertFalse(messageEvent.contains("模型思考"));
    assertTrue(events.stream().anyMatch(event -> isEvent(event, "complete")));
    assertQueuesRemoved(streamChatId);
    verify(emitter).complete();
  }

  @Test
  void cleansQueuesAndEmitsErrorWhenStreamingFails() throws Exception {
    AiControl aiControl = validStreamControl();
    String streamId = "failure";
    String streamChatId = streamChatId(streamId);
    SseEmitter emitter = org.mockito.Mockito.mock(SseEmitter.class);

    when(router.responseDebugStream(any(), any(), any(), any(), anyInt(), any()))
        .thenThrow(new IllegalStateException("stream failed"));

    invokeStream(aiControl, streamId, emitter);

    ArgumentCaptor<SseEmitter.SseEventBuilder> eventCaptor =
        ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
    verify(emitter, atLeastOnce()).send(eventCaptor.capture());
    assertTrue(eventCaptor.getAllValues().stream()
        .map(builder -> builder.build().stream().map(item -> item.getData()).toList())
        .anyMatch(event -> isEvent(event, "error")));
    assertQueuesRemoved(streamChatId);
    verify(emitter).complete();
  }

  @Test
  void stopRemovesMessageAndReasoningQueues() throws Exception {
    String streamId = "stop";
    String streamChatId = streamChatId(streamId);
    Router.queueMap.put(streamChatId, new ConcurrentLinkedQueue<>(List.of("正文")));
    reasoningQueueMap().put(streamChatId,
        new ConcurrentLinkedQueue<>(List.of("模型思考")));
    when(safetyService.controlAuthorizeProduct("token", 1)).thenReturn(true);

    JsonResult<?> result = aiService.stopAiResponseStream(1, streamId, "token");

    assertEquals(200, result.getErrorCode());
    assertQueuesRemoved(streamChatId);
  }

  private AiControl validStreamControl() throws Exception {
    Field maxInputCharsField = AiServiceImpl.class.getDeclaredField("streamMaxInputChars");
    maxInputCharsField.setAccessible(true);
    maxInputCharsField.setInt(aiService, 60000);
    when(safetyService.controlAuthorizeProduct("token", 1)).thenReturn(true);
    when(productService.findAllById(1)).thenReturn(List.of(new ProductEntity()));
    AiControl aiControl = new AiControl();
    aiControl.setProductId(1);
    aiControl.setContent("问题");
    return aiControl;
  }

  private void invokeStream(AiControl aiControl, String streamId, SseEmitter emitter)
      throws Exception {
    Method method = AiServiceImpl.class.getDeclaredMethod("handleAiResponseStream",
        AiControl.class, String.class, MultipartFile[].class, String.class, SseEmitter.class);
    method.setAccessible(true);
    method.invoke(aiService, aiControl, streamId, null, "token", emitter);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Queue<String>> reasoningQueueMap() throws Exception {
    Field field = AiServiceImpl.class.getDeclaredField("reasoningQueueMap");
    field.setAccessible(true);
    return (Map<String, Queue<String>>) field.get(aiService);
  }

  private void assertQueuesRemoved(String streamChatId) throws Exception {
    assertFalse(Router.queueMap.containsKey(streamChatId));
    assertFalse(reasoningQueueMap().containsKey(streamChatId));
  }

  private String streamChatId(String streamId) {
    return "chatProduct1:stream:" + streamId;
  }

  private boolean isEvent(List<Object> event, String eventName) {
    return event.stream()
        .map(String::valueOf)
        .anyMatch(value -> value.contains("event:" + eventName));
  }
}
