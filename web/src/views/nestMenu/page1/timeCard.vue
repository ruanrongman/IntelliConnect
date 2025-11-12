<template>
  <div class="time-picker">
    <a-range-picker
      class="custom-range-picker"
      :show-time="{ format: 'HH:mm' }"
      format="YYYY-MM-DD HH:mm"
      :placeholder="['开始时间', '结束时间']"
      @change="onRangeChange"
      @ok="onRangeOk"
    />
  </div>
</template>

<script setup>
const onRangeChange = (value, dateString) => {
  console.log('Selected Time: ', value)
  console.log('Formatted Selected Time: ', dateString)
}

const onRangeOk = value => {
  console.log('onOk: ', value)
}
</script>

<style lang="scss" scoped>
.time-picker {
  .custom-range-picker {
    border-radius: 6px;
    
    :deep(.ant-picker-input) {
      > input {
        font-size: 14px;
      }
    }
    
    &:hover {
      border-color: #40a9ff;
    }
  }
}
</style>