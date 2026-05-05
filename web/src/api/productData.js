import request from '@/utils/request'

export const getProductData = (data) =>
  request({
    url: '/api/v2/ProductData',
    method: 'get',
  })
  export const postProductData = (data) =>
    request({
      url: '/api/v2/ProductData',
      method: 'post',
    data
    })
    export const deleteProductData = (params) =>
      request({
        url: '/api/v2/ProductData',
        method: 'delete',
    params
      })