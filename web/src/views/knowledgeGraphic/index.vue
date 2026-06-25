<script setup>
import { reactive, ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Select,
  Button,
  Modal,
  Form,
  message,
  Input,
  InputNumber,
  Drawer,
  Switch,
  Tooltip,
  Slider,
  Progress,
} from 'ant-design-vue'
import {
  DeleteOutlined,
  DownloadOutlined,
  EditOutlined,
  QuestionCircleOutlined,
  UploadOutlined,
} from '@ant-design/icons-vue'
import NodeInfo from '@/views/knowledgeGraphic/nodeInfo.vue'
import RelationConfig from '@/views/knowledgeGraphic/relationConfig.vue'

import * as echarts from 'echarts'

import {
  queryKnowledgeGraphic,
  addKnowledgeGraphicNode,
  enableKnowledgeGraphic,
  disabledKnowledgeGraphic,
  getKnowledgeGraphicState,
  addKnowledgeGraphicToggleConfig,
  getKnowledgeGraphicForgetState,
  addKnowledgeGraphicForgetToggleConfig,
  knowledgeGraphicForgetToggle,
  updateKnowledgeGraphicForgetEpoch,
  getKnowledgeGraphicForgetEpoch,
  getKnowledgeGraphicNodeLimit,
  updateKnowledgeGraphicNodeLimit,
  getKnowledgeGraphicNodeHardLimit,
  updateKnowledgeGraphicNodeHardLimit,
  uploadKnowledgeGraphicFile,
  getKnowledgeGraphicFileProgress,
  downloadKnowledgeGraphicDataset,
  clearKnowledgeGraphic,
  getKnowledgeGraphicPromptState,
  addKnowledgeGraphicPromptToggleConfig,
  knowledgeGraphicPromptToggle,
  getProductKnowledgeGraphicPrompt,
  addProductKnowledgeGraphicPrompt,
  updateProductKnowledgeGraphicPrompt,
  deleteProductKnowledgeGraphicPrompt,
} from '@/api/knowledgeGraphic'
import { getProduct } from '@/api/product'

const router = useRouter()

