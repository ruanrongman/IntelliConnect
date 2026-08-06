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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import top.rslly.iot.utility.ai.voice.concentus.OpusApplication;
import top.rslly.iot.utility.ai.voice.concentus.OpusEncoder;
import top.rslly.iot.utility.ai.voice.concentus.OpusException;
import top.rslly.iot.utility.ai.voice.concentus.OpusSignal;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Getter
public class OpusEncoderUtils {

  private final OpusEncoder encoder;
  private final int bitrate = 24000;
  private final int complexity = 10;
  private final int sampleRate;
  private final int channels;
  private final int frameSizeMs;
  private final int frameSize;
  private final int totalFrameSize;
  private final PcmBoundarySmoother boundarySmoother;
  private short[] buffer = new short[0];
  private boolean hasPendingPcmByte;
  private byte pendingPcmByte;

  public OpusEncoderUtils(int sampleRate, int channels, int frameSizeMs) {
    this(sampleRate, channels, frameSizeMs, 0);
  }

  public OpusEncoderUtils(int sampleRate, int channels, int frameSizeMs, int boundaryFadeMs) {
    this.sampleRate = sampleRate;
    this.channels = channels;
    this.frameSizeMs = frameSizeMs;
    this.frameSize = (sampleRate * frameSizeMs) / 1000;
    this.totalFrameSize = frameSize * channels;
    this.boundarySmoother = boundaryFadeMs > 0
        ? new PcmBoundarySmoother(sampleRate, channels, boundaryFadeMs)
        : null;
    try {
      encoder = new OpusEncoder(sampleRate, channels, OpusApplication.OPUS_APPLICATION_AUDIO);
      encoder.setBitrate(bitrate);
      encoder.setComplexity(complexity);
      encoder.setSignalType(OpusSignal.OPUS_SIGNAL_VOICE);
    } catch (Exception e) {
      throw new RuntimeException("初始化失败", e);
    }
  }

  public void resetState() {
    encoder.resetState();
  }

  public List<byte[]> encodePcmToOpus(byte[] pcmData, boolean endOfStream) {
    byte[] alignedPcmData = alignPcmData(pcmData, endOfStream);
    short[] newSamples = convertByteArrayToShortArray(alignedPcmData);
    validatePcmData(newSamples);
    if (boundarySmoother != null) {
      boundarySmoother.applyFadeIn(newSamples);
    }

    // 将新数据追加到缓冲区
    buffer = concatArrays(buffer, newSamples);

    List<byte[]> opusPackets = new ArrayList<>();
    int offset = 0;

    // 处理所有完整帧
    while (offset <= buffer.length - totalFrameSize) {
      short[] frame = Arrays.copyOfRange(buffer, offset, offset + totalFrameSize);
      byte[] output = new byte[1275];
      int result = encode(frame, output);
      opusPackets.add(Arrays.copyOf(output, result));
      offset += totalFrameSize;
    }

    // 保留未处理的样本
    buffer = Arrays.copyOfRange(buffer, offset, buffer.length);

    // 流结束时处理剩余数据并补零
    if (endOfStream && buffer.length > 0) {
      if (boundarySmoother != null) {
        boundarySmoother.applyFadeOut(buffer);
      }
      short[] lastFrame = Arrays.copyOf(buffer, buffer.length);
      lastFrame = Arrays.copyOf(lastFrame, totalFrameSize);
      Arrays.fill(lastFrame, buffer.length, totalFrameSize, (short) 0);

      byte[] output = new byte[1275];
      int result = encode(lastFrame, output);
      opusPackets.add(Arrays.copyOf(output, result));
      buffer = new short[0]; // 清空缓冲区
    }

    return opusPackets;
  }

  private byte[] alignPcmData(byte[] pcmData, boolean endOfStream) {
    byte[] source = pcmData == null ? new byte[0] : pcmData;
    int sourceOffset = 0;
    int combinedLength = source.length + (hasPendingPcmByte ? 1 : 0);
    int alignedLength = combinedLength;
    if ((combinedLength & 1) != 0) {
      alignedLength = endOfStream ? combinedLength + 1 : combinedLength - 1;
    }

    byte[] aligned = new byte[alignedLength];
    int targetOffset = 0;
    if (hasPendingPcmByte) {
      aligned[targetOffset++] = pendingPcmByte;
      hasPendingPcmByte = false;
    }

    int copyLength = Math.min(source.length, alignedLength - targetOffset);
    if (copyLength > 0) {
      System.arraycopy(source, sourceOffset, aligned, targetOffset, copyLength);
      sourceOffset += copyLength;
    }
    if (!endOfStream && sourceOffset < source.length) {
      pendingPcmByte = source[sourceOffset];
      hasPendingPcmByte = true;
    }
    return aligned;
  }

