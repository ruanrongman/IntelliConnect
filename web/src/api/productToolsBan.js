import request from '@/utils/request'

export const getProductToolsBan = (params) =>
  request({
    url: '/api/v2/productToolsBan',
    method: 'get',
    params
  })
  export const postProductToolsBan = (data) =>
    request({
      url: '/api/v2/productToolsBan',
      method: 'post',
    data
    })
  export const deleteProductToolsBan = (params) =>
      request({
        url: '/api/v2/productToolsBan',
        method: 'delete',
    params
    })