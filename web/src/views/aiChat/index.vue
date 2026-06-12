<template>
  <div class="ai-chat-page">
    <ChatSidebar
      v-model="selectedProductId"
      :product-options="productOptions"
      :active-chat-id="activeChatId"
      :loading-products="loadingProducts"
      :streaming="streaming"
      :loading-history="loadingHistory"
      :has-more-history="hasMoreHistory"
      :history-total="historyPagination.total"
      :loaded-history-count="loadedHistoryCount"
      :status-text="statusText"
      :status-type="statusType"
      @refresh-products="loadProducts"
      @load-history="loadHistory(1)"
      @load-more-history="loadMoreHistory"
      @product-change="handleProductChange"
    />

    <main class="chat-main">
      <header class="chat-header">
        <div class="active-product">
          <strong>{{ activeProductName }}</strong>
          <span>{{ messages.length }} 条消息</span>
        </div>
        <a-button type="text" :disabled="streaming || messages.length === 0" @click="clearMessages">
          清空显示
        </a-button>
      </header>

      <ChatMessageList ref="messageListRef" :messages="messages" />

      <ChatComposer
        v-model="inputText"
        :selected-files="selectedFiles"
        :streaming="streaming"
        :can-send="canSend"
        :accepted-file-extensions="acceptedFileExtensions"
        @submit="sendMessage"
        @stop="stopStream"
        @select-file="selectFile"
        @paste="handlePaste"
        @remove-file="removeSelectedFile"
      />
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { message as antMessage } from 'ant-design-vue'
import { getProduct } from '@/api/product'
import { getHistoryMessage } from '@/api/agentMemory'
import { stopAiControlStream, streamAiControl } from '@/api/aiChat'
import ChatComposer from './ChatComposer.vue'
import ChatMessageList from './ChatMessageList.vue'
import ChatSidebar from './ChatSidebar.vue'
import { acceptedFileExtensions, acceptedFileLabel, getClipboardFiles, getFileIdentity, isAcceptedFile } from './fileUpload'

const products = ref([])
const selectedProductId = ref()
const messages = ref([])
const inputText = ref('')
const selectedFiles = ref([])
const loadingProducts = ref(false)
const loadingHistory = ref(false)
const streaming = ref(false)
const statusText = ref('正在加载产品...')
const statusType = ref('')
const messageListRef = ref(null)
let abortController = null
let scrollFrameId = 0
let currentStreamId = ''
const historyPagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
})
const loadedHistoryCount = ref(0)

const productOptions = computed(() =>
  products.value.map((item) => ({
    label: `${item.productName || '未命名产品'} #${item.id}`,
    value: item.id,
  }))
)

const activeProduct = computed(() =>
  products.value.find((item) => item.id === selectedProductId.value)
)

const activeProductName = computed(() =>
  activeProduct.value ? activeProduct.value.productName || `产品 ${activeProduct.value.id}` : '未选择产品'
)

const activeChatId = computed(() =>
  selectedProductId.value ? `chatProduct${selectedProductId.value}` : ''
)

const canSend = computed(() =>
  Boolean(selectedProductId.value && (inputText.value.trim() || selectedFiles.value.length) && !streaming.value)
)

const hasMoreHistory = computed(() =>
  historyPagination.total > 0 && loadedHistoryCount.value < historyPagination.total
)

onMounted(() => {
  loadProducts()
})

onBeforeUnmount(() => {
  if (abortController) {
    abortController.abort()
  }
  if (scrollFrameId) {
    window.cancelAnimationFrame(scrollFrameId)
  }
})

async function loadProducts() {
  loadingProducts.value = true
  statusText.value = '正在加载产品...'
  statusType.value = ''
  try {
    const res = await getProduct()
    const payload = res.data || {}
    if (payload.errorCode !== 200) {
      throw new Error(payload.errorMsg || '产品加载失败')
    }
    products.value = Array.isArray(payload.data) ? payload.data : []
    if (!selectedProductId.value && products.value.length > 0) {
      selectedProductId.value = products.value[0].id
    }
    statusText.value = `已加载 ${products.value.length} 个产品。`
    if (selectedProductId.value) {
      await loadHistory(1)
    }
  } catch (error) {
    products.value = []
    selectedProductId.value = undefined
    statusText.value = error.message || '产品加载失败'
    statusType.value = 'warn'
  } finally {
    loadingProducts.value = false
  }
}

