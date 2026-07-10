<template>
  <div class="model-center-page">
    <HeaderCard :device="homeData" />

    <section class="workspace-shell">
      <header class="workspace-header">
        <div>
          <h2>物模型配置中心</h2>
          <p v-if="selectedModel">
            {{ selectedModel.name }} · 产品 {{ getProductLabel(selectedModel.productId) }}
          </p>
          <p v-else>选择一个物模型后开始配置属性、功能、设备和事件。</p>
        </div>
        <div class="header-actions">
          <a-button :loading="loading" @click="refreshAll">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
          <a-button :disabled="!selectedModel" @click="openModelEdit">
            编辑当前物模型
          </a-button>
          <a-popconfirm title="确认删除当前物模型？" @confirm="handleDeleteModel">
            <a-button danger :disabled="!selectedModel">
              删除当前物模型
            </a-button>
          </a-popconfirm>
          <a-button type="primary" @click="openCreate('model')">
            <template #icon><PlusOutlined /></template>
            新增物模型
          </a-button>
        </div>
      </header>

      <div class="workspace-body">
        <aside class="model-sidebar">
          <div class="sidebar-search">
            <a-input
              v-model:value="modelKeyword"
              allow-clear
              placeholder="搜索物模型"
            >
              <template #prefix><SearchOutlined /></template>
            </a-input>
          </div>

          <div class="model-list">
            <button
              v-for="model in filteredModels"
              :key="model.id"
              type="button"
              class="model-item"
              :class="{ active: model.id === selectedModelId }"
              @click="selectModel(model.id)"
            >
              <span class="model-name">{{ model.name }}</span>
              <span class="model-meta">{{ getProductLabel(model.productId) }}</span>
            </button>
            <a-empty v-if="!filteredModels.length" description="暂无物模型" />
          </div>

          <nav class="resource-nav">
            <button
              v-for="resource in resourceTypes"
              :key="resource.key"
              type="button"
              class="resource-item"
              :class="{ active: resource.key === activeResource }"
              @click="selectResource(resource.key)"
            >
              <span>{{ resource.label }}</span>
              <a-badge :count="resourceCounts[resource.key] || 0" />
            </button>
          </nav>
        </aside>

        <main class="resource-list-panel">
          <div class="panel-toolbar">
            <div>
              <h3>{{ currentResource.label }}</h3>
              <span>
                {{ currentItems.length }} 条记录
                <em v-if="isDeviceResource" class="auto-refresh-hint">自动刷新中</em>
              </span>
            </div>
            <div class="toolbar-actions">
              <a-input
                v-model:value="itemKeyword"
                allow-clear
                class="item-search"
                placeholder="搜索当前分类"
              >
                <template #prefix><SearchOutlined /></template>
              </a-input>
              <a-button type="primary" :disabled="!selectedModel" @click="openCreate(activeResource)">
                <template #icon><PlusOutlined /></template>
                新增{{ currentResource.label }}
              </a-button>
            </div>
          </div>

          <div class="item-list">
            <button
              v-for="item in filteredCurrentItems"
              :key="item.rowKey"
              type="button"
              class="list-row"
              :class="{ active: item.rowKey === selectedItemKey }"
              @click="selectItem(item.rowKey)"
            >
              <div class="row-main">
                <span class="row-title">{{ getItemTitle(item) }}</span>
                <span class="row-subtitle">{{ getItemSubtitle(item) }}</span>
              </div>
              <div class="row-extra">
                <a-tag v-if="activeResource === 'device'" :color="item.online === 'disconnected' ? 'error' : 'success'">
                  {{ item.online || 'unknown' }}
                </a-tag>
                <a-tag v-else-if="item.type">{{ item.type }}</a-tag>
                <a-tag v-else-if="item.dataType">{{ item.dataType }}</a-tag>
              </div>
            </button>
            <a-empty
              v-if="!filteredCurrentItems.length"
              :description="selectedModel ? '当前分类暂无记录' : '请先选择物模型'"
            />
          </div>
        </main>

        <aside class="detail-panel">
          <template v-if="selectedItem">
            <div class="detail-header">
              <div>
                <span class="detail-type">{{ currentResource.label }}</span>
                <h3>{{ getItemTitle(selectedItem) }}</h3>
              </div>
              <div class="detail-actions">
                <template v-if="editingDetail">
                  <a-button size="small" @click="cancelEdit">取消</a-button>
                  <a-button type="primary" size="small" :loading="savingEdit" @click="submitEdit">保存</a-button>
                </template>
                <template v-else>
                  <a-button size="small" @click="openItemEdit">编辑</a-button>
                  <a-popconfirm title="确认删除这条配置？" @confirm="handleDelete(selectedItem)">
                    <a-button danger size="small">删除</a-button>
                  </a-popconfirm>
                </template>
              </div>
            </div>

            <a-form
              v-if="editingDetail"
              ref="editFormRef"
              :model="editState"
              layout="vertical"
              class="detail-edit-form"
            >
              <template v-if="activeResource === 'data'">
                <a-form-item label="属性名称" name="jsonKey" :rules="requiredRule('请输入属性名称')">
                  <a-input v-model:value="editState.jsonKey" />
                </a-form-item>
                <a-form-item label="属性描述" name="description" :rules="requiredRule('请输入属性描述')">
                  <a-input v-model:value="editState.description" />
                </a-form-item>
                <a-form-item label="读写设置" name="rRw" :rules="requiredRule('请选择读写设置')">
                  <a-select v-model:value="editState.rRw">
                    <a-select-option value="0">只读</a-select-option>
                    <a-select-option value="1">读写</a-select-option>
                  </a-select>
                </a-form-item>
                <a-form-item label="存储类型" name="storageType" :rules="requiredRule('请选择存储类型')">
                  <a-select v-model:value="editState.storageType">
                    <a-select-option value="permanent">永久</a-select-option>
                    <a-select-option value="week">一周</a-select-option>
                    <a-select-option value="never">从不</a-select-option>
                  </a-select>
                </a-form-item>
                <a-form-item label="数据类型" name="type" :rules="requiredRule('请选择数据类型')">
                  <a-select v-model:value="editState.type">
                    <a-select-option value="string">string</a-select-option>
                    <a-select-option value="int">int</a-select-option>
                    <a-select-option value="float">float</a-select-option>
                  </a-select>
                </a-form-item>
                <a-form-item label="最大值" name="max">
                  <a-input v-model:value="editState.max" />
                </a-form-item>
                <a-form-item label="最小值" name="min">
                  <a-input v-model:value="editState.min" />
                </a-form-item>
                <a-form-item label="步长" name="step">
                  <a-input v-model:value="editState.step" />
                </a-form-item>
                <a-form-item label="单位" name="unit">
                  <a-input v-model:value="editState.unit" />
                </a-form-item>
              </template>

              <template v-if="activeResource === 'function'">
                <a-form-item label="功能名称" name="functionName" :rules="requiredRule('请输入功能名称')">
                  <a-input v-model:value="editState.functionName" />
                </a-form-item>
                <a-form-item label="输入/输出" name="dataType" :rules="requiredRule('请选择输入/输出类型')">
                  <a-select v-model:value="editState.dataType">
                    <a-select-option value="input">输入</a-select-option>
                    <a-select-option value="output">输出</a-select-option>
                  </a-select>
                </a-form-item>
                <a-form-item label="参数名称" name="jsonKey" :rules="requiredRule('请输入参数名称')">
                  <a-input v-model:value="editState.jsonKey" />
                </a-form-item>
                <a-form-item label="功能描述" name="description" :rules="requiredRule('请输入功能描述')">
                  <a-input v-model:value="editState.description" />
                </a-form-item>
                <a-form-item label="数据类型" name="type" :rules="requiredRule('请选择数据类型')">
                  <a-select v-model:value="editState.type">
                    <a-select-option value="string">string</a-select-option>
                    <a-select-option value="int">int</a-select-option>
                    <a-select-option value="float">float</a-select-option>
                  </a-select>
                </a-form-item>
                <a-form-item label="最大值" name="max">
                  <a-input v-model:value="editState.max" />
                </a-form-item>
                <a-form-item label="最小值" name="min">
                  <a-input v-model:value="editState.min" />
                </a-form-item>
                <a-form-item label="步长" name="step">
                  <a-input v-model:value="editState.step" />
                </a-form-item>
                <a-form-item label="单位" name="unit">
                  <a-input v-model:value="editState.unit" />
                </a-form-item>
              </template>

              <template v-if="activeResource === 'device'">
                <a-alert
                  type="info"
                  show-icon
                  class="model-lock-alert"
                  message="设备名、clientId 和订阅主题由接入链路使用，这里只允许修改描述和使能。"
                />
                <a-form-item label="设备描述" name="description" :rules="requiredRule('请输入设备描述')">
                  <a-input v-model:value="editState.description" />
                </a-form-item>
                <a-form-item label="使能" name="allow" :rules="requiredRule('请选择使能状态')">
                  <a-select v-model:value="editState.allow">
                    <a-select-option value="0">禁止</a-select-option>
                    <a-select-option value="1">使能</a-select-option>
                  </a-select>
                </a-form-item>
              </template>

              <template v-if="activeResource === 'event'">
                <a-form-item label="事件名称" name="name" :rules="requiredRule('请输入事件名称')">
                  <a-input v-model:value="editState.name" />
                </a-form-item>
                <a-form-item label="事件描述" name="description" :rules="requiredRule('请输入事件描述')">
                  <a-input v-model:value="editState.description" />
                </a-form-item>
              </template>

              <template v-if="activeResource === 'eventData'">
                <a-form-item label="事件参数名称" name="jsonKey" :rules="requiredRule('请输入事件参数名称')">
                  <a-input v-model:value="editState.jsonKey" />
                </a-form-item>
                <a-form-item label="事件参数描述" name="description" :rules="requiredRule('请输入事件参数描述')">
                  <a-input v-model:value="editState.description" />
                </a-form-item>
                <a-form-item label="数据类型" name="type" :rules="requiredRule('请选择数据类型')">
                  <a-select v-model:value="editState.type">
                    <a-select-option value="string">string</a-select-option>
                    <a-select-option value="int">int</a-select-option>
                    <a-select-option value="float">float</a-select-option>
                  </a-select>
                </a-form-item>
              </template>

              <template v-if="activeResource === 'alarm'">
                <a-form-item label="事件名称" name="name" :rules="requiredRule('请选择事件')">
                  <a-select
                    v-model:value="editState.name"
                    :options="currentEventOptions"
                    placeholder="请选择当前物模型下的事件"
                    allow-clear
                  />
                </a-form-item>
              </template>
            </a-form>

            <dl v-else class="detail-list">
              <template v-for="field in detailFields" :key="field.key">
                <dt>{{ field.label }}</dt>
                <dd>{{ formatFieldValue(selectedItem, field) }}</dd>
              </template>
            </dl>
          </template>

          <template v-else-if="selectedModel">
            <div class="detail-header">
              <div>
                <span class="detail-type">当前物模型</span>
                <h3>{{ selectedModel.name }}</h3>
              </div>
              <div class="detail-actions">
                <a-button size="small" @click="openModelEdit">编辑</a-button>
              </div>
            </div>

            <dl class="detail-list">
              <template v-for="field in modelDetailFields" :key="field.key">
                <dt>{{ field.label }}</dt>
                <dd>{{ formatFieldValue(selectedModel, field) }}</dd>
              </template>
            </dl>
          </template>

          <div v-else class="detail-empty">
            <a-empty description="请先选择物模型" />
          </div>
        </aside>
      </div>
    </section>

    <a-modal
      v-model:open="createVisible"
      :title="createTitle"
      :confirm-loading="submitting"
      width="560px"
      @ok="submitCreate"
      @cancel="closeCreate"
    >
      <a-form ref="formRef" :model="formState" layout="vertical">
        <template v-if="createType === 'model'">
          <a-form-item label="名称" name="name" :rules="requiredRule('请输入物模型名称')">
            <a-input v-model:value="formState.name" />
          </a-form-item>
          <a-form-item label="产品" name="productId" :rules="requiredRule('请选择产品')">
            <a-select
              v-model:value="formState.productId"
              :options="productOptions"
              placeholder="请选择产品"
              allow-clear
            />
          </a-form-item>
          <a-form-item label="描述" name="description" :rules="requiredRule('请输入描述')">
            <a-input v-model:value="formState.description" />
          </a-form-item>
        </template>

        <template v-else>
          <a-alert
            v-if="selectedModel"
            type="info"
            show-icon
            class="model-lock-alert"
            :message="`将自动关联到物模型：${selectedModel.name}`"
          />

          <template v-if="createType === 'data'">
            <a-form-item label="属性名称" name="jsonKey" :rules="requiredRule('请输入属性名称')">
              <a-input v-model:value="formState.jsonKey" />
            </a-form-item>
            <a-form-item label="属性描述" name="description" :rules="requiredRule('请输入属性描述')">
              <a-input v-model:value="formState.description" />
            </a-form-item>
            <a-form-item label="读写设置" name="rRw" :rules="requiredRule('请选择读写设置')">
              <a-select v-model:value="formState.rRw">
                <a-select-option value="0">只读</a-select-option>
                <a-select-option value="1">读写</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="存储类型" name="storageType" :rules="requiredRule('请选择存储类型')">
              <a-select v-model:value="formState.storageType">
                <a-select-option value="permanent">永久</a-select-option>
                <a-select-option value="week">一周</a-select-option>
                <a-select-option value="never">从不</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="数据类型" name="type" :rules="requiredRule('请选择数据类型')">
              <a-select v-model:value="formState.type">
                <a-select-option value="string">string</a-select-option>
                <a-select-option value="int">int</a-select-option>
                <a-select-option value="float">float</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="最大值" name="max">
              <a-input v-model:value="formState.max" />
            </a-form-item>
            <a-form-item label="最小值" name="min">
              <a-input v-model:value="formState.min" />
            </a-form-item>
            <a-form-item label="步长" name="step">
              <a-input v-model:value="formState.step" />
            </a-form-item>
            <a-form-item label="单位" name="unit">
              <a-input v-model:value="formState.unit" />
            </a-form-item>
          </template>

          <template v-if="createType === 'function'">
            <a-form-item label="功能名称" name="functionName" :rules="requiredRule('请输入功能名称')">
              <a-input v-model:value="formState.functionName" />
            </a-form-item>
            <a-form-item label="输入/输出" name="dataType" :rules="requiredRule('请选择输入/输出类型')">
              <a-select v-model:value="formState.dataType">
                <a-select-option value="input">输入</a-select-option>
                <a-select-option value="output">输出</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="参数名称" name="jsonKey" :rules="requiredRule('请输入参数名称')">
              <a-input v-model:value="formState.jsonKey" />
            </a-form-item>
            <a-form-item label="功能描述" name="description" :rules="requiredRule('请输入功能描述')">
              <a-input v-model:value="formState.description" />
            </a-form-item>
            <a-form-item label="数据类型" name="type" :rules="requiredRule('请选择数据类型')">
              <a-select v-model:value="formState.type">
                <a-select-option value="string">string</a-select-option>
                <a-select-option value="int">int</a-select-option>
                <a-select-option value="float">float</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="最大值" name="max">
              <a-input v-model:value="formState.max" />
            </a-form-item>
            <a-form-item label="最小值" name="min">
              <a-input v-model:value="formState.min" />
            </a-form-item>
            <a-form-item label="步长" name="step">
              <a-input v-model:value="formState.step" />
            </a-form-item>
            <a-form-item label="单位" name="unit">
              <a-input v-model:value="formState.unit" />
            </a-form-item>
          </template>

          <template v-if="createType === 'device'">
            <a-form-item label="设备名" name="name" :rules="requiredRule('请输入设备名')">
              <a-input v-model:value="formState.name" placeholder="请输入英文" />
            </a-form-item>
            <a-form-item label="clientId" name="clientId" :rules="requiredRule('请输入 clientId')">
              <a-input v-model:value="formState.clientId" />
            </a-form-item>
            <a-form-item label="设备描述" name="description" :rules="requiredRule('请输入设备描述')">
              <a-input v-model:value="formState.description" />
            </a-form-item>
            <a-form-item label="使能" name="allow" :rules="requiredRule('请选择使能状态')">
              <a-select v-model:value="formState.allow">
                <a-select-option value="0">禁止</a-select-option>
                <a-select-option value="1">使能</a-select-option>
              </a-select>
            </a-form-item>
          </template>

          <template v-if="createType === 'event'">
            <a-form-item label="事件名称" name="name" :rules="requiredRule('请输入事件名称')">
              <a-input v-model:value="formState.name" />
            </a-form-item>
            <a-form-item label="事件描述" name="description" :rules="requiredRule('请输入事件描述')">
              <a-input v-model:value="formState.description" />
            </a-form-item>
          </template>

          <template v-if="createType === 'eventData'">
            <a-form-item label="事件参数名称" name="jsonKey" :rules="requiredRule('请输入事件参数名称')">
              <a-input v-model:value="formState.jsonKey" />
            </a-form-item>
            <a-form-item label="事件参数描述" name="description" :rules="requiredRule('请输入事件参数描述')">
              <a-input v-model:value="formState.description" />
            </a-form-item>
            <a-form-item label="数据类型" name="type" :rules="requiredRule('请选择数据类型')">
              <a-select v-model:value="formState.type">
                <a-select-option value="string">string</a-select-option>
                <a-select-option value="int">int</a-select-option>
                <a-select-option value="float">float</a-select-option>
              </a-select>
            </a-form-item>
          </template>

          <template v-if="createType === 'alarm'">
            <a-form-item label="事件名称" name="name" :rules="requiredRule('请选择事件')">
              <a-select
                v-model:value="formState.name"
                :options="currentEventOptions"
                placeholder="请选择当前物模型下的事件"
                allow-clear
              />
            </a-form-item>
          </template>
        </template>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { getConnectedNum } from '@/api/connectedNum'
