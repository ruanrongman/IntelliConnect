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

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.rslly.iot.utility.ai.voice.TTS.Text2audio;
import top.rslly.iot.utility.ai.voice.TTS.TtsServiceFactory;
import top.rslly.iot.utility.smartVoice.XiaoZhiWebsocket;

import java.io.IOException;
import java.util.*;

@Component
@Data
@Slf4j
public class GoodByeTool implements BaseTool<String> {
  @Autowired
  private TtsServiceFactory ttsServiceFactory;

  private String name = "goodByeTool";
  private String description = """
      This tool is used to say Goodbye with people
      Args: user question(str)
      """;
  private static final List<String> GOODBYE_MESSAGES = Arrays.asList(
      "溜了溜了！",
      "闪人！",
      "我跑路啦~",
      "拜拜，下次继续搞事情！",
      "撤退撤退！",
      "嘿嘿，我先走一步~",
      "冲冲冲，下次见！",
      "哼，本宝宝要走了！",
      "嘻嘻，886！",
      "溜达溜达，回见！",
      "嗖一下就消失啦~",
      "哒哒哒，人家要跑了！",
      "嘿呀，闪现走人！",
      "咻～下次再来玩！",
      "嘿嘿嘿，告辞！");

  private static final Random RANDOM = new Random();

  /**
   * 随机获取一句告别语
   * 
   * @return 随机选择的告别语
   */
  public static String getRandomGoodbyeMessage() {
    int index = RANDOM.nextInt(GOODBYE_MESSAGES.size());
    return GOODBYE_MESSAGES.get(index);
  }

  @Override
  public String run(String question) {
    return null;
  }

  @Override
  public String run(String question, Map<String, Object> globalMessage) {
    int productId = (int) globalMessage.get("productId");
    String chatId = (String) globalMessage.get("chatId");
    if (XiaoZhiWebsocket.clients.containsKey(chatId)) {
      try {
        var session = XiaoZhiWebsocket.clients.get(chatId);
        String message = getRandomGoodbyeMessage();
        session.getBasicRemote().sendText("""
            {
              "type": "tts",
              "state": "start"
            }""");
        session.getBasicRemote()
            .sendText("{\"type\": \"tts\", \"state\": \"sentence_start\", \"text\": \""
                + message + "\"}");
        ttsServiceFactory.websocketAudioSync(message, session, chatId, productId);
        session.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return getRandomGoodbyeMessage();
  }
}
