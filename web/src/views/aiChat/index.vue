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
          <div class="bubble">
            <div v-if="item.content" class="message-content">
              <template
                v-for="(segment, segmentIndex) in splitMarkdownContent(item.content)"
                :key="`${item.id}-segment-${segmentIndex}`"
              >
                <div v-if="segment.type === 'text'" class="message-text">{{ segment.content }}</div>
                <div v-else class="code-block">
                  <div class="code-header">
                    <span>{{ segment.language || 'text' }}</span>
                    <a-button
                      type="text"
                      size="small"
                      html-type="button"
                      @click="copyCode(segment.code)"
                    >
                      <template #icon><CopyOutlined /></template>
                      复制
                    </a-button>
                  </div>
                  <pre><code v-html="highlightCode(segment.code, segment.language)" /></pre>
                </div>
              </template>
            </div>
            <div v-if="item.fileNames?.length" class="message-files">
              <div v-for="(fileName, fileIndex) in item.fileNames" :key="`${fileName}-${fileIndex}`" class="message-file">
                <FileTextOutlined />
                <span>{{ fileName }}</span>
              </div>
            </div>
          </div>
          <div v-if="item.role === 'assistant' && item.content" class="message-actions">
            <a-tooltip title="复制">
              <a-button
                type="text"
                shape="circle"
                size="small"
                html-type="button"
                :disabled="item.pending"
                @click="copyMessage(item.content)"
              >
                <template #icon><CopyOutlined /></template>
              </a-button>
            </a-tooltip>
          </div>
        </article>
      </section>

      <form class="composer" @submit.prevent="sendMessage">
        <div v-if="selectedFiles.length" class="selected-file-preview">
          <div v-for="file in selectedFiles" :key="file.uid || file.name" class="selected-file-chip">
            <FileTextOutlined />
            <span>{{ file.name }}</span>
            <a-button
              type="text"
              shape="circle"
              size="small"
              html-type="button"
              :disabled="streaming"
              @click="removeSelectedFile(file)"
            >
              <template #icon><CloseOutlined /></template>
            </a-button>
          </div>
        </div>
        <div class="input-shell">
          <a-upload
            :before-upload="beforeUploadFile"
            :show-upload-list="false"
            multiple
            accept=".pdf,.txt,.md,.markdown,.doc,.docx,.ppt,.pptx,.xls,.xlsx"
          >
            <a-tooltip title="上传文件">
              <a-button class="attach-button" type="text" shape="circle" html-type="button" :disabled="streaming">
                <template #icon><PaperClipOutlined /></template>
              </a-button>
            </a-tooltip>
          </a-upload>
          <a-textarea
            v-model:value="inputText"
            :auto-size="{ minRows: 1, maxRows: 5 }"
            :disabled="streaming"
            placeholder="输入消息"
            @keydown.enter.exact.prevent="sendMessage"
          />
        </div>
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
const selectedFiles = ref([])
const loadingProducts = ref(false)
const loadingHistory = ref(false)
const streaming = ref(false)
const statusText = ref('正在加载产品...')
const statusType = ref('')
const messagePaneRef = ref(null)
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
    content: text,
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
      content: text,
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

function beforeUploadFile(file) {
  selectedFiles.value = [...selectedFiles.value, file]
  return false
}

function removeSelectedFile(file) {
  selectedFiles.value = selectedFiles.value.filter((item) => item !== file)
}

