import request from '@/utils/request'
import store from '@/store'

const token = store.getters['auth/token']
export const getProductRole = (data) =>
  request({
    url: '/api/v2/productRole',
    method: 'get',
    headers: {
        'Authorization': token
      }
  })
  export const postProductRole = (data) =>
    request({
      url: '/api/v2/productRole',
      method: 'post',
      headers: {
        'Authorization': token
      },
      data
    })
   export const putProductRole = (data) =>
    request({
      url: '/api/v2/productRole',
      method: 'put',
      headers: {
        'Authorization': token
      },
      data
    })
  export const deleteProductRole = (params) =>
      request({
        url: '/api/v2/productRole',
        method: 'delete',
        headers: {
          'Authorization': token
        },
        params
    })