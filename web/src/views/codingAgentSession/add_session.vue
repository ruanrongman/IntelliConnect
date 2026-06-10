<template>
  <a-button type="primary" @click="showModal">
    <template #icon><PlusOutlined /></template>
    新建会话
  </a-button>
  <a-modal :visible="visible" title="新建编码代理会话" :footer="null" @cancel="handleCancel">
    <a-form
      :model="formState"
      name="codingAgentSessionAdd"
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 16 }"
      autocomplete="off"
      @finish="onFinish"
    >
      <a-form-item
        label="代理设备"
        name="agentId"
        :rules="[{ required: true, message: '请选择代理设备!' }]"
      >
        <a-select
          v-model:value="formState.agentId"
          :options="agentOptions"
          placeholder="请选择代理设备"
          allowClear
        />
      </a-form-item>

      <a-form-item
        label="sessionId"
        name="sessionId"
        :rules="[{ required: true, message: '请输入sessionId!' }]"
      >
        <a-input v-model:value="formState.sessionId" />
      </a-form-item>

      <a-form-item label="cwd" name="cwd" :rules="[{ required: true, message: '请输入cwd!' }]">
        <a-input v-model:value="formState.cwd" />
      </a-form-item>

      <a-form-item
        label="状态"
        name="finalStatus"
        :rules="[{ required: true, message: '请选择状态!' }]"
      >
        <a-select
          v-model:value="formState.finalStatus"
          :options="statusOptions"
          placeholder="请选择状态"
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
import { getCodingAgentDevice } from '@/api/codingAgentDevice'
import { postCodingAgentSession } from '@/api/codingAgentSession'

const emit = defineEmits(['created'])
const router = useRouter()
const visible = ref(false)
const agentOptions = ref([])

const statusOptions = [
  { value: 'READY', label: 'READY' },
  { value: 'THINKING', label: 'THINKING' },
  { value: 'WORKING', label: 'WORKING' },
  { value: 'PROCESSING', label: 'PROCESSING' },
  { value: 'WARNING', label: 'WARNING' },
  { value: 'FINISHED', label: 'FINISHED' },
  { value: 'ERROR', label: 'ERROR' },
  { value: 'OFFLINE', label: 'OFFLINE' },
]

const formState = reactive({
  agentId: null,
  sessionId: '',
  cwd: '',
  finalStatus: '',
})

const fetchCodingAgentDevice = () => {
  getCodingAgentDevice()
    .then((res) => {
      const { data, errorCode } = res.data
      if (errorCode === 2001) {
        router.push('/login')
        return
      }
      agentOptions.value = (data || []).map((item) => ({
        value: item.id,
        label: `${item.id} / ${item.agentType} / ${item.pairCode}`,
      }))
    })
    .catch((err) => {
      console.log(err)
    })
}

const showModal = () => {
  fetchCodingAgentDevice()
  visible.value = true
}

const handleCancel = () => {
  visible.value = false
}

const handleSubmit = () => {
  postCodingAgentSession(toRaw(formState))
    .then((res) => {
      const { errorCode } = res.data
      if (errorCode === 200) {
        message.success('创建成功')
        formState.agentId = null
        formState.sessionId = ''
        formState.cwd = ''
        formState.finalStatus = ''
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
