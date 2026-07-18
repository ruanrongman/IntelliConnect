<script setup>
import { reactive, ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Select,
  Button,
  Modal,
  message,
  Switch,
  Tooltip,
  Progress,
  Form,
  Input,
  Drawer,
} from 'ant-design-vue'
import {
  DeleteOutlined,
  DownloadOutlined,
  DownOutlined,
  QuestionCircleOutlined,
  SettingOutlined,
  UploadOutlined,
} from '@ant-design/icons-vue'
import NodeInfo from '@/views/knowledgeGraphic/nodeInfo.vue'
import RelationConfig from '@/views/knowledgeGraphic/relationConfig.vue'
import ConfigPanel from '@/views/knowledgeGraphic/configPanel.vue'

import SVGPainter from 'zrender/lib/svg/Painter.js'
import * as echarts from 'echarts'

import {
  queryKnowledgeGraphic,
  addKnowledgeGraphicNode,
  enableKnowledgeGraphic,
  disabledKnowledgeGraphic,
  getKnowledgeGraphicState,
  addKnowledgeGraphicToggleConfig,
  uploadKnowledgeGraphicFile,
  getKnowledgeGraphicFileProgress,
  downloadKnowledgeGraphicDataset,
  clearKnowledgeGraphic,
} from '@/api/knowledgeGraphic'
import { getProduct } from '@/api/product'

const router = useRouter()

const chart = ref(null)
const baseOption = {
  tooltip: { show: false },
  series: [
    {
      name: 'Knowledge Graphic',
      type: 'graph',
      layout: 'force',
      edgeSymbol: ['circle', 'arrow'],
      force: {
        // 节点之间的斥力，值越大节点越分散，可以设置一个较大的值以避免重叠
        repulsion: 30,
        // 是否防止节点重叠
        avoidOverlap: true,
      },
      data: [],
      links: [],
      categories: [{ name: 'Nodes' }],
      roam: true,
      roamTrigger: 'global',
      scaleLimit: {
        max: 4,
        min: 0.1,
      },
      draggable: true,
      label: {
        show: true,
        color: '#000',
        textBorderWidth: 0,
        position: 'right',
        formatter: '{b}',
      },
      // labelLayout: {
      //   hideOverlap: true
      // },
      lineStyle: {
        color: 'source',
        curveness: 0.3,
      },
      emphasis: {
        focus: 'adjacency',
        lineStyle: {
          width: 5,
        },
        label: {
          show: true,
          fontWeight: 'bold',
          color: '#000',
        },
      },
    },
  ],
  thumbnail: {
    width: '200',
    height: '150',
    right: '5',
    bottom: '5',
    windowStyle: {
      color: 'rgba(140, 212, 250, 0.5)',
      borderColor: 'rgba(30, 64, 175, 0.7)',
      opacity: 1,
    },
  },
}
const repulsion = ref(30)
const crtOption = ref({})

const isAddingNode = ref(false)
const newNodeForm = reactive({
  productId: -1,
  name: '',
  des: '',
  attributes: [],
})

const knowledgeGraphicEnable = ref(false)
const configModalOpen = ref(false)
const allowedUploadTypes = [
  '.pdf',
  '.txt',
  '.md',
  '.markdown',
  '.doc',
  '.docx',
  '.ppt',
  '.pptx',
  '.xls',
  '.xlsx',
  '.zip',
]
const allowedUploadTypesString = allowedUploadTypes.join(', ')

const cavWidth = ref(300)
const cavHeight = ref(100)
const cavDom = ref(null)
const resizeObserver = ref(null)
const selectedNode = ref(null)
const showNodeInfo = ref(false)
const graphic = ref(null)

const products = ref([])
const productLoading = ref(true)
const currentProductId = ref(null)
const isUploadModalOpen = ref(false)
const uploadLoading = ref(false)
const downloadLoading = ref(false)
const uploadSelectedFiles = ref([])
const uploadProgressTimer = ref(null)
const uploadForm = reactive({
  file: [],
})
const uploadProgress = reactive({
  percent: 0,
  status: 'pending',
  message: '',
  fileName: '',
  currentChunk: 0,
  totalChunks: 0,
  finished: false,
  success: false,
  errorMessage: '',
})

// Variables for handling relation
const isConnection = ref(false)
const relationTemp = ref([])
const relationConfigToggle = ref(false)
const createRelationFlag = ref(false)

