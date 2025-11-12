<template>
  <div id="viewer-container"></div>
</template>
<script setup>
import { onMounted } from 'vue'

// import DC from '@/plugins/DCMap'
const emit = defineEmits(['on-viewer-completed'])
const props = defineProps({
  options: {
    type: Object,
    default: () => {},
  },
})
const initViewer = () => {
  let viewer = new window.DC.Viewer('viewer-container')
  const { key } = props.options
  viewer.addBaseLayer(
    window.DC.ImageryLayerFactory.createAmapImageryLayer({
      style: 'img',
      key: key ? key : null,
    })
  )
  emit('on-viewer-completed', viewer)
}
onMounted(() => {
  window.DC.ready(() => {
    initViewer()
  })
})
</script>
<style scoped>
#viewer-container {
  width: 100%;
  height: 100%;
}
</style>
