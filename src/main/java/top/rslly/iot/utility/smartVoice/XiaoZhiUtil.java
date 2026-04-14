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
import com.alibaba.druid.sql.visitor.functions.Char;
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
import top.rslly.iot.utility.ai.voice.AudioUtils;
import top.rslly.iot.utility.ai.voice.TTS.TtsServiceFactory;
import top.rslly.iot.utility.ai.voice.concentus.OpusDecoder;

import jakarta.annotation.PreDestroy;
import jakarta.websocket.Session;
import top.rslly.iot.utility.ai.voice.concentus.OpusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

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
  private RedisTemplate<String, byte[]> bytesRedisTemplate;
  @Autowired
  private RedisTemplate<String, Boolean> redisStateTemplate; // 用于记录TTS段落的处理状态
  @Autowired
  private RedisTemplate<String, String> redisStringTemplate;
  @Value("${ai.vision-explain-url}")
  private String visionExplainUrl;
  @Value("${ai.tts.skip-tool-prefix:true}")
  private boolean skipToolPrefix;
  @Value("${ai.detectRandom:false}")
  private boolean detectRandom;
  @Value("${ai.showThinking:false}")
  private boolean showThinking;
  @Value("${ai.tts.cache-expire-time}")
  private long ttsCacheExpireTime;
  private final ExecutorService routerExecutor = Executors.newVirtualThreadPerTaskExecutor();

  private final String STREAM_AUDIO_HANDLER_FLAG = "<|SAH|>";
  private final String STREAM_RESULT_HANDLER_FLAG = "<|SRH|>";

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
    log.debug("mcp...{}", mcpCanUse);
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
   * 
   * @param audioList 音频流
   * @param detect 检测文本
   * @return 解码后的数据或Null
   * @throws OpusException Opus异常
   */
  public String ASRHandler(List<byte[]> audioList, int productId, String... detect)
      throws OpusException, IOException {
    // 数据太短
    if (audioList.size() <= 20 && detect.length == 0)
      return "<|2SRT|>";
    // 如果ASR服务没有启动成功，那么应当字节返回空
    if (asrServiceFactory == null)
      return null;
    // 已有检测文本
    if (detect.length != 0)
      return detect[0];
    var asrService = asrServiceFactory.getService(getProductAsrProvider(productId));
    if (asrService == null)
      return null;
    // 构造解码器
    OpusDecoder decoder = new OpusDecoder(16000, 1);
    // 通过字节列表输出流构造字节列表并写入文件，完成WAV->PCM的转换
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    for (byte[] bytes : audioList) {
      if (bytes == null)
        continue;
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
   * 
   * @param chatId 聊天的ID
   */
  private void sendForUnclearMsg(String chatId) {
    XiaoZhiWebsocket.send(chatId, "{\"type\":\"stt\",\"text\":\"" + "没听清楚，说太快了" + "\"}");
  }

  /**
   * 发送文本
   * 
   * @param chatId 聊天ID
   * @param text 需要发送的文本
   */
  private void sendText(String chatId, String text) {
    XiaoZhiWebsocket.send(chatId, "{\"type\": \"tts\", \"state\": \"sentence_start\","
        + "\"text\": \"" + text + "\"}");
  }

  /**
   * 发送助手正在思考的信息
   * 
   * @param chatId 聊天ID
   */
  private void sendWhenThinking(String chatId) {
    sendText(chatId, "智能助手思考中");
    JSONObject emotionObject = new JSONObject();
    emotionObject.put("type", "llm");
    emotionObject.put("text", "🤔");
    emotionObject.put("emotion", "thinking");
    XiaoZhiWebsocket.send(chatId, emotionObject.toJSONString());
  }

  private void sendTTSStart(String chatId) {
    XiaoZhiWebsocket.send(chatId, "{\"type\":\"tts\",\"state\":\"start\"}");
  }

  private void sendSTT(String chatId, String text) {
    XiaoZhiWebsocket.send(chatId, "{\"type\":\"stt\",\"text\":\"" + text + "\"}");
  }

  /**
   * 发送结束信息
   * 
   * @param chatId 聊天ID
   */
  private void sendEndMsg(String chatId) {
    XiaoZhiWebsocket.send(chatId, "{\"type\":\"tts\",\"state\":\"stop\"}");
  }

  private void clearAudioHandlers(String chatId) {
    sendEndMsg(chatId);
    XiaoZhiWebsocket.haveVoice.put(chatId, false);
    XiaoZhiWebsocket.isAbort.put(chatId, false);
    Router.queueMap.remove(chatId);
    this.sendEndMsg(chatId);
  }

  private int getPunctuationPos(String str) {
    Set<Character> punctuationSet = new HashSet<>();
    String punctuations = "？！：；~。";
    String[] englishPunctuations = {"? ", "! ", "\" ", " \"", ": ", ", ", ". "};
    for (char c : punctuations.toCharArray()) {
      punctuationSet.add(c);
    }
    for (int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);
      if (punctuationSet.contains(c))
        return i;
    }
    for (int i = 0; i < englishPunctuations.length; i++) {
      String item = englishPunctuations[i];
      if (str.contains(item)) {
        return i;
      }
    }
    return -1;
  }

  /**
   * 清空Redis历史缓存
   * 
   * @param chatId 对话ID
   */
  private void clearRedisCache(String chatId) {
    redisStringTemplate.delete(chatId);
    redisStateTemplate.delete(chatId + "_state");
  }

  public static String getShortHash(String input, int bytes) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5"); // 或 "SHA-1"
      byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
      // 截取前 bytes 个字节
      byte[] truncated = new byte[bytes];
      System.arraycopy(digest, 0, truncated, 0, bytes);
      // 转为十六进制字符串
      return new BigInteger(1, truncated).toString(16);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * 异步TTS
   * 
   * @param chatId 对话ID
   * @param src 原文本
   * @param productId 产品ID
   */
  private void asyncTTS(String chatId, String src, int productId) {
    // 使用Thread.ofVirtual启动这个部分
    // 在Redis中写入当前句子的处理状态为false，表示开始处理
    // 将处理得到的结果转为String并存入Redis，并将当前句子的处理状态置为true以表示完成
    if (shouldSkipTts(src, skipToolPrefix))
      return;
    redisStateTemplate.opsForHash().put(chatId + "_state", src, false);
    String srcHash = getShortHash(src, 16);
    if (bytesRedisTemplate.hasKey(srcHash)) {
      redisStateTemplate.opsForHash().put(chatId + "_state", src, true);
      return;
    }
    List<byte[]> audioBytes = ttsServiceFactory.getTextAudio(chatId, src, productId);
    if (audioBytes == null) {
      log.error("No TTS result! chatID: {}, text: {}", chatId, src);
      redisStateTemplate.opsForHash().put(chatId + "_state", src, true);
      return;
    }
    bytesRedisTemplate.opsForList().leftPushAll(srcHash, audioBytes);
    bytesRedisTemplate.expire(srcHash, ttsCacheExpireTime, TimeUnit.MILLISECONDS);
    redisStateTemplate.opsForHash().put(chatId + "_state", src, true);
  }

  /**
   * 流式返回时的结果处理线程
   * 
   * @param chatId 对话ID
   */
  private void streamRspResultHandler(String chatId) {
    // 设置结果处理线程状态为工作中
    redisStateTemplate.opsForHash().put(chatId + "_state", STREAM_RESULT_HANDLER_FLAG, true);
    Object handleState =
        redisStateTemplate.opsForHash().get(chatId + "_state", STREAM_AUDIO_HANDLER_FLAG);
    if (handleState == null) {
      log.error("处理状态错误");
      return;
    }
    String crtS = null;
    while ((boolean) handleState) {
      if (XiaoZhiWebsocket.isAbort.getOrDefault(chatId, false))
        break;
      handleState =
          redisStateTemplate.opsForHash().get(chatId + "_state", STREAM_AUDIO_HANDLER_FLAG);
      if (handleState == null || !(boolean) handleState)
        break;
      // 左进右出模拟队列
      if (crtS == null)
        crtS = redisStringTemplate.opsForList().rightPop(chatId);
      if (StringUtils.isEmpty(crtS))
        continue;
      if (shouldSkipTts(crtS, skipToolPrefix)) {
        sendText(chatId, crtS);
        crtS = null;
        continue;
      }
      if (crtS.equals("<|EOS|>"))
        break;
      if (!redisStateTemplate.opsForHash().hasKey(chatId + "_state", crtS))
        continue;
      Object state = redisStateTemplate.opsForHash().get(chatId + "_state", crtS);
      if (state == null || !(boolean) state)
        continue;
      BlockingQueue<byte[]> sendQueue = new LinkedBlockingQueue<>();
      String srcHash = getShortHash(crtS, 16);
      Long audioBytesSize = bytesRedisTemplate.opsForList().size(srcHash);
      // 如果缓存的长度为0，直接跳过后续步骤
      if (audioBytesSize == null || audioBytesSize == 0)
        continue;
      for (long i = audioBytesSize - 1; i >= 0; i--) {
        // 左进右出，队列
        byte[] bytes = bytesRedisTemplate.opsForList().index(srcHash, i);
        if (bytes == null)
          continue;
        sendQueue.offer(bytes);
      }
      Session session = XiaoZhiWebsocket.clients.get(chatId);
      sendText(chatId, crtS);
      AudioUtils.asyncSendAudioQueue(chatId, session, sendQueue);
      crtS = null;
    }
    // 设置结果处理线程状态为结束
    redisStateTemplate.opsForHash().put(chatId + "_state", STREAM_RESULT_HANDLER_FLAG, false);
  }

  /**
   * 流式返回的处理方法
   * 
   * @param chatId 对话ID
   * @param productId 产品ID
   * @param isManual 是否对讲机模式
   * @throws InterruptedException 由Thread.sleep抛出的异常
   */
  private void handlerStreamRsp(String chatId, int productId, boolean isManual,
      CompletableFuture<String> res) throws InterruptedException {
    clearRedisCache(chatId);
    String voiceContent = XiaoZhiWebsocket.voiceContent.get(chatId);
    Map<String, Object> emotionMessage = new HashMap<>();
    emotionMessage.put("chatId", chatId);
    var emotionRes = emotionToolAsync.run(voiceContent, emotionMessage);
    // 结果字符串构造器
    StringBuilder answerSB = new StringBuilder();
    boolean emotionFlag = false;
    this.sendTTSStart(chatId);
    // 设置处理状态为true
    redisStateTemplate.opsForHash().put(chatId + "_state", STREAM_AUDIO_HANDLER_FLAG, true);
    // 逻辑是这样的：将收取到的句子放入Redis队列中，结果处理线程查询队列并以此处理
    Thread.ofVirtual().start(() -> {
      streamRspResultHandler(chatId);
    });
    while (res != null && !res.isDone() ||
        Router.queueMap.containsKey(chatId) && !Router.queueMap.get(chatId).isEmpty()) {
      Queue<String> queue = Router.queueMap.get(chatId);
      if (queue == null || queue.isEmpty())
        continue;
      // 表情处理模块
      if (emotionRes != null && emotionRes.isDone() && !emotionFlag) {
        try {
          Map<String, String> emotionResult = emotionRes.get();
          if (emotionResult == null)
            continue;
          JSONObject emotionObject = new JSONObject();
          emotionObject.put("type", "llm");
          emotionObject.put("text", emotionResult.get("emoji"));
          emotionObject.put("emotion", emotionResult.get("text"));
          XiaoZhiWebsocket.send(chatId, emotionObject.toJSONString());
        } catch (Exception e) {
          log.error("处理表情响应出错：{}", e.getMessage());
        }
      }
      // 处理终止
      if (XiaoZhiWebsocket.isAbort.getOrDefault(chatId, false)) {
        XiaoZhiWebsocket.haveVoice.put(chatId, false);
        XiaoZhiWebsocket.isAbort.put(chatId, false);
        Router.queueMap.remove(chatId);
        this.sendEndMsg(chatId);
      }
      // SSE元素处理
      String element = queue.poll();
      if (element == null) {
        Thread.sleep(10);
        continue;
      }
      if (element.equals("[DONE]")) {
        // 已经抵达最后一帧
        if (!answerSB.isEmpty()) {
          // 将之前累计的元素入队并交由转化线程处理，
          // 将构造器中缓存的部分交由虚拟线程处理
          String sentence = answerSB.toString();
          // 不允许处理空字符串
          if (StringUtils.isEmpty(sentence))
            break;
          redisStringTemplate.opsForList().leftPush(chatId, sentence);
          Thread.ofVirtual().start(() -> {
            this.asyncTTS(chatId, sentence, productId);
          });
          answerSB.setLength(0);
        }
        break;
      } else {
        // 判断是否存在标点，如果存在，则从将标点之前的已缓存元素构造字符串并入队，
        // 随后清空字符串构造器，将标点之后的部分存入构造器；如果不存在，则将整个元素
        // 加入构造器
        element = element.replace("\n", "");
        int pIdx = this.getPunctuationPos(element);
        // 小于五个字符认为需要连读
        if (pIdx != -1 && answerSB.length() >= 5) {
          String eBefore = element.substring(0, pIdx + 1);
          String eAfter = element.substring(pIdx + 1);
          answerSB.append(eBefore);
          String before = answerSB.toString();
          // 不允许向结果列表中写入空字符串
          if (StringUtils.isEmpty(before))
            continue;
          redisStringTemplate.opsForList().leftPush(chatId, before);
          // 将标点前的部分交由异步虚拟线程处理
          Thread.ofVirtual().start(() -> {
            this.asyncTTS(chatId, before, productId);
          });
          answerSB.setLength(0);
          answerSB.append(eAfter);
        } else
          answerSB.append(element);
      }
    }
    // 边界处理
    redisStringTemplate.opsForList().leftPush(chatId, "<|EOS|>");
    Object resultHandlerState =
        redisStateTemplate.opsForHash().get(chatId + "_state", STREAM_RESULT_HANDLER_FLAG);
    if (resultHandlerState == null) {
      log.error("结果处理线程出错");
      return;
    }
    // 等待结果处理线程结束
    while (true) {
      resultHandlerState =
          redisStateTemplate.opsForHash().get(chatId + "_state", STREAM_RESULT_HANDLER_FLAG);
      if (resultHandlerState == null)
        continue;
      if (!(boolean) resultHandlerState)
        break;
    }
    redisStateTemplate.opsForHash().put(chatId + "_state", STREAM_AUDIO_HANDLER_FLAG, false);
    this.clearAudioHandlers(chatId);
  }

  private void handlerSyncRsp(String chatId, int productId, CompletableFuture<String> res)
      throws IOException, ExecutionException, InterruptedException {
    log.info("Sync Rsp");
    JSONObject emotionObject = new JSONObject();
    emotionObject.put("type", "llm");
    emotionObject.put("text", "😶");
    emotionObject.put("emotion", "neutral");

    XiaoZhiWebsocket.send(chatId, emotionObject.toJSONString());
    String answer = null;
    if (res != null) {
      answer = res.get();
    }
    if (StringUtils.isEmpty(answer)) {
      answer = "抱歉，我暂时无法理解您的问题。";
    }
    if (answer.length() > 500)
      answer = answer.substring(0, 500);
    if (!answer.isBlank()) {
      splitSentences(answer, chatId, productId);
    }
    this.clearAudioHandlers(chatId);
  }

  @Async("taskExecutor")
  public void dealWithAudio(List<byte[]> audioList, String chatId, int productId, boolean isManual,
      String... detect) {
    if (audioList == null || chatId == null) {
      log.error("audioList或chatId为空，audioList: {}, chatId: {}", audioList, chatId);
      return;
    }
    String text = "";
    try {
      text = ASRHandler(audioList, productId, detect);
    } catch (OpusException e) {
      log.error("Opus音频解码错误: {}", e.getMessage());
      return;
    } catch (Exception e) {
      log.error("ASR提取失败！错误：{}", e.getMessage());
      return;
    }
    // 如果ASR处理结果为空，通知用户并返回
    if (StringUtils.isEmpty(text)) {
      this.sendForUnclearMsg(chatId);
      this.sendEndMsg(chatId);
      return;
    }
    // 如果音频太短，通知用户并返回
    if (text.equals("<|2SRT|>")) {
      // 保留最后的10帧信息
      int keepFrames = Math.min(10, audioList.size()); // 安全处理边界
      if (audioList.size() > keepFrames) {
        audioList.subList(0, audioList.size() - keepFrames).clear();
      }
      this.sendForUnclearMsg(chatId);
      this.sendEndMsg(chatId);
      return;
    }
    sendSTT(chatId, text);
    final String tCopy = text;
    XiaoZhiWebsocket.voiceContent.computeIfPresent(chatId, (k, v) -> v + tCopy);
    XiaoZhiWebsocket.voiceContent.putIfAbsent(chatId, text);
    audioList.clear();
    // 到这里，voiceContent不可能为空
    if (showThinking)
      sendWhenThinking(chatId);
    // 下面是TTS的部分
    String voiceContent = XiaoZhiWebsocket.voiceContent.get(chatId);
    CompletableFuture<String> res = null;
    // 首先获取router的结果
    if (router != null) {
      res = CompletableFuture.supplyAsync(
          () -> router.response(voiceContent, chatId, productId),
          routerExecutor);
    }
    boolean isStreamRsp = res != null && !res.isDone() ||
        Router.queueMap.containsKey(chatId) && Router.queueMap.get(chatId) != null;
    if (isStreamRsp) {
      try {
        this.handlerStreamRsp(chatId, productId, isManual, res);
        return;
      } catch (InterruptedException e) {
        log.error("音频处理线程出错", e);
      }
    }
    // 非流式的处理方法
    try {
      handlerSyncRsp(chatId, productId, res);
    } catch (Exception e) {
      log.error("音频处理出错", e);
    }
  }

  @PreDestroy
  public void shutdownExecutors() {
    routerExecutor.close();
  }

  private List<byte[]> snapshotAudioList(List<byte[]> audioList) {
    synchronized (audioList) {
      return new ArrayList<>(audioList);
    }
  }

  private void clearAudioList(List<byte[]> audioList) {
    synchronized (audioList) {
      audioList.clear();
    }
  }

  private void trimAudioList(List<byte[]> audioList, int keepFrames) {
    synchronized (audioList) {
      int safeKeepFrames = Math.max(0, Math.min(keepFrames, audioList.size()));
      if (audioList.size() > safeKeepFrames) {
        audioList.subList(0, audioList.size() - safeKeepFrames).clear();
      }
    }
  }

  @Async("taskExecutor")
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
            log.debug("产品 {} 使用数据库配置的ASR: {}", productId, asrName);
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
