import request from '@/utils/request'

export const getAdminConfig = (data) =>
  request({
    url: '/api/v2/adminConfig',
    method: 'get',
  })
  export const postAdminConfig = (data) =>
    request({
      url: '/api/v2/adminConfig',
      method: 'post',
    data
    })
   export const putAdminConfig = (data) =>
    request({
      url: '/api/v2/adminConfig',
      method: 'put',
    data
    })
  export const deleteAdminConfig = (params) =>
      request({
        url: '/api/v2/adminConfig',
        method: 'delete',
    params
    })