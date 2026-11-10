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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MemoryExtractionWindowTest {

  @Test
  void waitsForIntervalThenExtractsEveryTurnExactlyOnce() {
    List<ModelMessage> history = turns(1, 4);

    MemoryExtractionWindow.Batch first = MemoryExtractionWindow.next(history, null, 3, false);
    assertEquals(contents(1, 4), contents(first));
    assertEquals("r4", first.watermark());

    history.addAll(turns(5, 6));
    assertNull(MemoryExtractionWindow.next(history, first.watermark(), 3, false));

    history.addAll(turns(7, 7));
    MemoryExtractionWindow.Batch second =
        MemoryExtractionWindow.next(history, first.watermark(), 3, false);
    // Turn 4 is repeated once as context; turns 5-7 are new.
    assertEquals(contents(4, 7), contents(second));
    assertEquals("r7", second.watermark());
  }

  @Test
  void flushRunsForAnyPendingTurnButNotTwice() {
    List<ModelMessage> history = turns(1, 5);

    MemoryExtractionWindow.Batch flushed = MemoryExtractionWindow.next(history, "r4", 3, true);
    assertEquals(contents(4, 5), contents(flushed));
    assertNull(MemoryExtractionWindow.next(history, flushed.watermark(), 3, true));
  }

  @Test
  void treatsCompactedWatermarkAsAllRetainedTurnsNew() {
    MemoryExtractionWindow.Batch batch =
        MemoryExtractionWindow.next(turns(8, 11), "r2", 3, false);

    assertEquals(contents(8, 11), contents(batch));
  }

  @Test
  void splitsLegacyMessagesWithoutRequestIdByUserMessage() {
    List<ModelMessage> legacy = List.of(new ModelMessage("user", "a"),
        new ModelMessage("assistant", "b"), new ModelMessage("user", "c"),
        new ModelMessage("assistant", "d"));

    MemoryExtractionWindow.Batch batch = MemoryExtractionWindow.next(legacy, null, 2, false);
    assertEquals(4, batch.messages().size());
    assertNull(MemoryExtractionWindow.next(legacy, batch.watermark(), 1, true));
  }

  private static List<ModelMessage> turns(int from, int to) {
    List<ModelMessage> messages = new ArrayList<>();
    for (int turn = from; turn <= to; turn++) {
      messages.add(new ModelMessage("user", "q" + turn, "r" + turn));
      messages.add(new ModelMessage("assistant", "a" + turn, "r" + turn));
    }
    return messages;
  }

  private static List<Object> contents(int from, int to) {
    return turns(from, to).stream().map(ModelMessage::getContent).toList();
  }

  private static List<Object> contents(MemoryExtractionWindow.Batch batch) {
    return batch.messages().stream().map(ModelMessage::getContent).toList();
  }
}
