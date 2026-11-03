import { onBeforeUnmount, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'

export function useFullScreen() {
  const isFull = ref(Boolean(document.fullscreenElement))

  const syncFullscreen = () => {
    isFull.value = Boolean(document.fullscreenElement)
  }

  const requestFullScreen = async () => {
    try {
      await document.documentElement.requestFullscreen()
    } catch {
      message.info('浏览器未允许进入全屏，请重试。')
    }
    syncFullscreen()
  }
  const exitFullScreen = async () => {
    if (document.fullscreenElement) await document.exitFullscreen()
    syncFullscreen()
  }

  onMounted(() => {
    syncFullscreen()
    document.addEventListener('fullscreenchange', syncFullscreen)
  })
  onBeforeUnmount(() => document.removeEventListener('fullscreenchange', syncFullscreen))

  return {
    isFull,
    exitFullScreen,
    requestFullScreen,
  }
}
