<template>
  <div class="table-header">
    <slot></slot>
    <a-input-search
      v-model:value="searchValue"
      placeholder="输入搜索内容"
      style="width: 200px"
      allow-clear
      enter-button
      @search="(val) => emits('search', val)"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
const emits = defineEmits(['search'])
const searchValue = ref('')
</script>
<style lang="scss">
.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
</style>
