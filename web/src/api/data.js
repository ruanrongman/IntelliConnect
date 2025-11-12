import request from '@/utils/request'

export const queryTable = (params) =>
  request({
    url: '/queryTable',
    method: 'get',
    params,
  })
