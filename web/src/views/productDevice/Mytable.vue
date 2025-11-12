<template>
  <div class="table-container">
    <a-table 
      :columns="columns" 
      :data-source="dataSource" 
      :pagination="pagination"
      class="custom-table"
    >
      <template #online="{ record }">
        <a-tag 
          :key="record.online"
          :color="record.online === 'disconnected' ? 'error' : 'success'"
          class="status-tag"
        >
          {{ record.online }}
        </a-tag>
      </template>
      
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
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { getProductDevice,deleteProductDevice} from '@/api/device';
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { DeleteOutlined } from '@ant-design/icons-vue'

const router = useRouter()

const pagination = {
  pageSize: 5,
};

const dataSource = ref([]);

const columns = [
  {
    title: '设备id',
    dataIndex: 'id',
    key: 'id',
  },
  {
    title: '设备名称',
    dataIndex: 'name',
    key: 'name',
  },
  {
    title: 'mqtt密码',
    dataIndex: 'password',
    key: 'password',
  },
  {
    title: 'clientId',
    dataIndex: 'clientId',
    key: 'clientId',
  },
  {
    title: '订阅主题',
    dataIndex: 'subscribeTopic',
    key: 'subscribeTopic',
  },
  {
    title: '物模型id',
    dataIndex: 'modelId',
    key: 'modelId',
  },
  {
    title: '设备描述',
    dataIndex: 'description',
    key: 'description',
  },
  {
    title: '在线/离线',
    dataIndex: 'online',
    key: 'online',
    slots: { customRender: 'online' },
  },
  {
    title: '使能',
    dataIndex: 'allow',
    key: 'allow',
  },
  {
    title: 'Action',
    key: 'action',
    slots: { customRender: 'action' },
  },
];

let intervalId;

onMounted(() => {
  fetchProductDevice();
  intervalId = setInterval(fetchProductDevice, 1000); // 每 60 秒钟刷新一次数据
});

onUnmounted(() => {
  clearInterval(intervalId);
});

const fetchProductDevice = () => {
  getProductDevice()
    .then((res) => {
      const { data, errorCode } = res.data;
      if(errorCode==2001){
        router.push('/login')
      }
      if(errorCode==200&& data && Array.isArray(data)){
        dataSource.value = data.map((item, index) => ({
          key: index,
          id: item.id,
          password: item.password,
          name: item.name,
          clientId: item.clientId,
          modelId: item.modelId,
          subscribeTopic: item.subscribeTopic,
          online: item.online,
          allow: item.allow,
          description: item.description
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
  deleteProductDevice({id:record.id}).then((res) => {
      const { data, errorCode } = res.data;
      if(errorCode==200){
        message.success("删除成功")
      }else if(errorCode==3002){
        message.warn("删除失败，设备已绑定空中升级")
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
      border-radius: 12px;
      overflow: hidden;
    }

    :deep(.ant-table-thead > tr > th) {
      background: #fafafa;
      font-weight: 500;
      padding: 16px;
    }

    :deep(.ant-table-tbody > tr > td) {
      padding: 16px;
      transition: background 0.3s;
    }

    :deep(.ant-table-tbody > tr:hover > td) {
      background: #fafafa;
    }

    .status-tag {
      border-radius: 4px;
      padding: 4px 8px;
      font-size: 13px;
    }

    .action-buttons {
      .action-button {
        display: flex;
        align-items: center;
        gap: 4px;
        padding: 4px 0;
        transition: opacity 0.3s;

        &:hover {
          opacity: 0.8;
        }

        .anticon {
          font-size: 14px;
        }
      }
    }
  }
}
</style>