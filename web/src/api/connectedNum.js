import request from '@/utils/request'

export const  getConnectedNum = (data) =>
  request({
    url: '/api/v2/getConnectedNum',
    method: 'get',
  })
  