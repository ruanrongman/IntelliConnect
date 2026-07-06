<script setup>
import { reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  Button,
  Switch,
  Slider,
  InputNumber,
  Tooltip,
  Modal,
  Form,
  Input,
  message,
} from 'ant-design-vue'
import {
  EditOutlined,
  QuestionCircleOutlined,
} from '@ant-design/icons-vue'
import {
  getKnowledgeGraphicForgetState,
  addKnowledgeGraphicForgetToggleConfig,
  knowledgeGraphicForgetToggle,
  updateKnowledgeGraphicForgetEpoch,
  getKnowledgeGraphicForgetEpoch,
  getKnowledgeGraphicNodeLimit,
  updateKnowledgeGraphicNodeLimit,
  getKnowledgeGraphicNodeHardLimit,
  updateKnowledgeGraphicNodeHardLimit,
  getKnowledgeGraphicPromptState,
  addKnowledgeGraphicPromptToggleConfig,
  knowledgeGraphicPromptToggle,
  getProductKnowledgeGraphicPrompt,
  addProductKnowledgeGraphicPrompt,
  updateProductKnowledgeGraphicPrompt,
  deleteProductKnowledgeGraphicPrompt,
} from '@/api/knowledgeGraphic'

const props = defineProps({
  productId: { type: Number, default: null },
  repulsion: { type: Number, default: 30 },
})

const emit = defineEmits(['close', 'update:repulsion'])

const router = useRouter()

// --- State ---
const knowledgeGraphForgetEnable = ref(false)
const knowledgeGraphForgetEpoch = ref(10)
const knowledgeGraphNodeLimit = ref(100)
const knowledgeGraphNodeHardLimit = ref(300)
const knowledgeGraphicPromptEnable = ref(false)
const promptModalOpen = ref(false)
const promptLoading = ref(false)
const promptSaving = ref(false)
const promptDeleting = ref(false)
const knowledgeGraphicPromptForm = reactive({
  id: null,
  prompt: '',
})

// --- Helpers ---
function normalizeNodeLimit(value, defaultValue = 100) {
  const num = Number(value)
  if (Number.isNaN(num)) return defaultValue
  return Math.min(600, Math.max(0, Math.round(num)))
}

function normalizeKnowledgeGraphicPrompt(data) {
  if (Array.isArray(data) && data.length > 0) return data[0]
  if (data && typeof data === 'object' && !Array.isArray(data)) return data
  return null
}

function resetKnowledgeGraphicPromptForm() {
  knowledgeGraphicPromptForm.id = null
  knowledgeGraphicPromptForm.prompt = ''
}

function checkAuth(res) {
  const body = res && res.data ? res.data : res
  if (body.errorCode === 2001) {
    router.push('/login')
    return true
  }
  return false
}

// --- Data fetching ---
function loadAll() {
  if (!props.productId) return
  loadForgetState()
  loadForgetEpoch()
  loadNodeLimit()
  loadNodeHardLimit()
  loadPromptState()
}

function loadForgetState() {
  const params = { productId: props.productId }
  getKnowledgeGraphicForgetState(params).then((res) => {
    if (checkAuth(res)) return
    const { data, errorCode } = res.data
    if (errorCode !== 200) {
      message.error('获取知识图谱遗忘功能状态失败！')
      return
    }
    if (data === null) {
      addKnowledgeGraphicForgetToggleConfig(params).then(loadForgetState)
    } else {
      knowledgeGraphForgetEnable.value = data.value === 'true'
    }
  })
}

function loadForgetEpoch() {
  getKnowledgeGraphicForgetEpoch({ productId: props.productId }).then((res) => {
    if (checkAuth(res)) return
    const { data, errorCode } = res.data
    if (errorCode !== 200) {
      message.error('获取知识图谱遗忘轮次出错！')
      return
    }
    if (data === null) {
      updateKnowledgeGraphicForgetEpoch({ productId: props.productId, value: 10 })
    } else {
      knowledgeGraphForgetEpoch.value = parseInt(data.value)
    }
  })
}

function loadNodeLimit() {
  getKnowledgeGraphicNodeLimit({ productId: props.productId }).then((res) => {
    if (checkAuth(res)) return
    const { data, errorCode } = res.data
    if (errorCode !== 200) {
      message.error('获取知识图谱期望节点上限出错！')
      return
    }
    if (data === null) {
      knowledgeGraphNodeLimit.value = 100
      updateKnowledgeGraphicNodeLimit({ productId: props.productId, value: '100' })
    } else {
      knowledgeGraphNodeLimit.value = normalizeNodeLimit(data.value)
    }
  })
}

