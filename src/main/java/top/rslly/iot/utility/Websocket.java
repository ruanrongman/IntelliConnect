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
package top.rslly.iot.utility;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.rslly.iot.config.WebSocketConfig;
import top.rslly.iot.services.SafetyServiceImpl;
import top.rslly.iot.services.thingsModel.ProductServiceImpl;
import top.rslly.iot.utility.ai.chain.Router;
import top.rslly.iot.utility.ai.tools.EmotionToolAsync;
import top.rslly.iot.utility.ai.voice.Audio2Text;
import top.rslly.iot.utility.ai.voice.Text2audio;
import top.rslly.iot.utility.ai.voice.concentus.OpusDecoder;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@ServerEndpoint(value = "/websocket/{chatId}", configurator = WebSocketConfig.class)
@Component
@Slf4j
public class Websocket {
  private static Audio2Text audio2Text;
  public static final Map<String, Session> clients = new ConcurrentHashMap<>();
  private static final Map<String, String> voiceContent = new ConcurrentHashMap<>();
  public static volatile boolean isAbort = false;
  private static SafetyServiceImpl safetyService;
  private static Text2audio text2audio;
  private static ProductServiceImpl productService;
  private static EmotionToolAsync emotionToolAsync;
  private static Router router;
  List<byte[]> audioList = new CopyOnWriteArrayList<>();
  private String chatId;

  @Autowired
  public void setAudio2Text(Audio2Text audio2Text) {
    Websocket.audio2Text = audio2Text;
  }

  @Autowired
  public void setText2audio(Text2audio text2audio) {
    Websocket.text2audio = text2audio;
  }

  @Autowired
  public void setRouter(Router router) {
    Websocket.router = router;
  }

  @Autowired
  public void setSafetyService(SafetyServiceImpl safetyService) {
    Websocket.safetyService = safetyService;
  }

  @Autowired
  public void setProductService(ProductServiceImpl productService) {
    Websocket.productService = productService;
  }

  @Autowired
  public void setEmotionToolAsync(EmotionToolAsync emotionToolAsync) {
    Websocket.emotionToolAsync = emotionToolAsync;
  }

  /**
   * socket start
   */
  @OnOpen
  public void onOpen(@PathParam("chatId") String chatId, Session session) {
    if (clients.get(chatId) == null) {
      this.chatId = chatId;
      clients.put(chatId, session);
      isAbort = false;
      String token = getHeader(session);
      if (!safetyService.controlAuthorizeProduct(token, Integer.parseInt(chatId))) {
        try {
          session.getBasicRemote().sendText("没有权限");
          onClose();
          return;
        } catch (IOException e) {
          log.info("发送失败");
        }
      }
      if (productService.findAllById(Integer.parseInt(chatId)).isEmpty()) {
        try {
          session.getBasicRemote().sendText("产品不存在");
          onClose();
          return;
        } catch (IOException e) {
          log.info("发送失败");
        }
      }
      log.info("header{}", token);
    } else {
      log.info("冲突，无法连接");
      try {
        session.getBasicRemote().sendText("冲突，无法连接");
      } catch (IOException e) {
        log.info("发送失败");
      }
      onClose();
    }
  }

  /**
   * socket stop
   */
  @OnClose
  public void onClose() {
    log.info("close");
    if (chatId != null)
      clients.remove(chatId);
  }

