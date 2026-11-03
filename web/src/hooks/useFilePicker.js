import { ref } from 'vue'
import { message } from 'ant-design-vue'

export function useFilePicker() {
  const fileInput = ref(null)
  let fullscreenElement = null

  function openFilePicker() {
    if (!fileInput.value) return
    fullscreenElement = document.fullscreenElement
    fileInput.value.click()
  }

  function restoreFullscreen() {
    const element = fullscreenElement
    fullscreenElement = null
    if (!element?.isConnected || document.fullscreenElement) return
    // Request immediately in the picker change/cancel event, before any async upload work.
    try {
      Promise.resolve(element.requestFullscreen()).catch(notifyFullscreenBlocked)
    } catch {
      notifyFullscreenBlocked()
    }
  }

  function notifyFullscreenBlocked() {
    message.info('浏览器未允许自动恢复全屏，请点击右上角全屏按钮。')
  }

  return { fileInput, openFilePicker, restoreFullscreen }
}
