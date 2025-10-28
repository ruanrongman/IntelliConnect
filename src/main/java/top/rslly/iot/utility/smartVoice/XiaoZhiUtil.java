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
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import top.rslly.iot.utility.RedisUtil;
import top.rslly.iot.utility.ai.chain.Router;
import top.rslly.iot.utility.ai.mcp.McpProtocolDeal;
import top.rslly.iot.utility.ai.mcp.McpProtocolSend;
import top.rslly.iot.utility.ai.mcp.McpWebsocket;
import top.rslly.iot.utility.ai.tools.EmotionToolAsync;
import top.rslly.iot.utility.ai.voice.Audio2Text;
import top.rslly.iot.utility.ai.voice.TTS.TtsServiceFactory;
import top.rslly.iot.utility.ai.voice.concentus.OpusDecoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
public class XiaoZhiUtil {
  @Autowired
  private Audio2Text audio2Text;
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
  @Value("${ai.vision-explain-url}")
  private String visionExplainUrl;

  public void dealHello(String chatId, JSONObject helloObject, String token) throws IOException {
    boolean mcpCanUse = false;
    if (helloObject.containsKey("features")) {
      mcpCanUse = helloObject.getJSONObject("features").getBoolean("mcp");
    }
    XiaoZhiWebsocket.clients.get(chatId).getBasicRemote().sendText(
        "{\"type\":\"hello\",\"transport\":\"websocket\",\"audio_params\":{\"sample_rate\":16000}}");
    log.info("mcp...{}", mcpCanUse);
    if (mcpCanUse && !chatId.startsWith("register")) {
      XiaoZhiWebsocket.clients.get(chatId).getBasicRemote()
          .sendText(McpProtocolSend.sendInitialize(visionExplainUrl, token, false));
      XiaoZhiWebsocket.clients.get(chatId).getBasicRemote()
          .sendText(McpProtocolSend.sendToolList("", false));
    }
  }

  public void dealMcp(String chatId, JSONObject mcpObject) throws IOException {
    if (mcpObject.containsKey("payload")) {
      JSONObject payloadObject = mcpObject.getJSONObject("payload");
      if (payloadObject.containsKey("result")) {
        JSONObject resultObject = payloadObject.getJSONObject("result");
        mcpProtocolDeal.dealMcp(resultObject, McpWebsocket.DEVICE_SERVER_NAME, chatId,
            XiaoZhiWebsocket.clients.get(chatId), false);
      }
    }
  }

  public void destroyMcp(String chatId) {
    mcpProtocolDeal.destroyMcp(McpWebsocket.DEVICE_SERVER_NAME, chatId);
  }

