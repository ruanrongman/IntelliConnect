<template>
  <a-layout-content
    :style="{
      margin: '24px 16px',
      height: '100%',
    }"
  >
    <router-view class="content"></router-view>
  </a-layout-content>
</template>
<script>
export default {
  name: 'LayoutContent',
}
</script>
<style lang="scss">
.content {
  background-color: #fff;
  padding: 24px;
  // min-height: calc(100vh - 112px);
}
</style>
