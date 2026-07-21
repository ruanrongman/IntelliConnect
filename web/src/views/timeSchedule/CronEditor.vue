<template>
  <a-modal
    :open="open"
    title="Cron 表达式配置"
    width="720px"
    @ok="handleConfirm"
    @cancel="handleCancel"
  >
    <a-alert
      class="cron-tip"
      type="info"
      show-icon
      message="时间按服务器所在时区执行；每周规则使用 Quartz 的 MON-SUN 标识。"
    />

    <a-tabs v-model:activeKey="cronType">
      <a-tab-pane key="daily" tab="每天">
        <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
          <a-form-item label="执行时间">
            <a-time-picker
              v-model:value="dailyTime"
              format="HH:mm"
              value-format="HH:mm"
              style="width: 100%"
            />
          </a-form-item>
        </a-form>
      </a-tab-pane>

      <a-tab-pane key="weekly" tab="每周">
        <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
          <a-form-item label="选择星期">
            <a-checkbox-group v-model:value="weeklyDays">
              <a-checkbox v-for="item in weekOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </a-checkbox>
            </a-checkbox-group>
          </a-form-item>
          <a-form-item label="执行时间">
            <a-time-picker
              v-model:value="weeklyTime"
              format="HH:mm"
              value-format="HH:mm"
              style="width: 100%"
            />
          </a-form-item>
        </a-form>
      </a-tab-pane>

      <a-tab-pane key="monthly" tab="每月">
        <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
          <a-form-item label="选择日期">
            <a-select
              v-model:value="monthlyDays"
              mode="multiple"
              placeholder="请选择日期"
              style="width: 100%"
            >
              <a-select-option v-for="day in 31" :key="day" :value="day">
                {{ day }} 日
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="执行时间">
            <a-time-picker
              v-model:value="monthlyTime"
              format="HH:mm"
              value-format="HH:mm"
              style="width: 100%"
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
            <a-input-number
              v-model:value="intervalValue"
              :min="1"
              :max="intervalMax"
              style="width: 100%"
            />
          </a-form-item>
        </a-form>
      </a-tab-pane>

      <a-tab-pane key="custom" tab="自定义">
        <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
          <a-form-item v-for="field in customFields" :key="field.key" :label="field.label">
            <a-input v-model:value="customCron[field.key]" :placeholder="field.placeholder" />
          </a-form-item>
        </a-form>
      </a-tab-pane>
    </a-tabs>

    <a-divider />
    <a-descriptions :column="1" bordered size="small">
      <a-descriptions-item label="Cron 表达式">
        <a-typography-text code copyable>{{ generatedCron }}</a-typography-text>
      </a-descriptions-item>
      <a-descriptions-item label="说明">
        {{ cronDescription }}
      </a-descriptions-item>
    </a-descriptions>
  </a-modal>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'

