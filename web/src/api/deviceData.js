import request from '@/utils/request'

export const getDeviceData = (data) =>
  request({
    url: '/api/v2/readData',
    method: 'post',
    data
  })
  