<template>
  <form class="composer" @submit.prevent="$emit('submit')">
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
          @click="$emit('remove-file', file)"
        >
          <template #icon><CloseOutlined /></template>
        </a-button>
      </div>
    </div>
    <div class="input-shell">
      <a-textarea
        :value="modelValue"
        :auto-size="{ minRows: 1, maxRows: 5 }"
        :disabled="streaming"
        placeholder="输入消息"
        @update:value="$emit('update:modelValue', $event)"
        @compositionstart="handleCompositionStart"
        @compositionend="handleCompositionEnd"
        @keydown.enter.exact="handleEnter"
        @paste="$emit('paste', $event)"
      />
    </div>
    <div class="composer-tools">
      <a-tooltip title="上传文件">
        <a-button class="attach-button" type="text" html-type="button" :disabled="streaming"
          aria-label="上传聊天附件" @click="openFilePicker">
          <template #icon><PaperClipOutlined /></template>
          附件
        </a-button>
      </a-tooltip>
      <input ref="fileInput" type="file" multiple hidden :disabled="streaming"
        :accept="acceptedFileExtensions.join(',')" @change="handleFileChange"
        @cancel="restoreFullscreen" />
      <a-tooltip title="管理当前会话的文件与智能体产物">
        <a-button class="workspace-button" type="text" html-type="button"
          :disabled="!workspaceEnabled" @click="$emit('open-workspace')">
          <template #icon><FolderOpenOutlined /></template>
          会话工作区
        </a-button>
      </a-tooltip>
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
    <a-button v-else danger @click="$emit('stop')">
      <template #icon><StopOutlined /></template>
      停止
    </a-button>
    <Transition name="usage-fade">
      <div v-if="usageView" class="usage-status">
        <a-popover v-model:open="usageOpen" trigger="click" placement="topLeft"
          overlay-class-name="usage-popover" :arrow="false">
          <template #content>
            <div class="usage-card">
              <div class="usage-card-head">
                <span><DatabaseOutlined /> Token 用量 <em>{{ usageView.scope }}</em></span>
                <strong>{{ usageView.total }}</strong>
              </div>
              <div v-for="row in usageView.rows" :key="row.label" class="usage-card-row">
                <span>{{ row.label }}</span>
                <span>{{ row.value }}</span>
              </div>
              <div v-if="usageView.context" class="usage-card-row">
                <span>上下文占用</span>
                <span>{{ usageView.context.label }} · {{ usageView.context.detail }}</span>
              </div>
            </div>
          </template>
          <button type="button" class="usage-trigger" :class="{ active: usageOpen }">
            <DatabaseOutlined />
            <span>{{ usageView.chip }}</span>
            <template v-if="usageView.context">
              <svg class="usage-ring" viewBox="0 0 16 16" aria-hidden="true">
                <circle cx="8" cy="8" r="6" class="usage-ring-track" />
                <circle cx="8" cy="8" r="6" class="usage-ring-value"
                  :stroke-dasharray="`${(usageView.context.percent / 100) * RING_LENGTH} ${RING_LENGTH}`" />
              </svg>
              <span>{{ usageView.context.label }}</span>
            </template>
          </button>
        </a-popover>
      </div>
    </Transition>
  </form>
</template>

<script setup>
import { computed, ref } from 'vue'
// Popover is not in the global antd registration (plugins/antd.js), so register it locally.
import { Popover as APopover } from 'ant-design-vue'
import { useFilePicker } from '@/hooks/useFilePicker'
import { usageStatus } from '@/utils/llmUsage.mjs'

const RING_LENGTH = 2 * Math.PI * 6

const props = defineProps({
  usage: {
    type: Object,
    default: null,
  },
  modelValue: {
    type: String,
    default: '',
  },
  selectedFiles: {
    type: Array,
    default: () => [],
  },
  streaming: {
    type: Boolean,
    default: false,
  },
  canSend: {
    type: Boolean,
    default: false,
  },
  workspaceEnabled: {
    type: Boolean,
    default: false,
  },
  acceptedFileExtensions: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:modelValue', 'submit', 'stop', 'select-file', 'paste', 'remove-file', 'open-workspace'])
const { fileInput, openFilePicker, restoreFullscreen } = useFilePicker()
const usageView = computed(() => usageStatus(props.usage))
const usageOpen = ref(false)
let isComposing = false

function handleCompositionStart() {
  isComposing = true
}

function handleCompositionEnd() {
  isComposing = false
}

function handleEnter(event) {
  if (isComposing || event.isComposing || event.keyCode === 229) {
    return
  }
  event.preventDefault()
  emit('submit')
}

function handleFileChange(event) {
  restoreFullscreen()
  const files = Array.from(event.target.files || [])
  event.target.value = ''
  files.forEach((file) => emit('select-file', file))
}
</script>

<style lang="scss" scoped>
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
  grid-column: 1 / -1;
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

.composer-tools {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  min-width: 0;
}

.attach-button,
.workspace-button {
  color: #66736f;

  &:hover {
    color: #1f7a5c;
    background: #eef5f2;
  }
}

.usage-status {
  grid-column: 1 / -1;
  display: flex;
  margin-top: -6px;
  min-width: 0;
}

.usage-trigger {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 100%;
  min-width: 0;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #8a9692;
  padding: 3px 8px;
  font-size: 12px;
  line-height: 1.5;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
  cursor: pointer;
  transition: color 0.2s ease, background 0.2s ease;

  span {
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &:hover,
  &.active {
    color: #34423e;
    background: #f1f5f4;
  }

  &:focus-visible {
    outline: 2px solid rgba(31, 122, 92, 0.35);
    outline-offset: 1px;
  }
}

.usage-ring {
  flex: none;
  width: 14px;
  height: 14px;
  margin-left: 6px;
  transform: rotate(-90deg);

  circle {
    fill: none;
    stroke-width: 2;
  }
}

.usage-ring-track {
  stroke: #dfe6e4;
}

.usage-ring-value {
  stroke: #1f7a5c;
  stroke-linecap: round;
  transition: stroke-dasharray 0.4s ease;
}

.usage-card {
  min-width: 248px;
  color: #d7dddb;
  font-size: 13px;
  font-variant-numeric: tabular-nums;
}

.usage-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 8px;
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  color: #fff;

  span {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    font-weight: 600;
  }

  em {
    color: #8f9a97;
    font-size: 12px;
    font-style: normal;
    font-weight: 400;
  }
}

.usage-card-row {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 4px 0;

  span:first-child {
    color: #9aa5a2;
  }
}

.usage-fade-enter-active,
.usage-fade-leave-active {
  transition: opacity 0.3s ease;
}

.usage-fade-enter-from,
.usage-fade-leave-to {
  opacity: 0;
}

@media (max-width: 840px) {
  .composer {
    padding: 12px;
    gap: 8px;
  }
}
</style>

<style lang="scss">
/* The popover is teleported to <body>, so its shell needs an unscoped rule. */
.usage-popover .ant-popover-inner {
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 10px;
  background: #1f2322;
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.28);
  padding: 12px 14px;
}
</style>
