import request from '@/utils/request'
import store from '@/store'

const token = store.getters['auth/token']
export const getProductName = (params) =>
  request({
    url: '/api/v2/getProductName',
    method: 'get',
    headers: {
        'Authorization': token
      },
    params
  })
export const getProduct = (data) =>
  request({
    url: '/api/v2/Product',
    method: 'get',
    headers: {
        'Authorization': token
      }
  })
  export const postProduct = (data) =>
    request({
      url: '/api/v2/Product',
      method: 'post',
      headers: {
        'Authorization': token
      },
      data
    })
    export const deleteProduct = (params) =>
      request({
        url: '/api/v2/Product',
        method: 'delete',
        headers: {
          'Authorization': token
        },
        params
      })