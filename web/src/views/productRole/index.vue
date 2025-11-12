<template>
  <div class="product-container">
    <HeaderCard :device="homeData"/>
    <div class="product-content">
      <div class="action-bar">
        <product />
      </div>
      <Mytable />
    </div>
  </div>
</template>

<script setup>
import HeaderCard from './HeaderCard.vue'
import product from './product.vue'
import Mytable from './Mytable.vue'
import { getConnectedNum } from '@/api/connectedNum'
import { reactive, onUnmounted } from 'vue'
let homeData = reactive({
      num: 0,
      connected: 0,
      disconnected: 0
    })

let intervalId = setInterval(  //设置定时器，1s更新一次
	function(){
    //option.series[0].data[0] = option.series[0].data[0]+5
    //console.log(option)
		//setOption(option)
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
onUnmounted(() => {
  clearInterval(intervalId)
  console.log(`the component is now unmounted.`)
})
</script>

<style lang="scss" scoped>
.product-container {
  padding: 24px;
  background-color: #f0f2f5;
  min-height: 100vh;

  .product-content {
    background: #fff;
    border-radius: 8px;
    padding: 24px;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);

    .action-bar {
      margin-bottom: 24px;
      padding-bottom: 24px;
      border-bottom: 1px solid #f0f0f0;
    }
  }
}
</style>
