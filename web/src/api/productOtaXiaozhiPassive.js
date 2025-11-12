import request from '@/utils/request'
import store from '@/store'

const token = store.getters['auth/token']
export const getOtaXiaoZhiPassive = (data) =>
  request({
    url: '/api/v2/xiaozhi/otaPassive',
    method: 'get',
    headers: {
        'Authorization': token
      }
  })
  export const postOtaXiaoZhiPassive = (data) =>
    request({
      url: '/api/v2/xiaozhi/otaPassive',
      method: 'post',
      headers: {
        'Authorization': token
      },
      data
    })
  export const deleteOtaXiaoZhiPassive = (params) =>
      request({
        url: '/api/v2/xiaozhi/otaPassive',
        method: 'delete',
        headers: {
          'Authorization': token
        },
        params
    })