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
import java.util.Map;
import java.util.stream.Stream;

@AllArgsConstructor
@Getter
public enum VoiceTimbre {
  // CosyVoice v3 flash
  CosyVoiceV3FlashLongAnYang("cosy_v3_flash_longanyang",
      "龙安洋 - 阳光大男孩 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongAnHuanV3("cosy_v3_flash_longanhuan_v3",
      "龙安欢（V3） - 欢脱元气女 - 中文（普通话、广东话、东北话、河南话、湖南话、陕西话、山东话、四川话、安徽话）、英文"),
  CosyVoiceV3FlashLongAnHuan(
      "cosy_v3_flash_longanhuan",
      "龙安欢 - 欢脱元气女 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongHuHu("cosy_v3_flash_longhuhu_v3",
      "龙呼呼 - 天真烂漫女童 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongPaoPao(
      "cosy_v3_flash_longpaopao_v3",
      "龙泡泡 - 飞天泡泡音 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongJieLiDou(
      "cosy_v3_flash_longjielidou_v3",
      "龙杰力豆 - 阳光顽皮男 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongXian(
      "cosy_v3_flash_longxian_v3",
      "龙仙 - 豪放可爱女 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongLing(
      "cosy_v3_flash_longling_v3",
      "龙铃 - 稚气呆板女 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongShanShan(
      "cosy_v3_flash_longshanshan_v3",
      "龙闪闪 - 戏剧化童声 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongNiuNiu(
      "cosy_v3_flash_longniuniu_v3",
      "龙牛牛 - 阳光男童声 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongJiaXin(
      "cosy_v3_flash_longjiaxin_v3",
      "龙嘉欣 - 优雅粤语女 - 中文（粤语）、英文"),
  CosyVoiceV3FlashLongJiaYi(
      "cosy_v3_flash_longjiayi_v3",
      "龙嘉怡 - 知性粤语女 - 中文（粤语）、英文"),
  CosyVoiceV3FlashLongAnYue(
      "cosy_v3_flash_longanyue_v3",
      "龙安粤 - 欢脱粤语男 - 中文（粤语）、英文"),
  CosyVoiceV3FlashLongLaoTie(
      "cosy_v3_flash_longlaotie_v3",
      "龙老铁 - 东北直率男 - 中文（东北话）、英文"),
  CosyVoiceV3FlashLongShanGe(
      "cosy_v3_flash_longshange_v3",
      "龙陕哥 - 原味陕北男 - 中文（陕西话）、英文"),
  CosyVoiceV3FlashLongAnMin(
      "cosy_v3_flash_longanmin_v3",
      "龙安闽 - 清纯萝莉女 - 中文（闽南话）、英文"),
  CosyVoiceV3FlashLoongKyong(
      "cosy_v3_flash_loongkyong_v3",
      "loongkyong - 韩语女 - 韩语"),
  CosyVoiceV3FlashLoongRiko(
      "cosy_v3_flash_loongriko_v3",
      "Riko - 二次元霓虹女 - 日语"),
  CosyVoiceV3FlashLoongTomoka(
      "cosy_v3_flash_loongtomoka_v3",
      "loongtomoka - 日语女 - 日语"),
  CosyVoiceV3FlashLoongAbby(
      "cosy_v3_flash_loongabby_v3",
      "loongabby - 美式英文女 - 美式英语"),
  CosyVoiceV3FlashLoongAndy(
      "cosy_v3_flash_loongandy_v3",
      "loongandy - 美式英文男 - 美式英语"),
  CosyVoiceV3FlashLoongAnnie(
      "cosy_v3_flash_loongannie_v3",
      "loongannie - 美式英文女 - 美式英语"),
  CosyVoiceV3FlashLoongAva(
      "cosy_v3_flash_loongava_v3",
      "loongava - 美式英文女 - 美式英语"),
  CosyVoiceV3FlashLoongBeth(
      "cosy_v3_flash_loongbeth_v3",
      "loongbeth - 美式英文女 - 美式英语"),
  CosyVoiceV3FlashLoongBetty(
      "cosy_v3_flash_loongbetty_v3",
      "loongbetty - 美式英文女 - 美式英语"),
  CosyVoiceV3FlashLoongCally(
      "cosy_v3_flash_loongcally_v3",
      "loongcally - 美式英文女 - 美式英语"),
  CosyVoiceV3FlashLoongCindy(
      "cosy_v3_flash_loongcindy_v3",
      "loongcindy - 美式英文女 - 美式英语"),
  CosyVoiceV3FlashLoongDavid(
      "cosy_v3_flash_loongdavid_v3",
      "loongdavid - 美式英文男 - 美式英语"),
  CosyVoiceV3FlashLoongDonna(
      "cosy_v3_flash_loongdonna_v3",
      "loongdonna - 美式英文女 - 美式英语"),
  CosyVoiceV3FlashLoongEmily(
      "cosy_v3_flash_loongemily_v3",
      "loongemily - 英式英文女 - 英式英语"),
  CosyVoiceV3FlashLoongEric(
      "cosy_v3_flash_loongeric_v3",
      "loongeric - 英式英文男 - 英式英语"),
  CosyVoiceV3FlashLoongLuna(
      "cosy_v3_flash_loongluna_v3",
      "loongluna - 英式英文女 - 英式英语"),
  CosyVoiceV3FlashLoongLuca(
      "cosy_v3_flash_loongluca_v3",
      "loongluca - 英式英文男 - 英式英语"),
  CosyVoiceV3FlashLoongTomoya(
      "cosy_v3_flash_loongtomoya_v3",
      "loongtomoya - 日语男 - 日语"),
  CosyVoiceV3FlashLoongYuuna(
      "cosy_v3_flash_loongyuuna_v3",
      "Yuuna - 日语女 - 日语"),
  CosyVoiceV3FlashLoongYuuma(
      "cosy_v3_flash_loongyuuma_v3",
      "Yuuma - 日语男 - 日语"),
  CosyVoiceV3FlashLoongJihun(
      "cosy_v3_flash_loongjihun_v3",
      "Jihun - 韩语男 - 韩语"),
  CosyVoiceV3FlashLoongIndah(
      "cosy_v3_flash_loongindah_v3",
      "loongindah - 印尼女 - 印尼语"),
  CosyVoiceV3FlashLongFei(
      "cosy_v3_flash_longfei_v3",
      "龙飞 - 热血磁性男 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongYingXiao(
      "cosy_v3_flash_longyingxiao_v3",
      "龙应笑 - 清甜推销女 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongYingXun(
      "cosy_v3_flash_longyingxun_v3",
      "龙应询 - 年轻青涩男 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongYingJing(
      "cosy_v3_flash_longyingjing_v3",
      "龙应静 - 低调冷静女 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongYingLing(
      "cosy_v3_flash_longyingling_v3",
      "龙应聆 - 温和共情女 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongYingTao(
      "cosy_v3_flash_longyingtao_v3",
      "龙应桃 - 温柔淡定女 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongXiaoChun(
      "cosy_v3_flash_longxiaochun_v3",
      "龙小淳 - 知性积极女 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongXiaoXia(
      "cosy_v3_flash_longxiaoxia_v3",
      "龙小夏 - 沉稳权威女 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongYumi(
      "cosy_v3_flash_longyumi_v3",
      "YUMI - 正经青年女 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongAnYun(
      "cosy_v3_flash_longanyun_v3",
      "龙安昀 - 居家暖男 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongAnWen(
      "cosy_v3_flash_longanwen_v3",
      "龙安温 - 优雅知性女 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongAnLi(
      "cosy_v3_flash_longanli_v3",
      "龙安莉 - 利落从容女 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongAnLang(
      "cosy_v3_flash_longanlang_v3",
      "龙安朗 - 清爽利落男 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongYingMu(
      "cosy_v3_flash_longyingmu_v3",
      "龙应沐 - 优雅知性女 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongAnTai(
      "cosy_v3_flash_longantai_v3",
      "龙安台 - 嗲甜台湾女 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongHua(
      "cosy_v3_flash_longhua_v3",
      "龙华 - 元气甜美女 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongCheng(
      "cosy_v3_flash_longcheng_v3",
      "龙橙 - 智慧青年男 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongZe(
      "cosy_v3_flash_longze_v3",
      "龙泽 - 温暖元气男 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongZhe(
      "cosy_v3_flash_longzhe_v3",
      "龙哲 - 呆板大暖男 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongYan(
      "cosy_v3_flash_longyan_v3",
      "龙颜 - 温暖春风女 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongXing(
      "cosy_v3_flash_longxing_v3",
      "龙星 - 温婉邻家女 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongTian(
      "cosy_v3_flash_longtian_v3",
      "龙天 - 磁性理智男 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongWan(
      "cosy_v3_flash_longwan_v3",
      "龙婉 - 细腻柔声女 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongQiang(
      "cosy_v3_flash_longqiang_v3",
      "龙嫱 - 浪漫风情女 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongFeiFei(
      "cosy_v3_flash_longfeifei_v3",
      "龙菲菲 - 甜美娇气女 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongHao(
      "cosy_v3_flash_longhao_v3",
      "龙浩 - 多情忧郁男 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongAnRou(
      "cosy_v3_flash_longanrou_v3",
      "龙安柔 - 温柔闺蜜女 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongHan(
      "cosy_v3_flash_longhan_v3",
      "龙寒 - 温暖痴情男 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongAnZhi(
      "cosy_v3_flash_longanzhi_v3",
      "龙安智 - 睿智轻熟男 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongAnLing(
      "cosy_v3_flash_longanling_v3",
      "龙安灵 - 思维灵动女 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongAnYa(
      "cosy_v3_flash_longanya_v3",
      "龙安雅 - 高雅气质女 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongAnQin(
      "cosy_v3_flash_longanqin_v3",
      "龙安亲 - 亲和活泼女 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongMiao(
      "cosy_v3_flash_longmiao_v3",
      "龙妙 - 抑扬顿挫女 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongSanShu(
      "cosy_v3_flash_longsanshu_v3",
      "龙三叔 - 沉稳质感男 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongYuan(
      "cosy_v3_flash_longyuan_v3",
      "龙媛 - 温暖治愈女 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongYue(
      "cosy_v3_flash_longyue_v3",
      "龙悦 - 温暖磁性女 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongXiu(
      "cosy_v3_flash_longxiu_v3",
      "龙修 - 博才说书男 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongNan(
      "cosy_v3_flash_longnan_v3",
      "龙楠 - 睿智青年男 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongWanJun(
      "cosy_v3_flash_longwanjun_v3",
      "龙婉君 - 细腻柔声女 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongYiChen(
      "cosy_v3_flash_longyichen_v3",
      "龙逸尘 - 洒脱活力男 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongLaoBo(
      "cosy_v3_flash_longlaobo_v3",
      "龙老伯 - 沧桑岁月爷 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongLaoYi(
      "cosy_v3_flash_longlaoyi_v3",
      "龙老姨 - 烟火从容阿姨 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongJiQi(
      "cosy_v3_flash_longjiqi_v3",
      "龙机器 - 呆萌机器人 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongHouGe(
      "cosy_v3_flash_longhouge_v3",
      "龙猴哥 - 经典猴哥 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongDaiYu(
      "cosy_v3_flash_longdaiyu_v3",
      "龙黛玉 - 娇率才女音 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongAnRan(
      "cosy_v3_flash_longanran_v3",
      "龙安燃 - 活泼质感女 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongAnXuan(
      "cosy_v3_flash_longanxuan_v3",
      "龙安宣 - 经典直播女 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongShuo(
      "cosy_v3_flash_longshuo_v3",
      "龙硕 - 博才干练男 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLongShu(
      "cosy_v3_flash_longshu_v3",
      "龙书 - 沉稳青年男 - 中文（普通话）、英文"),
  CosyVoiceV3FlashLoongBella(
      "cosy_v3_flash_loongbella_v3",
      "Bella3.0 - 精准干练女 - 中文（普通话）、英文"),
  // Legacy IDs retained for API and database compatibility; hidden from new prompts.
  CosyVoiceLongXiaoChun("longxiaochun", "兼容别名: 龙小淳"),
  CosyVoiceLongXiaoXia("longxiaoxia",
      "兼容别名: 龙小夏"),
  CosyLongXiaoCheng("longxiaocheng", "兼容别名: 龙三叔"),
  CosyLongXiaoBai("longxiaobai",
      "兼容别名: 龙星"),
  CosyLongShu("longshu", "兼容别名: 龙书"),
  CosyLongTong("longtong",
      "兼容别名: 龙呼呼"),
  En_US_AnaNeural("edge-en-US-AnaNeural",
      "英文 - 美式 - 女 (安娜)"),
  Ko_KR_SunHiNeural("edge-ko-KR-SunHiNeural",
      "韩语 - 韩国 - 女 (孙Hi)"),
  Ru_RU_SvetlanaNeural(
      "edge-ru-RU-SvetlanaNeural",
      "俄语 - 俄语 - 女 ( Светлана)"),
  Ja_JP_NanamiNeural(
      "edge-ja-JP-NanamiNeural", "日语 - 日本 - 女 (南ami)"),
  // 电话销售
  CosyVoiceLongYingXiao("cosy_v2_longyingxiao", "清甜推销女"),
  // 短视频配音
  CosyVoiceLongJiQi("cosy_v2_longjiqi", "呆萌机器人"),
  CosyVoiceLongHouGe("cosy_v2_longhouge",
      "经典猴哥"),
  CosyVoiceLongJiXin("cosy_v2_longjixin", "毒舌心机女"),
  CosyVoiceLongAnYue(
      "cosy_v2_longanyue", "欢脱粤语男"),
  CosyVoiceLongShanGe("cosy_v2_longshange",
      "原味陕北男"),
  CosyVoiceLongAnMin("cosy_v2_longanmin", "甜美闽南女"),
  CosyVoiceLongDaiYu(
      "cosy_v2_longdaiyu",
      "娇率才女音"),
  CosyVoiceLongGaoSeng("cosy_v2_longgaoseng", "得道高僧音"),
  // 语音助手
  CosyVoiceLongAnLi("cosy_v2_longanli", "利落从容女"),
  CosyVoiceLongAnLang("cosy_v2_longanlang",
      "清爽利落男"),
  CosyVoiceLongAnWen("cosy_v2_longanwen", "优雅知性女"),
  CosyVoiceLongAnYun(
      "cosy_v2_longanyun", "居家暖男"),
  CosyVoiceLongYumiV2("cosy_v2_longyumi_v2",
      "正经青年女"),
  CosyVoiceLongXiaoChunV2("cosy_v2_longxiaochun_v2",
      "知性积极女"),
  CosyVoiceLongXiaoXiaV2("cosy_v2_longxiaoxia_v2", "沉稳权威女"),
  // 有声书
  CosyVoiceLongYiChen("cosy_v2_longyichen", "洒脱活力男"),
  CosyVoiceLongWanJun("cosy_v2_longwanjun",
      "细腻柔声女"),
  CosyVoiceLongLaoBo("cosy_v2_longlaobo", "沧桑岁月爷"),
  CosyVoiceLongLaoYi(
      "cosy_v2_longlaoyi", "烟火从容阿姨"),
  CosyVoiceLongBaiZhi("cosy_v2_longbaizhi",
      "睿气旁白女"),
  CosyVoiceLongSanShu("cosy_v2_longsanshu", "沉稳质感男"),
  CosyVoiceLongXiuV2(
      "cosy_v2_longxiu_v2",
      "博才说书男"),
  CosyVoiceLongMiaoV2("cosy_v2_longmiao_v2", "抑扬顿挫女"),
  CosyVoiceLongYueV2(
      "cosy_v2_longyue_v2", "温暖磁性女"),
  CosyVoiceLongNanV2("cosy_v2_longnan_v2",
      "睿智青年男"),
  CosyVoiceLongYuanV2("cosy_v2_longyuan_v2", "温暖治愈女"),
  // 社交陪伴
  CosyVoiceLongAnQin("cosy_v2_longanqin", "亲和活泼女"),
  CosyVoiceLongAnYa("cosy_v2_longanya",
      "高雅气质女"),
  CosyVoiceLongAnShuo("cosy_v2_longanshuo", "干净清爽男"),
  CosyVoiceLongAnLing(
      "cosy_v2_longanling",
      "思维灵动女"),
  CosyVoiceLongAnZhi("cosy_v2_longanzhi", "睿智轻熟男"),
  CosyVoiceLongAnRou(
      "cosy_v2_longanrou",
      "温柔闺蜜女"),
  CosyVoiceLongQiangV2("cosy_v2_longqiang_v2", "浪漫风情女"),
  CosyVoiceLongHanV2(
      "cosy_v2_longhan_v2",
      "温暖痴情男"),
  CosyVoiceLongXingV2("cosy_v2_longxing_v2", "温婉邻家女"),
  CosyVoiceLongHuaV2(
      "cosy_v2_longhua_v2", "元气甜美女"),
  CosyVoiceLongWanV2("cosy_v2_longwan_v2",
      "积极知性女"),
  CosyVoiceLongChengV2("cosy_v2_longcheng_v2",
      "智慧青年男"),
  CosyVoiceLongFeiFeiV2("cosy_v2_longfeifei_v2",
      "甜美娇气女"),
  CosyVoiceLongXiaoChengV2("cosy_v2_longxiaocheng_v2",
      "磁性低音男"),
  CosyVoiceLongZheV2("cosy_v2_longzhe_v2",
      "呆板大暖男"),
  CosyVoiceLongYanV2("cosy_v2_longyan_v2",
      "温暖春风女"),
  CosyVoiceLongTianV2("cosy_v2_longtian_v2",
      "磁性理智男"),
  CosyVoiceLongZeV2("cosy_v2_longze_v2",
      "温暖元气男"),
  CosyVoiceLongShaoV2(
      "cosy_v2_longshao_v2",
      "积极向上男"),
  CosyVoiceLongHaoV2(
      "cosy_v2_longhao_v2",
      "多情忧郁男"),
  CosyVoiceKabuleshenV2(
      "cosy_v2_kabuleshen_v2", "实力歌手男"),
  // 童声（标杆音色）
  CosyVoiceLongHuHu("cosy_v2_longhuhu", "天真烂漫女童"),
  // 消费电子-教育培训
  CosyVoiceLongAnPei("cosy_v2_longanpei", "青少年教师女"),
  // 消费电子-儿童陪伴
  CosyVoiceLongWangWang("cosy_v2_longwangwang", "台湾少年音"),
  CosyVoiceLongPaoPao("cosy_v2_longpaopao",
      "飞天泡泡音"),
  // 消费电子-儿童有声书
  CosyVoiceLongShanShan("cosy_v2_longshanshan", "戏剧化童声"),
  CosyVoiceLongNiuNiu("cosy_v2_longniuniu",
      "阳光男童声"),
  // 客服
  CosyVoiceLongYingMu("cosy_v2_longyingmu", "优雅知性女"),
  CosyVoiceLongYingXun("cosy_v2_longyingxun",
      "年轻青涩男"),
  CosyVoiceLongYingCui("cosy_v2_longyingcui", "严肃催收男"),
  CosyVoiceLongYingDa(
      "cosy_v2_longyingda", "开朗高音女"),
  CosyVoiceLongYingJing("cosy_v2_longyingjing",
      "低调冷静女"),
  CosyVoiceLongYingYan("cosy_v2_longyingyan", "义正严辞女"),
  CosyVoiceLongYingTian(
      "cosy_v2_longyingtian", "温柔甜美女"),
  CosyVoiceLongYingBing("cosy_v2_longyingbing",
      "尖锐强势女"),
  CosyVoiceLongYingTao("cosy_v2_longyingtao",
      "温柔淡定女"),
  CosyVoiceLongYingLing("cosy_v2_longyingling", "温和共情女"),
  // 直播带货
  CosyVoiceLongAnRan("cosy_v2_longanran", "活泼质感女"),
  CosyVoiceLongAnXuan("cosy_v2_longanxuan",
      "经典直播女"),
  CosyVoiceLongAnChong("cosy_v2_longanchong",
      "激情推销男"),
  CosyVoiceLongAnPing("cosy_v2_longanping", "高亢直播女"),
  // 童声
  CosyVoiceLongJieLiDouV2("cosy_v2_longjielidou_v2", "阳光顽皮男"),
  CosyVoiceLongLingV2(
      "cosy_v2_longling_v2", "稚气呆板女"),
  CosyVoiceLongKeV2("cosy_v2_longke_v2",
      "懵懂乖乖女"),
  CosyVoiceLongXianV2("cosy_v2_longxian_v2", "豪放可爱女"),
  // 方言
  CosyVoiceLongLaoTieV2("cosy_v2_longlaotie_v2", "东北直率男"),
  CosyVoiceLongJiaYiV2(
      "cosy_v2_longjiayi_v2", "知性粤语女"),
  CosyVoiceLongTaoV2("cosy_v2_longtao_v2", "积极粤语女"),
  // 诗词朗诵
  CosyVoiceLongFeiV2("cosy_v2_longfei_v2", "热血磁性男"),
  CosyVoiceLibaiV2("cosy_v2_libai_v2",
      "古代诗仙男"),
  CosyVoiceLongJinV2("cosy_v2_longjin_v2", "优雅温润男"),
  // 新闻播报
  CosyVoiceLongShuV2("cosy_v2_longshu_v2", "沉稳青年男"),
  CosyVoiceLoongBellaV2("cosy_v2_loongbella_v2",
      "精准干练女"),
  CosyVoiceLongShuoV2("cosy_v2_longshuo_v2", "博才干练男"),
  CosyVoiceLongXiaoBaiV2(
      "cosy_v2_longxiaobai_v2", "沉稳播报女"),
  CosyVoiceLongJingV2("cosy_v2_longjing_v2",
      "典型播音女"),
  CosyVoiceLoongStellaV2("cosy_v2_loongstella_v2", "飒爽利落女"),
  // 出海营销
  CosyVoiceLoongYuunaV2("cosy_v2_loongyuuna_v2", "元气霓虹女"),
  CosyVoiceLoongYuumaV2(
      "cosy_v2_loongyuuma_v2", "干练霓虹男"),
  CosyVoiceLoongJihunV2("cosy_v2_loongjihun_v2",
      "阳光韩国男"),
  CosyVoiceLoongEvaV2("cosy_v2_loongeva_v2", "知性英文女"),
  CosyVoiceLoongBrianV2(
      "cosy_v2_loongbrian_v2",
      "沉稳英文男"),
  CosyVoiceLoongLunaV2("cosy_v2_loongluna_v2", "英式英文女"),
  CosyVoiceLoongLucaV2(
      "cosy_v2_loongluca_v2", "英式英文男"),
  CosyVoiceLoongEmilyV2("cosy_v2_loongemily_v2",
      "英式英文女"),
  CosyVoiceLoongEricV2("cosy_v2_loongeric_v2",
      "英式英文男"),
  CosyVoiceLoongAbbyV2("cosy_v2_loongabby_v2",
      "美式英文女"),
  CosyVoiceLoongAnnieV2("cosy_v2_loongannie_v2",
      "美式英文女"),
  CosyVoiceLoongAndyV2("cosy_v2_loongandy_v2",
      "美式英文男"),
  CosyVoiceLoongAvaV2("cosy_v2_loongava_v2",
      "美式英文女"),
  CosyVoiceLoongBethV2("cosy_v2_loongbeth_v2",
      "美式英文女"),
  CosyVoiceLoongBettyV2(
      "cosy_v2_loongbetty_v2",
      "美式英文女"),
  CosyVoiceLoongCindyV2(
      "cosy_v2_loongcindy_v2",
      "美式英文女"),
  CosyVoiceLoongCallyV2(
      "cosy_v2_loongcally_v2",
      "美式英文女"),
  CosyVoiceLoongDavidV2(
      "cosy_v2_loongdavid_v2",
      "美式英文男"),
  CosyVoiceLoongDonnaV2(
      "cosy_v2_loongdonna_v2",
      "美式英文女"),
  CosyVoiceLoongKyongV2(
      "cosy_v2_loongkyong_v2",
      "韩语女"),
  CosyVoiceLoongTomokaV2(
      "cosy_v2_loongtomoka_v2",
      "日语女"),
  CosyVoiceLoongTomoyaV2(
      "cosy_v2_loongtomoya_v2",
      "日语男"),

