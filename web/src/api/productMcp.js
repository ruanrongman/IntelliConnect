import request from '@/utils/request'

export const getMcpServer = (data) =>
  request({
    url: '/api/v2/mcpServer',
    method: 'get',
  })
  export const postMcpServer = (data) =>
    request({
      url: '/api/v2/mcpServer',
      method: 'post',
    data
    })
    export const deleteMcpServer = (params) =>
      request({
        url: '/api/v2/mcpServer',
        method: 'delete',
    params
    })