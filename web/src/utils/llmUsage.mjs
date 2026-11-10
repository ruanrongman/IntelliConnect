// Formats the backend LlmUsageSummary (token usage / cache hits) as a compact footnote plus detail
// lines. Rates arrive as fractions in [0, 1].

export function formatTokens(value) {
  const number = Number(value)
  if (!Number.isFinite(number)) {
    return '0'
  }
  return Math.round(number).toLocaleString('en-US')
}

export function formatRate(value) {
  const number = Number(value)
  if (!Number.isFinite(number)) {
    return '0.0%'
  }
  return `${(number * 100).toFixed(1)}%`
}

function formatDuration(ms) {
  const number = Number(ms)
  if (!Number.isFinite(number) || number <= 0) {
    return ''
  }
  return number >= 1000 ? `${(number / 1000).toFixed(1)}s` : `${Math.round(number)}ms`
}

export function hasUsage(summary) {
  return Boolean(summary && Number(summary.calls) > 0)
}

/** One-line footnote, e.g. "输入 12,345 · 命中 8,000 (64.8%) · 输出 321 · 上下文 4.1% · 3 次调用". */
export function usageFootnote(summary) {
  if (!hasUsage(summary)) {
    return ''
  }
  const calls = Number(summary.calls) || 0
  const reported = Number(summary.reportedCalls) || 0
  if (reported === 0) {
    return `${calls} 次调用 · 服务商未返回用量`
  }
  const parts = [
    `输入 ${formatTokens(summary.promptTokens)}`,
    `命中 ${formatTokens(summary.cachedTokens)} (${formatRate(summary.cacheHitRate)})`,
  ]
  if (Number(summary.cacheCreationTokens) > 0) {
    parts.push(`建缓存 ${formatTokens(summary.cacheCreationTokens)}`)
  }
  parts.push(`输出 ${formatTokens(summary.completionTokens)}`)
  if (Number(summary.contextWindowTokens) > 0) {
    parts.push(`上下文 ${formatRate(summary.contextUsage)}`)
  }
  parts.push(`${calls} 次调用`)
  if (reported < calls) {
    parts.push(`${calls - reported} 次未返回用量`)
  }
  return parts.join(' · ')
}

/** Detail lines shown when the footnote is expanded. */
export function usageDetails(summary) {
  if (!hasUsage(summary) || !Number(summary.reportedCalls)) {
    return []
  }
  const lines = []
  const hitPercent = Math.round((Number(summary.cacheHitPriceRatio) || 0) * 100)
  lines.push(
    `等效计费输入 ≈ ${formatTokens(summary.effectiveInputTokens)} token（命中按 ${hitPercent}% 计，未命中 ${formatTokens(summary.uncachedPromptTokens)}）`
  )
  const minTokens = formatTokens(summary.minCacheableTokens)
  const shortCalls = (Number(summary.reportedCalls) || 0) - (Number(summary.eligibleCalls) || 0)
  let eligibleLine = `≥${minTokens} token 的调用 ${summary.eligibleCalls || 0} 次，命中率 ${formatRate(summary.eligibleCacheHitRate)}`
  if (shortCalls > 0) {
    eligibleLine += `；${shortCalls} 次低于门槛，不可能命中`
  }
  lines.push(eligibleLine)
  if (Number(summary.contextWindowTokens) > 0) {
    lines.push(
      `最大单次输入 ${formatTokens(summary.maxPromptTokens)} / 上下文窗口 ${formatTokens(summary.contextWindowTokens)}`
    )
  } else {
    lines.push(`最大单次输入 ${formatTokens(summary.maxPromptTokens)}`)
  }
  if (Number(summary.estimateRatio) > 0) {
    lines.push(`本地估算 / 实际输入 = ${Number(summary.estimateRatio).toFixed(2)}`)
  }
  if (Number(summary.reasoningTokens) > 0) {
    lines.push(`其中思考 ${formatTokens(summary.reasoningTokens)} token`)
  }
  const duration = formatDuration(summary.latencyMs)
  if (duration) {
    lines.push(`模型耗时合计 ${duration}`)
  }
  return lines
}

/** Per-scene rows, largest first (backend already sorts them). */
export function usageSceneLines(summary) {
  return (summary?.scenes || []).map((scene) => {
    const head = `${scene.scene} ×${scene.calls}`
    if (!Number(scene.reportedCalls)) {
      return `${head}：未返回用量`
    }
    return `${head}：输入 ${formatTokens(scene.promptTokens)}，命中 ${formatRate(scene.cacheHitRate)}，输出 ${formatTokens(scene.completionTokens)}`
  })
}

/** Tokens spent per merged knowledge-graph node or relation; empty if nothing was merged. */
export function tokensPerGraphItem(summary, nodeCount, relationCount) {
  const items = (Number(nodeCount) || 0) + (Number(relationCount) || 0)
  if (!hasUsage(summary) || items <= 0) {
    return ''
  }
  const tokens = (Number(summary.promptTokens) || 0) + (Number(summary.completionTokens) || 0)
  return `每个节点/关系约 ${formatTokens(tokens / items)} token`
}
