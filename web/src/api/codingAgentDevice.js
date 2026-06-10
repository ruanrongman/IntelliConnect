import request from '@/utils/request'

export const getCodingAgentDevice = () =>
  request({
    url: '/api/v2/codingAgentDevice',
    method: 'get',
  })

export const postCodingAgentDevice = (data) =>
  request({
    url: '/api/v2/codingAgentDevice',
    method: 'post',
    data,
  })

export const putCodingAgentDevice = (data) =>
  request({
    url: '/api/v2/codingAgentDevice',
    method: 'put',
    data,
  })

export const deleteCodingAgentDevice = (params) =>
  request({
    url: '/api/v2/codingAgentDevice',
    method: 'delete',
    params,
  })
