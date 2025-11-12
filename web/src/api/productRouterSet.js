import request from '@/utils/request'
import store from '@/store'

const token = store.getters['auth/token']
export const getProductRouterSet = (data) =>
  request({
    url: '/api/v2/productRouterSet',
    method: 'get',
    headers: {
        'Authorization': token
      }
  })
  export const postProductRouterSet = (data) =>
    request({
      url: '/api/v2/productRouterSet',
      method: 'post',
      headers: {
        'Authorization': token
      },
      data
    })
  export const deleteProductRouterSet = (params) =>
      request({
        url: '/api/v2/productRouterSet',
        method: 'delete',
        headers: {
          'Authorization': token
        },
        params
    })