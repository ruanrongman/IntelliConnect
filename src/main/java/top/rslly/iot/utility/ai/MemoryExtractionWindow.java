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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Decides which conversation turns the background extractors (long-term memory, chat knowledge
 * graph) still have to read. They used to re-read the last four turns on every turn, so each turn
 * was extracted four times; now every turn is extracted once, in batches of {@code interval}.
 */
public final class MemoryExtractionWindow {
  private MemoryExtractionWindow() {}

  /**
   * @param messages turns to extract, preceded by one already-extracted turn for context
   * @param watermark key of the newest turn in this batch; store it and pass it back next time
   */
  public record Batch(List<ModelMessage> messages, String watermark) {}

  /**
   * @param visibleMessages user/assistant messages, oldest first
   * @param watermark key of the last extracted turn, null when nothing was extracted yet
   * @param interval run once at least this many unextracted turns exist
   * @param flush run for any unextracted turn, e.g. when the conversation ends
   * @return null when nothing should run yet
   */
  public static Batch next(List<ModelMessage> visibleMessages, String watermark, int interval,
      boolean flush) {
    List<List<ModelMessage>> turns = splitTurns(visibleMessages);
    int firstNew = 0;
    if (watermark != null) {
      for (int index = turns.size() - 1; index >= 0; index--) {
        if (watermark.equals(turnKey(turns.get(index)))) {
          firstNew = index + 1;
          break;
        }
      }
      // Not found: the extracted turn was compacted away, so every retained turn is newer.
    }
    int newTurns = turns.size() - firstNew;
    if (newTurns <= 0 || (!flush && newTurns < Math.max(1, interval))) {
      return null;
    }
    List<ModelMessage> messages = new ArrayList<>();
    for (int index = Math.max(0, firstNew - 1); index < turns.size(); index++) {
      messages.addAll(turns.get(index));
    }
    return new Batch(messages, turnKey(turns.getLast()));
  }

  /**
   * Same turn boundaries as {@link ConversationMemoryPolicy}: requestId, else each user message.
   */
  static List<List<ModelMessage>> splitTurns(List<ModelMessage> messages) {
    List<List<ModelMessage>> turns = new ArrayList<>();
    ModelMessage previous = null;
    for (ModelMessage message : messages == null ? List.<ModelMessage>of() : messages) {
      if (message == null) {
        continue;
      }
      String requestId = message.getRequestId();
      boolean start = turns.isEmpty() || (requestId != null
          ? previous == null || !requestId.equals(previous.getRequestId())
          : ModelMessageRole.USER.value().equals(message.getRole()));
      if (start) {
        turns.add(new ArrayList<>());
      }
      turns.getLast().add(message);
      previous = message;
    }
    return turns;
  }

  static String turnKey(List<ModelMessage> turn) {
    String requestId = turn.getFirst().getRequestId();
    if (requestId != null) {
      return requestId;
    }
    // Legacy messages without requestId: identify the turn by its content.
    return "h" + Objects.hash(turn.stream().map(ModelMessage::getContent).toList());
  }
}
