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

import cn.hutool.captcha.generator.RandomGenerator;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import top.rslly.iot.models.AdminConfigEntity;
import top.rslly.iot.models.ProductAsrEntity;
import top.rslly.iot.services.AdminConfigServiceImpl;
import top.rslly.iot.dao.ProductAsrRepository;
import top.rslly.iot.utility.RedisUtil;
import top.rslly.iot.utility.ai.chain.Router;
import top.rslly.iot.utility.ai.mcp.McpProtocolDeal;
import top.rslly.iot.utility.ai.mcp.McpProtocolSend;
import top.rslly.iot.utility.ai.mcp.McpWebsocket;
import top.rslly.iot.utility.ai.tools.EmotionToolAsync;
import top.rslly.iot.utility.ai.tools.ToolPrefix;
import top.rslly.iot.utility.ai.voice.ASR.AsrServiceFactory;
import top.rslly.iot.utility.ai.voice.TTS.TtsServiceFactory;
import top.rslly.iot.utility.ai.voice.concentus.OpusDecoder;

import jakarta.annotation.PreDestroy;
import jakarta.websocket.Session;
import top.rslly.iot.utility.ai.voice.concentus.OpusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Slf4j
public class XiaoZhiUtil {
  @Autowired
  private AsrServiceFactory asrServiceFactory;
  @Autowired
  private TtsServiceFactory ttsServiceFactory;
  @Autowired
  private EmotionToolAsync emotionToolAsync;
  @Autowired
  private Router router;
  @Autowired
  private RedisUtil redisUtil;
  @Autowired
  private McpProtocolDeal mcpProtocolDeal;
  @Autowired
  private AdminConfigServiceImpl adminConfigService;
  @Autowired
  private ProductAsrRepository productAsrRepository;
  @Autowired
  private RedisTemplate<String, String> redisTemplate;
  @Value("${ai.vision-explain-url}")
  private String visionExplainUrl;
  @Value("${ai.tts.skip-tool-prefix:true}")
  private boolean skipToolPrefix;
  @Value("${ai.detectRandom:false}")
  private boolean detectRandom;
  @Value("${ai.showThinking:false}")
  private boolean showThinking;
  private final ExecutorService routerExecutor = Executors.newVirtualThreadPerTaskExecutor();

  public void dealHello(String chatId, JSONObject helloObject, String token) throws IOException {
    if (chatId == null || helloObject == null) {
      log.error("dealHello参数错误: chatId={}, helloObject={}", chatId, helloObject);
      return;
    }

    boolean mcpCanUse = false;
    if (helloObject.containsKey("features")) {
      JSONObject featuresObj = helloObject.getJSONObject("features");
      if (featuresObj != null) {
        Boolean mcpValue = featuresObj.getBoolean("mcp");
        if (mcpValue != null) {
          mcpCanUse = mcpValue;
        }
      }
    }

    Session session = XiaoZhiWebsocket.clients.get(chatId);
    if (session == null || !session.isOpen()) {
      log.error("WebSocket session不存在或已关闭: chatId={}", chatId);
      return;
    }

    session.getBasicRemote().sendText(
        "{\"type\":\"hello\",\"transport\":\"websocket\",\"audio_params\":{\"sample_rate\":16000}}");
    log.info("mcp...{}", mcpCanUse);
    if (mcpCanUse && !chatId.startsWith("register")) {
      if (visionExplainUrl != null && token != null) {
        session.getBasicRemote()
            .sendText(McpProtocolSend.sendInitialize(visionExplainUrl, token, false));
        session.getBasicRemote()
            .sendText(McpProtocolSend.sendToolList("", false));
      }
    }
  }

