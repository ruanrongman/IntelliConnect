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
package top.rslly.iot.utility.ai;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializer;
import top.rslly.iot.config.RedisConfig;
import top.rslly.iot.models.HistoryMessageEntity;
import top.rslly.iot.utility.ai.llm.FunctionToolCallMessage;
import top.rslly.iot.utility.ai.llm.FunctionToolResultMessage;

class ConversationHistoryCodecTest {
  private final FunctionToolCallMessage call = new FunctionToolCallMessage(
      "provider-id", "route_music", "{\"args\":\"song\"}", "preamble", "reasoning");
  private final FunctionToolResultMessage result =
      new FunctionToolResultMessage("provider-id", "raw result");

  @Test
  void roundTripsCompleteProtocolWithRealRedisSerializer() {
    RedisSerializer<Object> serializer = (RedisSerializer<Object>) new RedisConfig()
        .redisTemplate(mock(RedisConnectionFactory.class)).getValueSerializer();
    List<ModelMessage> messages = turn("request-1");

    Object deserialized = serializer.deserialize(serializer.serialize(messages));
    List<ModelMessage> restored = ConversationHistoryCodec.restoreCache(deserialized);

    assertEquals(messages, restored);
    assertInstanceOf(FunctionToolCallMessage.class, restored.get(1).getContent());
    assertInstanceOf(FunctionToolResultMessage.class, restored.get(2).getContent());
  }

  @Test
  void restoresMapsFromObjectContentBeforeSendingToModel() {
    ObjectMapper mapper = new ObjectMapper();
    List<Map> cache =
        turn("request-1").stream().map(message -> mapper.convertValue(message, Map.class)).toList();

    assertEquals(turn("request-1"), ConversationHistoryCodec.restoreCache(cache));
  }

  @Test
  void dropsDanglingMismatchedMalformedAndCrossRequestCallsButKeepsFinalAnswer() {
    for (String badResult : List.of("missing", "wrong-id", "malformed", "other-request")) {
      List<HistoryMessageEntity> rows = new ArrayList<>();
      rows.add(row("request", 1, "user", "original"));
      rows.add(row("request", 2, "assistant_tool_call", ConversationHistoryCodec.encode(call)));
      if (!badResult.equals("missing")) {
        rows.add(row(badResult.equals("other-request") ? "different" : "request", 3,
            "tool_result", badResult.equals("malformed") ? "{"
                : ConversationHistoryCodec.encode(
                    badResult.equals("wrong-id") ? new FunctionToolResultMessage("wrong", "raw")
                        : result)));
      }
      rows.add(row("request", 4, "assistant", "final answer"));

      List<ModelMessage> restored = ConversationHistoryCodec.restore(rows);

      assertEquals(List.of("original", "final answer"),
          restored.stream().map(ModelMessage::getContent).toList());
    }
  }

  @Test
  void oldAssistantJsonRemainsPlainTextAndInternalEventsStayOutOfAgentHistory() {
    String oldText = "{\"toolCallId\":\"looks-like-json\",\"functionName\":\"still text\"}";
    List<ModelMessage> restored = ConversationHistoryCodec.restore(List.of(
        row("legacy", 1, "user", "old question"), row("legacy", 2, "assistant", oldText)));
    assertEquals(oldText, restored.getLast().getContent());

    List<ModelMessage> agentHistory =
        GlobalMessageContext.conversationHistory(Map.of("memory", turn("r")));
    assertEquals(List.of("question", "visible final answer"),
        agentHistory.stream().map(ModelMessage::getContent).toList());
  }

  @Test
  void compactionKeepsWholeRequestsWithBothToolEventsAndFinalAnswer() {
    List<ModelMessage> messages = new ArrayList<>();
    messages.addAll(turn("r1"));
    messages.addAll(turn("r2"));
    messages.addAll(turn("r3"));
    ConversationMemoryPolicy policy = new ConversationMemoryPolicy(1, 0.5, 2);

    assertEquals(turn("r1"), policy.compactionPrefix(messages));
    List<ModelMessage> snapshot = policy.recentMessagesSnapshot(messages);
    policy.retainRecentTurns(messages);

    assertEquals(8, messages.size());
    assertEquals(snapshot, messages);
    assertEquals("r2", messages.getFirst().getRequestId());
    assertEquals(messages, ConversationHistoryCodec.restoreCache(messages));
  }

  @Test
  void restoresInterleavedRequestsAsWholeExchanges() {
    List<HistoryMessageEntity> rows = List.of(
        row("a", 1, "user", "A"), row("b", 1, "user", "B"),
        row("a", 2, "assistant_tool_call", ConversationHistoryCodec.encode(call)),
        row("b", 2, "assistant", "B answer"),
        row("a", 3, "tool_result", ConversationHistoryCodec.encode(result)),
        row("a", 4, "assistant", "A answer"));

    List<ModelMessage> restored = ConversationHistoryCodec.restore(rows);

    assertEquals(6, restored.size());
    assertInstanceOf(FunctionToolCallMessage.class, restored.get(1).getContent());
    assertInstanceOf(FunctionToolResultMessage.class, restored.get(2).getContent());
    assertEquals("A answer", restored.get(3).getContent());
  }

  private List<ModelMessage> turn(String requestId) {
    return new ArrayList<>(List.of(new ModelMessage("user", "question", requestId),
        new ModelMessage("assistant", call, requestId), new ModelMessage("tool", result, requestId),
        new ModelMessage("assistant", "visible final answer", requestId)));
  }

  private HistoryMessageEntity row(String requestId, int sequence, String type, String content) {
    HistoryMessageEntity row = new HistoryMessageEntity();
    row.setRequestId(requestId);
    row.setSequenceNum(sequence);
    row.setMessageType(type);
    row.setContent(content);
    row.setTime(100L + sequence);
    return row;
  }
}
