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
import { message } from 'ant-design-vue'
import { getProductEvent,deleteProductEvent } from '@/api/ProductEvent';
import { useRouter } from 'vue-router'
const router = useRouter()

const pagination = {
  pageSize: 5,
};

const dataSource = ref([]);

const columns = [
  {
    title: '事件id',
    dataIndex: 'id',
    key: 'id',
  },
  {
    title: '事件名称',
    dataIndex: 'name',
    key: 'name',
  },
  {
    title: '事件描述',
    dataIndex: 'description',
    key: 'description',
  },
  {
    title: '物模型id',
    dataIndex: 'modelId',
    key: 'modelId',
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
  getProductEvent()
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
          description: item.description,
          modelId: item.modelId,
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
  deleteProductEvent({id:record.id}).then((res) => {
      const { data, errorCode } = res.data;
      if(errorCode==200){
        message.success("删除成功")
      }else if(errorCode==3002){
        message.warn("删除失败，事件告警已激活")
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