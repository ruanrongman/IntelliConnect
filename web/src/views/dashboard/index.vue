<template>
  <div class="dashboard-container">
    <HeaderCard :home="homeData" />

    <a-card class="introduction-card" :bordered="false">
      <div class="platform-header">
        <h2>创万联InteliConnect物联网平台</h2>
        <div class="platform-badge">物联网平台</div>
      </div>

      <div class="platform-content">
        <div class="tech-stack">
          <h3>技术栈</h3>
          <div class="tech-tags">
            <a-tag color="#108ee9">SpringBoot</a-tag>
            <a-tag color="#87d068">Vue3</a-tag>
            <a-tag color="#2db7f5">Agent</a-tag>
          </div>
          <p class="tech-description">
            项目使用主流的前后端技术，并融合了先进的agent技术，可以方便地为硬件开发者提供可靠的agent技术赋能
          </p>
        </div>

        <div class="platform-links">
          <a-button
            type="primary"
            href="https://github.com/ruanrongman/IntelliConnect"
            target="_blank"
          >
            <template #icon><GithubOutlined /></template>
            GitHub
          </a-button>
          <a-button type="link" href="https://wordpress.rslly.top" target="_blank">
            <template #icon><LinkOutlined /></template>
            官方技术论坛
          </a-button>
        </div>
      </div>
    </a-card>

    <a-card class="chart-card" :bordered="false">
      <template #title>
        <div class="chart-header">
          <div class="chart-title">
            <BarChartOutlined />
            <span>Token 使用趋势</span>
            <a-select
              v-model:value="selectedProduct"
              class="trend-product-select"
              aria-label="按产品查看 Token 使用趋势"
              :options="productOptions"
              :loading="productsLoading"
              show-search
              :filter-option="filterProduct"
            />
          </div>
          <div class="chart-actions">
            <a-radio-group v-model:value="chartPeriod" size="small">
              <a-radio-button value="day" title="今天（按小时）">天</a-radio-button>
              <a-radio-button value="week" title="最近 7 天">周</a-radio-button>
              <a-radio-button value="month" title="最近 30 天">月</a-radio-button>
            </a-radio-group>
          </div>
        </div>
      </template>
      <a-alert v-if="productsError" type="warning" message="产品列表加载失败" show-icon>
        <template #action><a-button size="small" @click="loadProducts">重试</a-button></template>
      </a-alert>
      <a-spin :spinning="trendLoading" tip="正在加载 Token 用量">
        <a-alert v-if="trendError" type="error" :message="trendError" show-icon>
          <template #action><a-button size="small" @click="loadTrend">重试</a-button></template>
        </a-alert>
        <template v-if="!trendError">
          <div class="trend-caption"
            >{{ periodDescription }} · 北京时间 · 缓存 Token 已包含在输入中</div
          >
          <a-alert
            v-if="missingUsageCalls > 0"
            type="warning"
            :message="`有 ${missingUsageCalls} 次调用未返回用量，统计可能不完整`"
            show-icon
          />
          <div v-if="!trendLoading && trendData && !hasCalls" class="trend-empty">暂无使用记录</div>
        </template>
        <div v-show="!trendError" ref="trendChart" class="token-trend-chart"></div>
      </a-spin>
    </a-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { GithubOutlined, LinkOutlined, BarChartOutlined } from '@ant-design/icons-vue'
import { Spin as ASpin } from 'ant-design-vue'
import HeaderCard from './HeaderCard.vue'
import echarts from '@/plugins/echarts'
import { getMachineMessage } from '@/api/machineMessage'
import { getProduct } from '@/api/product'
import { getLlmUsageTrend } from '@/api/llmUsage'
import { formatTokens, formatCompactTokens, formatRate } from '@/utils/llmUsage.mjs'
import { useRouter } from 'vue-router'

