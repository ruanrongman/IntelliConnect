import request from '@/utils/request'

export const getProductEventData = (data) =>
  request({
    url: '/api/v2/EventData',
    method: 'get',
  })
  export const postProductEventData = (data) =>
    request({
      url: '/api/v2/EventData',
      method: 'post',
    data
    })
  export const deleteProductEventData = (params) =>
      request({
        url: '/api/v2/EventData',
        method: 'delete',
    params
    })