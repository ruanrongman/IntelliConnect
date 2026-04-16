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
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

@ServerEndpoint(value = "/xiaozhi/v1/{chatId}", configurator = WebSocketConfig.class)
@Component
@Slf4j
public class XiaoZhiWebsocket {
  public static final Map<String, Session> clients = new ConcurrentHashMap<>();
  public static final Map<String, String> voiceContent = new ConcurrentHashMap<>();
  private static final Map<String, ByteArrayOutputStream> pcmVadBuffer = new ConcurrentHashMap<>();
  private static final Map<String, OpusDecoder> opusDecoderMap = new ConcurrentHashMap<>();
  public static final Map<String, Boolean> isAbort = new ConcurrentHashMap<>();
  public static final Map<String, Boolean> haveVoice = new ConcurrentHashMap<>();
  // 每个连接独立的发送锁，保证WebSocket串行发送，避免TEXT_FULL_WRITING错误
  private static final ConcurrentHashMap<String, ReentrantLock> sendLocks =
      new ConcurrentHashMap<>();

  /**
   * Track active playback session for each chatId to prevent audio overlap
   */
  public static class PlaybackSession {
    public final Thread playbackThread;
    public final BlockingQueue<byte[]> audioQueue;
    public final AtomicBoolean isCancelled;

    public PlaybackSession(Thread playbackThread, BlockingQueue<byte[]> audioQueue) {
      this.playbackThread = playbackThread;
      this.audioQueue = audioQueue;
      this.isCancelled = new AtomicBoolean(false);
    }
  }

  public static final Map<String, PlaybackSession> activePlayback = new ConcurrentHashMap<>();
  private static volatile SafetyServiceImpl safetyService;
  private static volatile TtsServiceFactory ttsServiceFactory;
  private static volatile ProductServiceImpl productService;
  private static volatile XiaoZhiUtil xiaoZhiUtil;
  private static volatile OtaXiaozhiServiceImpl otaXiaozhiService;
  public static final AtomicBoolean isSystemShuttingDown = new AtomicBoolean(false);
  // 改为静态Map，为每个会话维护独立的VAD监听器实例
  private static final Map<String, SlieroVadListener> vadListenerMap = new ConcurrentHashMap<>();
  // 记录最近一次有效交互时间，用于空闲超时判断
  private static final Map<String, Long> lastInteractionTimeMap = new ConcurrentHashMap<>();
  private final List<byte[]> audioList = Collections.synchronizedList(new ArrayList<>());
  private String chatId;
  private int productId;
  private boolean isManual = true;
  private boolean isRealTime = false;