import { getProduct } from '@/api/product'
import HeaderCard from '@/views/productModel/HeaderCard.vue'
import { deleteProductModel, getproductModel, postProductModel, putProductModel } from '@/api/productModel'
import { deleteProductData, getProductData, postProductData, putProductData } from '@/api/productData'
import { deleteProductFunction, getProductFunction, postProductFunction, putProductFunction } from '@/api/productFunction'
import { deleteProductDevice, getProductDevice, postProductDevice, putProductDevice } from '@/api/device'
import { deleteProductEvent, getProductEvent, postProductEvent, putProductEvent } from '@/api/productEvent'
import { deleteProductEventData, getProductEventData, postProductEventData, putProductEventData } from '@/api/productEventData'
import { deleteAlarmEvent, getAlarmEvent, postAlarmEvent, putAlarmEvent } from '@/api/alarmEvent'

const router = useRouter()
const formRef = ref()
const editFormRef = ref()
const loading = ref(false)
const submitting = ref(false)
const savingEdit = ref(false)
const createVisible = ref(false)
const createType = ref('model')
const editingModel = ref(false)
const editingDetail = ref(false)
const selectedModelId = ref(null)
const selectedItemKey = ref(null)
const activeResource = ref('data')
const modelKeyword = ref('')
const itemKeyword = ref('')

