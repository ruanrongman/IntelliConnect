<script setup>
import {defineProps, defineEmits, watch, onMounted, ref, reactive} from 'vue';
import {useRouter} from "vue-router";
import {Form, Input, Button, Popconfirm, message} from 'ant-design-vue';

import {getNodeByName, deleteRelation, getRelationByNodes, addRelation, updateRelation} from "@/api/knowledgeGraphic";

const router = useRouter();
const props = defineProps({
  productId: {
    type: Number
  },
  nodeFrom: {
    type: String
  },
  'nodeTo': {
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
const relation = reactive({
  id: -1,
  from: -1,
  to: -1,
  productId: -1,
  des: ""
});
const newRelationForm = reactive({
  id: -1,
  nodeFrom: -1,
  nodeTo: -1,
  productId: -1,
  des: ""
})

async function initNodes(){
  if(!props.nodeFrom || !props.nodeTo) return;
  const res1 = await getNodeByName({
    productId: props.productId,
    name: props.nodeFrom
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
    name: props.nodeTo
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
        nodeFrom: props.nodeFrom,
        nodeTo: props.nodeTo,
        des: `${props.nodeFrom}->${props.nodeTo}`
      });
      return;
    }
    getRelationByNodes({
      productId: props.productId,
      from: sourceNode.value.id,
      to: targetNode.value.id
    }).then(res=>{
      const {data, errorCode} = res.data;
      if(errorCode === 2001){
        router.push("/login");
        return;
      }
      if(errorCode === 200){
        Object.assign(relation, {
          id: data.id,
          productId: props.productId,
          from: props.nodeFrom,
          to: props.nodeTo,
          des: data.des
        });
      }
    });
  })
}

function handleSubmitAdd(){
  addRelation({
    productId: props.productId,
    from: sourceNode.value.id,
    to: targetNode.value.id,
    des: newRelationForm.des
  }).then(res=>{
    const {errorCode, errorMsg} = res.data;
    if(errorCode === 2001){
      router.push("/login");
      return;
    }
    if(errorCode === 200){
      message.success("添加成功");
      emit("submit");
      return;
    }
    emit("cancel", {message: errorMsg});
  })
}

function handleDelete(){
  deleteRelation({
    productId: props.productId,
    from: sourceNode.value.id,
    to: targetNode.value.id,
  }).then(res=>{
    const {errorCode, errorMsg} = res.data;
    if(errorCode === 2001){
      router.push("/login");
      return;
    }
    if(errorCode === 200){
      message.success("删除成功");
      emit("submit");
      return;
    }
    emit("cancel", {message:errorMsg});
  })
}

function handleUpdate(){
  updateRelation({
    ...relation,
    from: sourceNode.value.id,
    to: targetNode.value.id
  }).then(res=>{
    const {errorCode, errorMsg} = res.data;
    if(errorCode === 2001){
      router.push("/login");
      return;
    }
    if(errorCode === 200){
      message.success("更新成功");
      emit("submit");
      return;
    }
    emit("cancel", {message:errorMsg});
  })
}

watch(()=>props.nodeFrom, ()=>{
  initData();
})

watch(()=>props.nodeTo, ()=>{
  initData();
})

onMounted(()=>{
  initData();
})
</script>

<template>
<div class="relation-info">
  <div class="relation-info-body" v-if="createFlag">
    <Form
        :disabled="loading"
        :model="newRelationForm"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16, offset: 2}"
    >
      <Form.Item
          label="源节点"
          name="from"
      >
        <Input :value="newRelationForm.nodeFrom" id="from" disabled />
      </Form.Item>
      <Form.Item
          label="目标节点"
          name="to"
      >
        <Input :value="newRelationForm.nodeTo" id="to" disabled />
      </Form.Item>
      <Form.Item
          label="关系描述"
          name="des"
          :rules="[{required: true, message:'关系描述为必填项'}]"
      >
        <Input.TextArea v-model:value="newRelationForm.des" id="des" :maxlength="255" placeholder="最多255字"/>
      </Form.Item>
    </Form>
  </div>
  <div class="relation-info-body" v-else>
    <Form
        :disabled="loading"
        :model="relation"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16, offset: 2}"
    >
      <Form.Item
          label="源节点"
          name="from"
      >
        <Input :value="relation.from" id="from" disabled />
      </Form.Item>
      <Form.Item
          label="目标节点"
          name="to"
      >
        <Input :value="relation.to" id="to" disabled />
      </Form.Item>
      <Form.Item
          label="关系描述"
          name="des"
          :rules="[{required: true, message:'关系描述为必填项'}]"
      >
        <Input.TextArea v-model:value="relation.des" id="des" :maxlength="255" placeholder="最多255字"/>
      </Form.Item>
    </Form>
  </div>
  <div class="relation-info-footer">
    <Popconfirm
        v-if="!createFlag"
        title="确认删除节点吗？"
        @confirm="handleDelete"
    >
      <Button danger>删除</Button>
    </Popconfirm>
    <Button v-if="!createFlag" :type="'primary'" @click="handleUpdate">更新关系</Button>
    <Button v-if="createFlag" :type="'primary'" @click="handleSubmitAdd">提交</Button>
  </div>
</div>
</template>

<style scoped>
.relation-info{
  position: relative;
  height: 100%;
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
}

.relation-info-body{
  width: 100%;
  flex-grow: 1;
}

.relation-info-footer{
  width: 100%;
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.relation-info-footer *:not(:last-child){
  margin-right: 10px;
}
</style>