function fetchProducts() {
  getProduct().then((res) => {
    const { data, errorCode } = res.data
    if (errorCode === 2001) {
      router.push('/login')
      return
    }
    if (errorCode === 200 && data && Array.isArray(data)) {
      products.value = data.map((item, index) => {
        return {
          key: item.id,
          value: item.id,
          label: item.productName,
        }
      })
      productLoading.value = false
      currentProductId.value = data.length > 0 ? data[0].id : null
      if (currentProductId.value !== null) {
        afterFetchCurrentProduct()
      }
    } else {
      products.value = []
      productLoading.value = false
    }
  })
}

function afterFetchCurrentProduct() {
  getCurrentKnowledgeGraphicState()
  getCurrentKnowledgeGraphic()
}

function handleKnowledgeGraphicEnableToggle() {
  const resultPreFilter = (res) => {
    const { errorCode } = res.data
    if (errorCode === 2001) {
      router.push('/login')
    }
  }
  const params = { productId: currentProductId.value }
  if (knowledgeGraphicEnable.value) {
    disabledKnowledgeGraphic(params)
      .then((res) => resultPreFilter(res))
      .then(() => {
        message.info('已关闭知识图谱自动生成，与模型的对话将不再生成知识图谱')
        getCurrentKnowledgeGraphicState()
      })
  } else {
    enableKnowledgeGraphic(params)
      .then((res) => resultPreFilter(res))
      .then(() => {
        message.info('已开启知识图谱自动生成，与模型的对话超过6轮将自动生成知识图谱')
        getCurrentKnowledgeGraphicState()
      })
  }
}

function handleStartAddNewNode() {
  newNodeForm.productId = currentProductId.value
  isAddingNode.value = true
}

function handleOpenConfigModal() {
  configModalOpen.value = true
}

function handleCloseConfigModal() {
  configModalOpen.value = false
}

function handleStopAddNewNode() {
  isAddingNode.value = false
  newNodeForm.name = ''
  newNodeForm.des = ''
  newNodeForm.attributes = []
}

function handleAddNewNode() {
  addKnowledgeGraphicNode(newNodeForm).then(() => {
    message.success('添加成功！')
    getCurrentKnowledgeGraphic()
  })
  handleStopAddNewNode()
}

function resetUploadProgress() {
  uploadProgress.percent = 0
  uploadProgress.status = 'pending'
  uploadProgress.message = ''
  uploadProgress.fileName = ''
  uploadProgress.currentChunk = 0
  uploadProgress.totalChunks = 0
  uploadProgress.finished = false
  uploadProgress.success = false
  uploadProgress.errorMessage = ''
}

function handleOpenUploadModal() {
  if (!currentProductId.value) return
  resetUploadProgress()
  uploadForm.file = []
  uploadSelectedFiles.value = []
  isUploadModalOpen.value = true
}

function handleCloseUploadModal() {
  if (uploadLoading.value) {
    message.warning('知识图谱正在生成中，请等待完成')
    return
  }
  isUploadModalOpen.value = false
}

function handleUploadDone() {
  isUploadModalOpen.value = false
  uploadForm.file = []
  uploadSelectedFiles.value = []
  resetUploadProgress()
}

function handleBeforeUpload(file) {
  const fileExtension = '.' + file.name.split('.').pop().toLowerCase()
  if (!allowedUploadTypes.includes(fileExtension)) {
    message.error(`文件类型不支持，请上传 ${allowedUploadTypesString} 格式的文件`)
    return false
  }
  const isLt200M = file.size / 1024 / 1024 < 200
  if (!isLt200M) {
    message.error('文件大小不能超过200MB')
    return false
  }
  const exists = uploadSelectedFiles.value.some(
    (item) => item.uid === file.uid || (item.name === file.name && item.size === file.size)
  )
  if (!exists) {
    uploadSelectedFiles.value = [...uploadSelectedFiles.value, file]
    uploadForm.file = uploadSelectedFiles.value
  }
  return false
}

function handleUploadRemove(file) {
  uploadSelectedFiles.value = uploadSelectedFiles.value.filter((item) => item.uid !== file.uid)
  uploadForm.file = uploadSelectedFiles.value
  return true
}

function stopUploadProgressPolling() {
  if (uploadProgressTimer.value) {
    clearInterval(uploadProgressTimer.value)
    uploadProgressTimer.value = null
  }
}

function applyUploadProgress(progress) {
  uploadProgress.percent = Number(progress.percent || 0)
  uploadProgress.status = progress.status || 'pending'
  uploadProgress.message = progress.message || ''
  uploadProgress.fileName = progress.fileName || ''
  uploadProgress.currentChunk = Number(progress.currentChunk || 0)
  uploadProgress.totalChunks = Number(progress.totalChunks || 0)
  uploadProgress.finished = Boolean(progress.finished)
  uploadProgress.success = Boolean(progress.success)
  uploadProgress.errorMessage = progress.errorMessage || ''
}

