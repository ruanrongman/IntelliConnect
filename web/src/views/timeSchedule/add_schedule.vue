<template>
  <a-button type="primary" @click="showModal"> 新建定时任务 </a-button>

  <a-modal
    v-model:open="visible"
    title="新建定时任务"
    :footer="null"
    width="640px"
    @cancel="handleCancel"
  >
    <a-form
      ref="formRef"
      :model="formState"
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 16 }"
      autocomplete="off"
      @finish="handleSubmit"
    >
      <a-form-item
        label="任务名称"
        name="taskName"
        :rules="[{ required: true, whitespace: true, message: '请输入任务名称' }]"
      >
        <a-input
          v-model:value="formState.taskName"
          :maxlength="255"
          placeholder="例如：工作日早上提醒"
        />
      </a-form-item>

      <a-form-item
        label="产品"
        name="productId"
        :rules="[{ required: true, message: '请选择产品' }]"
      >
        <a-select
          v-model:value="formState.productId"
          :options="productOptions"
          :loading="productLoading"
          placeholder="请选择已激活的产品"
          allow-clear
          show-search
          :filter-option="filterOption"
        />
      </a-form-item>

      <a-form-item
        label="Cron 表达式"
        name="cron"
        :rules="[{ required: true, whitespace: true, message: '请配置 Cron 表达式' }]"
      >
        <a-input v-model:value="formState.cron" :maxlength="255" placeholder="例如：0 0 8 * * ?">
          <template #addonAfter>
            <a-tooltip title="可视化配置">
              <SettingOutlined class="setting-icon" @click="cronVisible = true" />
            </a-tooltip>
          </template>
        </a-input>
      </a-form-item>

      <a-form-item label="任务类型" name="exec">
        <a-switch
          v-model:checked="formState.exec"
          checked-children="执行命令"
          un-checked-children="仅提醒"
        />
      </a-form-item>

      <a-form-item
        v-if="formState.exec"
        label="执行命令"
        name="execCommand"
        :rules="[{ required: true, whitespace: true, message: '请输入执行命令' }]"
      >
        <a-textarea
          v-model:value="formState.execCommand"
          :maxlength="255"
          :rows="3"
          show-count
          placeholder="到点后交给 Agent 执行的命令"
        />
      </a-form-item>

      <a-form-item :wrapper-col="{ offset: 6, span: 16 }">
        <a-space>
          <a-button type="primary" html-type="submit" :loading="submitting"> 创建 </a-button>
          <a-button :disabled="submitting" @click="handleCancel">取消</a-button>
        </a-space>
      </a-form-item>
    </a-form>
  </a-modal>

  <CronEditor
    v-model:open="cronVisible"
    :initial-cron="formState.cron"
    @confirm="formState.cron = $event"
  />
</template>

<script setup>
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { SettingOutlined } from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'
import { postTimeSchedule } from '@/api/timeSchedule'
import { getProduct } from '@/api/product'
import CronEditor from './CronEditor.vue'

const router = useRouter()
const emit = defineEmits(['refresh'])

const formRef = ref()
const visible = ref(false)
const cronVisible = ref(false)
const submitting = ref(false)
const productLoading = ref(false)
const productOptions = ref([])

const formState = reactive({
  taskName: '',
  cron: '',
  productId: null,
  exec: false,
  execCommand: '',
})

const resetForm = () => {
  Object.assign(formState, {
    taskName: '',
    cron: '',
    productId: null,
    exec: false,
    execCommand: '',
  })
  formRef.value?.clearValidate()
}

const showModal = () => {
  visible.value = true
  fetchProductList()
}

const handleCancel = () => {
  if (submitting.value) return
  visible.value = false
  resetForm()
}

const filterOption = (input, option) =>
  option.label.toLowerCase().includes(input.trim().toLowerCase())

const fetchProductList = async () => {
  productLoading.value = true
  try {
    const response = await getProduct()
    const { data, errorCode } = response.data
    if (errorCode === 2001) {
      router.push('/login')
      return
    }
    const list = errorCode === 200 && Array.isArray(data) ? data : []
    productOptions.value = list.map((item) => ({
      value: item.id,
      label: item.productName || `产品 ${item.id}`,
    }))
  } catch {
    productOptions.value = []
    message.error('产品列表加载失败')
  } finally {
    productLoading.value = false
  }
}

const handleSubmit = async () => {
  submitting.value = true
  const payload = {
    taskName: formState.taskName.trim(),
    cron: formState.cron.trim(),
    productId: Number(formState.productId),
    exec: formState.exec,
    execCommand: formState.exec ? formState.execCommand.trim() : '',
  }

  try {
    const response = await postTimeSchedule(payload)
    const { errorCode, errorMsg } = response.data
    if (errorCode === 2001) {
      router.push('/login')
      return
    }
    if (errorCode !== 200) {
      message.error(errorMsg || '创建失败，请检查产品激活状态和 Cron 表达式')
      return
    }

    message.success('定时任务创建成功')
    visible.value = false
    resetForm()
    emit('refresh')
  } catch {
    message.error('创建失败，请检查网络连接')
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.setting-icon {
  cursor: pointer;
  color: #1677ff;
}
</style>
