import request from '@/utils/request'
import store from '@/store'

const token = store.getters['auth/token']

export const getTimeSchedule = () =>
  request({
    url: '/api/v2/timeSchedule',
    method: 'get',
    headers: {
      'Authorization': token
    }
  })

export const postTimeSchedule = (data) =>
  request({
    url: '/api/v2/timeSchedule',
    method: 'post',
    headers: {
      'Authorization': token
    },
    data
  })

export const putTimeSchedule = (data) =>
  request({
    url: '/api/v2/timeSchedule',
    method: 'put',
    headers: {
      'Authorization': token
    },
    data
  })

export const deleteTimeSchedule = (params) =>
  request({
    url: '/api/v2/timeSchedule',
    method: 'delete',
    headers: {
      'Authorization': token
    },
    params
  })
