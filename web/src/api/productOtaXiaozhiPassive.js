import request from '@/utils/request'

export const getOtaXiaoZhiPassive = (data) =>
  request({
    url: '/api/v2/xiaozhi/otaPassive',
    method: 'get',
  })
  export const postOtaXiaoZhiPassive = (data) =>
    request({
      url: '/api/v2/xiaozhi/otaPassive',
      method: 'post',
    data
    })
  export const deleteOtaXiaoZhiPassive = (params) =>
      request({
        url: '/api/v2/xiaozhi/otaPassive',
        method: 'delete',
    params
    })