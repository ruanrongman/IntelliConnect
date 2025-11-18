import request from '@/utils/request'
import store from '@/store'

const token = store.getters['auth/token']
export const getLongMemory = (data) =>
  request({
    url: '/api/v2/longMemory',
    method: 'get',
    headers: {
        'Authorization': token
      }
  })
  export const postLongMemory = (data) =>
    request({
      url: '/api/v2/longMemory',
      method: 'post',
      headers: {
        'Authorization': token
      },
      data
    })
  export const deleteLongMemory = (params) =>
    request({
        url: '/api/v2/longMemory',
        method: 'delete',
        headers: {
          'Authorization': token
        },
        params
    })