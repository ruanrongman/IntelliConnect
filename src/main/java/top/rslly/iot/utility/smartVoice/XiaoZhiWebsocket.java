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
package top.rslly.iot.utility.smartVoice;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.rslly.iot.config.WebSocketConfig;
import top.rslly.iot.services.SafetyServiceImpl;
import top.rslly.iot.services.thingsModel.ProductServiceImpl;
import top.rslly.iot.utility.ai.voice.SlieroVadListener;
import top.rslly.iot.utility.ai.voice.Text2audio;
import top.rslly.iot.utility.ai.voice.concentus.OpusDecoder;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@ServerEndpoint(value = "/xiaozhi/v1/{chatId}", configurator = WebSocketConfig.class)
@Component
@Slf4j
public class XiaoZhiWebsocket {
  public static final Map<String, Session> clients = new ConcurrentHashMap<>();
  public static final Map<String, String> voiceContent = new ConcurrentHashMap<>();
  private static final Map<String, ByteArrayOutputStream> pcmVadBuffer = new ConcurrentHashMap<>();
  public static final Map<String, Boolean> isAbort = new ConcurrentHashMap<>();
  public static final Map<String, Boolean> haveVoice = new ConcurrentHashMap<>();
  private static volatile SafetyServiceImpl safetyService;
  private static volatile Text2audio text2audio;
  private static volatile ProductServiceImpl productService;
  private static volatile XiaoZhiUtil xiaoZhiUtil;
  private final SlieroVadListener slieroVadListener = new SlieroVadListener();
  List<byte[]> audioList = new CopyOnWriteArrayList<>();
  private String chatId;
  private int productId;
  private boolean isManual = true;
  private boolean isRealTime = false;

  @Autowired
  public void setText2audio(Text2audio text2audio) {
    if (XiaoZhiWebsocket.text2audio == null) {
      XiaoZhiWebsocket.text2audio = text2audio;
    }
  }

  @Autowired
  public void setXiaoZhiUtil(XiaoZhiUtil xiaoZhiUtil) {
    if (XiaoZhiWebsocket.xiaoZhiUtil == null) {
      XiaoZhiWebsocket.xiaoZhiUtil = xiaoZhiUtil;
    }
  }

  @Autowired
  public void setSafetyService(SafetyServiceImpl safetyService) {
    if (XiaoZhiWebsocket.safetyService == null) {
      XiaoZhiWebsocket.safetyService = safetyService;
    }
  }

  @Autowired
  public void setProductService(ProductServiceImpl productService) {
    if (XiaoZhiWebsocket.productService == null) {
      XiaoZhiWebsocket.productService = productService;
    }
  }


  /**
   * socket start
   */
  @OnOpen
  public void onOpen(@PathParam("chatId") String chatId, Session session) {
    if (chatId == null) {
      try {
        session.getBasicRemote().sendText("chatId为null");
        session.close();
        return;
      } catch (IOException e) {
        log.info("发送失败");
        return;
      }
    }
    if (chatId.equals("register")) {
      this.chatId = "register" + UUID.randomUUID();
      log.info("注册成功{}", this.chatId);
      try {
        clients.put(this.chatId, session);
        isAbort.put(this.chatId, false);
        haveVoice.put(this.chatId, false);
        slieroVadListener.init();
        return;
      } catch (Exception e) {
        log.info("发送失败");
        return;
      }
    }
    String deviceId = getDeviceId(session);
    String token = getHeader(session);
    if (clients.get("chatProduct" + chatId + deviceId) == null) {
      this.chatId = "chatProduct" + chatId + deviceId;
      this.productId = Integer.parseInt(chatId);
      if (!safetyService.controlAuthorizeProduct(token, productId)) {
        try {
          session.getBasicRemote().sendText("没有权限");
          session.close();
          return;
        } catch (IOException e) {
          log.info("发送失败");
        }
      }
      if (productService.findAllById(productId).isEmpty()) {
        try {
          session.getBasicRemote().sendText("产品不存在");
          session.close();
          return;
        } catch (IOException e) {
          log.info("发送失败");
        }
      }
      clients.put(this.chatId, session);
      isAbort.put(this.chatId, false);
      haveVoice.put(this.chatId, false);
      slieroVadListener.init();
      log.info("header{}", token);
    } else {
      log.info("冲突，无法连接");
      try {
        session.getBasicRemote().sendText("冲突，无法连接");
        session.close();
      } catch (IOException e) {
        log.info("发送失败");
      }
      try {
        session.close();
      } catch (IOException e) {
        log.error("关闭失败{}", e.getMessage());
      }
    }
  }

  /**
   * socket stop
   */
  @OnClose
  public void onClose() {
    log.info("close");
    slieroVadListener.destroy();
    if (chatId != null) {
      xiaoZhiUtil.destroyMcp(chatId);
      clients.remove(chatId);
      isAbort.remove(chatId);
      haveVoice.remove(chatId);
      pcmVadBuffer.remove(chatId); // 清理缓冲区
      voiceContent.remove(chatId); // 清理语音内容
    }
  }

