import request from '@/utils/request'

export const getAlarmEvent = (params) =>
  request({
    url: '/api/v2/alarmEvent',
    method: 'get',
    params,
  })
  export const postAlarmEvent = (data) =>
    request({
      url: '/api/v2/alarmEvent',
      method: 'post',
    data
    })
  export const putAlarmEvent = (data) =>
      request({
        url: '/api/v2/alarmEvent',
        method: 'put',
    data
    })
  export const deleteAlarmEvent = (params) =>
      request({
        url: '/api/v2/alarmEvent',
        method: 'delete',
    params
    })
