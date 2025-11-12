<template>
  <div class="flex-center">
    <a-breadcrumb :routes="matchedRoute">
      <template #itemRender="{ route, routes }">
        <span v-if="routes.indexOf(route) === routes.length - 1">
          {{ $t(`sidebar.${route.meta.title}`) }}
        </span>
        <router-link v-else :to="route.path">
          {{ $t(`sidebar.${route.meta.title}`) }}
        </router-link>
      </template>
    </a-breadcrumb>
  </div>
</template>
<script setup>
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
// import { setLocalLang } from '@/hooks/usei18n'
const $route = useRoute()
let matchedRoute = ref([])
watch(
  () => $route.matched,
  () => {
    matchedRoute.value = $route.matched.slice(1)
  },
  {
    immediate: true,
  }
)
</script>
