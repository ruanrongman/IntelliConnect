<template>
  <div class="product-llm-model-container">
    <HeaderCard :device="homeData"/>
    <div class="product-llm-model-content">
      <div class="action-bar">
        <productLlmModelForm ref="productLlmModelFormRef" />
      </div>
      <Mytable @edit-record="handleEdit" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import HeaderCard from './HeaderCard.vue'
import productLlmModelForm from './productLlmModel.vue'
import Mytable from './Mytable.vue'
import { getConnectedNum } from '@/api/connectedNum'
import { reactive, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const productLlmModelFormRef = ref()

let homeData = reactive({
      num: 0,
      connected: 0,
      disconnected: 0
    })

let intervalId = setInterval(  //设置定时器，1s更新一次
	function(){
    getConnectedNum().then((res) => {
          const { data ,errorCode} = res.data
          if(errorCode==2001){
           router.push('/login')
          }else if(errorCode==200){
            const {num,connectedNum,disconnectedNum} = data.data
            homeData.num = num
            homeData.connected = connectedNum
            homeData.disconnected = disconnectedNum
            console.log(num)
          }

        })
        .catch((err) => {
          console.log(err)
        })
	},5000
);

// Handle edit event from table
const handleEdit = (record) => {
  if (productLlmModelFormRef.value && productLlmModelFormRef.value.showEditModal) {
    productLlmModelFormRef.value.showEditModal(record)
  }
}

onUnmounted(() => {
  clearInterval(intervalId)
  console.log(`the component is now unmounted.`)
})
</script>

<style lang="scss" scoped>
.product-llm-model-container {
  padding: 24px;
  background-color: #f0f2f5;
  min-height: 100vh;

  .product-llm-model-content {
    background: #fff;
    border-radius: 12px;
    padding: 24px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

    .action-bar {
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