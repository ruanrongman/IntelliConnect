import request from '@/utils/request'
import store from '@/store'

const token = store.getters['auth/token']
export const getProductFunction = (data) =>
  request({
    url: '/api/v2/ProductFunction',
    method: 'get',
    headers: {
        'Authorization': token
      }
  })
  export const postProductFunction = (data) =>
    request({
      url: '/api/v2/ProductFunction',
      method: 'post',
      headers: {
        'Authorization': token
      },
      data
    })
    export const deleteProductFunction = (params) =>
      request({
        url: '/api/v2/ProductFunction',
        method: 'delete',
        headers: {
          'Authorization': token
        },
        params
      })