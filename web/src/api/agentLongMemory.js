import request from '@/utils/request'

export const getLongMemory = (data) =>
  request({
    url: '/api/v2/longMemory',
    method: 'get',
  })
  export const postLongMemory = (data) =>
    request({
      url: '/api/v2/longMemory',
      method: 'post',
    data
    })
  export const deleteLongMemory = (params) =>
    request({
        url: '/api/v2/longMemory',
        method: 'delete',
    params
    })