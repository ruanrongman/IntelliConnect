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
        label="功能名称"
        name="functionName"
        :rules="[{ required: true, message: '请输入功能名称!' }]"
      >
        <a-input v-model:value="formState.functionName" />
      </a-form-item>
      <a-form-item
        label="输入/输出"
        name="dataType"
       :rules="[{ required: true, message: '请选择输入/输出类型!' }]"
      >
        <a-select
         v-model:value="formState.dataType"
        >
           <a-select-option value="input">输入</a-select-option>
           <a-select-option value="output">输出</a-select-option>
        </a-select>
      </a-form-item> 
      <a-form-item
        label="参数名称"
        name="jsonKey"
        :rules="[{ required: true, message: '请输入参数名称!' }]"
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
        label="数据类型"
        name="type"
       :rules="[{ required: true, message: '请选择数据类型!' }]"
      >
        <a-select
         v-model:value="formState.type"
        >
           <a-select-option value="string">string</a-select-option>
           <a-select-option value="int">int</a-select-option>
           <a-select-option value="float">float</a-select-option>
        </a-select>
      </a-form-item> 
      <a-form-item
        label="最大值"
        name="max"
        :rules="[{ required: false, message: '请输入最大值!' }]"
      >
        <a-input v-model:value="formState.max" />
      </a-form-item>
      <a-form-item
        label="最小值"
        name="min"
        :rules="[{ required: false, message: '请输入最小值!' }]"
      >
        <a-input v-model:value="formState.min" />
      </a-form-item>
      <a-form-item
        label="步长"
        name="step"
        :rules="[{ required: false, message: '请输入步长!' }]"
      >
        <a-input v-model:value="formState.step" />
      </a-form-item>
      <a-form-item
        label="单位"
        name="unit"
        :rules="[{ required: false, message: '请输入单位!' }]"
      >
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
  import { postProductFunction } from '@/api/productFunction';
  // 引入获取物模型和产品名称的API
  import { getproductModel } from '@/api/productModel';
  import { getProductName } from '@/api/product';
  import { useRouter } from 'vue-router';

  const router = useRouter();
  const visible = ref(false);
  // 存放物模型下拉选项
  const modelOptions = ref([]);

  const showModal = () => {
    fetchproductModel(); // 打开弹窗时获取物模型列表
    visible.value = true;
  };

  const handleCancel = () => {
    visible.value = false;
  };

  const handleCreate = () => {
    visible.value = false;
  };

  const formState = reactive({
    functionName : "",
    dataType: "",
    description: "",
    jsonKey: "",
    modelId: null, // ⚡ 改成 null 而不是数字
    type: "",
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
    postProductFunction(toRaw(formState))
      .then((res) => {
        const { data, errorCode } = res.data;
        console.log('auth', data);
        if (errorCode != 200) {
          message.error("添加失败!");
          console.log("error");
        } else {
          message.success("添加成功!");
          console.log(data);
        }
      })
      .catch((err) => {
        console.log(err);
        message.error("添加失败!");
      })
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
