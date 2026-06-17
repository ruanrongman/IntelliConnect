<template>
  <section ref="messagePaneRef" class="message-pane">
    <div v-if="messages.length === 0" class="empty-state">
      选择产品后开始对话。
    </div>
    <article
      v-for="item in messages"
      :key="item.id"
      class="message-row"
      :class="[item.role, { pending: item.pending }]"
    >
      <div class="speaker">{{ item.role === 'user' ? '你' : '助手' }}</div>
      <div class="bubble">
        <MarkdownContent v-if="item.content" class="message-content" :content="item.content" />
        <div v-if="item.fileNames?.length" class="message-files">
          <div v-for="(fileName, fileIndex) in item.fileNames" :key="`${fileName}-${fileIndex}`" class="message-file">
            <FileTextOutlined />
            <span>{{ fileName }}</span>
          </div>
        </div>
        <details v-if="hasReferenceRecords(item)" class="reference-records">
          <summary>
            <span>参考记录</span>
            <small>{{ formatReferenceSummary(item) }}</small>
          </summary>
          <div class="reference-groups">
            <details v-if="item.knowledgeRecall?.length" class="reference-group reference-subdetails">
              <summary>
                <span>知识库</span>
                <small>{{ item.knowledgeRecall.length }} 条</small>
              </summary>
              <ol class="knowledge-recall-list">
                <li
                  v-for="(recallItem, recallIndex) in item.knowledgeRecall"
                  :key="`${item.id}-knowledge-${recallIndex}`"
                  class="knowledge-recall-item"
                >
                  <div class="knowledge-recall-meta">
                    相似度 {{ formatRecallScore(recallItem.score) }}
                  </div>
                  <p>{{ recallItem.text }}</p>
                </li>
              </ol>
            </details>
            <details v-if="hasKnowledgeGraphicReference(item)" class="reference-group reference-subdetails">
              <summary>
                <span>知识图谱</span>
                <small>
                  {{ formatKnowledgeGraphicSummary(item.knowledgeGraphicReference) }}
                </small>
              </summary>
              <div v-if="item.knowledgeGraphicReference.nodes?.length" class="graphic-subgroup">
                <div class="graphic-subtitle">
                  <span>节点</span>
                  <small>{{ item.knowledgeGraphicReference.nodes.length }} 个</small>
                </div>
                <ul class="graphic-node-list">
                  <li
                    v-for="(node, nodeIndex) in item.knowledgeGraphicReference.nodes.slice(0, 4)"
                    :key="`${item.id}-graphic-node-${node.name}-${nodeIndex}`"
                    class="graphic-node-item"
                  >
                    <strong>{{ node.name }}</strong>
                    <p v-if="node.des">{{ node.des }}</p>
                    <div v-if="node.attributes?.length" class="graphic-attributes">
                      <span
                        v-for="(attribute, attributeIndex) in node.attributes.slice(0, 4)"
                        :key="`${node.name}-attribute-${attribute}-${attributeIndex}`"
                      >
                        {{ attribute }}
                      </span>
                    </div>
                  </li>
                </ul>
              </div>
              <div v-if="item.knowledgeGraphicReference.relations?.length" class="graphic-subgroup">
                <div class="graphic-subtitle">
                  <span>节点关系</span>
                  <small>{{ item.knowledgeGraphicReference.relations.length }} 条</small>
                </div>
                <ol class="graphic-relation-list">
                  <li
                    v-for="(relation, relationIndex) in item.knowledgeGraphicReference.relations.slice(0, 5)"
                    :key="`${item.id}-graphic-relation-${relation.from}-${relation.to}-${relationIndex}`"
                  >
                    <span class="relation-node">{{ relation.from }}</span>
                    <span class="relation-edge">{{ relation.name || '关联' }}</span>
                    <span class="relation-node">{{ relation.to }}</span>
                  </li>
                </ol>
              </div>
            </details>
          </div>
        </details>
      </div>
      <div v-if="item.role === 'assistant' && item.content" class="message-actions">
        <a-tooltip title="复制">
          <a-button
            type="text"
            shape="circle"
            size="small"
            html-type="button"
            :disabled="item.pending"
            @click="copyMessage(item.content)"
          >
            <template #icon><CopyOutlined /></template>
          </a-button>
        </a-tooltip>
      </div>
    </article>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { message as antMessage } from 'ant-design-vue'
import MarkdownContent from '@/components/MarkdownContent/index.vue'

