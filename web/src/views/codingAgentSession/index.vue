<template>
  <div class="coding-agent-session-container">
    <div class="coding-agent-session-content">
      <div class="action-bar">
        <add-session @created="refreshList" />
      </div>
      <Mytable ref="tableRef" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import addSession from './add_session.vue'
import Mytable from './Mytable.vue'

const tableRef = ref(null)

const refreshList = () => {
  tableRef.value?.fetchCodingAgentSession?.()
}
</script>

<style lang="scss" scoped>
.coding-agent-session-container {
  padding: 24px;
  background-color: #f0f2f5;
  min-height: 100vh;

  .coding-agent-session-content {
    background: #fff;
    border-radius: 8px;
    padding: 24px;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);

    .action-bar {
      margin-bottom: 24px;
      padding-bottom: 24px;
      border-bottom: 1px solid #f0f0f0;
      display: flex;
      gap: 16px;
    }
  }
}
</style>
