import request from '@/utils/request'
import store from '@/store'

const token = store.getters['auth/token']
export const getMcpPointUrl = (params) =>
  request({
    url: '/api/v2/mcpEndpoint',
    method: 'get',
    headers: {
        'Authorization': token
      },
    params
  })

export const getMcpPointTools = (params) =>
    request({
        url: '/api/v2/mcpEndpoint/tools',
        method: 'get',
        headers: {
          'Authorization': token
        },
        params
    })