const homeData = reactive({
  num: 0,
  connected: 0,
  disconnected: 0,
})

const store = reactive({
  products: [],
  models: [],
  data: [],
  function: [],
  device: [],
  event: [],
  eventData: [],
  alarm: [],
})

const formState = reactive({})
const editState = reactive({})

const resourceTypes = [
  { key: 'data', label: '属性' },
  { key: 'function', label: '功能' },
  { key: 'device', label: '设备' },
  { key: 'event', label: '事件' },
  { key: 'eventData', label: '事件参数' },
  { key: 'alarm', label: '事件告警' },
]

const formatRw = (value) => {
  if (String(value) === '0') return '只读'
  if (String(value) === '1') return '读写'
  return value
}

const formatAllow = (value) => {
  if (String(value) === '0') return '禁止'
  if (String(value) === '1') return '使能'
  return value
}

const detailFieldMap = {
  data: [
    { key: 'id', label: '属性ID' },
    { key: 'jsonKey', label: '属性名称' },
    { key: 'description', label: '描述' },
    { key: 'storageType', label: '存储类型' },
    { key: 'rRw', label: '读写类型', formatter: formatRw },
    { key: 'type', label: '数据类型' },
    { key: 'max', label: '最大值' },
    { key: 'min', label: '最小值' },
    { key: 'step', label: '步长' },
    { key: 'unit', label: '单位' },
  ],
  function: [
    { key: 'id', label: '功能ID' },
    { key: 'functionName', label: '功能名称' },
    { key: 'dataType', label: '输入/输出' },
    { key: 'jsonKey', label: '参数名称' },
    { key: 'description', label: '描述' },
    { key: 'type', label: '数据类型' },
    { key: 'max', label: '最大值' },
    { key: 'min', label: '最小值' },
    { key: 'step', label: '步长' },
    { key: 'unit', label: '单位' },
  ],
  device: [
    { key: 'id', label: '设备ID' },
    { key: 'name', label: '设备名称' },
    { key: 'password', label: 'mqtt密码' },
    { key: 'modelId', label: '物模型ID' },
    { key: 'clientId', label: 'clientId' },
    { key: 'subscribeTopic', label: '订阅主题' },
    { key: 'description', label: '描述' },
    { key: 'online', label: '在线状态' },
    { key: 'allow', label: '使能', formatter: formatAllow },
  ],
  event: [
    { key: 'id', label: '事件ID' },
    { key: 'name', label: '事件名称' },
    { key: 'description', label: '描述' },
  ],
  eventData: [
    { key: 'id', label: '事件参数ID' },
    { key: 'jsonKey', label: '事件参数名称' },
    { key: 'description', label: '描述' },
    { key: 'type', label: '数据类型' },
  ],
  alarm: [
    { key: 'id', label: '告警ID' },
    { key: 'eventName', label: '关联事件' },
    { key: 'eventId', label: '事件ID' },
  ],
}

