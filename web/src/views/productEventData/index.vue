<template>
  <div class="data-container">
    <HeaderCard />
    <div class="data-content">
      <div class="action-bar">
        <AddData />
      </div>
      <Mytable />
    </div>
  </div>
</template>

<script setup>
import HeaderCard from './HeaderCard.vue'
import AddData from './AddData.vue'
import Mytable from './Mytable.vue'
import timeCard from './timeCard.vue'
</script>

<style lang="scss" scoped>
.data-container {
  padding: 24px;
  background-color: #f0f2f5;
  min-height: 100vh;

  .data-content {
    background: #fff;
    border-radius: 12px;
    padding: 24px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

    .action-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
      padding-bottom: 24px;
      border-bottom: 1px solid #f0f0f0;
    }
  }
}
</style>