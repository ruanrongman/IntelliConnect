import request from '@/utils/request'
import store from '@/store'

const token = store.getters['auth/token']
export const getProductData = (data) =>
  request({
    url: '/api/v2/ProductData',
    method: 'get',
    headers: {
        'Authorization': token
      }
  })
  export const postProductData = (data) =>
    request({
      url: '/api/v2/ProductData',
      method: 'post',
      headers: {
        'Authorization': token
      },
      data
    })
    export const deleteProductData = (params) =>
      request({
        url: '/api/v2/ProductData',
        method: 'delete',
        headers: {
          'Authorization': token
        },
        params
      })