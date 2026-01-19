<script setup>
import {defineProps, defineEmits, withDefaults, onMounted, ref, reactive} from 'vue';
import {useRouter} from "vue-router";
import {getNodeByName, deleteRelation, getRelationByNodes} from "@/api/knowledgeGraphic";

const router = useRouter();
const props = defineProps({
  productId: {
    type: Number
  },
  'from': {
    type: String
  },
  'to': {
    type: String
  },
  createFlag: {
    type: Boolean,
    default: false
  }
});
const emit = defineEmits(['submit', 'cancel']);

const loading = ref(true);
const sourceNode = ref(null);
const targetNode = ref(null);
const relation = ref(null);
const newRelationForm = reactive({
  id: -1,
  from: -1,
  to: -1,
  productId: -1,
  des: ""
})

async function initNodes(){
  const res1 = await getNodeByName({
    productId: props.productId,
    name: props.from
  });
  if(res1.data.errorCode === 2001){
    await router.push("/login");
    return;
  }else if(res1.data.errorCode !== 200){
    emit("cancel", {
      message: "Data error"
    });
    return;
  }
  sourceNode.value = res1.data.data;
  const res2 = await getNodeByName({
    productId: props.productId,
    name: props.to
  });
  if(res2.data.errorCode === 2001){
    await router.push("/login");
    return;
  }else if(res2.data.errorCode !== 200){
    emit("cancel", {
      message: "Data error"
    });
    return;
  }
  targetNode.value = res2.data.data;
}

function initData(){
  loading.value = true;
  initNodes().then(()=>{
    loading.value = false;
    if(props.createFlag){
      Object.assign(newRelationForm, {
        productId: props.productId,
        from: props.from,
        to: props.to,
        des: ""
      });
      return;
    }
    getRelationByNodes({
      productId: props.productId,
      from: props.from,
      to: props.to
    }).then(res=>{
      const {data, errorCode} = res.data;
      if(errorCode === 2001){
        router.push("/login");
        return;
      }
      if(errorCode === 200){
        relation.value = data;
      }
    });
  })
}

onMounted(()=>{
  initData();
})
</script>

<template>
<div>

</div>
</template>

<style scoped lang="scss">

</style>