function pollUploadProgress(taskId) {
  stopUploadProgressPolling()
  uploadProgressTimer.value = setInterval(() => {
    getKnowledgeGraphicFileProgress({ taskId })
      .then((res) => {
        const { data, errorCode } = res.data
        if (errorCode !== 200 || !data) return
        applyUploadProgress(data)
        if (data.finished) {
          stopUploadProgressPolling()
          uploadLoading.value = false
          if (data.success) {
            message.success('知识图谱生成完成')
            getCurrentKnowledgeGraphic()
          } else {
            message.error(data.errorMessage || '知识图谱生成失败')
          }
        }
      })
      .catch(() => {
        stopUploadProgressPolling()
        uploadLoading.value = false
        message.error('获取处理进度失败')
      })
  }, 1000)
}

async function handleUploadKnowledgeGraphic() {
  const files = uploadSelectedFiles.value.length > 0 ? uploadSelectedFiles.value : uploadForm.file
  if (!files || files.length === 0) {
    message.error('请先选择文件')
    return
  }
  try {
    uploadLoading.value = true
    resetUploadProgress()
    uploadProgress.status = 'uploading'
    uploadProgress.message = '正在上传文件'
    const filesToUpload = files.map((file) => file.originFileObj || file)
    const res = await uploadKnowledgeGraphicFile(filesToUpload, {
      productId: currentProductId.value,
    })
    const { data, errorCode, errorMsg } = res.data
    if (errorCode !== 200 || !data?.taskId) {
      uploadLoading.value = false
      message.error(errorMsg || '上传失败')
      return
    }
    uploadProgress.message = '文件已上传，正在开始处理'
    pollUploadProgress(data.taskId)
  } catch (err) {
    uploadLoading.value = false
    message.error('上传失败，请重试')
  }
}

async function handleDownloadDataset() {
  if (!currentProductId.value) return
  if (downloadLoading.value) return
  downloadLoading.value = true
  try {
    const res = await downloadKnowledgeGraphicDataset({ productId: currentProductId.value })
    if (res.data instanceof Blob && res.data.type.includes('application/json')) {
      const text = await res.data.text()
      try {
        const json = JSON.parse(text)
        if (json.errorCode && json.errorCode !== 200) {
          message.error(json.errorMsg || '下载失败')
          return
        }
      } catch (e) {
        // The response is the dataset JSON itself.
      }
      const blob = new Blob([text], { type: 'application/json;charset=utf-8' })
      downloadBlob(blob, 'json')
      message.success('数据集下载已开始')
      return
    }
    const blob = new Blob([res.data], { type: 'application/json;charset=utf-8' })
    downloadBlob(blob, 'json')
    message.success('数据集下载已开始')
  } catch (err) {
    message.error('下载失败，请重试')
  } finally {
    downloadLoading.value = false
  }
}

function downloadBlob(blob, extension) {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `knowledge-graphic-${currentProductId.value}.${extension}`
  link.style.display = 'none'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.setTimeout(() => {
    window.URL.revokeObjectURL(url)
  }, 1000)
}

function canvasToBlob(canvas) {
  return new Promise((resolve, reject) => {
    canvas.toBlob((blob) => {
      if (blob) {
        resolve(blob)
      } else {
        reject(new Error('PNG 图像生成失败'))
      }
    }, 'image/png')
  })
}

function createSvgContent() {
  const width = chart.value.getWidth()
  const height = chart.value.getHeight()
  const svgPainter = new SVGPainter(null, chart.value.getZr().storage, {
    width,
    height,
    ssr: true,
  })
  try {
    svgPainter.setBackgroundColor('#fff')
    return svgPainter.renderToString({
      cssAnimation: false,
      cssEmphasis: false,
      useViewBox: true,
    })
  } finally {
    svgPainter.dispose()
  }
}

async function handleDownloadGraphic(format) {
  if (!currentProductId.value || !chart.value || chart.value.isDisposed()) return
  if (downloadLoading.value) return
  downloadLoading.value = true
  try {
    if (format === 'svg') {
      const svgContent = createSvgContent()
      downloadBlob(new Blob([svgContent], { type: 'image/svg+xml;charset=utf-8' }), 'svg')
    } else {
      const imageCanvas = chart.value.renderToCanvas({ pixelRatio: 2, backgroundColor: '#fff' })
      downloadBlob(await canvasToBlob(imageCanvas), 'png')
    }
    message.success(`${format.toUpperCase()} 图谱下载已开始`)
  } catch (err) {
    message.error('图谱导出失败，请重试')
  } finally {
    downloadLoading.value = false
  }
}

