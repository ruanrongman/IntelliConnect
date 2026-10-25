import store from '@/store'
import { joinUrl } from '@/utils/url'
import { readAiEventStream } from '@/utils/aiEventStream.mjs'

const { VITE_BASE_URL } = import.meta.env

export const streamAiControl = async ({
  productId,
  chatId,
  streamId,
  content,
  files = [],
  signal,
  onMessage,
  onReasoning,
  onComplete,
}) => {
  const headers = {
    Authorization: store.getters['auth/token'],
    Accept: 'text/event-stream',
  }
  const body = new FormData()
  body.append('productId', productId)
  body.append('streamId', streamId)
  body.append('content', content)
  if (chatId) {
    body.append('chatId', chatId)
  }
  files.forEach((file) => {
    body.append('file', file)
  })
  const response = await fetch(joinUrl(VITE_BASE_URL, '/api/v2/aiControl/stream'), {
    method: 'POST',
    headers,
    body,
    signal,
  })
  if (!response.ok || !response.body) {
    throw new Error(`请求失败：${response.status}`)
  }
  if (!response.headers.get('content-type')?.includes('text/event-stream')) {
    const payload = await response.json().catch(() => null)
    throw new Error(payload?.errorMsg || '未收到流式响应，请检查登录状态后重试。')
  }
  await readAiEventStream(response.body, { onMessage, onReasoning, onComplete })
}

export const stopAiControlStream = async (productId, streamId) => {
  const params = new URLSearchParams({
    productId: String(productId),
    streamId,
  })
  const response = await fetch(
    `${joinUrl(VITE_BASE_URL, '/api/v2/aiControl/stream/stop')}?${params.toString()}`,
    {
      method: 'POST',
      headers: {
        Authorization: store.getters['auth/token'],
      },
    }
  )
  if (!response.ok) {
    throw new Error(`停止失败：${response.status}`)
  }
  return response.json()
}
