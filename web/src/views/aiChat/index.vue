<template>
  <div class="ai-chat-page">
    <aside class="chat-side">
      <div class="side-title">
        <div>
          <h2>智能体对话调试</h2>
          <span>{{ activeChatId || '未选择产品' }}</span>
        </div>
        <a-button type="text" :disabled="loadingProducts || streaming" @click="loadProducts">
          <template #icon><ReloadOutlined /></template>
        </a-button>
      </div>

      <div class="field">
        <label>产品</label>
        <a-select
          v-model:value="selectedProductId"
          :options="productOptions"
          show-search
          :filter-option="filterProductOption"
          :loading="loadingProducts"
          :disabled="streaming"
          placeholder="选择产品"
          @change="handleProductChange"
        />
      </div>

      <div class="side-actions">
        <a-button block :disabled="!selectedProductId || streaming || loadingHistory" @click="loadHistory(1)">
          <template #icon><HistoryOutlined /></template>
          历史
        </a-button>
      </div>

      <div class="history-pager">
        <a-button
          block
          size="small"
          :disabled="!selectedProductId || streaming || loadingHistory || !hasMoreHistory"
          @click="loadMoreHistory"
        >
          加载更早历史
        </a-button>
        <span v-if="historyPagination.total > 0">
          已显示 {{ loadedHistoryCount }} / {{ historyPagination.total }}
        </span>
      </div>

      <div class="side-meta" :class="{ warn: statusType === 'warn' }">
        {{ statusText }}
      </div>
    </aside>

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

      <section ref="messagePaneRef" class="message-pane">
        <div v-if="messages.length === 0" class="empty-state">
          选择产品后开始对话。
        </div>
        <article
          v-for="item in messages"
          :key="item.id"
          class="message-row"
          :class="[item.role, { pending: item.pending }]"
        >
          <div class="speaker">{{ item.role === 'user' ? '你' : '助手' }}</div>
          <div class="bubble">{{ item.content }}</div>
        </article>
      </section>

      <form class="composer" @submit.prevent="sendMessage">
        <a-textarea
          v-model:value="inputText"
          :auto-size="{ minRows: 1, maxRows: 5 }"
          :disabled="streaming"
          placeholder="输入消息"
          @keydown.enter.exact.prevent="sendMessage"
        />
        <a-button
          v-if="!streaming"
          type="primary"
          html-type="submit"
          :disabled="!canSend"
        >
          <template #icon><SendOutlined /></template>
          发送
        </a-button>
        <a-button v-else danger @click="stopStream">
          <template #icon><StopOutlined /></template>
          停止
        </a-button>
      </form>
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { message as antMessage } from 'ant-design-vue'
import { getProduct } from '@/api/product'
import { getHistoryMessage } from '@/api/agentMemory'
import { stopAiControlStream, streamAiControl } from '@/api/aiChat'