function handleDownloadMenuClick({ key }) {
  if (key === 'json') {
    handleDownloadDataset()
    return
  }
  handleDownloadGraphic(key)
}
function handleClearKnowledgeGraphic() {
  if (!currentProductId.value) return
  const hasGraphicData =
    graphic.value &&
    ((Array.isArray(graphic.value.nodes) && graphic.value.nodes.length > 0) ||
      (Array.isArray(graphic.value.relations) && graphic.value.relations.length > 0))
  if (!hasGraphicData) {
    message.info('当前知识图谱为空，无需清空')
    return
  }
  Modal.confirm({
    title: '确认清空知识图谱？',
    content: '该操作会删除当前产品下的所有节点、关系、属性和图谱向量数据。',
    okText: '清空',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      const res = await clearKnowledgeGraphic({ productId: currentProductId.value })
      const { errorCode, errorMsg } = res.data
      if (errorCode === 200) {
        message.success('知识图谱已清空')
        getCurrentKnowledgeGraphic()
      } else {
        message.error(errorMsg || '清空失败')
      }
    },
  })
}

function handleStartConnecting(e) {
  if (e.key !== 'c' || showNodeInfo.value) return
  if (isAddingNode.value || relationConfigToggle.value) return
  e.preventDefault()
  // Caution about this! Use relationConfigToggle to debounce!
  if (!isConnection.value && !relationConfigToggle.value) {
    isConnection.value = true
    relationTemp.value = []
  }
}

function handleCancelConnecting(e) {
  if (e.key !== 'c' || showNodeInfo.value) return
  if (isAddingNode.value) return
  if (relationTemp.value.length < 2) {
    const option = { ...crtOption.value }
    option.series[0].data = option.series[0].data.map((item) => {
      if (item.itemStyle !== undefined) item.itemStyle = undefined
      return item
    })
    chart.value.setOption(option)
    isConnection.value = false
    relationTemp.value = []
  }
}

function handleConnectingToggle(checked, e) {
  isConnection.value = checked
  if (relationTemp.value.length !== 0) {
    const option = { ...crtOption.value }
    option.series[0].data = option.series[0].data.map((item) => {
      if (item.itemStyle !== undefined) item.itemStyle = undefined
      return item
    })
    chart.value.setOption(option)
  }
  relationTemp.value = []
}

function handleRelationConfig() {
  relationConfigToggle.value = true
  if (showNodeInfo.value) showNodeInfo.value = false
}

function handleCancelConfigRelation(e) {
  if (e && e.message) message.error(e.message)
  relationTemp.value = []
  relationConfigToggle.value = false
  if (createRelationFlag.value) {
    // Filter temp link
    const option = { ...crtOption.value }
    option.series[0].links = option.series[0].links.filter((item) => {
      return item.lineStyle === undefined || item.lineStyle.type !== 'dashed'
    })
    option.series[0].data = option.series[0].data.map((item) => {
      if (item.itemStyle !== undefined) item.itemStyle = undefined
      return item
    })
    chart.value.setOption(option)
  }
}

function handleSubmitConfigRelation() {
  relationTemp.value = []
  relationConfigToggle.value = false
  getCurrentKnowledgeGraphic()
}

function handleConnected() {
  // Add temp link on graphic and config des to post
  isConnection.value = false
  let newOption = { ...crtOption.value }
  newOption.series[0].links.push({
    source: relationTemp.value[0],
    target: relationTemp.value[1],
    lineStyle: {
      type: 'dashed',
      width: 5,
    },
    label: {
      show: true,
      formatter: `${relationTemp.value[0]}->${relationTemp.value[1]}`,
    },
  })
  chart.value.setOption(newOption)
  //   Open relation config drawer
  // Notify component now config for adding
  createRelationFlag.value = true
  handleRelationConfig()
}

function updateGraphicData() {
  if (!graphic.value) return
  let option = { ...baseOption }
  // 强制抹除所有节点已有的分类信息，重新分类
  graphic.value.nodes.forEach((node) => {
    node.category = undefined
  })
  const TYPES = classifyNodes(graphic.value.nodes, graphic.value.relations)
  const categories = []
  for (let i = 0; i < TYPES; i++) {
    categories.push(`type${i}`)
  }
  option.series[0].categories = categories
  option.series[0].data = graphic.value.nodes.map((item, index) => {
    return {
      name: item.name,
      category: item.category,
      symbolSize: Math.min(10 * item.attributes.length + 10, 40),
      x: ((index * 50) % cavWidth.value) + Math.random() * 10,
      y: parseInt(index / 50) * 50 + Math.random() * 10,
      value: Math.min(10 * item.attributes.length, 30),
    }
  })
  option.series[0].links = graphic.value.relations.map((item) => {
    return {
      source: item.from,
      target: item.to,
      value: Math.min(item.name.length * 10, 255),
      // label: {
      //   show: true,
      //   formatter: item.name
      // }
    }
  })
  repulsion.value = Math.min(graphic.value.nodes.length * 5, 255)
  option.series[0].force.repulsion = repulsion.value
  refreshChartHandler(option)
}

