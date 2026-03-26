import request from '@/utils/request'
import store from '@/store'

const token = store.getters['auth/token']

export const getProductAsr = (params) =>
  request({
    url: '/api/v2/productAsr',
    method: 'get',
    headers: {
      'Authorization': token
    },
    params
  })

export const postProductAsr = (data) =>
  request({
    url: '/api/v2/productAsr',
    method: 'post',
    headers: {
      'Authorization': token
    },
    data
  })

export const putProductAsr = (data) =>
  request({
    url: '/api/v2/productAsr',
    method: 'put',
    headers: {
      'Authorization': token
    },
    data
  })

export const deleteProductAsr = (params) =>
  request({
    url: '/api/v2/productAsr',
    method: 'delete',
    headers: {
      'Authorization': token
    },
    params
  })
