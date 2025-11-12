export const base64ToBlob = (base64, mimeType) => {
  let bytes = window.atob(base64.split(',')[1])
  let ab = new ArrayBuffer(bytes.length)
  let ia = new Uint8Array(ab)
  for (let i = 0; i < bytes.length; i++) {
    ia[i] = bytes.charCodeAt(i)
  }
  return new Blob([ab], { type: mimeType })
}
export const urlToBase64 = (url, mineType) => {
  return new Promise((resolve, reject) => {
    const canvas = document.createElement('canvas')
    const ctx = canvas.getContext('2d')
    const img = new Image()
    img.crossOrigin = 'anonymous'
    img.src = url
    img.onload = function () {
      if (!canvas || !ctx) {
        return reject('转化出错')
      }
      canvas.height = img.height
      canvas.width = img.width
      ctx.drawImage(img, 0, 0) // 后面两个参数是图片偏移量
      const dataUrl = canvas.toDataURL(mineType || 'image/png')
      resolve(dataUrl)
    }
  })
}
