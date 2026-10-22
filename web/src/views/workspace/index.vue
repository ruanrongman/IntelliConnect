<template>
  <div class="workspace-page" :class="{ 'workspace-embedded': embedded }">
    <a-card :bordered="false">
      <div v-if="!embedded" class="workspace-heading">
        <div>
          <h2>会话工作区</h2>
          <p>保存上传文件与智能体产物。删除对话或回收沙箱后，文件仍会保留。</p>
        </div>
        <a-button :loading="loading" :disabled="!productId" @click="refresh">刷新</a-button>
      </div>
      <div class="workspace-toolbar">
        <a-select v-if="!embedded" v-model:value="productId" class="product-select" placeholder="选择产品"
          :options="productOptions" :disabled="uploading" @change="changeProduct" />
        <a-select v-if="!embedded" v-model:value="chatId" class="conversation-select" placeholder="选择会话"
          show-search :options="conversationOptions" :disabled="uploading || !productId"
          @change="changeConversation" />
        <a-button type="primary" :disabled="!chatId || loading" :loading="uploading"
          @click="fileInput.click()"><template #icon><UploadOutlined /></template>上传文件</a-button>
        <a-button v-if="embedded" :loading="loading" :disabled="uploading" @click="refresh">
          <template #icon><ReloadOutlined /></template>刷新
        </a-button>
        <input ref="fileInput" type="file" multiple hidden @change="upload" />
      </div>
      <div v-if="chatId" class="workspace-usage">
        <span>{{ embedded ? `${pagination.total} 个文件` : chatId }}</span>
        <span>{{ formatSize(usedBytes) }} / {{ formatSize(maxBytes) }}，单文件上限 {{ formatSize(maxFileBytes) }}</span>
      </div>
      <a-progress v-if="uploading" :percent="uploadPercent" size="small" />
      <a-alert v-if="errorText" :message="errorText" type="error" show-icon class="workspace-error" />
      <a-table :columns="columns" :data-source="files" :loading="loading" row-key="id"
        :pagination="pagination" :scroll="{ x: 800 }" @change="changePage">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <span class="file-name">{{ record.name }}</span>
          </template>
          <template v-else-if="column.key === 'size'">{{ formatSize(record.size) }}</template>
          <template v-else-if="column.key === 'source'">
            <a-tag>{{ sources[record.source] || record.source }}</a-tag>
          </template>
          <template v-else-if="column.key === 'createdAt'">{{ new Date(record.createdAt).toLocaleString() }}</template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-button type="link" size="small" @click="preview(record)">预览文本</a-button>
              <a-button type="link" size="small" @click="download(record)">下载</a-button>
              <a-popconfirm title="删除此文件？此操作不可撤销。" @confirm="remove(record)">
                <a-button type="link" size="small" danger :disabled="uploading">删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
        <template #emptyText><a-empty description="当前会话暂无文件" /></template>
      </a-table>
    </a-card>
    <a-modal v-model:open="previewOpen" :title="previewName" :footer="null" width="850px"
      @cancel="closePreview">
      <a-alert v-if="previewError" :message="previewError" type="info" show-icon />
      <pre class="file-preview">{{ previewContent }}</pre>
      <a-button v-if="previewNext !== null" :loading="previewLoading" @click="loadPreview">
        读取下一段
      </a-button>
      <p v-if="previewContent" class="preview-note">按普通文本显示，复杂文档请下载或交给智能体在沙箱中处理。</p>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { getProduct } from '@/api/product'
import { getAgentMemoryByNickName } from '@/api/agentMemory'
import {
  listWorkspaces, listWorkspaceFiles, uploadWorkspaceFiles, readWorkspaceFile,
  downloadWorkspaceFile, deleteWorkspaceFile, workspaceError,
} from '@/api/workspace'

