<script setup>
import {onMounted, reactive, ref, watch, defineEmits} from "vue";
import {getNodeInfo, getNodeAttributes, addNodeAttribute, deleteKnowledgeGraphicNode, updateKnowledgeGraphicNode, deleteNodeAttribute} from "@/api/knowledgeGraphic";
import {useRouter} from "vue-router";
import {message, Form, Input, Button, Tag, Popconfirm} from "ant-design-vue";
import * as Icons from "@ant-design/icons-vue";

const props = defineProps(["nodeName", "productId"]);
const emit = defineEmits(["updateNode", "deleteNode"]);
const router = useRouter();

const formRef = ref(null);
const nodeInfo = ref({});
const nodeAttributes = ref([]);
const updateNodeInfo = reactive({
  id: -1,
  name: "",
  des: "",
  attributes: []
});
const enableUpdate = ref(false);
const isAddingAttribute = ref(false);
const newAttribute = ref("");

function initNodeInfo(nodeName, productId){
  if(!nodeName) return;
  enableUpdate.value = false;
  formRef.value.clearValidate();
  getNodeInfo({
    name: nodeName,
    productId: productId
  }).then(res=>{
    const { data, errorCode } = res.data;
    if(errorCode === 2001) router.push("/login");
    nodeInfo.value = data;
    Object.assign(updateNodeInfo, data);
    getNodeAttributes({
      productId: productId,
      nodeId: data.id
    }).then(res=>{
      const { data } = res.data;
      nodeAttributes.value = data;
    });
  });
}

function handleEnableUpdate(){
  enableUpdate.value = true;
  Object.assign(updateNodeInfo, nodeInfo.value);
}

function handleStartAddAttribute(){
  isAddingAttribute.value = true;
}

function handleCancelAddAttribute(){
  isAddingAttribute.value = false;
  newAttribute.value = "";
}

function handleSubmitAttribute(){
//   Post attribute and re-get attributes;
  if(newAttribute.value === ""){
    message.warning("新属性为空，不会提交添加");
    return;
  }
  let attributeForm = {
    name: newAttribute.value,
    belong: nodeInfo.value.id,
    productId: props.productId
  }
  addNodeAttribute(attributeForm).then(res=>{
    const { errorCode } = res.data;
    if(errorCode === 2001) router.push("/login");
    if(errorCode === 200) {
      message.success("添加成功！");
      getNodeAttributes({
        productId: props.productId,
        nodeId: nodeInfo.value.id
      }).then(res => {
        const { data } = res.data;
        nodeAttributes.value = data;
        isAddingAttribute.value = false;
        newAttribute.value = "";
      });
    }
  })
}

function handleApplyUpdate(){
  // Post node update
  enableUpdate.value = false;
  isAddingAttribute.value = false;
//   Re-get node and emit event to notify parent that node has been updated
  updateKnowledgeGraphicNode({
    ...updateNodeInfo,
    attributes: []
  }).then(res=>{
    const { errorCode } = res.data;
    if(errorCode === 2001) router.push("/login");
    if(errorCode === 200) {
      message.success("更新成功");
      emit("updateNode");
    }else{
      message.error("更新失败！")
    }
  }).catch(()=>{
    message.error("更新失败！")
  })
}

function handleDeleteNode(){
  let deleteNodeForm = {...nodeInfo.value};
  deleteKnowledgeGraphicNode(deleteNodeForm).then(res=>{
    const { errorCode } = res.data;
    if(errorCode === 20001) router.push("/login");
    if(errorCode === 200) {
      message.success("删除成功");
      emit("deleteNode");
    }
  })
}

function handleCancelUpdate(){
  enableUpdate.value = false;
  isAddingAttribute.value = false;
  Object.assign(updateNodeInfo, nodeInfo.value);
  formRef.value.clearValidate();
}

