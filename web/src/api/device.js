import request from '@/utils/request'

export const getProductDevice = (params) =>
  request({
    url: '/api/v2/ProductDevice',
    method: 'get',
    params,
  })
  export const postProductDevice = (data) =>
    request({
      url: '/api/v2/ProductDevice',
      method: 'post',
    data
    })
    export const putProductDevice = (data) =>
      request({
        url: '/api/v2/ProductDevice',
        method: 'put',
    data
      })
    export const deleteProductDevice = (params) =>
      request({
        url: '/api/v2/ProductDevice',
        method: 'delete',
    params
      })
