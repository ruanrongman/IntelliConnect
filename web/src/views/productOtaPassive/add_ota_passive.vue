<template>  
  <a-button type="primary" @click="showModal">  
    新建/修改配置  
  </a-button>  
  <a-modal   
    :visible="visible"    
    :footer="null"  
    :closable="!submitting"  
    :mask-closable="!submitting"  
    @cancel="handleCancel"  
    title="新建/修改配置"  
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
        label="固件"  
        name="name"  
        :rules="[{ required: true, message: '请选择固件!' }]"  
      >  
        <a-select  
          v-model:value="formState.name"  
          :options="firmwareOptions"  
          placeholder="请选择固件"  
          :disabled="submitting"  
          :loading="loadingFirmware"  
          allowClear  
          show-search  
          :filter-option="filterOption"  
        />  
      </a-form-item>  

      <a-form-item  
        label="设备"  
        name="deviceName"  
        :rules="[{ required: true, message: '请选择设备!' }]"  
      >  
        <a-select  
          v-model:value="formState.deviceName"  
          :options="deviceOptions"  
          placeholder="请选择设备"  
          :disabled="submitting"  
          :loading="loadingDevices"  
          allowClear  
          show-search  
          :filter-option="filterOption"  
        />  
      </a-form-item>  

      <a-form-item  
        label="版本名称"  
        name="versionName"  
        :rules="[{ required: true, message: '请输入版本名称!' }]"  
      >  
        <a-input v-model:value="formState.versionName" :disabled="submitting" />  
      </a-form-item>  

      <a-form-item
      label="固件描述"
      name="description"
      :rules="[
        { required: true, message: 'Please input your description!' },
        { max: 2000, message: '输入内容不能超过2000字符!' }
      ]"
    >
      <a-textarea 
        v-model:value="formState.description" 
        :rows="6"
        :maxlength="2000"
        show-count
        placeholder="请输入待升级的固件描述..."
      />
    </a-form-item>


      <a-form-item :wrapper-col="{ offset: 8, span: 16 }">  
        <a-button   
          type="primary"   
          html-type="submit"  
          :loading="submitting"  
          :disabled="submitting"  
        >  
          {{ submitting ? '提交中...' : '提交' }}  
        </a-button>  
        <a-button   
          style="margin-left: 10px"   
          @click="handleCancel"  
          :disabled="submitting"  
        >  
          取消  
        </a-button>  
      </a-form-item>  
    </a-form>  
  </a-modal>  
</template>  

<script setup>  
import { reactive, ref, toRaw } from 'vue'  
import { message } from 'ant-design-vue'  
import { useRouter } from 'vue-router'  
import { postOtaPassive } from '@/api/productOtaPassive'  
import { otaList } from '@/api/productOta'  
import { getProductDevice } from '@/api/device'  
import { getProductName } from '@/api/product' // 添加获取产品名称的API

const router = useRouter()  

// 模态框控制  
const visible = ref(false)  

// 提交状态  
const submitting = ref(false)  

// 下拉选项数据  
const firmwareOptions = ref([])  
const deviceOptions = ref([])  

// 加载状态  
const loadingFirmware = ref(false)  
const loadingDevices = ref(false)  

// 表单数据  
const formState = reactive({  
  name: '',        // 固件名称  
  deviceName: '',  // 设备名称  
  versionName: '', // 版本名称  
  description: ''  // 固件描述
})  

// 显示模态框  
const showModal = () => {
  resetForm()
  Promise.all([fetchFirmwareList(), fetchDeviceList()])
  visible.value = true  
}  

// 取消操作  
const handleCancel = () => {  
  if (submitting.value) {  
    message.warning('正在提交中，请稍候...')  
    return  
  }  
  resetForm()  
  visible.value = false  
}  

// 重置表单  
const resetForm = () => {  
  formState.name = ''  
  formState.deviceName = ''  
  formState.versionName = ''  
}  