  // 新增的 Edge TTS 音色
  ZH_CN_XIAOXIAO_NEURAL("edge-zh-CN-XiaoxiaoNeural", "中文 (简体) - 普通话 - 女 (晓晓)"),
  ZH_CN_XIAOYI_NEURAL(
      "edge-zh-CN-XiaoyiNeural",
      "中文 (简体) - 普通话 - 女 (晓伊)"),
  ZH_CN_YUNJIAN_NEURAL("edge-zh-CN-YunjianNeural",
      "中文 (简体) - 普通话 - 男 (云健)"),
  ZH_CN_YUNXI_NEURAL("edge-zh-CN-YunxiNeural",
      "中文 (简体) - 普通话 - 男 (云希)"),
  ZH_CN_YUNXIA_NEURAL("edge-zh-CN-YunxiaNeural",
      "中文 (简体) - 普通话 - 男 (云夏)"),
  ZH_CN_YUNYANG_NEURAL("edge-zh-CN-YunyangNeural",
      "中文 (简体) - 普通话 - 男 (云扬)"),
  ZH_CN_LIAONING_XIAOBEI_NEURAL(
      "edge-zh-CN-liaoning-XiaobeiNeural",
      "中文 (简体) - 辽宁方言 - 女 (晓北)"),
  ZH_CN_SHAANXI_XIAONI_NEURAL(
      "edge-zh-CN-shaanxi-XiaoniNeural",
      "中文 (简体) - 陕西方言 - 女 (晓妮)"),
  ZH_HK_HIUGAAI_NEURAL(
      "edge-zh-HK-HiuGaaiNeural",
      "中文 (繁体) - 粤语 - 女 (HiuGaai)"),
  ZH_HK_HIUMAAN_NEURAL(
      "edge-zh-HK-HiuMaanNeural",
      "中文 (繁体) - 粤语 - 女 (HiuMaan)"),
  ZH_HK_WANLUNG_NEURAL(
      "edge-zh-HK-WanLungNeural",
      "中文 (繁体) - 粤语 - 男 (WanLung)"),
  ZH_TW_HSIAOCHEN_NEURAL(
      "edge-zh-TW-HsiaoChenNeural",
      "中文 (繁体) - 台湾 - 女 (晓臻)"),
  ZH_TW_HSIAOYU_NEURAL(
      "edge-zh-TW-HsiaoYuNeural",
      "中文 (繁体) - 台湾 - 女 (晓雨)"),
  ZH_TW_YUNJHE_NEURAL(
      "edge-zh-TW-YunJheNeural",
      "中文 (繁体) - 台湾 - 男 (云哲)"),