const modelDetailFields = [
  { key: 'id', label: '物模型ID' },
  { key: 'name', label: '物模型名称' },
  { key: 'productId', label: '产品', formatter: (value) => getProductLabel(value) },
  { key: 'description', label: '描述' },
]

const deleteMap = {
  data: deleteProductData,
  function: deleteProductFunction,
  device: deleteProductDevice,
  event: deleteProductEvent,
  eventData: deleteProductEventData,
  alarm: deleteAlarmEvent,
}

const postMap = {
  model: postProductModel,
  data: postProductData,
  function: postProductFunction,
  device: postProductDevice,
  event: postProductEvent,
  eventData: postProductEventData,
  alarm: postAlarmEvent,
}

const putMap = {
  model: putProductModel,
  data: putProductData,
  function: putProductFunction,
  device: putProductDevice,
  event: putProductEvent,
  eventData: putProductEventData,
  alarm: putAlarmEvent,
}

const fetchMap = {
  data: getProductData,
  function: getProductFunction,
  device: getProductDevice,
  event: getProductEvent,
  eventData: getProductEventData,
  alarm: getAlarmEvent,
}

const DEVICE_REFRESH_INTERVAL = 3000
let metricIntervalId = null
let deviceIntervalId = null
let deviceRefreshing = false

