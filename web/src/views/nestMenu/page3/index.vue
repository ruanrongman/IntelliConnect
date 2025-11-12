<template>
  <div style="background-color: #f0f2f5">
    {{ name }}
    <router-view></router-view>
  </div>
</template>
<script setup>
import { ref } from 'vue'

const name = ref('page3')
</script>