  // MiniMax TTS 系列音色 - 普通话
  MINIMAX_MANDARIN_WARM_BESTIE("minimax-Chinese (Mandarin)_Warm_Bestie",
      "MiniMax - 普通话 - 温暖闺蜜女"),
  MINIMAX_MANDARIN_NEWS_ANCHOR(
      "minimax-Chinese (Mandarin)_News_Anchor",
      "MiniMax - 普通话 - 新闻主播"),
  MINIMAX_MANDARIN_GENTLEMAN(
      "minimax-Chinese (Mandarin)_Gentleman",
      "MiniMax - 普通话 - 绅士男"),
  MINIMAX_MANDARIN_SWEET_LADY(
      "minimax-Chinese (Mandarin)_Sweet_Lady",
      "MiniMax - 普通话 - 甜美女士"),
  MINIMAX_MANDARIN_MALE_ANNOUNCER(
      "minimax-Chinese (Mandarin)_Male_Announcer",
      "MiniMax - 普通话 - 男播音员"),
  MINIMAX_MANDARIN_RELIABLE_EXECUTIVE(
      "minimax-Chinese (Mandarin)_Reliable_Executive",
      "MiniMax - 普通话 - 可靠高管男"),
  MINIMAX_MANDARIN_HK_FLIGHT_ATTENDANT(
      "minimax-Chinese (Mandarin)_HK_Flight_Attendant",
      "MiniMax - 普通话 - 香港空乘"),