const props = defineProps({
  embedded: { type: Boolean, default: false },
  productId: { type: [Number, String], default: undefined },
  chatId: { type: String, default: '' },
})
const route = useRoute()
const router = useRouter()
const productId = ref(props.productId)
const chatId = ref(props.chatId)
const products = ref([])
const conversations = ref([])
const files = ref([])
const loading = ref(false)
const uploading = ref(false)
const uploadPercent = ref(0)
const fileInput = ref()
const errorText = ref('')
const usedBytes = ref(0)
const maxBytes = ref(500 * 1024 * 1024)
const maxFileBytes = ref(50 * 1024 * 1024)
const pagination = reactive({ current: 1, pageSize: 20, total: 0, showSizeChanger: false })
const productOptions = computed(() => products.value.map((item) => ({
  value: item.id, label: `${item.productName || '未命名产品'} #${item.id}`,
})))
const conversationOptions = computed(() => conversations.value.map((item) => ({
  value: item.chatId, label: item.label || item.chatId,
})))
const sources = { upload: '页面上传', chat: '聊天附件', agent: '智能体文本', sandbox: '沙箱产物' }
const columns = [
  { title: '文件名', dataIndex: 'name', key: 'name', ellipsis: true },
  { title: '大小', key: 'size', width: 110 },
  { title: '来源', key: 'source', width: 115 },
  { title: '保存时间', key: 'createdAt', width: 190 },
  { title: '操作', key: 'actions', width: 240 },
]
let sequence = 0
let previewSequence = 0
const previewOpen = ref(false)
const previewName = ref('')
const previewContent = ref('')
const previewError = ref('')
const previewNext = ref(null)
const previewLoading = ref(false)
let previewFile = null

function params() { return { productId: productId.value, chatId: chatId.value } }
function formatSize(value) {
  const bytes = Number(value || 0)
  return bytes >= 1024 * 1024 ? `${(bytes / 1024 / 1024).toFixed(1)} MiB`
    : bytes >= 1024 ? `${(bytes / 1024).toFixed(1)} KiB` : `${bytes} B`
}

async function refresh() {
  if (!productId.value) return
  if (props.embedded) return loadFiles()
  const current = ++sequence
  const selectedProduct = productId.value
  loading.value = true
  errorText.value = ''
  try {
    const saved = []
    let page = 1
    let result
    let totalPages
    do {
      result = await listWorkspaces({ productId: selectedProduct, pageNum: page++, pageSize: 100 })
      saved.push(...(result.content || []))
      totalPages = Number(result.totalPages ?? result.page?.totalPages ?? 1)
    } while (page <= totalPages && current === sequence)
    const memoryResponse = await getAgentMemoryByNickName({ productId: selectedProduct })
    if (current !== sequence) return
    const choices = new Map()
    const defaultChatId = `chatProduct${selectedProduct}`
    choices.set(defaultChatId, { chatId: defaultChatId, label: `默认对话 · ${defaultChatId}` })
    const memories = memoryResponse.data?.errorCode === 200 ? memoryResponse.data.data : []
    ;(Array.isArray(memories) ? memories : []).forEach((item) => {
      if (item.chatId) choices.set(item.chatId, { chatId: item.chatId,
        label: `${item.deviceName || '对话'} · ${item.chatId}` })
    })
    saved.forEach((item) => {
      if (!choices.has(item.chatId)) choices.set(item.chatId, { chatId: item.chatId,
        label: `已保存工作区 · ${item.chatId}` })
    })
    // A newly drafted debug conversation may not have a memory row yet.
    const requested = String(route.query.chatId || '')
    if (requested && String(route.query.productId) === String(selectedProduct)) {
      choices.set(requested, choices.get(requested) || { chatId: requested, label: requested })
    }
    conversations.value = Array.from(choices.values())
    if (!choices.has(chatId.value)) chatId.value = choices.has(requested) ? requested : defaultChatId
    await loadFiles(current)
  } catch (error) {
    if (current === sequence) errorText.value = await workspaceError(error)
  } finally {
    if (current === sequence) loading.value = false
  }
}

async function loadFiles(current = ++sequence) {
  if (!chatId.value) return
  loading.value = true
  errorText.value = ''
  try {
    const data = await listWorkspaceFiles({ ...params(), pageNum: pagination.current, pageSize: pagination.pageSize })
    if (current !== sequence) return
    files.value = data.files?.content || []
    pagination.total = Number(data.files?.totalElements ?? data.files?.page?.totalElements ?? 0)
    usedBytes.value = data.usedBytes
    maxBytes.value = data.maxBytes
    maxFileBytes.value = data.maxFileBytes
  } catch (error) {
    if (current === sequence) { files.value = []; errorText.value = await workspaceError(error) }
  } finally {
    if (current === sequence) loading.value = false
  }
}

function changeProduct() {
  chatId.value = ''
  files.value = []
  conversations.value = []
  pagination.current = 1
  closePreview()
  router.replace({ path: '/workspace', query: { productId: productId.value } })
  refresh()
}
function changeConversation() {
  pagination.current = 1
  closePreview()
  router.replace({ path: '/workspace', query: params() })
  loadFiles()
}
function changePage(page) { pagination.current = page.current; loadFiles() }

