import request from '@/utils/request'
import store from '@/store'

const token = store.getters['auth/token']
export const getAdminConfig = (data) =>
  request({
    url: '/api/v2/adminConfig',
    method: 'get',
    headers: {
        'Authorization': token
      }
  })
  export const postAdminConfig = (data) =>
    request({
      url: '/api/v2/adminConfig',
      method: 'post',
      headers: {
        'Authorization': token
      },
      data
    })
   export const putAdminConfig = (data) =>
    request({
      url: '/api/v2/adminConfig',
      method: 'put',
      headers: {
        'Authorization': token
      },
      data
    })
  export const deleteAdminConfig = (params) =>
      request({
        url: '/api/v2/adminConfig',
        method: 'delete',
        headers: {
          'Authorization': token
        },
        params
    })