import request from '@/utils/request'

export const getProductRole = (data) =>
  request({
    url: '/api/v2/productRole',
    method: 'get',
  })
  export const postProductRole = (data) =>
    request({
      url: '/api/v2/productRole',
      method: 'post',
    data
    })
   export const putProductRole = (data) =>
    request({
      url: '/api/v2/productRole',
      method: 'put',
    data
    })
  export const deleteProductRole = (params) =>
      request({
        url: '/api/v2/productRole',
        method: 'delete',
    params
    })