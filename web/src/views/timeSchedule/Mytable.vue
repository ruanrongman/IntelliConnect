<template>
  <a-table :columns="columns" :data-source="dataSource" :pagination="pagination">
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'exec'">
        <a-tag :color="record.exec === 'true' ? 'green' : 'red'">
          {{ record.exec === 'true' ? '启用' : '禁用' }}
        </a-tag>
      </template>
      <template v-if="column.key === 'productName'">
        {{ record.productName || `产品ID: ${record.productId}` }}
      </template>
      <template v-if="column.key === 'action'">
        <a-space>
          <a-button
              type="link"
              size="small"
              @click="handleEdit(record)"
          >
            <EditOutlined />
            编辑
          </a-button>

          <a-popconfirm
              title="确定要删除这个日程吗？"
              description="删除后将无法恢复"
              ok-text="确定"
              cancel-text="取消"
              placement="topRight"
              @confirm="handleDelete(record)"
          >
            <a-button
                type="link"
                danger
                size="small"
            >
              <DeleteOutlined />
              删除
            </a-button>
          </a-popconfirm>
        </a-space>
      </template>
    </template>
  </a-table>

  <!-- 编辑弹窗 -->
  <a-modal 
    v-model:open="editVisible" 
    title="编辑日程"
    @ok="handleEditSubmit"
    @cancel="editVisible = false"
    width="600px"
  >
    <a-form
      :model="editForm"
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 16 }"
    >
      <a-form-item label="任务名称">
        <a-input :value="editForm.taskName" disabled />
      </a-form-item>
      <a-form-item label="Cron表达式" required>
        <a-input v-model:value="editForm.cron" placeholder="点击可视化配置">
          <template #addonAfter>
            <a-tooltip title="可视化配置">
              <SettingOutlined @click="showEditCronModal" style="cursor: pointer" />
            </a-tooltip>
          </template>
        </a-input>
      </a-form-item>
      <a-form-item label="是否执行">
        <a-switch v-model:checked="editForm.execBool" />
      </a-form-item>
      <a-form-item v-if="editForm.execBool" label="执行命令" required>
        <a-input v-model:value="editForm.execCommand" placeholder="请输入执行命令" />
      </a-form-item>
    </a-form>
  </a-modal>

  <!-- Cron可视化配置弹窗(编辑) -->
  <a-modal 
    v-model:open="editCronVisible"  
    title="Cron表达式配置"
    @ok="handleEditCronOk"
    @cancel="editCronVisible = false"
    width="700px"
  >
    <a-tabs v-model:activeKey="editCronType">
      <a-tab-pane key="daily" tab="每天">
        <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
          <a-form-item label="执行时间">
            <a-date-picker 
              v-model:value="editDailyTime" 
              picker="time" 
              format="HH:mm" 
              style="width: 100%" 
              @change="(time) => editDailyTime = time"
            />
          </a-form-item>
        </a-form>
      </a-tab-pane>
      
      <a-tab-pane key="weekly" tab="每周">
        <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
          <a-form-item label="选择星期">
            <a-checkbox-group v-model:value="editWeeklyDays">
              <a-checkbox :value="1">周一</a-checkbox>
              <a-checkbox :value="2">周二</a-checkbox>
              <a-checkbox :value="3">周三</a-checkbox>
              <a-checkbox :value="4">周四</a-checkbox>
              <a-checkbox :value="5">周五</a-checkbox>
              <a-checkbox :value="6">周六</a-checkbox>
              <a-checkbox :value="7">周日</a-checkbox>
            </a-checkbox-group>
          </a-form-item>
          <a-form-item label="执行时间">
            <a-date-picker 
              v-model:value="editWeeklyTime" 
              picker="time" 
              format="HH:mm" 
              style="width: 100%" 
              @change="(time) => editWeeklyTime = time"
            />
          </a-form-item>
        </a-form>
      </a-tab-pane>
      
      <a-tab-pane key="monthly" tab="每月">
        <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
          <a-form-item label="选择日期">
            <a-select v-model:value="editMonthlyDays" mode="multiple" placeholder="选择日期" style="width: 100%">
              <a-select-option v-for="i in 31" :key="i" :value="i">{{ i }}日</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="执行时间">
            <a-date-picker 
              v-model:value="editMonthlyTime" 
              picker="time" 
              format="HH:mm" 
              style="width: 100%" 
              @change="(time) => editMonthlyTime = time"
            />
          </a-form-item>
        </a-form>
      </a-tab-pane>
      
      <a-tab-pane key="interval" tab="间隔执行">
        <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
          <a-form-item label="间隔类型">
            <a-select v-model:value="editIntervalType" style="width: 100%">
              <a-select-option value="minute">每隔几分钟</a-select-option>
              <a-select-option value="hour">每隔几小时</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="间隔数值">
            <a-input-number v-model:value="editIntervalValue" :min="1" :max="59" style="width: 100%" />
          </a-form-item>
        </a-form>
      </a-tab-pane>
      
      <a-tab-pane key="custom" tab="自定义">
        <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
          <a-form-item label="秒">
            <a-input v-model:value="editCustomCron.second" placeholder="0-59 或 *" />
          </a-form-item>
          <a-form-item label="分">
            <a-input v-model:value="editCustomCron.minute" placeholder="0-59 或 *" />
          </a-form-item>
          <a-form-item label="时">
            <a-input v-model:value="editCustomCron.hour" placeholder="0-23 或 *" />
          </a-form-item>
          <a-form-item label="日">
            <a-input v-model:value="editCustomCron.day" placeholder="1-31 或 *" />
          </a-form-item>
          <a-form-item label="月">
            <a-input v-model:value="editCustomCron.month" placeholder="1-12 或 *" />
          </a-form-item>
          <a-form-item label="周">
            <a-input v-model:value="editCustomCron.week" placeholder="1-7 或 ?" />
          </a-form-item>
        </a-form>
      </a-tab-pane>
    </a-tabs>
    
    <a-divider />
    <a-descriptions :column="1" bordered size="small">
      <a-descriptions-item label="生成的Cron表达式">
        <a-tag color="blue" style="font-size: 14px;">{{ editGeneratedCron }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="说明">
        {{ editCronDescription }}
      </a-descriptions-item>
    </a-descriptions>
  </a-modal>
</template>

<script setup>
import { ref, onMounted, onUnmounted, reactive, computed } from 'vue'
import { getTimeSchedule, deleteTimeSchedule, putTimeSchedule } from '@/api/timeSchedule'
import { getProduct } from '@/api/product'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { EditOutlined, DeleteOutlined, SettingOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'

const router = useRouter()
const emit = defineEmits(['refresh'])

const pagination = {
  pageSize: 10
}

const dataSource = ref([])
const productList = ref([])
const editVisible = ref(false)
const editCronVisible = ref(false)
const editForm = ref({
  id: null,
  taskName: '',
  cron: '',
  exec: false,
  execBool: false,
  execCommand: ''
})

// 编辑时的Cron配置
const editCronType = ref('daily')
const editDailyTime = ref(dayjs().hour(8).minute(0).second(0))
const editWeeklyDays = ref([1])
const editWeeklyTime = ref(dayjs().hour(8).minute(0).second(0))
const editMonthlyDays = ref([1])
const editMonthlyTime = ref(dayjs().hour(8).minute(0).second(0))
const editIntervalType = ref('minute')
const editIntervalValue = ref(5)
const editCustomCron = reactive({
  second: '0',
  minute: '*',
  hour: '*',
  day: '*',
  month: '*',
  week: '?'
})

// 计算编辑时的Cron表达式
const editGeneratedCron = computed(() => {
  switch (editCronType.value) {
    case 'daily': {
      const hour = editDailyTime.value.hour()
      const minute = editDailyTime.value.minute()
      return `0 ${minute} ${hour} * * ?`
    }
    case 'weekly': {
      if (editWeeklyDays.value.length === 0) return '请选择星期'
      const hour = editWeeklyTime.value.hour()
      const minute = editWeeklyTime.value.minute()
      const days = editWeeklyDays.value.sort().join(',')
      return `0 ${minute} ${hour} ? * ${days}`
    }
    case 'monthly': {
      if (editMonthlyDays.value.length === 0) return '请选择日期'
      const hour = editMonthlyTime.value.hour()
      const minute = editMonthlyTime.value.minute()
      const days = editMonthlyDays.value.sort().join(',')
      return `0 ${minute} ${hour} ${days} * ?`
    }
    case 'interval': {
      if (editIntervalType.value === 'minute') {
        return `0 */${editIntervalValue.value} * * * ?`
      } else {
        return `0 0 */${editIntervalValue.value} * * ?`
      }
    }
    case 'custom': {
      return `${editCustomCron.second} ${editCustomCron.minute} ${editCustomCron.hour} ${editCustomCron.day} ${editCustomCron.month} ${editCustomCron.week}`
    }
    default:
      return ''
  }
})

const editCronDescription = computed(() => {
  switch (editCronType.value) {
    case 'daily': {
      const hour = editDailyTime.value.hour()
      const minute = editDailyTime.value.minute()
      return `每天 ${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')} 执行`
    }
    case 'weekly': {
      if (editWeeklyDays.value.length === 0) return '请选择至少一个星期'
      const weekNames = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日']
      const days = editWeeklyDays.value.map(d => weekNames[d]).join('、')
      const hour = editWeeklyTime.value.hour()
      const minute = editWeeklyTime.value.minute()
      return `每${days} ${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')} 执行`
    }
    case 'monthly': {
      if (editMonthlyDays.value.length === 0) return '请选择至少一个日期'
      const days = editMonthlyDays.value.map(d => `${d}日`).join('、')
      const hour = editMonthlyTime.value.hour()
      const minute = editMonthlyTime.value.minute()
      return `每月${days} ${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')} 执行`
    }
    case 'interval': {
      if (editIntervalType.value === 'minute') {
        return `每隔 ${editIntervalValue.value} 分钟执行一次`
      } else {
        return `每隔 ${editIntervalValue.value} 小时执行一次`
      }
    }
    case 'custom': {
      return '自定义Cron表达式'
    }
    default:
      return ''
  }
})

const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    key: 'id',
    width: 80
  },
  {
    title: '任务名称',
    dataIndex: 'taskName',
    key: 'taskName'
  },
  {
    title: 'Cron表达式',
    dataIndex: 'cron',
    key: 'cron'
  },
  {
    title: '状态',
    dataIndex: 'exec',
    key: 'exec',
    width: 100
  },
  {
    title: '执行命令',
    dataIndex: 'execCommand',
    key: 'execCommand',
    ellipsis: true
  },
  {
    title: '产品',
    key: 'productName',
    width: 150
  },
  {
    title: 'AppID',
    dataIndex: 'appid',
    key: 'appid',
    width: 150,
    ellipsis: true
  },
  {
    title: 'OpenID',
    dataIndex: 'openid',
    key: 'openid',
    width: 150,
    ellipsis: true
  },
  {
    title: '操作',
    key: 'action',
    width: 180
  }
]

