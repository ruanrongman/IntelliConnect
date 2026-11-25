import request from '@/utils/request'

export const getRodakDevices = (productId) =>
  request({ url: '/api/v2/rodak/devices', method: 'get', params: { productId } })

export const getRodakPairingRequests = () =>
  request({ url: '/api/v2/rodak/pairing-requests', method: 'get' })

export const confirmRodakPairing = (productId, pairingCode, requestId) =>
  request({
    url: '/api/v2/rodak/pairing-requests/confirm',
    method: 'post',
    data: { productId, pairingCode, requestId },
  })

export const unbindRodakDevice = (id, productId) =>
  request({ url: `/api/v2/rodak/devices/${encodeURIComponent(id)}/unbind`, method: 'post', params: { productId } })

export const pingRodakDevice = (id, productId) =>
  request({
    url: `/api/v2/rodak/devices/${encodeURIComponent(id)}/ping`,
    method: 'post',
    params: { productId },
  })

export const getRodakCommand = (id, productId, commandNo) =>
  request({
    url: `/api/v2/rodak/devices/${encodeURIComponent(id)}/commands/${encodeURIComponent(commandNo)}`,
    method: 'get',
    params: { productId },
  })

export const setRodakVolume = (id, productId, volume) =>
  request({
    url: `/api/v2/rodak/devices/${encodeURIComponent(id)}/volume`,
    method: 'put',
    params: { productId },
    data: { volume },
  })
