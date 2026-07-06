import request from '@/utils/request'

export const getUserConfigByName = (name, params) =>
  request({
    url: `/api/v2/user/config/${name}`,
    method: 'get',
    params
  })

export const updateUserConfig = (data) =>
  request({
    url: '/api/v2/user/config',
    method: 'put',
    data
  })
