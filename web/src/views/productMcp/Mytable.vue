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
import { getMcpServer,deleteMcpServer } from '@/api/productMcp';
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
const router = useRouter()

const pagination = {
  pageSize: 5,
};

const dataSource = ref([]);

const columns = [
  {
    title: 'mcp服务器id',
    dataIndex: 'id',
    key: 'id',
  },
  {
    title: 'mcp服务器描述',
    dataIndex: 'description',
    key: 'description',
  },
  {
    title: 'mcp服务器地址',
    dataIndex: 'url',
    key: 'url',
  },
  {
    title: 'mcp服务器sseEndpoint',
    dataIndex: 'sseEndpoint',
    key: 'sseEndpoint',
  },
  {
    title: '产品Id',
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
  getMcpServer()
    .then((res) => {
      const { data, errorCode } = res.data;
      if(errorCode==2001){
        router.push('/login')
      }
      if(errorCode==200&& data && Array.isArray(data)){
      dataSource.value = data.map((item, index) => ({
        key: index,
        id: item.id,
        description: item.description,
        sseEndpoint: item.sseEndpoint,
        url:item.url,
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
  deleteMcpServer({id:record.id}).then((res) => {
      const { data, errorCode } = res.data;
      if(errorCode==3002){
        message.warn("删除失败,该物模型绑定有设备")
      }else if(errorCode==2001){
        router.push('/login')
      }
      console.log(data)
    })
    .catch((err) => {
      console.log(err);
    });
};
</script>