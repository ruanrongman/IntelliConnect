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
package top.rslly.iot.utility.ai.voice.ASR;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionResult;
import com.alibaba.dashscope.audio.asr.transcription.*;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.rslly.iot.utility.MyFileUtil;
import ws.schild.jave.Encoder;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Slf4j
public class Audio2Text implements AsrService, StreamingAsrService {
  static final String STREAMING_MODEL = "qwen-audio-3.0-asr-flash-streaming";
  private static final int STREAMING_QUEUE_CAPACITY = 1024;
  private static final StreamCommand FINISH_COMMAND = new StreamCommand(null, true);

  private String apiKey;
  private volatile Semaphore realtimeSemaphore = new Semaphore(20, true);
  private int dashscopeMaxConcurrent = 20;
  private long dashscopeAcquireTimeoutMs = 30000;
  private long dashscopeStreamingFinalTimeoutMs = 10000;
  private final Set<DashScopeStreamingSession> activeStreamingSessions =
      java.util.concurrent.ConcurrentHashMap.newKeySet();
  private StreamingRecognizerFactory streamingRecognizerFactory =
      DashScopeStreamingRecognizer::new;

  @Value("${ai.audio-tmp-path}")
  private String audioPath;

  @Value("${ai.dashscope-key}")
  public void setApiKey(String apiKey) {
    // 填写自己的api key
    this.apiKey = apiKey;
  }

  @Value("${ai.asr.dashscope-max-concurrent:20}")
  public void setDashscopeMaxConcurrent(int dashscopeMaxConcurrent) {
    this.dashscopeMaxConcurrent = Math.max(1, dashscopeMaxConcurrent);
    this.realtimeSemaphore = new Semaphore(this.dashscopeMaxConcurrent, true);
  }

  @Value("${ai.asr.dashscope-acquire-timeout-ms:30000}")
  public void setDashscopeAcquireTimeoutMs(long dashscopeAcquireTimeoutMs) {
    this.dashscopeAcquireTimeoutMs = Math.max(0L, dashscopeAcquireTimeoutMs);
  }

  @Value("${ai.asr.dashscope-streaming-final-timeout-ms:10000}")
  public void setDashscopeStreamingFinalTimeoutMs(long dashscopeStreamingFinalTimeoutMs) {
    this.dashscopeStreamingFinalTimeoutMs = Math.max(1L, dashscopeStreamingFinalTimeoutMs);
  }

  @PostConstruct
  public void logAsrConcurrencyConfig() {
    log.info(
        "DashScope ASR本地并发闸门已启用: maxConcurrent={}, acquireTimeoutMs={}, streamingFinalTimeoutMs={}",
        dashscopeMaxConcurrent, dashscopeAcquireTimeoutMs, dashscopeStreamingFinalTimeoutMs);
  }

  @Override
  public StreamingAsrSession startStreaming(String sessionId) {
    DashScopeStreamingSession streamingSession =
        new DashScopeStreamingSession(sessionId, dashscopeStreamingFinalTimeoutMs);
    activeStreamingSessions.add(streamingSession);
    streamingSession.result.whenComplete(
        (text, error) -> activeStreamingSessions.remove(streamingSession));
    streamingSession.start();
    return streamingSession;
  }

  public String getText(String url) {
    try {
      var param =
          TranscriptionParam.builder()
              // 若没有将API Key配置到环境变量中，需将下面这行代码注释放开，并将apiKey替换为自己的API Key
              .apiKey(apiKey)
              .model("paraformer-v2")
              // “language_hints”只支持paraformer-v2和paraformer-realtime-v2模型
              .fileUrls(List.of(url))
              .parameter("language_hints", new String[] {"zh", "en"})
              .build();
      Transcription transcription = new Transcription();
      // 提交转写请求
      TranscriptionResult result = transcription.asyncCall(param);
      // 打印TaskId
      log.debug("TaskId: {}", result.getTaskId());
      // 等待转写完成
      result =
          transcription.wait(
              TranscriptionQueryParam.FromTranscriptionParam(param, result.getTaskId()));
      // 获取转写结果
      List<TranscriptionTaskResult> taskResultList = result.getResults();
      if (taskResultList != null && taskResultList.size() > 0) {
        TranscriptionTaskResult taskResult = taskResultList.get(0);
        // 获取转写结果的url
        String transcriptionUrl = taskResult.getTranscriptionUrl();
        // 通过Http获取url内对应的结果
        HttpURLConnection connection =
            (HttpURLConnection) new URL(transcriptionUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(connection.getInputStream()));
        // 格式化输出json结果
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        var JsonStr = gson.toJson(gson.fromJson(reader, JsonObject.class));
        JSONObject jsonObject = JSON.parseObject(JsonStr);
        String res = jsonObject.getJSONArray("transcripts").getJSONObject(0).getString("text");
        log.debug("语音转换结果{}", res);
        return res;
      } else
        return "语音识别失败";
    } catch (Exception e) {
      log.error("语音识别失败", e);
      return "语音识别失败";
    }

  }

