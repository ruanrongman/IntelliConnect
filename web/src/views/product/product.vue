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
      label="产品名称(不重复)"
      name="productName"
      :rules="[{ required: true, message: 'Please input your productName!' }]"
    >
      <a-input v-model:value="formState.productName" />
    </a-form-item>

    <a-form-item
      label="密钥"
      name="keyvalue"
      :rules="[{ required: true, message: 'Please input your keyvalue!' }]"
    >
      <a-input v-model:value="formState.keyvalue" />
    </a-form-item>
    <a-form-item
      label="register"
      name="register"
     :rules="[{ required: true, message: 'Please input your register!' }]"
    >
      <a-select
       v-model:value="formState.register"
      >
         <a-select-option value="0">未注册</a-select-option>
         <a-select-option value="1">已注册</a-select-option>
      </a-select>
    </a-form-item> 
    
    <a-form-item :wrapper-col="{ offset: 8, span: 16 }">
      <a-button type="primary" html-type="submit">Submit</a-button>
    </a-form-item>
  </a-form>
  </a-modal>
</template>
<script setup>
import { reactive,ref,toRaw } from 'vue';
import { message } from 'ant-design-vue'
import { postProduct } from '@/api/product';
const visible = ref(false)
const showModal = () => {
visible.value = !visible.value
}

const handleCancel = () => {
visible.value = false
}

const handleCreate = () => {
visible.value = false
}
const formState = reactive({
 keyvalue: "",
 productName: "",
 register: 0
});
const handleSubmit = () => {
    postProduct(toRaw(formState))
      .then((res) => {
        const { data ,errorCode} = res.data
        //data = data.substring(7)
        console.log('auth', data)
        if (errorCode!=200) {
          message.error("创建失败!")
          console.log("error")
        } else {
          message.success("创建成功!")
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