  public void dealMcp(String chatId, JSONObject mcpObject) throws IOException {
    if (chatId == null || mcpObject == null) {
      log.error("dealMcp参数错误: chatId={}, mcpObject={}", chatId, mcpObject);
      return;
    }

    if (mcpObject.containsKey("payload")) {
      JSONObject payloadObject = mcpObject.getJSONObject("payload");
      if (payloadObject != null && payloadObject.containsKey("result")) {
        JSONObject resultObject = payloadObject.getJSONObject("result");
        if (resultObject != null) {
          Session session = XiaoZhiWebsocket.clients.get(chatId);
          if (session != null && session.isOpen()) {
            mcpProtocolDeal.dealMcp(resultObject, McpWebsocket.DEVICE_SERVER_NAME, chatId, session,
                false);
          } else {
            log.error("WebSocket session不存在或已关闭: chatId={}", chatId);
          }
        }
      }
    }
  }

  public void destroyMcp(String chatId) {
    mcpProtocolDeal.destroyMcp(McpWebsocket.DEVICE_SERVER_NAME, chatId);
  }

  /**
   * ASR处理，同步方法，解码音频二进制流
   * @param audioList 音频流
   * @param detect  检测文本
   * @return  解码后的数据或Null
   * @throws OpusException  Opus异常
   */
  public String ASRHandler(List<byte[]> audioList, int productId, String... detect) throws OpusException, IOException {
    // 数据太短
    if(audioList.size() <= 20 && detect.length == 0) return "<|2SRT|>";
    // 如果ASR服务没有启动成功，那么应当字节返回空
    if(asrServiceFactory == null) return null;
    // 已有检测文本
    if(detect.length != 0) return detect[0];
    var asrService = asrServiceFactory.getService(getProductAsrProvider(productId));
    if(asrService == null) return null;
    // 构造解码器
    OpusDecoder decoder = new OpusDecoder(16000, 1);
    // 通过字节列表输出流构造字节列表并写入文件，完成WAV->PCM的转换
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    for(byte[] bytes: audioList){
      if(bytes == null) continue;
      byte[] data_packet = new byte[16000];
      int pcm_frame = decoder.decode(bytes, 0, bytes.length,
              data_packet, 0, 960, false);
      bos.write(data_packet, 0, pcm_frame * 2);
    }
    // 生成WAV文件
    Path tempFile = Files.createTempFile("audio_", ".wav");
    Files.write(tempFile, bos.toByteArray());
    bos.close();
    // 读取WAV文件并使用ASR服务提取文本返回
    String ret = asrService.getTextRealtime(tempFile.toFile(), 16000, "pcm");
    Files.deleteIfExists(tempFile);
    return ret;
  }

  /**
   * 消息太短或无法解析时的回复
   * @param chatId  聊天的ID
   */
  private void sendForUnclearMsg(String chatId){
    XiaoZhiWebsocket.send(chatId, "{\"type\":\"stt\",\"text\":\"" + "没听清楚，说太快了" + "\"}");
  }

  /**
   * 发送助手正在思考的信息
   * @param chatId  聊天ID
   */
  private void sendWhenThinking(String chatId){
    XiaoZhiWebsocket.send(chatId, "{\"type\": \"tts\", \"state\": \"sentence_start\","
            + " \"text\": \"智能助手思考中" + "\"}");
    JSONObject emotionObject = new JSONObject();
    emotionObject.put("type", "llm");
    emotionObject.put("text", "🤔");
    emotionObject.put("emotion", "thinking");
    XiaoZhiWebsocket.send(chatId, emotionObject.toJSONString());
  }

  private void sendTTSStart(String chatId){
    XiaoZhiWebsocket.send(chatId, "{\"type\":\"tts\",\"state\":\"start\"}");
  }

  /**
   * 发送结束信息
   * @param chatId  聊天ID
   */
  private void sendEndMsg(String chatId){
    XiaoZhiWebsocket.send(chatId, "{\"type\":\"tts\",\"state\":\"stop\"}");
  }

