<template>
  <div class="table-container">
    <a-table
      :columns="columns"
      :data-source="dataSource"
      :pagination="pagination"
      :loading="loading"
      class="custom-table"
    >
      <template #action="{ record }">
        <a-button
          type="link"
          danger
          @click="handleDelete(record)"
        >
          <template #icon><DeleteOutlined /></template>
          删除
        </a-button>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { DeleteOutlined } from '@ant-design/icons-vue'
import { getProductSkill, deleteProductSkill } from '@/api/productSkills'
import { useRouter } from 'vue-router'

const router = useRouter()
const emit = defineEmits(['refresh'])

const pagination = {
  pageSize: 10,
}

const loading = ref(false)
const dataSource = ref([])

const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    key: 'id',
    width: 80,
  },
  {
    title: '产品ID',
    dataIndex: 'productId',
    key: 'productId',
    width: 100,
  },
  {
    title: '技能名称',
    dataIndex: 'name',
    key: 'name',
  },
  {
    title: '描述',
    dataIndex: 'description',
    key: 'description',
    ellipsis: true,
  },
  {
    title: '文件路径',
    dataIndex: 'filePath',
    key: 'filePath',
  },
  {
    title: '操作',
    key: 'action',
    width: 120,
    slots: { customRender: 'action' },
  },
]

let intervalId

onMounted(() => {
  fetchData()
  intervalId = setInterval(fetchData, 1000) // 每 10 秒刷新一次数据
})

onUnmounted(() => {
  clearInterval(intervalId)
})

const fetchData = async () => {
  // Don't show loading for interval refreshes
  // loading.value = true
  try {
    const res = await getProductSkill()
    const { data, errorCode } = res.data

    if (errorCode === 2001) {
      router.push('/login')
    } else if (errorCode === 200) {
      dataSource.value = data || [] // Handle empty data
    } else {
      // For any other error codes, check if it's just no data found
      // COMMON_FAIL (4000) might mean no data, so treat as empty list
      dataSource.value = [] // Show empty table instead of error
    }
  } catch (err) {
    console.error('Error fetching skills:', err)
    // message.error('获取技能列表时出错')
  } finally {
    // loading.value = false
  }
}

const handleDelete = (record) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除技能 "${record.name}" 吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        const res = await deleteProductSkill(record.id)
        const { errorCode } = res.data

        if (errorCode === 200) {
          message.success('删除成功')
          fetchData() // Refresh data after deletion
        } else {
          message.error('删除失败')
        }
      } catch (err) {
        console.error('Error deleting skill:', err)
        message.error('删除时出错')
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.table-container {
  width: 100%;
}

.custom-table {
  :deep(.ant-table) {
    font-size: 14px;
  }
}
</style>