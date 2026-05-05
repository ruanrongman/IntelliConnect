import request from '@/utils/request'

  export const postProductBind = (data) =>
    request({
      url: '/api/v2/userProductBind',
      method: 'post',
    data
    })
  export const postProductUnbind = (data) =>
    request({
      url: '/api/v2/userProductUnbind',
      method: 'post',
    data
    })
   