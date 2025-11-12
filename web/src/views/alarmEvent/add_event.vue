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
    
      <!-- 修改这里为事件选择框 -->
      <a-form-item
        label="事件名称"
        name="name"
        :rules="[{ required: true, message: '请选择事件名称!' }]"
      >
        <a-select
          v-model:value="formState.name"
          :options="eventOptions"
          placeholder="请选择事件"
          allowClear
        />
      </a-form-item>
      
      <!-- 物模型选择框 -->
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
      
      <a-form-item :wrapper-col="{ offset: 8, span: 16 }">
        <a-button type="primary" html-type="submit">Submit</a-button>
      </a-form-item>
    </a-form>
    </a-modal>
  </template>
  <script setup>
  import { reactive,ref,toRaw} from 'vue';
  import { postAlarmEvent } from '@/api/alarmEvent';
  import { message } from 'ant-design-vue'
  import { getproductModel } from '@/api/productModel';
  import { getProductName } from '@/api/product';
  import { getProductEvent } from '@/api/ProductEvent'; // 引入获取事件列表的API
  import { useRouter } from 'vue-router';

  const router = useRouter();
  const visible = ref(false)
  // 存放物模型下拉选项
  const modelOptions = ref([]);
  // 存放事件下拉选项
  const eventOptions = ref([]);
  
  const showModal = () => {
    fetchproductModel(); // 获取物模型列表
    fetchEventList(); // 获取事件列表
    visible.value = true;
  }

  const handleCancel = () => {
    visible.value = false
  }

  const handleCreate = () => {
    visible.value = false
  }
  
  const formState = reactive({
    description: "",
    name: null, // 改为 null
    modelId: null
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

  // 获取事件列表并处理
  const fetchEventList = () => {
    getProductEvent()
      .then((res) => {
        const { data, errorCode } = res.data;
        if (errorCode === 2001) {
          router.push('/login');
          return;
        }
        
        if (errorCode === 200 && data && Array.isArray(data)) {
          eventOptions.value = data.map(item => ({
            value: item.name,
            label: item.name
          }));
        } else {
          eventOptions.value = [];
        }
      })
      .catch((err) => {
        console.error('获取事件列表失败:', err);
        message.error('获取事件列表失败');
      });
  };

  const handleSubmit = () => {
    postAlarmEvent(toRaw(formState))
        .then((res) => {
          const { data ,errorCode} = res.data
          if (errorCode != 200) {
            message.error("创建失败!")
            console.log("error")
          } else {
            message.success("创建成功!")
            console.log(data)
          }
        })
        .catch((err) => {
          console.log(err)
          message.error("创建失败!")
        })
  }
  
  const onFinish = values => {
    console.log('Success:', values);
    handleSubmit()
    visible.value = false
  };
  
  const onFinishFailed = errorInfo => {
    console.log('Failed:', errorInfo);
  };
  </script>
