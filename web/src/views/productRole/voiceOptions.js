// Source: https://docs.bailian.console.aliyun.com/zh/model-studio/cosyvoice-voice-list
// CosyVoice v3 flash catalog verified on 2026-09-17.
const voiceOptions = [
  { value: 'cosy_v3_flash_longxiaochun_v3', label: '龙小淳（V3 Flash） - 知性积极女；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longxiaoxia_v3', label: '龙小夏（V3 Flash） - 沉稳权威女；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longsanshu_v3', label: '龙三叔（V3 Flash） - 沉稳质感男；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longxing_v3', label: '龙星（V3 Flash） - 温婉邻家女；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longshu_v3', label: '龙书（V3 Flash） - 沉稳青年男；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longhuhu_v3', label: '龙呼呼（V3 Flash） - 天真烂漫女童；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longanyang', label: '龙安洋（V3 Flash） - 阳光大男孩；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longanhuan_v3', label: '龙安欢（V3）（V3 Flash） - 欢脱元气女；中文（普通话、广东话、东北话、河南话、湖南话、陕西话、山东话、四川话、安徽话）、英文' },
  { value: 'cosy_v3_flash_longanhuan', label: '龙安欢（V3 Flash） - 欢脱元气女；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longpaopao_v3', label: '龙泡泡（V3 Flash） - 飞天泡泡音；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longjielidou_v3', label: '龙杰力豆（V3 Flash） - 阳光顽皮男；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longxian_v3', label: '龙仙（V3 Flash） - 豪放可爱女；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longling_v3', label: '龙铃（V3 Flash） - 稚气呆板女；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longshanshan_v3', label: '龙闪闪（V3 Flash） - 戏剧化童声；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longniuniu_v3', label: '龙牛牛（V3 Flash） - 阳光男童声；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longjiaxin_v3', label: '龙嘉欣（V3 Flash） - 优雅粤语女；中文（粤语）、英文' },
  { value: 'cosy_v3_flash_longjiayi_v3', label: '龙嘉怡（V3 Flash） - 知性粤语女；中文（粤语）、英文' },
  { value: 'cosy_v3_flash_longanyue_v3', label: '龙安粤（V3 Flash） - 欢脱粤语男；中文（粤语）、英文' },
  { value: 'cosy_v3_flash_longlaotie_v3', label: '龙老铁（V3 Flash） - 东北直率男；中文（东北话）、英文' },
  { value: 'cosy_v3_flash_longshange_v3', label: '龙陕哥（V3 Flash） - 原味陕北男；中文（陕西话）、英文' },
  { value: 'cosy_v3_flash_longanmin_v3', label: '龙安闽（V3 Flash） - 清纯萝莉女；中文（闽南话）、英文' },
  { value: 'cosy_v3_flash_loongkyong_v3', label: 'loongkyong（V3 Flash） - 韩语女；韩语' },
  { value: 'cosy_v3_flash_loongriko_v3', label: 'Riko（V3 Flash） - 二次元霓虹女；日语' },
  { value: 'cosy_v3_flash_loongtomoka_v3', label: 'loongtomoka（V3 Flash） - 日语女；日语' },
  { value: 'cosy_v3_flash_loongabby_v3', label: 'loongabby（V3 Flash） - 美式英文女；美式英语' },
  { value: 'cosy_v3_flash_loongandy_v3', label: 'loongandy（V3 Flash） - 美式英文男；美式英语' },
  { value: 'cosy_v3_flash_loongannie_v3', label: 'loongannie（V3 Flash） - 美式英文女；美式英语' },
  { value: 'cosy_v3_flash_loongava_v3', label: 'loongava（V3 Flash） - 美式英文女；美式英语' },
  { value: 'cosy_v3_flash_loongbeth_v3', label: 'loongbeth（V3 Flash） - 美式英文女；美式英语' },
  { value: 'cosy_v3_flash_loongbetty_v3', label: 'loongbetty（V3 Flash） - 美式英文女；美式英语' },
  { value: 'cosy_v3_flash_loongcally_v3', label: 'loongcally（V3 Flash） - 美式英文女；美式英语' },
  { value: 'cosy_v3_flash_loongcindy_v3', label: 'loongcindy（V3 Flash） - 美式英文女；美式英语' },
  { value: 'cosy_v3_flash_loongdavid_v3', label: 'loongdavid（V3 Flash） - 美式英文男；美式英语' },
  { value: 'cosy_v3_flash_loongdonna_v3', label: 'loongdonna（V3 Flash） - 美式英文女；美式英语' },
  { value: 'cosy_v3_flash_loongemily_v3', label: 'loongemily（V3 Flash） - 英式英文女；英式英语' },
  { value: 'cosy_v3_flash_loongeric_v3', label: 'loongeric（V3 Flash） - 英式英文男；英式英语' },
  { value: 'cosy_v3_flash_loongluna_v3', label: 'loongluna（V3 Flash） - 英式英文女；英式英语' },
  { value: 'cosy_v3_flash_loongluca_v3', label: 'loongluca（V3 Flash） - 英式英文男；英式英语' },
  { value: 'cosy_v3_flash_loongtomoya_v3', label: 'loongtomoya（V3 Flash） - 日语男；日语' },
  { value: 'cosy_v3_flash_loongyuuna_v3', label: 'Yuuna（V3 Flash） - 日语女；日语' },
  { value: 'cosy_v3_flash_loongyuuma_v3', label: 'Yuuma（V3 Flash） - 日语男；日语' },
  { value: 'cosy_v3_flash_loongjihun_v3', label: 'Jihun（V3 Flash） - 韩语男；韩语' },
  { value: 'cosy_v3_flash_loongindah_v3', label: 'loongindah（V3 Flash） - 印尼女；印尼语' },
  { value: 'cosy_v3_flash_longfei_v3', label: '龙飞（V3 Flash） - 热血磁性男；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longyingxiao_v3', label: '龙应笑（V3 Flash） - 清甜推销女；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longyingxun_v3', label: '龙应询（V3 Flash） - 年轻青涩男；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longyingjing_v3', label: '龙应静（V3 Flash） - 低调冷静女；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longyingling_v3', label: '龙应聆（V3 Flash） - 温和共情女；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longyingtao_v3', label: '龙应桃（V3 Flash） - 温柔淡定女；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longyumi_v3', label: 'YUMI（V3 Flash） - 正经青年女；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longanyun_v3', label: '龙安昀（V3 Flash） - 居家暖男；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longanwen_v3', label: '龙安温（V3 Flash） - 优雅知性女；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longanli_v3', label: '龙安莉（V3 Flash） - 利落从容女；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longanlang_v3', label: '龙安朗（V3 Flash） - 清爽利落男；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longyingmu_v3', label: '龙应沐（V3 Flash） - 优雅知性女；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longantai_v3', label: '龙安台（V3 Flash） - 嗲甜台湾女；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longhua_v3', label: '龙华（V3 Flash） - 元气甜美女；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longcheng_v3', label: '龙橙（V3 Flash） - 智慧青年男；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longze_v3', label: '龙泽（V3 Flash） - 温暖元气男；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longzhe_v3', label: '龙哲（V3 Flash） - 呆板大暖男；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longyan_v3', label: '龙颜（V3 Flash） - 温暖春风女；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longtian_v3', label: '龙天（V3 Flash） - 磁性理智男；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longwan_v3', label: '龙婉（V3 Flash） - 细腻柔声女；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longqiang_v3', label: '龙嫱（V3 Flash） - 浪漫风情女；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longfeifei_v3', label: '龙菲菲（V3 Flash） - 甜美娇气女；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longhao_v3', label: '龙浩（V3 Flash） - 多情忧郁男；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longanrou_v3', label: '龙安柔（V3 Flash） - 温柔闺蜜女；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longhan_v3', label: '龙寒（V3 Flash） - 温暖痴情男；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longanzhi_v3', label: '龙安智（V3 Flash） - 睿智轻熟男；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longanling_v3', label: '龙安灵（V3 Flash） - 思维灵动女；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longanya_v3', label: '龙安雅（V3 Flash） - 高雅气质女；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longanqin_v3', label: '龙安亲（V3 Flash） - 亲和活泼女；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longmiao_v3', label: '龙妙（V3 Flash） - 抑扬顿挫女；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longyuan_v3', label: '龙媛（V3 Flash） - 温暖治愈女；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longyue_v3', label: '龙悦（V3 Flash） - 温暖磁性女；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longxiu_v3', label: '龙修（V3 Flash） - 博才说书男；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longnan_v3', label: '龙楠（V3 Flash） - 睿智青年男；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longwanjun_v3', label: '龙婉君（V3 Flash） - 细腻柔声女；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longyichen_v3', label: '龙逸尘（V3 Flash） - 洒脱活力男；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longlaobo_v3', label: '龙老伯（V3 Flash） - 沧桑岁月爷；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longlaoyi_v3', label: '龙老姨（V3 Flash） - 烟火从容阿姨；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longjiqi_v3', label: '龙机器（V3 Flash） - 呆萌机器人；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longhouge_v3', label: '龙猴哥（V3 Flash） - 经典猴哥；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longdaiyu_v3', label: '龙黛玉（V3 Flash） - 娇率才女音；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longanran_v3', label: '龙安燃（V3 Flash） - 活泼质感女；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longanxuan_v3', label: '龙安宣（V3 Flash） - 经典直播女；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_longshuo_v3', label: '龙硕（V3 Flash） - 博才干练男；中文（普通话）、英文' },
  { value: 'cosy_v3_flash_loongbella_v3', label: 'Bella3.0（V3 Flash） - 精准干练女；中文（普通话）、英文' },
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
  // MiniMax TTS 系列音色 - 普通话
  {
    value: 'minimax-Chinese (Mandarin)_Warm_Bestie',
    label: '【MiniMax】温暖闺蜜女'
  },
  {
    value: 'minimax-Chinese (Mandarin)_News_Anchor',
    label: '【MiniMax】新闻主播'
  },
  {
    value: 'minimax-Chinese (Mandarin)_Gentleman',
    label: '【MiniMax】绅士男'
  },
  {
    value: 'minimax-Chinese (Mandarin)_Sweet_Lady',
    label: '【MiniMax】甜美女士'
  },
  {
    value: 'minimax-Chinese (Mandarin)_Male_Announcer',
    label: '【MiniMax】男播音员'
  },
  {
    value: 'minimax-Chinese (Mandarin)_Reliable_Executive',
    label: '【MiniMax】可靠高管男'
  },
  {
    value: 'minimax-Chinese (Mandarin)_HK_Flight_Attendant',
    label: '【MiniMax】香港空乘'
  },
  // MiniMax TTS 系列音色 - 粤语
  {
    value: 'minimax-Cantonese_ProfessionalHost (F)',
    label: '【MiniMax】粤语-专业女主持'
  },
  {
    value: 'minimax-Cantonese_GentleLady',
    label: '【MiniMax】粤语-温柔女士'
  },
  {
    value: 'minimax-Cantonese_ProfessionalHost (M)',
    label: '【MiniMax】粤语-专业男主持'
  },
  {
    value: 'minimax-Cantonese_CuteGirl',
    label: '【MiniMax】粤语-可爱女孩'
  },
  {
    value: 'minimax-Cantonese_PlayfulMan',
    label: '【MiniMax】粤语-顽皮男'
  },
  {
    value: 'minimax-Cantonese_KindWoman',
    label: '【MiniMax】粤语-善良女士'
  },
];
const legacyVoiceMap = {
  longxiaochun: 'cosy_v3_flash_longxiaochun_v3',
  longxiaoxia: 'cosy_v3_flash_longxiaoxia_v3',
  longxiaocheng: 'cosy_v3_flash_longsanshu_v3',
  longxiaobai: 'cosy_v3_flash_longxing_v3',
  longshu: 'cosy_v3_flash_longshu_v3',
  longtong: 'cosy_v3_flash_longhuhu_v3'
};

export const normalizeVoice = (voice) => {
  const value = typeof voice === 'string' ? voice.trim() : voice;
  return Object.hasOwn(legacyVoiceMap, value) ? legacyVoiceMap[value] : value;
};
export default voiceOptions;
