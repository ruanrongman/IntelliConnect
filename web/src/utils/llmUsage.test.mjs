import test from 'node:test'
import assert from 'node:assert/strict'
import { tokensPerGraphItem, usageDetails, usageFootnote, usageSceneLines } from './llmUsage.mjs'

const summary = {
  calls: 4,
  reportedCalls: 3,
  promptTokens: 10500,
  cachedTokens: 7000,
  cacheCreationTokens: 0,
  uncachedPromptTokens: 3500,
  completionTokens: 310,
  reasoningTokens: 0,
  cacheHitRate: 0.6667,
  eligibleCalls: 2,
  eligibleCacheHitRate: 0.7,
  maxPromptTokens: 6000,
  contextWindowTokens: 300000,
  contextUsage: 0.02,
  estimateRatio: 0.9,
  effectiveInputTokens: 4900,
  cacheHitPriceRatio: 0.2,
  minCacheableTokens: 1024,
  latencyMs: 3200,
  scenes: [
    { scene: 'ChatTool.run', calls: 2, reportedCalls: 2, promptTokens: 10000, cacheHitRate: 0.7, completionTokens: 300 },
    { scene: 'ClassifierTool.run', calls: 1, reportedCalls: 0 },
  ],
}

test('footnote shows input, hits, output, context and missing usage', () => {
  assert.equal(
    usageFootnote(summary),
    '输入 10,500 · 命中 7,000 (66.7%) · 输出 310 · 上下文 2.0% · 4 次调用 · 1 次未返回用量'
  )
})

test('footnote handles providers that report nothing', () => {
  assert.equal(usageFootnote({ calls: 2, reportedCalls: 0 }), '2 次调用 · 服务商未返回用量')
  assert.equal(usageFootnote(null), '')
  assert.deepEqual(usageDetails({ calls: 2, reportedCalls: 0 }), [])
})

test('details explain effective cost, threshold and estimate', () => {
  const lines = usageDetails(summary)
  assert.equal(lines[0], '等效计费输入 ≈ 4,900 token（命中按 20% 计，未命中 3,500）')
  assert.equal(lines[1], '≥1,024 token 的调用 2 次，命中率 70.0%；1 次低于门槛，不可能命中')
  assert.ok(lines.includes('本地估算 / 实际输入 = 0.90'))
  assert.ok(lines.includes('模型耗时合计 3.2s'))
})

test('scene lines and graph efficiency', () => {
  assert.deepEqual(usageSceneLines(summary), [
    'ChatTool.run ×2：输入 10,000，命中 70.0%，输出 300',
    'ClassifierTool.run ×1：未返回用量',
  ])
  assert.equal(tokensPerGraphItem(summary, 20, 1), '每个节点/关系约 515 token')
  assert.equal(tokensPerGraphItem(summary, 0, 0), '')
})
