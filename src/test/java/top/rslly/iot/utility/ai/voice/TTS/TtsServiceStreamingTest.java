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
package top.rslly.iot.utility.ai.voice.TTS;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class TtsServiceStreamingTest {

  @Test
  void defaultStreamAdapterFiltersNullAndEndMarkers() {
    byte[] first = {1};
    byte[] second = {2};
    TtsService service = new TtsService() {
      @Override
      public void asyncSynthesizeAndSaveAudio(String text, String chatId) {}

      @Override
      public List<byte[]> getTextAudio(String chatId, String text, Float pitch, Float speed,
          String voice) {
        return Arrays.asList(first, null, new byte[0], second);
      }
    };
    List<byte[]> streamed = new ArrayList<>();

    boolean success = service.streamTextAudio("chat", "text", 1.0f, 1.0f, null, streamed::add);

    assertTrue(success);
    assertEquals(2, streamed.size());
    assertArrayEquals(first, streamed.get(0));
    assertArrayEquals(second, streamed.get(1));
  }

  @Test
  void dashScopeCallbackEmitsCompleteOpusFrameBeforeCompletion() throws Exception {
    List<byte[]> streamed = new ArrayList<>();
    Text2audio.ReactCallback callback = new Text2audio.ReactCallback("chat", streamed::add);
    int pcmFrameBytes = 16_000 * 60 / 1_000 * 2;
    byte[] pcmFrame = new byte[pcmFrameBytes];
    for (int i = 0; i < pcmFrame.length; i++) {
      pcmFrame[i] = (byte) (i * 17 + 3);
    }

    callback.acceptPcm(new byte[] {pcmFrame[0]});
    callback.acceptPcm(Arrays.copyOfRange(pcmFrame, 1, pcmFrame.length));

    assertEquals(1, streamed.size());
    int framesBeforeCompletion = streamed.size();

    callback.onComplete();
    callback.waitForComplete();
    callback.onComplete();

    assertTrue(callback.hasAudio());
    assertEquals(framesBeforeCompletion, streamed.size());
  }
}
