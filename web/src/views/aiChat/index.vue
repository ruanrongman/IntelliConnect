<template>
  <div class="ai-chat-page">
    <ChatSidebar
      v-model="selectedProductId"
      :product-options="productOptions"
      :conversations="conversations"
      :active-chat-id="activeChatId"
      :loading-products="loadingProducts"
      :loading-conversations="loadingConversations"
      :streaming="streaming"
      :streaming-chat-ids="streamingChatIds"
      :loading-history="loadingHistory"
      :has-more-history="hasMoreHistory"
      :history-total="historyPagination.total"
      :loaded-history-count="loadedHistoryCount"
      :status-text="statusText"
      :status-type="statusType"
      @refresh-products="refreshProducts"
      @load-more-history="loadMoreHistory"
      @product-change="handleProductChange"
      @new-conversation="createNewConversation"
      @select-conversation="selectConversation"
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
import { getAgentMemoryByNickName, getHistoryMessage } from '@/api/agentMemory'
import { stopAiControlStream, streamAiControl } from '@/api/aiChat'
import { queryKnowledgeGraphicReference } from '@/api/knowledgeGraphic'
import { postKnowledgeChatRecall } from '@/api/productKnowledge'
import ChatComposer from './ChatComposer.vue'
import ChatMessageList from './ChatMessageList.vue'
import ChatSidebar from './ChatSidebar.vue'
import { acceptedFileExtensions, acceptedFileLabel, getClipboardFiles, getFileIdentity, isAcceptedFile } from './fileUpload'

const products = ref([])
const selectedProductId = ref()
const conversations = ref([])
const activeChatId = ref('')
const messages = ref([])
const inputText = ref('')
const selectedFiles = ref([])
const loadingProducts = ref(false)
const loadingHistory = ref(false)
const loadingConversations = ref(false)
const conversationViews = new Map()
const streamContexts = reactive(new Map())
const streaming = computed(() => streamContexts.has(activeChatId.value))
const streamingChatIds = computed(() => Array.from(streamContexts.keys()))
const statusText = ref('正在加载产品...')
const statusType = ref('')
const messageListRef = ref(null)
let scrollFrameId = 0
let historyRequestSequence = 0
const historyPagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
})
const loadedHistoryCount = ref(0)
const KNOWLEDGE_RECALL_QUERY_MAX_LENGTH = 2048

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


const canSend = computed(() =>
  Boolean(activeChatId.value && (inputText.value.trim() || selectedFiles.value.length) && !streaming.value)
)

const hasMoreHistory = computed(() =>
  historyPagination.total > 0 && loadedHistoryCount.value < historyPagination.total
)

onMounted(() => {
  loadProducts(true)
})

onBeforeUnmount(() => {
  streamContexts.forEach((context) => context.abortController.abort())
  streamContexts.clear()
  if (scrollFrameId) {
    window.cancelAnimationFrame(scrollFrameId)
  }
})

async function loadProducts(resetConversation = false) {
  loadingProducts.value = true
  statusText.value = '正在加载产品...'
  statusType.value = ''
  try {
    const previousProductId = selectedProductId.value
    const res = await getProduct()
    const payload = res.data || {}
    if (payload.errorCode !== 200) {
      throw new Error(payload.errorMsg || '产品加载失败')
    }
    products.value = Array.isArray(payload.data) ? payload.data : []
    const selectedProductExists = products.value.some((item) => item.id === selectedProductId.value)
    if (!selectedProductExists) {
      selectedProductId.value = products.value[0]?.id
    }
    if (!selectedProductId.value) {
      resetConversationState()
      conversations.value = []
      statusText.value = '暂无可用产品。'
      return
    }
    const productChanged = previousProductId !== selectedProductId.value
    if (resetConversation || productChanged || !activeChatId.value) {
      activeChatId.value = buildDefaultChatId(selectedProductId.value)
    }
    await loadConversations(selectedProductId.value, true)
    await loadHistory(1)
  } catch (error) {
    products.value = []
    selectedProductId.value = undefined
    conversations.value = []
    activeChatId.value = ''
    resetConversationState()
    statusText.value = error.message || '产品加载失败'
    statusType.value = 'warn'
  } finally {
    loadingProducts.value = false
  }
}
async function refreshProducts() {
  await loadProducts(false)
}

