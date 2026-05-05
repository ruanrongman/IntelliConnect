import request from '@/utils/request'

export const getProductRouterSet = (data) =>
  request({
    url: '/api/v2/productRouterSet',
    method: 'get',
  })
  export const postProductRouterSet = (data) =>
    request({
      url: '/api/v2/productRouterSet',
      method: 'post',
    data
    })
  export const deleteProductRouterSet = (params) =>
      request({
        url: '/api/v2/productRouterSet',
        method: 'delete',
    params
    })