  // MiniMax TTS 系列音色 - 粤语
  MINIMAX_CANTONESE_FEMALE_HOST("minimax-Cantonese_ProfessionalHost (F)",
      "MiniMax - 粤语 - 专业女主持"),
  MINIMAX_CANTONESE_GENTLE_LADY("minimax-Cantonese_GentleLady",
      "MiniMax - 粤语 - 温柔女士"),
  MINIMAX_CANTONESE_MALE_HOST(
      "minimax-Cantonese_ProfessionalHost (M)",
      "MiniMax - 粤语 - 专业男主持"),
  MINIMAX_CANTONESE_CUTE_GIRL("minimax-Cantonese_CuteGirl",
      "MiniMax - 粤语 - 可爱女孩"),
  MINIMAX_CANTONESE_PLAYFUL_MAN(
      "minimax-Cantonese_PlayfulMan",
      "MiniMax - 粤语 - 顽皮男"),
  MINIMAX_CANTONESE_KIND_WOMAN(
      "minimax-Cantonese_KindWoman",
      "MiniMax - 粤语 - 善良女士");


  private String timbre;
  private String timbreDescription;

  public static Stream<VoiceTimbre> stream() {
    return Stream.of(VoiceTimbre.values()).filter(timbre -> !isLegacyVoice(timbre.getTimbre()));
  }


  private static final Map<String, String> LEGACY_V3_VOICE_MAP = Map.of(
      "longxiaochun", "longxiaochun_v3",
      "longxiaoxia", "longxiaoxia_v3",
      "longxiaocheng", "longsanshu_v3",
      "longxiaobai", "longxing_v3",
      "longshu", "longshu_v3",
      "longtong", "longhuhu_v3");

  public static String normalizeVoice(String voice) {
    if (voice == null) {
      return null;
    }
    String normalized = voice.trim();
    String mapped = LEGACY_V3_VOICE_MAP.get(normalized);
    return mapped == null ? normalized : "cosy_v3_flash_" + mapped;
  }

  public static boolean isLegacyVoice(String voice) {
    return voice != null && LEGACY_V3_VOICE_MAP.containsKey(voice.trim());
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
