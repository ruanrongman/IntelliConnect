import request from '@/utils/request'
import store from '@/store'

const token = store.getters['auth/token']
export const getOtaPassive = (data) =>
  request({
    url: '/api/v2/otaPassive',
    method: 'get',
    headers: {
        'Authorization': token
      }
  })
  export const postOtaPassive = (data) =>
    request({
      url: '/api/v2/otaPassive',
      method: 'post',
      headers: {
        'Authorization': token
      },
      data
    })
  export const deleteOtaPassive = (params) =>
      request({
        url: '/api/v2/otaPassive',
        method: 'delete',
        headers: {
          'Authorization': token
        },
        params
    })