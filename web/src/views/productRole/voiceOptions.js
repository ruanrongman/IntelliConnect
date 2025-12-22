const voiceOptions = [  
  {  
    value: 'longxiaochun',  
    label: '龙小醇 - 嗓音如丝般柔滑，温暖中流淌着亲切与抚慰，恰似春风吹过心田。'  
  },  
  {  
    value: 'longxiaoxia',  
    label: '龙小夏 - 以温润磁性的声线，宛如夏日细雨，悄然滋润听者心灵，营造恬静氛围。'  
  },  
  {  
    value: 'longxiaocheng',  
    label: '龙小诚 - 深邃而稳重的声音犹如醇厚佳酿，散发出成熟魅力。'  
  },  
  {  
    value: 'longxiaobai',  
    label: '龙小白 - 以轻松亲和的声调演绎闲适日常，其嗓音如邻家女孩般亲切自然。'  
  },  
  {  
    value: 'longshu',  
    label: '龙叔 - 以专业沉稳的播报风格传递新闻资讯，其嗓音富含权威与信赖感。'  
  },  
  {  
    value: 'longtong',  
    label: '龙童 - 以稚嫩的童声撒欢，像是春日里的小溪，清脆跳跃，流淌着生机勃勃的旋律。'  
  },
  // --- 电话销售 ---
  {
    value: 'cosy_v2_longyingxiao',
    label: '龙应笑 - 清甜推销女'
  },

// --- 短视频配音 ---
  {
    value: 'cosy_v2_longjiqi',
    label: '龙机器 - 呆萌机器人'
  },
  {
    value: 'cosy_v2_longhouge',
    label: '龙猴哥 - 经典猴哥'
  },
  {
    value: 'cosy_v2_longjixin',
    label: '龙机心 - 毒舌心机女'
  },
  {
    value: 'cosy_v2_longanyue',
    label: '龙安粤 - 欢脱粤语男'
  },
  {
    value: 'cosy_v2_longshange',
    label: '龙陕哥 - 原味陕北男'
  },
  {
    value: 'cosy_v2_longanmin',
    label: '龙安敏 - 甜美闽南女'
  },
  {
    value: 'cosy_v2_longdaiyu',
    label: '龙黛玉 - 娇率才女音'
  },
  {
    value: 'cosy_v2_longgaoseng',
    label: '龙高僧 - 得道高僧音'
  },

// --- 语音助手 ---
  {
    value: 'cosy_v2_longanli',
    label: '龙安莉 - 利落从容女'
  },
  {
    value: 'cosy_v2_longanlang',
    label: '龙安朗 - 清爽利落男'
  },
  {
    value: 'cosy_v2_longanwen',
    label: '龙安温 - 优雅知性女'
  },
  {
    value: 'cosy_v2_longanyun',
    label: '龙安昀 - 居家暖男'
  },
  {
    value: 'cosy_v2_longyumi_v2',
    label: 'YUMI - 正经青年女'
  },
  {
    value: 'cosy_v2_longxiaochun_v2',
    label: '龙小淳 - 知性积极女'
  },
  {
    value: 'cosy_v2_longxiaoxia_v2',
    label: '龙小夏 - 沉稳权威女'
  },

// --- 有声书 ---
  {
    value: 'cosy_v2_longyichen',
    label: '龙逸尘 - 洒脱活力男'
  },
  {
    value: 'cosy_v2_longwanjun',
    label: '龙婉君 - 细腻柔声女'
  },
  {
    value: 'cosy_v2_longlaobo',
    label: '龙老伯 - 沧桑岁月爷'
  },
  {
    value: 'cosy_v2_longlaoyi',
    label: '龙老姨 - 烟火从容阿姨'
  },
  {
    value: 'cosy_v2_longbaizhi',
    label: '龙白芷 - 睿气旁白女'
  },
  {
    value: 'cosy_v2_longsanshu',
    label: '龙三叔 - 沉稳质感男'
  },
  {
    value: 'cosy_v2_longxiu_v2',
    label: '龙修 - 博才说书男'
  },
  {
    value: 'cosy_v2_longmiao_v2',
    label: '龙妙 - 抑扬顿挫女'
  },
  {
    value: 'cosy_v2_longyue_v2',
    label: '龙悦 - 温暖磁性女'
  },
  {
    value: 'cosy_v2_longnan_v2',
    label: '龙楠 - 睿智青年男'
  },
  {
    value: 'cosy_v2_longyuan_v2',
    label: '龙媛 - 温暖治愈女'
  },

// --- 社交陪伴 ---
  {
    value: 'cosy_v2_longanqin',
    label: '龙安亲 - 亲和活泼女'
  },
  {
    value: 'cosy_v2_longanya',
    label: '龙安雅 - 高雅气质女'
  },
  {
    value: 'cosy_v2_longanshuo',
    label: '龙安朔 - 干净清爽男'
  },
  {
    value: 'cosy_v2_longanling',
    label: '龙安灵 - 思维灵动女'
  },
  {
    value: 'cosy_v2_longanzhi',
    label: '龙安智 - 睿智轻熟男'
  },
  {
    value: 'cosy_v2_longanrou',
    label: '龙安柔 - 温柔闺蜜女'
  },
  {
    value: 'cosy_v2_longqiang_v2',
    label: '龙嫱 - 浪漫风情女'
  },
  {
    value: 'cosy_v2_longhan_v2',
    label: '龙寒 - 温暖痴情男'
  },
  {
    value: 'cosy_v2_longxing_v2',
    label: '龙星 - 温婉邻家女'
  },
  {
    value: 'cosy_v2_longhua_v2',
    label: '龙华 - 元气甜美女'
  },
  {
    value: 'cosy_v2_longwan_v2',
    label: '龙婉 - 积极知性女'
  },
  {
    value: 'cosy_v2_longcheng_v2',
    label: '龙橙 - 智慧青年男'
  },
  {
    value: 'cosy_v2_longfeifei_v2',
    label: '龙菲菲 - 甜美娇气女'
  },
  {
    value: 'cosy_v2_longxiaocheng_v2',
    label: '龙小诚 - 磁性低音男'
  },
  {
    value: 'cosy_v2_longzhe_v2',
    label: '龙哲 - 呆板大暖男'
  },
  {
    value: 'cosy_v2_longyan_v2',
    label: '龙颜 - 温暖春风女'
  },
  {
    value: 'cosy_v2_longtian_v2',
    label: '龙天 - 磁性理智男'
  },
  {
    value: 'cosy_v2_longze_v2',
    label: '龙泽 - 温暖元气男'
  },
  {
    value: 'cosy_v2_longshao_v2',
    label: '龙邵 - 积极向上男'
  },
  {
    value: 'cosy_v2_longhao_v2',
    label: '龙浩 - 多情忧郁男'
  },
  {
    value: 'cosy_v2_kabuleshen_v2',
    label: '龙深 - 实力歌手男'
  },

// --- 童声 ---
  {
    value: 'cosy_v2_longhuhu',
    label: '龙呼呼 - 天真烂漫女童'
  },
  {
    value: 'cosy_v2_longjielidou_v2',
    label: '龙杰力豆 - 阳光顽皮男'
  },
  {
    value: 'cosy_v2_longling_v2',
    label: '龙铃 - 稚气呆板女'
  },
  {
    value: 'cosy_v2_longke_v2',
    label: '龙可 - 懵懂乖乖女'
  },
  {
    value: 'cosy_v2_longxian_v2',
    label: '龙仙 - 豪放可爱女'
  },

// --- 消费电子 ---
  {
    value: 'cosy_v2_longanpei',
    label: '龙安培 - 青少年教师女'
  },
  {
    value: 'cosy_v2_longwangwang',
    label: '龙汪汪 - 台湾少年音'
  },
  {
    value: 'cosy_v2_longpaopao',
    label: '龙泡泡 - 飞天泡泡音'
  },
  {
    value: 'cosy_v2_longshanshan',
    label: '龙闪闪 - 戏剧化童声'
  },
  {
    value: 'cosy_v2_longniuniu',
    label: '龙牛牛 - 阳光男童声'
  },

// --- 客服 ---
  {
    value: 'cosy_v2_longyingmu',
    label: '龙应沐 - 优雅知性女'
  },
  {
    value: 'cosy_v2_longyingxun',
    label: '龙应询 - 年轻青涩男'
  },
  {
    value: 'cosy_v2_longyingcui',
    label: '龙应催 - 严肃催收男'
  },
  {
    value: 'cosy_v2_longyingda',
    label: '龙应答 - 开朗高音女'
  },
  {
    value: 'cosy_v2_longyingjing',
    label: '龙应静 - 低调冷静女'
  },
  {
    value: 'cosy_v2_longyingyan',
    label: '龙应严 - 义正严辞女'
  },
  {
    value: 'cosy_v2_longyingtian',
    label: '龙应甜 - 温柔甜美女'
  },
  {
    value: 'cosy_v2_longyingbing',
    label: '龙应冰 - 尖锐强势女'
  },
  {
    value: 'cosy_v2_longyingtao',
    label: '龙应桃 - 温柔淡定女'
  },
  {
    value: 'cosy_v2_longyingling',
    label: '龙应聆 - 温和共情女'
  },

// --- 直播带货 ---
  {
    value: 'cosy_v2_longanran',
    label: '龙安燃 - 活泼质感女'
  },
  {
    value: 'cosy_v2_longanxuan',
    label: '龙安宣 - 经典直播女'
  },
  {
    value: 'cosy_v2_longanchong',
    label: '龙安冲 - 激情推销男'
  },
  {
    value: 'cosy_v2_longanping',
    label: '龙安萍 - 高亢直播女'
  },

// --- 方言 ---
  {
    value: 'cosy_v2_longlaotie_v2',
    label: '龙老铁 - 东北直率男'
  },
  {
    value: 'cosy_v2_longjiayi_v2',
    label: '龙嘉怡 - 知性粤语女'
  },
  {
    value: 'cosy_v2_longtao_v2',
    label: '龙桃 - 积极粤语女'
  },

// --- 诗词朗诵 ---
  {
    value: 'cosy_v2_longfei_v2',
    label: '龙飞 - 热血磁性男'
  },
  {
    value: 'cosy_v2_libai_v2',
    label: '李白 - 古代诗仙男'
  },
  {
    value: 'cosy_v2_longjin_v2',
    label: '龙津 - 优雅温润男'
  },

// --- 新闻播报 ---
  {
    value: 'cosy_v2_longshu_v2',
    label: '龙书 - 沉稳青年男'
  },
  {
    value: 'cosy_v2_loongbella_v2',
    label: 'Bella2.0 - 精准干练女'
  },
  {
    value: 'cosy_v2_longshuo_v2',
    label: '龙硕 - 博才干练男'
  },
  {
    value: 'cosy_v2_longxiaobai_v2',
    label: '龙小白 - 沉稳播报女'
  },
  {
    value: 'cosy_v2_longjing_v2',
    label: '龙婧 - 典型播音女'
  },
  {
    value: 'cosy_v2_loongstella_v2',
    label: 'loongstella - 飒爽利落女'
  },

// --- 出海营销 ---
  {
    value: 'cosy_v2_loongyuuna_v2',
    label: 'loongyuuna - 元气霓虹女'
  },
  {
    value: 'cosy_v2_loongyuuma_v2',
    label: 'loongyuuma - 干练霓虹男',
  },
  {
    value: 'cosy_v2_loongjihun_v2',
    label: 'loongjihun - 阳光韩国男'
  },
  {
    value: 'cosy_v2_loongeva_v2',
    label: 'loongeva - 知性英文女'
  },
  {
    value: 'cosy_v2_loongbrian_v2',
    label: 'loongbrian - 沉稳英文男'
  },
  {
    value: 'cosy_v2_loongluna_v2',
    label: 'loongluna - 英式英文女'
  },
  {
    value: 'cosy_v2_loongluca_v2',
    label: 'loongluca - 英式英文男'
  },
  {
    value: 'cosy_v2_loongemily_v2',
    label: 'loongemily - 英式英文女'
  },
  {
    value: 'cosy_v2_loongeric_v2',
    label: 'loongeric - 英式英文男'
  },
  {
    value: 'cosy_v2_loongabby_v2',
    label: 'loongabby - 美式英文女'
  },
  {
    value: 'cosy_v2_loongannie_v2',
    label: 'loongannie - 美式英文女'
  },
  {
    value: 'cosy_v2_loongandy_v2',
    label: 'loongandy - 美式英文男'
  },
  {
    value: 'cosy_v2_loongava_v2',
    label: 'loongava - 美式英文女'
  },
  {
    value: 'cosy_v2_loongbeth_v2',
    label: 'loongbeth - 美式英文女'
  },
  {
    value: 'cosy_v2_loongbetty_v2',
    label: 'loongbetty - 美式英文女'
  },
  {
    value: 'cosy_v2_loongcindy_v2',
    label: 'loongcindy - 美式英文女'
  },
  {
    value: 'cosy_v2_loongcally_v2',
    label: 'loongcally - 美式英文女'
  },
  {
    value: 'cosy_v2_loongdavid_v2',
    label: 'loongdavid - 美式英文男'
  },
  {
    value: 'cosy_v2_loongdonna_v2',
    label: 'loongdonna - 美式英文女'
  },
  {
    value: 'cosy_v2_loongkyong_v2',
    label: 'loongkyong - 韩语女'
  },
  {
    value: 'cosy_v2_loongtomoka_v2',
    label: 'loongtomoka - 日语女'
  },
  {
    value: 'cosy_v2_loongtomoya_v2',
    label: 'loongtomoya - 日语男'
  },
  // Edge TTS 系列音色
  {
    value: 'edge-zh-CN-XiaoxiaoNeural',
    label: '晓晓 - 中文 (简体) 普通话 女'
  },
  {
    value: 'edge-zh-CN-XiaoyiNeural',
    label: '晓伊 - 中文 (简体) 普通话 女'
  },
  {
    value: 'edge-zh-CN-YunjianNeural',
    label: '云健 - 中文 (简体) 普通话 男'
  },
  {
    value: 'edge-zh-CN-YunxiNeural',
    label: '云希 - 中文 (简体) 普通话 男'
  },
  {
    value: 'edge-zh-CN-YunxiaNeural',
    label: '云夏 - 中文 (简体) 普通话 男'
  },
  {
    value: 'edge-zh-CN-YunyangNeural',
    label: '云扬 - 中文 (简体) 普通话 男'
  },
  {
    value: 'edge-zh-CN-liaoning-XiaobeiNeural',
    label: '晓北 - 中文 (简体) 辽宁方言 女'
  },
  {
    value: 'edge-zh-CN-shaanxi-XiaoniNeural',
    label: '晓妮 - 中文 (简体) 陕西方言 女'
  },
  {
    value: 'edge-zh-HK-HiuGaaiNeural',
    label: 'HiuGaai - 中文 (繁体) 粤语 女'
  },
  {
    value: 'edge-zh-HK-HiuMaanNeural',
    label: 'HiuMaan - 中文 (繁体) 粤语 女'
  },
  {
    value: 'edge-zh-HK-WanLungNeural',
    label: 'WanLung - 中文 (繁体) 粤语 男'
  },
  {
    value: 'edge-zh-TW-HsiaoChenNeural',
    label: '晓臻 - 中文 (繁体) 台湾 女'
  },
  {
    value: 'edge-zh-TW-HsiaoYuNeural',
    label: '晓雨 - 中文 (繁体) 台湾 女'
  },
  {
    value: 'edge-zh-TW-YunJheNeural',
    label: '云哲 - 中文 (繁体) 台湾 男'
  },
  {
    value: 'edge-en-US-AnaNeural',
    label: '英文 - 美式 - 女 (安娜)'
  },
  {
    value: 'edge-ko-KR-SunHiNeural',
    label: '韩语 - 韩国 - 女 (孙Hi)'
  },
  {
    value: 'edge-ru-RU-SvetlanaNeural',
    label: '俄语 - 俄语 - 女 ( Светлана)'
  },
  {
    value: 'edge-ja-JP-NanamiNeural',
    label: '日语 - 日本 - 女 (南ami)'
  },
];
export default voiceOptions;