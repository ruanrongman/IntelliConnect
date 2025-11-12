<template>
  <div style="background-color: #f0f2f5">
    <router-view></router-view>
  </div>
</template>
<script setup>
import { ref } from 'vue'
let name = ref('无限级菜单')
</script>
