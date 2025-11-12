<template>
  <div>
    {{ name }}
    <router-view></router-view>
  </div>
</template>

<script setup>
import { ref } from 'vue'
const name = ref('page2')
</script>