async function loadConversations(productId, preserveActive = true) {
  if (!productId) {
    conversations.value = []
    return
  }
  loadingConversations.value = true
  const defaultChatId = buildDefaultChatId(productId)
  const conversationById = new Map([
    [defaultChatId, createDefaultConversation(productId)],
  ])
  try {
    const res = await getAgentMemoryByNickName({ productId })
    const payload = res.data || {}
    const records = payload.errorCode === 200 && Array.isArray(payload.data)
      ? payload.data.slice().sort((a, b) => Number(b.id || 0) - Number(a.id || 0))
      : []
    records.forEach((record) => {
      const chatId = String(record?.chatId || '').trim()
      if (!chatId) {
        return
      }
      const content = String(record?.content || '').trim()
      const deviceName = record?.deviceName && record.deviceName !== '未知设备'
        ? String(record.deviceName)
        : ''
      conversationById.set(chatId, {
        id: record.id,
        chatId,
        title: content ? getFirstSentence(content) : chatId === defaultChatId ? '默认对话' : '新对话',
        fullContent: content,
        deviceName,
        nickName: deviceName ? String(record?.nickName || '').trim() : '',
        isDefault: chatId === defaultChatId,
        isDraft: false,
      })
    })
  } catch (error) {
    console.warn('Conversation list failed:', error)
  }
  if (String(selectedProductId.value) !== String(productId)) {
    loadingConversations.value = false
    return
  }
  conversations.value
    .filter((item) => item.chatId.startsWith(`${defaultChatId}debug`)
      && (item.isDraft || streamContexts.has(item.chatId) || conversationViews.has(item.chatId)))
    .forEach((item) => {
      if (!conversationById.has(item.chatId)) {
        conversationById.set(item.chatId, item)
      }
    })
  const defaultConversation = conversationById.get(defaultChatId)
  conversations.value = [
    defaultConversation,
    ...Array.from(conversationById.values()).filter((item) => item.chatId !== defaultChatId),
  ]
  if (!preserveActive || !conversationById.has(activeChatId.value)) {
    activeChatId.value = defaultChatId
  }
  loadingConversations.value = false
}

function createDefaultConversation(productId) {
  const chatId = buildDefaultChatId(productId)
  return {
    id: 0,
    chatId,
    title: '默认对话',
    fullContent: '',
    deviceName: '',
    nickName: '',
    isDefault: true,
    isDraft: false,
  }
}

function getFirstSentence(content) {
  const text = String(content || '').trim()
  if (!text) {
    return ''
  }
  const match = text.match(/^[\s\S]*?(?:\r?\n|[。！？.!?]|$)/)
  return String(match?.[0] || text).trim()
}

function buildDefaultChatId(productId) {
  return `chatProduct${productId}`
}

async function loadHistory(page = 1, mode = 'replace') {
  if (!selectedProductId.value || !activeChatId.value) {
    return
  }
  const requestSequence = ++historyRequestSequence
  const requestChatId = activeChatId.value
  const previousScrollHeight = getMessagePane()?.scrollHeight || 0
  historyPagination.current = page
  loadingHistory.value = true
  statusText.value = '正在加载历史...'
  statusType.value = ''
  try {
    const res = await getHistoryMessage({
      pageNum: historyPagination.current,
      pageSize: historyPagination.pageSize,
      chatId: requestChatId,
    })
    if (!isCurrentHistoryRequest(requestSequence, requestChatId)) {
      return
    }
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
      saveConversationView(requestChatId, true)
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
        knowledgeRecall: [],
        knowledgeGraphicReference: null,
        loadingReferences: false,
        pending: false,
      }
    })
    if (mode === 'prepend') {
      messages.value = [...historyMessages, ...messages.value]
      loadedHistoryCount.value += historyMessages.length
      statusText.value = `已加载到第 ${historyPagination.current} 页，共 ${historyPagination.total} 条。`
      saveConversationView(requestChatId, true)
      await keepScrollPositionAfterPrepend(previousScrollHeight)
    } else {
      messages.value = historyMessages
      loadedHistoryCount.value = historyMessages.length
      statusText.value = `已显示最新 ${loadedHistoryCount.value} / ${historyPagination.total} 条。`
      saveConversationView(requestChatId, true)
      await scrollToBottom(true)
    }
  } catch (error) {
    if (!isCurrentHistoryRequest(requestSequence, requestChatId)) {
      return
    }
    if (mode === 'replace') {
      messages.value = []
      historyPagination.total = 0
      loadedHistoryCount.value = 0
    }
    statusText.value = error.message || '历史加载失败'
    statusType.value = 'warn'
    saveConversationView(requestChatId, true)
  } finally {
    if (isCurrentHistoryRequest(requestSequence, requestChatId)) {
      loadingHistory.value = false
    }
  }
}