async function upload(event) {
  const selected = Array.from(event.target.files || [])
  event.target.value = ''
  if (!selected.length || uploading.value) return
  if (selected.some((file) => !file.size || file.size > maxFileBytes.value)) {
    message.error(`请选择非空文件，单文件不超过 ${formatSize(maxFileBytes.value)}`)
    return
  }
  if (selected.reduce((total, file) => total + file.size, usedBytes.value) > maxBytes.value) {
    message.error('工作区容量不足')
    return
  }
  uploading.value = true
  uploadPercent.value = 0
  try {
    await uploadWorkspaceFiles(productId.value, chatId.value, selected, (progress) => {
      uploadPercent.value = Math.min(99, Math.round(progress.loaded / (progress.total || 1) * 100))
    })
    message.success('文件已保存到工作区')
    pagination.current = 1
    await refresh()
  } catch (error) { message.error(await workspaceError(error)) }
  finally { uploading.value = false }
}
async function download(file) {
  try { await downloadWorkspaceFile(file, params()) }
  catch (error) { message.error(await workspaceError(error)) }
}
async function remove(file) {
  const current = sequence
  const scope = params()
  try {
    await deleteWorkspaceFile(file.id, scope)
    if (current !== sequence) return
    if (files.value.length === 1 && pagination.current > 1) pagination.current--
    await loadFiles()
  } catch (error) { message.error(await workspaceError(error)) }
}
function closePreview() {
  previewSequence++
  previewOpen.value = false
  previewFile = null
  previewLoading.value = false
}
async function preview(file) {
  closePreview()
  previewFile = { file, scope: params() }
  previewName.value = file.name
  previewContent.value = ''
  previewError.value = ''
  previewNext.value = 0
  previewOpen.value = true
  await loadPreview()
}
async function loadPreview() {
  if (!previewFile || previewLoading.value) return
  const current = ++previewSequence
  previewLoading.value = true
  try {
    const data = await readWorkspaceFile(previewFile.file.id, {
      ...previewFile.scope, offset: previewNext.value, limit: 16000,
    })
    if (current !== previewSequence) return
    previewContent.value = data.content
    previewNext.value = data.truncated ? data.nextOffset : null
  } catch (error) {
    if (current === previewSequence) { previewError.value = await workspaceError(error); previewNext.value = null }
  } finally {
    if (current === previewSequence) previewLoading.value = false
  }
}

onMounted(async () => {
  if (props.embedded) {
    await loadFiles()
    return
  }
  try {
    const response = await getProduct()
    if (response.data?.errorCode !== 200) throw new Error(response.data?.errorMsg || '产品加载失败')
    products.value = response.data.data || []
    productId.value = products.value.find((item) => String(item.id) === String(route.query.productId))?.id
      || products.value[0]?.id
    chatId.value = String(route.query.chatId || '')
    await refresh()
  } catch (error) { errorText.value = await workspaceError(error) }
})

onBeforeUnmount(() => {
  sequence++
  closePreview()
})
</script>

<style scoped>
.workspace-page { padding: 20px; }
.workspace-embedded { padding: 0 !important; }
.workspace-embedded :deep(.ant-card-body) { padding: 0; }
.workspace-heading { display: flex; justify-content: space-between; gap: 16px; margin-bottom: 20px; }
.workspace-heading h2 { margin: 0 0 8px; }
.workspace-heading p, .preview-note { color: #7c8798; margin: 0; }
.workspace-toolbar { display: flex; gap: 12px; flex-wrap: wrap; margin-bottom: 16px; }
.product-select { min-width: 200px; }
.conversation-select { min-width: 260px; flex: 1; }
.workspace-usage { display: flex; justify-content: space-between; gap: 12px; flex-wrap: wrap; margin-bottom: 16px; color: #7c8798; overflow-wrap: anywhere; }
.workspace-error { margin-bottom: 16px; }
.file-name { overflow-wrap: anywhere; }
.file-preview { white-space: pre-wrap; overflow-wrap: anywhere; max-height: 60vh; overflow: auto; padding: 16px; background: #f4f6f9; color: #182538; }
.preview-note { margin-top: 12px; font-size: 12px; }
@media (max-width: 600px) {
  .workspace-page { padding: 8px !important; }
  .workspace-page :deep(.ant-card-body) { padding: 12px; }
  .workspace-embedded { padding: 0 !important; }
  .workspace-embedded :deep(.ant-card-body) { padding: 0; }
  .workspace-heading { flex-direction: column; }
  .workspace-heading > .ant-btn { align-self: flex-start; }
  .product-select, .conversation-select { min-width: 0; width: 100%; flex: auto; }
}
</style>
