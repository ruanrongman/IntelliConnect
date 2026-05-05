import request from '@/utils/request'

export const getProductLlmModel = (data) =>
  request({
    url: '/api/v2/productLlmModel',
    method: 'get',
  })
export const getProductLlmModelByProductId = (params) =>
  request({
    url: '/api/v2/productLlmModelByProductId',
    method: 'get',
    params
  })
  export const postProductLlmModel = (data) =>
    request({
      url: '/api/v2/productLlmModel',
      method: 'post',
    data
    })
  export const deleteProductLlmModel = (params) =>
      request({
        url: '/api/v2/productLlmModel',
        method: 'delete',
    params
    })