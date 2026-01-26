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

import lombok.extern.slf4j.Slf4j;
import top.rslly.iot.utility.smartVoice.XiaoZhiWebsocket;
import ws.schild.jave.Encoder;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

import jakarta.websocket.Session;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

@Slf4j
public class AudioUtils {
  // Frame duration in milliseconds (matching Opus encoding)
  private static final int FRAME_DURATION = 60;
  // Number of frames for pre-buffering
  private static final int PRE_BUFFER_COUNT = 3;

  public static byte[] convertMp3ToPcm(String mp3FilePath) {
    if (mp3FilePath == null || mp3FilePath.trim().isEmpty()) {
      throw new IllegalArgumentException("Input file path cannot be null or empty");
    }

    File sourceFile = new File(mp3FilePath);
    if (!sourceFile.exists()) {
      throw new IllegalArgumentException("Source file not found: " + mp3FilePath);
    }

    File tempOutputFile = null;
    try {

      tempOutputFile = File.createTempFile("tempOutput", ".wav");

      // 3. 设置目标音频属性 (PCM)
      AudioAttributes audio = new AudioAttributes();
      audio.setCodec("pcm_s16le"); // 16位, 小端序
      audio.setChannels(1); // 单声道
      audio.setSamplingRate(16000); // 16kHz采样率

      EncodingAttributes attrs = new EncodingAttributes();
      attrs.setOutputFormat("wav"); // 先转成 wav
      attrs.setAudioAttributes(audio);

      Encoder encoder = new Encoder();
      encoder.encode(new MultimediaObject(sourceFile), tempOutputFile, attrs);

      // 读取 wav 文件
      byte[] wavBytes = Files.readAllBytes(tempOutputFile.toPath());


      return wavBytesToPcm(wavBytes);

    } catch (Exception e) {
      throw new RuntimeException("Error during MP3 to PCM encoding from file: " + mp3FilePath, e);
    } finally {
      //
      if (tempOutputFile != null && tempOutputFile.exists()) {
        tempOutputFile.delete();
      }
    }
  }

  public static byte[] VoiceBitChange(byte[] bytes) {
    if (bytes == null || bytes.length == 0) {
      throw new IllegalArgumentException("Input byte array cannot be null or empty");
    }

    File tempInputFile = null;
    File tempOutputFile = null;
    try {
      // 创建临时文件
      tempInputFile = File.createTempFile("tempInput", ".tmp");
      tempOutputFile = File.createTempFile("tempOutput", ".mp3");

      // 将二进制数组写入临时文件
      try (FileOutputStream fos = new FileOutputStream(tempInputFile)) {
        fos.write(bytes);
      }

      // Audio Attributes
      AudioAttributes audio = new AudioAttributes();
      audio.setCodec("mp3");
      audio.setBitRate(16000);
      audio.setChannels(1);
      audio.setSamplingRate(16000);

      // Encoding attributes
      EncodingAttributes attrs = new EncodingAttributes();
      attrs.setOutputFormat("mp3");
      attrs.setAudioAttributes(audio);

      // Encode
      Encoder encoder = new Encoder();
      encoder.encode(new MultimediaObject(tempInputFile), tempOutputFile, attrs);

      // 将生成的MP3文件读回为二进制数组
      return Files.readAllBytes(tempOutputFile.toPath());

    } catch (Exception e) {
      throw new RuntimeException("Error during encoding: " + e.getMessage(), e);
    } finally {
      // 安全删除临时文件
      if (tempInputFile != null && tempInputFile.exists()) {
        tempInputFile.delete();
      }
      if (tempOutputFile != null && tempOutputFile.exists()) {
        tempOutputFile.delete();
      }
    }
  }

