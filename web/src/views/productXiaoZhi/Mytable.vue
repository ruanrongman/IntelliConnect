<template>
  <div class="table-container">
    <a-table 
      :columns="columns" 
      :data-source="dataSource" 
      :pagination="pagination"
      class="custom-table"
    >
      <template #action="{ record }">
        <a-button 
          type="link" 
          danger
          @click="handleDelete(record)"
        >
          <template #icon><DeleteOutlined /></template>
          解除绑定
        </a-button>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { message , Modal} from 'ant-design-vue'
import { getXiaoZhiManager,deleteXiaoZhiManager} from '@/api/xiaoZhi';
import { useRouter } from 'vue-router'
import { DeleteOutlined } from '@ant-design/icons-vue'

const router = useRouter()

const pagination = {
  pageSize: 5,
};

const dataSource = ref([]);

const columns = [
  {
    title: '产品id',
    dataIndex: 'productId',
    key: 'productId',
  },
  {
    title: '产品mac地址',
    dataIndex: 'deviceId',
    key: 'deviceId',
  },
  {
    title: '绑定用户名',
    dataIndex: 'userName',
    key: 'userName',
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

const fetchProduct = () => {
  getXiaoZhiManager()
    .then((res) => {
      const { data, errorCode } = res.data;
      if(errorCode==2001){
        router.push('/login')
      }
      if(errorCode==200&& data && Array.isArray(data)){
        dataSource.value = data.map((item, index) => ({
          key: index,
          id: item.id,
          productId: item.productId,
          deviceId: item.deviceId,
          userName: item.userName
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
  Modal.confirm({
    title: '确认解绑',
    // 使用 record.productName 来获取产品名称
    content: `确定要解绑"${record.deviceId}"的设备吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk() {
    console.log('Deleting record:', record);
    deleteXiaoZhiManager({id:record.id}).then((res) => {
        const { data, errorCode } = res.data;
        if(errorCode==200){
          message.success("解绑成功")
        }else if(errorCode==2001){
          router.push('/login')
        }
        console.log(data)
      })
      .catch((err) => {
        console.log(err);
      });
  }});
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