const requiredRule = (messageText) => [{ required: true, message: messageText }]

const currentResource = computed(() => resourceTypes.find((item) => item.key === activeResource.value) || resourceTypes[0])
const isDeviceResource = computed(() => activeResource.value === 'device')
const selectedModel = computed(() => store.models.find((item) => item.id === selectedModelId.value))
const productOptions = computed(() =>
  store.products.map((item) => ({
    value: item.id,
    label: item.productName || item.name || `产品 ${item.id}`,
  }))
)
const filteredModels = computed(() => {
  const keyword = modelKeyword.value.trim().toLowerCase()
  if (!keyword) return store.models
  return store.models.filter((item) =>
    [item.name, item.id, getProductLabel(item.productId)].some((value) =>
      String(value || '').toLowerCase().includes(keyword)
    )
  )
})
const currentItems = computed(() => getItemsByResource(activeResource.value))
const filteredCurrentItems = computed(() => {
  const keyword = itemKeyword.value.trim().toLowerCase()
  if (!keyword) return currentItems.value
  return currentItems.value.filter((item) =>
    [getItemTitle(item), getItemSubtitle(item), item.description, item.id].some((value) =>
      String(value || '').toLowerCase().includes(keyword)
    )
  )
})
const selectedItem = computed(() => filteredCurrentItems.value.find((item) => item.rowKey === selectedItemKey.value))
const detailFields = computed(() => detailFieldMap[activeResource.value] || [])
const resourceCounts = computed(() =>
  resourceTypes.reduce((result, item) => {
    result[item.key] = getItemsByResource(item.key).length
    return result
  }, {})
)
const createTitle = computed(() => {
  if (createType.value === 'model') return editingModel.value ? '编辑物模型' : '新增物模型'
  const resource = resourceTypes.find((item) => item.key === createType.value)
  return `新增${resource?.label || '配置'}`
})
const currentEventOptions = computed(() =>
  getItemsByResource('event').map((item) => ({
    value: item.name,
    label: item.name,
  }))
)

watch([activeResource, selectedModelId, () => store[activeResource.value]?.length], () => {
  cancelEdit()
  selectedItemKey.value = null
})

const normalizeRows = (list, resourceKey) =>
  (Array.isArray(list) ? list : []).map((item, index) => ({
    ...item,
    rowKey: `${resourceKey}-${item.id ?? index}`,
  }))

const unwrapList = (res) => {
  const { data, errorCode } = res.data
  if (errorCode === 2001) {
    router.push('/login')
    return []
  }
  return errorCode === 200 && Array.isArray(data) ? data : []
}

const refreshConnectedNum = async () => {
  try {
    const res = await getConnectedNum()
    const { data, errorCode } = res.data
    if (errorCode === 2001) {
      router.push('/login')
    } else if (errorCode === 200) {
      const { num, connectedNum, disconnectedNum } = data.data
      homeData.num = num
      homeData.connected = connectedNum
      homeData.disconnected = disconnectedNum
    }
  } catch (err) {
    console.log(err)
  }
}

const refreshBaseData = async () => {
  loading.value = true
  try {
    const [products, models] = await Promise.all([
      getProduct(),
      getproductModel(),
      refreshConnectedNum(),
    ])

    store.products = unwrapList(products)
    store.models = normalizeRows(unwrapList(models), 'model')

    if (!selectedModelId.value || !store.models.some((item) => item.id === selectedModelId.value)) {
      selectedModelId.value = store.models[0]?.id || null
    }
    await refreshModelResources()
    await nextTick()
  } catch (err) {
    console.log(err)
    message.error('加载物模型配置失败')
  } finally {
    loading.value = false
  }
}

const refreshAll = refreshBaseData

const refreshModelResources = async () => {
  cancelEdit()
  if (!selectedModelId.value) {
    resourceTypes.forEach((item) => {
      store[item.key] = []
    })
    selectedItemKey.value = null
    return
  }

  const modelParams = { modelId: selectedModelId.value }
  const [data, functions, devices, events, eventData, alarms] = await Promise.all([
    getProductData(modelParams),
    getProductFunction(modelParams),
    getProductDevice(modelParams),
    getProductEvent(modelParams),
    getProductEventData(modelParams),
    getAlarmEvent(modelParams),
  ])

  store.data = normalizeRows(unwrapList(data), 'data')
  store.function = normalizeRows(unwrapList(functions), 'function')
  store.device = normalizeRows(unwrapList(devices), 'device')
  store.event = normalizeRows(unwrapList(events), 'event')
  store.eventData = normalizeRows(unwrapList(eventData), 'eventData')
  store.alarm = normalizeRows(unwrapList(alarms).map(enrichAlarm), 'alarm')
  await nextTick()
  selectedItemKey.value = null
}

