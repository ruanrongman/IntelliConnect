<template>
  <aside class="chat-side">
    <div class="side-title">
      <div>
        <h2>智能体对话调试</h2>
        <span>{{ activeChatId || '未选择产品' }}</span>
      </div>
      <a-tooltip title="刷新产品和对话">
        <a-button
          type="text"
          :disabled="loadingProducts || loadingConversations || streaming"
          @click="$emit('refresh-products')"
        >
          <template #icon><ReloadOutlined /></template>
        </a-button>
      </a-tooltip>
    </div>

    <div class="field">
      <label>产品</label>
      <a-select
        :value="modelValue"
        :options="productOptions"
        show-search
        :filter-option="filterProductOption"
        :loading="loadingProducts"
        placeholder="选择产品"
        @change="handleProductChange"
      />
    </div>

    <a-button
      block
      type="primary"
      :disabled="!modelValue"
      @click="$emit('new-conversation')"
    >
      <template #icon><PlusOutlined /></template>
      新建对话
    </a-button>

    <section class="conversation-section">
      <div class="section-title">
        <span>对话</span>
        <span>{{ conversations.length }}</span>
      </div>
      <div class="conversation-list">
        <button
          v-for="conversation in conversations"
          :key="conversation.chatId"
          type="button"
          class="conversation-item"
          :class="{ active: conversation.chatId === activeChatId }"
          @click="handleConversationClick(conversation)"
        >
          <span class="conversation-copy">
            <strong>{{ conversation.title }}</strong>
            <span v-if="conversation.deviceName" class="device-meta">
              {{ conversation.nickName || '未命名设备' }} · {{ conversation.deviceName }}
            </span>
          </span>
          <LoadingOutlined v-if="streamingChatIds.includes(conversation.chatId)" class="conversation-loading" spin />
        </button>
      </div>
      <div
        v-if="memoryPreviewOpen && previewConversation?.fullContent"
        class="memory-preview"
        :class="{ collapsed: memoryPreviewCollapsed }"
      >
        <div class="memory-preview-header">
          <span>完整记忆</span>
          <div class="memory-preview-actions">
            <a-button
              type="text"
              size="small"
              :title="memoryPreviewCollapsed ? '展开记忆' : '折叠记忆'"
              @click.stop="memoryPreviewCollapsed = !memoryPreviewCollapsed"
            >
              <template #icon>
                <UpOutlined v-if="memoryPreviewCollapsed" />
                <DownOutlined v-else />
              </template>
            </a-button>
            <a-button type="text" size="small" title="关闭记忆" @click.stop="closeMemoryPreview">
              <template #icon><CloseOutlined /></template>
            </a-button>
          </div>
        </div>
        <p v-show="!memoryPreviewCollapsed">{{ previewConversation.fullContent }}</p>
      </div>
    </section>

    <div class="history-pager">
      <a-button
        block
        size="small"
        :disabled="!modelValue || streaming || loadingHistory || !hasMoreHistory"
        @click="$emit('load-more-history')"
      >
        加载更早历史
      </a-button>
      <span v-if="historyTotal > 0">
        已显示 {{ loadedHistoryCount }} / {{ historyTotal }}
      </span>
    </div>

    <div class="side-meta" :class="{ warn: statusType === 'warn' }">
      {{ statusText }}
    </div>
  </aside>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

const memoryPreviewChatId = ref('')
const memoryPreviewOpen = ref(false)
const memoryPreviewCollapsed = ref(false)
const props = defineProps({
  modelValue: {
    type: [Number, String],
    default: undefined,
  },
  productOptions: {
    type: Array,
    default: () => [],
  },
  conversations: {
    type: Array,
    default: () => [],
  },
  activeChatId: {
    type: String,
    default: '',
  },
  loadingProducts: {
    type: Boolean,
    default: false,
  },
  loadingConversations: {
    type: Boolean,
    default: false,
  },
  streaming: {
    type: Boolean,
    default: false,
  },
  streamingChatIds: {
    type: Array,
    default: () => [],
  },
  loadingHistory: {
    type: Boolean,
    default: false,
  },
  hasMoreHistory: {
    type: Boolean,
    default: false,
  },
  historyTotal: {
    type: Number,
    default: 0,
  },
  loadedHistoryCount: {
    type: Number,
    default: 0,
  },
  statusText: {
    type: String,
    default: '',
  },
  statusType: {
    type: String,
    default: '',
  },
})