async function loadHistory(page = 1, mode = 'replace') {
  if (!selectedProductId.value) {
    return
  }
  const previousScrollHeight = getMessagePane()?.scrollHeight || 0
  historyPagination.current = page
  loadingHistory.value = true
  statusText.value = '正在加载历史...'
  statusType.value = ''
  try {
    const res = await getHistoryMessage({
      pageNum: historyPagination.current,
      pageSize: historyPagination.pageSize,
      chatId: activeChatId.value,
    })
    const payload = res.data || {}
    if (payload.errorCode !== 200) {
      if (mode === 'replace') {
        messages.value = []
        historyPagination.total = 0
        loadedHistoryCount.value = 0
        statusText.value = '暂无历史消息。'
      } else {
        statusText.value = '没有更早的历史。'
      }
      return
    }
    const pageData = payload.data || {}
    const page = pageData.page || {}
    const records = Array.isArray(pageData.content)
      ? pageData.content.slice().reverse()
      : []
    historyPagination.total = Number(pageData.totalElements ?? page.totalElements ?? 0)
    historyPagination.current = Number(pageData.number ?? page.number ?? 0) + 1
    historyPagination.pageSize = Number(pageData.size ?? page.size ?? historyPagination.pageSize)
    const historyMessages = records.map((item) => {
      const parsedContent = parseMessageContent(item.content || '')
      return {
        id: item.id || `${item.requestId}-${item.sequenceNum}`,
        role: item.messageType === 'user' ? 'user' : 'assistant',
        content: parsedContent.content,
        fileNames: parsedContent.fileNames,
        pending: false,
      }
    })
    if (mode === 'prepend') {
      messages.value = [...historyMessages, ...messages.value]
      loadedHistoryCount.value += historyMessages.length
      statusText.value = `已加载到第 ${historyPagination.current} 页，共 ${historyPagination.total} 条。`
      await keepScrollPositionAfterPrepend(previousScrollHeight)
    } else {
      messages.value = historyMessages
      loadedHistoryCount.value = historyMessages.length
      statusText.value = `已显示最新 ${loadedHistoryCount.value} / ${historyPagination.total} 条。`
      await scrollToBottom()
    }
  } catch (error) {
    if (mode === 'replace') {
      messages.value = []
      historyPagination.total = 0
      loadedHistoryCount.value = 0
    }
    statusText.value = error.message || '历史加载失败'
    statusType.value = 'warn'
  } finally {
    loadingHistory.value = false
  }
}

async function loadMoreHistory() {
  if (!hasMoreHistory.value) {
    return
  }
  await loadHistory(historyPagination.current + 1, 'prepend')
}

async function handleProductChange() {
  historyPagination.current = 1
  historyPagination.total = 0
  loadedHistoryCount.value = 0
  await loadHistory(1)
}

async function sendMessage() {
  if (!canSend.value) {
    return
  }
  const text = inputText.value.trim()
  const requestText = text || '请阅读上传文件。'
  const files = [...selectedFiles.value]
  inputText.value = ''
  selectedFiles.value = []
  const assistantMessage = {
    id: `assistant-${Date.now()}`,
    role: 'assistant',
    content: '',
    pending: true,
  }
  messages.value.push({
    id: `user-${Date.now()}`,
    role: 'user',
    content: requestText,
    fileNames: files.map((file) => file.name),
    pending: false,
  })
  const assistantMessageIndex = messages.value.push(assistantMessage) - 1
  await scrollToBottom()

  streaming.value = true
  abortController = new AbortController()
  currentStreamId = createStreamId()
  statusText.value = '正在生成回复...'
  statusType.value = ''
  try {
    await streamAiControl({
      productId: selectedProductId.value,
      streamId: currentStreamId,
      content: requestText,
      files,
      signal: abortController.signal,
      onMessage: (chunk) => {
        const currentMessage = messages.value[assistantMessageIndex]
        if (currentMessage) {
          currentMessage.content += chunk
          queueScrollToBottom()
        }
      },
      onError: (errorText) => {
        const currentMessage = messages.value[assistantMessageIndex]
        if (currentMessage) {
          currentMessage.content += errorText
        }
        statusText.value = errorText
        statusType.value = 'warn'
      },
    })
    const currentMessage = messages.value[assistantMessageIndex]
    if (currentMessage && !currentMessage.content.trim()) {
      currentMessage.content = '请求已结束。'
    }
    statusText.value = '回复完成，正在刷新历史...'
    await loadHistory(1)
  } catch (error) {
    if (error.name === 'AbortError') {
      statusText.value = '已停止。'
    } else {
      const errorText = error.message || '发送失败'
      const currentMessage = messages.value[assistantMessageIndex]
      if (currentMessage) {
        currentMessage.content += errorText
      }
      statusText.value = errorText
      statusType.value = 'warn'
      antMessage.error(errorText)
    }
  } finally {
    const currentMessage = messages.value[assistantMessageIndex]
    if (currentMessage) {
      currentMessage.pending = false
    }
    streaming.value = false
    abortController = null
    currentStreamId = ''
    await scrollToBottom()
  }
}