let intervalId

// 获取产品列表
const fetchProductList = () => {
  getProduct()
    .then((res) => {
      const { data, errorCode } = res.data
      if (errorCode === 200 && data) {
        productList.value = Array.isArray(data) ? data : (data.data || [])
      }
    })
    .catch(() => {
      // 静默处理
    })
}

// 根据productId获取产品名称
const getProductName = (productId) => {
  const product = productList.value.find(p => p.id === productId)
  return product ? product.productName : null
}

const fetchTimeSchedule = () => {
  getTimeSchedule()
    .then((res) => {
      const { data, errorCode } = res.data
      if (errorCode === 2001) {
        router.push('/login')
        return
      }
      if (errorCode === 200 && data && Array.isArray(data)) {
        dataSource.value = data.map((item, index) => ({
          key: index,
          id: item.id,
          taskName: item.taskName,
          cron: item.cron,
          exec: item.exec,
          execCommand: item.execCommand,
          productId: item.productId,
          productName: getProductName(item.productId),
          appid: item.appid,
          openid: item.openid
        }))
      } else {
        dataSource.value = []
      }
    })
    .catch(() => {
      dataSource.value = []
    })
}

const handleEdit = (record) => {
  editForm.value = {
    id: record.id,
    taskName: record.taskName,
    cron: record.cron,
    exec: record.exec,
    execBool: record.exec === 'true',
    execCommand: record.execCommand || ''
  }
  editVisible.value = true
}

