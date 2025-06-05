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

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.audio.tts.SpeechSynthesisResult;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisAudioFormat;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesizer;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import top.rslly.iot.services.agent.ProductRoleServiceImpl;
import top.rslly.iot.utility.SseEmitterUtil;
import top.rslly.iot.utility.Websocket;
import top.rslly.iot.utility.ai.voice.concentus.OpusApplication;
import top.rslly.iot.utility.ai.voice.concentus.OpusEncoder;
import top.rslly.iot.utility.ai.voice.concentus.OpusException;
import top.rslly.iot.utility.ai.voice.concentus.OpusSignal;
import ws.schild.jave.Encoder;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

import javax.websocket.Session;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

@Component
@Slf4j
public class Text2audio {
  private static final String model = "cosyvoice-v1";
  private static final String voice = "longxiaochun";
  private static SpeechSynthesisParam param;
  @Autowired
  private ProductRoleServiceImpl productRoleService;

  // Frame duration in milliseconds (matching Opus encoding)
  private static final int FRAME_DURATION = 60;
  // Number of frames for pre-buffering
  private static final int PRE_BUFFER_COUNT = 3;

  class ReactCallback extends ResultCallback<SpeechSynthesisResult> {
    public CountDownLatch latch = new CountDownLatch(1);
    private final String chatId;
    private final Session session;

    // Only used for WebSocket audio sending.
    private final BlockingQueue<byte[]> audioQueue = new LinkedBlockingQueue<>();
    // End-of-stream marker: an empty byte array.
    private final byte[] EOS = new byte[0];
    private final OpusEncoderUtils encoder = new OpusEncoderUtils(16000, 1, 60);

    ReactCallback(String chatId, Session session) {
      this.chatId = chatId;
      this.session = session;
    }

    @Override
    public void onEvent(SpeechSynthesisResult message) {
      // Write Audio to player
      if (message.getAudioFrame() != null) {
        byte[] audio = message.getAudioFrame().array();
        if (session == null) {
          // audioPlayer.write(message.getAudioFrame());
          JSONObject aiResponse = new JSONObject();
          // audio = VoiceBitChange(audio);
          aiResponse.put("audio", Base64.getEncoder().encodeToString(audio));
          SseEmitterUtil.sendMessage(chatId, aiResponse.toJSONString());
          log.info(Arrays.toString(message.getAudioFrame().array()));
        } else {
          try {
            // For WebSocket: enqueue the frame.
            /*
             * byte[] data_packet = new byte[16000]; OpusEncoder opusEncoder = new
             * OpusEncoder(16000, 1, OpusApplication.OPUS_APPLICATION_AUDIO);
             * opusEncoder.setBitrate(16000);
             * opusEncoder.setSignalType(OpusSignal.OPUS_SIGNAL_VOICE);
             * opusEncoder.setComplexity(10); int bytesEncoded = opusEncoder.encode(audio, 0,
             * (16000*60)/1000, data_packet, 0, audio.length); byte[] packet = new
             * byte[bytesEncoded]; System.arraycopy(data_packet, 0, packet, 0, bytesEncoded);
             */
            List<byte[]> packets = encoder.encodePcmToOpus(audio, false);
            for (byte[] packet : packets) {
              audioQueue.offer(packet);
            }
            // audioQueue.offer(packet);
          } catch (Exception e) {
            log.error("sendBinary error{}", e.getMessage());
          }
        }
      }
    }

    @Override
    public void onComplete() {
      System.out.println("synthesis onComplete!");
      if (session == null) {
        SseEmitterUtil.removeUser(chatId);
      } else {
        List<byte[]> packets = encoder.encodePcmToOpus(new byte[0], true);
        for (byte[] packet : packets) {
          audioQueue.offer(packet);
        }
        // Signal end-of-stream by adding an empty array.
        audioQueue.offer(EOS);
        // Asynchronously send the queued frames.
        asyncSendAudioQueue(chatId, session, audioQueue);
      }
      latch.countDown();
    }

    @Override
    public void onError(Exception e) {
      System.out.println("synthesis onError!");
      if (session == null) {
        SseEmitterUtil.removeUser(chatId);
      }
      e.printStackTrace();
    }

