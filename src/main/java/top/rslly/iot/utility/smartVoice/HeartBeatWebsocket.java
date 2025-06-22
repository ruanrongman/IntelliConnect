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

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;

@Component
@Slf4j
public class HeartBeatWebsocket {
  @Scheduled(initialDelay = 1000, fixedDelay = 1000 * 5)
  public void task() {
    for (String key : Websocket.clients.keySet()) {
      try {
        Websocket.clients.get(key).getBasicRemote()
            .sendPing(ByteBuffer.wrap(new byte[] {0x00, 0x00, 0x00, 0x00}));
        // log.info("心跳成功{}",key);
      } catch (Exception e) {
        try {
          Websocket.clients.get(key).close();
        } catch (IOException ex) {
          log.error("关闭连接失败");
        }
        Websocket.clients.remove(key);
        log.info("心跳失败{}", e.getMessage());
      }
    }
  }
}
