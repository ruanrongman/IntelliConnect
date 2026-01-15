<script setup>
import {onBeforeUpdate, onMounted, ref, watch} from "vue";
import {getNodeInfo} from "@/api/knowledgeGraphic";

const props = defineProps(["nodeName", "productId"]);

const nodeInfo = ref({});

function initNodeInfo(nodeName, productId){
  if(!nodeName) return;
  getNodeInfo({
    name: nodeName,
    productId: productId
  }).then(res=>{
    const { data, errorCode } = res.data;
    console.log(data);
  });
}

watch(() => props.nodeName, (newVal)=>{
  initNodeInfo(props.nodeName, props.productId);
})
</script>

<template>
  <div>
    {{nodeName}}
  </div>
</template>

<style scoped lang="scss">

</style>