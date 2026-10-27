import request from '@/utils/request'

const base = '/api/v2/workspaces'

function unwrap(response) {
  if (response.data?.errorCode !== 200) {
    throw new Error(response.data?.errorMsg || '会话文件操作失败')
  }
  return response.data.data
}

export const listWorkspaces = (params) => request({ url: base, params }).then(unwrap)
export const listWorkspaceFiles = (params) => request({ url: `${base}/files`, params }).then(unwrap)
export const readWorkspaceFile = (fileId, params) =>
  request({ url: `${base}/files/${encodeURIComponent(fileId)}/text`, params }).then(unwrap)
export const deleteWorkspaceFile = (fileId, params) =>
  request({ url: `${base}/files/${encodeURIComponent(fileId)}`, method: 'delete', params }).then(unwrap)

export function uploadWorkspaceFiles(productId, chatId, files, onUploadProgress) {
  const data = new FormData()
  data.append('productId', productId)
  data.append('chatId', chatId)
  files.forEach((file) => data.append('file', file))
  return request({ url: `${base}/files`, method: 'post', data, onUploadProgress }).then(unwrap)
}

export async function downloadWorkspaceFile(file, params) {
  const response = await request({
    url: `${base}/files/${encodeURIComponent(file.id)}/download`,
    params,
    responseType: 'blob',
  })
  if (response.data.type?.includes('application/json')) {
    const error = JSON.parse(await response.data.text())
    throw new Error(error.errorMsg || '下载失败')
  }
  const url = URL.createObjectURL(response.data)
  const link = document.createElement('a')
  link.href = url
  link.download = file.name
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.setTimeout(() => URL.revokeObjectURL(url), 1000)
}

export async function workspaceError(error) {
  let data = error.response?.data
  if (data instanceof Blob) {
    try { data = JSON.parse(await data.text()) } catch { data = null }
  }
  return data?.errorMsg || error.message || '会话文件操作失败'
}
