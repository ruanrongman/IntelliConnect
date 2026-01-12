import request from '@/utils/request'
import store from '@/store'

const token = store.getters['auth/token']
export const getProductLlmModel = (data) =>
  request({
    url: '/api/v2/productLlmModel',
    method: 'get',
    headers: {
        'Authorization': token
      }
  })
  export const postProductLlmModel = (data) =>
    request({
      url: '/api/v2/productLlmModel',
      method: 'post',
      headers: {
        'Authorization': token
      },
      data
    })
  export const deleteProductLlmModel = (params) =>
      request({
        url: '/api/v2/productLlmModel',
        method: 'delete',
        headers: {
          'Authorization': token
        },
        params
    })