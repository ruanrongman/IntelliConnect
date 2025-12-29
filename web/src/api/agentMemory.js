import request from '@/utils/request'
import store from '@/store'

const token = store.getters['auth/token']
export const getAgentMemory = (data) =>
  request({
    url: '/api/v2/memory',
    method: 'get',
    headers: {
        'Authorization': token
      }
  })
  export const putAgentMemory = (data) =>
    request({
      url: '/api/v2/memory',
      method: 'put',
      headers: {
        'Authorization': token
      },
      data
    })
  export const deleteAgentMemory = (params) =>
      request({
        url: '/api/v2/memory',
        method: 'delete',
        headers: {
          'Authorization': token
        },
        params
    })