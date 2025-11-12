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
        label="mcp服务器地址"
        name="url"
        :rules="[{ required: true, message: '请输入mcp服务器地址!' }]"
      >
        <a-input v-model:value="formState.url" />
      </a-form-item>
      <a-form-item
        label="Endpoint"
        name="sseEndpoint"
        :rules="[{ required: true, message: '请输入Endpoint!' }]"
      >
        <a-input v-model:value="formState.sseEndpoint" />
      </a-form-item>

      <!-- 产品id下拉选择框 -->
      <a-form-item
        label="产品id"
        name="productId"
        :rules="[{ required: true, message: '请选择产品!' }]"
      >
        <a-select
          v-model:value="formState.productId"
          :options="options"
          placeholder="请选择产品"
          allowClear
        />
      </a-form-item>

      <a-form-item
        label="mcp服务器描述"
        name="description"
        :rules="[{ required: true, message: '请输入mcp服务器描述!' }]"
      >
        <a-input v-model:value="formState.description" />
      </a-form-item>

      <a-form-item :wrapper-col="{ offset: 8, span: 16 }">
        <a-button type="primary" html-type="submit">Submit</a-button>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { reactive, ref, toRaw } from 'vue';
import { postMcpServer } from '@/api/productMcp';
import { getProduct } from '@/api/product';
import { message } from 'ant-design-vue';

const visible = ref(false);
const options = ref([]); // 存储产品下拉列表

// 打开弹窗并获取产品列表
const showModal = () => {
  fetchProduct();
  visible.value = true;
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
        // 如果需要跳转登录可以添加
        // router.push('/login');
      }
      options.value = data.map((item) => ({
        value: item.id,
        label: item.productName,
      }));
    })
    .catch((err) => {
      console.error(err);
    });
};

const formState = reactive({
  sseEndpoint: '',
  url: '',
  description: '',
  productId: '' // 下拉框存储的是字符串
});

const handleSubmit = () => {
  postMcpServer(toRaw(formState))
    .then((res) => {
      const { data, errorCode } = res.data;
      if (errorCode != 200) {
        message.error('创建失败!');
      } else {
        message.success('创建成功!');
        // 重置表单
        formState.sseEndpoint = '';
        formState.url = '';
        formState.description = '';
        formState.productId = '';
      }
    })
    .catch((err) => {
      console.error(err);
      message.error('创建失败，请重试!');
    });
};

const onFinish = () => {
  handleSubmit();
  visible.value = false;
};

const onFinishFailed = (errorInfo) => {
  console.log('Failed:', errorInfo);
};
</script>