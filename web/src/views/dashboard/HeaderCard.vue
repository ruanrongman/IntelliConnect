<template>
  <a-row :gutter="16" class="header-cards">
    <a-col :span="6">
      <a-card :bordered="false" class="metric-card">
        <template #title>
          <div class="metric-title">
            <span>CPU 利用率</span>
            <a-tooltip title="当前系统CPU使用率">
              <InfoCircleOutlined />
            </a-tooltip>
          </div>
        </template>
        <div class="metric-content">
          <ThunderboltOutlined class="metric-icon" />
          <a-statistic 
            :value="home.value" 
            :precision="1" 
            suffix="%" 
            :value-style="{ color: getColorByValue(home.value) }"
          />
        </div>
      </a-card>
    </a-col>

    <a-col :span="6">
      <a-card :bordered="false" class="metric-card">
        <template #title>
          <div class="metric-title">
            <span>内存利用率</span>
            <a-tooltip title="当前系统内存使用率">
              <InfoCircleOutlined />
            </a-tooltip>
          </div>
        </template>
        <div class="metric-content">
          <DatabaseOutlined class="metric-icon" />
          <a-statistic 
            :value="home.memoryUsage" 
            :precision="1" 
            suffix="%" 
            :value-style="{ color: getColorByValue(home.memoryUsage) }"
          />
        </div>
      </a-card>
    </a-col>

    <a-col :span="6">
      <a-tooltip :title="`CPU使用率: ${home.SystemCpuUsage}%`">
        <a-card :bordered="false" class="metric-card">
          <template #title>
            <div class="metric-title">
              <span>CPU 仪表盘</span>
            </div>
          </template>
          <div id="cpu-chart" class="gauge-chart"></div>
        </a-card>
      </a-tooltip>
    </a-col>

    <a-col :span="6">
      <a-tooltip :title="`JVM使用率: ${home.jvmMemoryUsage}%`">
        <a-card :bordered="false" class="metric-card">
          <template #title>
            <div class="metric-title">
              <span>JVM 仪表盘</span>
            </div>
          </template>
          <div id="jvm-chart" class="gauge-chart"></div>
        </a-card>
      </a-tooltip>
    </a-col>
  </a-row>
</template>

<script setup>
import { onMounted } from 'vue'
import { useEcharts } from '@/hooks/useEcharts'
import { watch } from 'vue'
import { 
  ThunderboltOutlined, 
  DatabaseOutlined,
  InfoCircleOutlined 
} from '@ant-design/icons-vue'

const props = defineProps({
  home: {
    type: Object,
    required: true
  }
})

const getColorByValue = (value) => {
  if (value >= 80) return '#ff4d4f'
  if (value >= 60) return '#faad14'
  return '#52c41a'
}

//font-size: 13.6667px; color: rgb(96, 98, 102);
  // 使用 useEcharts 钩子初始化第一个图表
  const { setOption: setOption1 } = useEcharts('#jvm-chart')

// 使用 useEcharts 钩子初始化第二个图表
  const { setOption: setOption2 } = useEcharts('#cpu-chart')
  let option = {
    series: [{
      type: 'gauge',
      radius: '100%',
      center: ['50%', '60%'],
      startAngle: 200,
      endAngle: -20,
      min: 0,
      max: 100,
      splitNumber: 5,
      itemStyle: {
        color: '#5470C6',
        shadowColor: 'rgba(0,138,255,0.45)',
        shadowBlur: 10,
        shadowOffsetX: 2,
        shadowOffsetY: 2
      },
      progress: {
        show: true,
        roundCap: true,
        width: 18
      },
      pointer: {
        icon: 'path://M2090.36389,615.30999 L2090.36389,615.30999 C2091.48372,615.30999 2092.40383,616.194028 2092.44859,617.312956 L2096.90698,728.755929 C2097.05155,732.369577 2094.2393,735.416212 2090.62566,735.56078 C2090.53845,735.564269 2090.45117,735.566014 2090.36389,735.566014 L2090.36389,735.566014 C2086.74736,735.566014 2083.81557,732.63423 2083.81557,729.017692 C2083.81557,728.930412 2083.81732,728.84314 2083.82081,728.755929 L2088.2792,617.312956 C2088.32396,616.194028 2089.24407,615.30999 2090.36389,615.30999 Z',
        length: '75%',
        width: 16,
        offsetCenter: [0, '5%']
      },
      axisLine: {
        roundCap: true,
        lineStyle: {
          width: 18
        }
      },
      axisTick: {
        show: false
      },
      splitLine: {
        show: false
      },
      axisLabel: {
        show: false
      },
      title: {
        show: false
      },
      detail: {
        show: false
      },
      data: [{
        value: 0,
        name: 'JVM'
      }]
    }]
  };
  let option2 = {
    ...option,
    series: [{
      ...option.series[0],
      itemStyle: {
        color: '#91CC75',
        shadowColor: 'rgba(0,230,0,0.45)',
        shadowBlur: 10,
        shadowOffsetX: 2,
        shadowOffsetY: 2
      }
    }]
  };

onMounted(() => {
  setOption1(option)
  setOption2(option2)
  console.log(`the component is now mounted.`)
})
watch(
  () => props.home.jvmMemoryUsage,
  (val) => {
    option.series[0].data = [{ value: val, name: 'JVM' }]
    option.series[0].itemStyle.color = getColorByValue(val)
    setOption1(option)
  }
)

watch(
  () => props.home.SystemCpuUsage,
  (val) => {
    option2.series[0].data = [{ value: val, name: 'CPU' }]
    option2.series[0].itemStyle.color = getColorByValue(val)
    setOption2(option2)
  }
)

</script>

<style lang="scss" scoped>
.header-cards {
  margin-bottom: 24px;

  .metric-card {
    border-radius: 8px;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
    transition: all 0.3s;
    height: 200px;
    cursor: pointer;

    &:hover {
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
      transform: translateY(-2px);
    }

    .metric-title {
      display: flex;
      align-items: center;
      gap: 8px;
      color: #666;
      font-size: 16px;
      font-weight: 500;

      .anticon {
        color: #8c8c8c;
        cursor: help;
      }
    }

    .metric-content {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 20px 0;
      height: 100px;

      .metric-icon {
        font-size: 40px;
        color: #8c8c8c;
      }

      .ant-statistic {
        .ant-statistic-content {
          font-size: 42px;
          font-weight: 600;
        }
      }
    }
  }

  .gauge-chart {
    height: 150px;
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  :deep(.ant-tooltip) {
    .ant-tooltip-inner {
      padding: 8px 12px;
      border-radius: 4px;
      font-size: 14px;
      background-color: rgba(0, 0, 0, 0.85);
    }
  }
}
</style>