async function stopStream() {
  const productId = selectedProductId.value
  statusText.value = '正在停止生成...'
  if (abortController) {
    abortController.abort()
  }
  if (!productId || !currentStreamId) {
    return
  }
  try {
    await stopAiControlStream(productId, currentStreamId)
  } catch (error) {
    statusText.value = error.message || '停止失败'
    statusType.value = 'warn'
  }
}

function createStreamId() {
  if (window.crypto?.randomUUID) {
    return window.crypto.randomUUID()
  }
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function clearMessages() {
  messages.value = []
}

function selectFile(file) {
  addSelectedFiles([file], 'select')
}

function handlePaste(event) {
  if (streaming.value) {
    return
  }
  const pastedFiles = getClipboardFiles(event)
  if (!pastedFiles.length) {
    return
  }
  const addedCount = addSelectedFiles(pastedFiles, 'paste')
  if (addedCount > 0) {
    event.preventDefault()
  }
}

function addSelectedFiles(files, source = 'select') {
  const incomingFiles = Array.from(files || []).filter(Boolean)
  if (!incomingFiles.length) {
    return 0
  }
  const existingFileIds = new Set(selectedFiles.value.map(getFileIdentity))
  const acceptedFiles = []
  let unsupportedCount = 0
  let duplicateCount = 0

  incomingFiles.forEach((file) => {
    if (!isAcceptedFile(file)) {
      unsupportedCount += 1
      return
    }
    const fileId = getFileIdentity(file)
    if (existingFileIds.has(fileId)) {
      duplicateCount += 1
      return
    }
    existingFileIds.add(fileId)
    acceptedFiles.push(file)
  })

  if (acceptedFiles.length) {
    selectedFiles.value = [...selectedFiles.value, ...acceptedFiles]
    if (source === 'paste') {
      antMessage.success(`已添加 ${acceptedFiles.length} 个粘贴文件`)
    }
  }
  if (unsupportedCount) {
    antMessage.warning(`仅支持 ${acceptedFileLabel} 文档`)
  } else if (duplicateCount && !acceptedFiles.length) {
    antMessage.info('文件已在待发送列表中')
  }
  return acceptedFiles.length
}

function removeSelectedFile(file) {
  selectedFiles.value = selectedFiles.value.filter((item) => item !== file)
}

function parseMessageContent(content) {
  const marker = '\n\n上传文件：'
  const markerIndex = content.lastIndexOf(marker)
  if (markerIndex === -1) {
    return {
      content,
      fileNames: [],
    }
  }
  const fileText = content.slice(markerIndex + marker.length).trim()
  const fileNames = fileText
    .split(fileText.includes('\n') ? /\r?\n/ : '、')
    .map((name) => name.trim())
    .filter(Boolean)
  return {
    content: content.slice(0, markerIndex),
    fileNames,
  }
}

function queueScrollToBottom() {
  if (scrollFrameId) {
    return
  }
  scrollFrameId = window.requestAnimationFrame(async () => {
    scrollFrameId = 0
    await scrollToBottom()
  })
}

async function scrollToBottom() {
  await nextTick()
  const messagePane = getMessagePane()
  if (messagePane) {
    messagePane.scrollTop = messagePane.scrollHeight
  }
}

async function keepScrollPositionAfterPrepend(previousScrollHeight) {
  await nextTick()
  const messagePane = getMessagePane()
  if (messagePane) {
    messagePane.scrollTop = messagePane.scrollHeight - previousScrollHeight
  }
}

function getMessagePane() {
  return messageListRef.value?.getPaneElement?.()
}
</script>

<style lang="scss" scoped>
.ai-chat-page {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  height: calc(100vh - 64px);
  min-height: 0;
  overflow: hidden;
  background: #f4f6f7;
}

.chat-main {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  min-height: 0;
  min-width: 0;
  overflow: hidden;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #dde3e1;
  background: #fff;
  padding: 16px 22px;
}

.active-product {
  min-width: 0;

  strong {
    display: block;
    overflow: hidden;
    color: #15201d;
    font-size: 16px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  span {
    display: block;
    margin-top: 4px;
    color: #66736f;
    font-size: 12px;
  }
}

@media (max-width: 840px) {
  .ai-chat-page {
    grid-template-columns: 1fr;
  }
}
</style>
