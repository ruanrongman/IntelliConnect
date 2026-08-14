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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.CloseReason;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.SSLContext;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.rslly.iot.utility.ai.voice.AudioUtils;

@Slf4j
@Component
public class FunAsrClient implements AsrService, StreamingAsrService {

  private static final int DEFAULT_TIMEOUT_SECONDS = 2000;
  private static final int STREAMING_QUEUE_CAPACITY = 1024;
  private static final Pattern TEXT_CLEANUP_PATTERN =
      Pattern.compile("<\\|(.*?)\\|><\\|(.*?)\\|><\\|(.*?)\\|>(.*)");
  private static final StreamCommand FINISH_COMMAND = new StreamCommand(null, true);
  private static final OkHttpClient okHttpClient = new OkHttpClient.Builder()
      .connectTimeout(30, TimeUnit.SECONDS)
      .readTimeout(30, TimeUnit.SECONDS)
      .build();

  @Value("${ai.funasr.host:localhost}")
  private String host;

  @Value("${ai.funasr.port:10095}")
  private int port;

  @Value("${ai.funasr.api-key:none}")
  private String apiKey;

  @Value("${ai.funasr.is-ssl:false}")
  private boolean useSsl;

  @Value("${ai.funasr.delete-audio:true}")
  private boolean deleteAudioFile;

  @Value("${ai.funasr.output-dir:./funasr_output/}")
  private String outputDir;

  @Value("${ai.funasr.itn:false}")
  private boolean itn;

  @Value("${ai.funasr.streaming-final-timeout-ms:10000}")
  private long streamingFinalTimeoutMs;

  private final Set<FunAsrStreamingSession> activeStreamingSessions =
      java.util.concurrent.ConcurrentHashMap.newKeySet();
  private String wsUri;

  @PostConstruct
  public void init() {
    this.wsUri = String.format("%s://%s:%d", useSsl ? "wss" : "ws", host, port);
    log.debug("FunASR客户端初始化完成: uri={}, ssl={}", wsUri, useSsl);
  }

  @Override
  public StreamingAsrSession startStreaming(String sessionId) {
    FunAsrStreamingSession streamingSession =
        new FunAsrStreamingSession(sessionId, Math.max(1L, streamingFinalTimeoutMs));
    activeStreamingSessions.add(streamingSession);
    streamingSession.result.whenComplete(
        (text, error) -> activeStreamingSessions.remove(streamingSession));
    streamingSession.start();
    return streamingSession;
  }

  /** 主入口：语音转文字（线程安全）。 */
  public CompletableFuture<AsrResult> speechToText(byte[] pcmData, String sessionId) {
    return CompletableFuture.supplyAsync(() -> {
      CompletableFuture<String> receiveFuture = new CompletableFuture<>();
      Session session = null;
      try {
        log.debug("开始语音识别, sessionId={}, dataSize={}", sessionId, pcmData.length);
        session = connectWebSocket(createOfflineEndpoint(receiveFuture));
        sendConfiguration(session, sessionId, "offline");
        sendAudioData(session, pcmData);
        sendEndMarker(session);
        String rawText = receiveFuture.get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        String cleanedText = cleanText(rawText);
        log.debug("语音识别完成, sessionId={}, text={}", sessionId, cleanedText);
        return new AsrResult(cleanedText, "not support");
      } catch (Exception e) {
        log.error("语音识别失败 sessionId={}", sessionId, e);
        return new AsrResult("", null);
      } finally {
        closeSession(session);
      }
    });
  }

  private Session connectWebSocket(Endpoint endpoint) throws Exception {
    WebSocketContainer container = ContainerProvider.getWebSocketContainer();
    ClientEndpointConfig config = ClientEndpointConfig.Builder.create()
        .configurator(new ClientEndpointConfig.Configurator() {
          @Override
          public void beforeRequest(Map<String, List<String>> headers) {
            headers.put("Authorization", List.of("Bearer " + apiKey));
          }
        })
        .build();

    if (useSsl) {
      configureSslContext();
    }
    Session session = container.connectToServer(endpoint, config, URI.create(wsUri));
    log.debug("WebSocket连接已建立");
    return session;
  }

  private Endpoint createOfflineEndpoint(CompletableFuture<String> receiveFuture) {
    return new Endpoint() {
      @Override
      public void onOpen(Session session, EndpointConfig config) {
        session.addMessageHandler(createOfflineMessageHandler(receiveFuture));
      }

      @Override
      public void onError(Session session, Throwable error) {
        receiveFuture.completeExceptionally(error);
      }

      @Override
      public void onClose(Session session, CloseReason closeReason) {
        if (!receiveFuture.isDone()) {
          receiveFuture.complete("");
        }
      }
    };
  }

