<template>
  <div class="product-skills-container">
    <HeaderCard :device="homeData"/>
    <div class="product-skills-content">
      <div class="action-bar">
        <add-skill />
      </div>
      <Mytable />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import HeaderCard from './HeaderCard.vue'
import Mytable from './Mytable.vue'
import addSkill from './add_skill.vue'
import { getConnectedNum } from '@/api/connectedNum'
import { useRouter } from 'vue-router'

const router = useRouter()

let homeData = reactive({
  num: 0,
  connected: 0,
  disconnected: 0
})

let intervalId = setInterval(
  function(){
    getConnectedNum().then((res) => {
      const { data, errorCode } = res.data
      if(errorCode == 2001){
        router.push('/login')
      } else if(errorCode == 200){
        const { num, connectedNum, disconnectedNum } = data.data
        homeData.num = num
        homeData.connected = connectedNum
        homeData.disconnected = disconnectedNum
      }
    })
    .catch((err) => {
      console.log(err)
    })
  }, 5000
)

onUnmounted(() => {
  clearInterval(intervalId)
})

onMounted(() => {
  // Initialization logic here
})
</script>

<style lang="scss" scoped>
.product-skills-container {
  padding: 24px;
  background-color: #f0f2f5;
  min-height: 100vh;

  .product-skills-content {
    background: #fff;
    border-radius: 8px;
    padding: 24px;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);

    .action-bar {
      margin-bottom: 24px;
      padding-bottom: 24px;
      border-bottom: 1px solid #f0f0f0;
      display: flex;
      gap: 16px;
    }
  }
}
</style>