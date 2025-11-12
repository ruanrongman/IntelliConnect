<template>  
  <a-button type="primary" @click="showModal">  
    新建配置  
  </a-button>  
  <a-modal :visible="visible"    
  :footer="null"  
  @cancel="handleCancel"  
  @create="handleCreate">  
  <a-form  
    :model="formState"  
    name="basic"  
    :label-col="{ span: 6 }"  
    :wrapper-col="{ span: 16 }"  
    autocomplete="off"  
    @finish="onFinish"  
    @finishFailed="onFinishFailed"  
  >  
    <a-form-item  
      label="产品id"  
      name="productId"  
      :rules="[{ required: true, message: 'Please select your productId!' }]"  
    >  
      <a-select  
        v-model:value="formState.productId"  
        :options="options"  
        placeholder="请选择产品"  
        allowClear  
      />  
    </a-form-item>  

    <a-form-item  
      label="助手名称"  
      name="assistantName"  
      :rules="[{ required: true, message: 'Please input your assistantName!' }]"  
    >  
      <a-input v-model:value="formState.assistantName" />  
    </a-form-item>  

    <a-form-item  
      label="用户名"  
      name="userName"  
      :rules="[{ required: true, message: 'Please input your userName!' }]"  
    >  
      <a-input v-model:value="formState.userName" />  
    </a-form-item>  

    <a-form-item  
      label="角色"  
      name="role"  
      :rules="[{ required: true, message: 'Please input your role!' }]"  
    >  
      <a-input v-model:value="formState.role" />  
    </a-form-item>  

    <a-form-item  
      label="角色介绍"  
      name="roleIntroduction"  
      :rules="[{ required: true, message: 'Please input your roleIntroduction!' }]"  
    >  
      <a-textarea v-model:value="formState.roleIntroduction" :rows="4" :maxlength="2000" show-count/>  
    </a-form-item>  

    <a-form-item  
      label="语音"  
      name="voice"  
      :rules="[{ required: true, message: 'Please select your voice!' }]"  
    >  
      <a-select  
        v-model:value="formState.voice"  
        :options="voiceOptions"  
        placeholder="请选择语音"  
        allowClear  
      />  
    </a-form-item>  

    <a-form-item :wrapper-col="{ offset: 8, span: 16 }">  
      <a-button type="primary" html-type="submit">Submit</a-button>  
    </a-form-item>  
  </a-form>  
  </a-modal>  
</template>  

<script setup>  
import { reactive, ref, toRaw } from 'vue';  
import { message } from 'ant-design-vue';  
import { postProductRole } from '@/api/productRole';  
import { getProduct } from '@/api/product';  

const visible = ref(false);  
const options = ref([]);  

// 语音选项配置
const voiceOptions = ref([
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
  // Edge TTS 系列音色
  {
    value: 'edge-zh-CN-XiaoxiaoNeural',
    label: '晓晓 - 中文 (简体) - 普通话 - 女'
  },
  {
    value: 'edge-zh-CN-XiaoyiNeural',
    label: '晓伊 - 中文 (简体) - 普通话 - 女'
  },
  {
    value: 'edge-zh-CN-YunjianNeural',
    label: '云健 - 中文 (简体) - 普通话 - 男'
  },
  {
    value: 'edge-zh-CN-YunxiNeural',
    label: '云希 - 中文 (简体) - 普通话 - 男'
  },
  {
    value: 'edge-zh-CN-YunxiaNeural',
    label: '云夏 - 中文 (简体) - 普通话 - 男'
  },
  {
    value: 'edge-zh-CN-YunyangNeural',
    label: '云扬 - 中文 (简体) - 普通话 - 男'
  },
  {
    value: 'edge-zh-CN-liaoning-XiaobeiNeural',
    label: '晓北 - 中文 (简体) - 辽宁方言 - 女'
  },
  {
    value: 'edge-zh-CN-shaanxi-XiaoniNeural',
    label: '晓妮 - 中文 (简体) - 陕西方言 - 女'
  },
  {
    value: 'edge-zh-HK-HiuGaaiNeural',
    label: 'HiuGaai - 中文 (繁体) - 粤语 - 女'
  },
  {
    value: 'edge-zh-HK-HiuMaanNeural',
    label: 'HiuMaan - 中文 (繁体) - 粤语 - 女'
  },
  {
    value: 'edge-zh-HK-WanLungNeural',
    label: 'WanLung - 中文 (繁体) - 粤语 - 男'
  },
  {
    value: 'edge-zh-TW-HsiaoChenNeural',
    label: '晓臻 - 中文 (繁体) - 台湾 - 女'
  },
  {
    value: 'edge-zh-TW-HsiaoYuNeural',
    label: '晓雨 - 中文 (繁体) - 台湾 - 女'
  },
  {
    value: 'edge-zh-TW-YunJheNeural',
    label: '云哲 - 中文 (繁体) - 台湾 - 男'
  }
]);

const showModal = () => {  
  fetchProduct(); // 打开弹窗时获取产品列表  
  visible.value = !visible.value;  
};  

const handleCancel = () => {  
  visible.value = false;  
};  

const handleCreate = () => {  
  visible.value = false;  
};  

// 获取产品列表  
const fetchProduct = () => {  
  getProduct()  
    .then((res) => {  
      const { data, errorCode } = res.data;  
      if (errorCode == 2001) {  
        // 如果有路由，取消注释下面这行  
        // router.push('/login');  
      }  
      options.value = data.map((item, index) => ({  
        value: item.id,  
        label: item.productName,  
      }));  
    })  
    .catch((err) => {  
      console.log(err);  
    });  
};  

const formState = reactive({  
  productId: "", // 改为字符串，与下拉选择保持一致  
  assistantName: "",  
  userName: "",
  role: "",
  roleIntroduction: "",
  voice: ""
});  

const handleSubmit = () => {  
  postProductRole(toRaw(formState))  
    .then((res) => {  
      const { data, errorCode } = res.data;  
      console.log('auth', data);  
      if (errorCode != 200) {  
        message.error("创建失败!");  
        console.log("error");  
      } else {  
        message.success("创建成功!");  
        console.log(data);  
        // 重置表单  
        formState.productId = '';
        formState.assistantName = '';  
        formState.userName = '';
        formState.role = '';
        formState.roleIntroduction = '';
        formState.voice = '';
      }  
    })  
    .catch((err) => {  
      console.log(err);  
      message.error("创建失败，请重试!");  
    });  
};  

const onFinish = values => {  
  console.log('Success:', values);  
  handleSubmit();  
  visible.value = false;  
};  

const onFinishFailed = errorInfo => {  
  console.log('Failed:', errorInfo);  
};  
</script>