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
package top.rslly.iot.utility;

import java.util.HashMap;
import java.util.Map;

public class EmotionManager {
  private static final Map<String, String> EMOTIONS = new HashMap<>();

  static {
    EMOTIONS.put("neutral", "😶");
    EMOTIONS.put("happy", "🙂");
    EMOTIONS.put("laughing", "😆");
    EMOTIONS.put("funny", "😂");
    EMOTIONS.put("sad", "😔");
    EMOTIONS.put("angry", "😠");
    EMOTIONS.put("crying", "😭");
    EMOTIONS.put("loving", "😍");
    EMOTIONS.put("embarrassed", "😳");
    EMOTIONS.put("surprised", "😲");
    EMOTIONS.put("shocked", "😱");
    EMOTIONS.put("thinking", "🤔");
    EMOTIONS.put("winking", "😉");
    EMOTIONS.put("cool", "😎");
    EMOTIONS.put("relaxed", "😌");
    EMOTIONS.put("delicious", "🤤");
    EMOTIONS.put("kissy", "😘");
    EMOTIONS.put("confident", "😏");
    EMOTIONS.put("sleepy", "😴");
    EMOTIONS.put("silly", "😜");
    EMOTIONS.put("confused", "🙄");
  }

  // 获取当前表情的公共方法
  public static String getCurrentEmotion(String currentEmotion) {
    return EMOTIONS.getOrDefault(currentEmotion, "😶");
  }
}
