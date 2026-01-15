<script setup>
import { reactive, ref, onMounted, onUnmounted} from "vue";
import { useRouter } from "vue-router";
import { Select, Button, Modal, Form, message, Input } from "ant-design-vue";

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

const isAddingNode = ref(false);
const newNodeForm = reactive({
  productUid: -1,
  name: "",
  des: "",
  attributes: []
})

const cavDom = ref(null);
const resizeObserver = ref(null);
const nodes = ref(null);
const datasource = ref({});

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
  newNodeForm.productUid = currentProductId.value;
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
    message.open("添加成功！");
    handleStopAddNewNode();
  });
}

function getCurrentProductNodes(){
  if(echart){
    echart.value.showLoading();
  }
  let option = {
    tooltip: {},
    legend: [
      {
        data: []
      }
    ],
    series: [
      {
        name: 'Knowledge Graphic',
        type: 'graph',
        layout: 'none',
        data: [],
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
  getProductNodes({productUid: currentProductId.value}).then(res=>{
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
      option.series[0].data = data.map(item=>{
        return {
          id: item.id,
          name: item.name,
          symbolSize: 10
        }
      })
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

    resizeObserver.value = new ResizeObserver((entries)=>{
      for(const entry of entries){
        const newW = entry.target.offsetWidth;
        const newH = entry.target.offsetHeight;
        cavDom.value.width = newW;
        cavDom.value.height = newH;
      }
    });
    resizeObserver.value.observe(cavDom.value.offsetParent);

    echart.value = echarts.init(cavDom.value);
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