<template>
  <div ref="previewRef" class="markdown-content vditor-reset"></div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import Vditor from 'vditor/dist/method.min'
import 'vditor/dist/index.css'

const props = defineProps({
  content: {
    type: String,
    default: '',
  },
})

const previewRef = ref(null)
const vditorCdn = `${window.location.origin}${import.meta.env.BASE_URL.replace(/\/$/, '')}/vditor`
let renderTimer = 0
let renderVersion = 0

onMounted(() => {
  scheduleRender(0)
})

onBeforeUnmount(() => {
  if (renderTimer) {
    window.clearTimeout(renderTimer)
  }
})

watch(
  () => props.content,
  () => scheduleRender(80)
)

function scheduleRender(delay) {
  if (renderTimer) {
    window.clearTimeout(renderTimer)
  }
  renderTimer = window.setTimeout(() => {
    renderTimer = 0
    renderMarkdown()
  }, delay)
}

async function renderMarkdown() {
  const element = previewRef.value
  if (!element) {
    return
  }
  const currentVersion = ++renderVersion
  const markdown = normalizeMarkdownContent(props.content || '')
  try {
    await Vditor.preview(element, markdown, {
      mode: 'light',
      cdn: vditorCdn,
      anchor: 0,
      customEmoji: {},
      emojiPath: '',
      lang: 'zh_CN',
      hljs: {
        enable: true,
        lineNumber: false,
        style: 'atom-one-dark',
      },
      math: {
        engine: 'KaTeX',
        inlineDigit: true,
      },
      markdown: {
        autoSpace: true,
        fixTermTypo: false,
        footnotes: true,
        listStyle: true,
        mathBlockPreview: true,
        paragraphBeginningSpace: true,
        toc: true,
      },
      render: {
        media: {
          enable: false,
        },
      },
      after: async () => {
        if (currentVersion !== renderVersion) {
          return
        }
        restoreEmojiImages(element)
        await nextTick()
      },
    })
  } catch (error) {
    element.textContent = markdown
  }
}

function normalizeMarkdownContent(markdown) {
  return splitCodeFences(markdown)
    .map((segment) => (segment.code ? segment.content : normalizeMathText(segment.content)))
    .join('')
}

function splitCodeFences(markdown) {
  const segments = []
  const fencePattern = /(```[\s\S]*?(?:```|$)|~~~[\s\S]*?(?:~~~|$))/g
  let lastIndex = 0
  let match
  while ((match = fencePattern.exec(markdown)) !== null) {
    if (match.index > lastIndex) {
      segments.push({ code: false, content: markdown.slice(lastIndex, match.index) })
    }
    segments.push({ code: true, content: match[0] })
    lastIndex = fencePattern.lastIndex
  }
  if (lastIndex < markdown.length) {
    segments.push({ code: false, content: markdown.slice(lastIndex) })
  }
  return segments.length ? segments : [{ code: false, content: markdown }]
}

function normalizeMathText(text) {
  return normalizeLooseParenthesizedMath(
    text
      .replace(/\\\[([\s\S]*?)\\\]/g, (_, formula) => `$$${formula}$$`)
      .replace(/\\\(([\s\S]*?)\\\)/g, (_, formula) => `$${formula}$`)
  )
}

function normalizeLooseParenthesizedMath(text) {
  let result = ''
  let index = 0
  while (index < text.length) {
    const char = text[index]
    if (char === '`' || char === '$') {
      const skipped = readDelimitedText(text, index, char)
      result += skipped.value
      index = skipped.nextIndex
      continue
    }
    if (char !== '(' || text[index - 1] === ']') {
      result += char
      index += 1
      continue
    }
    const closeIndex = findMatchingParenthesis(text, index)
    if (closeIndex === -1) {
      result += char
      index += 1
      continue
    }
    const inner = text.slice(index + 1, closeIndex)
    if (isInlineLatexExpression(inner)) {
      result += `$${inner.trim()}$`
    } else {
      result += text.slice(index, closeIndex + 1)
    }
    index = closeIndex + 1
  }
  return result
}

function readDelimitedText(text, startIndex, delimiter) {
  const markerLength = delimiter === '$' && text[startIndex + 1] === '$' ? 2 : 1
  const marker = delimiter.repeat(markerLength)
  let index = startIndex + markerLength
  while (index < text.length) {
    if (text[index] === '\\') {
      index += 2
      continue
    }
    if (text.slice(index, index + markerLength) === marker) {
      return {
        value: text.slice(startIndex, index + markerLength),
        nextIndex: index + markerLength,
      }
    }
    index += 1
  }
  return {
    value: text.slice(startIndex),
    nextIndex: text.length,
  }
}

