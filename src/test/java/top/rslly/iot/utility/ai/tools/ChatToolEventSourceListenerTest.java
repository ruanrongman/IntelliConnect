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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.alibaba.fastjson.JSON;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import org.junit.jupiter.api.Test;

class ChatToolEventSourceListenerTest {
  @Test
  void preservesLineBreaksAndIndentationInTextStream() {
    String chatId = "workbench-chat";
    Queue<String> replies = new ArrayDeque<>();
    Queue<String> reasoning = new ArrayDeque<>();
    var listener = new ChatToolEventSourceListener(Map.of(chatId, replies), chatId,
        new ChatTool(), reasoning);
    List<String> chunks = List.of("示例", "\n\n", "```python\ndef sort():\n", "    ",
        "return []\n```");

    for (String chunk : chunks) {
      listener.onEvent(null, null, null,
          JSON.toJSONString(Map.of("choices", List.of(Map.of("delta", Map.of("content", chunk))))));
    }

    assertEquals(chunks, List.copyOf(replies));
  }
}
