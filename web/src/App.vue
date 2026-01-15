<template>
  <a-config-provider :locale="locale">
    <router-view></router-view>
  </a-config-provider>
</template>

<script setup>
import { useLocale } from '@/hooks/useLocale.js'
const { getAntdLocale } = useLocale()
const locale = getAntdLocale()
console.log(locale)
</script>

<style lang="scss">
#app {
  // min-height: 100%;
  // height: 100%; // 解决登陆界面居中问题
  width: 100%;
  height: 100%;
}
</style>
