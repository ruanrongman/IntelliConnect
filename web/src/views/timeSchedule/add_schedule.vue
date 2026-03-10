<template>
  <a-button type="primary" @click="showModal">
    新建日程
  </a-button>
  <a-modal 
    v-model:open="visible"  
    title="新建日程"
    :footer="null"
    @cancel="handleCancel"
    width="600px"
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
        label="任务名称"
        name="taskName"
        :rules="[{ required: true, message: '请输入任务名称!' }]"
      >
        <a-input v-model:value="formState.taskName" placeholder="请输入任务名称" />
      </a-form-item>
      
      <a-form-item
        label="产品"
        name="productId"
        :rules="[{ required: true, message: '请选择产品!' }]"
      >
        <a-select
          v-model:value="formState.productId"
          placeholder="请选择产品"
          allowClear
          show-search
          :filter-option="filterOption"
        >
          <a-select-option v-for="item in productOptions" :key="item.value" :value="item.value">
            {{ item.label }}
          </a-select-option>
        </a-select>
      </a-form-item>
      
      <a-form-item
        label="Cron表达式"
        name="cron"
        :rules="[{ required: true, message: '请配置Cron表达式!' }]"
      >
        <a-input v-model:value="formState.cron" placeholder="点击下方可视化配置">
          <template #addonAfter>
            <a-tooltip title="可视化配置">
              <SettingOutlined @click="showCronModal" style="cursor: pointer" />
            </a-tooltip>
          </template>
        </a-input>
      </a-form-item>
      
      <a-form-item
        label="是否执行"
        name="exec"
      >
        <a-switch v-model:checked="formState.exec" />
      </a-form-item>
      
      <a-form-item
        v-if="formState.exec"
        label="执行命令"
        name="execCommand"
        :rules="[{ required: true, message: '请输入执行命令!' }]"
      >
        <a-input v-model:value="formState.execCommand" placeholder="请输入执行命令" />
      </a-form-item>
      
      <a-form-item :wrapper-col="{ offset: 8, span: 16 }">
        <a-button type="primary" html-type="submit">提交</a-button>
      </a-form-item>
    </a-form>
  </a-modal>

  <!-- Cron可视化配置弹窗 -->
  <a-modal 
    v-model:open="cronVisible"  
    title="Cron表达式配置"
    @ok="handleCronOk"
    @cancel="cronVisible = false"
    width="700px"
  >
    <a-tabs v-model:activeKey="cronType">
      <a-tab-pane key="daily" tab="每天">
        <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
          <a-form-item label="执行时间">
            <a-date-picker 
              v-model:value="dailyTime" 
              picker="time" 
              format="HH:mm" 
              style="width: 100%" 
              @change="(time) => dailyTime = time"
            />
          </a-form-item>
        </a-form>
      </a-tab-pane>
      
      <a-tab-pane key="weekly" tab="每周">
        <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
          <a-form-item label="选择星期">
            <a-checkbox-group v-model:value="weeklyDays">
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
              v-model:value="weeklyTime" 
              picker="time" 
              format="HH:mm" 
              style="width: 100%" 
              @change="(time) => weeklyTime = time"
            />
          </a-form-item>
        </a-form>
      </a-tab-pane>
      
      <a-tab-pane key="monthly" tab="每月">
        <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
          <a-form-item label="选择日期">
            <a-select v-model:value="monthlyDays" mode="multiple" placeholder="选择日期" style="width: 100%">
              <a-select-option v-for="i in 31" :key="i" :value="i">{{ i }}日</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="执行时间">
            <a-date-picker 
              v-model:value="monthlyTime" 
              picker="time" 
              format="HH:mm" 
              style="width: 100%" 
              @change="(time) => monthlyTime = time"
            />
          </a-form-item>
        </a-form>
      </a-tab-pane>
      
      <a-tab-pane key="interval" tab="间隔执行">
        <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
          <a-form-item label="间隔类型">
            <a-select v-model:value="intervalType" style="width: 100%">
              <a-select-option value="minute">每隔几分钟</a-select-option>
              <a-select-option value="hour">每隔几小时</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="间隔数值">
            <a-input-number v-model:value="intervalValue" :min="1" :max="59" style="width: 100%" />
          </a-form-item>
        </a-form>
      </a-tab-pane>
      
      <a-tab-pane key="custom" tab="自定义">
        <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
          <a-form-item label="秒">
            <a-input v-model:value="customCron.second" placeholder="0-59 或 *" />
          </a-form-item>
          <a-form-item label="分">
            <a-input v-model:value="customCron.minute" placeholder="0-59 或 *" />
          </a-form-item>
          <a-form-item label="时">
            <a-input v-model:value="customCron.hour" placeholder="0-23 或 *" />
          </a-form-item>
          <a-form-item label="日">
            <a-input v-model:value="customCron.day" placeholder="1-31 或 *" />
          </a-form-item>
          <a-form-item label="月">
            <a-input v-model:value="customCron.month" placeholder="1-12 或 *" />
          </a-form-item>
          <a-form-item label="周">
            <a-input v-model:value="customCron.week" placeholder="1-7 或 ?" />
          </a-form-item>
        </a-form>
      </a-tab-pane>
    </a-tabs>
    
    <a-divider />
    <a-descriptions :column="1" bordered size="small">
      <a-descriptions-item label="生成的Cron表达式">
        <a-tag color="blue" style="font-size: 14px;">{{ generatedCron }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="说明">
        {{ cronDescription }}
      </a-descriptions-item>
    </a-descriptions>
  </a-modal>
</template>

<script setup>
import { reactive, ref, computed } from 'vue'
import { postTimeSchedule } from '@/api/timeSchedule'
import { getProduct } from '@/api/product'
import { message } from 'ant-design-vue'
import { SettingOutlined } from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'

const router = useRouter()
const emit = defineEmits(['refresh'])

const visible = ref(false)
const cronVisible = ref(false)
const productOptions = ref([])

const formState = reactive({
  taskName: '',
  cron: '',
  productId: null,
  exec: false,
  execCommand: ''
})

// Cron配置相关
const cronType = ref('daily')
const dailyTime = ref(dayjs().hour(8).minute(0).second(0))
const weeklyDays = ref([1])
const weeklyTime = ref(dayjs().hour(8).minute(0).second(0))
const monthlyDays = ref([1])
const monthlyTime = ref(dayjs().hour(8).minute(0).second(0))
const intervalType = ref('minute')
const intervalValue = ref(5)
const customCron = reactive({
  second: '0',
  minute: '*',
  hour: '*',
  day: '*',
  month: '*',
  week: '?'
})

// 计算生成的Cron表达式
const generatedCron = computed(() => {
  switch (cronType.value) {
    case 'daily': {
      const hour = dailyTime.value.hour()
      const minute = dailyTime.value.minute()
      return `0 ${minute} ${hour} * * ?`
    }
    case 'weekly': {
      if (weeklyDays.value.length === 0) return '请选择星期'
      const hour = weeklyTime.value.hour()
      const minute = weeklyTime.value.minute()
      const days = weeklyDays.value.sort().join(',')
      return `0 ${minute} ${hour} ? * ${days}`
    }
    case 'monthly': {
      if (monthlyDays.value.length === 0) return '请选择日期'
      const hour = monthlyTime.value.hour()
      const minute = monthlyTime.value.minute()
      const days = monthlyDays.value.sort().join(',')
      return `0 ${minute} ${hour} ${days} * ?`
    }
    case 'interval': {
      if (intervalType.value === 'minute') {
        return `0 */${intervalValue.value} * * * ?`
      } else {
        return `0 0 */${intervalValue.value} * * ?`
      }
    }
    case 'custom': {
      return `${customCron.second} ${customCron.minute} ${customCron.hour} ${customCron.day} ${customCron.month} ${customCron.week}`
    }
    default:
      return ''
  }
})

// Cron表达式说明
const cronDescription = computed(() => {
  switch (cronType.value) {
    case 'daily': {
      const hour = dailyTime.value.hour()
      const minute = dailyTime.value.minute()
      return `每天 ${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')} 执行`
    }
    case 'weekly': {
      if (weeklyDays.value.length === 0) return '请选择至少一个星期'
      const weekNames = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日']
      const days = weeklyDays.value.map(d => weekNames[d]).join('、')
      const hour = weeklyTime.value.hour()
      const minute = weeklyTime.value.minute()
      return `每${days} ${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')} 执行`
    }
    case 'monthly': {
      if (monthlyDays.value.length === 0) return '请选择至少一个日期'
      const days = monthlyDays.value.map(d => `${d}日`).join('、')
      const hour = monthlyTime.value.hour()
      const minute = monthlyTime.value.minute()
      return `每月${days} ${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')} 执行`
    }
    case 'interval': {
      if (intervalType.value === 'minute') {
        return `每隔 ${intervalValue.value} 分钟执行一次`
      } else {
        return `每隔 ${intervalValue.value} 小时执行一次`
      }
    }
    case 'custom': {
      return '自定义Cron表达式'
    }
    default:
      return ''
  }
})

const showModal = () => {
  fetchProductList()
  visible.value = true
}

const handleCancel = () => {
  visible.value = false
}

const showCronModal = () => {
  // 确保时间选择器有有效的 dayjs 对象
  if (!dailyTime.value || !dayjs.isDayjs(dailyTime.value)) {
    dailyTime.value = dayjs().hour(8).minute(0).second(0)
  }
  if (!weeklyTime.value || !dayjs.isDayjs(weeklyTime.value)) {
    weeklyTime.value = dayjs().hour(8).minute(0).second(0)
  }
  if (!monthlyTime.value || !dayjs.isDayjs(monthlyTime.value)) {
    monthlyTime.value = dayjs().hour(8).minute(0).second(0)
  }
  cronVisible.value = true
}

const handleCronOk = () => {
  // 验证生成的 Cron 表达式是否有效
  const cron = generatedCron.value
  if (cron.includes('请选择')) {
    message.error('请完善 Cron 表达式配置')
    return
  }
  // 验证自定义模式的 Cron 表达式格式
  if (cronType.value === 'custom') {
    const parts = cron.split(' ')
    if (parts.length !== 6) {
      message.error('自定义 Cron 表达式格式不正确，需要包含6个字段（秒 分 时 日 月 周）')
      return
    }
  }
  formState.cron = cron
  cronVisible.value = false
}

const filterOption = (input, option) => {
  return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
}

const fetchProductList = () => {
  getProduct()
    .then((res) => {
      const { data, errorCode } = res.data
      if (errorCode === 2001) {
        router.push('/login')
        return
      }
      if (errorCode === 200 && data) {
        const list = Array.isArray(data) ? data : (data.data || [])
        productOptions.value = list.map(item => ({
          value: item.id,
          label: item.productName
        }))
      }
    })
    .catch(() => {
      // 静默处理
    })
}

const handleSubmit = () => {
  // 验证必填字段
  if (!formState.taskName || !formState.cron || !formState.productId) {
    message.error('请填写所有必填项')
    return
  }
  
  // 后端 execCommand 不能为空，即使 exec=false 也要传值
  const submitData = {
    taskName: formState.taskName,
    cron: formState.cron,
    productId: Number(formState.productId),
    exec: Boolean(formState.exec),
    execCommand: formState.exec ? formState.execCommand : 'none'
  }
  
  console.log('提交数据:', submitData)
  
  postTimeSchedule(submitData)
    .then((res) => {
      const { data, errorCode, message: msg } = res.data
      if (errorCode === 2001) {
        router.push('/login')
      } else if (errorCode === 200) {
        message.success('创建成功')
        visible.value = false
        // 重置表单
        formState.taskName = ''
        formState.cron = ''
        formState.productId = null
        formState.exec = false
        formState.execCommand = ''
        emit('refresh')
      } else if (errorCode === 1001) {
        message.error('创建失败：产品不存在或未激活，请确保产品已在微信端激活')
      } else {
        message.error(msg || '创建失败，请检查参数')
      }
    })
    .catch((err) => {
      console.log(err)
      message.error('创建失败：网络错误')
    })
}

const onFinish = (values) => {
  console.log('Success:', values)
  handleSubmit()
}

const onFinishFailed = (errorInfo) => {
  console.log('Failed:', errorInfo)
}
</script>

<style lang="scss" scoped>
:deep(.ant-checkbox-wrapper) {
  margin-right: 8px;
  margin-bottom: 4px;
}
</style>