function handleProductIdChange(value) {
  currentProductId.value = value
  afterFetchCurrentProduct()
}

function filterProductOption(input, option) {
  const keyword = String(input || '')
    .trim()
    .toLowerCase()
  if (!keyword) return true
  return String(option?.label || '')
    .toLowerCase()
    .includes(keyword)
}

function handleRepulsionChange(value) {
  repulsion.value = value
  const option = { ...crtOption.value }
  option.series[0].force.repulsion = value
  chart.value.setOption(option)
}

function getCurrentKnowledgeGraphic() {
  if (!currentProductId.value) return
  if (chart.value) chart.value.showLoading()
  showNodeInfo.value = false
  queryKnowledgeGraphic({ productId: currentProductId.value }).then((res) => {
    const { data, errorCode } = res.data
    if (errorCode === 2001) {
      router.push('/login')
      return
    }
    if (errorCode === 200) {
      if (chart.value) chart.value.hideLoading()
      graphic.value = data
      updateGraphicData()
    }
  })
}

function handleNodeClick(params) {
  if (relationConfigToggle.value && createRelationFlag.value) return
  relationConfigToggle.value = false
  if (isConnection.value) {
    const option = { ...crtOption.value }
    option.series[0].data = option.series[0].data.map((item) => {
      if (item.name !== params.name) return item
      item.itemStyle = {
        borderWidth: 5,
        borderColor: '#ffb2b2',
      }
      return item
    })
    chart.value.setOption(option)
    if (relationTemp.value.length === 0) {
      relationTemp.value.push(params.name)
    } else if (relationTemp.value.length === 1 && relationTemp.value[0] !== params.name) {
      const relations = graphic.value.relations
      if (
        relations.filter((item) => item.from === relationTemp.value[0] && item.to === params.name)
          .length !== 0
      ) {
        message.warn('该关系已经存在')
        option.series[0].data = option.series[0].data.map((item) => {
          if (item.name !== params.name) return item
          item.itemStyle = undefined
          return item
        })
        chart.value.setOption(option)
        return
      }
      relationTemp.value.push(params.name)
      handleConnected()
    }
    return
  }
  selectedNode.value = params.name
  showNodeInfo.value = true
}

function handleEdgeClick(params) {
  if (isConnection.value) return
  const { data } = params
  relationTemp.value = [data.source, data.target]
  showNodeInfo.value = false
  relationConfigToggle.value = true
  createRelationFlag.value = false
}

function refreshChartHandler(option) {
  if (chart.value) {
    chart.value.dispose()
  }
  crtOption.value = option
  chart.value = echarts.init(cavDom.value)
  chart.value.setOption(option)
  chart.value.on('click', function (params) {
    if (params.dataType === 'node') {
      handleNodeClick(params)
    } else if (params.dataType === 'edge') {
      handleEdgeClick(params)
    }
  })

  if (isConnection.value) {
    isConnection.value = false
    relationConfigToggle.value = false
  }
}

function initializeCanvas() {
  if (cavDom.value) {
    // Set cav size to parent size
    const W = cavDom.value.offsetParent.offsetWidth
    const H = cavDom.value.offsetParent.offsetHeight
    cavDom.value.width = W
    cavDom.value.height = H
    cavWidth.value = W
    cavHeight.value = H

    resizeObserver.value = new ResizeObserver((entries) => {
      for (const entry of entries) {
        const newW = entry.target.offsetWidth
        const newH = entry.target.offsetHeight
        cavDom.value.width = newW
        cavDom.value.height = newH
        cavWidth.value = newW
        cavHeight.value = newH

        updateGraphicData()
        // refreshChartHandler(option);
      }
    })
    resizeObserver.value.observe(cavDom.value.offsetParent)
    refreshChartHandler(baseOption)
  }
}

/**
 * 节点分类：
 * 第一阶段：对入度+出度 >= 5 的高连通节点，按度数降序处理。
 *   每个节点为中心，将两度以内（双向）可达的未分类节点归为同一类。
 * 第二阶段：剩余节点随机选取一个，同样将两度以内可达的未分类节点归为同一类。
 * 已分类节点不再参与后续分类。
 * @param nodes
 * @param links
 * @returns {number}
 */
