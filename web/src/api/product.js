import request from '@/utils/request'

export const getProductName = (params) =>
  request({
    url: '/api/v2/getProductName',
    method: 'get',
    params
  })
export const getProduct = (data) =>
  request({
    url: '/api/v2/Product',
    method: 'get',
  })
  export const postProduct = (data) =>
    request({
      url: '/api/v2/Product',
      method: 'post',
    data
    })
    export const deleteProduct = (params) =>
      request({
        url: '/api/v2/Product',
        method: 'delete',
    params
      })