const showEditCronModal = () => {
  // 确保时间选择器有有效的 dayjs 对象
  if (!editDailyTime.value || !dayjs.isDayjs(editDailyTime.value)) {
    editDailyTime.value = dayjs().hour(8).minute(0).second(0)
  }
  if (!editWeeklyTime.value || !dayjs.isDayjs(editWeeklyTime.value)) {
    editWeeklyTime.value = dayjs().hour(8).minute(0).second(0)
  }
  if (!editMonthlyTime.value || !dayjs.isDayjs(editMonthlyTime.value)) {
    editMonthlyTime.value = dayjs().hour(8).minute(0).second(0)
  }
  editCronVisible.value = true
}

const handleEditCronOk = () => {
  // 验证生成的 Cron 表达式是否有效
  const cron = editGeneratedCron.value
  if (cron.includes('请选择')) {
    message.error('请完善 Cron 表达式配置')
    return
  }
  // 验证自定义模式的 Cron 表达式格式
  if (editCronType.value === 'custom') {
    const parts = cron.split(' ')
    if (parts.length !== 6) {
      message.error('自定义 Cron 表达式格式不正确，需要包含6个字段（秒 分 时 日 月 周）')
      return
    }
  }
  editForm.value.cron = cron
  editCronVisible.value = false
}

