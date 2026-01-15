<script setup>
import { reactive, ref, onMounted, onUnmounted} from "vue";
import { useRouter } from "vue-router";
import { Select, Button, Modal, Form, message, Input, Drawer } from "ant-design-vue";
import NodeInfo from "@/views/login/nodeInfo.vue";

import * as echarts from 'echarts/core';
import { TooltipComponent, LegendComponent } from 'echarts/components';
import { GraphChart } from 'echarts/charts';
import { LabelLayout } from 'echarts/features';
import { CanvasRenderer } from 'echarts/renderers';

import { queryKnowledgeGraphic, getProductNodes, addKnowledgeGraphicNode } from "@/api/knowledgeGraphic";
import { getProduct } from "@/api/product";

const router = useRouter();

echarts.use([
  TooltipComponent,
  LegendComponent,
  GraphChart,
  CanvasRenderer,
  LabelLayout
]);

const echart = ref(null);
const baseOption = {
  tooltip: {},
  legend: [
    {
      data: ["Nodes"]
    }
  ],
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
      label: {
        show: true,
        position: 'right',
        formatter: '{b}'
      },
      labelLayout: {
        hideOverlap: true
      },
      scaleLimit: {
        min: 0.4,
        max: 2
      },
      lineStyle: {
        color: 'source',
        curveness: 0.3
      }
    }
  ]
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
const nodes = ref(null);
const datasource = ref({});
const selectedNode = ref(null);
const showNodeInfo = ref(true);

const products = ref([]);
const productLoading = ref(true);
const currentProductId= ref(null);

function fetchProducts(){
  getProduct().then(res=>{
    const { data, errorCode } = res.data;
    if(errorCode === 20001){
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
        getCurrentProductNodes();
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
    getCurrentProductNodes();
  });
  handleStopAddNewNode();
}

function handleNodeClick(params){
  selectedNode.value = params.name;
  console.log(selectedNode.value);
  showNodeInfo.value = true;
}

function getCurrentProductNodes(){
  if(echart){
    echart.value.showLoading();
  }
  // If some node info is displaying, close it
  showNodeInfo.value = false;
  let option = {...baseOption};
  getProductNodes({productId: currentProductId.value}).then(res=>{
    const { data, errorCode } = res.data;
    if(errorCode === 20001){
      router.push("/login");
      return;
    }
    if(errorCode === 200 && data && Array.isArray(data)){
      if(echart){
        echart.value.hideLoading();
      }
      nodes.value = data;
      option.series[0].data = data.map((item, index)=>{
        return {
          id: item.id,
          category: 0,
          name: item.name,
          symbolSize: 40,
          x: index * 100 % cavWidth.value,
          y: index * 100 % 50,
          value: 10
        }
      });
      if(echart) echart.value.setOption(option);
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

        if(nodes.value){
          let option = {...baseOption};
          option.series.data = nodes.value.map((item, index)=>{
            return {
              id: item.id,
              category: 0,
              name: item.name,
              symbolSize: 40,
              x: index * 100 % cavWidth.value,
              y: index * 100 % 50,
              value: 10
            }
          });
          if(echart) echart.value.setOption(option);
        }
      }
    });
    resizeObserver.value.observe(cavDom.value.offsetParent);

    echart.value = echarts.init(cavDom.value);
    echart.value.on('click', function(params) {
      if (params.dataType === 'node') {
        handleNodeClick(params);
      }
    });
  }
}

onMounted(()=>{
  fetchProducts();
  initializeCanvas();
})

onUnmounted(()=>{
  resizeObserver.value.disconnect();
})

const open = ref(true);
</script>

<template>
<div class="wrapper">
  <div class="kg-main">
    <div class="kg-header">
      <div class="option-item">
        <label class="option-label product-label" for="product">产品</label>
        <Select class="option-main product-select" id="product"
                :value="currentProductId"
                :options="products"
                :loading="productLoading"
                v-on:change="getCurrentProductNodes"
        >
        </Select>
      </div>
      <div class="option-item">
        <Button type="primary" :disabled="currentProductId === null" @click="handleStartAddNewNode">添加节点</Button>
      </div>
    </div>
    <div class="kg-body">
      <canvas id="kg-cav" ref="cavDom">Your browser didn't support canvas.</canvas>
    </div>
  </div>
  <Modal
      :visible="isAddingNode"
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
          :rules="[{required: true, message: '请输入节点名称（不超过10字）！'}]"
      >
        <Input v-model:value="newNodeForm.name" />
      </Form.Item>
      <Form.Item
          label="节点描述"
          name="des"
      >
        <Input v-model:value="newNodeForm.des" />
      </Form.Item>
    </Form>
  </Modal>
  <Drawer
      title="节点详情"
      :mask="false"
      v-model:visible="showNodeInfo"
      :width="parseInt(cavWidth / 3)"
  >
    <NodeInfo :node-name="selectedNode" :product-id="currentProductId"></NodeInfo>
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