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
  private short[] buffer = new short[0];

  public OpusEncoderUtils(int sampleRate, int channels, int frameSizeMs) {
    this.sampleRate = sampleRate;
    this.channels = channels;
    this.frameSizeMs = frameSizeMs;
    this.frameSize = (sampleRate * frameSizeMs) / 1000;
    this.totalFrameSize = frameSize * channels;
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
    short[] newSamples = convertByteArrayToShortArray(pcmData);
    validatePcmData(newSamples);

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

