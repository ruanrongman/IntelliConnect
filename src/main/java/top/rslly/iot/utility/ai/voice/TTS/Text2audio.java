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
import top.rslly.iot.utility.smartVoice.XiaoZhiWebsocket;

import jakarta.websocket.Session;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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

    // Only used for WebSocket audio sending.
    private final BlockingQueue<byte[]> audioQueue = new LinkedBlockingQueue<>();
    private List<byte[]> bytes;
    // End-of-stream marker: an empty byte array.
    private final byte[] EOS = new byte[0];
    private final OpusEncoderUtils encoder = new OpusEncoderUtils(16000, 1, 60);
    private boolean sendAfterHandler = true;
    private final ByteArrayOutputStream mp3Buffer = new ByteArrayOutputStream();

    ReactCallback(String chatId, Session session) {
      this.chatId = chatId;
      this.session = session;
      if (session == null) {
        bytes = new ArrayList<>();
        sendAfterHandler = false;
      }
    }

    @Override
    public void onEvent(SpeechSynthesisResult message) {
      // Write Audio to player
      if (message.getAudioFrame() != null) {
        byte[] audio = message.getAudioFrame().array();
        try {
          // Accumulate MP3 chunks - process all at once onComplete
          mp3Buffer.write(audio);
        } catch (Exception e) {
          log.error("sendBinary error{}", e.getMessage());
        }
      }
    }

    @Override
    public void onComplete() {
      log.debug("synthesis onComplete!");
      try {
        if (mp3Buffer.size() > 0) {
          // All MP3 data received - now convert to PCM
          byte[] mp3Data = mp3Buffer.toByteArray();
          String outputPath = System.getProperty("java.io.tmpdir");
          String uuid = UUID.randomUUID().toString();
          String tempFilePath = Paths.get(outputPath, uuid + ".mp3").toString();
          Files.write(Paths.get(tempFilePath), mp3Data);

          // Convert MP3 to PCM (16kHz mono)
          byte[] pcmData = AudioUtils.convertMp3ToPcm(tempFilePath);

          // Encode PCM to Opus
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

          // Cleanup temp file
          Files.deleteIfExists(Paths.get(tempFilePath));
        }
      } catch (Exception e) {
        log.error("MP3 processing error: {}", e.getMessage());
      }

      if (!sendAfterHandler) {
        bytes.add(EOS);
        latch.countDown();
        return;
      }
      // Signal end-of-stream by adding an empty array.
      audioQueue.offer(EOS);
      // Asynchronously send the queued frames.
      AudioUtils.asyncSendAudioQueue(chatId, session, audioQueue);
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
  }

  @Value("${ai.dashscope-key}")
  public void setApiKey(String apiKey) {
    // 填写自己的api key
    param =
        SpeechSynthesisParam.builder()
            // 若没有将API Key配置到环境变量中，需将下面这行代码注释放开，并将apiKey替换为自己的API Key
            .apiKey(apiKey)
            .model(model)
            .format(SpeechSynthesisAudioFormat.MP3_16000HZ_MONO_128KBPS)
            .voice(voice)
            .build();
  }

  public static ByteBuffer synthesizeAndSaveAudio(String text) {

    SpeechSynthesizer synthesizer = new SpeechSynthesizer(param, null);
    ByteBuffer audio = synthesizer.call(text);
    log.debug("requestId{}", synthesizer.getLastRequestId());
    // log.info(Arrays.toString(audio.array()));
    return audio;
  }

  @Async("taskExecutor")
  public void asyncSynthesizeAndSaveAudio(String text, String chatId) {
    ReactCallback callback = new ReactCallback(chatId, null);
    SpeechSynthesizer synthesizer = new SpeechSynthesizer(param, callback);
    synthesizer.call(text);
  }

  @Override
  public void websocketAudioSync(String text, Float pitch, Float speed, Session session,
      String chatId, String voice) {
    ReactCallback callback = new ReactCallback(chatId, session);
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
          .format(SpeechSynthesisAudioFormat.MP3_16000HZ_MONO_128KBPS)
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
    ReactCallback callback = new ReactCallback(chatId, null);
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
          .format(SpeechSynthesisAudioFormat.MP3_16000HZ_MONO_128KBPS)
          .pitchRate(pitch)
          .speechRate(speed)
          .voice(StringUtils.isNotBlank(voice) ? voiceId : param.getVoice())
          .build();
      SpeechSynthesizer synthesizer = new SpeechSynthesizer(localParam, callback);
      synthesizer.call(text);
      callback.waitForComplete();
      return callback.bytes;
    } catch (Exception e) {
      log.error("getTextAudio error{}", e.getMessage());
    }
    return null;
  }
}
