<!-- eslint-disable vue/multi-word-component-names -->
<template>
  <div class="table-container">
    <a-table
      class="custom-table"
      :columns="columns"
      :data-source="dataSource"
      :loading="loading"
      :pagination="{ pageSize: 5 }"
      :scroll="{ x: 1300 }"
      row-key="id"
    >
      <template #emptyText>
        <a-empty description="暂无定时任务" />
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'cron'">
          <a-typography-text code copyable>{{ record.cron }}</a-typography-text>
        </template>

        <template v-else-if="column.key === 'nextEvalTime'">
          <span v-if="record.nextEvalTime">{{ formatNextTime(record.nextEvalTime) }}</span>
          <a-tag v-else-if="!record.cronValid" color="error">Cron 无效</a-tag>
          <a-tag v-else>无后续执行</a-tag>
        </template>

        <template v-else-if="column.key === 'exec'">
          <a-tag :color="record.exec ? 'blue' : 'green'">
            {{ record.exec ? '执行命令' : '仅提醒' }}
          </a-tag>
        </template>

        <template v-else-if="column.key === 'execCommand'">
          <a-tooltip v-if="record.exec && record.execCommand" :title="record.execCommand">
            <span class="ellipsis">{{ record.execCommand }}</span>
          </a-tooltip>
          <span v-else class="muted">—</span>
        </template>

        <template v-else-if="column.key === 'productName'">
          {{ record.productName || `产品 ID：${record.productId}` }}
        </template>

        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="handleEdit(record)">
              <EditOutlined />
              编辑
            </a-button>
            <a-popconfirm
              title="确定删除这个定时任务吗？"
              description="删除后 Quartz 中的任务也会同步移除。"
              ok-text="删除"
              cancel-text="取消"
              placement="topRight"
              @confirm="handleDelete(record)"
            >
              <a-button type="link" danger size="small" :loading="deletingId === record.id">
                <DeleteOutlined />
                删除
              </a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>

  <a-modal
    v-model:open="editVisible"
    title="编辑定时任务"
    width="640px"
    :confirm-loading="submitting"
    ok-text="保存"
    cancel-text="取消"
    @ok="handleEditSubmit"
    @cancel="handleEditCancel"
  >
    <a-form :model="editForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
      <a-form-item label="任务名称">
        <a-input :value="editForm.taskName" disabled />
      </a-form-item>

      <a-form-item label="Cron 表达式" required>
        <a-input v-model:value="editForm.cron" :maxlength="255" placeholder="例如：0 0 8 * * ?">
          <template #addonAfter>
            <a-tooltip title="可视化配置">
              <SettingOutlined class="setting-icon" @click="editCronVisible = true" />
            </a-tooltip>
          </template>
        </a-input>
      </a-form-item>

      <a-form-item label="任务类型">
        <a-switch
          v-model:checked="editForm.exec"
          checked-children="执行命令"
          un-checked-children="仅提醒"
        />
      </a-form-item>

      <a-form-item v-if="editForm.exec" label="执行命令" required>
        <a-textarea
          v-model:value="editForm.execCommand"
          :maxlength="255"
          :rows="3"
          show-count
          placeholder="请输入到点后执行的命令"
        />
      </a-form-item>
    </a-form>
  </a-modal>

  <CronEditor
    v-model:open="editCronVisible"
    :initial-cron="editForm.cron"
    @confirm="editForm.cron = $event"
  />
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { DeleteOutlined, EditOutlined, SettingOutlined } from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'
import { deleteTimeSchedule, putTimeSchedule } from '@/api/timeSchedule'
import CronEditor from './CronEditor.vue'

const props = defineProps({
  schedules: {
    type: Array,
    default: () => [],
  },
  loading: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['refresh'])
const router = useRouter()

const editVisible = ref(false)
const editCronVisible = ref(false)
const submitting = ref(false)
const deletingId = ref(null)
const editForm = reactive({
  id: null,
  taskName: '',
  cron: '',
  exec: false,
  execCommand: '',
})

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 70 },
  { title: '任务名称', dataIndex: 'taskName', key: 'taskName', width: 170 },
  { title: '产品', dataIndex: 'productName', key: 'productName', width: 160 },
  { title: 'Cron 表达式', dataIndex: 'cron', key: 'cron', width: 210 },
  {
    title: '下一次执行时间',
    dataIndex: 'nextEvalTime',
    key: 'nextEvalTime',
    width: 190,
  },
  { title: '任务类型', dataIndex: 'exec', key: 'exec', width: 110 },
  { title: '执行命令', dataIndex: 'execCommand', key: 'execCommand', width: 180 },
  { title: 'AppID', dataIndex: 'appid', key: 'appid', width: 160, ellipsis: true },
  { title: 'OpenID', dataIndex: 'openid', key: 'openid', width: 160, ellipsis: true },
  { title: '操作', key: 'action', width: 150 },
]

const dataSource = computed(() =>
  props.schedules.map((item) => ({
    ...item,
    exec: item.exec === true || item.exec === 'true',
  }))
)

const formatNextTime = (value) => {
  const time = dayjs(value)
  return time.isValid() ? time.format('YYYY-MM-DD HH:mm:ss') : '时间格式无效'
}

const handleEdit = (record) => {
  Object.assign(editForm, {
    id: record.id,
    taskName: record.taskName,
    cron: record.cron,
    exec: record.exec,
    execCommand: record.execCommand || '',
  })
  editVisible.value = true
}

const handleEditCancel = () => {
  if (!submitting.value) editVisible.value = false
}

const handleEditSubmit = async () => {
  const cron = editForm.cron.trim()
  const execCommand = editForm.exec ? editForm.execCommand.trim() : ''
  if (!cron) {
    message.error('Cron 表达式不能为空')
    return
  }
  if (editForm.exec && !execCommand) {
    message.error('执行命令不能为空')
    return
  }

  submitting.value = true
  try {
    const response = await putTimeSchedule({
      id: editForm.id,
      cron,
      exec: editForm.exec,
      execCommand,
    })
    const { errorCode, errorMsg } = response.data
    if (errorCode === 2001) {
      router.push('/login')
      return
    }
    if (errorCode !== 200) {
      message.error(errorMsg || '更新失败，请检查 Cron 表达式')
      return
    }

    message.success('定时任务更新成功')
    editVisible.value = false
    emit('refresh')
  } catch {
    message.error('更新失败，请检查网络连接')
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (record) => {
  deletingId.value = record.id
  try {
    const response = await deleteTimeSchedule({ id: record.id })
    const { errorCode, errorMsg } = response.data
    if (errorCode === 2001) {
      router.push('/login')
      return
    }
    if (errorCode !== 200) {
      message.error(errorMsg || '删除失败，任务可能已不存在')
      return
    }

    message.success('定时任务已删除')
    emit('refresh')
  } catch {
    message.error('删除失败，请检查网络连接')
  } finally {
    deletingId.value = null
  }
}
</script>

<style lang="scss" scoped>
.table-container {
  .custom-table {
    :deep(.ant-table) {
      overflow: hidden;
      border-radius: 12px;
    }

    :deep(.ant-table-thead > tr > th) {
      padding: 16px;
      background: #fafafa;
      font-weight: 500;
    }

    :deep(.ant-table-tbody > tr > td) {
      padding: 16px;
      transition: background 0.3s;
    }

    :deep(.ant-table-tbody > tr:hover > td) {
      background: #fafafa;
    }
  }
}

.setting-icon {
  cursor: pointer;
  color: #1677ff;
}

.ellipsis {
  display: inline-block;
  width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}

.muted {
  color: #bfbfbf;
}
</style>
