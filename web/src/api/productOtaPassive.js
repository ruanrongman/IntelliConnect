import request from '@/utils/request'

export const getOtaPassive = (data) =>
  request({
    url: '/api/v2/otaPassive',
    method: 'get',
  })
  export const postOtaPassive = (data) =>
    request({
      url: '/api/v2/otaPassive',
      method: 'post',
    data
    })
  export const deleteOtaPassive = (params) =>
      request({
        url: '/api/v2/otaPassive',
        method: 'delete',
    params
    })