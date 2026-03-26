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
        label="ASR服务"
        name="asrName"
        :rules="[{ required: true, message: 'Please select asrName!' }]"
      >
        <a-select
          v-model:value="formState.asrName"
          :options="asrOptions"
          placeholder="请选择ASR服务"
          allowClear
        />
      </a-form-item>

      <a-form-item
        label="提供商"
        name="providerName"
        :rules="[{ required: true, message: 'Please select providerName!' }]"
      >
        <a-select
          v-model:value="formState.providerName"
          :options="providerOptions"
          placeholder="请选择提供商"
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
import { postProductAsr } from '@/api/productAsr';
import { getProduct } from '@/api/product';

const visible = ref(false);
const options = ref([]);

// ASR服务选项
const asrOptions = ref([
  { value: 'dashscope', label: 'DashScope (阿里云)' },
  { value: 'funasr', label: 'FunASR' }
])

// 提供商选项
const providerOptions = ref([
  { value: 'default', label: '默认' }
])

const showModal = () => {
  fetchProduct();
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
  productId: "",
  asrName: "",
  providerName: "default"
});

const handleSubmit = () => {
  postProductAsr(toRaw(formState))
    .then((res) => {
      const { data, errorCode } = res.data;
      if (errorCode != 200) {
        message.error("创建失败!");
        console.log("error");
      } else {
        message.success("创建成功!");
        console.log(data);
        // 重置表单
        formState.productId = '';
        formState.asrName = '';
        formState.providerName = 'default';
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
