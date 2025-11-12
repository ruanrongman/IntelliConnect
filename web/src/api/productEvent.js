import request from '@/utils/request'
import store from '@/store'

const token = store.getters['auth/token']
export const getProductEvent = (data) =>
  request({
    url: '/api/v2/ProductEvent',
    method: 'get',
    headers: {
        'Authorization': token
      }
  })
  export const postProductEvent = (data) =>
    request({
      url: '/api/v2/ProductEvent',
      method: 'post',
      headers: {
        'Authorization': token
      },
      data
    })
  export const deleteProductEvent = (params) =>
      request({
        url: '/api/v2/ProductEvent',
        method: 'delete',
        headers: {
          'Authorization': token
        },
        params
    })