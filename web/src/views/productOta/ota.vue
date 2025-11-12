<template>
  <a-button type="primary" @click="showModal">
    上传固件
  </a-button>
  <a-modal 
    :visible="visible"  
    :footer="null"
    :closable="!submitting"
    :mask-closable="!submitting"
    @cancel="handleCancel"
    title="上传固件"
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
        label="固件名称"
        name="name"
        :rules="[{ required: true, message: '请输入固件名称!' }]"
      >
        <a-input v-model:value="formState.name" :disabled="submitting" />
      </a-form-item>

      <a-form-item
        label="产品"
        name="productId"
        :rules="[{ required: true, message: '请选择产品!' }]"
      >
        <a-select
          v-model:value="formState.productId"
          :options="options"
          placeholder="请选择产品"
          :disabled="submitting"
          :loading="loadingProducts"
          allowClear
        />
      </a-form-item>
      
      <a-form-item
        label="固件文件"
        name="file"
        :rules="[{ required: true, message: '请上传固件文件！' }]"
      >
        <a-upload
          v-model:file-list="formState.file"
          :max-count="1"
          :multiple="false"
          :before-upload="handleBeforeUpload"
          :show-upload-list="true"
          :disabled="submitting"
          @remove="handleRemove"
          accept=".bin"
        >
          <a-button :disabled="submitting">
            <upload-outlined />
            选择 .bin 文件 (限制200MB)
          </a-button>
        </a-upload>
      </a-form-item>
      
      <a-form-item :wrapper-col="{ offset: 8, span: 16 }">
        <a-button 
          type="primary" 
          html-type="submit" 
          :loading="submitting"
          :disabled="submitting"
        >
          {{ submitting ? '上传中...' : '提交' }}
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
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { UploadOutlined } from '@ant-design/icons-vue'
import { uploadFirmware } from '@/api/productOta'
import { getProduct } from '@/api/product'
import { useRouter } from 'vue-router'

const router = useRouter()

// 模态框控制
const visible = ref(false)

// 上传状态
const submitting = ref(false)

// 产品选择相关
const options = ref([])
const loadingProducts = ref(false)

// 选中的文件
const selectedFile = ref(null)

// 表单数据
const formState = reactive({
  name: '',
  productId: undefined,
  file: []
})

// 显示模态框
const showModal = () => {
  fetchProduct()
  visible.value = true
}

// 取消操作
const handleCancel = () => {
  if (submitting.value) {
    message.warning('正在上传中，请稍候...')
    return
  }
  resetForm()
  visible.value = false
}

// 重置表单
const resetForm = () => {
  formState.name = ''
  formState.productId = undefined
  formState.file = []
  selectedFile.value = null
}

// 获取产品列表
const fetchProduct = async () => {
  if (loadingProducts.value) return
  
  loadingProducts.value = true
  try {
    const res = await getProduct()
    const { data, errorCode } = res.data
    
    if (errorCode === 2001) {
      message.error('登录已过期，请重新登录')
      router.push('/login')
      return
    }
    
    if (errorCode === 200) {
      options.value = data.map((item) => ({
        value: item.id,
        label: item.productName,
      }))
    } else {
      message.error('获取产品列表失败')
      options.value = []
    }
  } catch (error) {
    console.error('获取产品列表错误:', error)
    message.error('获取产品列表失败')
    options.value = []
  } finally {
    loadingProducts.value = false
  }
}

// 文件上传前拦截
const handleBeforeUpload = (file) => {
  // 检查文件类型
  if (!file.name.toLowerCase().endsWith('.bin')) {
    message.error('只能上传 .bin 格式的文件')
    return false
  }
  
  // 检查文件大小 - 限制200MB
  const isLt200M = file.size / 1024 / 1024 < 200
  if (!isLt200M) {
    message.error('文件大小不能超过 200MB')
    return false
  }
  
  if (formState.file.length >= 1) {
    message.warning('只能上传一个文件')
    return false
  }
  
  selectedFile.value = file
  formState.file = [file]
  return false
}

// 移除文件
const handleRemove = () => {
  selectedFile.value = null
  formState.file = []
  return true
}

// 处理提交
const handleSubmit = async () => {
  const file = formState.file?.[0]
  if (!file) {
    message.error('请先上传文件')
    return
  }

  if (!formState.productId) {
    message.error('请选择产品')
    return
  }

  submitting.value = true
  
  try {
    const hideMessage = message.loading('正在上传固件，请稍候...', 0)
    
    const params = {
      name: formState.name,
      productId: formState.productId
    }
    
    const res = await uploadFirmware(file.originFileObj || file, params)
    
    hideMessage()
    
    console.log('上传响应:', res.data)
    
    const { errorCode } = res.data
    
    if (errorCode === 200) {
      message.success('固件上传成功!')
      resetForm()
      visible.value = false
    } else if (errorCode === 2001) {
      message.error('登录已过期，请重新登录')
      router.push('/login')
    } else {
      message.error(`上传失败: ${res.data.message || '未知错误'}`)
    }
    
  } catch (error) {
    console.error('上传错误:', error)
    message.error(`上传失败: ${error.message || '网络错误'}`)
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

<style scoped>
.ant-upload {
  width: 100%;
}
</style>