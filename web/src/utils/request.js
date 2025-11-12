import axios from 'axios'
import qs from 'qs'
// import { useAxiosCancel } from '@/hooks/useAxiosCancel'
// const CancelToken = axios.CancelToken
// const source = CancelToken.source()
const { VITE_BASE_URL, VITE_LOCAL_TEST_URL, VITE_MODE } = import.meta.env

const request = axios.create({
  baseURL: VITE_BASE_URL,
  // cancelToken: new CancelToken((cancel) => (axiosCancel = cancel)),
})

request.interceptors.request.use(
  (config) => {
    // removePendingRequest(config)
    // addPendingRequest(config)
    // const { removePendingRequest, addPendingRequest } = useAxiosCancel(config)
    return config
  },
  (err) => {
    return Promise.reject(err)
  }
)
request.interceptors.response.use(
  (response) => {
    removePendingRequest(response.config)
    return response
  },
  (error) => {
    removePendingRequest(error.config || {}) // 从pendingRequest对象中移除请求
    if (axios.isCancel(error)) {
      // 是取消请求的异常错误
    } else {
      // 其他异常错误
    }
    return Promise.reject(error)
  }
)

// request.post()
// console.dir(request)
export default request

// 创建请求唯一标识
function generateReqKey(config) {
  const { method, url, params, data } = config
  //console.log([method, url, qs.stringify(params), qs.stringify(data)].join('&'))
  return [method, url, qs.stringify(params), qs.stringify(data)].join('&')
}
const pendingRequest = new Map() // 存储请求唯一标识key

// 根据请求标识创建对用的cancelToken函数
// function addPendingRequest(config) {
//   const requestKey = generateReqKey(config)
//   config.cancelToken =
//     config.cancelToken ||
//     new axios.CancelToken((cancel) => {
//       if (!pendingRequest.has(requestKey)) {
//         pendingRequest.set(requestKey, cancel)
//       }
//     })
// }
// export let axiosCancel
// 根据请求标识移除对应的cancelToken函数，取消请求
export function removePendingRequest(config) {
  const requestKey = generateReqKey(config)
  if (pendingRequest.has(requestKey)) {
    const axiosCancel = pendingRequest.get(requestKey)
    axiosCancel('取消请求')
    pendingRequest.delete(requestKey)
  }
}

export const localRequest = axios.create({
  baseURL: VITE_MODE == 'development' ? '/local-test' : VITE_LOCAL_TEST_URL,
})
// export

export const cacheRequest = (request) => {
  const cache = Symbol('cache')
  return (...args) => {
    if (!request.prototype[cache]) {
      request.prototype[cache] = request(...args)
    }
    return request.prototype[cache]
  }
}
