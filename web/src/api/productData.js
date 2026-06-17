import request from '@/utils/request'

export const getProductData = (params) =>
  request({
    url: '/api/v2/ProductData',
    method: 'get',
    params,
  })
  export const postProductData = (data) =>
    request({
      url: '/api/v2/ProductData',
      method: 'post',
    data
    })
    export const putProductData = (data) =>
      request({
        url: '/api/v2/ProductData',
        method: 'put',
    data
      })
    export const deleteProductData = (params) =>
      request({
        url: '/api/v2/ProductData',
        method: 'delete',
    params
      })
