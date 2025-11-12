<template>
  <div class="dashboard-container">
    <HeaderCard :home="homeData"/>
    
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
          <a-button type="primary" href="https://github.com/ruanrongman/IntelliConnect" target="_blank">
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
            <span>访问趋势</span>
          </div>
          <div class="chart-actions">
            <a-radio-group v-model:value="chartPeriod" size="small">
              <a-radio-button value="week">周</a-radio-button>
              <a-radio-button value="month">月</a-radio-button>
              <a-radio-button value="year">年</a-radio-button>
            </a-radio-group>
          </div>
        </div>
      </template>
      <div id="views-chart"></div>
    </a-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted,onUnmounted } from 'vue'
import { GithubOutlined, LinkOutlined, BarChartOutlined } from '@ant-design/icons-vue'
import HeaderCard from './HeaderCard.vue'
import { useEcharts } from '@/hooks/useEcharts'
import { getMachineMessage } from '@/api/machineMessage'
import { useRouter } from 'vue-router'

const router = useRouter()
const chartPeriod = ref('week')
const { setOption } = useEcharts('#views-chart')

let homeData = reactive({
      value: 0,
      jvmMemoryUsage: 0,
      SystemCpuUsage: 0,
      memoryUsage: 0,
    })

let option = {
  title: {
    text: '访问趋势',
  },
  tooltip: {},
  legend: {
    data: ['每日访问量'],
  },
  xAxis: {
    data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
  },
  yAxis: {},
  series: [
    {
      name: '每日访问量',
      type: 'line',
      data: [5, 20, 36, 10, 10, 20, 23],
    },
  ],
}

onMounted(() => {
  setOption(option)
  console.log(`the component is now mounted.`)
})
let intervalId = setInterval(  //设置定时器，1s更新一次
	function(){
    //option.series[0].data[0] = option.series[0].data[0]+5
    //console.log(option)
		//setOption(option)
    getMachineMessage().then((res) => {
          const { data ,errorCode} = res.data
          if(errorCode==2001){
           router.push('/login')
          }
          const {SystemCpuUsage,jvmMemoryUsage,memoryUsage} = data
          
          homeData.value = SystemCpuUsage
          homeData.SystemCpuUsage = SystemCpuUsage
          homeData.jvmMemoryUsage = jvmMemoryUsage
          homeData.memoryUsage = memoryUsage
        })
        .catch((err) => {
          console.log(err)
        })
	},5000
);
onUnmounted(() => {
  clearInterval(intervalId)
  console.log(`the component is now unmounted.`)
})
//console.log(getDevice())
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

      .chart-title {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 16px;
        font-weight: 500;
        color: #1a1a1a;
      }
    }
  }

  #views-chart {
    background-color: #fff;
    padding: 24px;
    height: 400px;
    width: 100%;
  }
}
</style>
