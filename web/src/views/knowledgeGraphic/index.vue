<script setup>
import { reactive, ref, onMounted, onUnmounted} from "vue";
import { useRouter } from "vue-router";
import { Select, Button, Modal, Form, message, Input, Drawer } from "ant-design-vue";
import NodeInfo from "@/views/knowledgeGraphic/nodeInfo.vue";

import * as echarts from 'echarts';

import { queryKnowledgeGraphic, getProductNodes, addKnowledgeGraphicNode } from "@/api/knowledgeGraphic";
import { getProduct } from "@/api/product";

const router = useRouter();

const echart = ref(null);
const baseOption = {
  tooltip: {},
  series: [
    {
      name: 'Knowledge Graphic',
      type: 'graph',
      layout: 'force',
      force: {
        // 节点之间的斥力，值越大节点越分散，可以设置一个较大的值以避免重叠
        repulsion: 30,
        // 节点之间的边长，力导向图会根据边长调整节点间的距离
        edgeLength: 30,
        // 是否防止节点重叠
        avoidOverlap: true
      },
      data: [],
      links: [],
      categories: [{name: "Nodes"}],
      roam: true,
      roamTrigger: 'global',
      scaleLimit: {
        max: 8,
        min: 0.1
      },
      draggable: true,
      label: {
        show: true,
        position: 'right',
        formatter: '{b}'
      },
      labelLayout: {
        hideOverlap: true
      },
      lineStyle: {
        color: 'source',
        curveness: 0.3
      }
    }
  ],
  thumbnail: {
    width: '200',
    height: '150',
    right: '5',
    bottom: '5',
    windowStyle: {
      color: 'rgba(140, 212, 250, 0.5)',
      borderColor: 'rgba(30, 64, 175, 0.7)',
      opacity: 1
    }
  }
};

const isAddingNode = ref(false);
const newNodeForm = reactive({
  productId: -1,
  name: "",
  des: "",
  attributes: []
})

const cavWidth = ref(300);
const cavHeight = ref(100);
const cavDom = ref(null);
const resizeObserver = ref(null);
const selectedNode = ref(null);
const showNodeInfo = ref(false);
const graphic = ref(null);

const products = ref([]);
const productLoading = ref(true);
const currentProductId= ref(null);

// Variables for handling relation
const isConnection = ref(false);
const relationTemp = ref([]);

function fetchProducts(){
  getProduct().then(res=>{
    const { data, errorCode } = res.data;
    if(errorCode === 2001){
      router.push("/login");
      return;
    }
    if(errorCode === 200 && data && Array.isArray(data)){
      products.value = data.map((item, index)=>{
        return {
          key: item.keyvalue ? item.keyvalue : index,
          value: item.id,
          label: item.productName
        }
      });
      productLoading.value = false;
      currentProductId.value = data.length > 0 ? data[0].id : null;
      if(currentProductId.value !== null){
        getCurrentKnowledgeGraphic();
      }
    } else {
      products.value = [];
      productLoading.value = false;
    }
  });
}

function handleStartAddNewNode(){
  newNodeForm.productId = currentProductId.value;
  isAddingNode.value = true;
}

function handleStopAddNewNode(){
  isAddingNode.value = false;
  newNodeForm.name = "";
  newNodeForm.des = "";
  newNodeForm.attributes = [];
}

function handleAddNewNode(){
  addKnowledgeGraphicNode(newNodeForm).then(()=>{
    message.info("添加成功！");
    getCurrentKnowledgeGraphic();
  });
  handleStopAddNewNode();
}

function handleStartConnecting(e){
  if(e.key !== 'c') return;
  console.log("Start connecting");
  e.preventDefault();
  isConnection.value = true;
  relationTemp.value = [];
}

function handleCancelConnecting(e){
  if(e.key !== 'c') return;
  if(relationTemp.value.length < 2){
    isConnection.value = false;
    relationTemp.value = [];
  }
}

function handleConnected(){
// Add temp link on graphic and config des to post
}

function updateGraphicData(){
  if(!graphic.value) return;
  let option = {...baseOption};
  option.series[0].data = graphic.value.nodes.map((item, index)=>{
    return {
      id: index,
      name: item.name,
      category: 0,
      symbolSize: 30 + 10 * item.attributes.length,
      x: index * 100 % cavWidth.value,
      y: index * 100 % 50,
      value: 30 + item.attributes.length
    }
  });
  option.series[0].links = graphic.value.relations.map(item=>{
    return {
      source: item.from,
      target: item.to
    }
  });
  refreshChartHandler(option)
}

function getCurrentKnowledgeGraphic(){
  if(echart) echart.value.showLoading();
  showNodeInfo.value = false;
  queryKnowledgeGraphic({productId: currentProductId.value}).then(res=>{
    const { data, errorCode } = res.data;
    if(errorCode === 2001) {
      router.push("/login");
      return;
    }
    if(errorCode === 200){
      if(echart) echart.value.hideLoading();
      graphic.value = data;
      updateGraphicData();
    }
  })
}