const router = useRouter()
const chartPeriod = ref('week')
const selectedProduct = ref('all')
const productOptions = ref([{ value: 'all', label: '全部产品' }])
const productsLoading = ref(false)
const productsError = ref(false)
const trendChart = ref(null)
const trendData = ref(null)
const trendLoading = ref(false)
const trendError = ref('')
const periodDescription = computed(
  () =>
    ({
      day: '今天（按小时）',
      week: '最近 7 天',
      month: '最近 30 天',
    }[chartPeriod.value])
)
const missingUsageCalls = computed(() =>
  (trendData.value?.points || []).reduce(
    (sum, point) => sum + Math.max(0, point.calls - point.reportedCalls),
    0
  )
)
const hasCalls = computed(() => (trendData.value?.points || []).some((point) => point.calls > 0))
const filterProduct = (input, option) => option.label.toLowerCase().includes(input.toLowerCase())
let chartInstance
let resizeObserver
let intervalId
let requestSequence = 0
let disposed = false

let homeData = reactive({
  value: 0,
  jvmMemoryUsage: 0,
  SystemCpuUsage: 0,
  memoryUsage: 0,
})

const metrics = [
  { key: 'totalTokens', name: '总 Token', color: '#5470c6' },
  { key: 'promptTokens', name: '输入 Token', color: '#91cc75' },
  { key: 'completionTokens', name: '输出 Token', color: '#fac858' },
  { key: 'cachedTokens', name: '缓存命中 Token', color: '#ee6666' },
  { key: 'cacheCreationTokens', name: '缓存写入 Token', color: '#73c0de' },
]

function renderTrend(points) {
  chartInstance.setOption({
    color: metrics.map((metric) => metric.color),
    grid: {
      top: 36,
      left: 24,
      right: 24,
      bottom: 72,
      // ECharts 6 measures all axis labels against these bounds, including the endpoints.
      outerBoundsMode: 'same',
      outerBoundsContain: 'all',
    },
    legend: { bottom: 0, type: 'scroll', data: metrics.map((metric) => metric.name) },
    tooltip: {
      trigger: 'axis',
      // Plain text rich-text rendering keeps product/API content out of HTML tooltips.
      renderMode: 'richText',
      formatter: (params) => {
        const point = points[params[0]?.dataIndex]
        if (!point) return ''
        return [
          point.date,
          ...metrics.map((metric) => `${metric.name}：${formatTokens(point[metric.key])}`),
          `缓存命中率：${formatRate(point.cacheHitRate)}`,
          `调用：${formatTokens(point.calls)} 次（已返回用量 ${formatTokens(
            point.reportedCalls
          )} 次）`,
        ].join('\n')
      },
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: points.map((point) => point.date),
      axisLabel: {
        formatter: (value) => (chartPeriod.value === 'day' ? value.slice(-5) : value),
        showMinLabel: true,
        showMaxLabel: true,
        alignMinLabel: 'left',
        alignMaxLabel: 'right',
        hideOverlap: true,
      },
    },
    yAxis: {
      type: 'value',
      name: 'Token',
      min: 0,
      minInterval: 1,
      axisLabel: { formatter: formatCompactTokens },
    },
    series: metrics.map((metric) => ({
      id: metric.key,
      name: metric.name,
      type: 'line',
      symbolSize: 6,
      data: points.map((point) => point[metric.key]),
    })),
  })
  chartInstance.resize()
}

async function loadProducts() {
  productsLoading.value = true
  productsError.value = false
  try {
    const { data: result } = await getProduct()
    if (disposed) return
    // The existing product API returns COMMON_FAIL when the user has no products.
    if (!result.success && result.errorCode !== 999) throw new Error(result.errorMsg)
    productOptions.value = [
      { value: 'all', label: '全部产品' },
      ...(result.data || []).map((product) => ({ value: product.id, label: product.productName })),
    ]
  } catch {
    if (!disposed) productsError.value = true
  } finally {
    if (!disposed) productsLoading.value = false
  }
}