  private Endpoint createStreamingEndpoint(CompletableFuture<String> result) {
    return new Endpoint() {
      @Override
      public void onOpen(Session session, EndpointConfig config) {
        session.addMessageHandler(createStreamingMessageHandler(result));
      }

      @Override
      public void onError(Session session, Throwable error) {
        result.completeExceptionally(error);
      }

      @Override
      public void onClose(Session session, CloseReason closeReason) {
        if (!result.isDone()) {
          result.completeExceptionally(
              new IOException("FunASR流式连接提前关闭: " + closeReason));
        }
      }
    };
  }

  MessageHandler.Whole<String> createOfflineMessageHandler(
      CompletableFuture<String> receiveFuture) {
    return new MessageHandler.Whole<String>() {
      @Override
      public void onMessage(String message) {
        try {
          JSONObject json = JSON.parseObject(message);
          boolean isFinal =
              !json.containsKey("is_final") || json.getBooleanValue("is_final");
          String text = json.getString("text");
          log.debug("收到消息: isFinal={}, text={}", isFinal, text);
          if (isFinal) {
            receiveFuture.complete(text);
          }
        } catch (Exception e) {
          receiveFuture.completeExceptionally(e);
        }
      }
    };
  }

  MessageHandler.Whole<String> createStreamingMessageHandler(CompletableFuture<String> result) {
    return new MessageHandler.Whole<String>() {
      @Override
      public void onMessage(String message) {
        try {
          JSONObject json = JSON.parseObject(message);
          String mode = json.getString("mode");
          String text = json.getString("text");
          if (isFinalStreamingMessage(json)) {
            log.debug("收到FunASR最终结果: mode={}, text={}", mode, text);
            result.complete(cleanText(text));
          } else {
            log.trace("收到FunASR在线结果: mode={}, text={}", mode, text);
          }
        } catch (Exception e) {
          result.completeExceptionally(e);
        }
      }
    };
  }

  boolean isFinalStreamingMessage(JSONObject json) {
    if (json == null) {
      return false;
    }
    String mode = json.getString("mode");
    if ("2pass-online".equalsIgnoreCase(mode)) {
      return false;
    }
    return "2pass-offline".equalsIgnoreCase(mode)
        || json.containsKey("is_final") && json.getBooleanValue("is_final");
  }

