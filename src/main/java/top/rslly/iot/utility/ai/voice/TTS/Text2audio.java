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

import com.alibaba.dashscope.audio.tts.SpeechSynthesisResult;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisAudioFormat;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesizer;
import com.alibaba.dashscope.common.ResultCallback;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import top.rslly.iot.services.agent.ProductRoleServiceImpl;
import top.rslly.iot.utility.SseEmitterUtil;
import top.rslly.iot.utility.ai.voice.AudioUtils;
import top.rslly.iot.utility.ai.voice.OpusEncoderUtils;

import jakarta.websocket.Session;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

@Component
@Slf4j
public class Text2audio implements TtsService {
  private static final String model = "cosyvoice-v1";
  private static final String voice = "longxiaochun";
  private static SpeechSynthesisParam param;
  @Autowired
  private ProductRoleServiceImpl productRoleService;

  @Data
  static class ReactCallback extends ResultCallback<SpeechSynthesisResult> {
    public CountDownLatch latch = new CountDownLatch(1);
    private final String chatId;
    private final Session session;
    private final long generation;

    // Only used for WebSocket audio sending.
    private final BlockingQueue<byte[]> audioQueue = new LinkedBlockingQueue<>();
    private List<byte[]> bytes;
    // End-of-stream marker: an empty byte array.
    private final byte[] EOS = new byte[0];
    private final OpusEncoderUtils encoder = new OpusEncoderUtils(16000, 1, 60);
    private boolean sendAfterHandler = true;
    private final ByteArrayOutputStream pcmBuffer = new ByteArrayOutputStream();

    ReactCallback(String chatId, Session session, long generation) {
      this.chatId = chatId;
      this.session = session;
      this.generation = generation;
      if (session == null) {
        bytes = new ArrayList<>();
        sendAfterHandler = false;
      }
    }

    @Override
    public void onEvent(SpeechSynthesisResult message) {
      if (message.getAudioFrame() != null) {
        try {
          pcmBuffer.write(readAllBytes(message.getAudioFrame()));
        } catch (Exception e) {
          log.error("sendBinary error{}", e.getMessage());
        }
      }
    }

    @Override
    public void onComplete() {
      log.debug("synthesis onComplete!");
      try {
        if (pcmBuffer.size() > 0) {
          byte[] pcmData = pcmBuffer.toByteArray();
          List<byte[]> packets = encoder.encodePcmToOpus(pcmData, false);
          if (!sendAfterHandler) {
            bytes.addAll(packets);
          }
          for (byte[] packet : packets) {
            audioQueue.offer(packet);
          }

          // Flush encoder
          packets = encoder.encodePcmToOpus(new byte[0], true);
          if (!sendAfterHandler) {
            bytes.addAll(packets);
          }
          for (byte[] packet : packets) {
            audioQueue.offer(packet);
          }
        }
      } catch (Exception e) {
        log.error("PCM processing error: {}", e.getMessage());
      }

      if (!sendAfterHandler) {
        bytes.add(EOS);
        latch.countDown();
        return;
      }
      // Signal end-of-stream by adding an empty array.
      audioQueue.offer(EOS);
      // Asynchronously send the queued frames.
      AudioUtils.asyncSendAudioQueue(chatId, session, audioQueue, generation);
      latch.countDown();
    }

    @Override
    public void onError(Exception e) {
      log.debug("synthesis onError!");
      if (session == null) {
        SseEmitterUtil.removeUser(chatId);
      }
      latch.countDown(); // 确保释放等待锁
      log.error("TTS合成失败: {}", e.getMessage());
    }

    public void waitForComplete() throws InterruptedException {
      latch.await();
    }

    private byte[] readAllBytes(ByteBuffer buffer) {
      ByteBuffer duplicate = buffer.asReadOnlyBuffer();
      byte[] data = new byte[duplicate.remaining()];
      duplicate.get(data);
      return data;
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
    SpeechSynthesisParam localParam = SpeechSynthesisParam.builder()
        .apiKey(param.getApiKey())
        .model(param.getModel())
        .format(SpeechSynthesisAudioFormat.MP3_16000HZ_MONO_128KBPS)
        .voice(param.getVoice())
        .build();
    SpeechSynthesizer synthesizer = new SpeechSynthesizer(localParam, null);
    ByteBuffer audio = synthesizer.call(text);
    log.debug("requestId{}", synthesizer.getLastRequestId());
    // log.info(Arrays.toString(audio.array()));
    return audio;
  }

  @Async("taskExecutor")
  public void asyncSynthesizeAndSaveAudio(String text, String chatId) {
    ReactCallback callback = new ReactCallback(chatId, null, 0L);
    SpeechSynthesizer synthesizer = new SpeechSynthesizer(param, callback);
    synthesizer.call(text);
  }

  @Override
  public void websocketAudioSync(String text, Float pitch, Float speed, Session session,
      String chatId, String voice, long generation) {
    ReactCallback callback = new ReactCallback(chatId, session, generation);
    try {
      String model = param.getModel();
      if (voice != null && voice.startsWith("cosy_v2_")) {
        model = "cosyvoice-v2";
        voice = voice.substring(8);
        log.debug(model);
        log.debug(voice);
      }
      // 创建线程安全的参数副本
      SpeechSynthesisParam localParam = SpeechSynthesisParam.builder()
          .apiKey(param.getApiKey())
          .model(model)
          .format(SpeechSynthesisAudioFormat.PCM_16000HZ_MONO_16BIT)
          .pitchRate(pitch)
          .speechRate(speed)
          .voice(StringUtils.isNotBlank(voice) ? voice : param.getVoice())
          .build();
      SpeechSynthesizer synthesizer = new SpeechSynthesizer(localParam, callback);
      synthesizer.call(text);
      try {
        callback.waitForComplete();
      } catch (InterruptedException e) {
        log.error("waitForComplete error{}", e.getMessage());
      }
    } catch (Exception e) {
      log.error("websocketAudio error{}", e.getMessage());
    }
  }

  @Override
  public List<byte[]> getTextAudio(String chatId, String text, Float pitch, Float speed,
      String voice) {
    ReactCallback callback = new ReactCallback(chatId, null, 0L);
    try {
      String model = param.getModel();
      String voiceId = param.getVoice();
      if (voice != null && voice.startsWith("cosy_v2_")) {
        model = "cosyvoice-v2";
        voiceId = voice.substring(8);
        log.debug(model);
        log.debug(voiceId);
      }
      // 创建线程安全的参数副本
      SpeechSynthesisParam localParam = SpeechSynthesisParam.builder()
          .apiKey(param.getApiKey())
          .model(model)
          .format(SpeechSynthesisAudioFormat.PCM_16000HZ_MONO_16BIT)
          .pitchRate(pitch)
          .speechRate(speed)
          .voice(StringUtils.isNotBlank(voice) ? voiceId : param.getVoice())
          .build();
      SpeechSynthesizer synthesizer = new SpeechSynthesizer(localParam, callback);
      synthesizer.call(text);
      callback.waitForComplete();
      if (callback.bytes == null || callback.bytes.isEmpty()) {
        log.warn("TTS未生成有效音频: chatId={}, textLength={}", chatId,
            text == null ? 0 : text.length());
        return null;
      }
      return callback.bytes;
    } catch (Exception e) {
      log.error("getTextAudio error{}", e.getMessage());
    }
    return null;
  }
}
