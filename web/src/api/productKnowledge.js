import request from '@/utils/request'

export const getKnowledgeChat = (data) =>
  request({
    url: '/api/v2/knowledgeChat',
    method: 'get',
  })
export const postKnowledgeChatRecall = (data) =>
  request({
    url: '/api/v2/knowledgeChatRecall',
    method: 'post',
    data
  })
export const uploadKnowledge = (file, params) => {
  const formData = new FormData()
  formData.append('file', file)

  return request({
    url: '/api/v2/knowledgeChat',
    method: 'post',
    params,
    data: formData
  })
}
export const deleteKnowledgeChat = (params) =>
  request({
    url: '/api/v2/knowledgeChat',
    method: 'delete',
    params
  })