const chart = ref(null)
const baseOption = {
  tooltip: {},
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
const knowledgeGraphForgetEnable = ref(false)
const knowledgeGraphForgetEpoch = ref(10)
const knowledgeGraphNodeLimit = ref(100)
const knowledgeGraphNodeHardLimit = ref(300)
const knowledgeGraphicPromptEnable = ref(false)
const promptModalOpen = ref(false)
const promptLoading = ref(false)
const promptSaving = ref(false)
const promptDeleting = ref(false)
const knowledgeGraphicPromptForm = reactive({
  id: null,
  prompt: '',
})
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
  // When product id changed, invoke this hook
  getCurrentKnowledgeGraphicState()
  getCurrentKnowledgeGraphic()
  getCurrentKnowledgeGraphForgetState()
  getCurrentKnowledgeGraphForgetEpoch()
  getCurrentKnowledgeGraphNodeLimit()
  getCurrentKnowledgeGraphNodeHardLimit()
  getCurrentKnowledgeGraphicPromptState()
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

function handleKnowledgeGraphForgetEnableToggle() {
  const resultPreFilter = (res) => {
    const { errorCode } = res.data
    if (errorCode === 2001) {
      router.push('/login')
    }
  }
  const data = {
    productId: currentProductId.value,
    value: String(!knowledgeGraphForgetEnable.value),
  }
  knowledgeGraphicForgetToggle(data)
    .then((res) => resultPreFilter(res))
    .then(() => {
      if (knowledgeGraphForgetEnable.value) {
        message.info('已关闭知识图谱遗忘功能')
      } else {
        message.info('已开启知识图谱遗忘功能')
      }
      knowledgeGraphForgetEnable.value = !knowledgeGraphForgetEnable.value
    })
}

function handleKnowledgeGraphForgetEpochChange(value) {
  knowledgeGraphForgetEpoch.value = value
  const resultPreFilter = (res) => {
    const { errorCode } = res.data
    if (errorCode === 2001) {
      router.push('/login')
    }
    return res.data
  }
  const data = {
    productId: currentProductId.value,
    value: knowledgeGraphForgetEpoch.value,
  }
  updateKnowledgeGraphicForgetEpoch(data).then((res) => resultPreFilter(res))
}

function normalizeNodeLimit(value, defaultValue = 100) {
  const numberValue = Number(value)
  if (Number.isNaN(numberValue)) return defaultValue
  return Math.min(600, Math.max(0, Math.round(numberValue)))
}

function handleKnowledgeGraphNodeLimitChange(value) {
  const normalizedValue = normalizeNodeLimit(value)
  knowledgeGraphNodeLimit.value = normalizedValue
  const resultPreFilter = (res) => {
    const { errorCode } = res.data
    if (errorCode === 2001) {
      router.push('/login')
    }
    return res.data
  }
  const data = {
    productId: currentProductId.value,
    value: String(normalizedValue),
  }
  updateKnowledgeGraphicNodeLimit(data).then((res) => resultPreFilter(res))
}

function handleKnowledgeGraphNodeHardLimitChange(value) {
  const normalizedValue = normalizeNodeLimit(value, 300)
  knowledgeGraphNodeHardLimit.value = normalizedValue
  const resultPreFilter = (res) => {
    const { errorCode } = res.data
    if (errorCode === 2001) {
      router.push('/login')
    }
    return res.data
  }
  const data = {
    productId: currentProductId.value,
    value: String(normalizedValue),
  }
  updateKnowledgeGraphicNodeHardLimit(data).then((res) => resultPreFilter(res))
}

function handleStartAddNewNode() {
  newNodeForm.productId = currentProductId.value
  isAddingNode.value = true
}

function handleKnowledgeGraphicPromptToggle() {
  const resultPreFilter = (res) => {
    const { errorCode } = res.data
    if (errorCode === 2001) {
      router.push('/login')
    }
  }
  const data = {
    productId: currentProductId.value,
    value: String(!knowledgeGraphicPromptEnable.value),
  }
  knowledgeGraphicPromptToggle(data)
    .then((res) => resultPreFilter(res))
    .then(() => {
      if (knowledgeGraphicPromptEnable.value) {
        message.info('已关闭图谱提示增强')
        promptModalOpen.value = false
      } else {
        message.info('已开启图谱提示增强')
        fetchProductKnowledgeGraphicPrompt()
      }
      knowledgeGraphicPromptEnable.value = !knowledgeGraphicPromptEnable.value
    })
}

function resetKnowledgeGraphicPromptForm() {
  knowledgeGraphicPromptForm.id = null
  knowledgeGraphicPromptForm.prompt = ''
}

function normalizeKnowledgeGraphicPrompt(data) {
  if (Array.isArray(data) && data.length > 0) return data[0]
  if (data && typeof data === 'object' && !Array.isArray(data)) return data
  return null
}

function fetchProductKnowledgeGraphicPrompt() {
  if (!currentProductId.value) return Promise.resolve()
  promptLoading.value = true
  return getProductKnowledgeGraphicPrompt({ productId: currentProductId.value })
    .then((res) => {
      const { data, errorCode } = res.data
      if (errorCode === 2001) {
        router.push('/login')
        return
      }
      if (errorCode !== 200) {
        resetKnowledgeGraphicPromptForm()
        return
      }
      const promptConfig = normalizeKnowledgeGraphicPrompt(data)
      if (!promptConfig) {
        resetKnowledgeGraphicPromptForm()
        return
      }
      knowledgeGraphicPromptForm.id = promptConfig.id
      knowledgeGraphicPromptForm.prompt = promptConfig.prompt || ''
    })
    .catch(() => {
      resetKnowledgeGraphicPromptForm()
    })
    .finally(() => {
      promptLoading.value = false
    })
}

function handleOpenPromptModal() {
  if (!currentProductId.value || !knowledgeGraphicPromptEnable.value) return
  promptModalOpen.value = true
  fetchProductKnowledgeGraphicPrompt()
}

function handleClosePromptModal() {
  promptModalOpen.value = false
}

function handleSaveKnowledgeGraphicPrompt() {
  const prompt = knowledgeGraphicPromptForm.prompt.trim()
  if (!prompt) {
    message.warning('请输入背景信息及要求')
    return
  }
  promptSaving.value = true
  const hasExistingPrompt = Boolean(knowledgeGraphicPromptForm.id)
  const payload = {
    productId: currentProductId.value,
    prompt,
  }
  const request = hasExistingPrompt
    ? updateProductKnowledgeGraphicPrompt({
        ...payload,
        id: knowledgeGraphicPromptForm.id,
      })
    : addProductKnowledgeGraphicPrompt(payload)
  request
    .then((res) => {
      const { data, errorCode, errorMsg } = res.data
      if (errorCode !== 200) {
        message.error(errorMsg || '保存失败')
        return
      }
      const promptConfig = normalizeKnowledgeGraphicPrompt(data)
      if (promptConfig) {
        knowledgeGraphicPromptForm.id = promptConfig.id
        knowledgeGraphicPromptForm.prompt = promptConfig.prompt || prompt
      }
      message.success(hasExistingPrompt ? '已修改图谱提示增强' : '已设置图谱提示增强')
      promptModalOpen.value = false
    })
    .catch(() => {
      message.error('保存失败')
    })
    .finally(() => {
      promptSaving.value = false
    })
}

function handleDeleteKnowledgeGraphicPrompt() {
  if (!knowledgeGraphicPromptForm.id) {
    resetKnowledgeGraphicPromptForm()
    message.info('当前没有可删除的配置')
    return
  }
  Modal.confirm({
    title: '删除图谱提示增强？',
    content: '删除后，背景信息及要求将不再参与知识图谱生成。',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      promptDeleting.value = true
      try {
        const res = await deleteProductKnowledgeGraphicPrompt({
          id: knowledgeGraphicPromptForm.id,
        })
        const { errorCode, errorMsg } = res.data
        if (errorCode === 200) {
          resetKnowledgeGraphicPromptForm()
          promptModalOpen.value = false
          message.success('已删除图谱提示增强')
        } else {
          message.error(errorMsg || '删除失败')
        }
      } catch (err) {
        message.error('删除失败')
      } finally {
        promptDeleting.value = false
      }
    },
  })
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
      downloadBlob(blob)
      message.success('数据集下载已开始')
      return
    }
    const blob = new Blob([res.data], { type: 'application/json;charset=utf-8' })
    downloadBlob(blob)
    message.success('数据集下载已开始')
  } catch (err) {
    message.error('下载失败，请重试')
  } finally {
    downloadLoading.value = false
  }
}