  private void configureSslContext() throws Exception {
    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, new javax.net.ssl.TrustManager[] {
        new javax.net.ssl.X509TrustManager() {
          @Override
          public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[0];
          }

          @Override
          public void checkClientTrusted(java.security.cert.X509Certificate[] certs,
              String authType) {}

          @Override
          public void checkServerTrusted(java.security.cert.X509Certificate[] certs,
              String authType) {}
        }
    }, new SecureRandom());
  }

  private void sendConfiguration(Session session, String sessionId, String mode)
      throws IOException {
    session.getBasicRemote().sendText(JSON.toJSONString(buildConfiguration(sessionId, mode)));
  }

  Map<String, Object> buildStreamingConfiguration(String sessionId) {
    return buildConfiguration(sessionId, "2pass");
  }

  private Map<String, Object> buildConfiguration(String sessionId, String mode) {
    Map<String, Object> configuration = new HashMap<>();
    configuration.put("mode", mode);
    configuration.put("chunk_size", List.of(5, 10, 5));
    configuration.put("chunk_interval", 10);
    configuration.put("wav_name", sessionId);
    configuration.put("wav_format", "pcm");
    configuration.put("is_speaking", true);
    configuration.put("itn", itn);
    return configuration;
  }

  private void sendAudioData(Session session, byte[] pcmData) throws IOException {
    session.getBasicRemote().sendBinary(ByteBuffer.wrap(pcmData));
  }

  private void sendEndMarker(Session session) throws IOException {
    session.getBasicRemote().sendText(JSON.toJSONString(Map.of("is_speaking", false)));
  }

  private String cleanText(String raw) {
    if (raw == null) {
      return "";
    }
    Matcher matcher = TEXT_CLEANUP_PATTERN.matcher(raw);
    if (matcher.find()) {
      return matcher.group(4).trim();
    }
    return raw.trim();
  }

  private void closeSession(Session session) {
    if (session != null && session.isOpen()) {
      try {
        session.close();
      } catch (Exception ignored) {
        // The ASR result or original transport error is more useful than a close failure.
      }
    }
  }

  @Override
  public String getText(String url) {
    try {
      log.debug("开始从URL获取音频并识别: {}", url);
      byte[] audioData = downloadAudioFromUrl(url);
      String sessionId = "url_" + System.currentTimeMillis();
      AsrResult result = speechToText(audioData, sessionId)
          .get(DEFAULT_TIMEOUT_SECONDS + 5, TimeUnit.SECONDS);
      log.debug("URL音频识别完成: {}", result.getText());
      return result.getText();
    } catch (Exception e) {
      log.error("从URL识别音频失败: {}", url, e);
      return "";
    }
  }

  @Override
  public String getTextRealtime(File file, int sampleRate, String format) {
    try {
      if (!file.exists()) {
        return "";
      }
      byte[] audio = Files.readAllBytes(file.toPath());
      if ("wav".equalsIgnoreCase(format)) {
        audio = AudioUtils.wavBytesToPcm(audio);
      }
      return speechToText(audio, "file_" + file.getName())
          .get(DEFAULT_TIMEOUT_SECONDS + 5, TimeUnit.SECONDS)
          .getText();
    } catch (Exception e) {
      log.error("实时识别失败", e);
      return "";
    }
  }

  private byte[] downloadAudioFromUrl(String url) throws IOException {
    Request request = new Request.Builder().url(url).get().build();
    try (Response response = okHttpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new IOException("下载音频失败: " + response.code() + " " + response.message());
      }
      ResponseBody body = response.body();
      if (body == null) {
        throw new IOException("响应体为空");
      }
      byte[] data = body.bytes();
      if (data.length > 44 && isWavFormat(data)) {
        return AudioUtils.wavBytesToPcm(data);
      }
      return data;
    }
  }

  private boolean isWavFormat(byte[] data) {
    return data.length >= 4
        && data[0] == 'R'
        && data[1] == 'I'
        && data[2] == 'F'
        && data[3] == 'F';
  }

  @PreDestroy
  public void shutdownStreamingSessions() {
    new ArrayList<>(activeStreamingSessions).forEach(FunAsrStreamingSession::cancel);
  }

  private record StreamCommand(byte[] pcmData, boolean finish) {}

  private final class FunAsrStreamingSession implements StreamingAsrSession {
    private final String sessionId;
    private final long finalTimeoutMs;
    private final ArrayBlockingQueue<StreamCommand> commands =
        new ArrayBlockingQueue<>(STREAMING_QUEUE_CAPACITY);
    private final CompletableFuture<String> result = new CompletableFuture<>();
    private final AtomicBoolean finishRequested = new AtomicBoolean(false);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private volatile Session webSocketSession;
    private volatile Thread senderThread;

    private FunAsrStreamingSession(String sessionId, long finalTimeoutMs) {
      this.sessionId = sessionId;
      this.finalTimeoutMs = finalTimeoutMs;
    }

    private void start() {
      senderThread = Thread.ofVirtual()
          .name("funasr-stream-" + sessionId)
          .start(this::runSender);
      result.whenComplete((text, error) -> {
        cancelled.set(true);
        closeSession(webSocketSession);
        Thread currentSender = senderThread;
        if (currentSender != null && currentSender != Thread.currentThread()) {
          currentSender.interrupt();
        }
      });
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
        fail(new IllegalStateException("FunASR流式音频队列已满"));
      }
      return accepted;
    }

    @Override
    public CompletableFuture<String> finish() {
      if (finishRequested.compareAndSet(false, true) && !cancelled.get()) {
        if (!commands.offer(FINISH_COMMAND)) {
          fail(new IllegalStateException("FunASR流式音频队列无法写入结束标记"));
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
      result.completeExceptionally(new CancellationException("FunASR流式会话已取消"));
      closeSession(webSocketSession);
      Thread currentSender = senderThread;
      if (currentSender != null) {
        currentSender.interrupt();
      }
    }

    private void runSender() {
      try {
        webSocketSession = connectWebSocket(createStreamingEndpoint(result));
        sendConfiguration(webSocketSession, sessionId, "2pass");
        while (!cancelled.get()) {
          StreamCommand command = commands.take();
          if (command.finish()) {
            sendEndMarker(webSocketSession);
            return;
          }
          sendAudioData(webSocketSession, command.pcmData());
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        if (!cancelled.get()) {
          fail(e);
        }
      } catch (Exception e) {
        fail(e);
      }
    }

    private void fail(Throwable error) {
      commands.clear();
      cancelled.set(true);
      result.completeExceptionally(error);
      closeSession(webSocketSession);
    }
  }

  public static class AsrResult {
    private final String text;
    private final String filePath;

    public AsrResult(String text, String filePath) {
      this.text = text;
      this.filePath = filePath;
    }

    public String getText() {
      return text;
    }

    public String getFilePath() {
      return filePath;
    }
  }
}
