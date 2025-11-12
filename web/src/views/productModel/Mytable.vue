<template>
  <a-table :columns="columns" :data-source="dataSource" :pagination="pagination">
    <template #action="{ record }">
        <div class="action-buttons">
          <a-button 
            type="link" 
            danger
            @click="handleDelete(record)"
            class="action-button"
          >
            <template #icon><DeleteOutlined /></template>
            删除信息
          </a-button>
        </div>
      </template>
  </a-table>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { getproductModel,deleteProductModel } from '@/api/productModel';
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
const router = useRouter()

const pagination = {
  pageSize: 5,
};

const dataSource = ref([]);

const columns = [
  {
    title: '物模型id',
    dataIndex: 'id',
    key: 'id',
  },
  {
    title: '物模型名称',
    dataIndex: 'name',
    key: 'name',
  },
  {
    title: '功能',
    dataIndex: 'func',
    key: 'func',
  },
  {
    title: '产品id',
    dataIndex: 'productId',
    key: 'productId',
  },
  {
    title: 'Action',
    key: 'action',
    slots: { customRender: 'action' },
  },
];

let intervalId;

onMounted(() => {
  fetchproductModel();
  intervalId = setInterval(fetchproductModel, 1000); // 每 60 秒钟刷新一次数据
});

onUnmounted(() => {
  clearInterval(intervalId);
});

const fetchproductModel = () => {
  getproductModel()
    .then((res) => {
      const { data, errorCode } = res.data;
      if(errorCode==2001){
        router.push('/login')
      }
      if(errorCode==200&& data && Array.isArray(data)){
        dataSource.value = data.map((item, index) => ({
          key: index,
          id: item.id,
          name: item.name,
          func: item.description,
          productId: item.productId,
        }));
      } else {
        // 当没有数据时，设置为空数组
        dataSource.value = [];
      }
    })
    .catch((err) => {
      console.log(err);
    });
};

const handleDelete = (record) => {
  // 实现删除功能
  console.log('Deleting record:', record);
  deleteProductModel({id:record.id}).then((res) => {
      const { data, errorCode } = res.data;
      if(errorCode==200){
        message.success("删除成功")
      }else if(errorCode==3002){
        message.warn("删除失败，物模型被设备绑定")
      }else if(errorCode==2001){
        router.push('/login')
      }else{
        message.error("删除失败")
      }
      console.log(data)
    })
    .catch((err) => {
      console.log(err);
    });
};
</script>