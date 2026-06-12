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
      <a-upload
        :before-upload="beforeUploadFile"
        :show-upload-list="false"
        multiple
        :accept="acceptedFileExtensions.join(',')"
      >
        <a-tooltip title="上传文件">
          <a-button class="attach-button" type="text" shape="circle" html-type="button" :disabled="streaming">
            <template #icon><PaperClipOutlined /></template>
          </a-button>
        </a-tooltip>
      </a-upload>
      <a-textarea
        :value="modelValue"
        :auto-size="{ minRows: 1, maxRows: 5 }"
        :disabled="streaming"
        placeholder="输入消息"
        @update:value="$emit('update:modelValue', $event)"
        @keydown.enter.exact.prevent="$emit('submit')"
        @paste="$emit('paste', $event)"
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
    <a-button v-else danger @click="$emit('stop')">
      <template #icon><StopOutlined /></template>
      停止
    </a-button>
  </form>
</template>

<script setup>
defineProps({
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
  acceptedFileExtensions: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:modelValue', 'submit', 'stop', 'select-file', 'paste', 'remove-file'])

function beforeUploadFile(file) {
  emit('select-file', file)
  return false
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
  .composer {
    grid-template-columns: 1fr;
  }
}
</style>
