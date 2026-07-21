<template>
  <div class="header-cards">
    <a-row :gutter="[16, 16]">
      <a-col :span="8">
        <a-card class="metric-card" :bordered="false">
          <template #title>
            <div class="card-title">
              <CalendarOutlined />
              <span>任务总数</span>
            </div>
          </template>
          <a-statistic :value="scheduleData.total" :value-style="{ color: '#1890ff' }">
            <template #suffix>
              <span class="suffix-text">个</span>
            </template>
          </a-statistic>
        </a-card>
      </a-col>

      <a-col :span="8">
        <a-card class="metric-card" :bordered="false">
          <template #title>
            <div class="card-title">
              <ThunderboltOutlined />
              <span>执行命令</span>
            </div>
          </template>
          <a-statistic :value="scheduleData.command" :value-style="{ color: '#52c41a' }">
            <template #suffix>
              <span class="suffix-text">个</span>
            </template>
          </a-statistic>
        </a-card>
      </a-col>

      <a-col :span="8">
        <a-card class="metric-card" :bordered="false">
          <template #title>
            <div class="card-title">
              <BellOutlined />
              <span>仅提醒</span>
            </div>
          </template>
          <a-statistic :value="scheduleData.reminder" :value-style="{ color: '#cf1322' }">
            <template #suffix>
              <span class="suffix-text">个</span>
            </template>
          </a-statistic>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup>
import { BellOutlined, CalendarOutlined, ThunderboltOutlined } from '@ant-design/icons-vue'

defineProps({
  scheduleData: {
    type: Object,
    required: true,
  },
})
</script>

<style lang="scss" scoped>
.header-cards {
  margin-bottom: 24px;

  .metric-card {
    display: flex;
    height: 160px;
    flex-direction: column;
    justify-content: space-between;
    border-radius: 12px;
    box-shadow: 0 2px 8px rgb(0 0 0 / 6%);
    transition: all 0.3s;

    &:hover {
      box-shadow: 0 4px 16px rgb(0 0 0 / 10%);
      transform: translateY(-2px);
    }

    .card-title {
      display: flex;
      align-items: center;
      color: #666;
      font-size: 16px;
      gap: 8px;

      .anticon {
        font-size: 20px;
      }
    }

    .ant-statistic {
      padding: 16px 0;

      .ant-statistic-content {
        display: flex;
        align-items: baseline;
        font-size: 32px;
        font-weight: 600;
      }

      .suffix-text {
        margin-left: 4px;
        color: #999;
        font-size: 14px;
      }
    }
  }
}
</style>
