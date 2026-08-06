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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class OpusEncoderUtilsTest {

  @Test
  void incrementalOddByteChunksMatchAggregateEncoding() {
    byte[] pcm = new byte[2_050];
    for (int i = 0; i < pcm.length; i++) {
      pcm[i] = (byte) (i * 31 + 7);
    }

    OpusEncoderUtils aggregateEncoder = new OpusEncoderUtils(16_000, 1, 20);
    List<byte[]> aggregatePackets = new ArrayList<>(
        aggregateEncoder.encodePcmToOpus(pcm, false));
    aggregatePackets.addAll(aggregateEncoder.encodePcmToOpus(new byte[0], true));

    OpusEncoderUtils incrementalEncoder = new OpusEncoderUtils(16_000, 1, 20);
    List<byte[]> incrementalPackets = new ArrayList<>();
    int[] chunkSizes = {1, 127, 2, 511, 3, 64, 255};
    int offset = 0;
    int chunkIndex = 0;
    while (offset < pcm.length) {
      int length = Math.min(chunkSizes[chunkIndex++ % chunkSizes.length], pcm.length - offset);
      byte[] chunk = java.util.Arrays.copyOfRange(pcm, offset, offset + length);
      incrementalPackets.addAll(incrementalEncoder.encodePcmToOpus(chunk, false));
      offset += length;
    }
    incrementalPackets.addAll(incrementalEncoder.encodePcmToOpus(new byte[0], true));

    assertEquals(aggregatePackets.size(), incrementalPackets.size());
    for (int i = 0; i < aggregatePackets.size(); i++) {
      assertArrayEquals(aggregatePackets.get(i), incrementalPackets.get(i));
    }
    assertTrue(incrementalEncoder.encodePcmToOpus(new byte[0], true).isEmpty());
  }

  @Test
  void halfCosineFadeUsesConfiguredDurationAtSupportedSampleRates() {
    int[] sampleRates = {16_000, 24_000, 48_000};
    for (int sampleRate : sampleRates) {
      int fadeSampleFrames = sampleRate * 5 / 1000;
      short[] fadeIn = new short[fadeSampleFrames];
      java.util.Arrays.fill(fadeIn, (short) 12_000);

      new PcmBoundarySmoother(sampleRate, 1, 5).applyFadeIn(fadeIn);

      assertEquals(0, fadeIn[0]);
      assertEquals(12_000, fadeIn[fadeIn.length - 1]);
      for (int i = 1; i < fadeIn.length; i++) {
        assertTrue(fadeIn[i] >= fadeIn[i - 1]);
      }

      short[] fadeOut = new short[fadeSampleFrames];
      java.util.Arrays.fill(fadeOut, (short) 12_000);

      new PcmBoundarySmoother(sampleRate, 1, 5).applyFadeOut(fadeOut);

      assertEquals(12_000, fadeOut[0]);
      assertEquals(0, fadeOut[fadeOut.length - 1]);
      for (int i = 1; i < fadeOut.length; i++) {
        assertTrue(fadeOut[i] <= fadeOut[i - 1]);
      }
    }
  }

  @Test
  void fadeInStateCrossesChunksAndUsesTheSameGainForEachChannel() {
    int fadeSampleFrames = 16_000 * 5 / 1000;
    short[] pcm = new short[fadeSampleFrames * 2];
    for (int i = 0; i < fadeSampleFrames; i++) {
      pcm[i * 2] = 10_000;
      pcm[i * 2 + 1] = -10_000;
    }

    short[] aggregate = java.util.Arrays.copyOf(pcm, pcm.length);
    new PcmBoundarySmoother(16_000, 2, 5).applyFadeIn(aggregate);

    short[] firstChunk = java.util.Arrays.copyOfRange(pcm, 0, 3);
    short[] secondChunk = java.util.Arrays.copyOfRange(pcm, 3, pcm.length);
    PcmBoundarySmoother incrementalSmoother = new PcmBoundarySmoother(16_000, 2, 5);
    incrementalSmoother.applyFadeIn(firstChunk);
    incrementalSmoother.applyFadeIn(secondChunk);
    short[] incremental = new short[pcm.length];
    System.arraycopy(firstChunk, 0, incremental, 0, firstChunk.length);
    System.arraycopy(secondChunk, 0, incremental, firstChunk.length, secondChunk.length);

    assertArrayEquals(aggregate, incremental);
    for (int i = 0; i < incremental.length; i += 2) {
      assertEquals(Math.abs(incremental[i]), Math.abs(incremental[i + 1]));
    }
  }

  @Test
  void smoothedOddByteChunksMatchAggregateEncoding() {
    byte[] pcm = createPcm(2_051);
    OpusEncoderUtils aggregateEncoder = new OpusEncoderUtils(16_000, 1, 20, 5);
    List<byte[]> aggregatePackets = encodeAll(aggregateEncoder, pcm);

    OpusEncoderUtils incrementalEncoder = new OpusEncoderUtils(16_000, 1, 20, 5);
    List<byte[]> incrementalPackets = new ArrayList<>();
    int[] chunkSizes = {1, 127, 2, 511, 3, 64, 255};
    int offset = 0;
    int chunkIndex = 0;
    while (offset < pcm.length) {
      int length = Math.min(chunkSizes[chunkIndex++ % chunkSizes.length], pcm.length - offset);
      byte[] chunk = java.util.Arrays.copyOfRange(pcm, offset, offset + length);
      incrementalPackets.addAll(incrementalEncoder.encodePcmToOpus(chunk, false));
      offset += length;
    }
    incrementalPackets.addAll(incrementalEncoder.encodePcmToOpus(new byte[0], true));

    assertPacketListsEqual(aggregatePackets, incrementalPackets);
  }

  @Test
  void shortSmoothedTailFlushesExactlyOnce() {
    OpusEncoderUtils encoder = new OpusEncoderUtils(16_000, 1, 60, 5);
    byte[] pcm = createPcm(64 * 2);

    assertTrue(encoder.encodePcmToOpus(pcm, false).isEmpty());
    List<byte[]> tailPackets = encoder.encodePcmToOpus(new byte[0], true);

    assertEquals(1, tailPackets.size());
    assertFalse(tailPackets.get(0).length == 0);
    assertTrue(encoder.encodePcmToOpus(new byte[0], true).isEmpty());
  }

  @Test
  void zeroFadeConstructorMatchesDefaultEncoding() {
    byte[] pcm = createPcm(2_050);

    List<byte[]> defaultPackets = encodeAll(new OpusEncoderUtils(16_000, 1, 20), pcm);
    List<byte[]> zeroFadePackets = encodeAll(new OpusEncoderUtils(16_000, 1, 20, 0), pcm);

    assertPacketListsEqual(defaultPackets, zeroFadePackets);
  }

  private static List<byte[]> encodeAll(OpusEncoderUtils encoder, byte[] pcm) {
    List<byte[]> packets = new ArrayList<>(encoder.encodePcmToOpus(pcm, false));
    packets.addAll(encoder.encodePcmToOpus(new byte[0], true));
    return packets;
  }

  private static void assertPacketListsEqual(List<byte[]> expected, List<byte[]> actual) {
    assertEquals(expected.size(), actual.size());
    for (int i = 0; i < expected.size(); i++) {
      assertArrayEquals(expected.get(i), actual.get(i));
    }
  }

  private static byte[] createPcm(int length) {
    byte[] pcm = new byte[length];
    for (int i = 0; i < pcm.length; i++) {
      pcm[i] = (byte) (i * 31 + 7);
    }
    return pcm;
  }
}
