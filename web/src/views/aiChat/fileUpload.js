export const acceptedFileExtensions = ['.pdf', '.txt', '.md', '.markdown', '.doc', '.docx', '.ppt', '.pptx', '.xls', '.xlsx']
export const acceptedFileLabel = 'PDF/TXT/Markdown/Word/PPT/Excel'

export function getClipboardFiles(event) {
  const clipboardData = event.clipboardData
  if (!clipboardData) {
    return []
  }
  const files = []
  Array.from(clipboardData.items || []).forEach((item) => {
    if (item.kind === 'file') {
      const file = item.getAsFile()
      if (file) {
        files.push(file)
      }
    }
  })
  Array.from(clipboardData.files || []).forEach((file) => files.push(file))
  const fileMap = new Map()
  files.forEach((file) => fileMap.set(getFileIdentity(file), file))
  return Array.from(fileMap.values())
}

export function isAcceptedFile(file) {
  const extension = getFileExtension(file.name)
  return Boolean(extension && acceptedFileExtensions.includes(extension))
}

export function getFileIdentity(file) {
  return `${file.name || 'file'}-${file.size || 0}-${file.lastModified || 0}`
}

function getFileExtension(fileName = '') {
  const dotIndex = fileName.lastIndexOf('.')
  return dotIndex >= 0 ? fileName.slice(dotIndex).toLowerCase() : ''
}
