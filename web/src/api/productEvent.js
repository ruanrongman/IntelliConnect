import request from '@/utils/request'

export const getProductEvent = (params) =>
  request({
    url: '/api/v2/ProductEvent',
    method: 'get',
    params,
  })
  export const postProductEvent = (data) =>
    request({
      url: '/api/v2/ProductEvent',
      method: 'post',
    data
    })
  export const putProductEvent = (data) =>
      request({
        url: '/api/v2/ProductEvent',
        method: 'put',
    data
    })
  export const deleteProductEvent = (params) =>
      request({
        url: '/api/v2/ProductEvent',
        method: 'delete',
    params
    })
