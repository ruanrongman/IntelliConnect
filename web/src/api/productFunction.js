import request from '@/utils/request'

export const getProductFunction = (data) =>
  request({
    url: '/api/v2/ProductFunction',
    method: 'get',
  })
  export const postProductFunction = (data) =>
    request({
      url: '/api/v2/ProductFunction',
      method: 'post',
    data
    })
    export const deleteProductFunction = (params) =>
      request({
        url: '/api/v2/ProductFunction',
        method: 'delete',
    params
      })