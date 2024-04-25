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
import com.zhipu.oapi.service.v4.model.ChatMessage;
import com.zhipu.oapi.service.v4.model.ChatMessageRole;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.rslly.iot.utility.HttpRequestUtils;
import top.rslly.iot.utility.ai.Glm;
import top.rslly.iot.utility.ai.IcAiException;
import top.rslly.iot.utility.ai.Prompt;

import java.io.IOException;
import java.util.*;

@Data
@Component
public class MusicTool implements BaseTool<Map<String, String>> {
  @Autowired
  private Prompt prompt;
  @Autowired
  private HttpRequestUtils httpRequestUtils;
  private String name = "musicTool";
  private String description = """
      A tool for playing music.
      Args: Music or Singer Name, or both.(str)
      """;

  @Override
  public Map<String, String> run(String question) {
    Glm glm = new Glm();
    List<ChatMessage> messages = new ArrayList<>();
    Map<String, String> responseMap = new HashMap<>();

    ChatMessage systemMessage =
        new ChatMessage(ChatMessageRole.SYSTEM.value(), prompt.getMusicTool());
    ChatMessage userMessage = new ChatMessage(ChatMessageRole.USER.value(), question);
    messages.add(systemMessage);
    messages.add(userMessage);
    var obj = glm.jsonChat(question, messages, true).getJSONObject("action");
    var answer = (String) obj.get("answer");
    responseMap.put("answer", answer);
    responseMap.put("url", "");
    try {
      var url = process_llm_result(obj);
      responseMap.put("url", url);
      return responseMap;
    } catch (Exception e) {
      // e.printStackTrace();
      return responseMap;
    }
  }

  @Override
  public Map<String, String> run(String question, int productId) {
    return this.run(question);
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
