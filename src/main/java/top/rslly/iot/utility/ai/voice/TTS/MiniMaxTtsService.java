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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.rslly.iot.utility.ai.voice.AudioUtils;
import top.rslly.iot.utility.ai.voice.OpusEncoderUtils;

import jakarta.websocket.Session;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * MiniMax TTS 服务实现 支持语音合成并通过 WebSocket 发送音频流
 */
@Slf4j
@Component
public class MiniMaxTtsService implements TtsService {

  private static final String API_URL = "https://api.minimaxi.com/v1/t2a_v2";
  private static final String DEFAULT_MODEL = "speech-02-hd";
  private static final String DEFAULT_VOICE = "Chinese (Mandarin)_Warm_Bestie";

  @Value("${ai.minimax.api-key:}")
  private String apiKey;

  @Value("${ai.minimax.group-id:}")
  private String groupId;

  @Value("${ai.minimax.tts.enabled:false}")
  private boolean ttsEnabled;

  @Value("${ai.minimax.tts.model:speech-02-hd}")
  private String model;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void websocketAudioSync(String text, Float pitch, Float speed, Session session,
      String chatId, String voice) {
    List<byte[]> audioList = getTextAudio(chatId, text, pitch, speed, voice);
    // Only used for WebSocket audio sending.
    final BlockingQueue<byte[]> audioQueue = new LinkedBlockingQueue<>();
    for(byte[] b : audioList){
      audioQueue.offer(b);
    }
    try{
      // 异步发送音频队列
      AudioUtils.asyncSendAudioQueue(chatId, session, audioQueue);
    } catch (Exception e) {
      log.error("MiniMax TTS error for chatId: {}", chatId, e);
    }
  }

