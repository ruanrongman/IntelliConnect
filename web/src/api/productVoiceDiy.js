import request from '@/utils/request'

export const getProductVoiceDiy = (params) =>
  request({
    url: '/api/v2/productVoiceDiy',
    method: 'get',
    params
  })
  export const postProductVoiceDiy = (data) =>
    request({
      url: '/api/v2/productVoiceDiy',
      method: 'post',
    data
    })
  export const deleteProductVoiceDiy = (params) =>
      request({
        url: '/api/v2/productVoiceDiy',
        method: 'delete',
    params
    })