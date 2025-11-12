<template>
  <a-layout-header class="header">
    <div class="header-left">
      <slot name="headerLeft"> </slot>
      <BreadCrumb />
    </div>
    <div class="header-right">
      <div class="full-screen header-right-item">
        <CompressOutlined v-if="isFull" @click="exitFullScreen" />
        <ExpandOutlined v-else @click="requestFullScreen" />
      </div>
      <div class="header-right-item">
        <GithubOutlined @click="handleClickGit" />
      </div>

      <LangMenu />
      <!-- <a-dropdown>
        <span style="width: 60px" class="header-right-item">
          {{ $t('system.language') }}
        </span>
        <template #overlay>
          <a-menu @click="handleChangeLanguage">
            <a-menu-item :disabled="curLocale == 'zh_CN'" key="zh_CN">
              中文简体
            </a-menu-item>
            <a-menu-item :disabled="curLocale == 'en'" key="en">
              English</a-menu-item
            >
          </a-menu>
        </template>
      </a-dropdown> -->
      <a-dropdown>
        <span class="header-right-item">
          <a-avatar :src="avatar"></a-avatar>
        </span>
        <template #overlay>
          <a-menu>
            <a-menu-item>
              <router-link to="/setting">{{ $t('system.setting') }}</router-link>
            </a-menu-item>
            <a-menu-item @click="handleLoginOut"> {{ $t('system.loginOut') }}</a-menu-item>
          </a-menu>
        </template>
      </a-dropdown>
    </div>
  </a-layout-header>
</template>

<script setup>
import avatar from '@/assets/img/avatar.jpg'
// import LangMenu from './LangMenu.vue'
import BreadCrumb from './BreadCrumb.vue'
// import { ref, computed } from 'vue'
// import { useLocale } from '@/hooks/useLocale'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'

import { useFullScreen } from '@/hooks/useFullScreen'

const store = useStore()
const router = useRouter()
// const { changeLang, curLocale } = useLocale()
const { isFull, requestFullScreen, exitFullScreen } = useFullScreen()
// const handleChangeLanguage = async ({ key }) => {
//   await changeLang(key)
//   store.commit('app/SET_LANG', key)
//   location.reload()
// }
const handleClickGit = () => {
  location.href = 'https://github.com/BaiFangZi/vue3-antd-manage'
}
const cleanSys = () => {
  // store.commit('app/CLEAR_LANG')
  store.commit('app/CLEAR_AMAP_KEY')
  store.commit('app/CLEAR_BMAP_KEY')
  store.commit('auth/CLEAR_AUTH')
}
const handleLoginOut = () => {
  cleanSys()
  router.replace('/login')
}
</script>

<style lang="scss">
.header {
  background: #fff;
  padding: 0;
  display: flex;
  justify-content: space-between;

  .header-left {
    display: flex;
  }

  .header-right {
    margin: 0 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;

    .header-right-item {
      margin: 0 6px;
      cursor: pointer;

      &:hover {
        color: #1890ff;
      }
    }

    .anticon {
      font-size: 20px;
    }
  }
}

.trigger {
  font-size: 18px;
  line-height: 64px;
  padding: 0 24px;
  cursor: pointer;
  transition: color 0.3s;
}

.trigger:hover {
  color: #1890ff;
}
</style>