  /**
   * 从WAV字节数据中提取PCM数据
   *
   * @param wavData WAV文件的字节数据
   * @return PCM数据字节数组
   */
  public static byte[] wavBytesToPcm(byte[] wavData) throws IOException {
    if (wavData == null || wavData.length < 44) { // WAV头至少44字节
      throw new IOException("无效的WAV数据");
    }

    // 检查WAV文件标识
    if (wavData[0] != 'R' || wavData[1] != 'I' || wavData[2] != 'F' || wavData[3] != 'F' ||
        wavData[8] != 'W' || wavData[9] != 'A' || wavData[10] != 'V' || wavData[11] != 'E') {
      throw new IOException("不是有效的WAV文件格式");
    }

    // 查找data子块
    int dataOffset = -1;
    for (int i = 12; i < wavData.length - 4; i++) {
      if (wavData[i] == 'd' && wavData[i + 1] == 'a' && wavData[i + 2] == 't'
          && wavData[i + 3] == 'a') {
        dataOffset = i + 8; // 跳过"data"和数据大小字段
        break;
      }
    }

    if (dataOffset == -1) {
      throw new IOException("在WAV文件中找不到data子块");
    }

    // 计算PCM数据大小
    int dataSize = wavData.length - dataOffset;

    // 提取PCM数据
    byte[] pcmData = new byte[dataSize];
    System.arraycopy(wavData, dataOffset, pcmData, 0, dataSize);

    return pcmData;
  }

  public static ByteBuffer byte2Bytebuffer(byte[] byteArray) {

    // 初始化一个和byte长度一样的buffer
    ByteBuffer buffer = ByteBuffer.allocate(byteArray.length);
    // 数组放到buffer中
    buffer.put(byteArray);
    // 重置 limit 和postion 值 否则 buffer 读取数据不对
    buffer.flip();
    return buffer;
  }

  public static void asyncSendAudioQueue(String chatId, Session session,
      BlockingQueue<byte[]> queue) {
    try {
      // 首先检查会话是否有效
      if (session == null || !session.isOpen()) {
        log.warn("WebSocket session is null or closed for chatId: {}", chatId);
        queue.clear();
        return;
      }
      final long startTime = System.nanoTime();
      int playPosition = 0;

      // Pre-buffer: collect up to PRE_BUFFER_COUNT frames.
      List<byte[]> preBuffer = new ArrayList<>();
      for (int i = 0; i < PRE_BUFFER_COUNT; i++) {
        byte[] frame = queue.take();
        if (frame.length == 0) { // EOS encountered.
          break;
        }
        preBuffer.add(frame);
      }
      // Send pre-buffer frames immediately.
      for (byte[] bytes : preBuffer) {
        session.getBasicRemote().sendBinary(AudioUtils.byte2Bytebuffer(bytes));
        // log.info("预缓冲帧 {}, 时间: {}ms", i, (System.nanoTime() - startTime) / 1_000_000.0);
      }
      // Continue sending remaining frames with timing control.
      while (!XiaoZhiWebsocket.isAbort.get(chatId)) {
        byte[] opusPacket = queue.take();
        if (opusPacket.length == 0) { // End-of-stream marker.
          break;
        }
        long expectedTimeNs = startTime + playPosition * 1_000_000L;
        long currentTimeNs = System.nanoTime();
        long delayNs = expectedTimeNs - currentTimeNs;
        if (delayNs > 0) {
          long delayMs = delayNs / 1_000_000L;
          int extraNanos = (int) (delayNs % 1_000_000L);
          Thread.sleep(delayMs, extraNanos);
        }
        long beforeSend = System.nanoTime();
        session.getBasicRemote().sendBinary(AudioUtils.byte2Bytebuffer(opusPacket));
        double actualIntervalMs = (System.nanoTime() - beforeSend) / 1_000_000.0;
        // log.info("发送帧，位置: {}ms, 实际间隔: {}ms", playPosition, actualIntervalMs);
        playPosition += FRAME_DURATION;
      }
      queue.clear();
    } catch (Exception e) {
      log.error("Error in asyncSendAudioQueue: {}", e.getMessage(), e);
    }
  }
}
