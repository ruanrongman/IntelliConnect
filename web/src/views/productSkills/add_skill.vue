<template>
  <a-button type="primary" @click="showModal">
    <template #icon><PlusOutlined /></template>
    添加技能
  </a-button>
  <a-modal
    v-model:visible="visible"
    title="添加产品技能"
    @ok="handleSubmit"
    @cancel="handleCancel"
    :confirm-loading="submitLoading"
  >
    <a-form
      :model="formState"
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 16 }"
    >
      <a-form-item label="产品" required>
        <a-select
          v-model:value="formState.productId"
          :options="options"
          placeholder="请选择产品"
          allowClear
        />
      </a-form-item>
      
      <a-form-item label="技能文件" required>
        <a-upload
          v-model:file-list="formState.file"
          :before-upload="beforeUpload"
          :max-count="1"
          accept=".md"
        >
          <a-button>
            <template #icon><UploadOutlined /></template>
            选择文件
          </a-button>
        </a-upload>
        <div style="color: #999; font-size: 12px; margin-top: 8px;">
          请上传包含技能定义的 Markdown (.md) 文件，大小不超过50MB
        </div>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, UploadOutlined } from '@ant-design/icons-vue'
import { addProductSkill } from '@/api/productSkills'
import { getProduct } from '@/api/product'
import { useRouter } from 'vue-router'

const router = useRouter()

const visible = ref(false)
const submitLoading = ref(false)
const options = ref([])
const selectedFile = ref(null)

const formState = reactive({
  productId: undefined,
  file: []
})

// Fetch products for dropdown
const fetchProducts = () => {
  getProduct().then((res) => {
    const { data, errorCode } = res.data
    if(errorCode == 2001){
      router.push('/login')
    } else if(errorCode == 200){
      options.value = data.map((item) => ({
        value: item.id,
        label: item.productName,
      }))
    }
  }).catch((err) => {
    console.log(err)
  })
}

onMounted(() => {
  fetchProducts()
})

const showModal = () => {
  formState.productId = undefined
  formState.file = []
  selectedFile.value = null
  visible.value = true
}

const beforeUpload = (file) => {
  // Check file size (50MB limit)
  const isLt50M = file.size / 1024 / 1024 < 50
  if (!isLt50M) {
    message.error('文件大小不能超过50MB')
    return false
  }

  // Check file type
  const fileExtension = '.' + file.name.split('.').pop().toLowerCase()
  const allowedExtensions = ['.md']
  if (!allowedExtensions.includes(fileExtension)) {
    message.error('只允许上传 Markdown (.md) 文件')
    return false
  }

  // Store the file object
  selectedFile.value = file
  formState.file = [file]
  return false // Prevent auto upload
}

const handleCancel = () => {
  visible.value = false
  formState.productId = undefined
  formState.file = []
  selectedFile.value = null
}

const handleSubmit = async () => {
  if (!formState.productId) {
    message.error('请选择产品')
    return
  }

  const file = formState.file?.[0]
  if (!file) {
    message.error('请选择技能文件')
    return
  }

  submitLoading.value = true

  try {
    // 获取原始文件对象，类似于 add_knowledge_chat.vue
    const fileToUpload = file.originFileObj || file
    const response = await addProductSkill(formState.productId, fileToUpload)

    if (response.data.errorCode === 200) {
      message.success('技能添加成功')
      visible.value = false
      formState.productId = undefined
      formState.file = []
      selectedFile.value = null
      // Data will be refreshed automatically by the table component
    } else {
      message.error(response.data.message || '操作失败')
    }
  } catch (error) {
    console.error('Error submitting skill:', error)
    message.error('提交技能时出错')
  } finally {
    submitLoading.value = false
  }
}
</script>