function handleNodeClick(params){
  if(isConnection.value){
    if(relationTemp.value.length === 0){
      relationTemp.value.push(params.name);
    }else if(relationTemp.value.length === 1){
      relationTemp.value.push(params.name);
      handleConnected();
    }
    return;
  }
  selectedNode.value = params.name;
  showNodeInfo.value = true;
}

function refreshChartHandler(option){
  if(echart.value){
    echart.value.dispose();
  }
  echart.value = echarts.init(cavDom.value);
  echart.value.setOption(option);
  echart.value.on('click', function(params) {
    if (params.dataType === 'node') {
      handleNodeClick(params);
    }
  });
}

function initializeCanvas(){
  if(cavDom.value){
    // Set cav size to parent size
    const W = cavDom.value.offsetParent.offsetWidth;
    const H = cavDom.value.offsetParent.offsetHeight;
    cavDom.value.width = W;
    cavDom.value.height = H;
    cavWidth.value = W;
    cavHeight.value = H;

    resizeObserver.value = new ResizeObserver((entries)=>{
      for(const entry of entries){
        const newW = entry.target.offsetWidth;
        const newH = entry.target.offsetHeight;
        cavDom.value.width = newW;
        cavDom.value.height = newH;
        cavWidth.value = newW;
        cavHeight.value = newH;

        updateGraphicData();
        // refreshChartHandler(option);
      }
    });
    resizeObserver.value.observe(cavDom.value.offsetParent);
    refreshChartHandler(baseOption);
  }
}

onMounted(()=>{
  fetchProducts();
  initializeCanvas();
})

onUnmounted(()=>{
  resizeObserver.value.disconnect();
})
</script>

<template>
<div class="wrapper" tabindex="1" :onKeydown="handleStartConnecting" v-on:keyup="handleCancelConnecting">
  <div class="kg-main">
    <div class="kg-header">
      <div class="option-item">
        <label class="option-label product-label" for="product">产品</label>
        <Select class="option-main product-select" id="product"
                :value="currentProductId"
                :options="products"
                :loading="productLoading"
                v-on:change="getCurrentKnowledgeGraphic"
        >
        </Select>
      </div>
      <div class="option-item">
        <Button :type="'primary'" :disabled="currentProductId === null" @click="handleStartAddNewNode">添加节点</Button>
      </div>
    </div>
    <div class="kg-body">
      <canvas id="kg-cav" ref="cavDom">Your browser didn't support canvas.</canvas>
    </div>
  </div>
  <Modal
      :open="isAddingNode"
      title="添加节点"
      @cancel="handleStopAddNewNode"
      @ok="handleAddNewNode"
  >
<!--  For add node  -->
    <Form :model="newNodeForm"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
    >
      <Form.Item
          label="节点名称"
          name="name"
          :rules="[{required: true, message: '请输入节点名称'}]"
      >
        <Input :maxlength="10" v-model:value="newNodeForm.name" placeholder="节点名称，不超过10字" />
      </Form.Item>
      <Form.Item
          label="节点描述"
          name="des"
          :rules="[{required: true, message: '请输入节点描述'}]"
      >
        <Input.TextArea :maxlength="255" v-model:value="newNodeForm.des"  placeholder="节点描述，不超过255字"/>
      </Form.Item>
    </Form>
  </Modal>
  <Drawer
      title="节点详情"
      :mask="false"
      :open="showNodeInfo"
      :width="parseInt(cavWidth / 3)"
      @close="()=>{showNodeInfo = false;}"
  >
    <NodeInfo
        :node-name="selectedNode"
        :product-id="currentProductId"
        v-on:updateNode="getCurrentKnowledgeGraphic"
        v-on:deleteNode="getCurrentKnowledgeGraphic"
    />
  </Drawer>
</div>
</template>

<style>
.wrapper{
  height: 100%;
}

.kg-main{
  background-color: #fff;
  position: relative;
  width: 100%;
  height: 100%;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  align-items: center;
}

.kg-header{
  position: relative;
  width: 100%;
  height: fit-content;
  display: flex;
  justify-content: flex-start;
  align-items: center;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.option-item{
  display: flex;
  justify-content: space-evenly;
  max-width: 100%;
  min-width: 50px;
  align-items: center;
  padding: 2px;
  margin-right: 20px;
}

.option-label{
  margin-right: 5px;
}

.option-main{
  min-width: 150px;
}

.product-label:before{
  content: "*";
  color: red;
  margin-right: 2px;
}

.kg-body{
  position: relative;
  width: 100%;
  flex-grow: 1;
  overflow: hidden;
}
</style>