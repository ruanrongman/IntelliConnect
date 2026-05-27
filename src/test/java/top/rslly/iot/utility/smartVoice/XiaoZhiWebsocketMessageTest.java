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

import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class XiaoZhiWebsocketMessageTest {

  @Test
  void buildTtsSentenceStartMessageEscapesText() {
    String text = "Windows \"什么时候\" 才会稳定？路径 C:\\temp\\a\n中文引号“也保留”";

    var json = JSON.parseObject(XiaoZhiWebsocket.buildTtsSentenceStartMessage(text));

    Assertions.assertEquals("tts", json.getString("type"));
    Assertions.assertEquals("sentence_start", json.getString("state"));
    Assertions.assertEquals(text, json.getString("text"));
  }

  @Test
  void buildSttMessageEscapesText() {
    String text = "识别到：\"测试\"\\换行\n“中文引号”";

    var json = JSON.parseObject(XiaoZhiWebsocket.buildSttMessage(text));

    Assertions.assertEquals("stt", json.getString("type"));
    Assertions.assertEquals(text, json.getString("text"));
  }
}
