<template>
  <div class="table-container">
    <a-table :columns="columns" :data-source="dataSource" :pagination="pagination">
      <template #action="{ record }">
        <a-space>
          <a-button type="link" @click="handleEdit(record)">
            <template #icon><EditOutlined /></template>
            修改
          </a-button>
          <a-button type="link" danger @click="handleDelete(record)">
            <template #icon><DeleteOutlined /></template>
            删除
          </a-button>
        </a-space>
      </template>
    </a-table>

    <a-modal
      v-model:visible="editVisible"
      title="修改编码代理会话"
      :footer="null"
      @cancel="handleEditCancel"
    >
      <a-form :model="editForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }" @finish="handleEditSubmit">
        <a-form-item
          label="代理设备"
          name="agentId"
          :rules="[{ required: true, message: '请选择代理设备!' }]"
        >
          <a-select
            v-model:value="editForm.agentId"
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
          <a-input v-model:value="editForm.sessionId" />
        </a-form-item>
        <a-form-item label="cwd" name="cwd" :rules="[{ required: true, message: '请输入cwd!' }]">
          <a-input v-model:value="editForm.cwd" />
        </a-form-item>
        <a-form-item
          label="状态"
          name="finalStatus"
          :rules="[{ required: true, message: '请选择状态!' }]"
        >
          <a-select
            v-model:value="editForm.finalStatus"
            :options="statusOptions"
            placeholder="请选择状态"
            allowClear
          />
        </a-form-item>
        <a-form-item :wrapper-col="{ offset: 8, span: 16 }">
          <a-button @click="handleEditCancel">取消</a-button>
          <a-button type="primary" html-type="submit" style="margin-left: 8px">提交</a-button>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { EditOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import { getCodingAgentDevice } from '@/api/codingAgentDevice'
import {
  getCodingAgentSession,
  putCodingAgentSession,
  deleteCodingAgentSession,
} from '@/api/codingAgentSession'

const router = useRouter()
const pagination = { pageSize: 5 }
const dataSource = ref([])
const agentOptions = ref([])
const editVisible = ref(false)

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

const editForm = reactive({
  id: null,
  agentId: null,
  sessionId: '',
  cwd: '',
  finalStatus: '',
})

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id' },
  { title: '代理设备ID', dataIndex: 'agentId', key: 'agentId' },
  { title: 'sessionId', dataIndex: 'sessionId', key: 'sessionId' },
  { title: 'cwd', dataIndex: 'cwd', key: 'cwd' },
  { title: '状态', dataIndex: 'finalStatus', key: 'finalStatus' },
  { title: '更新时间', dataIndex: 'lastUpdate', key: 'lastUpdate' },
  { title: '操作', key: 'action', slots: { customRender: 'action' } },
]

const formatLocalDateTime = (value) => {
  if (!value) {
    return ''
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  const pad = (num) => String(num).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(
    date.getHours()
  )}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

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

const fetchCodingAgentSession = () => {
  getCodingAgentSession()
    .then((res) => {
      const { data, errorCode } = res.data
      if (errorCode === 2001) {
        router.push('/login')
        return
      }
      if (errorCode === 200 && Array.isArray(data)) {
        dataSource.value = data.map((item, index) => ({
          key: index,
          id: item.id,
          agentId: item.agentId,
          sessionId: item.sessionId,
          cwd: item.cwd,
          finalStatus: item.finalStatus,
          lastUpdate: formatLocalDateTime(item.lastUpdate),
        }))
      } else {
        dataSource.value = []
      }
    })
    .catch((err) => {
      console.log(err)
    })
}

let intervalId

onMounted(() => {
  fetchCodingAgentSession()
  intervalId = setInterval(fetchCodingAgentSession, 5000)
})

onUnmounted(() => {
  clearInterval(intervalId)
})

const handleEdit = (record) => {
  fetchCodingAgentDevice()
  editForm.id = record.id
  editForm.agentId = record.agentId
  editForm.sessionId = record.sessionId
  editForm.cwd = record.cwd
  editForm.finalStatus = record.finalStatus
  editVisible.value = true
}

const handleEditSubmit = () => {
  putCodingAgentSession({
    id: editForm.id,
    agentId: editForm.agentId,
    sessionId: editForm.sessionId,
    cwd: editForm.cwd,
    finalStatus: editForm.finalStatus,
  })
    .then((res) => {
      const { errorCode } = res.data
      if (errorCode === 200) {
        message.success('修改成功')
        editVisible.value = false
        fetchCodingAgentSession()
      } else if (errorCode === 2001) {
        router.push('/login')
      } else {
        message.error('修改失败')
      }
    })
    .catch((err) => {
      console.log(err)
      message.error('修改失败')
    })
}

const handleEditCancel = () => {
  editVisible.value = false
}

const handleDelete = (record) => {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除这条编码代理会话记录吗？',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk() {
      deleteCodingAgentSession({ id: record.id })
        .then((res) => {
          const { errorCode } = res.data
          if (errorCode === 200) {
            message.success('删除成功')
            fetchCodingAgentSession()
          } else if (errorCode === 2001) {
            router.push('/login')
          } else {
            message.error('删除失败')
          }
        })
        .catch((err) => {
          console.log(err)
          message.error('删除失败')
        })
    },
  })
}

defineExpose({
  fetchCodingAgentSession,
})
</script>
