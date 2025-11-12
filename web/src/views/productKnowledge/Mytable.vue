<template>
  <div class="table-container">
    <a-table 
      :columns="columns" 
      :data-source="dataSource" 
      :pagination="pagination"
      class="custom-table"
    >
      <template #status="{ record }">
        <a-tag :color="getStatusColor(record.status)">
          {{ getStatusText(record.status) }}
        </a-tag>
      </template>
      <template #action="{ record }">
        <a-button 
          type="link" 
          danger
          @click="handleDelete(record)"
        >
          <template #icon><DeleteOutlined /></template>
          删除
        </a-button>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { message } from 'ant-design-vue'
import { getKnowledgeChat,deleteKnowledgeChat} from '@/api/productKnowledge';
import { useRouter } from 'vue-router'
import { DeleteOutlined } from '@ant-design/icons-vue'

const router = useRouter()

const pagination = {
  pageSize: 5,
};

const dataSource = ref([]);

const columns = [
  {
    title: '知识库id',
    dataIndex: 'id',
    key: 'id',
  },
  {
    title: '文件名称',
    dataIndex: 'filename',
    key: 'filename',
  },
  {
    title: '产品id',
    dataIndex: 'productId',
    key: 'productId',
  },
  {
    title: '训练状态',
    dataIndex: 'status',
    key: 'status',
    slots: { customRender: 'status' },
  },
  {
    title: 'Action',
    key: 'action',
    slots: { customRender: 'action' },
  },
];

let intervalId;

onMounted(() => {
  fetchProduct();
  intervalId = setInterval(fetchProduct, 1000); // 每 60 秒钟刷新一次数据
});

onUnmounted(() => {
  clearInterval(intervalId);
});

const getStatusColor = (status) => {
  switch(status) {
    case 'success':
      return 'success';
    case 'error':
      return 'error';
    case 'training':
      return 'warning';
    default:
      return 'default';
  }
};

const getStatusText = (status) => {
  switch(status) {
    case 'success':
      return '成功';
    case 'error':
      return '失败';
    case 'training':
      return '训练中';
    default:
      return status;
  }
};

const fetchProduct = () => {
  getKnowledgeChat()
    .then((res) => {
      const { data, errorCode } = res.data;
      if(errorCode==2001){
        router.push('/login')
      }
      if(errorCode==200&& data && Array.isArray(data)){
        dataSource.value = data.map((item, index) => ({
          key: index,
          id: item.id,
          filename: item.filename,
          productId: item.productId,
          status:item.status
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
  deleteKnowledgeChat({id:record.id}).then((res) => {
      const { data, errorCode } = res.data;
      if(errorCode==200){
        message.success("删除成功")
      }else if(errorCode==3002){
        message.warn("删除失败，知识库正在被系统使用中")
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

<style lang="scss" scoped>
.table-container {
  .custom-table {
    :deep(.ant-table) {
      border-radius: 8px;
    }

    :deep(.ant-table-thead > tr > th) {
      background: #fafafa;
      font-weight: 500;
    }

    :deep(.ant-table-tbody > tr > td) {
      padding: 16px;
    }

    :deep(.ant-btn-link) {
      padding: 4px 0;
      height: auto;
      
      .anticon {
        margin-right: 4px;
      }
    }
  }
}
</style>