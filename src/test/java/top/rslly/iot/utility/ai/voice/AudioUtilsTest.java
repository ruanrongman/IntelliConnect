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
package top.rslly.iot.utility.ai.voice;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.junit.jupiter.api.Test;

class AudioUtilsTest {

  @Test
  void shortAudioKeepsFramesAndReportsEndOfStream() throws Exception {
    byte[] first = {1};
    byte[] second = {2};
    BlockingQueue<byte[]> queue = new LinkedBlockingQueue<>();
    queue.offer(first);
    queue.offer(second);
    queue.offer(new byte[0]);

    AudioUtils.PreBufferResult result = AudioUtils.readPreBuffer(queue, () -> false);

    assertTrue(result.endOfStream());
    assertFalse(result.cancelled());
    assertArrayEquals(first, result.frames().get(0));
    assertArrayEquals(second, result.frames().get(1));
    assertTrue(queue.isEmpty());
  }

  @Test
  void fullPreBufferLeavesEndMarkerForMainSendLoop() throws Exception {
    BlockingQueue<byte[]> queue = new LinkedBlockingQueue<>();
    for (int i = 0; i < 5; i++) {
      queue.offer(new byte[] {(byte) i});
    }
    queue.offer(new byte[0]);

    AudioUtils.PreBufferResult result = AudioUtils.readPreBuffer(queue, () -> false);

    assertFalse(result.endOfStream());
    assertFalse(result.cancelled());
    assertTrue(result.frames().size() == 5);
    assertTrue(queue.remove().length == 0);
  }

  @Test
  void cancellationDoesNotConsumeQueuedAudio() throws Exception {
    BlockingQueue<byte[]> queue = new LinkedBlockingQueue<>();
    queue.offer(new byte[] {1});

    AudioUtils.PreBufferResult result = AudioUtils.readPreBuffer(queue, () -> true);

    assertTrue(result.cancelled());
    assertFalse(queue.isEmpty());
  }
}
