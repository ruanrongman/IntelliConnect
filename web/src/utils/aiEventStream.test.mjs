import test from 'node:test'
import assert from 'node:assert/strict'
import { readAiEventStream } from './aiEventStream.mjs'

function stream(text, chunkSize = 7) {
  const bytes = new TextEncoder().encode(text)
  return new ReadableStream({
    start(controller) {
      for (let offset = 0; offset < bytes.length; offset += chunkSize) {
        controller.enqueue(bytes.slice(offset, offset + chunkSize))
      }
      controller.close()
    },
  })
}

test('handles fragmented UTF-8, CRLF, reasoning and heartbeats without exposing heartbeats', async () => {
  const messages = []
  const reasoning = []
  const completions = []
  await readAiEventStream(
    stream(
      ':connected\r\n\r\n:heartbeat\r\n\r\n' +
        'event:reasoning\r\ndata:思考中\r\n\r\n' +
        'event:message\r\ndata:  第一行\r\ndata:第二行😀\r\n\r\n' +
        'event:complete\r\ndata:最终结果\r\n\r\n',
      1
    ),
    {
      onMessage: (value) => messages.push(value),
      onReasoning: (value) => reasoning.push(value),
      onComplete: (value) => completions.push(value),
    }
  )
  assert.deepEqual(messages, ['  第一行\n第二行😀'])
  assert.deepEqual(reasoning, ['思考中'])
  assert.deepEqual(completions, ['最终结果'])
})

test('server error rejects instead of reporting successful completion', async () => {
  let completed = false
  await assert.rejects(
    readAiEventStream(
      stream(
        'event:error\ndata:{"errorMsg":"附件解析失败"}\n\n' +
          'event:complete\ndata:错误的成功结果\n\n'
      ),
      {
        onComplete: () => {
          completed = true
        },
      }
    ),
    /附件解析失败/
  )
  assert.equal(completed, false)
})

test('EOF after a partial response is a disconnect, preserving received content', async () => {
  const messages = []
  await assert.rejects(
    readAiEventStream(stream('event:message\ndata:已收到的内容\n\n'), {
      onMessage: (value) => messages.push(value),
    }),
    /连接已中断/
  )
  assert.deepEqual(messages, ['已收到的内容'])
})

test('does not promote a truncated final frame to completion', async () => {
  await assert.rejects(readAiEventStream(stream('event:complete\ndata:截断')), /连接已中断/)
})

test('finishes and releases the connection on complete without waiting for TCP EOF', async () => {
  let cancelled = false
  let completion
  const body = new ReadableStream({
    start(controller) {
      controller.enqueue(new TextEncoder().encode('event:complete\ndata:完成\n\n'))
      // Deliberately leave the connection open, as a buffering proxy might do.
    },
    cancel() {
      cancelled = true
    },
  })
  await readAiEventStream(body, {
    onComplete: (value) => {
      completion = value
    },
  })
  assert.equal(completion, '完成')
  assert.equal(cancelled, true)
  assert.equal(body.locked, false)
})

test('recognizes an empty completion payload', async () => {
  let completion
  await readAiEventStream(stream('event:complete\ndata:\n\n'), {
    onComplete: (value) => {
      completion = value
    },
  })
  assert.equal(completion, '')
})

test('preserves a user abort instead of converting it to a network failure', async () => {
  const abort = new DOMException('Stopped', 'AbortError')
  await assert.rejects(
    readAiEventStream(
      new ReadableStream({
        start(controller) {
          controller.error(abort)
        },
      })
    ),
    (error) => error === abort
  )
})
