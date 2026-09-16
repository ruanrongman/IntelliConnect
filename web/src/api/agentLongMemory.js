import request from '@/utils/request'

export const getLongMemory = (data) =>
  request({
    url: '/api/v2/longMemory',
    method: 'get',
  })

export const getLongMemoryByProductId = (params) =>
  request({
    url: '/api/v2/longMemoryByProductId',
    method: 'get',
    params
  })

export const fastInitLongMemory = (data) =>
  request({
    url: '/api/v2/longMemoryFastInit',
    method: 'post',
    data
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
