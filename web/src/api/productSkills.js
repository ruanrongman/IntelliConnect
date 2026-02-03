import request from '@/utils/request'
import store from '@/store'

const token = store.getters['auth/token']

// 获取产品技能列表
export const getProductSkill = () => {
  return request({
    url: '/api/v2/productSkills',
    method: 'get',
    headers: {
      'Authorization': token
    }
  })
}

// 添加产品技能（上传技能文件）
export const addProductSkill = (productId, file) => {
  const formData = new FormData()
  formData.append('file', file)

  return request({
    url: '/api/v2/productSkills',
    method: 'post',
    headers: {
      'Authorization': token
      // 不设置 Content-Type，让浏览器自动设置带有 boundary 的 multipart/form-data
    },
    params: {
      productId: productId
    },
    data: formData
  })
}

// 删除产品技能
export const deleteProductSkill = (id) => {
  return request({
    url: '/api/v2/productSkills',
    method: 'delete',
    headers: {
      'Authorization': token
    },
    params: {
      id: id
    }
  })
}