function isCurrentHistoryRequest(requestSequence, chatId) {
  return requestSequence === historyRequestSequence && chatId === activeChatId.value
}

function invalidateHistoryRequest() {
  historyRequestSequence += 1
  loadingHistory.value = false
}

function saveConversationView(chatId, initialized = false) {
  if (!chatId) {
    return
  }
  const existingView = conversationViews.get(chatId)
  if (!existingView && !initialized && messages.value.length === 0 && !streamContexts.has(chatId)) {
    return
  }
  conversationViews.set(chatId, {
    messages: messages.value,
    currentPage: historyPagination.current,
    pageSize: historyPagination.pageSize,
    total: historyPagination.total,
    loadedCount: loadedHistoryCount.value,
    initialized: initialized || Boolean(existingView?.initialized),
    statusText: statusText.value,
    statusType: statusType.value,
  })
}

function restoreConversationView(chatId) {
  const view = conversationViews.get(chatId)
  if (!view?.initialized) {
    return false
  }
  messages.value = view.messages
  historyPagination.current = view.currentPage
  historyPagination.pageSize = view.pageSize
  historyPagination.total = view.total
  loadedHistoryCount.value = view.loadedCount
  statusText.value = view.statusText
  statusType.value = view.statusType
  return true
}

function updateConversationStatus(chatId, text, type = '') {
  const view = conversationViews.get(chatId)
  if (view) {
    view.statusText = text
    view.statusType = type
  }
  if (chatId === activeChatId.value) {
    statusText.value = text
    statusType.value = type
  }
}

function scrollActiveConversation(chatId) {
  if (chatId === activeChatId.value) {
    queueScrollToBottom()
  }
}

async function loadMoreHistory() {
  if (!hasMoreHistory.value) {
    return
  }
  await loadHistory(historyPagination.current + 1, 'prepend')
}

async function handleProductChange(productId) {
  saveConversationView(activeChatId.value)
  selectedProductId.value = productId
  const nextChatId = buildDefaultChatId(productId)
  activeChatId.value = nextChatId
  invalidateHistoryRequest()
  inputText.value = ''
  selectedFiles.value = []
  const restored = restoreConversationView(nextChatId)
  if (!restored) {
    resetConversationState()
    statusText.value = '正在加载对话...'
    statusType.value = ''
  }
  await loadConversations(productId, false)
  if (restored) {
    await scrollToBottom(true)
  } else {
    await loadHistory(1)
  }
}

async function selectConversation(chatId) {
  if (!chatId || chatId === activeChatId.value) {
    return
  }
  saveConversationView(activeChatId.value)
  activeChatId.value = chatId
  invalidateHistoryRequest()
  inputText.value = ''
  selectedFiles.value = []
  if (restoreConversationView(chatId)) {
    await scrollToBottom(true)
    return
  }
  resetConversationState()
  await loadHistory(1)
}

function createNewConversation() {
  if (!selectedProductId.value) {
    return
  }
  saveConversationView(activeChatId.value)
  invalidateHistoryRequest()
  const existingDraft = conversations.value.find((item) => item.isDraft)
  let chatId = existingDraft?.chatId
  if (!chatId) {
    chatId = `${buildDefaultChatId(selectedProductId.value)}debug${createUuid()}`
    const draft = {
      id: 0,
      chatId,
      title: '新对话',
      fullContent: '',
      deviceName: '',
      nickName: '',
      isDefault: false,
      isDraft: true,
    }
    const [defaultConversation, ...otherConversations] = conversations.value
    conversations.value = [defaultConversation, draft, ...otherConversations].filter(Boolean)
  }
  activeChatId.value = chatId
  inputText.value = ''
  selectedFiles.value = []
  if (restoreConversationView(chatId)) {
    scrollActiveConversation(chatId)
    return
  }
  resetConversationState()
  statusText.value = '新对话已创建。'
  statusType.value = ''
  saveConversationView(chatId, true)
}

function resetConversationState() {
  messages.value = []
  historyPagination.current = 1
  historyPagination.total = 0
  loadedHistoryCount.value = 0
}