  private void clearAudioHandlers(String chatId){
    XiaoZhiWebsocket.haveVoice.put(chatId, false);
    XiaoZhiWebsocket.isAbort.put(chatId, false);
    Router.queueMap.remove(chatId);
    this.sendEndMsg(chatId);
  }

  @Async("taskExecutor")
  public void dealWthAudio2(List<byte[]> audioList, String chatId, int productId, boolean isManual,
      String... detect) throws InterruptedException {
    if(audioList == null || chatId == null){
      log.error("audioList或chatId为空，audioList: {}, chatId: {}", audioList, chatId);
      return;
    }
    String text = "";
    try {
      text = ASRHandler(audioList, productId, detect);
    }catch(OpusException e){
      log.error("Opus音频解码错误: {}", e.getMessage());
      return;
    }catch(Exception e){
      log.error("ASR提取失败！错误：{}", e.getMessage());
      return;
    }
    // 如果ASR处理结果为空，通知用户并返回
    if(StringUtils.isEmpty(text)){
      this.sendForUnclearMsg(chatId);
      this.sendEndMsg(chatId);
      return;
    }
    // 如果音频太短，通知用户并返回
    if(text.equals("<|2SRT|>")){
      // 保留最后的10帧信息
      int keepFrames = Math.min(10, audioList.size()); // 安全处理边界
      if (audioList.size() > keepFrames) {
        audioList.subList(0, audioList.size() - keepFrames).clear();
      }
      this.sendForUnclearMsg(chatId);
    }
    final String tCopy = text;
    XiaoZhiWebsocket.voiceContent.computeIfPresent(chatId, (k, v) -> v + tCopy);
    XiaoZhiWebsocket.voiceContent.putIfAbsent(chatId, text);
    XiaoZhiWebsocket.send(chatId, text);
    audioList.clear();
    // 到这里，voiceContent不可能为空
    sendWhenThinking(chatId);
    // 下面是TTS的部分
    String voiceContent = XiaoZhiWebsocket.voiceContent.get(chatId);
    Map<String, Object> emotionMessage = new HashMap<>();
    emotionMessage.put("chatId", chatId);
    var emotionRes = emotionToolAsync.run(voiceContent, emotionMessage);
    CompletableFuture<String> res = null;
    // 首先获取router的结果
    if(router != null){
      res = CompletableFuture.supplyAsync(
              () -> router.response(voiceContent, chatId, productId),
              routerExecutor
      );
    }
    // 结果字符串构造器
    StringBuilder answerSB = new StringBuilder();
    // 用于存储回复句子
    List<String> answerList = new ArrayList<>();
    boolean emotionFlag = false;
    if(isManual){
      this.sendTTSStart(chatId);
    }
    while(res != null && !res.isDone() ||
            Router.queueMap.containsKey(chatId) && !Router.queueMap.get(chatId).isEmpty()){
      // 表情处理模块
      if(emotionRes != null&& emotionRes.isDone() && !emotionFlag){
        try {
          Map<String, String> emotionResult = emotionRes.get();
          if(emotionResult == null) continue;
          JSONObject emotionObject = new JSONObject();
          emotionObject.put("type", "llm");
          emotionObject.put("text", emotionResult.get("emoji"));
          emotionObject.put("emotion", emotionResult.get("text"));
          XiaoZhiWebsocket.send(chatId, emotionObject.toJSONString());
        }catch(Exception e){
          log.error("处理表情响应出错：{}", e.getMessage());
        }
      }
      // 处理终止
      if(XiaoZhiWebsocket.isAbort.getOrDefault(chatId, false)){
        XiaoZhiWebsocket.haveVoice.put(chatId, false);
        XiaoZhiWebsocket.isAbort.put(chatId, false);
        Router.queueMap.remove(chatId);
        this.sendEndMsg(chatId);
      }
      // SSE元素处理
      String element = Router.queueMap.get(chatId).poll();
      if(element == null){
        Thread.sleep(10);
        continue;
      }
      if(element.equals("[DONE]")){
        // 已经抵达最后一帧
        if(!answerSB.isEmpty()){
          // 将之前累计的元素入队并交由转化线程处理，
          answerList.add(answerSB.toString());
          // TODO: 异步TTS处理
        }
        break;
      }else{
        // TODO: 异步TTS处理
        // 判断是否存在标点，如果存在，则从将标点之前的已缓存元素构造字符串并入队，
        // 随后清空字符串构造器，将标点之后的部分存入构造器；如果不存在，则将整个元素
        // 加入构造器
      }
    }
  }

