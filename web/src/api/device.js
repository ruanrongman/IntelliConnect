import request from '@/utils/request'

export const getProductDevice = (data) =>
  request({
    url: '/api/v2/ProductDevice',
    method: 'get',
  })
  export const postProductDevice = (data) =>
    request({
      url: '/api/v2/ProductDevice',
      method: 'post',
    data
    })
    export const deleteProductDevice = (params) =>
      request({
        url: '/api/v2/ProductDevice',
        method: 'delete',
    params
      })