import request from '@/utils/request'

export const getMcpPointUrl = (params) =>
  request({
    url: '/api/v2/mcpEndpoint',
    method: 'get',
    params
  })

export const getMcpPointTools = (params) =>
    request({
        url: '/api/v2/mcpEndpoint/tools',
        method: 'get',
    params
    })