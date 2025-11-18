<template>
  <a-button type="primary" @click="showModal">
    新建长期记忆
  </a-button>
  <a-modal 
    v-model:visible="visible" 
    title="新建长期记忆"
    :footer="null"
    @cancel="handleCancel"
  >
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
        label="想要记忆的概念"
        name="memoryKey"
        :rules="[{ required: true, message: '请输入 想要记忆的概念!' }]"
      >
        <a-input v-model:value="formState.memoryKey" placeholder="请输入想记忆的概念" />
      </a-form-item>

      <a-form-item
        label="产品ID"
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
        label="描述"
        name="description"
        :rules="[{ required: true, message: '请输入描述!' }]"
      >
        <a-input 
          v-model:value="formState.description" 
          placeholder="请输入描述"
          :maxlength="255"
          show-count
        />
      </a-form-item>

      <a-form-item
        label="记忆内容"
        name="memoryValue"
        :rules="[{ required: true, message: '请输入初始的记忆内容!' }]"
      >
        <a-textarea 
          v-model:value="formState.memoryValue" 
          placeholder="请输入记忆内容"
          :rows="4"
          :maxlength="1000"
          show-count
        />
      </a-form-item>

      <a-form-item :wrapper-col="{ offset: 8, span: 16 }">
        <a-button type="primary" html-type="submit">确认创建</a-button>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import { useRouter } from 'vue-router';
import { postLongMemory } from '@/api/agentLongMemory';
import { getProduct } from '@/api/product';

const router = useRouter();
const visible = ref(false);
const options = ref([]);

const formState = reactive({
  memoryKey: "",
  description: "",
  memoryValue: "无",
  productId: null,
});

const showModal = () => {
  Object.assign(formState, {
    memoryKey: "",
    description: "",
    memoryValue: "无",
    productId: null,
  });
  fetchProduct();
  visible.value = true;
};

const handleCancel = () => {
  visible.value = false;
};

const fetchProduct = () => {
  getProduct()
    .then((res) => {
      const { data, errorCode } = res.data;
      if (errorCode == 2001) {
        router.push('/login');
        return;
      }
      if (data && Array.isArray(data)) {
        options.value = data.map((item) => ({
          value: item.id,
          label: item.productName,
        }));
      }
    })
    .catch((err) => {
      console.error('获取产品列表失败:', err);
      message.error('获取产品列表失败');
    });
};

const handleSubmit = () => {
  postLongMemory(formState)
    .then((res) => {
      const { errorCode, errorMsg } = res.data;
      if (errorCode == 200) {
        message.success("创建成功!");
        visible.value = false;
      } else if (errorCode == 2001) {
        router.push('/login');
      } else {
        message.error(errorMsg || "创建失败!");
      }
    })
    .catch((err) => {
      console.error('创建失败:', err);
      message.error("创建失败，请重试!");
    });
};

const onFinish = (values) => {
  console.log('Success:', values);
  handleSubmit();
};

const onFinishFailed = (errorInfo) => {
  console.log('Failed:', errorInfo);
};
</script>
