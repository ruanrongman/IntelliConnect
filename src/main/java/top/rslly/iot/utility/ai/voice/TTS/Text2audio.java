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
import com.alibaba.dashscope.protocol.ConnectionConfigurations;
import com.alibaba.dashscope.protocol.ConnectionOptions;
import com.alibaba.dashscope.utils.Constants;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import top.rslly.iot.services.agent.ProductRoleServiceImpl;
import top.rslly.iot.utility.ai.voice.AudioFrameDuration;
import top.rslly.iot.utility.ai.voice.OpusEncoderUtils;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Component
@Slf4j
public class Text2audio implements TtsService {
  private static final String model = "cosyvoice-v1";
  private static final String voice = "longxiaochun";
  private static final int BOUNDARY_FADE_MS = 5;
  private static SpeechSynthesisParam param;
  private static volatile ConnectionOptions connectionOptions;
  private volatile Semaphore ttsSemaphore = new Semaphore(64, true);
  private int dashscopeMaxConcurrent = 64;
  private long dashscopeAcquireTimeoutMs = 5000;
  private int connectionPoolSize = 128;
  private int maximumAsyncRequests = 128;
  private int maximumAsyncRequestsPerHost = 128;
  private long connectTimeoutSeconds = 10;
  private long writeTimeoutSeconds = 20;
  private long readTimeoutSeconds = 120;
  @Autowired
  private ProductRoleServiceImpl productRoleService;

  static class ReactCallback extends ResultCallback<SpeechSynthesisResult> {
    private final CountDownLatch latch = new CountDownLatch(1);
    private final Consumer<byte[]> onChunk;
    private final OpusEncoderUtils encoder;
    private final AtomicBoolean terminal = new AtomicBoolean();
    private volatile boolean emitted;
    private volatile boolean failed;

    ReactCallback(String chatId, Consumer<byte[]> onChunk) {
      this.onChunk = onChunk;
      this.encoder =
          new OpusEncoderUtils(AudioFrameDuration.resolveOutboundSampleRate(chatId), 1,
              AudioFrameDuration.resolveOutboundFrameDurationMs(chatId), BOUNDARY_FADE_MS);
    }

    @Override
    public synchronized void onEvent(SpeechSynthesisResult message) {
      if (terminal.get() || failed || message.getAudioFrame() == null) {
        return;
      }
      acceptPcm(readAllBytes(message.getAudioFrame()));
    }

    synchronized void acceptPcm(byte[] pcmData) {
      if (terminal.get() || failed || pcmData == null || pcmData.length == 0) {
        return;
      }
      try {
        emitPackets(encoder.encodePcmToOpus(pcmData, false));
      } catch (Exception e) {
        failed = true;
        log.error("PCM processing error: {}", e.getMessage(), e);
      }
    }

    @Override
    public synchronized void onComplete() {
      if (!terminal.compareAndSet(false, true)) {
        return;
      }
      log.debug("synthesis onComplete!");
      try {
        if (!failed) {
          emitPackets(encoder.encodePcmToOpus(new byte[0], true));
        }
      } catch (Exception e) {
        failed = true;
        log.error("PCM processing error: {}", e.getMessage(), e);
      } finally {
        latch.countDown();
      }
    }

    @Override
    public synchronized void onError(Exception e) {
      if (!terminal.compareAndSet(false, true)) {
        return;
      }
      log.debug("synthesis onError!");
      failed = true;
      latch.countDown();
      log.error("TTS合成失败: {}", e.getMessage());
    }

    synchronized void cancel() {
      if (terminal.compareAndSet(false, true)) {
        failed = true;
        latch.countDown();
      }
    }

    public void waitForComplete() throws InterruptedException {
      latch.await();
    }

    boolean hasAudio() {
      return emitted && !failed;
    }

