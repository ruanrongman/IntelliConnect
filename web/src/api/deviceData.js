import request from '@/utils/request'
import store from '@/store'

const token = store.getters['auth/token']
export const getDeviceData = (data) =>
  request({
    url: '/api/v2/readData',
    method: 'post',
    headers: {
        'Authorization': token
      },
    data
  })
  