<template>
  <div class="header-cards">
    <a-row :gutter="[16, 16]">
      <a-col :span="8">
        <a-card class="metric-card" :bordered="false">
          <template #title>
            <div class="card-title">
              <AppstoreOutlined />
              <span>设备数</span>
            </div>
          </template>
          <a-statistic 
            :value="30" 
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
              <StarOutlined />
              <span>Star</span>
            </div>
          </template>
          <a-statistic 
            :value="100"
            :value-style="{ color: '#52c41a' }"
          />
        </a-card>
      </a-col>
      
      <a-col :span="8">
        <a-card class="metric-card" :bordered="false">
          <template #title>
            <div class="card-title">
              <PercentageOutlined />
              <span>使用率</span>
            </div>
          </template>
          <a-statistic 
            :value="9.3" 
            :precision="1"
            suffix="%" 
            :value-style="{ color: '#cf1322' }"
          />
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup>
import { AppstoreOutlined, StarOutlined, PercentageOutlined } from '@ant-design/icons-vue'
</script>

<style lang="scss" scoped>
.header-cards {
  margin-bottom: 24px;

  .metric-card {
    border-radius: 8px;
    transition: all 0.3s;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
    }

    .card-title {
      display: flex;
      align-items: center;
      gap: 8px;
      color: #666;
      font-size: 16px;

      .anticon {
        font-size: 18px;
      }
    }

    .ant-statistic {
      .ant-statistic-content {
        font-size: 28px;
        font-weight: 600;
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