  private short[] concatArrays(short[] a, short[] b) {
    short[] result = Arrays.copyOf(a, a.length + b.length);
    System.arraycopy(b, 0, result, a.length, b.length);
    return result;
  }

  private int encode(short[] frameBuffer, byte[] outputBuffer) {
    try {
      return encoder.encode(frameBuffer, 0, frameBuffer.length, outputBuffer, 0,
          outputBuffer.length);
    } catch (OpusException e) {
      log.error("Opus 编码失败！", e);
      return 0;
    }
  }

  private short[] convertByteArrayToShortArray(byte[] bytes) {
    ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
    ShortBuffer shortBuffer = buffer.asShortBuffer();
    short[] shorts = new short[shortBuffer.remaining()];
    shortBuffer.get(shorts);
    return shorts;
  }

  private void validatePcmData(short[] pcmShorts) {
    // 实际项目中可记录错误而不是直接抛出异常
    for (short s : pcmShorts) {
      if (s < -32768 || s > 32767) {
        throw new IllegalArgumentException("Invalid PCM sample: " + s);
      }
    }
  }

  public void close() {

  }
}


final class PcmBoundarySmoother {

  private final int channels;
  private final int fadeSampleFrames;
  private long fadeInSamplesProcessed;

  PcmBoundarySmoother(int sampleRate, int channels, int fadeDurationMs) {
    if (sampleRate <= 0) {
      throw new IllegalArgumentException("sampleRate must be positive");
    }
    if (channels <= 0) {
      throw new IllegalArgumentException("channels must be positive");
    }
    if (fadeDurationMs <= 0) {
      throw new IllegalArgumentException("fadeDurationMs must be positive");
    }
    this.channels = channels;
    this.fadeSampleFrames = Math.max(1, sampleRate * fadeDurationMs / 1000);
  }

  void applyFadeIn(short[] samples) {
    if (samples == null || samples.length == 0) {
      return;
    }
    long fadeSampleCount = (long) fadeSampleFrames * channels;
    int samplesToFade = (int) Math.min(samples.length,
        Math.max(0L, fadeSampleCount - fadeInSamplesProcessed));
    for (int i = 0; i < samplesToFade; i++) {
      long sampleFrame = fadeInSamplesProcessed / channels;
      samples[i] = scale(samples[i], fadeInGain((int) sampleFrame, fadeSampleFrames));
      fadeInSamplesProcessed++;
    }
  }

  void applyFadeOut(short[] samples) {
    if (samples == null || samples.length == 0) {
      return;
    }
    int availableSampleFrames = samples.length / channels;
    int fadeFrames = Math.min(fadeSampleFrames, availableSampleFrames);
    if (fadeFrames == 0) {
      return;
    }
    int fadeStart = samples.length - fadeFrames * channels;
    for (int frame = 0; frame < fadeFrames; frame++) {
      double gain = fadeOutGain(frame, fadeFrames);
      int frameOffset = fadeStart + frame * channels;
      for (int channel = 0; channel < channels; channel++) {
        samples[frameOffset + channel] = scale(samples[frameOffset + channel], gain);
      }
    }
  }

  private static double fadeInGain(int sampleFrame, int fadeFrames) {
    if (fadeFrames == 1) {
      return 0.0;
    }
    return 0.5 - 0.5 * Math.cos(Math.PI * sampleFrame / (fadeFrames - 1));
  }

  private static double fadeOutGain(int sampleFrame, int fadeFrames) {
    if (fadeFrames == 1) {
      return 0.0;
    }
    return 0.5 + 0.5 * Math.cos(Math.PI * sampleFrame / (fadeFrames - 1));
  }

  private static short scale(short sample, double gain) {
    return (short) Math.round(sample * gain);
  }
}