const refreshResource = async (resourceKey) => {
  if (resourceKey === 'model') {
    try {
      const res = await getproductModel()
      store.models = normalizeRows(unwrapList(res), 'model')
      return
    } catch (err) {
      console.log(err)
      message.error('刷新失败')
      return
    }
  }
  if (!selectedModelId.value) return
  const fetcher = fetchMap[resourceKey]
  if (!fetcher) return

  try {
    const res = await fetcher({ modelId: selectedModelId.value })
    const rows = unwrapList(res)
    if (resourceKey === 'alarm') {
      store.alarm = normalizeRows(rows.map(enrichAlarm), 'alarm')
    } else {
      store[resourceKey] = normalizeRows(rows, resourceKey)
    }
    if (selectedItemKey.value && !filteredCurrentItems.value.some((item) => item.rowKey === selectedItemKey.value)) {
      selectedItemKey.value = null
    }
  } catch (err) {
    console.log(err)
    message.error('刷新失败')
  }
}

const refreshDeviceStatus = async () => {
  if (!isDeviceResource.value || !selectedModelId.value || deviceRefreshing) {
    return
  }

  deviceRefreshing = true
  const previousItemKey = selectedItemKey.value

  try {
    const [deviceRes] = await Promise.all([
      getProductDevice({ modelId: selectedModelId.value }),
      refreshConnectedNum(),
    ])
    store.device = normalizeRows(unwrapList(deviceRes), 'device')
    await nextTick()

    if (previousItemKey && filteredCurrentItems.value.some((item) => item.rowKey === previousItemKey)) {
      selectedItemKey.value = previousItemKey
    } else {
      selectedItemKey.value = null
    }
  } catch (err) {
    console.log(err)
  } finally {
    deviceRefreshing = false
  }
}

const stopDeviceAutoRefresh = () => {
  if (deviceIntervalId) {
    clearInterval(deviceIntervalId)
    deviceIntervalId = null
  }
}

const startDeviceAutoRefresh = () => {
  stopDeviceAutoRefresh()
  if (!isDeviceResource.value) {
    return
  }
  refreshDeviceStatus()
  deviceIntervalId = setInterval(refreshDeviceStatus, DEVICE_REFRESH_INTERVAL)
}

const handleDeleteModel = async () => {
  if (!selectedModel.value) {
    message.warning('请先选择物模型')
    return
  }

  try {
    const res = await deleteProductModel({ id: selectedModel.value.id })
    const { errorCode } = res.data
    if (errorCode === 2001) {
      router.push('/login')
    } else if (errorCode === 200) {
      message.success('删除成功')
      selectedModelId.value = null
      selectedItemKey.value = null
      await refreshResource('model')
      selectedModelId.value = store.models[0]?.id || null
      await refreshModelResources()
    } else if (errorCode === 3002) {
      message.warn('删除失败，物模型存在关联配置')
    } else {
      message.error('删除失败')
    }
  } catch (err) {
    console.log(err)
    message.error('删除失败')
  }
}

const enrichAlarm = (item) => {
  const matchedEvent = store.event.find((event) => {
    const eventId = event.id != null && String(event.id) === String(item.eventId)
    const eventName = event.name && String(event.name) === String(item.name || item.eventName || item.eventId)
    return eventId || eventName
  })
  return {
    ...item,
    eventName: item.name || item.eventName || matchedEvent?.name || item.eventId,
    modelId: item.modelId ?? matchedEvent?.modelId,
  }
}

const getProductLabel = (productId) => {
  const product = store.products.find((item) => item.id === productId)
  return product?.productName || product?.name || `产品 ${productId ?? '-'}`
}

const getItemsByResource = (resourceKey) => {
  if (!selectedModelId.value) return []
  return store[resourceKey]
}

const selectModel = async (modelId) => {
  if (selectedModelId.value === modelId) {
    cancelEdit()
    selectedItemKey.value = null
    return
  }
  selectedModelId.value = modelId
  await refreshModelResources()
}

const selectResource = (resourceKey) => {
  activeResource.value = resourceKey
  itemKeyword.value = ''
  selectedItemKey.value = null
}

const selectItem = (rowKey) => {
  cancelEdit()
  selectedItemKey.value = rowKey
}


const getItemTitle = (item) => {
  if (!item) return '-'
  return item.jsonKey || item.functionName || item.name || item.eventName || `#${item.id}`
}

const getItemSubtitle = (item) => {
  if (!item) return ''
  if (activeResource.value === 'device') return item.clientId || item.description || ''
  if (activeResource.value === 'function') return `${item.dataType || '-'} · ${item.description || ''}`
  if (activeResource.value === 'alarm') return `关联事件 ${item.eventName || item.eventId || '-'}`
  return item.description || `ID ${item.id}`
}

const formatFieldValue = (item, field) => {
  const value = item[field.key]
  if (field.formatter) return field.formatter(value, item)
  return value ?? '-'
}

const clearEditState = () => {
  Object.keys(editState).forEach((key) => delete editState[key])
}

const toFormValue = (value) => {
  if (value === undefined || value === null) return value
  return String(value)
}

