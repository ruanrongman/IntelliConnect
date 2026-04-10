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
package top.rslly.iot.utility.ai.tools;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.rslly.iot.services.agent.LlmProviderInformationServiceImpl;
import top.rslly.iot.services.agent.ProductLlmModelServiceImpl;
import top.rslly.iot.utility.HttpRequestUtils;
import top.rslly.iot.utility.ai.IcAiException;
import top.rslly.iot.utility.ai.LlmDiyUtility;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.ModelMessageRole;
import top.rslly.iot.utility.ai.llm.LLM;
import top.rslly.iot.utility.ai.llm.LLMFactory;
import top.rslly.iot.utility.ai.prompts.MusicToolPrompt;
import top.rslly.iot.utility.ai.voice.TTS.TtsServiceFactory;
import top.rslly.iot.utility.smartVoice.XiaoZhiWebsocket;

import java.io.IOException;
import java.util.*;

@Data
@Component
@Slf4j
public class MusicTool implements BaseTool<Map<String, String>> {
  @Autowired
  private MusicToolPrompt musicToolPrompt;
  @Autowired
  private HttpRequestUtils httpRequestUtils;
  @Autowired
  private LlmDiyUtility llmDiyUtility;
  @Autowired
  private TtsServiceFactory ttsServiceFactory;
  @Value("${ai.musicTool-llm}")
  private String llmName;
  @Value("${ai.music.path:}")
  private String musicPath;
  private String name = "musicTool";
  private String description = """
      A tool for playing music.
      Args: Requests from users related to music(str)
      """;

  @Override
  public Map<String, String> run(String question) {
    // For backward compatibility, create a minimal globalMessage with default productId
    Map<String, Object> globalMessage = new HashMap<>();
    globalMessage.put("productId", 0); // Default productId
    return this.run(question, globalMessage);
  }

  @Override
  public Map<String, String> run(String question, Map<String, Object> globalMessage) {
    int productId = (int) globalMessage.get("productId");
    String chatId = (String) globalMessage.get("chatId");
    LLM llm = llmDiyUtility.getDiyLlm(productId, llmName, "3");
    List<ModelMessage> messages = new ArrayList<>();
    Map<String, String> responseMap = new HashMap<>();

    ModelMessage systemMessage =
        new ModelMessage(ModelMessageRole.SYSTEM.value(), musicToolPrompt.getMusicTool());
    ModelMessage userMessage = new ModelMessage(ModelMessageRole.USER.value(), question);
    messages.add(systemMessage);
    messages.add(userMessage);
    var obj = llm.jsonChat(question, messages, true).getJSONObject("action");
    var answer = (String) obj.get("answer");
    responseMap.put("answer", answer);
    responseMap.put("url", "");

    // 检查是否是小智设备连接
    boolean isXiaoZhiDevice = chatId != null && XiaoZhiWebsocket.clients.containsKey(chatId);

    try {
      if (isXiaoZhiDevice && musicPath != null && !musicPath.isEmpty()) {
        // 小智设备：使用本地音乐文件夹播放
        String singer = obj.getString("singer");
        String musicName = obj.getString("music");
        if (musicName != null && !musicName.isEmpty()) {
          // 异步播放音乐
          MusicPlayer player = new MusicPlayer(chatId, productId, musicName, singer,
              musicPath, ttsServiceFactory);
          Thread.ofVirtual().start(player::play);
          // 返回空前缀，避免TTS重复播报
          responseMap.put("answer", "");
        }
        return responseMap;
      } else {
        // 非小智设备或未配置音乐路径：使用网易云
        var url = process_llm_result(obj);
        responseMap.put("url", url);
        return responseMap;
      }
    } catch (Exception e) {
      log.error("MusicTool error: {}", e.getMessage(), e);
      return responseMap;
    }
  }

  private String process_llm_result(JSONObject llmObject) throws IOException, IcAiException {
    if (llmObject.get("code").equals("200") || llmObject.get("code").equals(200)) {
      String singer = llmObject.getString("singer");
      String music_name = llmObject.getString("music");
      String question;
      if (singer != null)
        question = singer + music_name;
      else
        question = music_name;
      if (question == null)
        question = "好听的歌";
      var musicSearch = httpRequestUtils
          .httpGet("https://music.163.com/api/search/get/web?csrf_token=hlpretag=&hlposttag=&s="
              + question + "&type=1&offset=0&total=true&limit=2");
      String music_message = Objects.requireNonNull(musicSearch.body()).string();
      // System.out.println(music_message);
      JSONObject jsonObject = JSON.parseObject(music_message);
      var songsArray = jsonObject.getJSONObject("result").getJSONArray("songs");
      JSONObject song = (JSONObject) songsArray.get(0);
      // return "https://music.163.com/song/media/outer/url?id=" + song.get("id");
      return "https://music.163.com/#/song?id=" + song.get("id");
    } else
      throw new IcAiException("llm response error");
  }

}