function downloadBlob(blob) {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `knowledge-graphic-${currentProductId.value}.json`
  link.style.display = 'none'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.setTimeout(() => {
    window.URL.revokeObjectURL(url)
  }, 1000)
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
  const classifiedTypes = graphic.value.nodes.filter(
    (node) => node.category !== undefined && node.category !== null
  ).length
  const TYPES = classifyNodes(graphic.value.nodes, graphic.value.relations) + classifiedTypes
  const categories = []
  for (let i = 0; i < TYPES; i++) {
    categories.push(`type${i}`)
  }
  option.series[0].categories = categories
  option.series[0].data = graphic.value.nodes.map((item, index) => {
    return {
      name: item.name,
      category: item.category,
      symbolSize: 30 + 10 * item.attributes.length,
      x: ((index * 50) % cavWidth.value) + Math.random() * 10,
      y: parseInt(index / 50) * 50 + Math.random() * 10,
      value: 10 * item.attributes.length,
    }
  })
  option.series[0].links = graphic.value.relations.map((item) => {
    return {
      source: item.from,
      target: item.to,
      value: item.name.length,
      // label: {
      //   show: true,
      //   formatter: item.name
      // }
    }
  })
  refreshChartHandler(option)
}

function handleProductIdChange(value) {
  currentProductId.value = value
  afterFetchCurrentProduct()
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
 * This method generated by Deepseek
 * @param nodes
 * @param links
 * @returns {number}
 */
function classifyNodes(nodes, links) {
  // 如果节点为空，直接返回0
  if (nodes.length === 0) return 0

  // 构建节点名称到节点对象的映射
  const nodeMap = new Map()
  const nameToNode = new Map()

  nodes.forEach((node) => {
    nodeMap.set(node.id, node)
    nameToNode.set(node.name, node)
  })

  // 构建邻接表和逆邻接表
  const adjList = new Map() // 出边
  const revAdjList = new Map() // 入边
  const degrees = new Map() // 记录每个节点的总度数（入度+出度）

  // 初始化数据结构
  nodes.forEach((node) => {
    adjList.set(node.name, new Set())
    revAdjList.set(node.name, new Set())
    degrees.set(node.name, 0)
  })

  // 填充邻接表和计算度数
  links.forEach((link) => {
    if (nameToNode.has(link.from) && nameToNode.has(link.to)) {
      // 添加出边
      adjList.get(link.from).add(link.to)
      // 添加入边
      revAdjList.get(link.to).add(link.from)

      // 更新度数
      degrees.set(link.from, degrees.get(link.from) + 1)
      degrees.set(link.to, degrees.get(link.to) + 1)
    }
  })

  // 获取未分类的节点名称
  const getUnclassifiedNodes = () => {
    return Array.from(nameToNode.values())
      .filter((node) => node.category === null || node.category === undefined)
      .map((node) => node.name)
  }

  // 广度优先搜索，获取3步内可达的所有节点
  const getNodesWithinThreeSteps = (startNodeName) => {
    const visited = new Set()
    const queue = [{ node: startNodeName, distance: 0 }]

    while (queue.length > 0) {
      const { node, distance } = queue.shift()

      if (visited.has(node) || distance > 3) continue

      visited.add(node)

      // 添加出边方向的邻居
      for (const neighbor of adjList.get(node) || []) {
        if (!visited.has(neighbor)) {
          queue.push({ node: neighbor, distance: distance + 1 })
        }
      }

      // 添加入边方向的邻居（双向搜索，因为我们要找3次简单路径内的节点）
      // 根据题目描述，应该是考虑所有简单路径，包括入边和出边方向
      for (const neighbor of revAdjList.get(node) || []) {
        if (!visited.has(neighbor)) {
          queue.push({ node: neighbor, distance: distance + 1 })
        }
      }
    }

    return Array.from(visited)
  }

  // 主要分类逻辑
  let categoryId = 0
  let unclassifiedCount = nodes.length

  while (unclassifiedCount > 0) {
    // 获取所有未分类节点
    const unclassifiedNodeNames = getUnclassifiedNodes()

    if (unclassifiedNodeNames.length === 0) break

    // 选择度数最大的未分类节点
    let maxDegree = -1
    let startNodeName = null

    for (const nodeName of unclassifiedNodeNames) {
      const degree = degrees.get(nodeName)
      if (degree > maxDegree || (degree === maxDegree && nodeName < startNodeName)) {
        maxDegree = degree
        startNodeName = nodeName
      }
    }

    if (!startNodeName) break

    // 获取3步内可达的所有节点
    const nodesToClassify = getNodesWithinThreeSteps(startNodeName)

    // 将这些节点标记为当前类别
    for (const nodeName of nodesToClassify) {
      const node = nameToNode.get(nodeName)
      if (node && (node.category === null || node.category === undefined)) {
        node.category = categoryId
        unclassifiedCount--
      }
    }

    categoryId++
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

function getCurrentKnowledgeGraphForgetState() {
  const resultPreFilter = (res) => {
    const { errorCode } = res.data
    if (errorCode === 2001) {
      router.push('/login')
    }
    return res.data
  }
  const params = { productId: currentProductId.value }
  getKnowledgeGraphicForgetState(params)
    .then((res) => resultPreFilter(res))
    .then((res) => {
      const { data, errorCode } = res
      if (errorCode !== 200) {
        message.error('获取知识图谱遗忘功能状态失败！')
        return
      }
      if (data === null) {
        addKnowledgeGraphicForgetToggleConfig(params).then(() => {
          getCurrentKnowledgeGraphForgetState()
        })
      } else {
        knowledgeGraphForgetEnable.value = data.value === 'true'
      }
    })
}

function getCurrentKnowledgeGraphForgetEpoch() {
  const resultPreFilter = (res) => {
    const { errorCode } = res.data
    if (errorCode === 2001) {
      router.push('/login')
    }
    return res.data
  }
  const params = { productId: currentProductId.value }
  getKnowledgeGraphicForgetEpoch(params)
    .then((res) => resultPreFilter(res))
    .then((res) => {
      const { data, errorCode } = res
      if (errorCode !== 200) {
        message.error('获取知识图谱遗忘轮次出错！')
        return
      }
      if (data === null) {
        const createData = {
          productId: currentProductId.value,
          value: 10,
        }
        updateKnowledgeGraphicForgetEpoch(createData)
      } else {
        knowledgeGraphForgetEpoch.value = parseInt(data.value)
      }
    })
}

function getCurrentKnowledgeGraphNodeLimit() {
  const resultPreFilter = (res) => {
    const { errorCode } = res.data
    if (errorCode === 2001) {
      router.push('/login')
    }
    return res.data
  }
  const params = { productId: currentProductId.value }
  getKnowledgeGraphicNodeLimit(params)
    .then((res) => resultPreFilter(res))
    .then((res) => {
      const { data, errorCode } = res
      if (errorCode !== 200) {
        message.error('获取知识图谱期望节点上限出错！')
        return
      }
      if (data === null) {
        const createData = {
          productId: currentProductId.value,
          value: '100',
        }
        knowledgeGraphNodeLimit.value = 100
        updateKnowledgeGraphicNodeLimit(createData)
      } else {
        knowledgeGraphNodeLimit.value = normalizeNodeLimit(data.value)
      }
    })
}

function getCurrentKnowledgeGraphNodeHardLimit() {
  const resultPreFilter = (res) => {
    const { errorCode } = res.data
    if (errorCode === 2001) {
      router.push('/login')
    }
    return res.data
  }
  const params = { productId: currentProductId.value }
  getKnowledgeGraphicNodeHardLimit(params)
    .then((res) => resultPreFilter(res))
    .then((res) => {
      const { data, errorCode } = res
      if (errorCode !== 200) {
        message.error('获取知识图谱节点硬上限出错！')
        return
      }
      if (data === null) {
        const createData = {
          productId: currentProductId.value,
          value: '300',
        }
        knowledgeGraphNodeHardLimit.value = 300
        updateKnowledgeGraphicNodeHardLimit(createData)
      } else {
        knowledgeGraphNodeHardLimit.value = normalizeNodeLimit(data.value, 300)
      }
    })
}

function getCurrentKnowledgeGraphicPromptState() {
  const resultPreFilter = (res) => {
    const { errorCode } = res.data
    if (errorCode === 2001) {
      router.push('/login')
    }
    return res.data
  }
  const params = { productId: currentProductId.value }
  getKnowledgeGraphicPromptState(params)
    .then((res) => resultPreFilter(res))
    .then((res) => {
      const { data, errorCode } = res
      if (errorCode !== 200) {
        message.error('获取图谱提示增强状态失败！')
        return
      }
      if (data === null) {
        addKnowledgeGraphicPromptToggleConfig(params).then(() => {
          getCurrentKnowledgeGraphicPromptState()
        })
      } else {
        knowledgeGraphicPromptEnable.value = data.value === 'true'
        if (knowledgeGraphicPromptEnable.value) {
          fetchProductKnowledgeGraphicPrompt()
        } else {
          resetKnowledgeGraphicPromptForm()
        }
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
          <label class="option-label" for="prompt-toggle"> 图谱提示增强 </label>
          <Switch
            id="prompt-toggle"
            :checked="knowledgeGraphicPromptEnable"
            @click="handleKnowledgeGraphicPromptToggle"
          />
        </div>
        <div v-if="currentProductId !== null && knowledgeGraphicPromptEnable" class="option-item">
          <Button :loading="promptLoading" @click="handleOpenPromptModal">
            <template #icon><EditOutlined /></template>
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
            上传生成图谱
          </Button>
        </div>
        <div class="option-item">
          <Button
            :disabled="currentProductId === null"
            :loading="downloadLoading"
            @click="handleDownloadDataset"
          >
            <template #icon><DownloadOutlined /></template>
            下载数据集
          </Button>
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
        <div v-if="currentProductId !== null" class="option-item repulsion">
          <label class="option-label" for="repulsion">节点斥力大小（边长）</label>
          <Slider
            id="repulsion"
            :max="255"
            width="200"
            :value="repulsion"
            @change="handleRepulsionChange"
          />
        </div>
        <div v-if="currentProductId !== null" class="option-item node-limit">
          <label class="option-label" for="expected-node-limit">
            <Tooltip title="提示给模型的期望节点上限，0 表示不希望自动新增节点">
              <QuestionCircleOutlined />
            </Tooltip>
            期望节点上限
          </label>
          <Slider
            id="expected-node-limit"
            :min="0"
            :max="600"
            :value="knowledgeGraphNodeLimit"
            @change="handleKnowledgeGraphNodeLimitChange"
          />
          <InputNumber
            class="node-limit-input"
            :min="0"
            :max="600"
            :precision="0"
            :value="knowledgeGraphNodeLimit"
            @change="handleKnowledgeGraphNodeLimitChange"
          />
        </div>
        <div v-if="currentProductId !== null" class="option-item node-limit">
          <label class="option-label" for="hard-node-limit">
            <Tooltip title="后端实际写入保护，不会发送给模型，0 表示不允许自动生成新增节点落库">
              <QuestionCircleOutlined />
            </Tooltip>
            节点硬上限
          </label>
          <Slider
            id="hard-node-limit"
            :min="0"
            :max="600"
            :value="knowledgeGraphNodeHardLimit"
            @change="handleKnowledgeGraphNodeHardLimitChange"
          />
          <InputNumber
            class="node-limit-input"
            :min="0"
            :max="600"
            :precision="0"
            :value="knowledgeGraphNodeHardLimit"
            @change="handleKnowledgeGraphNodeHardLimitChange"
          />
        </div>
        <div v-if="currentProductId !== null" class="option-item">
          <label class="option-label" for="forget-toggle"> 知识图谱遗忘开关 </label>
          <Switch
            id="forget-toggle"
            :checked="knowledgeGraphForgetEnable"
            @click="handleKnowledgeGraphForgetEnableToggle"
          />
        </div>
        <div
          v-if="currentProductId !== null && knowledgeGraphForgetEnable"
          class="option-item repulsion"
        >
          <label for="repulsion" class="option-label">
            <Tooltip
              title="对话达到这个轮次后会自动执行一次节点清理（模拟遗忘），随后重新计数。注意！这个值小于4时清理永远不会生效！"
            >
              <QuestionCircleOutlined />
            </Tooltip>
            知识图谱遗忘轮次
          </label>
          <Slider
            id="forget-epoch"
            :min="3"
            :max="40"
            :value="knowledgeGraphForgetEpoch"
            @change="handleKnowledgeGraphForgetEpochChange"
          />
        </div>
      </div>
      <div class="kg-body">
        <canvas id="kg-cav" ref="cavDom">Your browser didn't support canvas.</canvas>
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
      :open="promptModalOpen"
      title="图谱提示增强"
      :confirm-loading="promptSaving"
      ok-text="保存"
      cancel-text="取消"
      @cancel="handleClosePromptModal"
      @ok="handleSaveKnowledgeGraphicPrompt"
    >
      <Form
        :model="knowledgeGraphicPromptForm"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 17 }"
      >
        <Form.Item
          label="背景要求"
          name="prompt"
          :rules="[{ required: true, message: '请输入背景信息及要求' }]"
        >
          <Input.TextArea
            v-model:value="knowledgeGraphicPromptForm.prompt"
            :maxlength="2048"
            :auto-size="{ minRows: 6, maxRows: 10 }"
            show-count
            placeholder="例如：优先抽取设备、场景、故障现象之间的关系；不要把临时对话称呼写入图谱。"
          />
        </Form.Item>
        <Form.Item :wrapper-col="{ offset: 6, span: 17 }">
          <Button
            danger
            :disabled="!knowledgeGraphicPromptForm.id"
            :loading="promptDeleting"
            @click="handleDeleteKnowledgeGraphicPrompt"
          >
            删除
          </Button>
        </Form.Item>
      </Form>
    </Modal>
    <Modal
      :open="isUploadModalOpen"
      title="上传生成知识图谱"
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

.option-item.repulsion {
  width: fit-content;
}

#repulsion {
  width: 200px;
}

#forget-epoch {
  width: 150px;
}

.option-item.node-limit {
  width: fit-content;
}

#expected-node-limit,
#hard-node-limit {
  width: 180px;
}

.node-limit-input {
  width: 76px;
  margin-left: 8px;
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
</style>
