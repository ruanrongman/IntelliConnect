import request from '@/utils/request'

export const getXiaoZhiManager = (data) =>
  request({
    url: '/api/v2/xiaozhi/otaManage',
    method: 'get',
  })
  export const postXiaoZhiManager = (data) =>
    request({
      url: '/api/v2/xiaozhi/otaManage',
      method: 'post',
    data
    })
  export const putXiaoZhiManager = (params) =>
        request({
            url: '/api/v2/xiaozhi/otaManage',
            method: 'put',
    params
        })
    export const deleteXiaoZhiManager = (params) =>
      request({
        url: '/api/v2/xiaozhi/otaManage',
        method: 'delete',
    params
      })