function classifyNodes(nodes, links) {
  // 如果节点为空，直接返回0
  if (nodes.length === 0) return 0

  // 构建节点名称到节点对象的映射
  const nameToNode = new Map()

  nodes.forEach((node) => {
    nameToNode.set(node.name, node)
  })

  // 构建邻接表（出边）、逆邻接表（入边），记录每个节点的总度数（入度+出度）
  const adjList = new Map()
  const revAdjList = new Map()
  const degrees = new Map()

  nodes.forEach((node) => {
    adjList.set(node.name, new Set())
    revAdjList.set(node.name, new Set())
    degrees.set(node.name, 0)
  })

  links.forEach((link) => {
    if (nameToNode.has(link.from) && nameToNode.has(link.to)) {
      adjList.get(link.from).add(link.to)
      revAdjList.get(link.to).add(link.from)

      degrees.set(link.from, degrees.get(link.from) + 1)
      degrees.set(link.to, degrees.get(link.to) + 1)
    }
  })

  // 获取所有未分类的节点名称
  const getUnclassifiedNodes = () => {
    return Array.from(nameToNode.values())
      .filter((node) => node.category === null || node.category === undefined)
      .map((node) => node.name)
  }

  // BFS：获取三度以内（双向搜索，含入边和出边方向）的所有节点
  const getDirectNeighbors = (startNodeName) => {
    const visited = new Set()
    const queue = [{ node: startNodeName, distance: 0 }]

    while (queue.length > 0) {
      const { node, distance } = queue.shift()

      if (visited.has(node) || distance > 3) continue

      visited.add(node)

      for (const neighbor of adjList.get(node) || []) {
        if (!visited.has(neighbor)) {
          queue.push({ node: neighbor, distance: distance + 1 })
        }
      }

      for (const neighbor of revAdjList.get(node) || []) {
        if (!visited.has(neighbor)) {
          queue.push({ node: neighbor, distance: distance + 1 })
        }
      }
    }

    return Array.from(visited)
  }

  // 将起始节点及三度内未分类节点归为一个新类别，返回本次分类的节点数
  const classifyCluster = (startNodeName, categoryId) => {
    const nodesToClassify = getDirectNeighbors(startNodeName)
    let classifiedCount = 0
    for (const nodeName of nodesToClassify) {
      const node = nameToNode.get(nodeName)
      if (node && (node.category === null || node.category === undefined)) {
        node.category = categoryId
        classifiedCount++
      }
    }
    return classifiedCount
  }

  let categoryId = 0
  let unclassifiedCount = nodes.length

  // ===== 第一阶段：优先处理入度出度和 >= 5 的高连通节点 =====
  const allUnclassified = getUnclassifiedNodes()
  const highDegreeNodeNames = new Set(
    allUnclassified.filter((name) => degrees.get(name) >= 5)
  )
  const highDegreeNodes = Array.from(highDegreeNodeNames)
    .sort((a, b) => degrees.get(b) - degrees.get(a)) // 度数降序

  for (const nodeName of highDegreeNodes) {
    const node = nameToNode.get(nodeName)
    if (!node || (node.category !== null && node.category !== undefined)) continue

    // 检查是否有直接邻居也是关键节点 —— 若有则跳过，留给第二阶段随机归类
    const hasHighDegreeNeighbor = [...(adjList.get(nodeName) || [])]
      .concat([...(revAdjList.get(nodeName) || [])])
      .some((neighbor) => highDegreeNodeNames.has(neighbor))
    if (hasHighDegreeNeighbor) continue

    const count = classifyCluster(nodeName, categoryId)
    if (count > 0) {
      unclassifiedCount -= count
      categoryId++
    }
  }

  // ===== 第二阶段：剩余未分类节点随机选取分类 =====
  while (unclassifiedCount > 0) {
    const remaining = getUnclassifiedNodes()
    if (remaining.length === 0) break

    // 从剩余节点中随机选一个
    const randomIndex = Math.floor(Math.random() * remaining.length)
    const startNodeName = remaining[randomIndex]

    const count = classifyCluster(startNodeName, categoryId)
    if (count > 0) {
      unclassifiedCount -= count
      categoryId++
    }
  }

  // 返回类别数量
  return categoryId
}

function registerGlobalEvent() {
  window.addEventListener('keydown', handleStartConnecting)
  window.addEventListener('keyup', handleCancelConnecting)
}

function removeGlobalEvent() {
  window.removeEventListener('keydown', handleStartConnecting)
  window.removeEventListener('keyup', handleCancelConnecting)
}

