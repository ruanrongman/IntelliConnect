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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import top.rslly.iot.models.AdminConfigEntity;
import top.rslly.iot.services.AdminConfigServiceImpl;
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

import jakarta.websocket.Session;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
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
  @Value("${ai.vision-explain-url}")
  private String visionExplainUrl;
  @Value("${ai.tts.skip-tool-prefix:true}")
  private boolean skipToolPrefix;
  @Value("${ai.detectRandom:false}")
  private boolean detectRandom;
  @Value("${ai.showThinking:false}")
  private boolean showThinking;

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

  @Async("taskExecutor")
  public void dealWithAudio(List<byte[]> audioList, String chatId, int productId, boolean isManual,
      String... detect)
      throws IOException {
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

        StringBuilder sentences = new StringBuilder("");
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
          Path tempFile = Files.createTempFile("audio_", ".wav");
          Files.write(tempFile, bos.toByteArray());
          bos.close();
          if (asrServiceFactory != null) {
            var audio2Text = asrServiceFactory.getService();
            if (audio2Text != null) {
              String text = audio2Text.getTextRealtime(tempFile.toFile(), 16000, "pcm");
              log.info("text{}", text);
              sentences.append(text);
            }
          }
        } else {
          sentences.append(detect[0]);
        }
        if (sentences.length() > 0) {
          if (XiaoZhiWebsocket.voiceContent.containsKey(chatId)
              && XiaoZhiWebsocket.voiceContent.get(chatId) != null
              && XiaoZhiWebsocket.voiceContent.get(chatId).length() > 0) {
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
          && XiaoZhiWebsocket.voiceContent.get(chatId).length() > 0) {
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
                  Executors.newVirtualThreadPerTaskExecutor());
        }
        // String answer = router.response(voiceContent.get(chatId), "chatProduct" + chatId,
        // Integer.parseInt(chatId));
        String answer = null;
        StringBuilder answerBuilder = new StringBuilder();
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
            && Router.queueMap.get(chatId).size() > 0) {
          if (!Router.queueMap.containsKey(chatId)) {
            log.warn("queueMap已被释放，停止处理: chatId={}", chatId);
            break;
          }
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
              if (answerBuilder.length() > 0) {
                // 立即发送已累积的内容
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("type", "tts");
                jsonObject.put("state", "sentence_start");
                jsonObject.put("text", answerBuilder.toString());

                session = XiaoZhiWebsocket.clients.get(chatId);
                if (session != null && session.isOpen()) {
                  session.getBasicRemote()
                      .sendText(jsonObject.toJSONString());
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
            } else if (element != null) {
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

                // 立即发送已累积的内容
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("type", "tts");
                jsonObject.put("state", "sentence_start");
                jsonObject.put("text", answerBuilder.toString());

                session = XiaoZhiWebsocket.clients.get(chatId);
                if (session != null && session.isOpen()) {
                  session.getBasicRemote()
                      .sendText(jsonObject.toJSONString());
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
        if (answer == null || answer.equals("")) {
          answer = "抱歉，我暂时无法理解您的问题。";
        }
        if (answer.length() > 500)
          answer = answer.substring(0, 500);
        splitSentences(answer, chatId, productId);
      }
    } catch (Exception e) {
      log.error("Error in dealWithAudio: {}", e.getMessage(), e);
    } finally {
      XiaoZhiWebsocket.haveVoice.put(chatId, false);
      XiaoZhiWebsocket.isAbort.put(chatId, false);
      try {
        Session session = XiaoZhiWebsocket.clients.get(chatId);
        if (session != null && session.isOpen()) {
          session.getBasicRemote().sendText("{\"type\":\"tts\",\"state\":\"stop\"}");
        }
      } catch (IOException e) {
        log.warn("Failed to send stop message to client {}: {}", chatId, e.getMessage());
      }
    }
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
}