function findMatchingParenthesis(text, openIndex) {
  let depth = 0
  for (let index = openIndex; index < text.length; index += 1) {
    if (text[index] === '\\') {
      index += 1
      continue
    }
    if (text[index] === '(') {
      depth += 1
    } else if (text[index] === ')') {
      depth -= 1
      if (depth === 0) {
        return index
      }
    }
  }
  return -1
}

function isInlineLatexExpression(value) {
  const expression = value.trim()
  return (
    expression.length > 1 &&
    expression.length <= 300 &&
    !expression.includes('\n') &&
    /\\[a-zA-Z]+/.test(expression)
  )
}

function restoreEmojiImages(element) {
  element.querySelectorAll('img.emoji').forEach((emojiImage) => {
    const fallbackText =
      emojiImage.getAttribute('alt') || emojiImage.getAttribute('title') || emojiImage.getAttribute('aria-label')
    if (fallbackText) {
      emojiImage.replaceWith(document.createTextNode(fallbackText))
    }
  })
}
</script>

<style lang="scss" scoped>
.markdown-content {
  max-width: 100%;
  overflow-wrap: anywhere;
  color: inherit;
  font-size: 14px;
  line-height: 1.72;

  :deep(*) {
    letter-spacing: 0;
  }

  :deep(p) {
    margin: 0 0 10px;
  }

  :deep(p:last-child) {
    margin-bottom: 0;
  }

  :deep(h1),
  :deep(h2),
  :deep(h3),
  :deep(h4),
  :deep(h5),
  :deep(h6) {
    margin: 12px 0 8px;
    color: inherit;
    line-height: 1.35;
  }

  :deep(h1) {
    font-size: 22px;
  }

  :deep(h2) {
    font-size: 19px;
  }

  :deep(h3) {
    font-size: 16px;
  }

  :deep(ul),
  :deep(ol) {
    margin: 6px 0 10px;
    padding-left: 22px;
  }

  :deep(li + li) {
    margin-top: 4px;
  }

  :deep(blockquote) {
    margin: 10px 0;
    border-left: 3px solid #9bb9ad;
    background: #f5f8f7;
    color: #34423e;
    padding: 8px 12px;
  }

  :deep(table) {
    display: block;
    max-width: 100%;
    margin: 10px 0;
    overflow: auto;
    border-collapse: collapse;
  }

  :deep(th),
  :deep(td) {
    border: 1px solid #d8e2df;
    padding: 6px 9px;
    vertical-align: top;
  }

  :deep(th) {
    background: #f3f7f5;
    font-weight: 700;
  }

  :deep(a) {
    color: #1677ff;
    word-break: break-word;
  }

  :deep(img) {
    max-width: 100%;
    border-radius: 6px;
  }

  :deep(:not(pre) > code) {
    border: 1px solid #dbe4e1;
    border-radius: 4px;
    background: #f5f8f7;
    color: #0f5c45;
    padding: 1px 5px;
    font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', monospace;
    font-size: 0.92em;
  }

  :deep(pre) {
    max-width: 100%;
    margin: 10px 0;
    overflow: auto;
    border: 1px solid #26352f;
    border-radius: 8px;
    background: #101915;
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
  }

  :deep(pre code) {
    display: block;
    padding: 13px 14px 15px;
    background: #101915;
    color: #dbe7e1;
    font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', monospace;
    font-size: 13px;
    line-height: 1.65;
    tab-size: 2;
    white-space: pre;
  }

  :deep(pre code.hljs) {
    background: #101915;
    color: #dbe7e1;
  }

  :deep(.hljs-comment),
  :deep(.hljs-quote) {
    color: #7e9289;
  }

  :deep(.hljs-keyword),
  :deep(.hljs-selector-tag),
  :deep(.hljs-subst) {
    color: #7cc7ff;
  }

  :deep(.hljs-string),
  :deep(.hljs-regexp),
  :deep(.hljs-addition),
  :deep(.hljs-attribute) {
    color: #9bd88f;
  }

  :deep(.hljs-number),
  :deep(.hljs-literal),
  :deep(.hljs-variable),
  :deep(.hljs-template-variable) {
    color: #f2c16b;
  }

  :deep(.hljs-title),
  :deep(.hljs-section),
  :deep(.hljs-name),
  :deep(.hljs-type) {
    color: #f0d78c;
  }

  :deep(.vditor-copy) {
    top: 8px;
    right: 8px;
    color: #c9d6d0;
  }

  :deep(.vditor-copy span) {
    border-radius: 6px;
    background: rgba(255, 255, 255, 0.08);
  }

  :deep(.vditor-copy span:hover) {
    background: rgba(255, 255, 255, 0.14);
    color: #fff;
  }

  :deep(.katex-display) {
    max-width: 100%;
    margin: 12px 0;
    overflow-x: auto;
    overflow-y: hidden;
    padding: 2px 0;
  }

  :deep(.katex) {
    font-size: 1.04em;
  }
}
</style>
