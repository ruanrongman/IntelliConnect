// A stream is successful only after the server's explicit completion event.
// Reading EOF alone can also mean that a proxy or the network dropped the connection.
export async function readAiEventStream(body, { onMessage, onReasoning, onComplete } = {}) {
  const reader = body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  try {
    while (true) {
      const { value, done } = await reader.read()
      if (done) {
        throw new Error('连接已中断，尚未收到完成结果。请稍后查看历史记录，确认任务进度。')
      }
      buffer += decoder.decode(value, { stream: true })
      const frames = buffer.split(/\r?\n\r?\n/)
      buffer = frames.pop() || ''
      for (const frame of frames) {
        const event = parseEvent(frame)
        if (event.name === 'complete') {
          onComplete?.(event.data)
          return
        }
        if (event.name === 'error') {
          throw new Error(parseError(event.data))
        }
        if (event.hasData && event.name === 'reasoning') {
          onReasoning?.(event.data)
        } else if (event.hasData && event.name === 'message') {
          onMessage?.(event.data)
        }
      }
    }
  } finally {
    await reader.cancel().catch(() => {})
    reader.releaseLock()
  }
}

function parseEvent(frame) {
  let name = 'message'
  const data = []
  frame.split(/\r?\n/).forEach((line) => {
    if (line.startsWith('event:')) {
      name = line.slice(6).trim() || 'message'
    } else if (line.startsWith('data:')) {
      // SseEmitter writes directly after the colon; preserve payload indentation.
      data.push(line.slice(5))
    }
  })
  return { name, data: data.join('\n'), hasData: data.length > 0 }
}

function parseError(data) {
  try {
    return JSON.parse(data).errorMsg || '请求失败'
  } catch {
    return data || '请求失败'
  }
}
