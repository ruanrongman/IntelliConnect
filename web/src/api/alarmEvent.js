import request from '@/utils/request'

export const getAlarmEvent = (data) =>
  request({
    url: '/api/v2/alarmEvent',
    method: 'get',
  })
  export const postAlarmEvent = (data) =>
    request({
      url: '/api/v2/alarmEvent',
      method: 'post',
    data
    })
  export const deleteAlarmEvent = (params) =>
      request({
        url: '/api/v2/alarmEvent',
        method: 'delete',
    params
    })