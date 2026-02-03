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
  Drawer,
  Switch,
  Tooltip,
  Slider,
} from 'ant-design-vue'
import { QuestionCircleOutlined } from '@ant-design/icons-vue'
import NodeInfo from '@/views/knowledgeGraphic/nodeInfo.vue'
import RelationConfig from '@/views/knowledgeGraphic/relationConfig.vue'

import * as echarts from 'echarts'

import {
  queryKnowledgeGraphic,
  addKnowledgeGraphicNode,
  enableKnowledgeGraphic,
  disabledKnowledgeGraphic,
  getKnowledgeGraphicState,
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
        getCurrentKnowledgeGraphicState()
        getCurrentKnowledgeGraphic()
      }
    } else {
      products.value = []
      productLoading.value = false
    }
  })
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
  getCurrentKnowledgeGraphicState()
  getCurrentKnowledgeGraphic()
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
    knowledgeGraphicEnable.value = data === null
  })
}

onMounted(() => {
  fetchProducts()
  initializeCanvas()
  registerGlobalEvent()
})

onUnmounted(() => {
  resizeObserver.value.disconnect()
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
        <div v-if="currentProductId !== null" class="option-item">
          <label class="option-label" for="connect-toggle"> 知识图谱自动生成开关 </label>
          <Switch
            id="connect-toggle"
            :checked="knowledgeGraphicEnable"
            @click="handleKnowledgeGraphicEnableToggle"
          />
        </div>
        <div class="option-item">
          <Button
            :type="'primary'"
            :disabled="currentProductId === null"
            @click="handleStartAddNewNode"
            >添加节点</Button
          >
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
        <div class="option-item">
          <Button
            :type="'primary'"
            :disabled="!currentProductId"
            @click="getCurrentKnowledgeGraphic"
            >手动刷新数据</Button
          >
        </div>
        <div v-if="currentProductId !== null" class="option-item repulsion">
          <label for="repulsion">节点斥力大小（边长）</label>
          <Slider
            id="repulsion"
            :max="255"
            width="200"
            :value="repulsion"
            @change="handleRepulsionChange"
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
              :maxlength="10"
              placeholder="节点名称，不超过10字"
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

.kg-body {
  position: relative;
  width: 100%;
  flex-grow: 1;
  overflow: hidden;
}
</style>
