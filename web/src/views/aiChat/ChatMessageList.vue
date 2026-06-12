<template>
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
        <MarkdownContent v-if="item.content" class="message-content" :content="item.content" />
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
</template>

<script setup>
import { ref } from 'vue'
import { message as antMessage } from 'ant-design-vue'
import MarkdownContent from '@/components/MarkdownContent/index.vue'

defineProps({
  messages: {
    type: Array,
    default: () => [],
  },
})

const messagePaneRef = ref(null)

defineExpose({
  getPaneElement: () => messagePaneRef.value,
})

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
</script>

<style lang="scss" scoped>
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

@media (max-width: 840px) {
  .message-row {
    max-width: 94%;
  }
}
</style>
