import request from '@/utils/request'


export const getProductAsr = (params) =>
  request({
    url: '/api/v2/productAsr',
    method: 'get',
    params
  })

export const postProductAsr = (data) =>
  request({
    url: '/api/v2/productAsr',
    method: 'post',
    data
  })

export const putProductAsr = (data) =>
  request({
    url: '/api/v2/productAsr',
    method: 'put',
    data
  })

export const deleteProductAsr = (params) =>
  request({
    url: '/api/v2/productAsr',
    method: 'delete',
    params
  })
