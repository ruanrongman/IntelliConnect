import request from '@/utils/request'

export const getMachineMessage = (data) =>
  request({
    url: '/api/v2/machineMessage',
    method: 'get',
  })