defineProps({
  messages: {
    type: Array,
    default: () => [],
  },
})

const messagePaneRef = ref(null)

defineExpose({
  getPaneElement: () => messagePaneRef.value,
})

async function copyMessage(content) {
  try {
    await copyText(content)
    antMessage.success('已复制回复')
  } catch (error) {
    antMessage.error('复制失败')
  }
}

async function copyText(text) {
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(text)
    return
  }
  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.style.position = 'fixed'
  textarea.style.opacity = '0'
  document.body.appendChild(textarea)
  textarea.select()
  document.execCommand('copy')
  document.body.removeChild(textarea)
}

function formatRecallScore(score) {
  const normalizedScore = Number(score)
  if (!Number.isFinite(normalizedScore)) {
    return '0.00%'
  }
  return `${(normalizedScore * 100).toFixed(2)}%`
}

function hasReferenceRecords(item) {
  return Boolean(item?.knowledgeRecall?.length || hasKnowledgeGraphicReference(item))
}

function hasKnowledgeGraphicReference(item) {
  const graphicReference = item?.knowledgeGraphicReference
  return Boolean(graphicReference?.nodes?.length || graphicReference?.relations?.length)
}

function formatReferenceSummary(item) {
  const parts = []
  if (item?.knowledgeRecall?.length) {
    parts.push(`知识库 ${item.knowledgeRecall.length}`)
  }
  if (hasKnowledgeGraphicReference(item)) {
    const nodeCount = item.knowledgeGraphicReference.nodes?.length || 0
    const relationCount = item.knowledgeGraphicReference.relations?.length || 0
    parts.push(`图谱 ${nodeCount} 节点 / ${relationCount} 关系`)
  }
  return parts.join(' · ')
}

function formatKnowledgeGraphicSummary(graphicReference) {
  const nodeCount = graphicReference?.nodes?.length || 0
  const relationCount = graphicReference?.relations?.length || 0
  return `${nodeCount} 节点 / ${relationCount} 关系`
}
</script>

<style lang="scss" scoped>
.message-pane {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 0;
  overflow: auto;
  overscroll-behavior: contain;
  padding: 22px;
}

.empty-state {
  margin: 18vh auto 0;
  color: #66736f;
  font-size: 14px;
}

.message-row {
  display: grid;
  gap: 6px;
  max-width: min(760px, 82%);

  &.user {
    align-self: flex-end;
  }

  &.assistant {
    align-self: flex-start;
  }
}

.message-row.assistant:hover .message-actions,
.message-row.assistant:focus-within .message-actions {
  opacity: 1;
}

.speaker {
  color: #66736f;
  font-size: 12px;
  font-weight: 700;
}

.bubble {
  display: grid;
  gap: 10px;
  border: 1px solid #dde3e1;
  border-radius: 8px;
  background: #fff;
  color: #15201d;
  padding: 12px 14px;
  line-height: 1.7;
  overflow-wrap: anywhere;
  box-shadow: 0 8px 22px rgba(21, 32, 29, 0.07);
}

.message-content {
  display: grid;
  gap: 10px;
}