function getCurrentKnowledgeGraphicState() {
  getKnowledgeGraphicState({
    productId: currentProductId.value,
  }).then((res) => {
    const { data, errorCode } = res.data
    if (errorCode === 2001) {
      router.push('/login')
      return
    }
    if (data === null) {
      addKnowledgeGraphicToggleConfig({
        productId: currentProductId.value,
      }).then((res) => {
        const { errorCode } = res.data
        if (errorCode === 2001) {
          router.push('/login')
          return
        }
        getCurrentKnowledgeGraphicState()
      })
    } else {
      knowledgeGraphicEnable.value = data.value === 'true'
    }
  })
}

onMounted(() => {
  fetchProducts()
  initializeCanvas()
  registerGlobalEvent()
})

onUnmounted(() => {
  if (resizeObserver.value) resizeObserver.value.disconnect()
  stopUploadProgressPolling()
  removeGlobalEvent()
})
</script>

<template>
  <div class="wrapper">
    <div class="kg-main">
      <div class="kg-header">
        <div class="option-item">
          <label class="option-label product-label" for="product">产品</label>
          <Select
            id="product"
            class="option-main product-select"
            :value="currentProductId"
            :options="products"
            :loading="productLoading"
            show-search
            :filter-option="filterProductOption"
            placeholder="搜索或选择产品"
            @change="handleProductIdChange"
          >
          </Select>
        </div>
        <div class="option-item">
          <Button
            :type="'primary'"
            :disabled="!currentProductId"
            @click="getCurrentKnowledgeGraphic"
            >手动刷新数据</Button
          >
        </div>
        <div v-if="currentProductId !== null" class="option-item">
          <label class="option-label" for="connect-toggle"> 知识图谱自动生成开关 </label>
          <Switch
            id="connect-toggle"
            :checked="knowledgeGraphicEnable"
            @click="handleKnowledgeGraphicEnableToggle"
          />
        </div>
        <div v-if="currentProductId !== null" class="option-item">
          <Button
            :type="'default'"
            :disabled="currentProductId === null"
            @click="handleOpenConfigModal"
          >
            <template #icon><SettingOutlined /></template>
            配置
          </Button>
        </div>
        <div class="option-item">
          <Button
            :type="'primary'"
            :disabled="currentProductId === null"
            @click="handleStartAddNewNode"
            >添加节点</Button
          >
        </div>
        <div class="option-item">
          <Button
            :type="'primary'"
            :disabled="currentProductId === null"
            @click="handleOpenUploadModal"
          >
            <template #icon><UploadOutlined /></template>
            上传文件以生成图谱
          </Button>
        </div>
        <div class="option-item">
          <a-dropdown :disabled="currentProductId === null || downloadLoading">
            <Button :disabled="currentProductId === null" :loading="downloadLoading">
              <template #icon><DownloadOutlined /></template>
              下载图谱
              <DownOutlined class="download-arrow" />
            </Button>
            <template #overlay>
              <a-menu @click="handleDownloadMenuClick">
                <a-menu-item key="png">PNG 图片</a-menu-item>
                <a-menu-item key="svg">SVG 矢量图</a-menu-item>
                <a-menu-divider />
                <a-menu-item key="json">JSON 数据</a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
        <div class="option-item">
          <Button danger :disabled="currentProductId === null" @click="handleClearKnowledgeGraphic">
            <template #icon><DeleteOutlined /></template>
            清空图谱
          </Button>
        </div>
        <div v-if="currentProductId !== null" class="option-item">
          <label class="option-label" for="connect-toggle">
            <Tooltip title="按住C键可开启，松开自动关闭"><QuestionCircleOutlined /></Tooltip>
            手动连接模式开关
          </label>
          <Switch
            id="connect-toggle"
            :disabled="relationConfigToggle"
            :checked="isConnection"
            @click="handleConnectingToggle"
          />
        </div>
      </div>
      <div class="kg-body">
        <canvas id="kg-cav" ref="cavDom">Your browser didn't support canvas.</canvas>
        <ConfigPanel
          v-if="configModalOpen"
          :product-id="currentProductId"
          :repulsion="repulsion"
          @close="handleCloseConfigModal"
          @update:repulsion="handleRepulsionChange"
        />
      </div>
    </div>
    <Modal
      :open="isAddingNode"
      title="添加节点"
      @cancel="handleStopAddNewNode"
      @ok="handleAddNewNode"
    >
      <Form :model="newNodeForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <Form.Item
          label="节点名称"
          name="name"
          :rules="[{ required: true, message: '请输入节点名称' }]"
        >
          <Tooltip title="如果节点名称重复会更新节点描述">
            <Input
              v-model:value="newNodeForm.name"
              :maxlength="50"
              placeholder="节点名称，不超过50字"
            />
          </Tooltip>
        </Form.Item>
        <Form.Item
          label="节点描述"
          name="des"
          :rules="[{ required: true, message: '请输入节点描述' }]"
        >
          <Input.TextArea
            v-model:value="newNodeForm.des"
            :maxlength="255"
            placeholder="节点描述，不超过255字"
          />
        </Form.Item>
      </Form>
    </Modal>
    <Modal
      :open="isUploadModalOpen"
      title="上传文件以生成知识图谱"
      :footer="null"
      :closable="!uploadLoading"
      :mask-closable="!uploadLoading"
      @cancel="handleCloseUploadModal"
    >
      <Form :model="uploadForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <Form.Item
          label="选择文件"
          name="file"
          :rules="[{ required: true, message: '请上传文件' }]"
        >
          <a-upload
            v-model:file-list="uploadForm.file"
            :multiple="true"
            :before-upload="handleBeforeUpload"
            :show-upload-list="true"
            :disabled="uploadLoading"
            :accept="allowedUploadTypesString"
            @remove="handleUploadRemove"
          >
            <Button :disabled="uploadLoading">
              <template #icon><UploadOutlined /></template>
              选择文件（单个200MB以内）
            </Button>
          </a-upload>
          <div class="upload-tip">支持格式：{{ allowedUploadTypesString }}</div>
        </Form.Item>
        <Form.Item v-if="uploadLoading || uploadProgress.finished" label="处理进度">
          <Progress
            :percent="uploadProgress.percent"
            :status="
              uploadProgress.status === 'error'
                ? 'exception'
                : uploadProgress.finished && uploadProgress.success
                  ? 'success'
                  : 'active'
            "
          />
          <div class="upload-progress-text">
            {{ uploadProgress.message }}
          </div>
          <div v-if="uploadProgress.totalChunks > 0" class="upload-progress-subtext">
            当前片段：{{ uploadProgress.currentChunk }} / {{ uploadProgress.totalChunks }}
          </div>
        </Form.Item>
        <Form.Item :wrapper-col="{ offset: 6, span: 16 }">
          <Button
            v-if="uploadProgress.finished && uploadProgress.success"
            type="primary"
            @click="handleUploadDone"
          >
            完成
          </Button>
          <Button
            v-else
            type="primary"
            :loading="uploadLoading"
            :disabled="uploadLoading"
            @click="handleUploadKnowledgeGraphic"
          >
            {{ uploadLoading ? '处理中...' : uploadProgress.finished ? '重新生成' : '开始生成' }}
          </Button>
        </Form.Item>
      </Form>
    </Modal>
    <Drawer
      title="节点详情"
      :mask="false"
      :open="showNodeInfo"
      :width="parseInt(cavWidth / 3)"
      @close="
        () => {
          showNodeInfo = false
        }
      "
    >
      <NodeInfo
        :node-name="selectedNode"
        :product-id="currentProductId"
        @update-node="getCurrentKnowledgeGraphic"
        @delete-node="getCurrentKnowledgeGraphic"
      />
    </Drawer>
    <Drawer
      title="关系配置"
      :mask="false"
      :open="relationConfigToggle"
      :width="parseInt(cavWidth / 3)"
      @close="handleCancelConfigRelation"
    >
      <RelationConfig
        :node-from="relationTemp[0]"
        :node-to="relationTemp[1]"
        :product-id="currentProductId"
        :create-flag="createRelationFlag"
        @cancel="handleCancelConfigRelation"
        @submit="handleSubmitConfigRelation"
      />
    </Drawer>
  </div>
</template>

<style>
.wrapper {
  height: 100%;
}

.kg-main {
  background-color: #fff;
  position: relative;
  width: 100%;
  height: 100%;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  align-items: center;
}

.kg-header {
  position: relative;
  width: 100%;
  height: fit-content;
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-start;
  align-items: center;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.option-item {
  display: flex;
  justify-content: space-evenly;
  max-width: 100%;
  min-width: 50px;
  align-items: center;
  padding: 2px;
  margin-right: 20px;
}

.option-label {
  margin-right: 5px;
}

.option-main {
  min-width: 150px;
}

.product-label:before {
  content: '*';
  color: red;
  margin-right: 2px;
}

.upload-tip,
.upload-progress-subtext {
  margin-top: 8px;
  color: #8c8c8c;
  font-size: 12px;
}

.upload-progress-text {
  margin-top: 8px;
}

.kg-body {
  position: relative;
  width: 100%;
  flex-grow: 1;
  overflow: hidden;
}

.download-arrow {
  margin-left: 4px;
  font-size: 10px;
}
</style>
