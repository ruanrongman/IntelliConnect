<template>  
  <a-button type="primary" @click="showModal">  
    绑定/解绑产品  
  </a-button>  
  <a-modal 
    :visible="visible"    
    :footer="null"  
    @cancel="handleCancel"
    :title="modalTitle"
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
        label="产品名称"  
        name="productName"  
        :rules="[{ required: true, message: 'Please input your productName!' }]"  
      >  
        <a-input v-model:value="formState.productName" />  
      </a-form-item>  
  
      <a-form-item  
        label="产品密钥"  
        name="productKey"  
        :rules="[{ required: true, message: 'Please input your productKey!' }]"  
      >  
        <a-input v-model:value="formState.productKey" />  
      </a-form-item>  
        
      <a-form-item :wrapper-col="{ offset: 6, span: 16 }">  
        <a-space>
          <a-button type="primary" html-type="submit">{{ submitButtonText }}</a-button>
          <a-button v-if="isBindMode" @click="switchToUnbind">切换到解绑</a-button>
          <a-button v-else @click="switchToBind">切换到绑定</a-button>
        </a-space>
      </a-form-item>  
    </a-form>  
  </a-modal>  
</template>  

<script setup>  
import { reactive, ref, toRaw, computed } from 'vue';  
import { message } from 'ant-design-vue'  
import { postProductBind, postProductUnbind } from '@/api/productBind';  

const visible = ref(false)  
const isBindMode = ref(true) // true: 绑定模式, false: 解绑模式

// 计算属性
const modalTitle = computed(() => isBindMode.value ? '绑定产品' : '解绑产品')
const submitButtonText = computed(() => isBindMode.value ? '绑定' : '解绑')

const showModal = () => {  
  visible.value = true
  isBindMode.value = true // 默认打开绑定模式
}  

const handleCancel = () => {  
  visible.value = false
  resetForm()
}

const resetForm = () => {
  formState.productKey = ''
  formState.productName = ''
  isBindMode.value = true
}

const switchToBind = () => {
  isBindMode.value = true
}

const switchToUnbind = () => {
  isBindMode.value = false
}

const formState = reactive({  
  productKey: "",  
  productName: "",  
});  

const handleBind = () => {
  postProductBind(toRaw(formState))  
    .then((res) => {  
      const { data, errorCode } = res.data  
      console.log('bind result:', data)  
      if (errorCode != 200) {  
        message.error("绑定失败!")  
        console.log("error")  
      } else {  
        message.success("绑定成功!")  
        console.log(data)
        visible.value = false
        resetForm()
      }  
    })  
    .catch((err) => {  
      console.log(err)
      message.error("绑定失败!")
    })  
}

const handleUnbind = () => {
  postProductUnbind(toRaw(formState))  
    .then((res) => {  
      const { data, errorCode } = res.data  
      console.log('unbind result:', data)  
      if (errorCode != 200) {  
        message.error("解绑失败!")  
        console.log("error")  
      } else {  
        message.success("解绑成功!")  
        console.log(data)
        visible.value = false
        resetForm()
      }  
    })  
    .catch((err) => {  
      console.log(err)
      message.error("解绑失败!")
    })  
}

const handleSubmit = () => {
  if (isBindMode.value) {
    handleBind()
  } else {
    handleUnbind()
  }
}

const onFinish = values => {  
  console.log('Success:', values);  
  handleSubmit()
};  

const onFinishFailed = errorInfo => {  
  console.log('Failed:', errorInfo);  
};  
</script>