import request from '@/utils/request'
import store from '@/store'

const token = store.getters['auth/token']
export const getProductEventData = (data) =>
  request({
    url: '/api/v2/EventData',
    method: 'get',
    headers: {
        'Authorization': token
      }
  })
  export const postProductEventData = (data) =>
    request({
      url: '/api/v2/EventData',
      method: 'post',
      headers: {
        'Authorization': token
      },
      data
    })
  export const deleteProductEventData = (params) =>
      request({
        url: '/api/v2/EventData',
        method: 'delete',
        headers: {
          'Authorization': token
        },
        params
    })