    private void emitPackets(List<byte[]> packets) {
      for (byte[] packet : packets) {
        if (packet == null || packet.length == 0) {
          continue;
        }
        onChunk.accept(packet);
        emitted = true;
      }
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

  @Value("${ai.tts.dashscope-max-concurrent:64}")
  public void setDashscopeMaxConcurrent(int dashscopeMaxConcurrent) {
    this.dashscopeMaxConcurrent = Math.max(1, dashscopeMaxConcurrent);
    this.ttsSemaphore = new Semaphore(this.dashscopeMaxConcurrent, true);
  }

  @Value("${ai.tts.dashscope-acquire-timeout-ms:5000}")
  public void setDashscopeAcquireTimeoutMs(long dashscopeAcquireTimeoutMs) {
    this.dashscopeAcquireTimeoutMs = Math.max(0L, dashscopeAcquireTimeoutMs);
  }

  @Value("${ai.tts.dashscope-connection-pool-size:128}")
  public void setConnectionPoolSize(int connectionPoolSize) {
    this.connectionPoolSize = Math.max(1, connectionPoolSize);
  }

  @Value("${ai.tts.dashscope-max-async-requests:128}")
  public void setMaximumAsyncRequests(int maximumAsyncRequests) {
    this.maximumAsyncRequests = Math.max(1, maximumAsyncRequests);
  }

  @Value("${ai.tts.dashscope-max-async-requests-per-host:128}")
  public void setMaximumAsyncRequestsPerHost(int maximumAsyncRequestsPerHost) {
    this.maximumAsyncRequestsPerHost = Math.max(1, maximumAsyncRequestsPerHost);
  }

  @Value("${ai.tts.dashscope-connect-timeout-seconds:10}")
  public void setConnectTimeoutSeconds(long connectTimeoutSeconds) {
    this.connectTimeoutSeconds = Math.max(1L, connectTimeoutSeconds);
  }

  @Value("${ai.tts.dashscope-write-timeout-seconds:20}")
  public void setWriteTimeoutSeconds(long writeTimeoutSeconds) {
    this.writeTimeoutSeconds = Math.max(1L, writeTimeoutSeconds);
  }

  @Value("${ai.tts.dashscope-read-timeout-seconds:120}")
  public void setReadTimeoutSeconds(long readTimeoutSeconds) {
    this.readTimeoutSeconds = Math.max(1L, readTimeoutSeconds);
  }

  @PostConstruct
  public void configureDashScopeConnectionPool() {
    Constants.connectionConfigurations = ConnectionConfigurations.builder()
        .connectionPoolSize(connectionPoolSize)
        .maximumAsyncRequests(maximumAsyncRequests)
        .maximumAsyncRequestsPerHost(maximumAsyncRequestsPerHost)
        .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
        .writeTimeout(Duration.ofSeconds(writeTimeoutSeconds))
        .readTimeout(Duration.ofSeconds(readTimeoutSeconds))
        .build();
    connectionOptions = ConnectionOptions.builder()
        .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
        .writeTimeout(Duration.ofSeconds(writeTimeoutSeconds))
        .readTimeout(Duration.ofSeconds(readTimeoutSeconds))
        .build();
    log.info(
        "DashScope TTS连接池配置: poolSize={}, maxRequests={}, maxRequestsPerHost={}, "
            + "connectTimeout={}s, writeTimeout={}s, readTimeout={}s, localMaxConcurrent={}, "
            + "localAcquireTimeoutMs={}",
        connectionPoolSize, maximumAsyncRequests, maximumAsyncRequestsPerHost,
        connectTimeoutSeconds, writeTimeoutSeconds, readTimeoutSeconds, dashscopeMaxConcurrent,
        dashscopeAcquireTimeoutMs);
  }

  public static ByteBuffer synthesizeAndSaveAudio(String text) {
    return synthesizeAndSaveAudio(text, null);
  }

  public static ByteBuffer synthesizeAndSaveAudio(String text, String voice) {
    String model = param.getModel();
    String voiceId = StringUtils.isNotBlank(voice) ? voice : param.getVoice();
    if (voiceId.startsWith("cosy_v2_")) {
      model = "cosyvoice-v2";
      voiceId = voiceId.substring(8);
    }
    SpeechSynthesisParam localParam = SpeechSynthesisParam.builder()
        .apiKey(param.getApiKey())
        .model(model)
        .format(SpeechSynthesisAudioFormat.MP3_16000HZ_MONO_128KBPS)
        .voice(voiceId)
        .build();
    SpeechSynthesizer synthesizer = new SpeechSynthesizer(localParam, null, null,
        connectionOptions);
    ByteBuffer audio = synthesizer.call(text);
    log.debug("requestId{}", synthesizer.getLastRequestId());
    // log.info(Arrays.toString(audio.array()));
    return audio;
  }

  private static SpeechSynthesisAudioFormat getPcmFormat(String chatId) {
    return switch (AudioFrameDuration.resolveOutboundSampleRate(chatId)) {
      case 24000 -> SpeechSynthesisAudioFormat.PCM_24000HZ_MONO_16BIT;
      case 48000 -> SpeechSynthesisAudioFormat.PCM_48000HZ_MONO_16BIT;
      default -> SpeechSynthesisAudioFormat.PCM_16000HZ_MONO_16BIT;
    };
  }

  @Async("taskExecutor")
  public void asyncSynthesizeAndSaveAudio(String text, String chatId) {
    ReactCallback callback = new ReactCallback(chatId, ignored -> {
    });
    SpeechSynthesizer synthesizer = new SpeechSynthesizer(param, callback, null,
        connectionOptions);
    synthesizer.call(text);
  }

  @Override
  public List<byte[]> getTextAudio(String chatId, String text, Float pitch, Float speed,
      String voice) {
    List<byte[]> audioFrames = new ArrayList<>();
    boolean success = streamTextAudio(chatId, text, pitch, speed, voice, audioFrames::add);
    if (!success) {
      return null;
    }
    audioFrames.add(AUDIO_EOS);
    return audioFrames;
  }

  @Override
  public boolean streamTextAudio(String chatId, String text, Float pitch, Float speed,
      String voice, Consumer<byte[]> onChunk) {
    ReactCallback callback = new ReactCallback(chatId, onChunk);
    try {
      String model = param.getModel();
      String voiceId = StringUtils.isNotBlank(voice) ? voice : param.getVoice();
      if (voiceId.startsWith("cosy_v2_")) {
        model = "cosyvoice-v2";
        voiceId = voiceId.substring(8);
        log.debug(model);
        log.debug(voiceId);
      }
      // 创建线程安全的参数副本
      SpeechSynthesisParam localParam = SpeechSynthesisParam.builder()
          .apiKey(param.getApiKey())
          .model(model)
          .format(getPcmFormat(chatId))
          .pitchRate(pitch)
          .speechRate(speed)
          .voice(voiceId)
          .build();
      SpeechSynthesizer synthesizer = new SpeechSynthesizer(localParam, callback, null,
          connectionOptions);
      acquireDashScopeTts("streamTextAudio", chatId, text);
      try {
        synthesizer.call(text);
        callback.waitForComplete();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.error("waitForComplete error{}", e.getMessage());
        callback.cancel();
      } finally {
        ttsSemaphore.release();
      }
      if (!callback.hasAudio()) {
        log.warn("TTS未生成有效音频: chatId={}, textLength={}", chatId,
            text == null ? 0 : text.length());
        return false;
      }
      return true;
    } catch (InterruptedException e) {
      callback.cancel();
      Thread.currentThread().interrupt();
      log.error("等待DashScope TTS并发许可被中断: chatId={}", chatId);
    } catch (Exception e) {
      callback.cancel();
      log.error("streamTextAudio error{}", e.getMessage());
    }
    return false;
  }

  private void acquireDashScopeTts(String scene, String chatId, String text)
      throws InterruptedException {
    long waitStartNs = System.nanoTime();
    boolean acquired = ttsSemaphore.tryAcquire(dashscopeAcquireTimeoutMs, TimeUnit.MILLISECONDS);
    if (!acquired) {
      long waitMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - waitStartNs);
      log.warn(
          "DashScope TTS本地并发闸门已满: scene={}, chatId={}, maxConcurrent={}, "
              + "acquireTimeoutMs={}, waitedMs={}, textLength={}, text={}",
          scene, chatId, dashscopeMaxConcurrent, dashscopeAcquireTimeoutMs, waitMs,
          text == null ? 0 : text.length(), abbreviate(text));
      throw new IllegalStateException("DashScope TTS local concurrency gate timeout");
    }
  }

  private String abbreviate(String text) {
    if (text == null) {
      return "";
    }
    String normalized = text.replace("\r", " ").replace("\n", " ").trim();
    return normalized.length() <= 40 ? normalized : normalized.substring(0, 40) + "...";
  }
}
