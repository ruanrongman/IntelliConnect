<template>
  <!-- <div>
   
  </div> -->
  <a-layout-sider :collapsed="collapsed" :trigger="null" collapsible class="scrollbar">
    <div class="logo"></div>
    <a-menu v-model:selectedKeys="selectedKeys" theme="dark" mode="inline" @click="handleGoRouter">
      <template v-for="item in menuList" :key="item.path">
        <template v-if="!item.children">
          <a-menu-item v-if="!item.meta.isHidden" :key="item.path">
            <template #icon>
              <component :is="item.meta.icon"></component>
            </template>
            {{ $t(`sidebar.${item.meta.title}`) }}
          </a-menu-item>
        </template>
        <template v-else>
          <SubMenu :menu-list="item"></SubMenu>
        </template>
      </template>
    </a-menu>
  </a-layout-sider>
</template>

<script>
const SubMenu = {
  name: 'sub-menu',
  props: {
    menuList: {
      type: Object,
      default: () => ({}),
    },
  },
  template: `
  <a-sub-menu :key="menuList.path">
     <template #title>{{ $t('sidebar.'+menuList.meta.title) }}</template>
      <template #icon>
            <component :is="menuList.meta.icon"></component>
      </template>
     <template v-for="item in menuList.children" :key="item.path">
        <template v-if="!item.children">
          <a-menu-item :key="item.path">
          
           {{ $t('sidebar.'+item.meta.title)}}
          </a-menu-item>
        </template>
        <template v-else>
          <sub-menu :menu-list="item" :key="item.path" />
        </template>
      </template>
  </a-sub-menu>`,
}
</script>

<script setup>
import { useStore } from 'vuex'
import { useRouter, useRoute } from 'vue-router'
import { ref } from '@vue/reactivity'
import { watch } from '@vue/runtime-core'
defineProps({
  collapsed: Boolean,
})
const store = useStore()
const router = useRouter()
const route = useRoute()
const menuList = store.state.auth.menuList

let selectedKeys = ref([])
watch(
  () => route.path,
  () => {
    selectedKeys.value = [route.path]
  },
  {
    immediate: true,
  }
)

const handleGoRouter = ({ key }) => {
  router.push(key)
}
</script>

<style lang="scss">
.logo {
  // display: inline-block;
  height: 100px;
  background: rgba(255, 255, 255, 0.3);
  margin: 16px;
  color: #fff;
  text-align: center;
  // vertical-align: middle;
  //line-height: 32px;
  //font-size: 18px;
  background: url(../../assets/logo.png) no-repeat;
  background-position: center center;
  background-size: contain;
}
</style>
