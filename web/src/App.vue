<template>
  <a-config-provider :locale="locale">
    <router-view></router-view>
  </a-config-provider>
</template>

<script setup>
import { useLocale } from '@/hooks/useLocale.js'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
const { getAntdLocale } = useLocale()
const locale = getAntdLocale()
console.log(locale)
// TODO: 判断用户是否登录，如果登录则初始化路由并跳转
const store = useStore();
await store.commit('auth/GENERATE_ROUTES', "Bearer eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiW1JPTEVfYWRtaW5dIiwiZXhwIjoxNzY4OTg3NzcwLCJpYXQiOjE3NjgzODI5NzAsInVzZXJuYW1lIjoiYWRtaW4ifQ._n0JKRLoe86FcOd6FcD6ApAtfssazX3BAEvMqQH6rho");
const router = useRouter();
console.log(router.getRoutes());
</script>

<style lang="scss">
#app {
  // min-height: 100%;
  // height: 100%; // 解决登陆界面居中问题
  width: 100%;
  height: 100%;
}
</style>
