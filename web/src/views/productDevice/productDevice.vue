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
        label="设备名(不重复)"
        name="name"
        :rules="[{ required: true, message: 'Please input your name!' }]"
      >
        <a-input v-model:value="formState.name" placeholder="请输入英文"/>
      </a-form-item>
  
      <a-form-item
        label="clientId"
        name="clientId"
        :rules="[{ required: true, message: 'Please input your clientId!' }]"
      >
        <a-input v-model:value="formState.clientId" />
      </a-form-item>
      <a-form-item
        label="功能描述"
        name="description"
        :rules="[{ required: true, message: 'Please input your description!' }]"
      >
        <a-input v-model:value="formState.description" />
      </a-form-item>
      <!-- 替换原有的模型Id输入框为选择框 -->
      <a-form-item
        label="物模型"
        name="modelId"
        :rules="[{ required: true, message: 'Please select a model!' }]"
      >
        <a-select
          v-model:value="formState.modelId"
          :options="modelOptions"
          placeholder="请选择物模型"
          allowClear
        />
      </a-form-item>
      <a-form-item
        label="使能"
        name="allow"
       :rules="[{ required: true, message: 'Please input your allow!' }]"
      >
        <a-select
         v-model:value="formState.allow"
        >
           <a-select-option value="0">禁止</a-select-option>
           <a-select-option value="1">使能</a-select-option>
        </a-select>
      </a-form-item> 
      
      <a-form-item :wrapper-col="{ offset: 8, span: 16 }">
        <a-button type="primary" html-type="submit">Submit</a-button>
      </a-form-item>
    </a-form>
    </a-modal>
</template>

<script setup>
import { reactive, ref, toRaw, onMounted } from 'vue';
import { message } from 'ant-design-vue'
import { postProductDevice } from '@/api/device';
import { getproductModel } from '@/api/productModel'; // 确保导入这个方法
import { getProductName } from '@/api/product'; // 确保导入这个方法
import { useRouter } from 'vue-router';

const router = useRouter();
const visible = ref(false);
const modelOptions = ref([]); // 物模型选项

const showModal = () => {
  fetchproductModel(); // 显示模态框时获取物模型数据
  visible.value = !visible.value;
}

const handleCancel = () => {
  visible.value = false;
}

const handleCreate = () => {
  visible.value = false;
}

const formState = reactive({
  allow: null,
  clientId: "",
  description: "",
  modelId: null, // 改为null，因为现在是选择框
  name: ""
});

// 获取产品名称的方法
const getProductNameById = async (productId) => {
  try {
    const res = await getProductName({ id: productId });
    const { data, errorCode } = res.data;
    if (errorCode === 200) {
      return data;
    }
    return `产品ID: ${productId}`; // 如果获取失败，显示ID
  } catch (error) {
    console.error('获取产品名称失败:', error);
    return `产品ID: ${productId}`;
  }
};

// 获取物模型数据
const fetchproductModel = async () => {
  try {
    const res = await getproductModel();
    const { data, errorCode } = res.data;
    if (errorCode === 2001) {
      router.push('/login');
      return;
    }
    
    // 处理数据并获取产品名称
    const processedData = await Promise.all(
      data.map(async (item) => {
        const productName = await getProductNameById(item.productId);
        return {
          value: item.id,
          label: `${item.name} (${productName})`, // 显示模型名称和产品名称
        };
      })
    );
    
    modelOptions.value = processedData;
  } catch (err) {
    console.error('获取物模型失败:', err);
    message.error('获取物模型数据失败');
  }
};

const handleSubmit = () => {
  postProductDevice(toRaw(formState))
    .then((res) => {
      const { data, errorCode } = res.data;
      console.log('auth', data);
      if (errorCode != 200) {
        message.error("创建失败!");
        console.log("error");
      } else {
        message.success("创建成功!");
        console.log(data);
      }
    })
    .catch((err) => {
      console.log(err);
      message.error("创建失败!");
    });
}

const onFinish = values => {
  console.log('Success:', values);
  handleSubmit();
  visible.value = false;
};

const onFinishFailed = errorInfo => {
  console.log('Failed:', errorInfo);
};
</script>