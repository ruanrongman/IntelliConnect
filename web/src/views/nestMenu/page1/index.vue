<template>
  <div class="page1-container">
    <HeaderCard />
    
    <div class="page1-content">
      <div class="action-bar">
        <newModel />
        <timeCard />
      </div>
      
      <Mytable />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import HeaderCard from './HeaderCard.vue'
import timeCard from './timeCard.vue'
import newModel from './add_model.vue'
import Mytable from './Mytable.vue'

const name = ref('page1')
</script>

<style lang="scss" scoped>
.page1-container {
  padding: 24px;
  background-color: #f0f2f5;
  min-height: 100vh;

  .page1-content {
    background: #fff;
    border-radius: 8px;
    padding: 24px;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);

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
