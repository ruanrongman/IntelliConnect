export function normalizeBaseUrl(baseUrl = '') {
  return baseUrl.replace(/\/+$/, '')
}

export function joinUrl(baseUrl, path) {
  return `${normalizeBaseUrl(baseUrl)}/${path.replace(/^\/+/, '')}`
}