function createUuid() {
  const cryptoApi = window.crypto
  if (cryptoApi?.randomUUID) {
    return cryptoApi.randomUUID()
  }
  const bytes = new Uint8Array(16)
  if (cryptoApi?.getRandomValues) {
    cryptoApi.getRandomValues(bytes)
  } else {
    bytes.forEach((value, index) => {
      bytes[index] = Math.floor(Math.random() * 256)
    })
  }
  bytes[6] = (bytes[6] & 0x0f) | 0x40
  bytes[8] = (bytes[8] & 0x3f) | 0x80
  const hex = Array.from(bytes, (value) => value.toString(16).padStart(2, '0'))
  return `${hex.slice(0, 4).join('')}-${hex.slice(4, 6).join('')}-${hex.slice(6, 8).join('')}-${hex.slice(8, 10).join('')}-${hex.slice(10).join('')}`
}

async function sendMessage() {
  if (!canSend.value) {
    return
  }
  const productId = selectedProductId.value
  const conversationChatId = activeChatId.value
  const conversationMessages = messages.value
  const text = inputText.value.trim()
  const requestText = text || '请阅读上传文件。'
  const files = [...selectedFiles.value]
  inputText.value = ''
  selectedFiles.value = []
  const assistantMessage = reactive({
    id: `assistant-${Date.now()}`,
    role: 'assistant',
    content: '',
    knowledgeRecall: [],
    knowledgeGraphicReference: null,
    loadingReferences: false,
    pending: true,
  })
  conversationMessages.push({
    id: `user-${Date.now()}`,
    role: 'user',
    content: requestText,
    fileNames: files.map((file) => file.name),
    pending: false,
  })
  conversationMessages.push(assistantMessage)
  const conversation = conversations.value.find((item) => item.chatId === conversationChatId)
  if (conversation) {
    conversation.isDraft = false
  }
  const streamContext = {
    productId,
    chatId: conversationChatId,
    streamId: createStreamId(),
    abortController: new AbortController(),
  }
  streamContexts.set(conversationChatId, streamContext)
  saveConversationView(conversationChatId, true)
  updateConversationStatus(conversationChatId, '正在生成回复...')
  await scrollToBottom()
  try {
    await streamAiControl({
      productId,
      chatId: conversationChatId,
      streamId: streamContext.streamId,
      content: requestText,
      files,
      signal: streamContext.abortController.signal,
      onMessage: (chunk) => {
        assistantMessage.content += chunk
        scrollActiveConversation(conversationChatId)
      },
      onComplete: (finalContent) => {
        assistantMessage.content = finalContent
        scrollActiveConversation(conversationChatId)
      },
      onError: (errorText) => {
        assistantMessage.content += errorText
        updateConversationStatus(conversationChatId, errorText, 'warn')
      },
    })
    if (!assistantMessage.content.trim()) {
      assistantMessage.content = '请求已结束。'
    }
    updateConversationStatus(conversationChatId, '回复完成，正在查询参考记录...')
    await appendReferenceRecords(assistantMessage, productId, conversationChatId, requestText)
    updateConversationStatus(conversationChatId, '回复完成。')
  } catch (error) {
    if (error.name === 'AbortError') {
      updateConversationStatus(conversationChatId, '已停止。')
    } else {
      const errorText = error.message || '发送失败'
      assistantMessage.content += errorText
      updateConversationStatus(conversationChatId, errorText, 'warn')
      antMessage.error(errorText)
    }
  } finally {
    assistantMessage.pending = false
    if (streamContexts.get(conversationChatId)?.streamId === streamContext.streamId) {
      streamContexts.delete(conversationChatId)
    }
    scrollActiveConversation(conversationChatId)
  }
  if (String(selectedProductId.value) === String(productId)) {
    await loadConversations(productId, true)
  }
}

async function appendReferenceRecords(currentMessage, productId, chatId, query) {
  if (!query.trim()) {
    return
  }
  currentMessage.loadingReferences = true
  try {
    const referenceQuery = query.slice(0, KNOWLEDGE_RECALL_QUERY_MAX_LENGTH)
    const [knowledgeRecallResult, knowledgeGraphicResult] = await Promise.allSettled([
      fetchKnowledgeRecall(productId, referenceQuery),
      fetchKnowledgeGraphicReference(productId, referenceQuery),
    ])
    let shouldScroll = false
    if (knowledgeRecallResult.status === 'fulfilled' && knowledgeRecallResult.value.length) {
      currentMessage.knowledgeRecall = knowledgeRecallResult.value
      shouldScroll = true
    }
    if (knowledgeGraphicResult.status === 'fulfilled' && hasGraphicReference(knowledgeGraphicResult.value)) {
      currentMessage.knowledgeGraphicReference = knowledgeGraphicResult.value
      shouldScroll = true
    }
    if (shouldScroll && chatId === activeChatId.value) {
      await scrollToBottom()
    }
  } catch (error) {
    console.warn('Reference records failed:', error)
  } finally {
    currentMessage.loadingReferences = false
  }
}