function loadNodeHardLimit() {
  getKnowledgeGraphicNodeHardLimit({ productId: props.productId }).then((res) => {
    if (checkAuth(res)) return
    const { data, errorCode } = res.data
    if (errorCode !== 200) {
      message.error('获取知识图谱节点硬上限出错！')
      return
    }
    if (data === null) {
      knowledgeGraphNodeHardLimit.value = 300
      updateKnowledgeGraphicNodeHardLimit({ productId: props.productId, value: '300' })
    } else {
      knowledgeGraphNodeHardLimit.value = normalizeNodeLimit(data.value, 300)
    }
  })
}

function loadPromptState() {
  getKnowledgeGraphicPromptState({ productId: props.productId }).then((res) => {
    if (checkAuth(res)) return
    const { data, errorCode } = res.data
    if (errorCode !== 200) {
      message.error('获取图谱提示增强状态失败！')
      return
    }
    if (data === null) {
      addKnowledgeGraphicPromptToggleConfig({ productId: props.productId }).then(loadPromptState)
    } else {
      knowledgeGraphicPromptEnable.value = data.value === 'true'
      if (knowledgeGraphicPromptEnable.value) {
        fetchProductKnowledgeGraphicPrompt()
      } else {
        resetKnowledgeGraphicPromptForm()
      }
    }
  })
}

function fetchProductKnowledgeGraphicPrompt() {
  if (!props.productId) return Promise.resolve()
  promptLoading.value = true
  return getProductKnowledgeGraphicPrompt({ productId: props.productId })
    .then((res) => {
      if (checkAuth(res)) return
      const { data, errorCode } = res.data
      if (errorCode !== 200) {
        resetKnowledgeGraphicPromptForm()
        return
      }
      const pc = normalizeKnowledgeGraphicPrompt(data)
      if (!pc) { resetKnowledgeGraphicPromptForm(); return }
      knowledgeGraphicPromptForm.id = pc.id
      knowledgeGraphicPromptForm.prompt = pc.prompt || ''
    })
    .catch(() => resetKnowledgeGraphicPromptForm())
    .finally(() => { promptLoading.value = false })
}

watch(() => props.productId, (val) => { if (val) loadAll() }, { immediate: true })

// --- Handlers ---
function handleRepulsionChange(value) {
  emit('update:repulsion', value)
}

function handleForgetToggle() {
  const data = { productId: props.productId, value: String(!knowledgeGraphForgetEnable.value) }
  knowledgeGraphicForgetToggle(data).then((res) => {
    if (checkAuth(res)) return
    message.info(knowledgeGraphForgetEnable.value ? '已关闭知识图谱遗忘功能' : '已开启知识图谱遗忘功能')
    knowledgeGraphForgetEnable.value = !knowledgeGraphForgetEnable.value
  })
}

function handleForgetEpochChange(value) {
  knowledgeGraphForgetEpoch.value = value
  updateKnowledgeGraphicForgetEpoch({ productId: props.productId, value }).then((res) => {
    if (checkAuth(res)) return
  })
}

function handleNodeLimitChange(value) {
  const norm = normalizeNodeLimit(value)
  knowledgeGraphNodeLimit.value = norm
  updateKnowledgeGraphicNodeLimit({ productId: props.productId, value: String(norm) }).then((res) => {
    if (checkAuth(res)) return
  })
}

function handleNodeHardLimitChange(value) {
  const norm = normalizeNodeLimit(value, 300)
  knowledgeGraphNodeHardLimit.value = norm
  updateKnowledgeGraphicNodeHardLimit({ productId: props.productId, value: String(norm) }).then((res) => {
    if (checkAuth(res)) return
  })
}

function handlePromptToggle() {
  const data = { productId: props.productId, value: String(!knowledgeGraphicPromptEnable.value) }
  knowledgeGraphicPromptToggle(data).then((res) => {
    if (checkAuth(res)) return
    if (knowledgeGraphicPromptEnable.value) {
      message.info('已关闭图谱提示增强')
      promptModalOpen.value = false
    } else {
      message.info('已开启图谱提示增强')
      fetchProductKnowledgeGraphicPrompt()
    }
    knowledgeGraphicPromptEnable.value = !knowledgeGraphicPromptEnable.value
  })
}