const props = defineProps({
  open: {
    type: Boolean,
    default: false,
  },
  initialCron: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['update:open', 'confirm'])

const weekOptions = [
  { label: '周一', value: 'MON' },
  { label: '周二', value: 'TUE' },
  { label: '周三', value: 'WED' },
  { label: '周四', value: 'THU' },
  { label: '周五', value: 'FRI' },
  { label: '周六', value: 'SAT' },
  { label: '周日', value: 'SUN' },
]

const customFields = [
  { key: 'second', label: '秒', placeholder: '0-59 或 *' },
  { key: 'minute', label: '分', placeholder: '0-59 或 *' },
  { key: 'hour', label: '时', placeholder: '0-23 或 *' },
  { key: 'day', label: '日', placeholder: '1-31、* 或 ?' },
  { key: 'month', label: '月', placeholder: '1-12 或 *' },
  { key: 'week', label: '周', placeholder: 'MON-SUN、* 或 ?' },
  { key: 'year', label: '年', placeholder: '可选，例如 2026' },
]

const cronType = ref('daily')
const dailyTime = ref('08:00')
const weeklyDays = ref(['MON'])
const weeklyTime = ref('08:00')
const monthlyDays = ref([1])
const monthlyTime = ref('08:00')
const intervalType = ref('minute')
const intervalValue = ref(5)
const customCron = reactive({
  second: '0',
  minute: '*',
  hour: '*',
  day: '*',
  month: '*',
  week: '?',
  year: '',
})

const intervalMax = computed(() => (intervalType.value === 'hour' ? 23 : 59))

watch(intervalType, () => {
  if (intervalValue.value > intervalMax.value) {
    intervalValue.value = intervalMax.value
  }
})

watch(
  () => props.open,
  (open) => {
    if (open) {
      initializeFromCron(props.initialCron)
    }
  }
)

const parseTime = (value) => {
  const [hour = '0', minute = '0'] = (value || '00:00').split(':')
  return {
    hour: Number(hour),
    minute: Number(minute),
  }
}

const generatedCron = computed(() => {
  if (cronType.value === 'daily') {
    const { hour, minute } = parseTime(dailyTime.value)
    return `0 ${minute} ${hour} * * ?`
  }
  if (cronType.value === 'weekly') {
    if (!weeklyDays.value.length) return '请选择星期'
    const { hour, minute } = parseTime(weeklyTime.value)
    const order = weekOptions.map((item) => item.value)
    const days = [...weeklyDays.value]
      .sort((first, second) => order.indexOf(first) - order.indexOf(second))
      .join(',')
    return `0 ${minute} ${hour} ? * ${days}`
  }
  if (cronType.value === 'monthly') {
    if (!monthlyDays.value.length) return '请选择日期'
    const { hour, minute } = parseTime(monthlyTime.value)
    const days = [...monthlyDays.value].sort((first, second) => first - second).join(',')
    return `0 ${minute} ${hour} ${days} * ?`
  }
  if (cronType.value === 'interval') {
    return intervalType.value === 'minute'
      ? `0 */${intervalValue.value} * * * ?`
      : `0 0 */${intervalValue.value} * * ?`
  }

  const fields = [
    customCron.second,
    customCron.minute,
    customCron.hour,
    customCron.day,
    customCron.month,
    customCron.week,
  ]
  if (customCron.year.trim()) fields.push(customCron.year)
  return fields.map((value) => value.trim()).join(' ')
})

const cronDescription = computed(() => {
  if (cronType.value === 'daily') return `每天 ${dailyTime.value} 执行`
  if (cronType.value === 'weekly') {
    const labels = weeklyDays.value
      .map((value) => weekOptions.find((item) => item.value === value)?.label)
      .filter(Boolean)
      .join('、')
    return labels ? `每${labels} ${weeklyTime.value} 执行` : '请选择至少一个星期'
  }
  if (cronType.value === 'monthly') {
    const days = [...monthlyDays.value]
      .sort((first, second) => first - second)
      .map((day) => `${day} 日`)
      .join('、')
    return days ? `每月 ${days} ${monthlyTime.value} 执行` : '请选择至少一个日期'
  }
  if (cronType.value === 'interval') {
    return intervalType.value === 'minute'
      ? `每隔 ${intervalValue.value} 分钟执行一次`
      : `每隔 ${intervalValue.value} 小时执行一次`
  }
  return '自定义 Quartz Cron 表达式'
})

const initializeFromCron = (cron) => {
  const parts = (cron || '').trim().split(/\s+/).filter(Boolean)
  if (parts.length === 6 || parts.length === 7) {
    cronType.value = 'custom'
    const [second, minute, hour, day, month, week, year = ''] = parts
    Object.assign(customCron, { second, minute, hour, day, month, week, year })
    return
  }
  cronType.value = 'daily'
}

const handleConfirm = () => {
  const cron = generatedCron.value.trim()
  const parts = cron.split(/\s+/)
  if (cron.includes('请选择') || (parts.length !== 6 && parts.length !== 7)) {
    message.error('请完善 Cron 表达式配置')
    return
  }
  emit('confirm', cron)
  emit('update:open', false)
}

const handleCancel = () => {
  emit('update:open', false)
}
</script>

<style lang="scss" scoped>
.cron-tip {
  margin-bottom: 12px;
}

:deep(.ant-checkbox-wrapper) {
  margin-right: 12px;
  margin-bottom: 8px;
}
</style>
