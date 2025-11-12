import request from '@/utils/request'
import store from '@/store'

const token = store.getters['auth/token']
export const getXiaoZhiManager = (data) =>
  request({
    url: '/api/v2/xiaozhi/otaManage',
    method: 'get',
    headers: {
        'Authorization': token
      }
  })
  export const postXiaoZhiManager = (data) =>
    request({
      url: '/api/v2/xiaozhi/otaManage',
      method: 'post',
      headers: {
        'Authorization': token
      },
      data
    })
    export const deleteXiaoZhiManager = (params) =>
      request({
        url: '/api/v2/xiaozhi/otaManage',
        method: 'delete',
        headers: {
          'Authorization': token
        },
        params
      })