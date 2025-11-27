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
package top.rslly.iot.utility.ai.voice.TTS;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.rslly.iot.services.agent.ProductRoleServiceImpl;
import top.rslly.iot.services.agent.ProductVoiceDiyServiceImpl;

import javax.websocket.Session;

@Component
public class TtsServiceFactory {
  @Autowired
  private TtsService text2audio;
  @Autowired
  private EdgeTTs edgeTTs;
  @Autowired
  private ProductRoleServiceImpl productRoleService;
  @Autowired
  private ProductVoiceDiyServiceImpl productVoiceDiyService;

  public TtsService getTtsService(String type) {
    if (type.equals("edge")) {
      return edgeTTs;
    }
    return text2audio;
  }

  public void websocketAudioSync(String text, Session session, String chatId, int productId) {
    String provider = "dashscope";
    // 语音音调 (0.5-2.0)
    float pitch = 1.0f;

    // 语音语速 (0.5-2.0)
    float speed = 1.0f;
    String voice = null;
    try {
      var roles = productRoleService.findAllByProductId(productId);
      if (!roles.isEmpty() && roles.get(0).getVoice() != null) {
        voice = roles.get(0).getVoice();
        if (voice.startsWith("edge-")) {
          provider = "edge";
          voice = voice.substring(5);
        }
      }
      var voiceDiyEntityList = productVoiceDiyService.findAllByProductId(productId);
      if (!voiceDiyEntityList.isEmpty()) {
        pitch = Float.parseFloat(voiceDiyEntityList.get(0).getPitch());
        speed = Float.parseFloat(voiceDiyEntityList.get(0).getSpeed());
      }
    } catch (Exception ignored) {
    }
    TtsService ttsService = getTtsService(provider);
    ttsService.websocketAudioSync(text, pitch, speed, session, chatId, voice);
  }
}
