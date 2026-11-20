import request from '@/utils/request'

export const getLlmUsageTrend = (params) =>
  request({
    url: '/api/v2/llm/usage/trend',
    method: 'get',
    params,
  })