const handleEditSubmit = () => {
  // 后端 execCommand 不能为空
  const submitData = {
    id: editForm.value.id,
    cron: editForm.value.cron,
    exec: editForm.value.execBool,
    execCommand: editForm.value.execBool ? editForm.value.execCommand : 'none'
  }
  
  putTimeSchedule(submitData)
    .then((res) => {
      const { data, errorCode } = res.data
      if (errorCode === 2001) {
        router.push('/login')
      } else if (errorCode === 200) {
        message.success('更新成功')
        editVisible.value = false
        fetchTimeSchedule()
        emit('refresh')
      } else {
        message.error('更新失败')
      }
    })
    .catch(() => {
      message.error('更新失败')
    })
}

const handleDelete = (record) => {
  console.log('删除记录:', record)
  const deleteId = Number(record.id)
  console.log('删除ID:', deleteId)
  
  deleteTimeSchedule({ id: deleteId })
    .then((res) => {
      console.log('删除响应:', res.data)
      const { data, errorCode, message: msg } = res.data
      if (errorCode === 2001) {
        router.push('/login')
      } else if (errorCode === 200) {
        message.success('删除成功')
        fetchTimeSchedule()
        emit('refresh')
      } else if (errorCode === 1001) {
        message.error('删除失败：日程不存在或无权限')
      } else {
        message.error(msg || '删除失败')
      }
    })
    .catch((err) => {
      console.error('删除错误:', err)
      message.error('删除失败：网络错误')
    })
}

onMounted(() => {
  fetchProductList()
  fetchTimeSchedule()
  intervalId = setInterval(fetchTimeSchedule, 5000)
})

onUnmounted(() => {
  clearInterval(intervalId)
})
</script>

<style lang="scss" scoped>
.action-buttons {
  display: flex;
  gap: 8px;
  
  .action-button {
    padding: 0 4px;
  }
}

:deep(.ant-checkbox-wrapper) {
  margin-right: 8px;
  margin-bottom: 4px;
}
</style>
