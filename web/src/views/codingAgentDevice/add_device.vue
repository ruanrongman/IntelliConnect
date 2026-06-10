<template>
  <a-button type="primary" @click="showModal">
    <template #icon><PlusOutlined /></template>
    新建设备
  </a-button>
  <a-modal :visible="visible" :footer="null" @cancel="handleCancel">
    <a-form
      :model="formState"
      name="codingAgentDeviceAdd"
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 16 }"
      autocomplete="off"
      @finish="onFinish"
    >
      <a-form-item
        label="产品"
        name="productId"
        :rules="[{ required: true, message: '请选择产品!' }]"
      >
        <a-select
          v-model:value="formState.productId"
          :options="options"
          placeholder="请选择产品"
          allowClear
        />
      </a-form-item>

      <a-form-item
        label="pairCode"
        name="pairCode"
        :rules="[{ required: true, message: '请输入pairCode!' }]"
      >
        <a-input v-model:value="formState.pairCode" />
      </a-form-item>

      <a-form-item
        label="agentType"
        name="agentType"
        :rules="[{ required: true, message: '请输入agentType!' }]"
      >
        <a-select
          v-model:value="formState.agentType"
          :options="agentTypeOptions"
          placeholder="请选择agentType"
          allowClear
        />
      </a-form-item>

      <a-form-item :wrapper-col="{ offset: 8, span: 16 }">
        <a-button type="primary" html-type="submit">提交</a-button>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { reactive, ref, toRaw } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { getProduct } from '@/api/product'
import { postCodingAgentDevice } from '@/api/codingAgentDevice'

const emit = defineEmits(['created'])
const router = useRouter()
const visible = ref(false)
const options = ref([])

const formState = reactive({
  productId: '',
  pairCode: '',
  agentType: 'CODEX',
})

const agentTypeOptions = [
  { value: 'CODEX', label: 'CODEX' },
  { value: 'CLAUDE_CODE', label: 'CLAUDE_CODE' },
]

const fetchProduct = () => {
  getProduct()
    .then((res) => {
      const { data, errorCode } = res.data
      if (errorCode === 2001) {
        router.push('/login')
        return
      }
      options.value = (data || []).map((item) => ({
        value: item.id,
        label: item.productName,
      }))
    })
    .catch((err) => {
      console.log(err)
    })
}

const showModal = () => {
  fetchProduct()
  visible.value = true
}

const handleCancel = () => {
  visible.value = false
}

const handleSubmit = () => {
  postCodingAgentDevice(toRaw(formState))
    .then((res) => {
      const { errorCode } = res.data
      if (errorCode === 200) {
        message.success('创建成功')
        formState.productId = ''
        formState.pairCode = ''
        formState.agentType = 'CODEX'
        visible.value = false
        emit('created')
      } else if (errorCode === 2001) {
        router.push('/login')
      } else {
        message.error('创建失败')
      }
    })
    .catch((err) => {
      console.log(err)
      message.error('创建失败，请重试')
    })
}

const onFinish = () => {
  handleSubmit()
}
</script>
