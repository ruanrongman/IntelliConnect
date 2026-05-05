import request from '@/utils/request'


export const getTimeSchedule = () =>
  request({
    url: '/api/v2/timeSchedule',
    method: 'get',
  })

export const postTimeSchedule = (data) =>
  request({
    url: '/api/v2/timeSchedule',
    method: 'post',
    data
  })

export const putTimeSchedule = (data) =>
  request({
    url: '/api/v2/timeSchedule',
    method: 'put',
    data
  })

export const deleteTimeSchedule = (params) =>
  request({
    url: '/api/v2/timeSchedule',
    method: 'delete',
    params
  })
