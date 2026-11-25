<template>
  <div class="product-container">
    <HeaderCard :device="counts" />
    <div class="product-content">
      <div class="action-bar">
        <a-space wrap>
          <a-button type="primary" :disabled="!products.length" @click="openBinding">
            新建配置
          </a-button>
          <a-select
            v-model:value="productId"
            :options="products"
            placeholder="请选择产品"
            class="product-select"
            @change="changeProduct"
          />
        </a-space>
      </div>

      <a-table
        :columns="columns"
        :data-source="devices"
        :loading="loading"
        :pagination="{ pageSize: 5 }"
        :scroll="{ x: 'max-content' }"
        row-key="id"
        class="custom-table"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'product'">
            {{ productName }}
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 'ACTIVE' ? 'success' : 'default'" class="status-tag">
              {{ record.status === 'ACTIVE' ? '已绑定' : record.status === 'REVOKED' ? '已解绑' : record.status }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'voiceConnected'">
            <a-tag :color="record.voiceConnected ? 'success' : 'error'" class="status-tag">
              {{ record.voiceConnected ? '已连接' : '未连接' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'mqttConnected'">
            <a-tag :color="record.mqttConnected ? 'success' : 'default'" class="status-tag">
              {{ record.mqttConnected ? '已连接' : '未连接' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'lastSeenAt'">
            {{ record.lastSeenAt ? new Date(record.lastSeenAt).toLocaleString() : '—' }}
          </template>
          <template v-else-if="column.key === 'action' && record.status === 'ACTIVE'">
            <a-space>
              <a-button type="link" @click="openDetails(record)">设备详情</a-button>
              <a-button type="link" danger @click="confirmUnbind(record)">
                <template #icon><DeleteOutlined /></template>
                解除绑定
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <a-modal
      v-model:open="bindingOpen"
      title="新建 Rodak 设备绑定"
      ok-text="绑定"
      cancel-text="取消"
      :confirm-loading="binding"
      :closable="!binding"
      :mask-closable="!binding"
      :keyboard="!binding"
      :cancel-button-props="{ disabled: binding }"
      @ok="submitBinding"
    >
      <a-form
        ref="bindingForm"
        :model="form"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
        autocomplete="off"
        @finish="bindDevice"
      >
        <a-form-item
          label="配对码"
          name="pairingCode"
          :rules="[{ required: true, pattern: /^\d{8}$/, message: '请输入设备显示的 8 位数字配对码' }]"
        >
          <a-input
            v-model:value="form.pairingCode"
            :maxlength="8"
            inputmode="numeric"
            placeholder="请输入设备显示的配对码"
            @press-enter.prevent="submitBinding"
          />
        </a-form-item>
        <a-form-item
          label="产品"
          name="productId"
          :rules="[{ required: true, message: '请选择产品' }]"
        >
          <a-select v-model:value="form.productId" :options="products" placeholder="请选择产品" />
        </a-form-item>
        <a-form-item v-if="isAdmin && (requestsLoading || requests.length)" label="待认领申请">
          <a-select
            v-model:value="form.requestId"
            :options="requests"
            :loading="requestsLoading"
            placeholder="可选，按设备标识选择"
            allow-clear
            show-search
            option-filter-prop="label"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="detailsOpen"
      title="Rodak 设备详情"
      :footer="null"
      @cancel="closeDetails"
    >
      <a-descriptions v-if="selectedDevice" :column="1" size="small" bordered>
        <a-descriptions-item label="设备标识">{{ selectedDevice.deviceKey }}</a-descriptions-item>
        <a-descriptions-item label="上报时间">
          {{ telemetryAt ? new Date(telemetryAt).toLocaleString() : '—' }}
        </a-descriptions-item>
        <a-descriptions-item label="固件版本">{{ telemetry.firmware ?? reported.firmware ?? '—' }}</a-descriptions-item>
        <a-descriptions-item label="Wi-Fi 信号">
          {{ telemetry.wifi_rssi != null ? `${telemetry.wifi_rssi} dBm` : '—' }}
        </a-descriptions-item>
        <a-descriptions-item label="电量">
          {{ telemetry.battery != null ? `${telemetry.battery}%` : '—' }}
        </a-descriptions-item>
        <a-descriptions-item label="设备音量">{{ reported.volume ?? '—' }}</a-descriptions-item>
        <a-descriptions-item label="目标音量">{{ selectedDevice.shadow?.desired?.volume ?? '—' }}</a-descriptions-item>
      </a-descriptions>
      <a-space wrap class="details-actions">
        <a-input-number v-model:value="volume" :min="0" :max="100" :precision="0" placeholder="音量" />
        <a-button type="primary" :loading="savingVolume" :disabled="volume == null" @click="applyVolume">
          设置音量
        </a-button>
        <a-button :loading="pinging" @click="ping">发送 Ping</a-button>
      </a-space>
      <a-alert v-if="pingNotice" :type="pingNotice.type" :message="pingNotice.message" show-icon />
    </a-modal>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useStore } from 'vuex'
import { jwtDecode } from 'jwt-decode'
import { Modal, message } from 'ant-design-vue'
import { DeleteOutlined } from '@ant-design/icons-vue'
import HeaderCard from '@/views/productXiaoZhi/HeaderCard.vue'
import { getProduct } from '@/api/product'
import {
  confirmRodakPairing,
  getRodakDevices,
  getRodakPairingRequests,
  getRodakCommand,
  pingRodakDevice,
  setRodakVolume,
  unbindRodakDevice,
} from '@/api/rodak'

const store = useStore()
const isAdmin = computed(() => {
  const token = store.getters['auth/token']
  return token && jwtDecode(token).role === '[ROLE_admin]'
})
const productId = ref()
const products = ref([])
const devices = ref([])
const loading = ref(false)
const bindingOpen = ref(false)
const binding = ref(false)
const bindingForm = ref()
const requests = ref([])
const requestsLoading = ref(false)
const form = reactive({ productId: undefined, pairingCode: '', requestId: undefined })
const detailsOpen = ref(false)
const detailsId = ref()
const selectedDevice = computed(() => devices.value.find((device) => device.id === detailsId.value))
const telemetry = computed(() => selectedDevice.value?.shadow?.telemetry ?? {})
const reported = computed(() => selectedDevice.value?.shadow?.reported ?? {})
const telemetryAt = computed(() => selectedDevice.value?.shadow?.telemetryAt)
const volume = ref()
const savingVolume = ref(false)
const pinging = ref(false)
const pingNotice = ref()
const DEVICE_REFRESH_INTERVAL = 3000
let deviceRefreshTimer
let deviceRequestInFlight = false
let disposed = false
let pingTimer
let detailsVersion = 0
let loadVersion = 0

const productName = computed(() => products.value.find((product) => product.value === productId.value)?.label)
const counts = computed(() => {
  const bound = devices.value.filter((device) => device.status === 'ACTIVE')
  const connected = bound.filter((device) => device.voiceConnected || device.mqttConnected || device.recentlySeen).length
  return { num: bound.length, connected, disconnected: bound.length - connected }
})

const columns = [
  { title: '产品', key: 'product' },
  { title: '设备标识', dataIndex: 'deviceKey', key: 'deviceKey' },
  { title: '绑定状态', key: 'status' },
  { title: '语音连接', key: 'voiceConnected' },
  { title: 'MQTT 连接', key: 'mqttConnected' },
  { title: '最近连接', key: 'lastSeenAt' },
  { title: '凭据版本', dataIndex: 'tokenVersion', key: 'tokenVersion' },
  { title: '操作', key: 'action' },
]

function readResult({ data }) {
  if (!data?.success) throw new Error(data?.errorMsg || '处理失败')
  return data.data
}

async function loadDevices(background = false) {
  if (disposed || !productId.value) return
  const version = ++loadVersion
  const selected = productId.value
  deviceRequestInFlight = true
  if (!background) loading.value = true
  try {
    const rows = readResult(await getRodakDevices(selected))
    if (loadVersion === version) devices.value = rows
  } catch (error) {
    if (!background && loadVersion === version) message.error(error.message || '加载设备失败')
  } finally {
    if (loadVersion === version) {
      deviceRequestInFlight = false
      loading.value = false
    }
  }
}

function changeProduct() {
  devices.value = []
  loadDevices()
}

function openDetails(device) {
  closeDetails()
  detailsId.value = device.id
  volume.value = device.shadow?.desired?.volume ?? device.shadow?.reported?.volume
  detailsOpen.value = true
}

function closeDetails() {
  detailsVersion++
  clearTimeout(pingTimer)
  pingNotice.value = undefined
  pinging.value = false
}

async function applyVolume() {
  const device = selectedDevice.value
  if (!device || !Number.isInteger(volume.value)) return
  savingVolume.value = true
  try {
    readResult(await setRodakVolume(device.id, device.productId, volume.value))
    message.success('目标音量已保存')
    await loadDevices(true)
  } catch (error) {
    message.error(error.message || '音量设置失败')
  } finally {
    savingVolume.value = false
  }
}

async function ping() {
  const device = selectedDevice.value
  if (!device) return
  const version = detailsVersion
  pinging.value = true
  pingNotice.value = { type: 'info', message: '等待设备应答' }
  try {
    const command = readResult(await pingRodakDevice(device.id, device.productId))
    if (version === detailsVersion) pollCommand(device, command.commandNo, version)
  } catch (error) {
    if (version === detailsVersion) showPingError(error.message || '发送 Ping 失败')
  }
}

async function pollCommand(device, commandNo, version) {
  try {
    const result = readResult(await getRodakCommand(device.id, device.productId, commandNo))
    if (version !== detailsVersion) return
    if (result.status === 'pending') {
      pingTimer = setTimeout(() => pollCommand(device, commandNo, version), 1500)
    } else if (result.status === 'ok' && result.acknowledgement?.result?.pong === true) {
      pinging.value = false
      pingNotice.value = { type: 'success', message: '设备已返回 Pong' }
    } else {
      showPingError(result.status === 'expired' ? '等待设备应答超时' : '设备返回错误')
    }
  } catch (error) {
    if (version === detailsVersion) showPingError(error.message || '查询 Ping 结果失败')
  }
}

function showPingError(text) {
  pinging.value = false
  pingNotice.value = { type: 'error', message: text }
}

onUnmounted(() => {
  disposed = true
  loadVersion++
  clearInterval(deviceRefreshTimer)
  closeDetails()
})

async function openBinding() {
  form.productId = productId.value
  form.pairingCode = ''
  form.requestId = undefined
  bindingOpen.value = true
  await nextTick()
  bindingForm.value?.clearValidate()
  if (!isAdmin.value || requestsLoading.value) return
  requests.value = []
  requestsLoading.value = true
  try {
    const rows = readResult(await getRodakPairingRequests())
    requests.value = rows.map((row) => ({ value: row.requestId, label: row.deviceKey }))
  } catch (error) {
    message.error(error.message || '加载待认领申请失败')
  } finally {
    requestsLoading.value = false
  }
}

async function bindDevice() {
  if (binding.value) return
  binding.value = true
  try {
    readResult(await confirmRodakPairing(form.productId, form.pairingCode, form.requestId))
    productId.value = form.productId
    bindingOpen.value = false
    message.success('绑定成功')
    await loadDevices()
  } catch (error) {
    message.error(error.message || '绑定失败')
  } finally {
    binding.value = false
  }
}

function submitBinding() {
  bindingForm.value?.validate().then(bindDevice, () => {
    // Ant Design displays validation messages next to the fields.
  })
}

function confirmUnbind(device) {
  Modal.confirm({
    title: '确认解绑',
    content: `确定要解除绑定设备「${device.deviceKey}」吗？`,
    okText: '确认',
    cancelText: '取消',
    async onOk() {
      try {
        readResult(await unbindRodakDevice(device.id, device.productId))
        message.success('解绑成功')
        await loadDevices()
      } catch (error) {
        message.error(error.message || '解绑失败')
        throw error
      }
    },
  })
}

onMounted(async () => {
  deviceRefreshTimer = setInterval(() => {
    if (!deviceRequestInFlight) loadDevices(true)
  }, DEVICE_REFRESH_INTERVAL)

  try {
    const { data } = await getProduct()
    products.value = (Array.isArray(data?.data) ? data.data : []).map((product) => ({
      value: product.id,
      label: product.productName,
    }))
    productId.value = products.value[0]?.value
    await loadDevices()
  } catch (error) {
    message.error(error.message || '加载产品失败')
  }
})
</script>

<style lang="scss" scoped>
.product-container {
  min-height: 100vh;
  padding: 24px;
  background-color: #f0f2f5;
}

.product-content {
  padding: 24px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
}

.action-bar {
  margin-bottom: 24px;
  padding-bottom: 24px;
  border-bottom: 1px solid #f0f0f0;
}

.product-select {
  min-width: 180px;
}

.details-actions {
  margin: 16px 0;
}

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

  :deep(.ant-btn-link) {
    height: auto;
    padding: 4px 0;
  }
}

.status-tag {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 13px;
}
</style>