  public String getTextRealtime(File file, int sampleRate, String format) {
    if (file == null || !file.exists() || file.length() == 0) {
      return "";
    }
    // 创建Recognition实例
    Recognition recognizer = new Recognition();
    // 创建RecognitionParam
    RecognitionParam param =
        RecognitionParam.builder()
            // 若没有将API Key配置到环境变量中，需将下面这行代码注释放开，并将apiKey替换为自己的API Key
            .apiKey(apiKey)
            .model("paraformer-realtime-v2")
            .format(format)
            .sampleRate(sampleRate)
            // “language_hints”只支持paraformer-v2和paraformer-realtime-v2模型
            .parameter("language_hints", new String[] {"zh", "en"})
            .build();

    boolean acquired = false;
    try {
      acquired = realtimeSemaphore.tryAcquire(dashscopeAcquireTimeoutMs, TimeUnit.MILLISECONDS);
      if (!acquired) {
        log.warn("DashScope ASR本地并发闸门已满: maxConcurrent={}, acquireTimeoutMs={}, file={}",
            dashscopeMaxConcurrent, dashscopeAcquireTimeoutMs,
            file == null ? "null" : file.getName());
        return "";
      }
      // System.out.println("识别结果：" + recognizer.call(param, file));
      String text = recognizer.call(param, file);
      StringBuilder sentences = new StringBuilder();
      var jsonObject = JSON.parseObject(text);
      var sentencesArray = jsonObject.getJSONArray("sentences");
      if (sentencesArray.size() > 0) {
        for (int i = 0; i < sentencesArray.size(); i++) {
          sentences.append(sentencesArray.getJSONObject(i).getString("text"));
        }
      } else {
        return "";
      }
      return sentences.toString();
    } catch (Exception e) {
      if (isRateLimitError(e)) {
        log.warn("DashScope ASR触发上游限流: {}", e.getMessage());
      } else {
        log.error("语音识别失败{}", e.getMessage());
      }
      return "";
    } finally {
      if (acquired) {
        realtimeSemaphore.release();
      }
    }
  }

  public String getTextRealtime(String url, int sampleRate, String format) {
    String filePath = audioPath; // 上传后的路径
    String fileName = UUID.randomUUID() + "." + format; // 新文件名
    try {
      InputStream in = new URL(url).openStream();
      MyFileUtil.uploadFile(in.readAllBytes(), filePath, fileName);
      return this.getTextRealtime(new File(filePath + fileName), sampleRate, format);
    } catch (Exception e) {
      e.printStackTrace();
      log.error("语音识别失败{}", e.getMessage());
      return "语音识别失败";
    } finally {
      try {
        MyFileUtil.deleteFile(filePath + fileName);
      } catch (Exception e) {
        log.error("语音识别失败{}", e.getMessage());
      }
    }
  }

  private boolean isRateLimitError(Exception e) {
    if (e == null || e.getMessage() == null) {
      return false;
    }
    String message = e.getMessage();
    return message.contains("Throttling.RateQuota")
        || message.contains("Requests rate limit exceeded");
  }

  RecognitionParam buildStreamingParam() {
    return RecognitionParam.builder()
        .apiKey(apiKey)
        .model(STREAMING_MODEL)
        .format("pcm")
        .sampleRate(16000)
        .build();
  }

  void setStreamingRecognizerFactory(StreamingRecognizerFactory streamingRecognizerFactory) {
    this.streamingRecognizerFactory = streamingRecognizerFactory;
  }

  @PreDestroy
  public void shutdownStreamingSessions() {
    new ArrayList<>(activeStreamingSessions).forEach(DashScopeStreamingSession::cancel);
  }

  interface StreamingRecognizerFactory {
    StreamingRecognizer create();
  }

  interface StreamingRecognizer {
    void start(RecognitionParam param, ResultCallback<RecognitionResult> callback);

    void sendAudioFrame(ByteBuffer pcmData);

    void stop();

    void close();
  }

  private static final class DashScopeStreamingRecognizer implements StreamingRecognizer {
    private final Recognition recognizer = new Recognition();

    @Override
    public void start(RecognitionParam param, ResultCallback<RecognitionResult> callback) {
      recognizer.call(param, callback);
    }

    @Override
    public void sendAudioFrame(ByteBuffer pcmData) {
      recognizer.sendAudioFrame(pcmData);
    }

    @Override
    public void stop() {
      recognizer.stop();
    }

    @Override
    public void close() {
      try {
        if (recognizer.getDuplexApi() != null) {
          recognizer.getDuplexApi().close(1000, "bye");
        }
      } catch (Exception ignored) {
        // The recognition result or original transport error is more useful than a close failure.
      }
    }
  }

  private record StreamCommand(byte[] pcmData, boolean finish) {}

