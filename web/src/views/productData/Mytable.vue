<template>
  <div class="table-container">
    <a-table 
      :columns="columns" 
      :data-source="dataSource" 
      :pagination="pagination"
      class="custom-table"
    >
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
import { getProductData,deleteProductData } from '@/api/productData';
import { useRouter } from 'vue-router'
const router = useRouter()

const pagination = {
  pageSize: 5,
};

const dataSource = ref([]);

const columns = [
  {
    title: '属性id',
    dataIndex: 'id',
    key: 'id',
  },
  {
    title: '属性名称',
    dataIndex: 'jsonKey',
    key: 'jsonKey',
  },
  {
    title: '物模型id',
    dataIndex: 'modelId',
    key: 'modelId',
  },
  {
    title: '属性描述',
    dataIndex: 'description',
    key: 'description',
  },
  {
    title: '存储类型',
    dataIndex: 'storageType',
    key: 'storageType',
  },
  {
    title: '读写类型',
    dataIndex: 'rRw',
    key: 'rRw',
  },
  {
    title: '属性类型',
    dataIndex: 'type',
    key: 'type',
  },
  {
    title: '最大值',
    dataIndex: 'max',
    key: 'max',
  },
  {
    title: '最小值',
    dataIndex: 'min',
    key: 'min',
  },
  {
    title: '步长',
    dataIndex: 'step',
    key: 'step',
  },
  {
    title: '单位',
    dataIndex: 'unit',
    key: 'unit',
  },
  {
    title: 'Action',
    key: 'action',
    slots: { customRender: 'action' },
  },
];

let intervalId;

onMounted(() => {
  fetchproductData();
  intervalId = setInterval(fetchproductData, 1000); // 每 60 秒钟刷新一次数据
});

onUnmounted(() => {
  clearInterval(intervalId);
});

const fetchproductData = () => {
  getProductData()
    .then((res) => {
      const { data, errorCode } = res.data;
      if(errorCode==2001){
        router.push('/login')
      }
      if(errorCode==200&& data && Array.isArray(data)){
        dataSource.value = data.map((item, index) => ({
          key: index,
          id: item.id,
          jsonKey: item.jsonKey,
          modelId: item.modelId,
          description: item.description,
          storageType: item.storageType,
          rRw : item.rRw,
          type: item.type,
          max: item.max,
          min: item.min,
          step: item.step,
          unit: item.unit,
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
  deleteProductData({id:record.id}).then((res) => {
      const { data, errorCode } = res.data;
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