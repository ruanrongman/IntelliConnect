import store from '@/store'

const { VITE_BASE_URL } = import.meta.env
const DONE_SEPARATOR = /\r?\n\r?\n/

export const streamAiControl = async ({ productId, content, signal, onMessage, onError }) => {
  const response = await fetch(`${VITE_BASE_URL}/api/v2/aiControl/stream`, {
    method: 'POST',
    headers: {
      Authorization: store.getters['auth/token'],
      Accept: 'text/event-stream',
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ productId, content }),
    signal,
  })
  if (!response.ok || !response.body) {
    throw new Error(`请求失败：${response.status}`)
  }
  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  while (true) {
    const { value, done } = await reader.read()
    if (done) {
      break
    }
    buffer += decoder.decode(value, { stream: true })
    buffer = consumeSseBuffer(buffer, onMessage, onError)
  }
  buffer += decoder.decode()
  if (buffer.trim()) {
    consumeSseBuffer(`${buffer}\n\n`, onMessage, onError)
  }
}

export const stopAiControlStream = async (productId) => {
  const response = await fetch(`${VITE_BASE_URL}/api/v2/aiControl/stream/stop?productId=${productId}`, {
    method: 'POST',
    headers: {
      Authorization: store.getters['auth/token'],
    },
  })
  if (!response.ok) {
    throw new Error(`停止失败：${response.status}`)
  }
  return response.json()
}

function consumeSseBuffer(buffer, onMessage, onError) {
  const parts = buffer.split(DONE_SEPARATOR)
  const rest = parts.pop() || ''
  parts.forEach((part) => {
    const event = parseSseEvent(part)
    if (!event.data) {
      return
    }
    if (event.name === 'error') {
      onError?.(parseErrorMessage(event.data))
    } else {
      onMessage?.(event.data)
    }
  })
  return rest
}

function parseSseEvent(raw) {
  const lines = raw.split(/\r?\n/)
  let name = 'message'
  const data = []
  lines.forEach((line) => {
    if (line.startsWith('event:')) {
      name = line.slice(6).trim() || 'message'
    } else if (line.startsWith('data:')) {
      data.push(line.slice(5).replace(/^ /, ''))
    }
  })
  return {
    name,
    data: data.join('\n'),
  }
}

function parseErrorMessage(data) {
  try {
    const payload = JSON.parse(data)
    return payload.errorMsg || '请求失败'
  } catch (error) {
    return data || '请求失败'
  }
}