  /**
   * socket message
   */
  @OnMessage
  public void onMessage(String message) {
    log.info("message{}", message);
    try {
      // OpusDecoder decoder = new OpusDecoder(16000, 1);
      var json = JSON.parseObject(message);
      String type = json.getString("type");
      if (type.equals("hello")) {
        String token = getHeader(clients.get(chatId));
        xiaoZhiUtil.dealHello(chatId, json, token);
      } else if (type.equals("mcp")) {
        xiaoZhiUtil.dealMcp(chatId, json);
      } else if (type.equals("abort")) {
        isAbort.put(chatId, true);
      } else if (type.equals("listen")) {
        String state = json.getString("state");
        switch (state) {
          case "detect" -> {
            xiaoZhiUtil.dealDetect(chatId, productId);
            isAbort.put(chatId, false);
          }
          case "start" -> {
            if (this.chatId.startsWith("register")) {
              xiaoZhiUtil.dealRegister(chatId, productId);
              isAbort.put(chatId, false);
              clients.get(chatId).close();
            }
            String mode = json.getString("mode");
            if (mode.equals("auto")) {
              isManual = false;
              isRealTime = false;
            } else if (mode.equals("manual")) {
              isManual = true;
              isRealTime = false;
            } else if (mode.equals("realtime")) {
              isManual = false;
              isRealTime = true;
            }
            log.info("listen start");
            voiceContent.clear();
          }
          case "stop" -> {
            xiaoZhiUtil.dealWithAudio(audioList, chatId, productId, isManual);
          }
        }
      }
    } catch (Exception e) {
      log.error("json error{}", e.getMessage());
    }

  }

  @OnMessage
  public void onBinaryMessage(byte[] message) {
    if (!isManual) {
      try {
        int timeOutSize = 420;
        if (isRealTime) {
          timeOutSize = 420 * 7;
        }
        if (audioList.size() > timeOutSize && !haveVoice.get(chatId)) {
          clients.get(chatId).getBasicRemote().sendText("""
              {
                "type": "tts",
                "state": "start"
              }""");
          audioList.clear();
          text2audio.websocketAudioSync("时间过得真快，没什么事我先退下了", clients.get(chatId), chatId, productId);
          clients.get(chatId).getBasicRemote().sendText("{\"type\":\"tts\",\"state\":\"stop\"}");
          isAbort.put(chatId, false);
          clients.get(chatId).close();
          return;
          // 存在问题，导致无法播放
          // onClose();
        }
        byte[] bytes = message.clone();
        OpusDecoder decoder = new OpusDecoder(16000, 1);
        byte[] data_packet = new byte[16000];

        // 解码音频数据
        int pcm_frame = decoder.decode(bytes, 0, bytes.length,
            data_packet, 0, 960, false);
        int decodedBytes = pcm_frame * 2;

        // 初始化缓冲区
        if (!pcmVadBuffer.containsKey(chatId)) {
          pcmVadBuffer.put(chatId, new ByteArrayOutputStream());
        }
        ByteArrayOutputStream buffer = pcmVadBuffer.get(chatId);

        // 将解码数据写入缓冲区
        buffer.write(data_packet, 0, decodedBytes);
        byte[] bufferArray = buffer.toByteArray();
        int bufferLength = bufferArray.length;
        int processed = 0;

        // 循环处理512*2字节的数据块
        while (processed + 512 * 2 <= bufferLength) {
          byte[] chunk = Arrays.copyOfRange(
              bufferArray,
              processed,
              processed + 512 * 2);
          // 处理音频片段
          var map = slieroVadListener.listen(chunk);
          if (map != null && map.containsKey("start")) {
            log.info("map{}", map);
            haveVoice.put(chatId, true);
            if (isRealTime) {
              isAbort.put(chatId, true);
            }
            // 保留音频数据最后10帧（直接修改原始列表）
            int keepFrames = Math.min(10, audioList.size()); // 安全处理边界
            if (audioList.size() > keepFrames) {
              audioList.subList(0, audioList.size() - keepFrames).clear();
            }
          }
          if (map != null && map.containsKey("end")) {
            log.info("map{}", map);
            if (isRealTime) {
              isAbort.put(chatId, false);
            }
            xiaoZhiUtil.dealWithAudio(audioList, chatId, productId, isManual);
            pcmVadBuffer.remove(chatId);
            return;
          }
          processed += 512 * 2;
        }

        // 保留未处理数据
        buffer.reset();
        if (processed < bufferLength) {
          buffer.write(bufferArray, processed, bufferLength - processed);
        }

      } catch (Exception e) {
        log.error("音频转换失败{}", e.getMessage());
      }
    }
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

  public static String getDeviceId(Session session) {
    final String deviceId = (String) session.getUserProperties().get("Device-Id");
    if (StrUtil.isBlank(deviceId)) {
      log.error("获取deviceId失败，不安全的链接，即将关闭");
      try {
        session.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return deviceId;
  }


}
