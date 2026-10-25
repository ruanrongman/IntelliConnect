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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import top.rslly.iot.models.HistoryMessageEntity;
import top.rslly.iot.utility.ai.llm.FunctionToolCallMessage;
import top.rslly.iot.utility.ai.llm.FunctionToolResultMessage;

/** Encodes persisted protocol events and restores only complete tool exchanges. */
@Slf4j
public final class ConversationHistoryCodec {
  public static final String TOOL_CALL_TYPE = "assistant_tool_call";
  public static final String TOOL_RESULT_TYPE = "tool_result";
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private ConversationHistoryCodec() {}

  public static String encode(Object protocolMessage) {
    try {
      return MAPPER.writeValueAsString(protocolMessage);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Cannot encode history protocol event", e);
    }
  }

  public static List<ModelMessage> restore(List<HistoryMessageEntity> rows) {
    List<ModelMessage> messages = new ArrayList<>();
    // Concurrent requests may interleave in wall-clock order. Keep each request together.
    Map<String, List<HistoryMessageEntity>> turns = new LinkedHashMap<>();
    rows.stream().filter(Objects::nonNull)
        .sorted(Comparator.comparing(HistoryMessageEntity::getTime,
            Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(HistoryMessageEntity::getSequenceNum,
                Comparator.nullsFirst(Comparator.naturalOrder())))
        .forEach(
            row -> turns.computeIfAbsent(row.getRequestId(), key -> new ArrayList<>()).add(row));
    List<HistoryMessageEntity> ordered = new ArrayList<>();
    turns.values().forEach(turn -> {
      turn.sort(Comparator.comparing(HistoryMessageEntity::getSequenceNum,
          Comparator.nullsFirst(Comparator.naturalOrder())));
      ordered.addAll(turn);
    });
    for (HistoryMessageEntity row : ordered) {
      if (row == null || row.getContent() == null) {
        continue;
      }
      try {
        String role = row.getMessageType();
        Object content = row.getContent();
        if (TOOL_CALL_TYPE.equals(role)) {
          role = ModelMessageRole.ASSISTANT.value();
          content = MAPPER.readValue(row.getContent(), FunctionToolCallMessage.class);
        } else if (TOOL_RESULT_TYPE.equals(role)) {
          role = ModelMessageRole.TOOL.value();
          content = MAPPER.readValue(row.getContent(), FunctionToolResultMessage.class);
        } else if (!ModelMessageRole.USER.value().equals(role)
            && !ModelMessageRole.ASSISTANT.value().equals(role)) {
          continue;
        }
        messages.add(new ModelMessage(role, content, row.getRequestId()));
      } catch (JsonProcessingException | IllegalArgumentException e) {
        log.warn("Ignoring malformed history event, requestId={}, sequence={}",
            row.getRequestId(), row.getSequenceNum(), e);
      }
    }
    return completeExchanges(messages);
  }

  /** Redis may deserialize Object content as a Map; always rebuild the protocol records. */
  public static List<ModelMessage> restoreCache(Object cache) {
    List<ModelMessage> messages = new ArrayList<>();
    if (!(cache instanceof List<?> items)) {
      return messages;
    }
    for (Object item : items) {
      try {
        ModelMessage message = item instanceof ModelMessage modelMessage ? modelMessage
            : MAPPER.convertValue(item, ModelMessage.class);
        if (message == null) {
          continue;
        }
        Object content = message.getContent();
        if (content instanceof Map<?, ?> map && map.containsKey("toolCallId")) {
          content = map.containsKey("functionName")
              ? MAPPER.convertValue(content, FunctionToolCallMessage.class)
              : MAPPER.convertValue(content, FunctionToolResultMessage.class);
        }
        messages.add(new ModelMessage(message.getRole(), content, message.getRequestId()));
      } catch (IllegalArgumentException e) {
        log.warn("Ignoring malformed cached conversation event", e);
      }
    }
    return completeExchanges(messages);
  }

  private static List<ModelMessage> completeExchanges(List<ModelMessage> messages) {
    List<ModelMessage> complete = new ArrayList<>();
    for (int index = 0; index < messages.size(); index++) {
      ModelMessage message = messages.get(index);
      if (message.getContent()instanceof FunctionToolCallMessage call) {
        if (index + 1 < messages.size()) {
          ModelMessage next = messages.get(index + 1);
          if (ModelMessageRole.ASSISTANT.value().equals(message.getRole())
              && ModelMessageRole.TOOL.value().equals(next.getRole())
              && next.getContent()instanceof FunctionToolResultMessage result
              && call.toolCallId() != null && !call.toolCallId().isBlank()
              && call.toolCallId().equals(result.toolCallId())
              && Objects.equals(message.getRequestId(), next.getRequestId())) {
            complete.add(message);
            complete.add(next);
            index++;
          }
        }
      } else if (message.getContent() instanceof String
          && (ModelMessageRole.USER.value().equals(message.getRole())
              || ModelMessageRole.ASSISTANT.value().equals(message.getRole()))) {
        complete.add(message);
      }
    }
    return complete;
  }

  /** Ordinary agents and memory summaries consume user text and the visible final answer. */
  public static List<ModelMessage> visibleMessages(List<ModelMessage> messages) {
    return messages.stream()
        .filter(message -> message != null && message.getContent() instanceof String
            && (ModelMessageRole.USER.value().equals(message.getRole())
                || ModelMessageRole.ASSISTANT.value().equals(message.getRole())))
        .map(message -> new ModelMessage(message.getRole(), message.getContent(),
            message.getRequestId()))
        .toList();
  }
}
