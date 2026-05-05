import request from '@/utils/request'

export const getAgentMemory = (data) =>
  request({
    url: '/api/v2/memory',
    method: 'get',
  })
export const getAgentMemoryByNickName = (params) =>
  request({
    url: '/api/v2/memoryByNickName',
    method: 'get',
    params
  })
  export const putAgentMemory = (data) =>
    request({
      url: '/api/v2/memory',
      method: 'put',
    data
    })
  export const deleteAgentMemory = (params) =>
      request({
        url: '/api/v2/memory',
        method: 'delete',
    params
    })