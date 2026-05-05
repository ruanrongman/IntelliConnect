import request from '@/utils/request'


// 获取产品技能列表
export const getProductSkill = () => {
  return request({
    url: '/api/v2/productSkills',
    method: 'get',
  })
}

// 添加产品技能（上传技能文件）
export const addProductSkill = (productId, file) => {
  const formData = new FormData()
  formData.append('file', file)

  return request({
    url: '/api/v2/productSkills',
    method: 'post',
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
    params: {
      id: id
    }
  })
}
