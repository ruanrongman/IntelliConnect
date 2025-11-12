import request from '@/utils/request'
import store from '@/store'

const token = store.getters['auth/token']
export const getProductToolsBan = (params) =>
  request({
    url: '/api/v2/productToolsBan',
    method: 'get',
    headers: {
        'Authorization': token
      },
    params
  })
  export const postProductToolsBan = (data) =>
    request({
      url: '/api/v2/productToolsBan',
      method: 'post',
      headers: {
        'Authorization': token
      },
      data
    })
  export const deleteProductToolsBan = (params) =>
      request({
        url: '/api/v2/productToolsBan',
        method: 'delete',
        headers: {
          'Authorization': token
        },
        params
    })