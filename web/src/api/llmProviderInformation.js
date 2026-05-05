import request from '@/utils/request'

export const getLlmProviderInformation = (data) =>
  request({
    url: '/api/v2/llmProviderInformation',
    method: 'get',
  })
  export const postLlmProviderInformation = (data) =>
    request({
      url: '/api/v2/llmProviderInformation',
      method: 'post',
    data
    })
  export const deleteLlmProviderInformation = (params) =>
      request({
        url: '/api/v2/llmProviderInformation',
        method: 'delete',
    params
    })