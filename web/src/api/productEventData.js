import request from '@/utils/request'

export const getProductEventData = (params) =>
  request({
    url: '/api/v2/EventData',
    method: 'get',
    params,
  })
  export const postProductEventData = (data) =>
    request({
      url: '/api/v2/EventData',
      method: 'post',
    data
    })
  export const putProductEventData = (data) =>
      request({
        url: '/api/v2/EventData',
        method: 'put',
    data
    })
  export const deleteProductEventData = (params) =>
      request({
        url: '/api/v2/EventData',
        method: 'delete',
    params
    })
