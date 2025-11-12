import request from '@/utils/request'

export const loginIn = (data) =>
  request({
    url: '/api/v2/login',
    method: 'post',
    data
  })

export const register = (data) =>
  request({
    url: '/api/v2/newUser',
    method: 'post',
    data
  })
export const forgetPassword = (data) =>
  request({
    url: '/api/v2/forgotPassword',
    method: 'post',
    data
  })

export const getEmailCode = (data) =>
  request({
    url: '/api/v2/getUserCode',
    method: 'post',
    data
  })