  @Async("taskExecutor")
  public void dealWithAudio(List<byte[]> audioList, String chatId, int productId, boolean isManual)
      throws IOException {
    try {
      OpusDecoder decoder = new OpusDecoder(16000, 1);
      if (audioList.size() > 20) {
        if (!isManual) {
          XiaoZhiWebsocket.clients.get(chatId).getBasicRemote().sendText("""
              {
                "type": "tts",
                "state": "start"
              }""");
        }
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
          if (XiaoZhiWebsocket.voiceContent.containsKey(chatId)
              && XiaoZhiWebsocket.voiceContent.get(chatId).length() > 0) {
            XiaoZhiWebsocket.voiceContent.put(chatId,
                XiaoZhiWebsocket.voiceContent.get(chatId) + sentences);
          } else {
            XiaoZhiWebsocket.voiceContent.put(chatId, sentences.toString());
          }
          XiaoZhiWebsocket.clients.get(chatId).getBasicRemote()
              .sendText("{\"type\":\"stt\",\"text\":\"" + sentences + "\"}");
          audioList.clear();
        } else {
          // 保留音频数据最后10帧（直接修改原始列表）
          int keepFrames = Math.min(10, audioList.size()); // 安全处理边界
          if (audioList.size() > keepFrames) {
            audioList.subList(0, audioList.size() - keepFrames).clear();
          }
          XiaoZhiWebsocket.clients.get(chatId).getBasicRemote()
              .sendText("{\"type\":\"stt\",\"text\":\"" + "没听清楚，说太快了" + "\"}");
          XiaoZhiWebsocket.clients.get(chatId).getBasicRemote()
              .sendText("{\"type\":\"tts\",\"state\":\"stop\"}");
        }
      } else {
        XiaoZhiWebsocket.clients.get(chatId).getBasicRemote()
            .sendText("{\"type\":\"stt\",\"text\":\"" + "没听清楚，说太快了" + "\"}");
      }
      if (XiaoZhiWebsocket.voiceContent.containsKey(chatId)
          && XiaoZhiWebsocket.voiceContent.get(chatId).length() > 0) {
        XiaoZhiWebsocket.clients.get(chatId).getBasicRemote()
            .sendText("{\"type\": \"tts\", \"state\": \"sentence_start\", \"text\": \""
                + "智能助手思考中" + "\"}");
        JSONObject emotionObject = new JSONObject();
        emotionObject.put("type", "llm");
        emotionObject.put("text", "🤔");
        emotionObject.put("emotion", "thinking");
        XiaoZhiWebsocket.clients.get(chatId).getBasicRemote()
            .sendText(emotionObject.toJSONString());
        log.info("listen stop,message{}", XiaoZhiWebsocket.voiceContent.get(chatId));
        Map<String, Object> emotionMessage = new HashMap<>();
        emotionMessage.put("chatId", chatId);
        var emotionRes =
            emotionToolAsync.run(XiaoZhiWebsocket.voiceContent.get(chatId), emotionMessage);
        // 异步执行router,成功后把回复发送给前端，future 返回结果
        var res = CompletableFuture
            .supplyAsync(
                () -> router.response(XiaoZhiWebsocket.voiceContent.get(chatId), chatId,
                    productId));
        // String answer = router.response(voiceContent.get(chatId), "chatProduct" + chatId,
        // Integer.parseInt(chatId));
        String answer;
        StringBuilder answerBuilder = new StringBuilder();
        boolean emotionFlag = false;
        if (isManual) {
          XiaoZhiWebsocket.clients.get(chatId).getBasicRemote().sendText("""
              {
                "type": "tts",
                "state": "start"
              }""");
        }
        while (!res.isDone() || Router.queueMap.containsKey(chatId)
            && Router.queueMap.get(chatId).size() > 0) {
          if (emotionRes.isDone() && !emotionFlag) {
            emotionObject.put("text", emotionRes.get().get("emoji"));
            emotionObject.put("emotion", emotionRes.get().get("text"));
            log.info("emotionObject{}", emotionObject);
            XiaoZhiWebsocket.clients.get(chatId).getBasicRemote()
                .sendText(emotionObject.toJSONString());
            emotionFlag = true;
          }
          if (XiaoZhiWebsocket.isAbort.get(chatId)) {
            XiaoZhiWebsocket.haveVoice.put(chatId, false);
            XiaoZhiWebsocket.isAbort.put(chatId, false);
            Router.queueMap.remove(chatId);
            XiaoZhiWebsocket.clients.get(chatId).getBasicRemote()
                .sendText("{\"type\":\"tts\",\"state\":\"stop\"}");
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
                XiaoZhiWebsocket.clients.get(chatId).getBasicRemote()
                    .sendText(jsonObject.toJSONString());
                ttsServiceFactory.websocketAudioSync(answerBuilder.toString(),
                    XiaoZhiWebsocket.clients.get(chatId),
                    chatId, productId);
                answerBuilder.setLength(0);
              }
              XiaoZhiWebsocket.clients.get(chatId).getBasicRemote()
                  .sendText("{\"type\":\"tts\",\"state\":\"stop\"}");
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
                    || c == '!' || c == '~') {
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
                XiaoZhiWebsocket.clients.get(chatId).getBasicRemote()
                    .sendText(jsonObject.toJSONString());
                ttsServiceFactory.websocketAudioSync(answerBuilder.toString(),
                    XiaoZhiWebsocket.clients.get(chatId),
                    chatId, productId);
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
                  XiaoZhiWebsocket.clients.get(chatId).getBasicRemote()
                      .sendText("{\"type\": \"tts\", \"state\": \"sentence_start\", \"text\": \""
                          + answerBuilder + "\"}");
                  ttsServiceFactory.websocketAudioSync(answerBuilder.toString(),
                      XiaoZhiWebsocket.clients.get(chatId),
                      chatId, productId);
                  answerBuilder.setLength(0);
                }
              }
            }
          }
          // 延时，防止cpu空转
          Thread.sleep(10);
        }
        if (!emotionFlag) {
          emotionObject.put("text", "😶");
          emotionObject.put("emotion", "neutral");
          XiaoZhiWebsocket.clients.get(chatId).getBasicRemote()
              .sendText(emotionObject.toJSONString());
        }
        answer = res.get();
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
      XiaoZhiWebsocket.clients.get(chatId).getBasicRemote()
          .sendText("{\"type\":\"tts\",\"state\":\"stop\"}");
    }
  }

  public void dealDetect(String chatId, int productId) throws IOException {
    XiaoZhiWebsocket.clients.get(chatId).getBasicRemote().sendText("""
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
    ttsServiceFactory.websocketAudioSync(selectedGreeting, XiaoZhiWebsocket.clients.get(chatId),
        chatId,
        productId);
    XiaoZhiWebsocket.clients.get(chatId).getBasicRemote()
        .sendText("{\"type\":\"tts\",\"state\":\"stop\"}");
  }

  public void dealRegister(String chatId, int productId) throws IOException {
    RandomGenerator randomGenerator = new RandomGenerator("0123456789", 6);
    String code = randomGenerator.generate();
    String registerMsg = "请登录到控制面板添加设备，输入验证码" + code;
    redisUtil.set(code, XiaoZhiWebsocket.getDeviceId(XiaoZhiWebsocket.clients.get(chatId)), 60 * 5);
    XiaoZhiWebsocket.clients.get(chatId).getBasicRemote()
        .sendText("{\"type\": \"tts\", \"state\": \"sentence_start\", \"text\": \""
            + registerMsg + "\"}");
    XiaoZhiWebsocket.clients.get(chatId).getBasicRemote().sendText("""
        {
        "type": "tts",
        "state": "start"
        }""");
    ttsServiceFactory.websocketAudioSync(registerMsg, XiaoZhiWebsocket.clients.get(chatId), chatId,
        productId);
    XiaoZhiWebsocket.clients.get(chatId).getBasicRemote()
        .sendText("{\"type\":\"tts\",\"state\":\"stop\"}");
  }

  // 分割句子
  private void splitSentences(String answer, String chatId, int productId) throws IOException {
    String[] sentences = answer.split("(?<=[。？！；：])");
    for (String sentence : sentences) {
      sentence = sentence.trim();
      if (XiaoZhiWebsocket.isAbort.get(chatId)) {
        return;
      }
      if (sentence.isEmpty())
        continue;
      JSONObject jsonObject = new JSONObject();
      jsonObject.put("type", "tts");
      jsonObject.put("state", "sentence_start");
      jsonObject.put("text", sentence);
      log.info(sentence);
      XiaoZhiWebsocket.clients.get(chatId).getBasicRemote()
          .sendText(jsonObject.toJSONString());
      ttsServiceFactory.websocketAudioSync(sentence, XiaoZhiWebsocket.clients.get(chatId), chatId,
          productId);
    }
  }

}
