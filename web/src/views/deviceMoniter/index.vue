<template>
  <a-space direction="vertical" :size="12">
      <a-range-picker
        :show-time="{ format: 'HH:mm' }"
        format="YYYY-MM-DD HH:mm"
        :placeholder="['Start Time', 'End Time']"
        @change="onRangeChange"
        @ok="onRangeOk"
      />
      <a-input v-model:value=formState.jsonKey placeholder="属性" />
      <a-input v-model:value=formState.name  placeholder="设备名称" />
  </a-space>
  <div id="chart"></div>
  <div id ="realTime-chart"></div>
</template>

<script setup>
import { ref, toRaw ,reactive,onUnmounted} from 'vue'
import { useEcharts } from '@/hooks/useEcharts'
import { getDeviceData} from '@/api/deviceData';
import { useRouter } from 'vue-router'
const router = useRouter()
const { setOption: setOption1 } = useEcharts('#chart')
const { setOption: setOption2 } = useEcharts('#realTime-chart')
const formState = reactive({
   jsonKey: "",
   name: "",
   time1: 0,
   time2: 0
});
const realTimeState =reactive({
   jsonKey: "",
   name: "",
   time1: 0,
   time2: 0
});
let option1 = {
  title: {
    text: '设备数据',
  },
  tooltip: {},
  xAxis: {
    data: [],
  },
  yAxis: {},
  series: [
    {
      name: formState.jsonKey,
      type: 'line',
      data: [],
    },
  ],
}
let option2 ={
  title: {
    text: '实时设备数据',
  },
  tooltip: {},
  xAxis: {
    data: [5, 20, 36, 10, 10, 20, 23],
  },
  yAxis: {},
  series: [
    {
      name: realTimeState.jsonKey,
      type: 'line',
      data: [5, 20, 36, 10, 10, 20, 23],
    },
  ],}
setOption1(option1)
setOption2(option2)
const onRangeChange = (value, dateString) => {
    console.log('Selected Time: ', value);
    console.log('Formatted Selected Time: ', dateString);
    let startTimestamp = Date.parse(new Date(dateString[0]).toString());
    let endTimestamp = Date.parse(new Date(dateString[1]).toString());
    formState.time1 = startTimestamp;
    formState.time2 = endTimestamp;
  };
  const onRangeOk = value => {
    //console.log('onOk: ', value);
    getDeviceData(toRaw(formState)).then((res) => {
          const { data ,errorCode,success} = res.data
          if(errorCode==2001){
           router.push('/login')
          }
          if(success==false){
            option1.xAxis.data = [];
            option1.series[0].data = [];
            setOption1(option1)
            return;
          }
          //option.series[0].data.push(data.data.value)
        console.log(data);
        let arrnew = data.map((item,index) => {
            return item.value
        })
        let arrx = data.map((item,index) => {
            return timestampToTime(item.time)
        })
          console.log(arrnew);
          option1.xAxis.data = arrx;
          option1.series[0].data = arrnew;
          option1.series[0].name = formState.jsonKey;
          setOption1(option1)
        })
        .catch((err) => {
          console.log(err);
        })
  };
  let intervalId = setInterval(  //设置定时器，1s更新一次
	function(){
        let startTimestamp = new Date().getTime()-30*1000;
        let endTimestamp = startTimestamp+30*1000;
        realTimeState.jsonKey = formState.jsonKey;
        realTimeState.name = formState.name;
        realTimeState.time1 = startTimestamp;
        realTimeState.time2 = endTimestamp;
        getDeviceData(toRaw(realTimeState)).then((res) => {
          const { data ,errorCode,success} = res.data
          if(errorCode==2001){
           router.push('/login')
          }
          if(success==false){
            option2.xAxis.data = [];
            option2.series[0].data = [];
            setOption2(option2)
            return;
          }
          //option.series[0].data.push(data.data.value)
        console.log(data);
        let arrnew = data.map((item,index) => {
            return item.value
        })
        let arrx = data.map((item,index) => {
            return timestampToTime(item.time)
        })
          console.log(arrnew);
          option2.xAxis.data = arrx;
          option2.series[0].data = arrnew;
          option2.series[0].name = realTimeState.jsonKey;
          setOption2(option2)
        })
        .catch((err) => {
          console.log(err);
        })
	},1000
);
onUnmounted(() => {
  clearInterval(intervalId)
  console.log(`the component is now unmounted.`)
})
  /* 时间戳转换为时间 */
  function timestampToTime(timestamp) {
    timestamp = timestamp ? timestamp : null;
    let date = new Date(timestamp);//时间戳为10位需*1000，时间戳为13位的话不需乘1000
    let Y = date.getFullYear() + '-';
    let M = (date.getMonth() + 1 < 10 ? '0' + (date.getMonth() + 1) : date.getMonth() + 1) + '-';
    let D = (date.getDate() < 10 ? '0' + date.getDate() : date.getDate()) + ' ';
    let h = (date.getHours() < 10 ? '0' + date.getHours() : date.getHours()) + ':';
    let m = (date.getMinutes() < 10 ? '0' + date.getMinutes() : date.getMinutes()) + ':';
    let s = date.getSeconds() < 10 ? '0' + date.getSeconds() : date.getSeconds();
    return Y + M + D + h + m + s;
  }
</script>
<style lang="scss">
#chart {
  // width: 300px;
  background-color: #fff;
  padding: 10px;
  margin-top: 10px;
  width: 100%;
  height: 300px;
}
#realTime-chart {
  // width: 300px;
  background-color: #fff;
  padding: 10px;
  margin-top: 10px;
  width: 100%;
  height: 300px;
}
</style>