  @Async("taskExecutor")
  public void dealWithAudio(List<byte[]> audioList, String chatId, int productId, boolean isManual,
      String... detect)
      throws IOException {
    Path tempFile = null;
    try {
      if (audioList == null || chatId == null) {
        log.error("dealWithAudio参数错误: audioList={}, chatId={}", audioList, chatId);
        return;
      }

      OpusDecoder decoder = new OpusDecoder(16000, 1);
      if (audioList.size() > 20 || detect.length > 0) {
        Session session = XiaoZhiWebsocket.clients.get(chatId);
        if (session != null && session.isOpen() && !isManual) {
          session.getBasicRemote().sendText("""
              {
                "type": "tts",
                "state": "start"
              }""");
        }

        StringBuilder sentences = new StringBuilder();
        if (detect.length == 0) {
          // 安全读取字节数据
          ByteArrayOutputStream bos = new ByteArrayOutputStream();
          for (byte[] bytes : audioList) {
            if (bytes != null) {
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
          }
          log.info("data_size{}", bos.size());
          tempFile = Files.createTempFile("audio_", ".wav");
          Files.write(tempFile, bos.toByteArray());
          bos.close();
          if (asrServiceFactory != null) {
            var audio2Text = asrServiceFactory.getService(getProductAsrProvider(productId));
            if (audio2Text != null) {
              String text = audio2Text.getTextRealtime(tempFile.toFile(), 16000, "pcm");
              log.info("text{}", text);
              sentences.append(text);
            }
          }
        } else {
          sentences.append(detect[0]);
        }
        if (!sentences.isEmpty()) {
          if (XiaoZhiWebsocket.voiceContent.containsKey(chatId)
              && XiaoZhiWebsocket.voiceContent.get(chatId) != null
              && !XiaoZhiWebsocket.voiceContent.get(chatId).isEmpty()) {
            XiaoZhiWebsocket.voiceContent.put(chatId,
                XiaoZhiWebsocket.voiceContent.get(chatId) + sentences);
          } else {
            XiaoZhiWebsocket.voiceContent.put(chatId, sentences.toString());
          }

          session = XiaoZhiWebsocket.clients.get(chatId);
          if (session != null && session.isOpen()) {
            session.getBasicRemote()
                .sendText("{\"type\":\"stt\",\"text\":\"" + sentences + "\"}");
          }
          audioList.clear();
        } else {
          // 保留音频数据最后10帧（直接修改原始列表）
          int keepFrames = Math.min(10, audioList.size()); // 安全处理边界
          if (audioList.size() > keepFrames) {
            audioList.subList(0, audioList.size() - keepFrames).clear();
          }

          session = XiaoZhiWebsocket.clients.get(chatId);
          if (session != null && session.isOpen()) {
            session.getBasicRemote()
                .sendText("{\"type\":\"stt\",\"text\":\"" + "没听清楚，说太快了" + "\"}");
            session.getBasicRemote()
                .sendText("{\"type\":\"tts\",\"state\":\"stop\"}");
          }
        }
      } else {
        Session session = XiaoZhiWebsocket.clients.get(chatId);
        if (session != null && session.isOpen()) {
          session.getBasicRemote()
              .sendText("{\"type\":\"stt\",\"text\":\"" + "没听清楚，说太快了" + "\"}");
        }
      }
      if (XiaoZhiWebsocket.voiceContent.containsKey(chatId)
          && XiaoZhiWebsocket.voiceContent.get(chatId) != null
          && !XiaoZhiWebsocket.voiceContent.get(chatId).isEmpty()) {
        Session session = XiaoZhiWebsocket.clients.get(chatId);
        if (session != null && session.isOpen()) {
          if (showThinking) {
            session.getBasicRemote()
                .sendText("{\"type\": \"tts\", \"state\": \"sentence_start\", \"text\": \""
                    + "智能助手思考中" + "\"}");
          }
          JSONObject emotionObject = new JSONObject();
          emotionObject.put("type", "llm");
          emotionObject.put("text", "🤔");
          emotionObject.put("emotion", "thinking");
          session.getBasicRemote()
              .sendText(emotionObject.toJSONString());
        }
        log.info("listen stop,message{}", XiaoZhiWebsocket.voiceContent.get(chatId));
        Map<String, Object> emotionMessage = new HashMap<>();
        emotionMessage.put("chatId", chatId);
        var emotionRes =
            emotionToolAsync.run(XiaoZhiWebsocket.voiceContent.get(chatId), emotionMessage);
        // 异步执行router,成功后把回复发送给前端，future 返回结果
        CompletableFuture<String> res = null;
        if (router != null) {
          res = CompletableFuture
              .supplyAsync(
                  () -> router.response(XiaoZhiWebsocket.voiceContent.get(chatId), chatId,
                      productId),
                  routerExecutor);
        }
        // String answer = router.response(voiceContent.get(chatId), "chatProduct" + chatId,
        // Integer.parseInt(chatId));
        String answer = null;
        StringBuilder answerBuilder = new StringBuilder();
        boolean queuePlaybackDelivered = false;
        boolean emotionFlag = false;
        if (isManual) {
          session = XiaoZhiWebsocket.clients.get(chatId);
          if (session != null && session.isOpen()) {
            session.getBasicRemote().sendText("""
                {
                  "type": "tts",
                  "state": "start"
                }""");
          }
        }
        while ((res != null && !res.isDone()) || Router.queueMap.containsKey(chatId)
            && !Router.queueMap.get(chatId).isEmpty()) {
          if (emotionRes != null && emotionRes.isDone() && !emotionFlag) {
            try {
              Map<String, String> emotionResult = emotionRes.get();
              if (emotionResult != null) {
                JSONObject emotionObject = new JSONObject();
                emotionObject.put("type", "llm");
                emotionObject.put("text", emotionResult.get("emoji"));
                emotionObject.put("emotion", emotionResult.get("text"));
                log.info("emotionObject{}", emotionObject);

                session = XiaoZhiWebsocket.clients.get(chatId);
                if (session != null && session.isOpen()) {
                  session.getBasicRemote()
                      .sendText(emotionObject.toJSONString());
                }
                emotionFlag = true;
              }
            } catch (Exception e) {
              log.error("处理情绪响应时出错: {}", e.getMessage(), e);
            }
          }
          if (XiaoZhiWebsocket.isAbort.getOrDefault(chatId, false)) {
            XiaoZhiWebsocket.haveVoice.put(chatId, false);
            XiaoZhiWebsocket.isAbort.put(chatId, false);
            Router.queueMap.remove(chatId);
            session = XiaoZhiWebsocket.clients.get(chatId);
            if (session != null && session.isOpen()) {
              session.getBasicRemote()
                  .sendText("{\"type\":\"tts\",\"state\":\"stop\"}");
            }
            return;
          }
          if (Router.queueMap.containsKey(chatId)) {
            String element = Router.queueMap.get(chatId).poll();
            if (element != null && element.equals("[DONE]")) {
              if (!answerBuilder.isEmpty()) {
                // 立即发送已累积的内容
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("type", "tts");
                jsonObject.put("state", "sentence_start");
                jsonObject.put("text", answerBuilder.toString());

                session = XiaoZhiWebsocket.clients.get(chatId);
                if (session != null && session.isOpen()) {
                  session.getBasicRemote()
                      .sendText(jsonObject.toJSONString());
                  queuePlaybackDelivered = true;
                }
                if (ttsServiceFactory != null
                    && !shouldSkipTts(answerBuilder.toString(), skipToolPrefix)) {
                  session = XiaoZhiWebsocket.clients.get(chatId);
                  if (session != null && session.isOpen()) {
                    ttsServiceFactory.websocketAudioSync(answerBuilder.toString(),
                        session,
                        chatId, productId);
                  }
                }
                answerBuilder.setLength(0);
              }

              session = XiaoZhiWebsocket.clients.get(chatId);
              if (session != null && session.isOpen()) {
                session.getBasicRemote()
                    .sendText("{\"type\":\"tts\",\"state\":\"stop\"}");
              }
              XiaoZhiWebsocket.haveVoice.put(chatId, false);
              XiaoZhiWebsocket.isAbort.put(chatId, false);
              Router.queueMap.remove(chatId);
              return;
            }
            else if (element != null) {
              element = element.replace("\n", "");
              // 查找字符串中的第一个标点位置
              int punctuationIndex = -1;
              for (int i = 0; i < element.length(); i++) {
                char c = element.charAt(i);
                if (c == '。' || c == '？' || c == '！' || c == '；' || c == '：' || c == '.' || c == '?'
                    || c == '!' || c == '~' || c == '～') {
                  punctuationIndex = i;
                  break;
                }
              }

              if (punctuationIndex != -1) {
                // 截取标点之前的内容（包括标点）
                String partBeforePunctuation = element.substring(0, punctuationIndex + 1);
                answerBuilder.append(partBeforePunctuation);
                String answerBuilderString = answerBuilder.toString().replace("\\n", "");

                // 立即发送已累积的内容
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("type", "tts");
                jsonObject.put("state", "sentence_start");
                jsonObject.put("text", answerBuilderString);

                session = XiaoZhiWebsocket.clients.get(chatId);
                if (session != null && session.isOpen()) {
                  session.getBasicRemote()
                      .sendText(jsonObject.toJSONString());
                  queuePlaybackDelivered = true;
                }
                if (ttsServiceFactory != null
                    && !shouldSkipTts(answerBuilderString, skipToolPrefix)) {
                  session = XiaoZhiWebsocket.clients.get(chatId);
                  if (session != null && session.isOpen()) {
                    ttsServiceFactory.websocketAudioSync(answerBuilderString,
                        session,
                        chatId, productId);
                  }
                }
                answerBuilder.setLength(0);

                // 将剩余部分添加到builder中（不发送）
                String remainingPart = element.substring(punctuationIndex + 1);
                if (!remainingPart.isEmpty()) {
                  answerBuilder.append(remainingPart);
                }
              } else {
                // 没有标点，直接添加整个元素
                answerBuilder.append(element);

                // 检查是否需要发送（基于长度条件）
                if (answerBuilder.length() > 100) {
                  session = XiaoZhiWebsocket.clients.get(chatId);
                  if (session != null && session.isOpen()) {
                    session.getBasicRemote()
                        .sendText("{\"type\": \"tts\", \"state\": \"sentence_start\", \"text\": \""
                            + answerBuilder + "\"}");
                    queuePlaybackDelivered = true;
                  }
                  if (ttsServiceFactory != null
                      && !shouldSkipTts(answerBuilder.toString(), skipToolPrefix)) {
                    session = XiaoZhiWebsocket.clients.get(chatId);
                    if (session != null && session.isOpen()) {
                      ttsServiceFactory.websocketAudioSync(answerBuilder.toString(),
                          session,
                          chatId, productId);
                    }
                  }
                  answerBuilder.setLength(0);
                }
              }
            }
          }
          // 延时，防止cpu空转
          Thread.sleep(10);
        }
        if (!emotionFlag) {
          JSONObject emotionObject = new JSONObject();
          emotionObject.put("type", "llm");
          emotionObject.put("text", "😶");
          emotionObject.put("emotion", "neutral");

          session = XiaoZhiWebsocket.clients.get(chatId);
          if (session != null && session.isOpen()) {
            session.getBasicRemote()
                .sendText(emotionObject.toJSONString());
          }
        }
        if (res != null) {
          answer = res.get();
        }
        if (StringUtils.isEmpty(answer)) {
          answer = "抱歉，我暂时无法理解您的问题。";
        }
        answer = resolveFinalVoiceAnswer(answer, answerBuilder.toString(), queuePlaybackDelivered);
        if (answer.length() > 500)
          answer = answer.substring(0, 500);
        if (!answer.isBlank()) {
          splitSentences(answer, chatId, productId);
        }
      }
    } catch (Exception e) {
      log.error("Error in dealWithAudio: {}", e.getMessage(), e);
    } finally {
      XiaoZhiWebsocket.haveVoice.put(chatId, false);
      XiaoZhiWebsocket.isAbort.put(chatId, false);
      try {
        if (tempFile != null) {
          Files.deleteIfExists(tempFile);
        }
      } catch (IOException e) {
        log.warn("Failed to delete temp audio file for {}: {}", chatId, e.getMessage());
      }
      try {
        Router.queueMap.remove(chatId);
        Session session = XiaoZhiWebsocket.clients.get(chatId);
        if (session != null && session.isOpen()) {
          session.getBasicRemote().sendText("{\"type\":\"tts\",\"state\":\"stop\"}");
        }
      } catch (IOException e) {
        log.warn("Failed to send stop message to client {}: {}", chatId, e.getMessage());
      }
    }
  }

  @PreDestroy
  public void shutdownExecutors() {
    routerExecutor.close();
  }

  public void dealDetect(String chatId, int productId, String text) throws IOException {
    if (chatId == null) {
      log.error("dealDetect参数错误: chatId is null");
      return;
    }

    if (!getDetectRandomConfig()) {
      dealWithAudio(new ArrayList<>(), chatId, productId, true, text);
      return;
    }

    Session session = XiaoZhiWebsocket.clients.get(chatId);
    if (session == null || !session.isOpen()) {
      log.error("WebSocket session不存在或已关闭: chatId={}", chatId);
      return;
    }

    session.getBasicRemote().sendText("""
        {
          "type": "tts",
          "state": "start"
        }""");
    // 定义问候语列表
    List<String> greetings = Arrays.asList(
        "很高兴见到你",
        "你好啊",
        "我们又见面了",
        "最近可好?",
        "很高兴再次和你谈话",
        "在干嘛");

    // 生成随机索引
    Random random = new Random();

    String selectedGreeting = greetings.get(random.nextInt(greetings.size()));

    // 发送随机问候语
    if (ttsServiceFactory != null && selectedGreeting != null) {
      ttsServiceFactory.websocketAudioSync(selectedGreeting, session, chatId, productId);
    }


    session.getBasicRemote()
        .sendText("{\"type\":\"tts\",\"state\":\"stop\"}");
  }

  public void dealRegister(String chatId, int productId) throws IOException {
    if (chatId == null) {
      log.error("dealRegister参数错误: chatId is null");
      return;
    }

    RandomGenerator randomGenerator = new RandomGenerator("0123456789", 6);
    String code = randomGenerator.generate();
    String registerMsg = "请登录到控制面板添加设备，输入验证码" + code;

    Session session = XiaoZhiWebsocket.clients.get(chatId);
    if (session == null || !session.isOpen()) {
      log.error("WebSocket session不存在或已关闭: chatId={}", chatId);
      return;
    }

    String deviceId = XiaoZhiWebsocket.getDeviceId(session);
    if (redisUtil != null && deviceId != null && code != null) {
      redisUtil.set(code, deviceId, 60 * 5);
    }

    session.getBasicRemote()
        .sendText("{\"type\": \"tts\", \"state\": \"sentence_start\", \"text\": \""
            + registerMsg + "\"}");
    session.getBasicRemote().sendText("""
        {
        "type": "tts",
        "state": "start"
        }""");

    if (ttsServiceFactory != null) {
      ttsServiceFactory.websocketAudioSync(registerMsg, session, chatId, productId);
    }

    session.getBasicRemote()
        .sendText("{\"type\":\"tts\",\"state\":\"stop\"}");
  }

  // 分割句子
  private void splitSentences(String answer, String chatId, int productId) throws IOException {
    if (answer == null || chatId == null) {
      log.error("splitSentences参数错误: answer={}, chatId={}", answer, chatId);
      return;
    }

    String[] sentences = answer.split("(?<=[。？！；：])");
    for (String sentence : sentences) {
      if (sentence == null)
        continue;
      sentence = sentence.trim();
      Boolean isAborted = XiaoZhiWebsocket.isAbort.get(chatId);
      if (isAborted != null && isAborted) {
        return;
      }
      if (sentence.isEmpty())
        continue;
      JSONObject jsonObject = new JSONObject();
      jsonObject.put("type", "tts");
      jsonObject.put("state", "sentence_start");
      jsonObject.put("text", sentence);
      log.info(sentence);

      Session session = XiaoZhiWebsocket.clients.get(chatId);
      if (session != null && session.isOpen()) {
        session.getBasicRemote()
            .sendText(jsonObject.toJSONString());
        if (!shouldSkipTts(sentence, skipToolPrefix)) {
          if (ttsServiceFactory != null) {
            ttsServiceFactory.websocketAudioSync(sentence, session, chatId, productId);
          }
        }
      }
    }
  }

  private static boolean shouldSkipTts(String text, boolean globalSkipConfig) {
    if (!globalSkipConfig)
      return false;
    if (text == null)
      return false;
    return ToolPrefix.startsWithAnyPrefix(text);
  }

  static String resolveFinalVoiceAnswer(String finalAnswer, String pendingStreamText,
      boolean queuePlaybackDelivered) {
    if (!queuePlaybackDelivered) {
      return finalAnswer == null ? "" : finalAnswer;
    }
    return pendingStreamText == null ? "" : pendingStreamText;
  }

  private boolean getDetectRandomConfig() {
    try {
      if (adminConfigService != null) {
        List<AdminConfigEntity> configs = adminConfigService.findAllBySetKey("ai_detect_random");
        if (configs != null && !configs.isEmpty() && configs.get(0) != null) {
          String setValue = configs.get(0).getSetValue();
          if (setValue != null) {
            return Boolean.parseBoolean(setValue);
          }
        }
      }
    } catch (Exception e) {
      log.error("从数据库获取detectRandom配置失败", e);
    }
    // 数据库查不到时使用配置文件默认值
    return detectRandom;
  }

  private String getProductAsrProvider(int productId) {
    try {
      if (productAsrRepository != null) {
        List<ProductAsrEntity> productAsrList = productAsrRepository.findAllByProductId(productId);
        if (productAsrList != null && !productAsrList.isEmpty() && productAsrList.get(0) != null) {
          String asrName = productAsrList.get(0).getAsrName();
          if (asrName != null && !asrName.isEmpty()) {
            log.info("产品 {} 使用数据库配置的ASR: {}", productId, asrName);
            return asrName;
          }
        }
      }
    } catch (Exception e) {
      log.error("从数据库获取产品ASR配置失败, productId: {}", productId, e);
    }
    // 数据库没有配置，返回null让AsrServiceFactory使用默认配置
    log.info("产品 {} 没有数据库ASR配置，使用默认配置", productId);
    return null;
  }
}
