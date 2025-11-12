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
import { getOtaXiaoZhiPassive, deleteOtaXiaoZhiPassive } from '@/api/productOtaXiaozhiPassive'; 
import { message, Modal } from 'ant-design-vue';
import { useRouter } from 'vue-router';
import { DeleteOutlined } from '@ant-design/icons-vue';

const router = useRouter();

const pagination = {
  pageSize: 5,
};

const dataSource = ref([]);

// 1. 修改 columns 定义，直接使用 API 返回的字段名
const columns = [
  {
    title: '被动升级id',
    dataIndex: 'id',
    key: 'id',
  },
  {
    title: '固件id',
    dataIndex: 'otaId',
    key: 'otaId',
  },
  {
    title: '固件名称',
    dataIndex: 'otaName',
    key: 'otaName',
  },
  {
    title: '产品id',
    dataIndex: 'productId', // 直接使用 productId
    key: 'productId',
  },
  {
    title: '产品名称',
    dataIndex: 'productName', // 直接使用 productName
    key: 'productName',
  },
  {
    title: '版本名称',
    dataIndex: 'versionName',
    key: 'versionName',
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
  intervalId = setInterval(fetchproductModel, 1000); // 每 1 秒钟刷新一次数据
});

onUnmounted(() => {
  clearInterval(intervalId);
});

// 2. 修改数据获取函数，移除映射，直接使用 API 字段
const fetchproductModel = () => {
  getOtaXiaoZhiPassive()
    .then((res) => {
      const { data, errorCode } = res.data;
      if(errorCode == 2001){
        router.push('/login');
        return;
      }
      if(errorCode == 200 && data && Array.isArray(data)){
        dataSource.value = data.map((item, index) => ({
          key: index,
          id: item.id,
          otaId: item.otaId,
          // 直接赋值，不再需要映射
          productId: item.productId,
          productName: item.productName,
          versionName: item.versionName,
          otaName: item.otaName,
        }));
      } else {
        dataSource.value = [];
      }
    })
    .catch((err) => {
      console.log(err);
      message.error('获取数据失败');
    });
};

// 3. 修改删除处理函数，使用新的字段名
const handleDelete = (record) => {  
  Modal.confirm({
    title: '确认删除',
    // 使用 record.productName 来获取产品名称
    content: `确定要删除产品"${record.productName}"的升级信息吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk() {
      deleteOtaXiaoZhiPassive({id: record.id})
        .then((res) => {  
          const { errorCode } = res.data;  
          if(errorCode == 200){  
            message.success("删除成功");
            fetchproductModel();
          } else if(errorCode == 2001){  
            router.push('/login')  
          } else {
            const errorMsg = res.data.errorMsg || "删除失败";
            message.error(errorMsg);
          }
        })  
        .catch((err) => {  
          console.log(err);
          message.error("删除失败");
        });
    },
  });
};  
</script>
