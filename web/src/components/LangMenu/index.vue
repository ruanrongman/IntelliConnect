<template>
  <div class="lang-menu">
    <a-dropdown>
      <span style="width: 60px" class="header-right-item">
        {{ $t('system.language') }}
      </span>
      <template #overlay>
        <a-menu @click="handleChangeLanguage">
          <a-menu-item key="zh_CN" :disabled="curLocale == 'zh_CN'"> 中文简体 </a-menu-item>
          <a-menu-item key="en" :disabled="curLocale == 'en'"> English</a-menu-item>
        </a-menu>
      </template>
    </a-dropdown>
  </div>
</template>

<script setup>
import { useLocale } from '@/hooks/useLocale'
import { useStore } from 'vuex'
const store = useStore()
!store.state.app.lang && store.commit('app/SET_LANG', 'zh_CN')
const { changeLang, curLocale } = useLocale()
const handleChangeLanguage = async ({ key }) => {
  await changeLang(key)
  store.commit('app/SET_LANG', key)
  location.reload()
}
</script>