function handleOpenPromptModal() {
  if (!props.productId || !knowledgeGraphicPromptEnable.value) return
  promptModalOpen.value = true
  fetchProductKnowledgeGraphicPrompt()
}

function handleClosePromptModal() {
  promptModalOpen.value = false
}

function handleSavePrompt() {
  const prompt = knowledgeGraphicPromptForm.prompt.trim()
  if (!prompt) { message.warning('请输入背景信息及要求'); return }
  promptSaving.value = true
  const hasExisting = Boolean(knowledgeGraphicPromptForm.id)
  const payload = { productId: props.productId, prompt }
  const req = hasExisting
    ? updateProductKnowledgeGraphicPrompt({ ...payload, id: knowledgeGraphicPromptForm.id })
    : addProductKnowledgeGraphicPrompt(payload)
  req
    .then((res) => {
      const { data, errorCode, errorMsg } = res.data
      if (errorCode !== 200) { message.error(errorMsg || '保存失败'); return }
      const pc = normalizeKnowledgeGraphicPrompt(data)
      if (pc) { knowledgeGraphicPromptForm.id = pc.id; knowledgeGraphicPromptForm.prompt = pc.prompt || prompt }
      message.success(hasExisting ? '已修改图谱提示增强' : '已设置图谱提示增强')
      promptModalOpen.value = false
    })
    .catch(() => message.error('保存失败'))
    .finally(() => { promptSaving.value = false })
}

function handleDeletePrompt() {
  if (!knowledgeGraphicPromptForm.id) {
    resetKnowledgeGraphicPromptForm()
    message.info('当前没有可删除的配置')
    return
  }
  Modal.confirm({
    title: '删除图谱提示增强？',
    content: '删除后，背景信息及要求将不再参与知识图谱生成。',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      promptDeleting.value = true
      try {
        const res = await deleteProductKnowledgeGraphicPrompt({ id: knowledgeGraphicPromptForm.id })
        const { errorCode, errorMsg } = res.data
        if (errorCode === 200) {
          resetKnowledgeGraphicPromptForm()
          promptModalOpen.value = false
          message.success('已删除图谱提示增强')
        } else {
          message.error(errorMsg || '删除失败')
        }
      } catch { message.error('删除失败') }
      finally { promptDeleting.value = false }
    },
  })
}
</script>

