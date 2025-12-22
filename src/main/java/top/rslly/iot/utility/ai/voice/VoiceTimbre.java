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
  // 电话销售
  CosyVoiceLongYingXiao("cosy_v2_longyingxiao", "清甜推销女"),
  // 短视频配音
  CosyVoiceLongJiQi("cosy_v2_longjiqi", "呆萌机器人"), CosyVoiceLongHouGe("cosy_v2_longhouge",
      "经典猴哥"), CosyVoiceLongJiXin("cosy_v2_longjixin", "毒舌心机女"), CosyVoiceLongAnYue(
          "cosy_v2_longanyue", "欢脱粤语男"), CosyVoiceLongShanGe("cosy_v2_longshange",
              "原味陕北男"), CosyVoiceLongAnMin("cosy_v2_longanmin", "甜美闽南女"), CosyVoiceLongDaiYu(
                  "cosy_v2_longdaiyu",
                  "娇率才女音"), CosyVoiceLongGaoSeng("cosy_v2_longgaoseng", "得道高僧音"),
  // 语音助手
  CosyVoiceLongAnLi("cosy_v2_longanli", "利落从容女"), CosyVoiceLongAnLang("cosy_v2_longanlang",
      "清爽利落男"), CosyVoiceLongAnWen("cosy_v2_longanwen", "优雅知性女"), CosyVoiceLongAnYun(
          "cosy_v2_longanyun", "居家暖男"), CosyVoiceLongYumiV2("cosy_v2_longyumi_v2",
              "正经青年女"), CosyVoiceLongXiaoChunV2("cosy_v2_longxiaochun_v2",
                  "知性积极女"), CosyVoiceLongXiaoXiaV2("cosy_v2_longxiaoxia_v2", "沉稳权威女"),
  // 有声书
  CosyVoiceLongYiChen("cosy_v2_longyichen", "洒脱活力男"), CosyVoiceLongWanJun("cosy_v2_longwanjun",
      "细腻柔声女"), CosyVoiceLongLaoBo("cosy_v2_longlaobo", "沧桑岁月爷"), CosyVoiceLongLaoYi(
          "cosy_v2_longlaoyi", "烟火从容阿姨"), CosyVoiceLongBaiZhi("cosy_v2_longbaizhi",
              "睿气旁白女"), CosyVoiceLongSanShu("cosy_v2_longsanshu", "沉稳质感男"), CosyVoiceLongXiuV2(
                  "cosy_v2_longxiu_v2",
                  "博才说书男"), CosyVoiceLongMiaoV2("cosy_v2_longmiao_v2", "抑扬顿挫女"), CosyVoiceLongYueV2(
                      "cosy_v2_longyue_v2", "温暖磁性女"), CosyVoiceLongNanV2("cosy_v2_longnan_v2",
                          "睿智青年男"), CosyVoiceLongYuanV2("cosy_v2_longyuan_v2", "温暖治愈女"),
  // 社交陪伴
  CosyVoiceLongAnQin("cosy_v2_longanqin", "亲和活泼女"), CosyVoiceLongAnYa("cosy_v2_longanya",
      "高雅气质女"), CosyVoiceLongAnShuo("cosy_v2_longanshuo", "干净清爽男"), CosyVoiceLongAnLing(
          "cosy_v2_longanling",
          "思维灵动女"), CosyVoiceLongAnZhi("cosy_v2_longanzhi", "睿智轻熟男"), CosyVoiceLongAnRou(
              "cosy_v2_longanrou",
              "温柔闺蜜女"), CosyVoiceLongQiangV2("cosy_v2_longqiang_v2", "浪漫风情女"), CosyVoiceLongHanV2(
                  "cosy_v2_longhan_v2",
                  "温暖痴情男"), CosyVoiceLongXingV2("cosy_v2_longxing_v2", "温婉邻家女"), CosyVoiceLongHuaV2(
                      "cosy_v2_longhua_v2", "元气甜美女"), CosyVoiceLongWanV2("cosy_v2_longwan_v2",
                          "积极知性女"), CosyVoiceLongChengV2("cosy_v2_longcheng_v2",
                              "智慧青年男"), CosyVoiceLongFeiFeiV2("cosy_v2_longfeifei_v2",
                                  "甜美娇气女"), CosyVoiceLongXiaoChengV2("cosy_v2_longxiaocheng_v2",
                                      "磁性低音男"), CosyVoiceLongZheV2("cosy_v2_longzhe_v2",
                                          "呆板大暖男"), CosyVoiceLongYanV2("cosy_v2_longyan_v2",
                                              "温暖春风女"), CosyVoiceLongTianV2("cosy_v2_longtian_v2",
                                                  "磁性理智男"), CosyVoiceLongZeV2("cosy_v2_longze_v2",
                                                      "温暖元气男"), CosyVoiceLongShaoV2(
                                                          "cosy_v2_longshao_v2",
                                                          "积极向上男"), CosyVoiceLongHaoV2(
                                                              "cosy_v2_longhao_v2",
                                                              "多情忧郁男"), CosyVoiceKabuleshenV2(
                                                                  "cosy_v2_kabuleshen_v2", "实力歌手男"),
  // 童声（标杆音色）
  CosyVoiceLongHuHu("cosy_v2_longhuhu", "天真烂漫女童"),
  // 消费电子-教育培训
  CosyVoiceLongAnPei("cosy_v2_longanpei", "青少年教师女"),
  // 消费电子-儿童陪伴
  CosyVoiceLongWangWang("cosy_v2_longwangwang", "台湾少年音"), CosyVoiceLongPaoPao("cosy_v2_longpaopao",
      "飞天泡泡音"),
  // 消费电子-儿童有声书
  CosyVoiceLongShanShan("cosy_v2_longshanshan", "戏剧化童声"), CosyVoiceLongNiuNiu("cosy_v2_longniuniu",
      "阳光男童声"),
  // 客服
  CosyVoiceLongYingMu("cosy_v2_longyingmu", "优雅知性女"), CosyVoiceLongYingXun("cosy_v2_longyingxun",
      "年轻青涩男"), CosyVoiceLongYingCui("cosy_v2_longyingcui", "严肃催收男"), CosyVoiceLongYingDa(
          "cosy_v2_longyingda", "开朗高音女"), CosyVoiceLongYingJing("cosy_v2_longyingjing",
              "低调冷静女"), CosyVoiceLongYingYan("cosy_v2_longyingyan", "义正严辞女"), CosyVoiceLongYingTian(
                  "cosy_v2_longyingtian", "温柔甜美女"), CosyVoiceLongYingBing("cosy_v2_longyingbing",
                      "尖锐强势女"), CosyVoiceLongYingTao("cosy_v2_longyingtao",
                          "温柔淡定女"), CosyVoiceLongYingLing("cosy_v2_longyingling", "温和共情女"),
  // 直播带货
  CosyVoiceLongAnRan("cosy_v2_longanran", "活泼质感女"), CosyVoiceLongAnXuan("cosy_v2_longanxuan",
      "经典直播女"), CosyVoiceLongAnChong("cosy_v2_longanchong",
          "激情推销男"), CosyVoiceLongAnPing("cosy_v2_longanping", "高亢直播女"),
  // 童声
  CosyVoiceLongJieLiDouV2("cosy_v2_longjielidou_v2", "阳光顽皮男"), CosyVoiceLongLingV2(
      "cosy_v2_longling_v2", "稚气呆板女"), CosyVoiceLongKeV2("cosy_v2_longke_v2",
          "懵懂乖乖女"), CosyVoiceLongXianV2("cosy_v2_longxian_v2", "豪放可爱女"),
  // 方言
  CosyVoiceLongLaoTieV2("cosy_v2_longlaotie_v2", "东北直率男"), CosyVoiceLongJiaYiV2(
      "cosy_v2_longjiayi_v2", "知性粤语女"), CosyVoiceLongTaoV2("cosy_v2_longtao_v2", "积极粤语女"),
  // 诗词朗诵
  CosyVoiceLongFeiV2("cosy_v2_longfei_v2", "热血磁性男"), CosyVoiceLibaiV2("cosy_v2_libai_v2",
      "古代诗仙男"), CosyVoiceLongJinV2("cosy_v2_longjin_v2", "优雅温润男"),
  // 新闻播报
  CosyVoiceLongShuV2("cosy_v2_longshu_v2", "沉稳青年男"), CosyVoiceLoongBellaV2("cosy_v2_loongbella_v2",
      "精准干练女"), CosyVoiceLongShuoV2("cosy_v2_longshuo_v2", "博才干练男"), CosyVoiceLongXiaoBaiV2(
          "cosy_v2_longxiaobai_v2", "沉稳播报女"), CosyVoiceLongJingV2("cosy_v2_longjing_v2",
              "典型播音女"), CosyVoiceLoongStellaV2("cosy_v2_loongstella_v2", "飒爽利落女"),
  // 出海营销
  CosyVoiceLoongYuunaV2("cosy_v2_loongyuuna_v2", "元气霓虹女"), CosyVoiceLoongYuumaV2(
      "cosy_v2_loongyuuma_v2", "干练霓虹男"), CosyVoiceLoongJihunV2("cosy_v2_loongjihun_v2",
          "阳光韩国男"), CosyVoiceLoongEvaV2("cosy_v2_loongeva_v2", "知性英文女"), CosyVoiceLoongBrianV2(
              "cosy_v2_loongbrian_v2",
              "沉稳英文男"), CosyVoiceLoongLunaV2("cosy_v2_loongluna_v2", "英式英文女"), CosyVoiceLoongLucaV2(
                  "cosy_v2_loongluca_v2", "英式英文男"), CosyVoiceLoongEmilyV2("cosy_v2_loongemily_v2",
                      "英式英文女"), CosyVoiceLoongEricV2("cosy_v2_loongeric_v2",
                          "英式英文男"), CosyVoiceLoongAbbyV2("cosy_v2_loongabby_v2",
                              "美式英文女"), CosyVoiceLoongAnnieV2("cosy_v2_loongannie_v2",
                                  "美式英文女"), CosyVoiceLoongAndyV2("cosy_v2_loongandy_v2",
                                      "美式英文男"), CosyVoiceLoongAvaV2("cosy_v2_loongava_v2",
                                          "美式英文女"), CosyVoiceLoongBethV2("cosy_v2_loongbeth_v2",
                                              "美式英文女"), CosyVoiceLoongBettyV2(
                                                  "cosy_v2_loongbetty_v2",
                                                  "美式英文女"), CosyVoiceLoongCindyV2(
                                                      "cosy_v2_loongcindy_v2",
                                                      "美式英文女"), CosyVoiceLoongCallyV2(
                                                          "cosy_v2_loongcally_v2",
                                                          "美式英文女"), CosyVoiceLoongDavidV2(
                                                              "cosy_v2_loongdavid_v2",
                                                              "美式英文男"), CosyVoiceLoongDonnaV2(
                                                                  "cosy_v2_loongdonna_v2",
                                                                  "美式英文女"), CosyVoiceLoongKyongV2(
                                                                      "cosy_v2_loongkyong_v2",
                                                                      "韩语女"), CosyVoiceLoongTomokaV2(
                                                                          "cosy_v2_loongtomoka_v2",
                                                                          "日语女"), CosyVoiceLoongTomoyaV2(
                                                                              "cosy_v2_loongtomoya_v2",
                                                                              "日语男"),

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
