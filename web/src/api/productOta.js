import request from '@/utils/request'

export const otaList = (data) =>
  request({
    url: '/api/v2/otaList',
    method: 'get',
  })
export const otaEnable = (params) =>
  request({
    url: 'api/v2/otaEnable',
    method: 'post',
    params
  })
export const uploadFirmware = (file, params) => {
  const formData = new FormData()
  formData.append('file', file)

  return request({
    url: '/api/v2/otaUpload',
    method: 'post',
    params,
    data: formData
  })
}
export const otaDelete = (params) =>
  request({
    url: '/api/v2/otaDelete',
    method: 'delete',
    params
  })
