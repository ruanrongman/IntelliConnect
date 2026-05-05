import request from '@/utils/request'

export const getProductEvent = (data) =>
  request({
    url: '/api/v2/ProductEvent',
    method: 'get',
  })
  export const postProductEvent = (data) =>
    request({
      url: '/api/v2/ProductEvent',
      method: 'post',
    data
    })
  export const deleteProductEvent = (params) =>
      request({
        url: '/api/v2/ProductEvent',
        method: 'delete',
    params
    })