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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.Test;

class ClassifierToolEventSourceListenerTest {

  @Test
  void queuesReasoningOnlyStreamChunk() {
    String chatId = "debug-chat";
    ClassifierTool classifierTool = new ClassifierTool();
    ReentrantLock lock = new ReentrantLock();
    classifierTool.getLockMap().put(chatId, lock);
    classifierTool.getConditionMap().put(chatId, lock.newCondition());
    classifierTool.getDataMap().put(chatId, new HashMap<>());
    Queue<String> reasoningQueue = new ConcurrentLinkedQueue<>();
    ClassifierToolEventSourceListener listener = new ClassifierToolEventSourceListener(
        "question", new int[] {5, 11}, chatId, classifierTool, reasoningQueue);

    listener.onEvent(null, null, null,
        "{\"choices\":[{\"delta\":{\"reasoning_content\":\"route analysis\"}}]}");

    assertEquals("route analysis", reasoningQueue.poll());
    assertTrue(classifierTool.getDataMap().get(chatId).isEmpty());
  }

  @Test
  void preservesWhitespaceOnlyReasoningChunk() {
    String chatId = "debug-chat";
    ClassifierTool classifierTool = new ClassifierTool();
    ReentrantLock lock = new ReentrantLock();
    classifierTool.getLockMap().put(chatId, lock);
    classifierTool.getConditionMap().put(chatId, lock.newCondition());
    classifierTool.getDataMap().put(chatId, new HashMap<>());
    Queue<String> reasoningQueue = new ConcurrentLinkedQueue<>();
    ClassifierToolEventSourceListener listener = new ClassifierToolEventSourceListener(
        "question", new int[] {5, 11}, chatId, classifierTool, reasoningQueue);

    listener.onEvent(null, null, null,
        "{\"choices\":[{\"delta\":{\"reasoning_content\":\" \"}}]}");

    assertEquals(" ", reasoningQueue.poll());
  }
}