  /**
   * socket message
   */
  @OnMessage
  public void onMessage(String message) {
    log.info("message{}", message);
    try {
      OpusDecoder decoder = new OpusDecoder(16000, 1);
      var json = JSON.parseObject(message);
      String type = json.getString("type");
      if (type.equals("hello")) {
        clients.get(chatId).getBasicRemote().sendText(
            "{\"type\":\"hello\",\"transport\":\"websocket\",\"audio_params\":{\"sample_rate\":16000}}");
      } else if (type.equals("listen")) {
        String state = json.getString("state");
        if (state.equals("start")) {
          String mode = json.getString("mode");
          if (!mode.equals("manual")) {
            clients.get(chatId).getBasicRemote()
                .sendText("{\"type\":\"stt\",\"text\":\"" + "不支持非手动模式" + "\"}");
            onClose();
            return;
          }
          log.info("listen start");
          voiceContent.clear();
        } else if (state.equals("stop")) {
          if (audioList.size() > 20) {
            // 安全读取字节数据
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            for (byte[] bytes : audioList) {
              try {
                // log.info("len{}",bytes.length);
                byte[] data_packet = new byte[16000];
                int pcm_frame = decoder.decode(bytes, 0, bytes.length,
                    data_packet, 0, 960, false);
                // log.info("data_packet{}",data_packet);
                bos.write(data_packet, 0, pcm_frame * 2);
              } catch (Exception e) {
                log.error("音频转换失败{}", e.getMessage());
              }
            }
            log.info("data_size{}", bos.size());
            Path tempFile = Files.createTempFile("audio_", ".wav");
            Files.write(tempFile, bos.toByteArray());
            bos.close();
            String text = audio2Text.getTextRealtime(tempFile.toFile(), 16000, "pcm");
            log.info("text{}", text);
            var jsonObject = JSON.parseObject(text);
            var sentencesArray = jsonObject.getJSONArray("sentences");
            StringBuilder sentences = new StringBuilder("");
            if (sentencesArray.size() > 0) {
              for (int i = 0; i < sentencesArray.size(); i++) {
                sentences.append(sentencesArray.getJSONObject(i).getString("text"));
              }
            }
            if (sentences.length() > 0) {
              if (voiceContent.containsKey(chatId) && voiceContent.get(chatId).length() > 0) {
                voiceContent.put(chatId, voiceContent.get(chatId) + sentences);
              } else {
                voiceContent.put(chatId, sentences.toString());
              }
              clients.get(chatId).getBasicRemote()
                  .sendText("{\"type\":\"stt\",\"text\":\"" + sentences + "\"}");
              audioList.clear();
            } else {
              // 保留音频数据最后10帧
              audioList = audioList.subList(audioList.size() - 10, audioList.size());
              clients.get(chatId).getBasicRemote()
                  .sendText("{\"type\":\"stt\",\"text\":\"" + "没听清楚，说太快了" + "\"}");
              clients.get(chatId).getBasicRemote()
                  .sendText("{\"type\":\"tts\",\"state\":\"stop\"}");
            }
          } else {
            clients.get(chatId).getBasicRemote()
                .sendText("{\"type\":\"stt\",\"text\":\"" + "没听清楚，说太快了" + "\"}");
          }
          if (voiceContent.containsKey(chatId) && voiceContent.get(chatId).length() > 0) {
            clients.get(chatId).getBasicRemote()
                .sendText("{\"type\": \"tts\", \"state\": \"sentence_start\", \"text\": \""
                    + "智能助手思考中" + "\"}");
            JSONObject emotionObject = new JSONObject();
            emotionObject.put("type", "llm");
            emotionObject.put("text", "🤔");
            emotionObject.put("emotion", "thinking");
            clients.get(chatId).getBasicRemote()
                .sendText(emotionObject.toJSONString());
            log.info("listen stop,message{}", voiceContent.get(chatId));
            Map<String, Object> emotionMessage = new HashMap<>();
            emotionMessage.put("chatId", chatId);
            var emotionRes = emotionToolAsync.run(voiceContent.get(chatId), emotionMessage);
            String answer = router.response(voiceContent.get(chatId), "chatProduct" + chatId,
                Integer.parseInt(chatId));
            if (answer.length() > 200)
              answer = answer.substring(0, 200);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", "tts");
            jsonObject.put("state", "sentence_start");
            jsonObject.put("text", answer);
            if (emotionRes.isDone()) {
              emotionObject.put("text", emotionRes.get().get("emoji"));
              emotionObject.put("emotion", emotionRes.get().get("text"));
              log.info("emotionObject{}", emotionObject);
            } else {
              emotionObject.put("text", "😶");
              emotionObject.put("emotion", "neutral");
            }
            clients.get(chatId).getBasicRemote()
                .sendText(emotionObject.toJSONString());
            clients.get(chatId).getBasicRemote()
                .sendText(jsonObject.toJSONString());
            clients.get(chatId).getBasicRemote().sendText("""
                {
                  "type": "tts",
                  "state": "start"
                }""");
            text2audio.websocketAudio(answer, clients.get(chatId));
          }
        } else if (state.equals("abort")) {
          isAbort = true;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      log.error("json error{}", e.getMessage());
    }

  }

  @OnMessage
  public void onBinaryMessage(byte[] message) {
    log.info("message{}", Arrays.toString(message));

    // String text = "你好,小智";
    audioList.add(message);
  }

  @OnError
  public void onError(Session session, Throwable error) {
    onClose();
    log.error("Error in onError: {}", error.getMessage());
  }

  private static String getHeader(Session session) {
    final String header = (String) session.getUserProperties().get("Authorization");
    if (StrUtil.isBlank(header)) {
      log.error("获取header失败，不安全的链接，即将关闭");
      try {
        session.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return header;
  }


}
