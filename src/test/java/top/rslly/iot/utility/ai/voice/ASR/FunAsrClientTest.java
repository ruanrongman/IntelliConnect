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
package top.rslly.iot.utility.ai.voice.ASR;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.fastjson.JSONObject;
import jakarta.websocket.MessageHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class FunAsrClientTest {

  @Test
  void tomcatResolvesMessageHandlersAsStringHandlers() throws Exception {
    FunAsrClient client = new FunAsrClient();

    assertEquals(String.class,
        resolveTomcatMessageType(client.createOfflineMessageHandler(new CompletableFuture<>())));
    assertEquals(String.class,
        resolveTomcatMessageType(client.createStreamingMessageHandler(new CompletableFuture<>())));
  }

  @Test
  void streamingConfigurationUsesTwoPassPcmProtocol() {
    FunAsrClient client = new FunAsrClient();
    ReflectionTestUtils.setField(client, "itn", false);

    Map<String, Object> configuration = client.buildStreamingConfiguration("chat_round_7");

    assertEquals("2pass", configuration.get("mode"));
    assertEquals("pcm", configuration.get("wav_format"));
    assertEquals(List.of(5, 10, 5), configuration.get("chunk_size"));
    assertEquals(10, configuration.get("chunk_interval"));
    assertEquals("chat_round_7", configuration.get("wav_name"));
    assertEquals(Boolean.TRUE, configuration.get("is_speaking"));
    assertEquals(Boolean.FALSE, configuration.get("itn"));
  }

  @Test
  void twoPassOnlineResultIsNotFinal() {
    FunAsrClient client = new FunAsrClient();
    JSONObject message = new JSONObject();
    message.put("mode", "2pass-online");
    message.put("is_final", true);

    assertFalse(client.isFinalStreamingMessage(message));
  }

  @Test
  void twoPassOfflineResultIsFinalEvenWhenFlagIsFalse() {
    FunAsrClient client = new FunAsrClient();
    JSONObject message = new JSONObject();
    message.put("mode", "2pass-offline");
    message.put("is_final", false);

    assertTrue(client.isFinalStreamingMessage(message));
  }

  @Test
  void explicitFinalFlagRemainsCompatible() {
    FunAsrClient client = new FunAsrClient();
    JSONObject message = new JSONObject();
    message.put("mode", "offline");
    message.put("is_final", true);

    assertTrue(client.isFinalStreamingMessage(message));
  }

  private Class<?> resolveTomcatMessageType(MessageHandler handler) throws Exception {
    Class<?> utilClass = Class.forName("org.apache.tomcat.websocket.Util");
    Method getMessageType = utilClass.getDeclaredMethod("getMessageType", MessageHandler.class);
    getMessageType.setAccessible(true);
    return (Class<?>) getMessageType.invoke(null, handler);
  }
}
