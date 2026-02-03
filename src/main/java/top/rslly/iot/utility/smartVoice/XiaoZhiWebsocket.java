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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.rslly.iot.config.WebSocketConfig;
import top.rslly.iot.services.SafetyServiceImpl;
import top.rslly.iot.services.agent.OtaXiaozhiServiceImpl;
import top.rslly.iot.services.thingsModel.ProductServiceImpl;
import top.rslly.iot.utility.ai.voice.SlieroVadListener;
import top.rslly.iot.utility.ai.voice.TTS.Text2audio;
import top.rslly.iot.utility.ai.voice.TTS.TtsServiceFactory;
import top.rslly.iot.utility.ai.voice.concentus.OpusDecoder;

import jakarta.annotation.PreDestroy;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

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
  private static volatile TtsServiceFactory ttsServiceFactory;
  private static volatile ProductServiceImpl productService;
  private static volatile XiaoZhiUtil xiaoZhiUtil;
  private static volatile OtaXiaozhiServiceImpl otaXiaozhiService;
  public static final AtomicBoolean isSystemShuttingDown = new AtomicBoolean(false);
  // 改为静态Map，为每个会话维护独立的VAD监听器实例
  private static final Map<String, SlieroVadListener> vadListenerMap = new ConcurrentHashMap<>();
  // 在类的静态字段中添加
  private static final Map<String, Long> sessionStartTimeMap = new ConcurrentHashMap<>();
  List<byte[]> audioList = new CopyOnWriteArrayList<>();
  private String chatId;
  private int productId;
  private boolean isManual = true;
  private boolean isRealTime = false;

  // 使用静态变量存储配置
  private static long TIMEOUT_NORMAL = 60; // 默认60秒
  private static long TIMEOUT_REALTIME = 120000; // 默认120秒

  // 通过静态setter注入配置
  @Value("${ai.vad.timeout:60}")
  public void setTimeoutNormal(long timeout) {
    XiaoZhiWebsocket.TIMEOUT_NORMAL = timeout;
  }

  @Value("${ai.vad.timeoutRealtime:120}")
  public void setTimeoutRealtime(long timeout) {
    XiaoZhiWebsocket.TIMEOUT_REALTIME = timeout;
  }

  @Autowired
  public void setTtsServiceFactory(TtsServiceFactory ttsServiceFactory) {
    if (XiaoZhiWebsocket.ttsServiceFactory == null) {
      XiaoZhiWebsocket.ttsServiceFactory = ttsServiceFactory;
    }
  }

  @Autowired
  public void setOtaXiaozhiService(OtaXiaozhiServiceImpl otaXiaozhiService) {
    if (XiaoZhiWebsocket.otaXiaozhiService == null) {
      XiaoZhiWebsocket.otaXiaozhiService = otaXiaozhiService;
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
    // 系统正在关闭时，拒绝新连接
    if (isSystemShuttingDown.get()) {
      try {
        session.getBasicRemote().sendText(
            "{\"type\":\"system\",\"state\":\"shutdown\",\"message\":\"系统正在维护，暂时无法建立连接。\"}");
        session.close();
      } catch (IOException ignored) {
      }
      return;
    }
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
        // 为该会话创建独立的VAD监听器实例
        SlieroVadListener listener = new SlieroVadListener();
        listener.init();
        vadListenerMap.put(this.chatId, listener);
        return;
      } catch (Exception e) {
        log.info("发送失败");
        return;
      }
    }
    String deviceId = getDeviceId(session);
    String token = getHeader(session);
    if (deviceId == null) {
      try {
        session.getBasicRemote().sendText("deviceId为null");
        session.close();
        return;
      } catch (IOException e) {
        log.info("发送失败");
        return;
      }
    }
    if (clients.get("chatProduct" + chatId + deviceId) == null) {
      this.chatId = "chatProduct" + chatId + deviceId;
      this.productId = Integer.parseInt(chatId);
      if (safetyService != null && !safetyService.controlAuthorizeProduct(token, productId)) {
        try {
          session.getBasicRemote().sendText("没有权限");
          session.close();
          return;
        } catch (IOException e) {
          log.info("发送失败");
        }
      }
      if (productService != null && productService.findAllById(productId).isEmpty()) {
        try {
          session.getBasicRemote().sendText("产品不存在");
          session.close();
          return;
        } catch (IOException e) {
          log.info("发送失败");
        }
      }
      if (otaXiaozhiService != null && !otaXiaozhiService.findAllByDeviceId(deviceId).isEmpty()) {
        otaXiaozhiService.updateStatus(deviceId, "connected");
      }
      clients.put(this.chatId, session);
      isAbort.put(this.chatId, false);
      haveVoice.put(this.chatId, false);
      // 为该会话创建独立的VAD监听器实例
      SlieroVadListener listener = new SlieroVadListener();
      listener.init();
      vadListenerMap.put(this.chatId, listener);
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
    // 销毁该会话对应的VAD监听器
    SlieroVadListener listener = vadListenerMap.remove(chatId);
    if (listener != null) {
      listener.destroy();
    }
    if (chatId != null) {
      Session session = clients.get(chatId);
      if (session != null) {
        String deviceId = getDeviceId(session);
        if (otaXiaozhiService != null && deviceId != null
            && !otaXiaozhiService.findAllByDeviceId(deviceId).isEmpty()) {
          otaXiaozhiService.updateStatus(deviceId, "disconnected");
        }
      }
      if (xiaoZhiUtil != null) {
        xiaoZhiUtil.destroyMcp(chatId);
      }
      clients.remove(chatId);
      isAbort.remove(chatId);
      haveVoice.remove(chatId);
      pcmVadBuffer.remove(chatId); // 清理缓冲区
      voiceContent.remove(chatId); // 清理语音内容
      sessionStartTimeMap.remove(chatId); // 清理时间记录
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
        if (xiaoZhiUtil != null) {
          String token = getHeader(clients.get(chatId));
          xiaoZhiUtil.dealHello(chatId, json, token);
        }
      } else if (type.equals("mcp")) {
        if (xiaoZhiUtil != null) {
          xiaoZhiUtil.dealMcp(chatId, json);
        }
      } else if (type.equals("abort")) {
        isAbort.put(chatId, true);
      } else if (type.equals("listen")) {
        String state = json.getString("state");
        switch (state) {
          case "detect" -> {
            String text = json.getString("text");
            if (xiaoZhiUtil != null) {
              xiaoZhiUtil.dealDetect(chatId, productId, text);
            }
            isAbort.put(chatId, false);
          }
          case "start" -> {
            if (this.chatId.startsWith("register")) {
              if (xiaoZhiUtil != null) {
                xiaoZhiUtil.dealRegister(chatId, productId);
              }
              isAbort.put(chatId, false);
              Session session = clients.get(chatId);
              if (session != null && session.isOpen()) {
                session.close();
              }
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
            // 重置会话开始时间
            sessionStartTimeMap.put(chatId, System.currentTimeMillis());
          }
          case "stop" -> {
            if (xiaoZhiUtil != null) {
              xiaoZhiUtil.dealWithAudio(audioList, chatId, productId, isManual);
            }
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
        // 检查超时 - 基于时间而不是缓冲区大小
        long currentTime = System.currentTimeMillis();
        long startTime = sessionStartTimeMap.computeIfAbsent(chatId, k -> currentTime);

        long timeoutThreshold = isRealTime ? TIMEOUT_REALTIME * 1000L : TIMEOUT_NORMAL * 1000L;
        // 如果没有检测到语音且超时，则退出
        if (haveVoice.get(chatId) != null && !haveVoice.get(chatId)
            && (currentTime - startTime) > timeoutThreshold) {
          log.info("会话超时，chatId: {}, 经过时间: {}ms", chatId, (currentTime - startTime));

          // 清理所有缓冲区
          audioList.clear();
          pcmVadBuffer.remove(chatId);

          Session session = XiaoZhiWebsocket.clients.get(chatId);
          if (session != null && session.isOpen()) {
            session.getBasicRemote().sendText("""
                {
                  "type": "tts",
                  "state": "start"
                }""");
            if (ttsServiceFactory != null) {
              ttsServiceFactory.websocketAudioSync("时间过得真快，没什么事我先退下了", session, chatId,
                  productId);
            }
            try {
              session.getBasicRemote().sendText("{\"type\":\"tts\",\"state\":\"stop\"}");
              session.close();
            } catch (IOException e) {
              log.error("Failed to close session: {}", e.getMessage());
            }
          }
          isAbort.put(chatId, false);
          return;
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
        if (buffer != null) {
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
            // 获取该会话对应的VAD监听器实例
            SlieroVadListener listener = vadListenerMap.get(chatId);
            if (listener == null) {
              log.warn("未找到chatId对应的VAD监听器: {}", chatId);
              pcmVadBuffer.remove(chatId);
              return;
            }
            // 处理音频片段
            var map = listener.listen(chunk);
            if (map != null && map.containsKey("start")) {
              log.info("检测到语音开始: {}", map);
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
              log.info("检测到语音结束: {}", map);
              if (isRealTime) {
                isAbort.put(chatId, false);
              }
              if (xiaoZhiUtil != null) {
                xiaoZhiUtil.dealWithAudio(audioList, chatId, productId, isManual);
              }
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

          // 防御性清理：如果audioList过大（超过5分钟的数据），清理旧数据
          // 假设每帧约20ms，5分钟 = 300秒 = 15000帧
          int maxFrames = 15000;
          if (audioList.size() > maxFrames) {
            log.warn("audioList过大，chatId: {}, 当前大小: {}, 清理旧数据", chatId, audioList.size());
            int keepFrames = Math.min(100, audioList.size()); // 保留最近100帧
            if (audioList.size() > keepFrames) {
              audioList.subList(0, audioList.size() - keepFrames).clear();
            }
          }

          // 防御性清理：如果pcmVadBuffer过大，重置
          if (buffer.size() > 1024 * 1024) { // 超过1MB
            log.warn("pcmVadBuffer过大，chatId: {}, 当前大小: {}bytes, 重置缓冲区", chatId, buffer.size());
            buffer.reset();
          }
        }

      } catch (Exception e) {
        log.error("音频转换失败, chatId: {}, 错误: {}", chatId, e.getMessage(), e);
        // 异常时清理缓冲区，防止内存泄漏
        pcmVadBuffer.remove(chatId);
      }
    }
    audioList.add(message);
  }

  @OnError
  public void onError(Session session, Throwable error) {
    // onClose();
    log.error("Error in onError: {}", error.getMessage());
  }

  @PreDestroy
  public void gracefulShutdown() {
    log.info("系统正在优雅关闭，准备关闭所有 xiaozhi WebSocket 连接…");


    // 标记系统正在关闭，后续 onOpen 会拒绝连接
    isSystemShuttingDown.set(true);

    // 先 snapshot 避免并发修改
    List<Map.Entry<String, Session>> snapshot = new ArrayList<>(clients.entrySet());


    for (Map.Entry<String, Session> e : snapshot) {
      String id = e.getKey();
      Session sess = e.getValue();
      if (sess != null && sess.isOpen()) {
        try {
          sess.getBasicRemote().sendText(
              "{\"type\":\"system\",\"state\":\"shutdown\",\"message\":\"系统正在维护，将自动断开，请稍后重新连接。\"}");
          sess.close();
        } catch (Exception ex) {
          log.error("关闭 WebSocket 失败 chatId={}, error={}", id, ex.getMessage());
        }
      }
    }


    clients.clear();
    voiceContent.clear();
    pcmVadBuffer.clear();
    isAbort.clear();
    haveVoice.clear();
    sessionStartTimeMap.clear();
    // 清理所有VAD监听器实例
    for (Map.Entry<String, SlieroVadListener> entry : vadListenerMap.entrySet()) {
      try {
        entry.getValue().destroy();
      } catch (Exception ex) {
        log.error("销毁VAD监听器失败 chatId={}, error={}", entry.getKey(), ex.getMessage());
      }
    }
    vadListenerMap.clear();
    if (otaXiaozhiService != null) {
      otaXiaozhiService.cleanStatus();
    }

    log.info("所有 xiaozhi WebSocket 会话已优雅关闭完成。");
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
    String deviceId = (String) session.getUserProperties().get("Device-Id");
    if (StrUtil.isBlank(deviceId)) {
      log.error("获取deviceId失败，不安全的链接，即将关闭");
      return null;
    }
    return deviceId;
  }


}
