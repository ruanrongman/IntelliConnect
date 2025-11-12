import { ref, unref } from 'vue'

export function useFullScreen() {
  let isFull = ref(false)

  const requestFullScreen = () => {
    isFull.value = true
    document.documentElement.requestFullscreen()
  }
  const exitFullScreen = () => {
    isFull.value = false
    document.exitFullscreen()
  }

  return {
    isFull,
    exitFullScreen,
    requestFullScreen,
  }
}
