<template>
  <aside class="chat-side">
    <div class="side-title">
      <div>
        <h2>智能体对话调试</h2>
        <span>{{ activeChatId || '未选择产品' }}</span>
      </div>
      <a-button type="text" :disabled="loadingProducts || streaming" @click="$emit('refresh-products')">
        <template #icon><ReloadOutlined /></template>
      </a-button>
    </div>

    <div class="field">
      <label>产品</label>
      <a-select
        :value="modelValue"
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
      <a-button block :disabled="!modelValue || streaming || loadingHistory" @click="$emit('load-history')">
        <template #icon><HistoryOutlined /></template>
        历史
      </a-button>
    </div>

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
defineProps({
  modelValue: {
    type: [Number, String],
    default: undefined,
  },
  productOptions: {
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
  streaming: {
    type: Boolean,
    default: false,
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

const emit = defineEmits(['update:modelValue', 'refresh-products', 'load-history', 'load-more-history', 'product-change'])

function handleProductChange(value) {
  emit('update:modelValue', value)
  emit('product-change')
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

@media (max-width: 840px) {
  .chat-side {
    border-right: 0;
    border-bottom: 1px solid #dde3e1;
  }
}
</style>
