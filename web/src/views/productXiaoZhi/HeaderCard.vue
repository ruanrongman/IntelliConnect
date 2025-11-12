<template>
  <div class="header-cards">
    <a-row :gutter="[16, 16]">
      <a-col :span="8">
        <a-card class="metric-card" :bordered="false">
          <template #title>
            <div class="card-title">
              <DesktopOutlined />
              <span>设备数</span>
            </div>
          </template>
          <a-statistic 
            :value= device.num
            :value-style="{ color: '#1890ff' }"
          >
            <template #suffix>
              <span class="suffix-text">台</span>
            </template>
          </a-statistic>
        </a-card>
      </a-col>
      
      <a-col :span="8">
        <a-card class="metric-card" :bordered="false">
          <template #title>
            <div class="card-title">
              <LinkOutlined />
              <span>连接数</span>
            </div>
          </template>
          <a-statistic 
            :value= device.connected
            :value-style="{ color: '#52c41a' }"
          >
            <template #suffix>
              <span class="suffix-text">台</span>
            </template>
          </a-statistic>
        </a-card>
      </a-col>
      
      <a-col :span="8">
        <a-card class="metric-card" :bordered="false">
          <template #title>
            <div class="card-title">
              <DisconnectOutlined />
              <span>断开数</span>
            </div>
          </template>
          <a-statistic 
            :value= device.disconnected
            :value-style="{ color: '#cf1322' }"
          >
            <template #suffix>
              <span class="suffix-text">台</span>
            </template>
          </a-statistic>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup>
import { DesktopOutlined, LinkOutlined, DisconnectOutlined } from '@ant-design/icons-vue'
const props = defineProps({
  device: {
    type: Object,
    required: true
  }
})
</script>

<style lang="scss" scoped>
.header-cards {
  margin-bottom: 24px;

  .metric-card {
    border-radius: 12px;
    transition: all 0.3s;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    height: 160px;
    display: flex;
    flex-direction: column;
    justify-content: space-between;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
    }

    .card-title {
      display: flex;
      align-items: center;
      gap: 8px;
      color: #666;
      font-size: 16px;

      .anticon {
        font-size: 20px;
      }
    }

    .ant-statistic {
      padding: 16px 0;
      
      .ant-statistic-content {
        font-size: 32px;
        font-weight: 600;
        display: flex;
        align-items: baseline;
      }

      .suffix-text {
        font-size: 14px;
        color: #999;
        margin-left: 4px;
      }
    }
  }
}
</style>
