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
        label="名称"
        name="name"
        :rules="[{ required: true, message: 'Please input your name!' }]"
      >
        <a-input v-model:value="formState.name" />
      </a-form-item>
      <a-form-item
        label="产品id"
        name="productId"
        :rules="[{ required: true, message: 'Please input your productId!' }]"
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
        :rules="[{ required: true, message: 'Please input your description!' }]"
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
  import { reactive,ref,toRaw,onMounted} from 'vue';
  import { postProductModel } from '@/api/productModel';
  import { message } from 'ant-design-vue'
  import { getProduct} from '@/api/product';
  const visible = ref(false)
  const options = ref([]);
  const showModal = () => {
  fetchProduct()
  visible.value = !visible.value
}

const handleCancel = () => {
  visible.value = false
}

const handleCreate = () => {
  visible.value = false
}
  const formState = reactive({
  description: "",
  name: "",
  productId: ""
}
);
const fetchProduct = () => {
  getProduct()
    .then((res) => {
      const { data, errorCode } = res.data;
      if(errorCode==2001){
        router.push('/login')
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
const handleSubmit = () => {
      postProductModel(toRaw(formState))
        .then((res) => {
          const { data ,errorCode} = res.data
          //data = data.substring(7)
          console.log('auth', data)
          if (errorCode!=200) {
            message.error("创建失败!")
            console.log("error")
          } else {
            console.log(data)
          }
        })
        .catch((err) => {
          console.log(err)
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