    public void waitForComplete() throws InterruptedException {
      latch.await();
    }
  }

  @Value("${ai.dashscope-key}")
  public void setApiKey(String apiKey) {
    // 填写自己的api key
    param =
        SpeechSynthesisParam.builder()
            // 若没有将API Key配置到环境变量中，需将下面这行代码注释放开，并将apiKey替换为自己的API Key
            .apiKey(apiKey)
            .model(model)
            .format(SpeechSynthesisAudioFormat.PCM_16000HZ_MONO_16BIT)
            .voice(voice)
            .build();
  }

  public static ByteBuffer synthesizeAndSaveAudio(String text) {

    SpeechSynthesizer synthesizer = new SpeechSynthesizer(param, null);
    ByteBuffer audio = synthesizer.call(text);
    log.info("requestId{}", synthesizer.getLastRequestId());
    // log.info(Arrays.toString(audio.array()));
    return audio;
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

  @Async("taskExecutor")
  public void asyncSynthesizeAndSaveAudio(String text, String chatId) {
    ReactCallback callback = new ReactCallback(chatId, null);
    SpeechSynthesizer synthesizer = new SpeechSynthesizer(param, callback);
    synthesizer.call(text);
  }

  @Async("taskExecutor")
  public void websocketAudio(String text, Session session, String chatId) {
    ReactCallback callback = new ReactCallback(chatId, session);
    try {
      var roles = productRoleService.findAllByProductId(Integer.parseInt(chatId));
      if (!roles.isEmpty() && roles.get(0).getVoice() != null) {
        param.setVoice(roles.get(0).getVoice());
      }
    } catch (Exception e) {
      log.error("websocketAudio error{}", e.getMessage());
    }
    SpeechSynthesizer synthesizer = new SpeechSynthesizer(param, callback);
    synthesizer.call(text);
    try {
      callback.waitForComplete();
    } catch (InterruptedException e) {
      log.error("waitForComplete interrupted: {}", e.getMessage());
    } finally {
      try {
        session.getBasicRemote().sendText("{\"type\":\"tts\",\"state\":\"stop\"}");
      } catch (IOException ex) {
        log.warn("Failed to send stop message: {}", ex.getMessage());
      }
      Websocket.isAbort.put(chatId, false);
    }
  }

  public void websocketAudioSync(String text, Session session, String chatId) {
    ReactCallback callback = new ReactCallback(chatId, session);
    try {
      var roles = productRoleService.findAllByProductId(Integer.parseInt(chatId));
      if (!roles.isEmpty() && roles.get(0).getVoice() != null) {
        param.setVoice(roles.get(0).getVoice());
      }
    } catch (Exception e) {
      log.error("websocketAudio error{}", e.getMessage());
    }
    SpeechSynthesizer synthesizer = new SpeechSynthesizer(param, callback);
    synthesizer.call(text);
    try {
      callback.waitForComplete();
    } catch (InterruptedException e) {
      log.error("waitForComplete error{}", e.getMessage());
    }
  }

  private static ByteBuffer byte2Bytebuffer(byte[] byteArray) {

    // 初始化一个和byte长度一样的buffer
    ByteBuffer buffer = ByteBuffer.allocate(byteArray.length);
    // 数组放到buffer中
    buffer.put(byteArray);
    // 重置 limit 和postion 值 否则 buffer 读取数据不对
    buffer.flip();
    return buffer;
  }

  @Async("taskExecutor")
  public void asyncSendAudioQueue(String chatId, Session session, BlockingQueue<byte[]> queue) {
    try {
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
      for (int i = 0; i < preBuffer.size(); i++) {
        session.getBasicRemote().sendBinary(byte2Bytebuffer(preBuffer.get(i)));
        // log.info("预缓冲帧 {}, 时间: {}ms", i, (System.nanoTime() - startTime) / 1_000_000.0);
      }
      // Continue sending remaining frames with timing control.
      while (!Websocket.isAbort.get(chatId)) {
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
        session.getBasicRemote().sendBinary(byte2Bytebuffer(opusPacket));
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

