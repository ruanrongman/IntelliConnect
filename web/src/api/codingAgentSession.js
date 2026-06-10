import request from '@/utils/request'

export const getCodingAgentSession = () =>
  request({
    url: '/api/v2/codingAgentSession',
    method: 'get',
  })

export const postCodingAgentSession = (data) =>
  request({
    url: '/api/v2/codingAgentSession',
    method: 'post',
    data,
  })

export const putCodingAgentSession = (data) =>
  request({
    url: '/api/v2/codingAgentSession',
    method: 'put',
    data,
  })

export const deleteCodingAgentSession = (params) =>
  request({
    url: '/api/v2/codingAgentSession',
    method: 'delete',
    params,
  })
