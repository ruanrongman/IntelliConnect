<template>
  <a-button type="primary" @click="showModal">
    新建配置
  </a-button>
  <a-modal
    :visible="visible"
    :footer="null"
    :closable="!submitting"
    :mask-closable="!submitting"
    @cancel="handleCancel"
    title="新建/修改配置"
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
        label="固件"
        name="otaId"
        :rules="[{ required: true, message: '请选择固件!' }]"
      >
        <a-select
          v-model:value="formState.otaId"
          :options="firmwareOptions"
          placeholder="请选择固件"
          :disabled="submitting"
          :loading="loadingFirmware"
          allowClear
          show-search
          :filter-option="filterOption"
        />
      </a-form-item>

      <a-form-item
        label="产品"
        name="productId"
        :rules="[{ required: true, message: '请选择产品!' }]"
      >
        <a-select
          v-model:value="formState.productId"
          :options="productOptions"
          placeholder="请选择产品"
          :disabled="submitting"
          :loading="loadingProducts"
          allowClear
          show-search
          :filter-option="filterOption"
        />
      </a-form-item>

      <a-form-item
        label="版本名称"
        name="versionName"
        :rules="[{ required: true, message: '请输入版本名称!' }]"
      >
        <a-input v-model:value="formState.versionName" :disabled="submitting" />
      </a-form-item>

      <!-- 移除了固件描述字段 -->

      <a-form-item :wrapper-col="{ offset: 8, span: 16 }">
        <a-button
          type="primary"
          html-type="submit"
          :loading="submitting"
          :disabled="submitting"
        >
          {{ submitting ? '提交中...' : '提交' }}
        </a-button>
        <a-button
          style="margin-left: 10px"
          @click="handleCancel"
          :disabled="submitting"
        >
          取消
        </a-button>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { reactive, ref, toRaw } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
// 1. 引入新的 API
import { postOtaXiaoZhiPassive } from '@/api/productOtaXiaozhiPassive'
import { otaList } from '@/api/productOta'
import { getProduct } from '@/api/product' // 引入获取产品列表的API

const router = useRouter()

// 模态框控制
const visible = ref(false)

// 提交状态
const submitting = ref(false)

// 下拉选项数据
const firmwareOptions = ref([])
const productOptions = ref([]) // 将 deviceOptions 改为 productOptions

// 加载状态
const loadingFirmware = ref(false)
const loadingProducts = ref(false) // 将 loadingDevices 改为 loadingProducts

// 2. 修改表单数据结构以匹配 POST 参数
const formState = reactive({
  otaId: undefined, // 固件ID
  productId: undefined, // 产品ID
  versionName: '', // 版本名称
})

// 显示模态框
const showModal = () => {
  resetForm()
  // 并行加载固件和产品列表
  Promise.all([fetchFirmwareList(), fetchProductList()]).finally(() => {
    visible.value = true
  })
}

// 取消操作
const handleCancel = () => {
  if (submitting.value) {
    message.warning('正在提交中，请稍候...')
    return
  }
  resetForm()
  visible.value = false
}

// 重置表单
const resetForm = () => {
  formState.otaId = undefined
  formState.productId = undefined
  formState.versionName = ''
}

// 获取固件列表
const fetchFirmwareList = async () => {
  if (loadingFirmware.value) return

  loadingFirmware.value = true
  try {
    const res = await otaList()
    const { data, errorCode } = res.data

    if (errorCode === 2001) {
      message.error('登录已过期，请重新登录')
      router.push('/login')
      return
    }

    if (errorCode === 200) {
      // 3. 修改数据处理，value为otaId，label为名称
      firmwareOptions.value = data.map((item) => ({
        value: item.id, // 使用固件ID作为value
        label: item.name, // 使用固件名称作为label
        title: `${item.name} (路径: ${item.path})`,
      }))
    } else {
      message.error('获取固件列表失败')
      firmwareOptions.value = []
    }
  } catch (error) {
    console.error('获取固件列表错误:', error)
    message.error('获取固件列表失败')
    firmwareOptions.value = []
  } finally {
    loadingFirmware.value = false
  }
}

// 4. 新增获取产品列表函数 (仿照示例代码)
const fetchProductList = async () => {
  if (loadingProducts.value) return

  loadingProducts.value = true
  try {
    const res = await getProduct()
    const { data, errorCode } = res.data

    if (errorCode === 2001) {
      message.error('登录已过期，请重新登录')
      router.push('/login')
      return
    }

    if (errorCode === 200) {
      productOptions.value = data.map((item) => ({
        value: item.id, // 使用产品ID作为value
        label: item.productName, // 使用产品名称作为label
      }))
    } else {
      message.error('获取产品列表失败')
      productOptions.value = []
    }
  } catch (error) {
    console.error('获取产品列表错误:', error)
    message.error('获取产品列表失败')
    productOptions.value = []
  } finally {
    loadingProducts.value = false
  }
}

// 搜索过滤函数
const filterOption = (input, option) => {
  return option.label.toLowerCase().includes(input.toLowerCase())
}

// 处理提交
const handleSubmit = async () => {
  submitting.value = true

  try {
    const hideMessage = message.loading('正在提交配置，请稍候...', 0)

    const submitData = toRaw(formState)
    console.log('提交数据:', submitData)

    // 5. 调用新的 API
    const res = await postOtaXiaoZhiPassive(submitData)

    hideMessage()

    const { errorCode } = res.data
    console.log('提交响应:', res.data)

    if (errorCode === 200) {
      message.success('配置创建成功!')
      resetForm()
      visible.value = false
      // 可以在这里触发父组件的刷新事件，例如 emit('success')
    } else if (errorCode === 2001) {
      message.error('登录已过期，请重新登录')
      router.push('/login')
    } else {
      message.error(`创建失败: ${res.data.errorMsg || '未知错误'}`)
    }

  } catch (error) {
    console.error('提交错误:', error)
    message.error(`创建失败: ${error.message || '网络错误'}`)
  } finally {
    submitting.value = false
  }
}

// 表单提交成功
const onFinish = (values) => {
  console.log('表单验证成功:', values)
  handleSubmit()
}

// 表单提交失败
const onFinishFailed = (errorInfo) => {
  console.log('表单验证失败:', errorInfo)
  message.error('请检查表单信息')
}
</script>
