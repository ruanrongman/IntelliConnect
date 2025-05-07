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
package top.rslly.iot.utility.ai.voice;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.stream.Stream;

@AllArgsConstructor
@Getter
public enum VoiceTimbre {
  CosyVoiceLongXiaoChun("longxiaochun", "嗓音如丝般柔滑，温暖中流淌着亲切与抚慰，恰似春风吹过心田。"), CosyVoiceLongXiaoXia(
      "longxiaoxia", "以温润磁性的声线，宛如夏日细雨，悄然滋润听者心灵，营造恬静氛围。"), CosyLongXiaoCheng("longxiaocheng",
          "深邃而稳重的声音犹如醇厚佳酿，散发出成熟魅力。"), CosyLongXiaoBai("longxiaobai",
              "以轻松亲和的声调演绎闲适日常，其嗓音如邻家女孩般亲切自然。"), CosyLongShu("longshu",
                  "以专业沉稳的播报风格传递新闻资讯，其嗓音富含权威与信赖感。"), CosyLongTong("longtong",
                      "以稚嫩的童声撒欢，像是春日里的小溪，清脆跳跃，流淌着生机勃勃的旋律。");

  private String timbre;
  private String timbreDescription;

  public static Stream<VoiceTimbre> stream() {
    return Stream.of(VoiceTimbre.values());
  }
}
