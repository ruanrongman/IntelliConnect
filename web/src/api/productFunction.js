import request from '@/utils/request'

export const getProductFunction = (params) =>
  request({
    url: '/api/v2/ProductFunction',
    method: 'get',
    params,
  })
  export const postProductFunction = (data) =>
    request({
      url: '/api/v2/ProductFunction',
      method: 'post',
    data
    })
    export const putProductFunction = (data) =>
      request({
        url: '/api/v2/ProductFunction',
        method: 'put',
    data
      })
    export const deleteProductFunction = (params) =>
      request({
        url: '/api/v2/ProductFunction',
        method: 'delete',
    params
      })
