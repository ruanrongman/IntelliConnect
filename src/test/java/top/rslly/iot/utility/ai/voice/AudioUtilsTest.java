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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

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
  @Test
  void extractsPcmFromCanonicalWav() throws Exception {
    byte[] pcm = {1, 2, 3, 4};
    assertArrayEquals(pcm, AudioUtils.wavBytesToPcm(wav(chunk("data", pcm))));
  }

  @Test
  void ignoresDataMarkerInsideMetadata() throws Exception {
    byte[] pcm = {1, 2, 3, 4};
    byte[] metadata = "metadata contains data marker".getBytes(StandardCharsets.US_ASCII);
    assertArrayEquals(pcm,
        AudioUtils.wavBytesToPcm(wav(chunk("JUNK", metadata), chunk("data", pcm))));
  }

  @Test
  void excludesChunksAfterPcmData() throws Exception {
    byte[] pcm = {1, 2, 3, 4};
    assertArrayEquals(pcm, AudioUtils.wavBytesToPcm(
        wav(chunk("data", pcm), chunk("JUNK", new byte[] {5, 6}))));
  }

  @Test
  void skipsOddSizedChunkPadding() throws Exception {
    byte[] pcm = {1, 2, 3, 4};
    assertArrayEquals(pcm, AudioUtils.wavBytesToPcm(
        wav(chunk("JUNK", new byte[] {9}), chunk("data", pcm))));
  }

  @Test
  void excludesDataChunkPadding() throws Exception {
    byte[] pcm = {1, 2, 3};
    assertArrayEquals(pcm, AudioUtils.wavBytesToPcm(wav(chunk("data", pcm))));
  }

  @Test
  void preservesEmptyDataChunk() throws Exception {
    assertArrayEquals(new byte[0], AudioUtils.wavBytesToPcm(
        wav(chunk("data", new byte[0]), chunk("JUNK", new byte[] {1, 2}))));
  }

  @Test
  void rejectsTruncatedDataPayload() {
    byte[] input = wav(chunk("data", new byte[] {1, 2}));
    ByteBuffer.wrap(input).order(ByteOrder.LITTLE_ENDIAN).putInt(40, 4);
    assertThrows(IOException.class, () -> AudioUtils.wavBytesToPcm(input));
  }

  @Test
  void rejectsTruncatedChunkHeader() {
    byte[] input = wav(chunk("JUNK", new byte[8]), new byte[] {'d', 'a', 't', 'a', 0, 0});
    assertThrows(IOException.class, () -> AudioUtils.wavBytesToPcm(input));
  }

  @Test
  void rejectsUnsignedChunkSizeBeyondInput() {
    byte[] input = wav(chunk("data", new byte[] {1, 2}));
    ByteBuffer.wrap(input).order(ByteOrder.LITTLE_ENDIAN).putInt(40, -1);
    assertThrows(IOException.class, () -> AudioUtils.wavBytesToPcm(input));
  }

  @Test
  void doesNotReadDataOutsideRiffContainer() {
    byte[] input = wav(chunk("data", new byte[] {1, 2}));
    ByteBuffer.wrap(input).order(ByteOrder.LITTLE_ENDIAN).putInt(4, 28);
    assertThrows(IOException.class, () -> AudioUtils.wavBytesToPcm(input));
  }

  @Test
  void rejectsTruncatedRiffContainer() {
    byte[] input = wav(chunk("data", new byte[] {1, 2}));
    ByteBuffer.wrap(input).order(ByteOrder.LITTLE_ENDIAN).putInt(4, input.length);
    assertThrows(IOException.class, () -> AudioUtils.wavBytesToPcm(input));
  }

  @Test
  void rejectsMissingDataPadding() {
    byte[] input = wav(chunk("data", new byte[] {1}));
    byte[] truncated = java.util.Arrays.copyOf(input, input.length - 1);
    ByteBuffer.wrap(truncated).order(ByteOrder.LITTLE_ENDIAN).putInt(4, truncated.length - 8);
    assertThrows(IOException.class, () -> AudioUtils.wavBytesToPcm(truncated));
  }

  @Test
  void rejectsMissingDataChunk() {
    assertThrows(IOException.class,
        () -> AudioUtils.wavBytesToPcm(wav(chunk("JUNK", new byte[8]))));
  }

  private static byte[] wav(byte[]... chunks) {
    ByteArrayOutputStream body = new ByteArrayOutputStream();
    body.writeBytes("WAVE".getBytes(StandardCharsets.US_ASCII));
    // PCM, mono, 16 kHz, 8-bit samples (allows odd-sized audio payloads).
    body.writeBytes(chunk("fmt ", new byte[] {
        1, 0, 1, 0, (byte) 0x80, 0x3e, 0, 0,
        (byte) 0x80, 0x3e, 0, 0, 1, 0, 8, 0}));
    for (byte[] chunk : chunks) {
      body.writeBytes(chunk);
    }
    return chunk("RIFF", body.toByteArray());
  }

  private static byte[] chunk(String id, byte[] payload) {
    ByteBuffer buffer = ByteBuffer.allocate(8 + payload.length + payload.length % 2)
        .order(ByteOrder.LITTLE_ENDIAN);
    buffer.put(id.getBytes(StandardCharsets.US_ASCII));
    buffer.putInt(payload.length);
    buffer.put(payload);
    return buffer.array();
  }
}
