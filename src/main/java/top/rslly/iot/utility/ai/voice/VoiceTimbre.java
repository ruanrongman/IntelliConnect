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

import java.util.Arrays;
import java.util.stream.Stream;

@AllArgsConstructor
@Getter
public enum VoiceTimbre {
  CosyVoiceLongXiaoChun("longxiaochun", "嗓音如丝般柔滑，温暖中流淌着亲切与抚慰，恰似春风吹过心田。"), CosyVoiceLongXiaoXia(
      "longxiaoxia", "以温润磁性的声线，宛如夏日细雨，悄然滋润听者心灵，营造恬静氛围。"), CosyLongXiaoCheng("longxiaocheng",
          "深邃而稳重的声音犹如醇厚佳酿，散发出成熟魅力。"), CosyLongXiaoBai("longxiaobai",
              "以轻松亲和的声调演绎闲适日常，其嗓音如邻家女孩般亲切自然。"), CosyLongShu("longshu",
                  "以专业沉稳的播报风格传递新闻资讯，其嗓音富含权威与信赖感。"), CosyLongTong("longtong",
                      "以稚嫩的童声撒欢，像是春日里的小溪，清脆跳跃，流淌着生机勃勃的旋律。"), En_US_AnaNeural("edge-en-US-AnaNeural",
                          "英文 - 美式 - 女 (安娜)"), Ko_KR_SunHiNeural("edge-ko-KR-SunHiNeural",
                              "韩语 - 韩国 - 女 (孙Hi)"), Ru_RU_SvetlanaNeural(
                                  "edge-ru-RU-SvetlanaNeural",
                                  "俄语 - 俄语 - 女 ( Светлана)"), Ja_JP_NanamiNeural(
                                      "edge-ja-JP-NanamiNeural", "日语 - 日本 - 女 (南ami)"),
  // 新增的 Edge TTS 音色
  ZH_CN_XIAOXIAO_NEURAL("edge-zh-CN-XiaoxiaoNeural", "中文 (简体) - 普通话 - 女 (晓晓)"), ZH_CN_XIAOYI_NEURAL(
      "edge-zh-CN-XiaoyiNeural",
      "中文 (简体) - 普通话 - 女 (晓伊)"), ZH_CN_YUNJIAN_NEURAL("edge-zh-CN-YunjianNeural",
          "中文 (简体) - 普通话 - 男 (云健)"), ZH_CN_YUNXI_NEURAL("edge-zh-CN-YunxiNeural",
              "中文 (简体) - 普通话 - 男 (云希)"), ZH_CN_YUNXIA_NEURAL("edge-zh-CN-YunxiaNeural",
                  "中文 (简体) - 普通话 - 男 (云夏)"), ZH_CN_YUNYANG_NEURAL("edge-zh-CN-YunyangNeural",
                      "中文 (简体) - 普通话 - 男 (云扬)"), ZH_CN_LIAONING_XIAOBEI_NEURAL(
                          "edge-zh-CN-liaoning-XiaobeiNeural",
                          "中文 (简体) - 辽宁方言 - 女 (晓北)"), ZH_CN_SHAANXI_XIAONI_NEURAL(
                              "edge-zh-CN-shaanxi-XiaoniNeural",
                              "中文 (简体) - 陕西方言 - 女 (晓妮)"), ZH_HK_HIUGAAI_NEURAL(
                                  "edge-zh-HK-HiuGaaiNeural",
                                  "中文 (繁体) - 粤语 - 女 (HiuGaai)"), ZH_HK_HIUMAAN_NEURAL(
                                      "edge-zh-HK-HiuMaanNeural",
                                      "中文 (繁体) - 粤语 - 女 (HiuMaan)"), ZH_HK_WANLUNG_NEURAL(
                                          "edge-zh-HK-WanLungNeural",
                                          "中文 (繁体) - 粤语 - 男 (WanLung)"), ZH_TW_HSIAOCHEN_NEURAL(
                                              "edge-zh-TW-HsiaoChenNeural",
                                              "中文 (繁体) - 台湾 - 女 (晓臻)"), ZH_TW_HSIAOYU_NEURAL(
                                                  "edge-zh-TW-HsiaoYuNeural",
                                                  "中文 (繁体) - 台湾 - 女 (晓雨)"), ZH_TW_YUNJHE_NEURAL(
                                                      "edge-zh-TW-YunJheNeural",
                                                      "中文 (繁体) - 台湾 - 男 (云哲)");


  private String timbre;
  private String timbreDescription;

  public static Stream<VoiceTimbre> stream() {
    return Stream.of(VoiceTimbre.values());
  }

  /**
   * 校验声音音色是否存在
   * 
   * @param voice 音色名称
   * @return 是否存在
   */
  public static boolean isValidVoice(String voice) {
    if (voice == null || voice.trim().isEmpty()) {
      return false;
    }
    boolean result = Arrays.stream(values())
        .anyMatch(timbre -> timbre.getTimbre().equals(voice.trim()));
    return result;
  }
}
