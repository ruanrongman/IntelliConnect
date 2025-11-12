import request from '@/utils/request'
import store from '@/store'

const token = store.getters['auth/token']
 export const otaList = (data) =>
  request({
    url: '/api/v2/otaList',
    method: 'get',
    headers: {
        'Authorization': token
      }
  })
  export const otaEnable = (params) =>
  request({
    url: 'api/v2/otaEnable',
    method: 'post',
    headers: {
        'Authorization': token
      },
    params
  })
  export const uploadFirmware = (file, params) => {
  const formData = new FormData()
  formData.append('file', file)

  return request({
    url: '/api/v2/otaUpload',
    method: 'post',
    headers: {
      'Authorization': token,
    },
    params, 
    data: formData
  })
  }
  export const otaDelete = (params) =>
    request({
        url: '/api/v2/otaDelete',
        method: 'delete',
        headers: {
          'Authorization': token
        },
        params
    })