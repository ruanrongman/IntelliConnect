<template>
  <svg aria-hidden="true" :style="getSize">
    <use :href="symbolId" :fill="color" />
  </svg>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  prefix: {
    type: String,
    default: 'icon',
  },
  size: {
    type: [Number, String],
    default: 16,
  },
  name: {
    type: String,
    required: true,
  },
  color: {
    type: String,
    default: '#333',
  },
})
const getSize = computed(() => {
  console.log(props.size)
  return {
    width: props.size,
    height: props.size,
  }
})

const symbolId = computed(() => `#${props.prefix}-${props.name}`)
</script>
