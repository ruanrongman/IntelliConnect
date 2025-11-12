<template>
  <a-button type="primary" @click="showModal">
    新建配置
  </a-button>
  <a-modal
    :visible="visible"
    :footer="null"
    @cancel="handleCancel"
    @create="handleCreate"
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
        label="属性名称"
        name="jsonKey"
        :rules="[{ required: true, message: '请输入属性名称!' }]"
      >
        <a-input v-model:value="formState.jsonKey" />
      </a-form-item>

      <!-- 修改这里为物模型选择框 -->
      <a-form-item
        label="物模型"
        name="modelId"
        :rules="[{ required: true, message: '请选择物模型!' }]"
      >
        <a-select
          v-model:value="formState.modelId"
          :options="modelOptions"
          placeholder="请选择物模型"
          allowClear
        />
      </a-form-item>

      <a-form-item
        label="功能描述"
        name="description"
        :rules="[{ required: true, message: '请输入功能描述!' }]"
      >
        <a-input v-model:value="formState.description" />
      </a-form-item>

      <a-form-item
        label="读写设置"
        name="rrw"
        :rules="[{ required: true, message: '请选择读写设置!' }]"
      >
        <a-select v-model:value="formState.rrw">
          <a-select-option value="0">只读</a-select-option>
          <a-select-option value="1">读写</a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item
        label="存储类型"
        name="storageType"
        :rules="[{ required: true, message: '请选择存储类型!' }]"
      >
        <a-select v-model:value="formState.storageType">
          <a-select-option value="permanent">永久</a-select-option>
          <a-select-option value="week">一周</a-select-option>
          <a-select-option value="never">从不</a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item
        label="数据类型"
        name="type"
        :rules="[{ required: true, message: '请选择数据类型!' }]"
      >
        <a-select v-model:value="formState.type">
          <a-select-option value="string">string</a-select-option>
          <a-select-option value="int">int</a-select-option>
          <a-select-option value="float">float</a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item label="最大值" name="max">
        <a-input v-model:value="formState.max" />
      </a-form-item>

      <a-form-item label="最小值" name="min">
        <a-input v-model:value="formState.min" />
      </a-form-item>

      <a-form-item label="步长" name="step">
        <a-input v-model:value="formState.step" />
      </a-form-item>

      <a-form-item label="单位" name="unit">
        <a-input v-model:value="formState.unit" />
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
import { postProductData } from '@/api/productData';
import { getproductModel } from '@/api/productModel';
import { getProductName } from '@/api/product';
import { useRouter } from 'vue-router';

const router = useRouter();
const visible = ref(false);

// 存放物模型下拉选项
const modelOptions = ref([]);

const showModal = () => {
  fetchproductModel(); // 打开弹窗时获取物模型列表
  visible.value = !visible.value;
};

const handleCancel = () => {
  visible.value = false;
};

const handleCreate = () => {
  visible.value = false;
};

const formState = reactive({
  description: '',
  jsonKey: '',
  modelId: null, // ⚡ 改成 null 而不是数字
  rrw: 0,
  storageType: '',
  type: '',
  max: null,
  min: null,
  step: null,
  unit: null
});

// 获取产品名称（用于显示物模型及关联产品）
const getProductNameById = async (productId) => {
  try {
    const res = await getProductName({ id: productId });
    const { data, errorCode } = res.data;
    if (errorCode === 200) {
      return data;
    }
    return `产品ID: ${productId}`;
  } catch (error) {
    console.error('获取产品名称失败:', error);
    return `产品ID: ${productId}`;
  }
};

// 获取物模型数据并处理
const fetchproductModel = async () => {
  try {
    const res = await getproductModel();
    const { data, errorCode } = res.data;
    if (errorCode === 2001) {
      router.push('/login');
      return;
    }

    const processedData = await Promise.all(
      data.map(async (item) => {
        const productName = await getProductNameById(item.productId);
        return {
          value: item.id,
          label: `${item.name} (${productName})`
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
  postProductData(toRaw(formState))
    .then((res) => {
      const { data, errorCode } = res.data;
      if (errorCode != 200) {
        message.error('添加失败!');
      } else {
        message.success('添加成功!');
        console.log(data);
      }
    })
    .catch((err) => {
      console.log(err);
      message.error('添加失败!');
    });
};

const onFinish = (values) => {
  console.log('Success:', values);
  handleSubmit();
  visible.value = false;
};

const onFinishFailed = (errorInfo) => {
  console.log('Failed:', errorInfo);
};
</script>