function handleDeleteNodeAttribute(name){
  deleteNodeAttribute({
    name: name,
    belong: nodeInfo.value.id,
    productId: props.productId
  }).then(res=>{
    const { errorCode } = res.data;
    if(errorCode === 2001){
      router.push("/login");
      return;
    }
    if(errorCode === 200){
      message.success("删除成功");
      getNodeAttributes({
        productId: props.productId,
        nodeId: nodeInfo.value.id
      }).then(res => {
        const { data } = res.data;
        nodeAttributes.value = data;
      });
    }
  })
}

watch(() => props.nodeName, ()=>{
  initNodeInfo(props.nodeName, props.productId);
})

watch(() => props.productId, ()=>{
  initNodeInfo(props.nodeName, props.productId);
})

onMounted(()=>{
  // First initialized
  initNodeInfo(props.nodeName, props.productId);
})

</script>

<template>
  <div class="node-info">
    <div class="info-body">
      <Form
          ref="formRef"
          :model="updateNodeInfo"
          :disabled="!enableUpdate"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16, offset: 2}"
      >
        <Form.Item
            label="节点名称"
            name="name"
            :rules="[{required: true, message:'节点名称不能为空'}]"
        >
          <Input :maxlength="10" v-model:value="updateNodeInfo.name" placeholder="节点名称，不超过10字"/>
        </Form.Item>
        <Form.Item
            label="节点描述"
            name="des"
            :rules="[{required: true, message:'节点描述不能为空'}]"
        >
          <Input.TextArea :maxlength="255" v-model:value="updateNodeInfo.des" placeholder="节点描述，不超过255字"/>
        </Form.Item>
        <Form.Item
            label="属性"
            name="attributes"
        >
          <Tag v-if="enableUpdate && !isAddingAttribute" style="cursor: pointer;margin-bottom: 5px" color="#87d068" @click="handleStartAddAttribute">
            <Icons.PlusCircleFilled style="margin-bottom: 0; margin-right: 2px" />添加属性
          </Tag>
          <div class="attribute-add-form" v-if="isAddingAttribute">
            <Input v-model:value="newAttribute" :maxlength="10" placeholder="不超过10字"/>
            <Button :type="'primary'" @click="handleCancelAddAttribute">取消</Button>
            <Button :type="'primary'" @click="handleSubmitAttribute">提交</Button>
          </div>
          <div class="tags">
            <Tag v-if="!enableUpdate && nodeAttributes.length === 0">该节点无属性</Tag>
            <Tag color="#108ee9" v-for="(item, index) in nodeAttributes" :key="index">
              <Icons.CloseCircleFilled style="margin-right:2px" @click="()=>{handleDeleteNodeAttribute(item.name)}" />{{item.name}}
            </Tag>
          </div>
        </Form.Item>
      </Form>
    </div>
    <div class="info-footer">
      <Button :type="'primary'" @click="enableUpdate ? handleCancelUpdate() : handleEnableUpdate()">
        {{enableUpdate ? '取消' : '更改'}}
      </Button>
      <Button v-if="enableUpdate" :type="'primary'" @click="handleApplyUpdate">提交</Button>
      <Popconfirm
          title="确认删除节点吗？"
          @confirm="handleDeleteNode"
      >
        <Button danger>删除</Button>
      </Popconfirm>
    </div>
  </div>
</template>

<style scoped>
.node-info{
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
}

.info-body{
  position: relative;
  width: 100%;
  height: 100%;
  flex-grow: 1;
}

.attribute-add-form{
  position: relative;
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 10px;
  border: 1px solid #f0f0f0;
  box-sizing: border-box;
  padding: 5px;
  border-radius: 8px;
}

.attribute-add-form *:first-child{
  margin-bottom: 10px;
}

.tags *{
  margin-bottom: 5px;
  text-align: center;
}

.tags span span{
  margin-bottom: 0;
}

.info-footer{
  position: relative;
  width: 100%;
  height: 40px;
  margin-bottom: 20px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  flex-shrink: 0;
  border-top: 1px solid #f0f0f0;
}

.info-footer *:not(:last-child){
  margin-right: 10px;
}
</style>