  /**
   * 获取文本的Opus音频字节流
   * @param chatId  对话ID
   * @param text  文本
   * @param pitch 语调
   * @param speed 语速
   * @param voice 声音类型
   * @return  Null或一个转载有Opus字节流的BlockingQueue
   */
  @Override
  public List<byte[]> getTextAudio(String chatId, String text, Float pitch, Float speed, String voice) {
    // Only used for WebSocket audio sending.
    List<byte[]> audioList = new ArrayList<>();
    // End-of-stream marker: an empty byte array.
    final byte[] EOS = new byte[0];
    final OpusEncoderUtils encoder = new OpusEncoderUtils(16000, 1, 60);
    String tempFilePath = null;

    try {
      // 构建 API URL
      String url = API_URL + "?GroupId=" + URLEncoder.encode(groupId, StandardCharsets.UTF_8);

      // 构建 JSON 请求体
      String requestBody = buildRequestBody(text, pitch, speed, voice, true);

      // 发送请求
      URL apiUrl = new URL(url);
      HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Authorization", "Bearer " + apiKey);
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setDoOutput(true);
      connection.getOutputStream().write(requestBody.getBytes(StandardCharsets.UTF_8));

      int responseCode = connection.getResponseCode();
      if (responseCode != 200) {
        BufferedReader errorReader =
                new BufferedReader(
                        new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8));
        StringBuilder errorResponse = new StringBuilder();
        String line;
        while ((line = errorReader.readLine()) != null) {
          errorResponse.append(line);
        }
        errorReader.close();
        log.error("MiniMax TTS API error for voice '{}': {} - {}", voice, responseCode,
                errorResponse);
        return null;
      }

      log.debug("MiniMax TTS API request successful for voice: {}, text length: {}", voice,
          text.length());

      // 读取流式响应
      ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();
      try (BufferedReader reader = new BufferedReader(
              new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          if (line.startsWith("data: ")) {
            String jsonData = line.substring(6);
            if (jsonData.isEmpty() || jsonData.equals("[DONE]")) {
              continue;
            }

            try {
              JsonNode root = objectMapper.readTree(jsonData);

              // 检查错误状态
              JsonNode baseResp = root.path("base_resp");
              if (baseResp.has("status_code")
                      && baseResp.get("status_code").asInt() != 0) {
                log.error("MiniMax TTS API returned error: {}",
                        baseResp.path("status_msg").asText());
                continue;
              }

              // 获取音频数据
              JsonNode data = root.path("data");
              if (data.has("audio")) {
                String audioHex = data.get("audio").asText();
                if (audioHex != null && !audioHex.isEmpty()) {
                  byte[] audioBytes = hexStringToByteArray(audioHex);
                  audioBuffer.write(audioBytes);
                }
              }

              // 检查是否完成 (status=2 表示最后一帧)
              if (data.has("status") && data.get("status").asInt() == 2) {
                break;
              }
            } catch (Exception e) {
              log.debug("Failed to parse SSE data: {}", e.getMessage());
            }
          }
        }
      }

      // 将音频数据保存为临时 MP3 文件
      byte[] mp3Data = audioBuffer.toByteArray();
      if (mp3Data.length == 0) {
        log.warn(
                "MiniMax TTS returned empty audio data for voice: '{}', model: '{}', text length: {}",
                voice, model != null && !model.isBlank() ? model : DEFAULT_MODEL, text.length());
        return null;
      }
      log.debug("MiniMax TTS generated {} bytes of audio data for voice: '{}'", mp3Data.length,
          voice);

      String outputPath = System.getProperty("java.io.tmpdir");
      // 将 chatId 中的冒号替换为下划线，避免 Windows 路径非法字符问题
      String safeChatId = chatId.replace(":", "_");
      tempFilePath = Paths.get(outputPath, "minimax_tts_" + safeChatId + ".mp3").toString();
      Files.write(Paths.get(tempFilePath), mp3Data);

      // 将 MP3 转换为 PCM (16kHz 单声道)
      byte[] pcmData = AudioUtils.convertMp3ToPcm(tempFilePath);

      // 编码为 Opus 并发送到队列
      List<byte[]> packets = encoder.encodePcmToOpus(pcmData, false);
      audioList.addAll(packets);

      // 刷新编码器
      packets = encoder.encodePcmToOpus(new byte[0], true);
      audioList.addAll(packets);

      // 信号结束
      audioList.add(EOS);

      return audioList;

    } catch (Exception e) {
      log.error("MiniMax TTS error for chatId: {}", chatId, e);
    } finally {
      // 清理临时文件
      if (tempFilePath != null) {
        try {
          Files.deleteIfExists(Paths.get(tempFilePath));
          log.debug("Successfully deleted temporary file: {}", tempFilePath);
        } catch (Exception e) {
          log.warn("Failed to delete temporary file: {}", tempFilePath, e);
        }
      }
    }
    return null;
  }

  @Override
  public void asyncSynthesizeAndSaveAudio(String text, String chatId) {
    throw new UnsupportedOperationException("MiniMax TTS does not support async synthesis yet");
  }

  /**
   * 构建 MiniMax TTS API 请求体
   */
  private String buildRequestBody(String text, Float pitch, Float speed, String voice,
      boolean stream) {
    try {
      StringBuilder json = new StringBuilder();
      json.append("{");
      json.append("\"text\":\"").append(escapeJson(text)).append("\",");
      // 使用配置的模型，如果未配置则使用默认模型
      String modelToUse = (model != null && !model.isBlank()) ? model : DEFAULT_MODEL;
      json.append("\"model\":\"").append(modelToUse).append("\",");
      json.append("\"stream\":").append(stream).append(",");

      // 音色设置
      String voiceId = (voice != null && !voice.isBlank()) ? voice : DEFAULT_VOICE;

      // language_boost 参数：增强对指定语种/方言的识别能力
      // 粤语音色使用 "Chinese,Yue"，普通话使用 "Chinese"
      String languageBoost = "Chinese";
      if (voiceId.startsWith("Cantonese_")) {
        languageBoost = "Chinese,Yue";
      }
      json.append("\"language_boost\":\"").append(languageBoost).append("\",");

      // 音频设置 - 指定 mp3 格式
      json.append("\"audio_setting\":{");
      json.append("\"format\":\"mp3\"");
      json.append("},");

      if (stream) {
        json.append("\"stream_options\":{\"exclude_aggregated_audio\":true},");
      }

      json.append("\"voice_setting\":{");
      json.append("\"voice_id\":\"").append(voiceId).append("\",");
      json.append("\"speed\":").append(speed != null ? speed : 1.0f).append(",");
      // MiniMax pitch 范围是 -12 到 12，需要转换
      int pitchValue = 0;
      if (pitch != null) {
        // 将 0.5-2.0 映射到 -12 到 12
        pitchValue = (int) ((pitch - 1.0f) * 24);
        pitchValue = Math.max(-12, Math.min(12, pitchValue));
      }
      json.append("\"pitch\":").append(pitchValue);
      json.append("}");

      json.append("}");
      log.debug("MiniMax TTS request body for voice '{}': {}", voiceId, json);
      return json.toString();
    } catch (Exception e) {
      log.error("Failed to build request body", e);
      return "{}";
    }
  }

  /**
   * 转义 JSON 字符串中的特殊字符
   */
  private String escapeJson(String text) {
    if (text == null) {
      return "";
    }
    return text.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }

  /**
   * 将十六进制字符串转换为字节数组
   */
  private byte[] hexStringToByteArray(String hex) {
    int len = hex.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
          + Character.digit(hex.charAt(i + 1), 16));
    }
    return data;
  }
}