const buildEditPayload = (type, item) => {
  if (type === 'model') {
    return {
      id: item.id,
      name: item.name,
      productId: item.productId,
      description: item.description,
    }
  }
  if (type === 'data') {
    return {
      id: item.id,
      description: item.description,
      jsonKey: item.jsonKey,
      modelId: selectedModelId.value,
      rRw: toFormValue(item.rRw),
      storageType: item.storageType,
      type: item.type,
      max: item.max,
      min: item.min,
      step: item.step,
      unit: item.unit,
    }
  }
  if (type === 'function') {
    return {
      id: item.id,
      functionName: item.functionName,
      dataType: item.dataType,
      description: item.description,
      jsonKey: item.jsonKey,
      modelId: selectedModelId.value,
      type: item.type,
      max: item.max,
      min: item.min,
      step: item.step,
      unit: item.unit,
    }
  }
  if (type === 'device') {
    return {
      id: item.id,
      allow: toFormValue(item.allow),
      description: item.description,
      modelId: item.modelId,
      clientId: item.clientId,
      name: item.name,
    }
  }
  if (type === 'event') {
    return {
      id: item.id,
      description: item.description,
      name: item.name,
      modelId: selectedModelId.value,
    }
  }
  if (type === 'eventData') {
    return {
      id: item.id,
      description: item.description,
      jsonKey: item.jsonKey,
      modelId: selectedModelId.value,
      type: item.type,
    }
  }
  if (type === 'alarm') {
    return {
      id: item.id,
      name: item.eventName,
      modelId: selectedModelId.value,
    }
  }
  return {}
}

const openModelEdit = () => {
  if (!selectedModel.value) {
    message.warning('请先选择物模型')
    return
  }
  editingModel.value = true
  createType.value = 'model'
  Object.keys(formState).forEach((key) => delete formState[key])
  Object.assign(formState, buildEditPayload('model', selectedModel.value))
  createVisible.value = true
}

const openItemEdit = () => {
  if (!selectedItem.value) return
  clearEditState()
  Object.assign(editState, buildEditPayload(activeResource.value, selectedItem.value))
  editingDetail.value = true
}

const cancelEdit = () => {
  if (!editingDetail.value) return
  editingDetail.value = false
  savingEdit.value = false
  clearEditState()
}

const resetForm = (type) => {
  Object.keys(formState).forEach((key) => delete formState[key])
  if (type === 'model') {
    Object.assign(formState, {
      name: '',
      productId: null,
      description: '',
    })
  } else if (type === 'data') {
    Object.assign(formState, {
      description: '',
      jsonKey: '',
      modelId: selectedModelId.value,
      rRw: '0',
      storageType: '',
      type: '',
      max: null,
      min: null,
      step: null,
      unit: null,
    })
  } else if (type === 'function') {
    Object.assign(formState, {
      functionName: '',
      dataType: '',
      description: '',
      jsonKey: '',
      modelId: selectedModelId.value,
      type: '',
      max: null,
      min: null,
      step: null,
      unit: null,
    })
  } else if (type === 'device') {
    Object.assign(formState, {
      allow: null,
      clientId: '',
      description: '',
      modelId: selectedModelId.value,
      name: '',
    })
  } else if (type === 'event') {
    Object.assign(formState, {
      description: '',
      name: '',
      modelId: selectedModelId.value,
    })
  } else if (type === 'eventData') {
    Object.assign(formState, {
      description: '',
      jsonKey: '',
      modelId: selectedModelId.value,
      type: '',
    })
  } else if (type === 'alarm') {
    Object.assign(formState, {
      description: '',
      name: null,
      modelId: selectedModelId.value,
    })
  }
}

const openCreate = (type) => {
  if (type !== 'model' && !selectedModel.value) {
    message.warning('请先选择物模型')
    return
  }
  editingModel.value = false
  createType.value = type
  resetForm(type)
  createVisible.value = true
}

const closeCreate = () => {
  createVisible.value = false
  editingModel.value = false
}

const submitCreate = async () => {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    const payload = { ...formState }
    if (createType.value !== 'model') {
      payload.modelId = selectedModelId.value
    }

    const submitter = editingModel.value && createType.value === 'model'
      ? putMap.model
      : postMap[createType.value]
    const res = await submitter(payload)
    const { errorCode } = res.data
    if (errorCode === 2001) {
      router.push('/login')
      return
    }
    if (errorCode !== 200) {
      message.error(editingModel.value ? '保存失败' : '创建失败')
      return
    }

    message.success(editingModel.value ? '保存成功' : '创建成功')
    createVisible.value = false
    if (editingModel.value) {
      editingModel.value = false
      await refreshResource('model')
      await refreshModelResources()
    } else {
      await refreshResource(createType.value)
    }
  } catch (err) {
    console.log(err)
    message.error(editingModel.value ? '保存失败' : '创建失败')
  } finally {
    submitting.value = false
  }
}

const submitEdit = async () => {
  try {
    await editFormRef.value?.validate()
  } catch {
    return
  }

  const putFn = putMap[activeResource.value]
  if (!putFn) return

  savingEdit.value = true
  try {
    const payload = {
      ...editState,
      modelId: selectedModelId.value,
    }
    const res = await putFn(payload)
    const { errorCode } = res.data
    if (errorCode === 2001) {
      router.push('/login')
      return
    }
    if (errorCode !== 200) {
      message.error('保存失败')
      return
    }
    const previousKey = selectedItemKey.value
    message.success('保存成功')
    cancelEdit()
    await refreshResource(activeResource.value)
    if (previousKey && filteredCurrentItems.value.some((item) => item.rowKey === previousKey)) {
      selectedItemKey.value = previousKey
    }
    if (activeResource.value === 'event') {
      await refreshResource('alarm')
    }
  } catch (err) {
    console.log(err)
    message.error('保存失败')
  } finally {
    savingEdit.value = false
  }
}

