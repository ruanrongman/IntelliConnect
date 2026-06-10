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
      title="修改编码代理设备"
      :footer="null"
      @cancel="handleEditCancel"
    >
      <a-form :model="editForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }" @finish="handleEditSubmit">
        <a-form-item label="产品" name="productId" :rules="[{ required: true, message: '请选择产品!' }]">
          <a-select
            v-model:value="editForm.productId"
            :options="productOptions"
            placeholder="请选择产品"
            allowClear
          />
        </a-form-item>
        <a-form-item label="pairCode" name="pairCode" :rules="[{ required: true, message: '请输入pairCode!' }]">
          <a-input v-model:value="editForm.pairCode" />
        </a-form-item>
        <a-form-item label="agentType" name="agentType" :rules="[{ required: true, message: '请输入agentType!' }]">
          <a-select
            v-model:value="editForm.agentType"
            :options="agentTypeOptions"
            placeholder="请选择agentType"
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
import { getCodingAgentDevice, putCodingAgentDevice, deleteCodingAgentDevice } from '@/api/codingAgentDevice'
import { getProduct } from '@/api/product'

const router = useRouter()
const pagination = { pageSize: 5 }
const dataSource = ref([])
const productOptions = ref([])
const agentTypeOptions = [
  { value: 'CODEX', label: 'CODEX' },
  { value: 'CLAUDE_CODE', label: 'CLAUDE_CODE' },
]
const editVisible = ref(false)

const editForm = reactive({
  id: null,
  productId: null,
  pairCode: '',
  agentType: '',
})

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id' },
  { title: '产品ID', dataIndex: 'productId', key: 'productId' },
  { title: 'pairCode', dataIndex: 'pairCode', key: 'pairCode' },
  { title: 'agentType', dataIndex: 'agentType', key: 'agentType' },
  { title: '操作', key: 'action', slots: { customRender: 'action' } },
]

const fetchProduct = () => {
  getProduct()
    .then((res) => {
      const { data, errorCode } = res.data
      if (errorCode === 2001) {
        router.push('/login')
        return
      }
      productOptions.value = (data || []).map((item) => ({
        value: item.id,
        label: item.productName,
      }))
    })
    .catch((err) => {
      console.log(err)
    })
}

const fetchCodingAgentDevice = () => {
  getCodingAgentDevice()
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
          productId: item.productId,
          pairCode: item.pairCode,
          agentType: item.agentType,
          agentId: item.agentId ?? item.id,
          sessionId: item.sessionId,
          cwd: item.cwd,
          finalStatus: item.finalStatus,
          lastUpdate: item.lastUpdate,
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
  fetchCodingAgentDevice()
  intervalId = setInterval(fetchCodingAgentDevice, 5000)
})

onUnmounted(() => {
  clearInterval(intervalId)
})

const handleEdit = (record) => {
  fetchProduct()
  editForm.id = record.id
  editForm.productId = record.productId
  editForm.pairCode = record.pairCode
  editForm.agentType = record.agentType
  editVisible.value = true
}

const handleEditSubmit = () => {
  putCodingAgentDevice({
    id: editForm.id,
    productId: editForm.productId,
    pairCode: editForm.pairCode,
    agentType: editForm.agentType,
  })
    .then((res) => {
      const { errorCode } = res.data
      if (errorCode === 200) {
        message.success('修改成功')
        editVisible.value = false
        fetchCodingAgentDevice()
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
    content: '确定要删除这条编码代理设备记录吗？',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk() {
      deleteCodingAgentDevice({ id: record.id })
        .then((res) => {
          const { errorCode } = res.data
          if (errorCode === 200) {
            message.success('删除成功')
            fetchCodingAgentDevice()
          } else if (errorCode === 3002) {
            message.warn('删除失败，存在依赖记录')
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
  fetchCodingAgentDevice,
})
</script>
