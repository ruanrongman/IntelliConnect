import request from '@/utils/request'

export const getproductModel = (params) =>
  request({
    url: '/api/v2/ProductModel',
    method: 'get',
    params,
  })
  export const postProductModel = (data) =>
    request({
      url: '/api/v2/ProductModel',
      method: 'post',
    data
    })
    export const putProductModel = (data) =>
      request({
        url: '/api/v2/ProductModel',
        method: 'put',
    data
      })
    export const deleteProductModel = (params) =>
      request({
        url: '/api/v2/ProductModel',
        method: 'delete',
    params
      })