async function loadTrend() {
  const sequence = ++requestSequence
  trendLoading.value = true
  trendError.value = ''
  trendData.value = null
  try {
    const { data: result } = await getLlmUsageTrend({
      period: chartPeriod.value,
      ...(selectedProduct.value === 'all' ? {} : { productId: selectedProduct.value }),
    })
    if (disposed || sequence !== requestSequence) return
    if (!result.success || !Array.isArray(result.data?.points)) {
      throw new Error(result.errorMsg || 'Token 用量查询失败')
    }
    trendData.value = result.data
    await nextTick()
    if (!disposed && sequence === requestSequence) renderTrend(result.data.points)
  } catch (error) {
    if (!disposed && sequence === requestSequence) {
      trendError.value = error.message || 'Token 用量查询失败，请稍后重试'
    }
  } finally {
    if (!disposed && sequence === requestSequence) trendLoading.value = false
  }
}

function resizeChart() {
  chartInstance?.resize()
}

watch([chartPeriod, selectedProduct], loadTrend)

onMounted(() => {
  chartInstance = echarts.init(trendChart.value)
  resizeObserver = new ResizeObserver(resizeChart)
  resizeObserver.observe(trendChart.value)
  loadProducts()
  loadTrend()
  intervalId = setInterval(() => {
    getMachineMessage()
      .then((res) => {
        if (disposed) return
        const { data, errorCode } = res.data
        if (errorCode === 2001) router.push('/login')
        if (!data) return
        const { SystemCpuUsage, jvmMemoryUsage, memoryUsage } = data
        Object.assign(homeData, {
          value: SystemCpuUsage,
          SystemCpuUsage,
          jvmMemoryUsage,
          memoryUsage,
        })
      })
      .catch((error) => console.log(error))
  }, 5000)
})

onUnmounted(() => {
  disposed = true
  requestSequence++
  clearInterval(intervalId)
  resizeObserver?.disconnect()
  chartInstance?.dispose()
})
</script>

<style lang="scss">
.dashboard-container {
  padding: 24px;
  background-color: #f0f2f5;
  min-height: 100vh;

  .introduction-card {
    margin-bottom: 24px;
    border-radius: 8px;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);

    .platform-header {
      display: flex;
      align-items: center;
      margin-bottom: 24px;

      h2 {
        margin: 0;
        font-size: 24px;
        font-weight: 600;
        color: #1a1a1a;
      }

      .platform-badge {
        margin-left: 16px;
        padding: 4px 12px;
        background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
        color: white;
        border-radius: 16px;
        font-size: 14px;
      }
    }

    .platform-content {
      .tech-stack {
        margin-bottom: 24px;

        h3 {
          font-size: 16px;
          margin-bottom: 16px;
          color: #1a1a1a;
        }

        .tech-tags {
          margin-bottom: 16px;

          .ant-tag {
            margin-right: 8px;
            padding: 4px 12px;
            border: none;
          }
        }

        .tech-description {
          color: #666;
          line-height: 1.6;
        }
      }

      .platform-links {
        display: flex;
        gap: 16px;

        .ant-btn {
          display: flex;
          align-items: center;
          gap: 8px;
        }
      }
    }
  }

  .chart-card {
    border-radius: 8px;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);

    .chart-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 12px;

      .chart-title {
        display: flex;
        align-items: center;
        flex-wrap: wrap;
        gap: 8px;
        font-size: 16px;
        font-weight: 500;
        color: #1a1a1a;
      }
    }
  }

  .trend-product-select {
    width: 220px;
    max-width: 100%;
  }

  .trend-caption,
  .trend-empty {
    padding: 12px 0;
    color: #666;
    font-size: 13px;
  }

  .trend-empty {
    text-align: center;
  }

  .token-trend-chart {
    background-color: #fff;
    height: 400px;
    width: 100%;
  }
}
</style>