async function fetchKnowledgeRecall(productId, query) {
  try {
    const res = await postKnowledgeChatRecall({
      productId,
      query,
    })
    const payload = res.data || {}
    if (payload.errorCode !== 200) {
      return []
    }
    return normalizeKnowledgeRecall(payload.data)
  } catch (error) {
    console.warn('Knowledge recall failed:', error)
    return []
  }
}

async function fetchKnowledgeGraphicReference(productId, query) {
  try {
    const res = await queryKnowledgeGraphicReference({
      productId,
      query,
    })
    const payload = res.data || {}
    if (payload.errorCode !== 200) {
      return null
    }
    return normalizeKnowledgeGraphicReference(payload.data)
  } catch (error) {
    console.warn('Knowledge graphic reference failed:', error)
    return null
  }
}

function normalizeKnowledgeRecall(data) {
  if (!Array.isArray(data)) {
    return []
  }
  return data
    .map((item) => ({
      text: String(item?.text || '').trim(),
      score: Number(item?.score ?? 0),
    }))
    .filter((item) => item.text)
}

function normalizeKnowledgeGraphicReference(data) {
  if (!data || typeof data !== 'object') {
    return null
  }
  const nodes = normalizeGraphicNodes(data.nodes)
  const relations = normalizeGraphicRelations(data.relations)
  if (!nodes.length && !relations.length) {
    return null
  }
  return {
    nodes,
    relations,
  }
}

function normalizeGraphicNodes(nodes) {
  if (!Array.isArray(nodes)) {
    return []
  }
  return nodes
    .map((node) => ({
      name: String(node?.name || '').trim(),
      des: String(node?.des || '').trim(),
      attributes: Array.isArray(node?.attributes)
        ? node.attributes.map((attribute) => String(attribute || '').trim()).filter(Boolean)
        : [],
    }))
    .filter((node) => node.name)
}

function normalizeGraphicRelations(relations) {
  if (!Array.isArray(relations)) {
    return []
  }
  return relations
    .map((relation) => ({
      from: String(relation?.from || '').trim(),
      name: String(relation?.name || '').trim(),
      to: String(relation?.to || '').trim(),
    }))
    .filter((relation) => relation.from && relation.to)
}

function hasGraphicReference(graphicReference) {
  return Boolean(
    graphicReference
    && (graphicReference.nodes?.length || graphicReference.relations?.length)
  )
}

async function stopStream() {
  const chatId = activeChatId.value
  const streamContext = streamContexts.get(chatId)
  if (!streamContext) {
    return
  }
  updateConversationStatus(chatId, '正在停止生成...')
  streamContext.abortController.abort()
  try {
    await stopAiControlStream(streamContext.productId, streamContext.streamId)
  } catch (error) {
    updateConversationStatus(chatId, error.message || '停止失败', 'warn')
  }
}

function createStreamId() {
  return createUuid()
}

function clearMessages() {
  messages.value = []
  saveConversationView(activeChatId.value, true)
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

async function scrollToBottom(settleLayout = false) {
  await nextTick()
  scrollMessagePaneToBottom()
  const maxFrames = settleLayout ? 24 : 1
  let previousHeight = -1
  let stableFrames = 0
  for (let frameIndex = 0; frameIndex < maxFrames; frameIndex += 1) {
    await new Promise((resolve) => window.requestAnimationFrame(resolve))
    const messagePane = getMessagePane()
    if (!messagePane) {
      return
    }
    const currentHeight = messagePane.scrollHeight
    messagePane.scrollTop = currentHeight
    stableFrames = currentHeight === previousHeight ? stableFrames + 1 : 0
    previousHeight = currentHeight
    if (settleLayout && frameIndex >= 11 && stableFrames >= 3) {
      break
    }
  }
}

function scrollMessagePaneToBottom() {
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
  grid-template-columns: 300px minmax(0, 1fr);
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
    grid-template-rows: minmax(260px, 42vh) minmax(0, 1fr);
  }
}
</style>
