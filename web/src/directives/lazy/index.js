import { useThrottle } from '@/hooks/useThrottle'

// 初始化
function init(el, val, def) {
  // console.log(def)
  el.setAttribute('data-src', val)
  // el.setAttribute('src', def)
}
// 利用IntersectionObserver监听el
function observe(el) {
  var io = new IntersectionObserver((entries) => {
    // console.dir(el.dataset, 2)
    const realSrc = el.dataset.src
    if (entries[0].isIntersecting) {
      if (realSrc) {
        el.src = realSrc
        el.removeAttribute('data-src')
      }
    }
  })
  io.observe(el)
}
// 监听scroll事件
function listenerScroll(el) {
  const handler = useThrottle(load, 300)
  console.log(handler)
  load(el)
  document.querySelector('.img-wrapper').addEventListener('scroll', () => handler(el))
}
// 加载真实图片
function load(el) {
  console.log(34)
  const windowHeight = document.documentElement.clientHeight
  const elTop = el.getBoundingClientRect().top
  const elBtm = el.getBoundingClientRect().bottom
  const realSrc = el.dataset.src
  if (elTop - windowHeight < 0 && elBtm > 0) {
    if (realSrc) {
      el.src = realSrc
      el.removeAttribute('data-src')
    }
  }
}

export default {
  beforeMount: (el, binding) => init(el, binding.value),
  mounted: (el) => {
    if (IntersectionObserver) {
      observe(el)
    } else {
      listenerScroll(el)
    }
  },
}
