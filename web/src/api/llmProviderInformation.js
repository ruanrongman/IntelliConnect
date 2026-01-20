import request from '@/utils/request'
import store from '@/store'

const token = store.getters['auth/token']
export const getLlmProviderInformation = (data) =>
  request({
    url: '/api/v2/llmProviderInformation',
    method: 'get',
    headers: {
        'Authorization': token
      }
  })
  export const postLlmProviderInformation = (data) =>
    request({
      url: '/api/v2/llmProviderInformation',
      method: 'post',
      headers: {
        'Authorization': token
      },
      data
    })
  export const deleteLlmProviderInformation = (params) =>
      request({
        url: '/api/v2/llmProviderInformation',
        method: 'delete',
        headers: {
          'Authorization': token
        },
        params
    })