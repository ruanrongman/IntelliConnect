import auth from '@/store/auth'

function has(el, binding) {
  const { value } = binding
  if (Array.isArray(value)) {
    // const { auth } = auth.state
    // console.log(auth.state)
    if (!value.includes(auth.state.auth)) {
      // 权限不匹配 移除节点
      el.parentNode && el.parentNode.removeChild(el)
    }
  } else {
    throw new Error("The argument to 'v-has' must be an array")
  }
}

export default {
  mounted: (el, binding) => has(el, binding),
  updated: (el, binding) => has(el, binding),
}
