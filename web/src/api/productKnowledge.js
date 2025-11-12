import request from '@/utils/request'
import store from '@/store'

const token = store.getters['auth/token']
 export const getKnowledgeChat = (data) =>
  request({
    url: '/api/v2/knowledgeChat',
    method: 'get',
    headers: {
        'Authorization': token
      }
  })
  export const postKnowledgeChatRecall = (data) =>
  request({
    url: '/api/v2/knowledgeChatRecall',
    method: 'post',
    headers: {
        'Authorization': token
      },
    data
  })
  export const uploadKnowledge = (file, params) => {
  const formData = new FormData()
  formData.append('file', file)

  return request({
    url: '/api/v2/knowledgeChat',
    method: 'post',
    headers: {
      'Authorization': token,
    },
    params, 
    data: formData
  })
  }
  export const deleteKnowledgeChat = (params) =>
    request({
        url: '/api/v2/knowledgeChat',
        method: 'delete',
        headers: {
          'Authorization': token
        },
        params
    })