const handleDelete = async (item) => {
  const deleteFn = deleteMap[activeResource.value]
  if (!deleteFn) return

  try {
    const res = await deleteFn({ id: item.id })
    const { errorCode } = res.data
    if (errorCode === 2001) {
      router.push('/login')
    } else if (errorCode === 200) {
      message.success('删除成功')
      await refreshResource(activeResource.value)
    } else if (errorCode === 3002) {
      message.warn('删除失败，存在关联配置')
    } else {
      message.error('删除失败')
    }
  } catch (err) {
    console.log(err)
    message.error('删除失败')
  }
}

watch([activeResource, selectedModelId], () => {
  if (isDeviceResource.value) {
    startDeviceAutoRefresh()
  } else {
    stopDeviceAutoRefresh()
  }
})

onMounted(() => {
  refreshAll()
  metricIntervalId = setInterval(refreshConnectedNum, 5000)
})

onUnmounted(() => {
  if (metricIntervalId) {
    clearInterval(metricIntervalId)
  }
  stopDeviceAutoRefresh()
})
</script>

<style lang="scss" scoped>
.model-center-page {
  min-height: 100vh;
  padding: 24px;
  background: #f0f2f5;
}

.workspace-shell {
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
}

.workspace-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid #f0f0f0;
  padding: 18px 20px;

  h2 {
    margin: 0;
    color: #1f2933;
    font-size: 20px;
    font-weight: 700;
  }

  p {
    margin: 4px 0 0;
    color: #6b7280;
    font-size: 13px;
  }
}

.header-actions,
.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-actions :deep(.ant-btn-primary .anticon),
.toolbar-actions :deep(.ant-btn-primary .anticon) {
  color: #fff;
}

.detail-actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 8px;
}

.workspace-body {
  display: grid;
  grid-template-columns: 260px minmax(360px, 1fr) 320px;
  min-height: 640px;
}

.model-sidebar,
.resource-list-panel,
.detail-panel {
  min-width: 0;
}

.model-sidebar {
  display: flex;
  flex-direction: column;
  border-right: 1px solid #f0f0f0;
  background: #fbfcfd;
}

.sidebar-search {
  padding: 16px;
}

.model-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 280px;
  overflow: auto;
  padding: 0 12px 14px;
}

.model-item,
.resource-item,
.list-row {
  border: 0;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.model-item {
  display: grid;
  gap: 3px;
  border-radius: 6px;
  padding: 10px 12px;

  &:hover,
  &.active {
    background: #e6f4ff;
  }
}

.model-name {
  overflow: hidden;
  color: #1f2933;
  font-size: 14px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-meta {
  overflow: hidden;
  color: #6b7280;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resource-nav {
  display: grid;
  gap: 4px;
  border-top: 1px solid #f0f0f0;
  padding: 14px 12px;
}

.resource-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-radius: 6px;
  padding: 9px 10px;
  color: #374151;

  &:hover,
  &.active {
    background: #f0f7ff;
    color: #1677ff;
    font-weight: 600;
  }
}

.resource-list-panel {
  display: flex;
  flex-direction: column;
  border-right: 1px solid #f0f0f0;
}

.panel-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid #f0f0f0;
  padding: 16px 18px;

  h3 {
    margin: 0;
    color: #1f2933;
    font-size: 18px;
    font-weight: 700;
  }

  span {
    color: #6b7280;
    font-size: 12px;
  }
}

.auto-refresh-hint {
  margin-left: 8px;
  color: #52c41a;
  font-style: normal;
}

.item-search {
  width: 220px;
}

.item-list {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 8px;
  overflow: auto;
  padding: 14px;
}

.list-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  background: #fff;
  padding: 12px 14px;
  transition: border-color 0.2s, box-shadow 0.2s, background 0.2s;

  &:hover,
  &.active {
    border-color: #91caff;
    background: #f8fbff;
    box-shadow: 0 2px 8px rgba(22, 119, 255, 0.08);
  }
}

.row-main {
  display: grid;
  min-width: 0;
  gap: 3px;
}

.row-title,
.row-subtitle {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row-title {
  color: #1f2933;
  font-size: 14px;
  font-weight: 600;
}

.row-subtitle {
  color: #6b7280;
  font-size: 12px;
}

.row-extra {
  flex: 0 0 auto;
}

.detail-panel {
  background: #fff;
  padding: 18px;
}

.detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 16px;

  h3 {
    margin: 4px 0 0;
    color: #1f2933;
    font-size: 18px;
    font-weight: 700;
    word-break: break-all;
  }
}

.detail-type {
  color: #1677ff;
  font-size: 12px;
  font-weight: 600;
}

.detail-list {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  gap: 12px 14px;
  margin: 18px 0 0;

  dt {
    color: #6b7280;
    font-size: 13px;
  }

  dd {
    min-width: 0;
    margin: 0;
    color: #1f2933;
    font-size: 13px;
    word-break: break-all;
  }
}

.detail-edit-form {
  margin-top: 18px;
}

.detail-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 360px;
}

.model-lock-alert {
  margin-bottom: 16px;
}

@media (max-width: 1180px) {
  .workspace-body {
    grid-template-columns: 220px minmax(320px, 1fr);
  }

  .detail-panel {
    grid-column: 1 / -1;
    border-top: 1px solid #f0f0f0;
  }
}

@media (max-width: 820px) {
  .model-center-page {
    overflow-x: auto;
    padding: 12px;
  }

  .workspace-shell,
  .header-cards {
    min-width: 760px;
  }

  .workspace-header,
  .panel-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .header-actions,
  .toolbar-actions {
    flex-wrap: wrap;
  }

  .workspace-body {
    display: flex;
    flex-direction: column;
  }

  .model-sidebar,
  .resource-list-panel {
    border-right: 0;
    border-bottom: 1px solid #f0f0f0;
  }

  .item-search {
    width: 100%;
  }
}
</style>
