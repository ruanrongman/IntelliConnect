import request from '@/utils/request'
import store from '@/store'

const token = store.getters['auth/token']
export const getMcpServer = (data) =>
  request({
    url: '/api/v2/mcpServer',
    method: 'get',
    headers: {
        'Authorization': token
      }
  })
  export const postMcpServer = (data) =>
    request({
      url: '/api/v2/mcpServer',
      method: 'post',
      headers: {
        'Authorization': token
      },
      data
    })
    export const deleteMcpServer = (params) =>
      request({
        url: '/api/v2/mcpServer',
        method: 'delete',
        headers: {
          'Authorization': token
        },
        params
    })