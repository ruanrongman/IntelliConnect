import request from '@/utils/request'
import store from '@/store'

const token = store.getters['auth/token']
export const getAlarmEvent = (data) =>
  request({
    url: '/api/v2/alarmEvent',
    method: 'get',
    headers: {
        'Authorization': token
      }
  })
  export const postAlarmEvent = (data) =>
    request({
      url: '/api/v2/alarmEvent',
      method: 'post',
      headers: {
        'Authorization': token
      },
      data
    })
  export const deleteAlarmEvent = (params) =>
      request({
        url: '/api/v2/alarmEvent',
        method: 'delete',
        headers: {
          'Authorization': token
        },
        params
    })