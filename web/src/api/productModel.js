import request from '@/utils/request'

export const getproductModel = (data) =>
  request({
    url: '/api/v2/ProductModel',
    method: 'get',
  })
  export const postProductModel = (data) =>
    request({
      url: '/api/v2/ProductModel',
      method: 'post',
    data
    })
    export const deleteProductModel = (params) =>
      request({
        url: '/api/v2/ProductModel',
        method: 'delete',
    params
      })