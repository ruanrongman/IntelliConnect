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

import jakarta.websocket.Session;
import top.rslly.iot.utility.ai.voice.AudioUtils;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public interface TtsService {
  byte[] AUDIO_EOS = new byte[0];

  default void websocketAudioSync(String text, Float pitch, Float speed, Session session,
      String chatId, String voice, long generation) {
    BlockingQueue<byte[]> audioQueue = new LinkedBlockingQueue<>();
    Thread playbackThread = Thread.ofVirtual().start(
        () -> AudioUtils.asyncSendAudioQueue(chatId, session, audioQueue, generation));
    try {
      streamTextAudio(chatId, text, pitch, speed, voice, audioQueue::offer);
    } finally {
      audioQueue.offer(AUDIO_EOS);
      try {
        playbackThread.join();
      } catch (InterruptedException e) {
        playbackThread.interrupt();
        Thread.currentThread().interrupt();
      }
    }
  }

  void asyncSynthesizeAndSaveAudio(String text, String chatId);

  List<byte[]> getTextAudio(String chatId, String text, Float pitch, Float speed, String voice);

  default boolean streamTextAudio(String chatId, String text, Float pitch, Float speed,
      String voice, Consumer<byte[]> onChunk) {
    List<byte[]> audioFrames = getTextAudio(chatId, text, pitch, speed, voice);
    if (audioFrames == null || audioFrames.isEmpty()) {
      return false;
    }
    boolean emitted = false;
    for (byte[] frame : audioFrames) {
      if (frame == null || frame.length == 0) {
        continue;
      }
      onChunk.accept(frame);
      emitted = true;
    }
    return emitted;
  }
}
