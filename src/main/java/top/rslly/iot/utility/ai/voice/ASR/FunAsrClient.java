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
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.rslly.iot.utility.ai.voice.AudioUtils;

import jakarta.annotation.PostConstruct;
import javax.net.ssl.SSLContext;
import jakarta.websocket.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class FunAsrClient implements AsrService {

  private static final int DEFAULT_TIMEOUT_SECONDS = 2000;
  private static final Pattern TEXT_CLEANUP_PATTERN =
      Pattern.compile("<\\|(.*?)\\|><\\|(.*?)\\|><\\|(.*?)\\|>(.*)");

  // 或者使用单例
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

  private String wsUri;

  @PostConstruct
  public void init() {
    this.wsUri = String.format("%s://%s:%d",
        useSsl ? "wss" : "ws", host, port);
    log.info("FunASR客户端初始化完成: uri={}, ssl={}", wsUri, useSsl);
  }

  /**
   * 主入口：语音转文字（线程安全）
   */
  public CompletableFuture<AsrResult> speechToText(byte[] pcmData, String sessionId) {

    return CompletableFuture.supplyAsync(() -> {
      CompletableFuture<String> receiveFuture = new CompletableFuture<>();
      Session session = null;

      try {
        log.info("开始语音识别, sessionId={}, dataSize={}", sessionId, pcmData.length);

        // 建立 WebSocket，新版返回独立 Session
        session = connectWebSocket(receiveFuture);

        // 发送配置
        sendConfiguration(session, sessionId);

        // 发送音频
        sendAudioData(session, pcmData);

        // 发送结束标记
        sendEndMarker(session);

        // 等待识别结果（每个请求独享 future）
        String rawText = receiveFuture.get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        String cleanedText = cleanText(rawText);

        log.info("语音识别完成, sessionId={}, text={}", sessionId, cleanedText);

        return new AsrResult(cleanedText, "not support");

      } catch (Exception e) {
        log.error("语音识别失败 sessionId={}", sessionId, e);
        return new AsrResult("", null);

      } finally {
        closeSession(session);
      }
    });
  }

  /**
   * 建立 WebSocket（返回独立 Session）
   */
  private Session connectWebSocket(CompletableFuture<String> receiveFuture) throws Exception {

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

    Endpoint endpoint = createEndpoint(receiveFuture);

    Session session = container.connectToServer(endpoint, config, URI.create(wsUri));

    log.debug("WebSocket连接已建立");
    return session;
  }

  /**
   * 创建 WebSocket Endpoint(绑定独立 future)
   */
  private Endpoint createEndpoint(CompletableFuture<String> receiveFuture) {
    return new Endpoint() {

      @Override
      public void onOpen(Session session, EndpointConfig config) {

        session.addMessageHandler(new MessageHandler.Whole<String>() {
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
              log.error("处理消息失败", e);
              receiveFuture.completeExceptionally(e);
            }
          }
        });
      }

      @Override
      public void onError(Session session, Throwable thr) {
        log.error("WebSocket错误", thr);
        receiveFuture.completeExceptionally(thr);
      }

      @Override
      public void onClose(Session session, CloseReason closeReason) {
        log.debug("连接关闭: {}", closeReason);
        if (!receiveFuture.isDone()) {
          receiveFuture.complete("");
        }
      }
    };
  }

  /**
   * SSL 支持（开发环境）
   */
  private void configureSslContext() throws Exception {
    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, new javax.net.ssl.TrustManager[] {
        new javax.net.ssl.X509TrustManager() {
          public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
          }

          public void checkClientTrusted(java.security.cert.X509Certificate[] certs,
              String authType) {}

          public void checkServerTrusted(java.security.cert.X509Certificate[] certs,
              String authType) {}
        }
    }, new SecureRandom());
  }

  /** 发送配置信息 */
  private void sendConfiguration(Session session, String sessionId) throws Exception {
    Map<String, Object> map = new HashMap<>();
    map.put("mode", "offline");
    map.put("chunk_size", List.of(5, 10, 5));
    map.put("chunk_interval", 10);
    map.put("wav_name", sessionId);
    map.put("is_speaking", true);
    map.put("itn", false);

    session.getBasicRemote().sendText(JSON.toJSONString(map));
  }

  /** 发送音频 */
  private void sendAudioData(Session session, byte[] pcmData) throws Exception {
    session.getBasicRemote().sendBinary(ByteBuffer.wrap(pcmData));
  }

  /** 结束标记 */
  private void sendEndMarker(Session session) throws Exception {
    Map<String, Object> endMap = new HashMap<>();
    endMap.put("is_speaking", false);
    session.getBasicRemote().sendText(JSON.toJSONString(endMap));
  }

  /** 文本清理 */
  private String cleanText(String raw) {
    if (raw == null)
      return "";
    Matcher m = TEXT_CLEANUP_PATTERN.matcher(raw);
    if (m.find())
      return m.group(4).trim();
    return raw.trim();
  }

  /** 安全关闭 session */
  private void closeSession(Session session) {
    if (session != null && session.isOpen()) {
      try {
        session.close();
      } catch (Exception ignored) {
      }
    }
  }

  @Override
  public String getText(String url) {
    try {
      log.info("开始从URL获取音频并识别: {}", url);

      // 从URL下载音频文件
      byte[] audioData = downloadAudioFromUrl(url);

      // 生成会话ID
      String sessionId = "url_" + System.currentTimeMillis();

      // 调用语音识别
      CompletableFuture<AsrResult> future = speechToText(audioData, sessionId);
      AsrResult result = future.get(DEFAULT_TIMEOUT_SECONDS + 5, TimeUnit.SECONDS);

      log.info("URL音频识别完成: {}", result.getText());
      return result.getText();

    } catch (Exception e) {
      log.error("从URL识别音频失败: {}", url, e);
      return "";
    }
  }

  @Override
  public String getTextRealtime(File file, int sampleRate, String format) {
    try {
      if (!file.exists())
        return "";

      byte[] audio = Files.readAllBytes(file.toPath());

      if ("wav".equalsIgnoreCase(format)) {
        audio = AudioUtils.wavBytesToPcm(audio);
      }

      CompletableFuture<AsrResult> future =
          speechToText(audio, "file_" + file.getName());
      return future.get(DEFAULT_TIMEOUT_SECONDS + 5, TimeUnit.SECONDS)
          .getText();

    } catch (Exception e) {
      log.error("实时识别失败", e);
      return "";
    }
  }

  /**
   * 从URL下载音频数据
   */
  private byte[] downloadAudioFromUrl(String url) throws IOException {
    log.debug("开始下载音频: {}", url);

    Request request = new Request.Builder()
        .url(url)
        .get()
        .build();

    try (Response response = okHttpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new IOException("下载音频失败: " + response.code() + " " + response.message());
      }

      ResponseBody body = response.body();
      if (body == null) {
        throw new IOException("响应体为空");
      }

      byte[] data = body.bytes();
      log.debug("音频下载完成: {} bytes", data.length);

      // 如果是WAV格式，移除头部
      if (data.length > 44 && isWavFormat(data)) {
        return AudioUtils.wavBytesToPcm(data);
      }

      return data;
    }
  }

  /**
   * 检查是否为WAV格式
   */
  private boolean isWavFormat(byte[] data) {
    if (data.length < 4) {
      return false;
    }
    // 检查 "RIFF" 标识
    return data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F';
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
