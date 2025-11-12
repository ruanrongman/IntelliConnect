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
            class="delete-button"
          >
            <template #icon><DeleteOutlined /></template>
            删除信息
          </a-button>
          <a-divider type="vertical" />
          <a-button 
            type="link" 
            @click="handleDisable(record)"
            class="disable-button"
          >
            <template #icon><StopOutlined /></template>
            禁用
          </a-button>
        </div>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { getproductModel } from '@/api/productModel';
import { DeleteOutlined, StopOutlined } from '@ant-design/icons-vue'

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
      dataSource.value = data.map((item, index) => ({
        key: index,
        id: item.id,
        name: item.name,
        func: item.description,
        productId: item.productId,
      }));
    })
    .catch((err) => {
      console.log(err);
    });
};

const handleDelete = (record) => {
  // 实现删除功能
  console.log('Deleting record:', record);
};

const handleDisable = (record) => {
  console.log('Disabling record:', record)
}
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
  }

  .action-buttons {
    display: flex;
    align-items: center;
    gap: 8px;

    .delete-button,
    .disable-button {
      display: flex;
      align-items: center;
      gap: 4px;
      padding: 4px 0;
    }

    .ant-divider {
      margin: 0 4px;
    }
  }
}
</style>