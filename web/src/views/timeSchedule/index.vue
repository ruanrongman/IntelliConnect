<template>
  <div class="model-container">
    <HeaderCard :scheduleData="homeData"/>
    <div class="model-content">
      <div class="model-bar">
        <newSchedule @refresh="fetchTimeSchedule"></newSchedule>
      </div>
      <Mytable @refresh="fetchTimeSchedule"/>
    </div>
  </div>
</template>

<script setup>
import HeaderCard from './HeaderCard.vue'
import newSchedule from './add_schedule.vue'
import Mytable from './Mytable.vue'
import { getTimeSchedule } from '@/api/timeSchedule'
import { reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

let homeData = reactive({
  total: 0,
  enabled: 0,
  disabled: 0
})

let intervalId

const fetchTimeSchedule = () => {
  getTimeSchedule()
    .then((res) => {
      const { data, errorCode } = res.data
      if (errorCode === 2001) {
        router.push('/login')
      } else if (errorCode === 200 && data && Array.isArray(data)) {
        homeData.total = data.length
        homeData.enabled = data.filter(item => item.exec === 'true').length
        homeData.disabled = data.filter(item => item.exec === 'false').length
      } else {
        homeData.total = 0
        homeData.enabled = 0
        homeData.disabled = 0
      }
    })
    .catch(() => {
      // 静默处理
    })
}

onMounted(() => {
  fetchTimeSchedule()
  intervalId = setInterval(fetchTimeSchedule, 5000)
})

onUnmounted(() => {
  clearInterval(intervalId)
})
</script>

<style lang="scss" scoped>
.model-container {
  padding: 24px;
  background-color: #f0f2f5;
  min-height: 100vh;

  .model-content {
    background: #fff;
    border-radius: 12px;
    padding: 24px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

    .model-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
      padding-bottom: 24px;
      border-bottom: 1px solid #f0f0f0;
    }
  }
}
</style>