  // 使用静态变量存储配置
  private static long TIMEOUT_NORMAL = 60; // 默认60秒
  private static long TIMEOUT_REALTIME = 120; // 默认120秒

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
        refreshInteractionTime(this.chatId);
        // 为该会话创建独立的VAD监听器实例
        SlieroVadListener listener = new SlieroVadListener();
        listener.init();
        if (!listener.isReady()) {
          session.getBasicRemote().sendText("VAD初始化失败");
          session.close();
          clients.remove(this.chatId);
          isAbort.remove(this.chatId);
          haveVoice.remove(this.chatId);
          lastInteractionTimeMap.remove(this.chatId);
          return;
        }
        vadListenerMap.put(this.chatId, listener);
        opusDecoderMap.put(this.chatId, new OpusDecoder(16000, 1));
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
      refreshInteractionTime(this.chatId);
      try {
        // 为该会话创建独立的VAD监听器实例
        SlieroVadListener listener = new SlieroVadListener();
        listener.init();
        if (!listener.isReady()) {
          session.getBasicRemote().sendText("VAD初始化失败");
          session.close();
          clients.remove(this.chatId);
          isAbort.remove(this.chatId);
          haveVoice.remove(this.chatId);
          lastInteractionTimeMap.remove(this.chatId);
          return;
        }
        vadListenerMap.put(this.chatId, listener);
        opusDecoderMap.put(this.chatId, new OpusDecoder(16000, 1));
      } catch (Exception e) {
        log.error("初始化VAD会话失败, chatId={}", this.chatId, e);
        try {
          session.getBasicRemote().sendText("VAD初始化失败");
          session.close();
        } catch (IOException ex) {
          log.error("关闭初始化失败会话失败, chatId={}", this.chatId, ex);
        }
        clients.remove(this.chatId);
        isAbort.remove(this.chatId);
        haveVoice.remove(this.chatId);
        lastInteractionTimeMap.remove(this.chatId);
        return;
      }
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
      // 取消正在播放的音频
      cancelActivePlayback(chatId);
      clients.remove(chatId);
      isAbort.remove(chatId);
      haveVoice.remove(chatId);
      pcmVadBuffer.remove(chatId); // 清理缓冲区
      opusDecoderMap.remove(chatId);
      voiceContent.remove(chatId); // 清理语音内容
      lastInteractionTimeMap.remove(chatId); // 清理时间记录
      sendLocks.remove(chatId); // 清理发送锁
    }
  }

  /**
   * socket message
   */
  @OnMessage
  public void onMessage(String message) {
    log.debug("message{}", message);
    try {
      refreshInteractionTime(chatId);
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
            voiceContent.remove(chatId);
            refreshInteractionTime(chatId);
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
        long currentTime = System.currentTimeMillis();
        byte[] bytes = message.clone();
        OpusDecoder decoder = opusDecoderMap.get(chatId);
        if (decoder == null) {
          decoder = new OpusDecoder(16000, 1);
          opusDecoderMap.put(chatId, decoder);
        }
        byte[] data_packet = new byte[16000];
        boolean interactionDetected = Boolean.TRUE.equals(haveVoice.get(chatId));

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
              log.debug("检测到语音开始: {}", map);
              haveVoice.put(chatId, true);
              interactionDetected = true;
              if (isRealTime) {
                isAbort.put(chatId, true);
              }
              // 主动取消当前正在播放的音频，防止混叠
              cancelActivePlayback(chatId);
              // 保留音频数据最后10帧（直接修改原始列表）
              int keepFrames = Math.min(10, audioList.size()); // 安全处理边界
              trimAudioFrames(keepFrames);
            }
            if (map != null && map.containsKey("end")) {
              log.info("检测到语音结束: {}", map);
              interactionDetected = true;
              if (isRealTime) {
                isAbort.put(chatId, false);
              }
              refreshInteractionTime(chatId);
              if (xiaoZhiUtil != null) {
                xiaoZhiUtil.dealWithAudio(audioList, chatId, productId, isManual);
              }
              pcmVadBuffer.remove(chatId);
              return;
            }
            processed += 512 * 2;
          }

          if (interactionDetected) {
            refreshInteractionTime(chatId);
          } else if (hasExceededIdleTimeout(chatId, currentTime, isRealTime)) {
            closeSessionForIdleTimeout(currentTime);
            return;
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
            trimAudioFrames(keepFrames);
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
    new ArrayList<>(voiceContent.keySet()).forEach(voiceContent::remove);
    pcmVadBuffer.clear();
    opusDecoderMap.clear();
    isAbort.clear();
    haveVoice.clear();
    lastInteractionTimeMap.clear();
    // 清理所有VAD监听器实例
    for (Map.Entry<String, SlieroVadListener> entry : vadListenerMap.entrySet()) {
      try {
        entry.getValue().destroy();
      } catch (Exception ex) {
        log.error("销毁VAD监听器失败 chatId={}, error={}", entry.getKey(), ex.getMessage());
      }
    }
    vadListenerMap.clear();
    activePlayback.clear();
    SlieroVadListener.destroySharedResources();
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

  /**
   * 获取客户端IP地址
   */
  public static String getClientIp(Session session) {
    String ip = (String) session.getUserProperties().get("Client-Ip");
    return ip != null ? ip : "unknown";
  }

  static boolean hasExceededIdleTimeout(String chatId, long currentTime, boolean isRealTime) {
    Long lastInteractionTime = lastInteractionTimeMap.computeIfAbsent(chatId, k -> currentTime);
    return hasExceededIdleTimeout(lastInteractionTime, currentTime, isRealTime);
  }

  static boolean hasExceededIdleTimeout(long lastInteractionTime, long currentTime,
      boolean isRealTime) {
    long timeoutThreshold = getIdleTimeoutMillis(isRealTime);
    return (currentTime - lastInteractionTime) > timeoutThreshold;
  }

  static long getIdleTimeoutMillis(boolean isRealTime) {
    long timeoutSeconds = isRealTime ? TIMEOUT_REALTIME : TIMEOUT_NORMAL;
    return timeoutSeconds * 1000L;
  }

  private static void refreshInteractionTime(String chatId) {
    if (chatId != null) {
      lastInteractionTimeMap.put(chatId, System.currentTimeMillis());
    }
  }

  private void trimAudioFrames(int keepFrames) {
    synchronized (audioList) {
      int safeKeepFrames = Math.max(0, Math.min(keepFrames, audioList.size()));
      if (audioList.size() > safeKeepFrames) {
        audioList.subList(0, audioList.size() - safeKeepFrames).clear();
      }
    }
  }

  private void closeSessionForIdleTimeout(long currentTime) throws IOException {
    Long lastInteractionTime = lastInteractionTimeMap.get(chatId);
    long idleMillis = lastInteractionTime == null ? 0L : currentTime - lastInteractionTime;
    log.info("会话空闲超时，chatId: {}, 空闲时长: {}ms", chatId, idleMillis);

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
    cancelActivePlayback(chatId);
  }

  /**
   * Cancel active playback for the given chatId to prevent audio overlap.
   *
   * Strategy: 1. Mark as cancelled and clear the audio queue immediately 2. Wait a short time
   * (40ms) for the playback thread to exit naturally 3. Don't force interrupt because that would
   * interrupt the entire caller thread (e.g. MusicPlayer) causing unexpected side effects like
   * connection close
   *
   * Why 40ms? - The playback thread checks cancellation every ~20ms, so 40ms is enough for it to
   * detect and exit, and 40ms wait is not noticeable to users.
   *
   * This balances: - No audio overlap: Old playback exits before new playback starts - No卡顿: Only
   * 40ms wait, not noticeable - No side effects: No forced interrupt, MusicPlayer works correctly
   */
  public static void cancelActivePlayback(String chatId) {
    PlaybackSession session = activePlayback.remove(chatId);
    if (session != null) {
      log.debug("Cancelling active playback for chatId: {}", chatId);
      session.isCancelled.set(true);
      if (session.audioQueue != null) {
        session.audioQueue.clear();
      }
      // Wait a short time for the thread to exit on its own
      // 40ms is enough because playback checks every ~20ms, not noticeable to users
      // Don't interrupt - it would break the caller thread causing connection close
      if (session.playbackThread != null && session.playbackThread.isAlive()) {
        try {
          session.playbackThread.join(40);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  public static void send(String chatId, String msg) {
    Session session = XiaoZhiWebsocket.clients.get(chatId);
    if (session == null || !session.isOpen())
      return;
    // 获取或创建该连接对应的发送锁，保证同一连接串行发送
    try {
      session.getBasicRemote().sendText(msg);
    } catch (IOException e) {
      log.error("发送消息失败：{}", e.getMessage());
    }
  }
}