  private final class DashScopeStreamingSession implements StreamingAsrSession {
    private final String sessionId;
    private final long finalTimeoutMs;
    private final ArrayBlockingQueue<StreamCommand> commands =
        new ArrayBlockingQueue<>(STREAMING_QUEUE_CAPACITY);
    private final CompletableFuture<String> result = new CompletableFuture<>();
    private final AtomicBoolean finishRequested = new AtomicBoolean(false);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final AtomicBoolean permitHeld = new AtomicBoolean(false);
    private final AtomicReference<String> latestText = new AtomicReference<>("");
    private final StringBuilder finalText = new StringBuilder();
    private volatile Semaphore acquiredSemaphore;
    private volatile StreamingRecognizer recognizer;
    private volatile Thread senderThread;

    private DashScopeStreamingSession(String sessionId, long finalTimeoutMs) {
      this.sessionId = sessionId;
      this.finalTimeoutMs = Math.max(1L, finalTimeoutMs);
    }

    private void start() {
      senderThread = Thread.ofVirtual()
          .name("dashscope-asr-stream-" + sessionId)
          .start(this::runSender);
      result.whenComplete((text, error) -> cleanup());
    }

    @Override
    public boolean sendPcm(byte[] pcmData) {
      if (pcmData == null || pcmData.length == 0) {
        return true;
      }
      if (finishRequested.get() || cancelled.get() || result.isDone()) {
        return false;
      }
      boolean accepted = commands.offer(new StreamCommand(pcmData.clone(), false));
      if (!accepted) {
        fail(new IllegalStateException("DashScope流式音频队列已满"));
      }
      return accepted;
    }

    @Override
    public CompletableFuture<String> finish() {
      if (finishRequested.compareAndSet(false, true) && !cancelled.get()) {
        if (!commands.offer(FINISH_COMMAND)) {
          fail(new IllegalStateException("DashScope流式音频队列无法写入结束标记"));
        } else {
          result.orTimeout(finalTimeoutMs, TimeUnit.MILLISECONDS);
        }
      }
      return result;
    }

    @Override
    public void cancel() {
      if (!cancelled.compareAndSet(false, true)) {
        return;
      }
      commands.clear();
      result.completeExceptionally(new CancellationException("DashScope流式会话已取消"));
    }

    private void runSender() {
      try {
        Semaphore semaphore = realtimeSemaphore;
        acquiredSemaphore = semaphore;
        if (!semaphore.tryAcquire(dashscopeAcquireTimeoutMs, TimeUnit.MILLISECONDS)) {
          fail(new IllegalStateException(
              "DashScope ASR本地并发闸门已满: maxConcurrent=" + dashscopeMaxConcurrent));
          return;
        }
        permitHeld.set(true);
        if (cancelled.get() || result.isDone()) {
          cleanup();
          return;
        }

        StreamingRecognizer currentRecognizer = streamingRecognizerFactory.create();
        recognizer = currentRecognizer;
        currentRecognizer.start(buildStreamingParam(), createStreamingCallback());
        while (!cancelled.get() && !result.isDone()) {
          StreamCommand command = commands.take();
          if (command.finish()) {
            currentRecognizer.stop();
            return;
          }
          currentRecognizer.sendAudioFrame(ByteBuffer.wrap(command.pcmData()));
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        if (!cancelled.get() && !result.isDone()) {
          fail(e);
        }
      } catch (Exception e) {
        fail(e);
      }
    }

    private ResultCallback<RecognitionResult> createStreamingCallback() {
      return new ResultCallback<RecognitionResult>() {
        @Override
        public void onEvent(RecognitionResult recognitionResult) {
          if (recognitionResult == null || recognitionResult.getSentence() == null
              || result.isDone()) {
            return;
          }
          String text = recognitionResult.getSentence().getText();
          if (text == null) {
            return;
          }
          if (recognitionResult.isSentenceEnd()) {
            synchronized (finalText) {
              finalText.append(text);
            }
            latestText.set("");
            log.debug("收到DashScope流式最终句: sessionId={}, text={}", sessionId, text);
          } else {
            latestText.set(text);
            log.trace("收到DashScope流式中间结果: sessionId={}, text={}", sessionId, text);
          }
        }

        @Override
        public void onComplete() {
          String completedText;
          synchronized (finalText) {
            completedText = finalText.length() > 0 ? finalText.toString() : latestText.get();
          }
          result.complete(completedText == null ? "" : completedText.trim());
        }

        @Override
        public void onError(Exception error) {
          fail(error == null
              ? new IllegalStateException("DashScope流式识别返回未知错误")
              : error);
        }
      };
    }

    private void fail(Throwable error) {
      commands.clear();
      cancelled.set(true);
      result.completeExceptionally(error);
    }

    private void cleanup() {
      cancelled.set(true);
      commands.clear();
      StreamingRecognizer currentRecognizer = recognizer;
      if (currentRecognizer != null) {
        currentRecognizer.close();
      }
      Thread currentSender = senderThread;
      if (currentSender != null && currentSender != Thread.currentThread()) {
        currentSender.interrupt();
      }
      Semaphore semaphore = acquiredSemaphore;
      if (semaphore != null && permitHeld.compareAndSet(true, false)) {
        semaphore.release();
      }
    }
  }

}