// 获取产品名称的方法
const getProductNameById = async (productId) => {
  try {
    const res = await getProductName({ id: productId })
    const { data, errorCode } = res.data
    if (errorCode === 200) {
      return data
    }
    return `产品ID: ${productId}`
  } catch (error) {
    console.error('获取产品名称失败:', error)
    return `产品ID: ${productId}`
  }
}

// 获取固件列表（添加产品名称）
const fetchFirmwareList = async () => {  
  if (loadingFirmware.value) return  
    
  loadingFirmware.value = true  
  try {  
    const res = await otaList()  
    const { data, errorCode } = res.data  
      
    if (errorCode === 2001) {  
      message.error('登录已过期，请重新登录')  
      router.push('/login')  
      return  
    }  
      
    if (errorCode === 200) {  
      // 处理数据并获取产品名称
      const processedData = await Promise.all(
        data.map(async (item) => {
          // 假设固件数据中有 productId 字段
          let productName = ''
          if (item.productId) {
            productName = await getProductNameById(item.productId)
          }
          
          return {
            value: item.name,
            label: productName 
              ? `${item.name} (${productName})` 
              : item.name,
            title: `${item.name} (路径: ${item.path})${productName ? ` - ${productName}` : ''}`
          }
        })
      )
      
      firmwareOptions.value = processedData
    } else {  
      message.error('获取固件列表失败')  
      firmwareOptions.value = []  
    }  
  } catch (error) {  
    console.error('获取固件列表错误:', error)  
    message.error('获取固件列表失败')  
    firmwareOptions.value = []  
  } finally {  
    loadingFirmware.value = false  
  }  
}  

// 获取设备列表
const fetchDeviceList = async () => {  
  if (loadingDevices.value) return  
    
  loadingDevices.value = true  
  try {  
    const res = await getProductDevice()  
    const { data, errorCode } = res.data  
      
    if (errorCode === 2001) {  
      message.error('登录已过期，请重新登录')  
      router.push('/login')  
      return  
    }  
      
    if (errorCode === 200) {  
      // 如果设备数据中也有 productId，可以同样处理
      const processedData = await Promise.all(
        data.map(async (item) => {
          let productName = ''
          if (item.productId) {
            productName = await getProductNameById(item.productId)
          }
          
          return {
            value: item.name,
            label: productName 
              ? `${item.name} (${productName})` 
              : item.name,
          }
        })
      )
      
      deviceOptions.value = processedData
    } else {  
      message.error('获取设备列表失败')  
      deviceOptions.value = []  
    }  
  } catch (error) {  
    console.error('获取设备列表错误:', error)  
    message.error('获取设备列表失败')  
    deviceOptions.value = []  
  } finally {  
    loadingDevices.value = false  
  }  
}  

// 搜索过滤函数  
const filterOption = (input, option) => {  
  return option.label.toLowerCase().includes(input.toLowerCase())  
}  

// 处理提交  
const handleSubmit = async () => {  
  submitting.value = true  
    
  try {  
    const hideMessage = message.loading('正在提交配置，请稍候...', 0)  
      
    const submitData = toRaw(formState)  
      
    console.log('提交数据:', submitData)  
      
    const res = await postOtaPassive(submitData)  
      
    hideMessage()  
      
    const { data, errorCode } = res.data  
    console.log('提交响应:', data)  
      
    if (errorCode === 200) {  
      message.success('配置创建成功!')  
      resetForm()  
      visible.value = false  
    } else if (errorCode === 2001) {  
      message.error('登录已过期，请重新登录')  
      router.push('/login')  
    } else {  
      message.error(`创建失败: ${res.data.message || '未知错误'}`)  
    }  
      
  } catch (error) {  
    console.error('提交错误:', error)  
    message.error(`创建失败: ${error.message || '网络错误'}`)  
  } finally {  
    submitting.value = false  
  }  
}  

// 表单提交成功  
const onFinish = (values) => {  
  console.log('表单验证成功:', values)  
  handleSubmit()  
}  

// 表单提交失败  
const onFinishFailed = (errorInfo) => {  
  console.log('表单验证失败:', errorInfo)  
  message.error('请检查表单信息')  
}  
</script>