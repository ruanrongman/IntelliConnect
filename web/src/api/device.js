import request from '@/utils/request'
import store from '@/store'

const token = store.getters['auth/token']
export const getProductDevice = (data) =>
  request({
    url: '/api/v2/ProductDevice',
    method: 'get',
    headers: {
        'Authorization': token
      }
  })
  export const postProductDevice = (data) =>
    request({
      url: '/api/v2/ProductDevice',
      method: 'post',
      headers: {
        'Authorization': token
      },
      data
    })
    export const deleteProductDevice = (params) =>
      request({
        url: '/api/v2/ProductDevice',
        method: 'delete',
        headers: {
          'Authorization': token
        },
        params
      })