.message-files {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.message-file {
  display: inline-flex;
  align-items: center;
  justify-self: start;
  max-width: min(320px, 100%);
  gap: 8px;
  border: 1px solid #d8e2df;
  border-radius: 8px;
  background: rgba(247, 250, 249, 0.9);
  color: #34423e;
  padding: 7px 10px;
  font-size: 13px;
  line-height: 1.3;

  span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.reference-records {
  border-top: 1px solid #e4ebe8;
  padding-top: 10px;

  > summary {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    color: #34423e;
    cursor: pointer;
    font-size: 13px;
    font-weight: 700;
    list-style: none;
  }

  > summary::-webkit-details-marker {
    display: none;
  }

  > summary::before {
    content: '';
    width: 0;
    height: 0;
    border-top: 4px solid transparent;
    border-bottom: 4px solid transparent;
    border-left: 6px solid #1f7a5c;
    transition: transform 0.16s ease;
  }

  &[open] > summary::before {
    transform: rotate(90deg);
  }

  > summary small {
    margin-left: auto;
    color: #66736f;
    font-size: 12px;
    font-weight: 500;
  }
}

.reference-groups {
  display: grid;
  gap: 10px;
  margin-top: 10px;
}

.reference-group {
  display: grid;
  gap: 8px;
}

.reference-subdetails {
  border: 1px solid #d8e2df;
  border-radius: 8px;
  background: #f7faf9;
  padding: 8px 10px;

  > summary {
    display: flex;
    align-items: center;
    gap: 8px;
    color: #34423e;
    cursor: pointer;
    font-size: 12px;
    font-weight: 700;
    line-height: 1.3;
    list-style: none;
  }

  > summary::-webkit-details-marker {
    display: none;
  }

  > summary::before {
    content: '';
    width: 0;
    height: 0;
    border-top: 4px solid transparent;
    border-bottom: 4px solid transparent;
    border-left: 6px solid #1f7a5c;
    transition: transform 0.16s ease;
  }

  &[open] > summary::before {
    transform: rotate(90deg);
  }

  > summary small {
    margin-left: auto;
    color: #7a8783;
    font-size: 12px;
    font-weight: 500;
  }
}

.knowledge-recall-list {
  display: grid;
  gap: 8px;
  margin: 8px 0 0;
  padding: 0;
  list-style: none;
}

.knowledge-recall-item {
  border: 1px solid #d8e2df;
  border-radius: 8px;
  background: #f7faf9;
  padding: 9px 10px;

  p {
    display: -webkit-box;
    margin: 4px 0 0;
    overflow: hidden;
    color: #34423e;
    font-size: 13px;
    line-height: 1.6;
    white-space: pre-wrap;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 4;
  }
}

.knowledge-recall-meta {
  color: #1f7a5c;
  font-size: 12px;
  font-weight: 700;
}

.graphic-node-list,
.graphic-relation-list {
  display: grid;
  gap: 7px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.graphic-subgroup {
  display: grid;
  gap: 6px;
  margin-top: 8px;
}

.graphic-subtitle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: #4c5b57;
  font-size: 12px;
  font-weight: 700;
  line-height: 1.3;

  small {
    color: #7a8783;
    font-size: 12px;
    font-weight: 500;
  }
}

.graphic-node-item {
  border: 1px solid #d8e2df;
  border-radius: 8px;
  background: #f7faf9;
  padding: 8px 10px;

  strong {
    display: block;
    overflow: hidden;
    color: #15201d;
    font-size: 13px;
    line-height: 1.4;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  p {
    display: -webkit-box;
    margin: 4px 0 0;
    overflow: hidden;
    color: #34423e;
    font-size: 13px;
    line-height: 1.55;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
  }
}

.graphic-attributes {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin-top: 6px;

  span {
    max-width: 100%;
    overflow: hidden;
    border: 1px solid #d4dedb;
    border-radius: 6px;
    background: #fff;
    color: #4c5b57;
    padding: 1px 6px;
    font-size: 12px;
    line-height: 1.6;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.graphic-relation-list li {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(54px, auto) minmax(0, 1fr);
  align-items: center;
  gap: 6px;
  border: 1px solid #d8e2df;
  border-radius: 8px;
  background: #fbfcfc;
  padding: 7px 9px;
  color: #34423e;
  font-size: 12px;
  line-height: 1.4;

  .relation-node {
    overflow: hidden;
    border-radius: 6px;
    background: #f7faf9;
    padding: 3px 6px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .relation-edge {
    position: relative;
    max-width: 120px;
    overflow: hidden;
    border-radius: 999px;
    background: #e8f4ef;
    color: #1f7a5c;
    padding: 3px 18px 3px 8px;
    font-weight: 700;
    text-overflow: ellipsis;
    white-space: nowrap;

    &::after {
      content: '→';
      position: absolute;
      right: 6px;
      color: #1f7a5c;
      font-weight: 700;
    }
  }
}

.message-actions {
  display: flex;
  align-items: center;
  min-height: 28px;
  opacity: 0;
  transition: opacity 0.16s ease;

  :deep(.ant-btn) {
    color: #687672;
  }

  :deep(.ant-btn:hover) {
    background: #edf3f1;
    color: #15201d;
  }
}

.user .bubble {
  border-color: rgba(31, 122, 92, 0.26);
  background: #e8f4ef;
  box-shadow: none;
}

.pending .bubble::after {
  content: '';
  display: inline-block;
  width: 7px;
  height: 16px;
  margin-left: 3px;
  border-radius: 4px;
  background: #1f7a5c;
  vertical-align: -3px;
  animation: blink 1s steps(2, start) infinite;
}

@keyframes blink {
  50% {
    opacity: 0;
  }
}

@media (max-width: 840px) {
  .message-row {
    max-width: 94%;
  }
}
</style>
