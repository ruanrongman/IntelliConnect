import request from '@/utils/request'
import store from '@/store'

const token = store.getters['auth/token']
export const  getConnectedNum = (data) =>
  request({
    url: '/api/v2/getConnectedNum',
    method: 'get',
    headers: {
        'Authorization': token
      }
  })
  