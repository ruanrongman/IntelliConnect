import request from '@/utils/request'
import store from '@/store'

const token = store.getters['auth/token']
export const getproductModel = (data) =>
  request({
    url: '/api/v2/ProductModel',
    method: 'get',
    headers: {
        'Authorization': token
      }
  })
  export const postProductModel = (data) =>
    request({
      url: '/api/v2/ProductModel',
      method: 'post',
      headers: {
        'Authorization': token
      },
      data
    })
    export const deleteProductModel = (params) =>
      request({
        url: '/api/v2/ProductModel',
        method: 'delete',
        headers: {
          'Authorization': token
        },
        params
      })