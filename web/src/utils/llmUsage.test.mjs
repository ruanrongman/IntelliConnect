import test from 'node:test'
import assert from 'node:assert/strict'
import {
  formatCompactTokens,
  tokensPerGraphItem,
  usageDetails,
  usageFootnote,
  usageSceneLines,
  usageStatus,
  addTurnUsage,
} from './llmUsage.mjs'

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

test('status bar chip and click card', () => {
  const status = usageStatus(summary)
  assert.equal(status.chip, '10.8K tok · 缓存命中 67%')
  assert.equal(status.total, '10,810 tok')
  assert.deepEqual(status.rows, [
    { label: '缓存命中', value: '67%' },
    { label: '未缓存输入', value: '3,500 tok' },
    { label: '缓存读取', value: '7,000 tok' },
    { label: '输出', value: '310 tok' },
  ])
  assert.deepEqual(status.context, { percent: 2, label: '2%', detail: '6,000 / 300,000 tok' })
})

test('status bar edge cases', () => {
  assert.equal(usageStatus(null), null)
  assert.equal(usageStatus({ calls: 0 }), null)
  const silent = usageStatus({ calls: 1, reportedCalls: 0 })
  assert.equal(silent.chip, '服务商未返回用量')
  assert.deepEqual(silent.rows, [])
  assert.equal(silent.context, null)
  assert.equal(usageStatus({ ...summary, contextWindowTokens: 0 }).context, null)
  assert.equal(usageStatus({ ...summary, contextUsage: 0.004 }).context.label, '<1%')
  const written = usageStatus({ ...summary, cacheCreationTokens: 512 })
  assert.deepEqual(written.rows[3], { label: '缓存写入', value: '512 tok' })
})

test('compact token counts', () => {
  assert.equal(formatCompactTokens(980), '980')
  assert.equal(formatCompactTokens(1000), '1K')
  assert.equal(formatCompactTokens(12345), '12.3K')
  assert.equal(formatCompactTokens(106099), '106K')
  assert.equal(formatCompactTokens(345678), '346K')
  assert.equal(formatCompactTokens(1250000), '1.3M')
  assert.equal(formatCompactTokens(undefined), '0')
})

test('turns add up like the backend sum', () => {
  const first = { ...summary, turns: 0 }
  const second = { calls: 1, reportedCalls: 1, promptTokens: 5000, cachedTokens: 4500, uncachedPromptTokens: 500, completionTokens: 90, maxPromptTokens: 5000, contextWindowTokens: 300000, contextUsage: 0.0167 }
  const one = addTurnUsage(null, first)
  assert.equal(one.turns, 1)
  assert.equal(usageStatus(one).scope, '整个会话 · 1 轮')
  const two = addTurnUsage(one, second)
  assert.equal(two.turns, 2)
  assert.equal(two.calls, 5)
  assert.equal(two.promptTokens, 15500)
  assert.equal(two.cachedTokens, 11500)
  assert.equal(two.cacheHitRate, 0.7419)
  assert.equal(two.maxPromptTokens, 5000)
  assert.equal(two.contextUsage, 0.0167)
  assert.equal(usageStatus(two).scope, '整个会话 · 2 轮')
  assert.equal(addTurnUsage(two, null), two)
  assert.equal(addTurnUsage(null, { calls: 0 }), null)
  // a total loaded from the backend already carries its turn count
  assert.equal(addTurnUsage({ ...two, turns: 7 }, second).turns, 8)
})
