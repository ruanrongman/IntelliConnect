<!-- eslint-disable vue/multi-word-component-names -->
<template>
  <div class="model-container">
    <HeaderCard :schedule-data="scheduleSummary" />

    <div class="model-content">
      <div class="model-bar">
        <NewSchedule @refresh="fetchTimeSchedule" />
      </div>

      <Mytable :schedules="schedules" :loading="loading" @refresh="fetchTimeSchedule" />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getTimeSchedule } from '@/api/timeSchedule'
import HeaderCard from './HeaderCard.vue'
import NewSchedule from './add_schedule.vue'
import Mytable from './Mytable.vue'

const router = useRouter()
const schedules = ref([])
const loading = ref(true)
let intervalId
let requestInFlight = false

const scheduleSummary = computed(() => ({
  total: schedules.value.length,
  command: schedules.value.filter((item) => item.exec === true || item.exec === 'true').length,
  reminder: schedules.value.filter((item) => item.exec === false || item.exec === 'false').length,
}))

const fetchTimeSchedule = async () => {
  if (requestInFlight) return
  requestInFlight = true

  try {
    const response = await getTimeSchedule()
    const { data, errorCode } = response.data
    if (errorCode === 2001) {
      router.push('/login')
      return
    }
    schedules.value = errorCode === 200 && Array.isArray(data) ? data : []
  } catch {
    // 轮询失败时保留当前列表，等待下一次请求恢复。
  } finally {
    loading.value = false
    requestInFlight = false
  }
}

onMounted(() => {
  fetchTimeSchedule()
  intervalId = window.setInterval(fetchTimeSchedule, 1000)
})

onUnmounted(() => {
  window.clearInterval(intervalId)
})
</script>

<style lang="scss" scoped>
.model-container {
  min-height: 100vh;
  padding: 24px;
  background-color: #f0f2f5;

  .model-content {
    padding: 24px;
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 2px 8px rgb(0 0 0 / 6%);

    .model-bar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 24px;
      padding-bottom: 24px;
      border-bottom: 1px solid #f0f0f0;
    }
  }
}

@media (max-width: 768px) {
  .model-container {
    padding: 12px;

    .model-content {
      padding: 16px;
    }
  }
}
</style>