<template>
  <div class="config-panel">
    <div class="config-panel-header">
      <span class="config-panel-title">图谱配置</span>
      <Button type="text" class="config-panel-close" @click="emit('close')">✕</Button>
    </div>
    <div class="config-panel-body">
      <div class="config-section">
        <div class="config-row">
          <label class="option-label" for="prompt-toggle-config">图谱提示增强</label>
          <Switch
            id="prompt-toggle-config"
            :checked="knowledgeGraphicPromptEnable"
            @click="handlePromptToggle"
          />
        </div>
        <div v-if="knowledgeGraphicPromptEnable" class="config-row config-row-sub">
          <Button :loading="promptLoading" @click="handleOpenPromptModal">
            <template #icon><EditOutlined /></template>
            编辑提示内容
          </Button>
        </div>
      </div>
      <div class="config-divider"></div>
      <div class="config-section">
        <div class="config-row">
          <label class="option-label" for="repulsion-config">节点斥力大小（边长）</label>
        </div>
        <div class="config-row config-slider-row">
          <Slider
            id="repulsion-config"
            :max="255"
            :value="repulsion"
            @change="handleRepulsionChange"
          />
        </div>
      </div>
      <div class="config-divider"></div>
      <div class="config-section">
        <div class="config-row">
          <label class="option-label" for="expected-node-limit-config">
            <Tooltip title="提示给模型的期望节点上限，0 表示不希望自动新增节点">
              <QuestionCircleOutlined />
            </Tooltip>
            期望节点上限
          </label>
        </div>
        <div class="config-row config-slider-with-input">
          <Slider
            id="expected-node-limit-config"
            :min="0"
            :max="600"
            :value="knowledgeGraphNodeLimit"
            @change="handleNodeLimitChange"
          />
          <InputNumber
            :min="0"
            :max="600"
            :precision="0"
            :value="knowledgeGraphNodeLimit"
            @change="handleNodeLimitChange"
          />
        </div>
      </div>
      <div class="config-divider"></div>
      <div class="config-section">
        <div class="config-row">
          <label class="option-label" for="hard-node-limit-config">
            <Tooltip title="后端实际写入保护，不会发送给模型，0 表示不允许自动生成新增节点落库">
              <QuestionCircleOutlined />
            </Tooltip>
            节点硬上限
          </label>
        </div>
        <div class="config-row config-slider-with-input">
          <Slider
            id="hard-node-limit-config"
            :min="0"
            :max="600"
            :value="knowledgeGraphNodeHardLimit"
            @change="handleNodeHardLimitChange"
          />
          <InputNumber
            :min="0"
            :max="600"
            :precision="0"
            :value="knowledgeGraphNodeHardLimit"
            @change="handleNodeHardLimitChange"
          />
        </div>
      </div>
      <div class="config-divider"></div>
      <div class="config-section">
        <div class="config-row">
          <label class="option-label" for="forget-toggle-config">知识图谱遗忘开关</label>
          <Switch
            id="forget-toggle-config"
            :checked="knowledgeGraphForgetEnable"
            @click="handleForgetToggle"
          />
        </div>
        <div v-if="knowledgeGraphForgetEnable" class="config-row config-slider-row">
          <label for="forget-epoch-config" class="option-label">
            <Tooltip title="对话达到这个轮次后会自动执行一次节点清理（模拟遗忘），随后重新计数。注意！这个值小于4时清理永远不会生效！">
              <QuestionCircleOutlined />
            </Tooltip>
            遗忘轮次
          </label>
          <Slider
            id="forget-epoch-config"
            :min="3"
            :max="40"
            :value="knowledgeGraphForgetEpoch"
            @change="handleForgetEpochChange"
          />
        </div>
      </div>
    </div>
    <Modal
      :open="promptModalOpen"
      title="图谱提示增强"
      :confirm-loading="promptSaving"
      ok-text="保存"
      cancel-text="取消"
      @cancel="handleClosePromptModal"
      @ok="handleSavePrompt"
    >
      <Form
        :model="knowledgeGraphicPromptForm"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 17 }"
      >
        <Form.Item
          label="背景要求"
          name="prompt"
          :rules="[{ required: true, message: '请输入背景信息及要求' }]"
        >
          <Input.TextArea
            v-model:value="knowledgeGraphicPromptForm.prompt"
            :maxlength="2048"
            :auto-size="{ minRows: 6, maxRows: 10 }"
            show-count
            placeholder="例如：优先抽取设备、场景、故障现象之间的关系；不要把临时对话称呼写入图谱。"
          />
        </Form.Item>
        <Form.Item :wrapper-col="{ offset: 6, span: 17 }">
          <Button
            danger
            :disabled="!knowledgeGraphicPromptForm.id"
            :loading="promptDeleting"
            @click="handleDeletePrompt"
          >
            删除
          </Button>
        </Form.Item>
      </Form>
    </Modal>
  </div>
</template>

<style scoped>
.config-panel {
  position: absolute;
  right: 12px;
  top: 12px;
  bottom: 12px;
  width: calc(50% - 24px);
  min-width: 380px;
  max-width: 520px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  z-index: 10;
  display: flex;
  flex-direction: column;
}

.config-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px 0;
  flex-shrink: 0;
}

.config-panel-title {
  font-size: 16px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
}

.config-panel-close {
  font-size: 16px;
  color: rgba(0, 0, 0, 0.45);
}

.config-panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px 20px;
}

.config-section {
  margin-bottom: 16px;
}

.config-section:last-child {
  margin-bottom: 0;
}

.config-row {
  display: flex;
  align-items: center;
  min-height: 32px;
}

.config-row-sub {
  margin-top: 8px;
  padding-left: 0;
}

.config-slider-row {
  padding: 0 8px;
  flex-wrap: nowrap;
  gap: 8px;
}

.config-slider-row .option-label {
  white-space: nowrap;
  flex-shrink: 0;
}

.config-slider-with-input {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 8px;
}

.config-slider-with-input :deep(.ant-slider) {
  flex: 1;
}

.config-slider-row :deep(.ant-slider) {
  width: 100%;
}

.config-divider {
  height: 1px;
  background: #f0f0f0;
  margin: 12px 0;
}

.option-label {
  margin-right: 5px;
}
</style>