function splitMarkdownContent(content) {
  const segments = []
  const fencePattern = /```([^\n`]*)\n?([\s\S]*?)(?:```|$)/g
  let lastIndex = 0
  let match
  while ((match = fencePattern.exec(content)) !== null) {
    if (match.index > lastIndex) {
      segments.push({
        type: 'text',
        content: content.slice(lastIndex, match.index),
      })
    }
    segments.push({
      type: 'code',
      language: normalizeCodeLanguage(match[1]),
      code: match[2].replace(/\n$/, ''),
    })
    lastIndex = fencePattern.lastIndex
  }
  if (lastIndex < content.length) {
    segments.push({
      type: 'text',
      content: content.slice(lastIndex),
    })
  }
  return segments.length ? segments : [{ type: 'text', content }]
}

function normalizeCodeLanguage(language) {
  return String(language || '').trim().split(/\s+/)[0].toLowerCase()
}

function highlightCode(code, language) {
  const escaped = escapeHtml(code)
  const normalized = normalizeCodeLanguage(language)
  if (['json'].includes(normalized)) {
    return highlightJson(escaped)
  }
  if (['html', 'xml', 'vue'].includes(normalized)) {
    return highlightMarkup(escaped)
  }
  if (['yaml', 'yml'].includes(normalized)) {
    return highlightYaml(escaped)
  }
  if (['bash', 'shell', 'sh', 'powershell', 'ps1'].includes(normalized)) {
    return highlightShell(escaped)
  }
  return highlightCommonCode(escaped)
}

function highlightJson(code) {
  return code.replace(
    /(&quot;(?:\\.|[^&])*?&quot;)(\s*:)?|\b(true|false|null)\b|-?\b\d+(?:\.\d+)?\b/g,
    (match, stringValue, colon, keyword) => {
      if (stringValue) {
        return `<span class="${colon ? 'code-token key' : 'code-token string'}">${stringValue}</span>${colon || ''}`
      }
      if (keyword) {
        return `<span class="code-token keyword">${match}</span>`
      }
      return `<span class="code-token number">${match}</span>`
    }
  )
}

function highlightMarkup(code) {
  return code.replace(/(&lt;[\s\S]*?&gt;)/g, '<span class="code-token keyword">$1</span>')
}

function highlightYaml(code) {
  return code
    .split('\n')
    .map((line) => {
      if (/^\s*#/.test(line)) {
        return `<span class="code-token comment">${line}</span>`
      }
      return line.replace(/^(\s*[\w.-]+)(:)(.*)$/, (match, key, colon, value) => {
        const valueHtml = value
          ? `<span class="code-token string">${value}</span>`
          : ''
        return `<span class="code-token key">${key}</span>${colon}${valueHtml}`
      })
    })
    .join('\n')
}

function highlightShell(code) {
  return code
    .replace(/^(\s*#.*)$/gm, '<span class="code-token comment">$1</span>')
    .replace(/\b(cd|ls|dir|npm|mvn|git|docker|kubectl|curl|echo|set|Get-Content|Select-Object|rg)\b/g, '<span class="code-token keyword">$1</span>')
    .replace(/(--?[A-Za-z0-9-]+)/g, '<span class="code-token key">$1</span>')
}

function highlightCommonCode(code) {
  const tokenPattern = /(\/\/.*$|#.*$)|(&quot;.*?&quot;|&#39;.*?&#39;|`.*?`)|\b(class|public|private|protected|static|final|void|return|new|if|else|for|while|switch|case|try|catch|finally|import|package|const|let|var|function|async|await|true|false|null|undefined)\b|\b(\d+(?:\.\d+)?)\b/gm
  return code.replace(tokenPattern, (match, comment, stringValue, keyword, number) => {
    if (comment) {
      return `<span class="code-token comment">${comment}</span>`
    }
    if (stringValue) {
      return `<span class="code-token string">${stringValue}</span>`
    }
    if (keyword) {
      return `<span class="code-token keyword">${keyword}</span>`
    }
    if (number) {
      return `<span class="code-token number">${number}</span>`
    }
    return match
  })
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

async function copyCode(code) {
  try {
    await copyText(code)
    antMessage.success('已复制代码')
  } catch (error) {
    antMessage.error('复制失败')
  }
}

async function copyMessage(content) {
  try {
    await copyText(content)
    antMessage.success('已复制回复')
  } catch (error) {
    antMessage.error('复制失败')
  }
}

async function copyText(text) {
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(text)
    return
  }
  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.style.position = 'fixed'
  textarea.style.opacity = '0'
  document.body.appendChild(textarea)
  textarea.select()
  document.execCommand('copy')
  document.body.removeChild(textarea)
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

.message-row.assistant:hover .message-actions,
.message-row.assistant:focus-within .message-actions {
  opacity: 1;
}

.speaker {
  color: #66736f;
  font-size: 12px;
  font-weight: 700;
}

.bubble {
  display: grid;
  gap: 10px;
  border: 1px solid #dde3e1;
  border-radius: 8px;
  background: #fff;
  color: #15201d;
  padding: 12px 14px;
  line-height: 1.7;
  overflow-wrap: anywhere;
  box-shadow: 0 8px 22px rgba(21, 32, 29, 0.07);
}

.message-content {
  display: grid;
  gap: 10px;
  white-space: pre-wrap;
}

.message-text {
  white-space: pre-wrap;
}

.code-block {
  overflow: hidden;
  border: 1px solid #26352f;
  border-radius: 8px;
  background: #101915;
  color: #dbe7e1;
  white-space: normal;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);

  pre {
    margin: 0;
    max-width: 100%;
    overflow: auto;
    padding: 13px 14px 15px;
    font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', monospace;
    font-size: 13px;
    line-height: 1.65;
    tab-size: 2;
    white-space: pre;
  }
}

.code-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  background: #17231e;
  color: #aebdb6;
  padding: 6px 8px 6px 12px;
  font-size: 12px;
  line-height: 1.2;

  span {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  :deep(.ant-btn) {
    color: #c9d6d0;
    font-size: 12px;
  }

  :deep(.ant-btn:hover) {
    background: rgba(255, 255, 255, 0.08);
    color: #fff;
  }
}

:deep(.code-token.comment) {
  color: #7e9289;
}

:deep(.code-token.keyword) {
  color: #7cc7ff;
}

:deep(.code-token.string) {
  color: #9bd88f;
}

:deep(.code-token.number) {
  color: #f2c16b;
}

:deep(.code-token.key) {
  color: #f0d78c;
}

:deep(.code-token.punctuation) {
  color: #8fb5a6;
}

.message-files {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.message-file {
  display: inline-flex;
  align-items: center;
  justify-self: start;
  max-width: min(320px, 100%);
  gap: 8px;
  border: 1px solid #d8e2df;
  border-radius: 8px;
  background: rgba(247, 250, 249, 0.9);
  color: #34423e;
  padding: 7px 10px;
  font-size: 13px;
  line-height: 1.3;

  span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.message-actions {
  display: flex;
  align-items: center;
  min-height: 28px;
  opacity: 0;
  transition: opacity 0.16s ease;

  :deep(.ant-btn) {
    color: #687672;
  }

  :deep(.ant-btn:hover) {
    background: #edf3f1;
    color: #15201d;
  }
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
  gap: 10px 12px;
  align-items: end;
  border-top: 1px solid #dde3e1;
  background: #fff;
  padding: 16px 22px;
}

.selected-file-preview {
  display: flex;
  flex-wrap: wrap;
  grid-column: 1 / -1;
  align-items: center;
  justify-self: start;
  max-width: 100%;
  gap: 8px;
  min-width: 0;
}

.selected-file-chip {
  display: inline-flex;
  align-items: center;
  max-width: min(420px, 100%);
  min-width: 0;
  gap: 8px;
  border: 1px solid #d8e2df;
  border-radius: 8px;
  background: #f7faf9;
  color: #34423e;
  padding: 7px 8px 7px 10px;
  font-size: 13px;
  line-height: 1.3;

  span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.input-shell {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 6px;
  border: 1px solid #cfd8d5;
  border-radius: 8px;
  background: #fff;
  padding: 6px 8px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;

  &:focus-within {
    border-color: #1f7a5c;
    box-shadow: 0 0 0 2px rgba(31, 122, 92, 0.12);
  }

  :deep(.ant-upload) {
    display: flex;
  }

  :deep(textarea) {
    border: 0;
    box-shadow: none;
    line-height: 1.6;
    padding: 4px 2px;

    &:focus {
      border: 0;
      box-shadow: none;
    }
  }
}

.attach-button {
  color: #66736f;

  &:hover {
    color: #1f7a5c;
    background: #eef5f2;
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