const previewConversation = computed(() =>
  props.conversations.find((conversation) => conversation.chatId === memoryPreviewChatId.value)
)

watch(() => props.modelValue, () => {
  closeMemoryPreview()
})

const emit = defineEmits([
  'update:modelValue',
  'refresh-products',
  'load-more-history',
  'product-change',
  'new-conversation',
  'select-conversation',
])

function handleProductChange(value) {
  closeMemoryPreview()
  emit('update:modelValue', value)
  emit('product-change', value)
}

function handleConversationClick(conversation) {
  if (conversation?.fullContent) {
    memoryPreviewChatId.value = conversation.chatId
    memoryPreviewOpen.value = true
    memoryPreviewCollapsed.value = false
  } else {
    closeMemoryPreview()
  }
  emit('select-conversation', conversation.chatId)
}

function closeMemoryPreview() {
  memoryPreviewOpen.value = false
  memoryPreviewCollapsed.value = false
  memoryPreviewChatId.value = ''
}

function filterProductOption(input, option) {
  const keyword = String(input || '').trim().toLowerCase()
  if (!keyword) {
    return true
  }
  return String(option?.label || '').toLowerCase().includes(keyword)
}
</script>

<style lang="scss" scoped>
.chat-side {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 0;
  border-right: 1px solid #dde3e1;
  background: #fff;
  overflow: hidden;
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

.conversation-section {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 8px;
  min-height: 0;
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #66736f;
  font-size: 12px;
  font-weight: 600;
}

.conversation-list {
  display: grid;
  align-content: start;
  gap: 4px;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
}

.conversation-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  width: 100%;
  min-height: 48px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #27332f;
  cursor: pointer;
  padding: 9px 10px;
  text-align: left;
  transition: background 0.15s ease, color 0.15s ease;

  &:hover:not(:disabled) {
    background: #f1f5f3;
  }

  &.active {
    background: #e4efeb;
    color: #10231d;
  }

  &:disabled {
    cursor: not-allowed;
  }
}


.conversation-loading {
  color: #1f7a5c;
  font-size: 14px;
}

.conversation-copy {
  display: block;
  min-width: 0;

  strong,
  .device-meta {
    display: block;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  strong {
    font-size: 13px;
    font-weight: 600;
    line-height: 1.5;
  }
}

.device-meta {
  margin-top: 3px;
  color: #66736f;
  font-size: 11px;
  line-height: 1.4;
}

.memory-preview {
  display: grid;
  flex: 0 0 132px;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 5px;
  min-height: 0;
  border-top: 1px solid #dde3e1;
  background: #f7faf9;
  overflow: hidden;
  padding: 7px 8px 9px;
  transition: flex-basis 0.15s ease;

  &.collapsed {
    flex-basis: 38px;
    grid-template-rows: auto;
    padding-bottom: 7px;
  }

  p {
    margin: 0;
    color: #27332f;
    font-size: 12px;
    line-height: 1.55;
    overflow-y: auto;
    padding: 0 2px;
    white-space: pre-wrap;
    word-break: break-word;
  }
}

.memory-preview-header {
  display: flex;
  min-height: 24px;
  align-items: center;
  justify-content: space-between;
  gap: 8px;

  > span {
    color: #66736f;
    font-size: 11px;
    font-weight: 600;
  }
}

.memory-preview-actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;

  :deep(.ant-btn) {
    width: 26px;
    height: 24px;
    color: #66736f;
    padding: 0;
  }
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

@media (max-width: 840px) {
  .chat-side {
    border-right: 0;
    border-bottom: 1px solid #dde3e1;
  }
}
</style>