const products = ref([])
const selectedProductId = ref()
const messages = ref([])
const inputText = ref('')
const loadingProducts = ref(false)
const loadingHistory = ref(false)
const streaming = ref(false)
const statusText = ref('正在加载产品...')
const statusType = ref('')
const messagePaneRef = ref(null)
let abortController = null
let scrollFrameId = 0
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
  Boolean(selectedProductId.value && inputText.value.trim() && !streaming.value)
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
  const previousScrollHeight = messagePaneRef.value?.scrollHeight || 0
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
    const historyMessages = records.map((item) => ({
      id: item.id || `${item.requestId}-${item.sequenceNum}`,
      role: item.messageType === 'user' ? 'user' : 'assistant',
      content: item.content || '',
      pending: false,
    }))
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
  inputText.value = ''
  const assistantMessage = {
    id: `assistant-${Date.now()}`,
    role: 'assistant',
    content: '',
    pending: true,
  }
  messages.value.push({
    id: `user-${Date.now()}`,
    role: 'user',
    content: text,
    pending: false,
  })
  const assistantMessageIndex = messages.value.push(assistantMessage) - 1
  await scrollToBottom()

  streaming.value = true
  abortController = new AbortController()
  statusText.value = '正在生成回复...'
  statusType.value = ''
  try {
    await streamAiControl({
      productId: selectedProductId.value,
      content: text,
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
    await scrollToBottom()
  }
}

async function stopStream() {
  const productId = selectedProductId.value
  statusText.value = '正在停止生成...'
  if (abortController) {
    abortController.abort()
  }
  if (!productId) {
    return
  }
  try {
    await stopAiControlStream(productId)
  } catch (error) {
    statusText.value = error.message || '停止失败'
    statusType.value = 'warn'
  }
}

function clearMessages() {
  messages.value = []
}

function filterProductOption(input, option) {
  const keyword = String(input || '').trim().toLowerCase()
  if (!keyword) {
    return true
  }
  return String(option?.label || '').toLowerCase().includes(keyword)
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
  if (messagePaneRef.value) {
    messagePaneRef.value.scrollTop = messagePaneRef.value.scrollHeight
  }
}

async function keepScrollPositionAfterPrepend(previousScrollHeight) {
  await nextTick()
  if (messagePaneRef.value) {
    messagePaneRef.value.scrollTop = messagePaneRef.value.scrollHeight - previousScrollHeight
  }
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

.chat-side {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-height: 0;
  border-right: 1px solid #dde3e1;
  background: #fff;
  overflow: auto;
  padding: 20px;
}

.side-title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;

  h2 {
    margin: 0;
    color: #15201d;
    font-size: 18px;
    font-weight: 700;
  }

  span {
    display: block;
    margin-top: 6px;
    color: #66736f;
    font-size: 12px;
    overflow-wrap: anywhere;
  }
}

.field {
  display: grid;
  gap: 8px;

  label {
    color: #66736f;
    font-size: 13px;
    font-weight: 600;
  }
}

.side-actions {
  display: grid;
  gap: 8px;
}

.history-pager {
  display: grid;
  gap: 6px;
  min-height: 24px;

  span {
    color: #66736f;
    font-size: 12px;
    text-align: center;
  }
}

.side-meta {
  margin-top: auto;
  min-height: 36px;
  border: 1px solid #dde3e1;
  border-radius: 8px;
  background: #f7faf9;
  color: #66736f;
  padding: 9px 10px;
  font-size: 12px;
  line-height: 1.5;
  overflow-wrap: anywhere;

  &.warn {
    border-color: #f2c36b;
    background: #fff8e8;
    color: #8a5d00;
  }
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

.message-pane {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 0;
  overflow: auto;
  overscroll-behavior: contain;
  padding: 22px;
}

.empty-state {
  margin: 18vh auto 0;
  color: #66736f;
  font-size: 14px;
}

.message-row {
  display: grid;
  gap: 6px;
  max-width: min(760px, 82%);

  &.user {
    align-self: flex-end;
  }

  &.assistant {
    align-self: flex-start;
  }
}

.speaker {
  color: #66736f;
  font-size: 12px;
  font-weight: 700;
}

.bubble {
  border: 1px solid #dde3e1;
  border-radius: 8px;
  background: #fff;
  color: #15201d;
  padding: 12px 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  box-shadow: 0 8px 22px rgba(21, 32, 29, 0.07);
}

.user .bubble {
  border-color: rgba(31, 122, 92, 0.26);
  background: #e8f4ef;
  box-shadow: none;
}

.pending .bubble::after {
  content: '';
  display: inline-block;
  width: 7px;
  height: 16px;
  margin-left: 3px;
  border-radius: 4px;
  background: #1f7a5c;
  vertical-align: -3px;
  animation: blink 1s steps(2, start) infinite;
}

@keyframes blink {
  50% {
    opacity: 0;
  }
}

.composer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  align-items: end;
  border-top: 1px solid #dde3e1;
  background: #fff;
  padding: 16px 22px;

  :deep(textarea) {
    border-radius: 8px;
    line-height: 1.6;
  }
}

@media (max-width: 840px) {
  .ai-chat-page {
    grid-template-columns: 1fr;
  }

  .chat-side {
    border-right: 0;
    border-bottom: 1px solid #dde3e1;
  }

  .message-row {
    max-width: 94%;
  }

  .composer {
    grid-template-columns: 1fr;
  }
}
</style>
