<template>
  <div class="table-container">
    <div class="search-bar">
      <a-select
        v-model:value="selectedProductId"
        :options="productOptions"
        placeholder="按产品筛选"
        allowClear
        show-search
        :filter-option="filterOption"
        style="width: 220px; margin-right: 12px;"
        @change="handleFilterChange"
      />
      <a-button @click="handleFilterReset" v-if="selectedProductId !== null">
        重置
      </a-button>
    </div>
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
            @click="handleEdit(record)"
            class="action-button"
          >
            <template #icon><EditOutlined /></template>
            编辑
          </a-button>
          <a-button
            type="link"
            danger
            @click="handleDelete(record)"
            class="action-button"
          >
            <template #icon><DeleteOutlined /></template>
            删除
          </a-button>
        </div>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { getProductLlmModel, getProductLlmModelByProductId, deleteProductLlmModel } from '@/api/productLlmModel';
import { getProduct, getProductName } from '@/api/product';
import { getLlmProviderInformation } from '@/api/llmProviderInformation';
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { DeleteOutlined, EditOutlined } from '@ant-design/icons-vue'

// Define emits for parent component communication
const emit = defineEmits(['editRecord']);

const router = useRouter()

const pagination = {
  pageSize: 5,
};

const dataSource = ref([]);
const productNameMap = ref({});
const providerNameMap = ref({});
const selectedProductId = ref(null);
const productOptions = ref([]);
const FILTER_STORAGE_KEY = 'productLlmModel:selectedProductId';

const filterOption = (input, option) => {
  return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0;
};

const fetchProductList = () => {
  getProduct()
    .then(async (res) => {
      const { data, errorCode } = res.data;
      if (errorCode === 2001) {
        router.push('/login');
        return;
      }
      if (errorCode === 200 && data && Array.isArray(data)) {
        productOptions.value = await Promise.all(data.map(async (item) => {
          try {
            const nameRes = await getProductName({ id: item.id });
            const { data: nameData, errorCode: nameErrorCode } = nameRes.data;
            if (nameErrorCode === 200 && nameData) {
              return { value: item.id, label: nameData };
            }
            return { value: item.id, label: `产品ID: ${item.id}` };
          } catch (err) {
            return { value: item.id, label: `产品ID: ${item.id}` };
          }
        }));
      }
    })
    .catch((err) => {
      console.error('获取产品列表失败:', err);
    });
};

const handleFilterChange = (value) => {
  selectedProductId.value = value === null || value === undefined ? null : value;
  persistSelectedProductId();
  stopPolling();
  fetchCurrentData();
  if (selectedProductId.value === null) {
    startPolling();
  }
};

const handleFilterReset = () => {
  selectedProductId.value = null;
  persistSelectedProductId();
  stopPolling();
  fetchCurrentData();
  startPolling();
};

// 工具ID到名称的映射
const toolsIdMap = {
  '1': '天气工具',
  '2': '控制工具',
  '3': '音乐工具',
  '4': 'AI代理工具(内部工具与外部工具协同)',
  '5': '聊天工具',
  '6': '微信绑定产品工具',
  '7': '微信产品激活工具',
  '8': '定时任务工具',
  '9': '产品角色工具',
  '10': 'MCP代理工具',
  'classifier': '分类器工具',
  'longMemory': '长期记忆工具',
  'memory': '记忆工具'
};

// 获取工具名称的辅助函数
const getToolsName = (toolsId) => {
  return toolsIdMap[toolsId] || toolsId;
};

const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    key: 'id',
  },
  {
    title: '产品名称',
    dataIndex: 'productName',
    key: 'productName',
  },
  {
    title: '服务商名称',
    dataIndex: 'providerName',
    key: 'providerName',
  },
  {
    title: '模型名称',
    dataIndex: 'modelName',
    key: 'modelName',
  },
  {
    title: '工具名称',
    dataIndex: 'toolsName',
    key: 'toolsName',
  },
  {
    title: 'Action',
    key: 'action',
    slots: { customRender: 'action' },
  },
];

let intervalId;
let latestRequestId = 0;

const persistSelectedProductId = () => {
  try {
    if (selectedProductId.value === null) {
      sessionStorage.removeItem(FILTER_STORAGE_KEY);
      return;
    }
    sessionStorage.setItem(FILTER_STORAGE_KEY, JSON.stringify(selectedProductId.value));
  } catch (err) {
    console.warn('持久化产品筛选条件失败:', err);
  }
};

const restoreSelectedProductId = () => {
  try {
    const cachedValue = sessionStorage.getItem(FILTER_STORAGE_KEY);
    if (!cachedValue) {
      selectedProductId.value = null;
      return;
    }
    selectedProductId.value = JSON.parse(cachedValue);
  } catch (err) {
    console.warn('恢复产品筛选条件失败:', err);
    selectedProductId.value = null;
    sessionStorage.removeItem(FILTER_STORAGE_KEY);
  }
};

const fetchCurrentData = () => {
  if (selectedProductId.value === null || selectedProductId.value === undefined) {
    fetchProductLlmModel();
    return;
  }
  fetchProductLlmModelByProductId(selectedProductId.value);
};

const startPolling = () => {
  clearInterval(intervalId);
  intervalId = setInterval(fetchCurrentData, 1000);
};

const stopPolling = () => {
  clearInterval(intervalId);
};

onMounted(() => {
  fetchProductList();
  restoreSelectedProductId();
  fetchCurrentData();
  if (selectedProductId.value === null) {
    startPolling();
  }
});

onUnmounted(() => {
  stopPolling();
});

const mapDataSource = async (data, requestId) => {
  if (!data || !Array.isArray(data)) return [];
  // 获取产品名称映射
  const newProductNameMap = {};
  const uniqueProductIds = [...new Set(data.map(item => item.productId))];
  const productNameResults = await Promise.all(uniqueProductIds.map(async (productId) => {
    try {
      const nameRes = await getProductName({ id: productId });
      const { data: nameData, errorCode: nameErrorCode } = nameRes.data;
      if (nameErrorCode === 200 && nameData) {
        return [productId, nameData];
      }
    } catch (err) {
      console.error(`获取产品ID ${productId} 名称失败:`, err);
    }
    return [productId, `产品ID: ${productId}`];
  }));
  if (requestId !== latestRequestId) {
    return null;
  }
  productNameResults.forEach(([productId, productName]) => {
    newProductNameMap[productId] = productName;
  });

  // 获取提供者名称映射
  const providerRes = await getLlmProviderInformation();
  const { data: providerData, errorCode: providerErrorCode } = providerRes.data;
  if (requestId !== latestRequestId) {
    return null;
  }
  const newProviderNameMap = {};
  if(providerErrorCode === 200 && providerData && Array.isArray(providerData)){
    providerData.forEach(item => {
      const displayName = item.userName
        ? `${item.providerName} (${item.userName})`
        : item.providerName || `提供者ID: ${item.id}`;
      newProviderNameMap[item.id] = displayName;
    });
  }

  Object.assign(productNameMap.value, newProductNameMap);
  Object.assign(providerNameMap.value, newProviderNameMap);

  return data.map((item) => ({
    key: item.id,
    id: item.id,
    productId: item.productId,
    providerId: item.providerId,
    modelName: item.modelName,
    toolsId: item.toolsId,
    toolsName: getToolsName(item.toolsId),
    productName: newProductNameMap[item.productId] || `产品ID: ${item.productId}`,
    providerName: newProviderNameMap[item.providerId] || `服务商ID: ${item.providerId}`
  }));
};

const fetchProductLlmModel = () => {
  const requestId = ++latestRequestId;
  getProductLlmModel()
    .then(async (res) => {
      const { data, errorCode } = res.data;
      if (requestId !== latestRequestId) {
        return;
      }
      if(errorCode === 2001){
        router.push('/login')
        return;
      }
      if(errorCode === 200 && data && Array.isArray(data)){
        const mappedData = await mapDataSource(data, requestId);
        if (requestId === latestRequestId && mappedData) {
          dataSource.value = mappedData;
        }
      } else {
        dataSource.value = [];
      }
    })
    .catch((err) => {
      console.log(err);
    });
};

const fetchProductLlmModelByProductId = (productId) => {
  const requestId = ++latestRequestId;
  getProductLlmModelByProductId({ productId })
    .then(async (res) => {
      const { data, errorCode } = res.data;
      if (requestId !== latestRequestId) {
        return;
      }
      if(errorCode === 2001){
        router.push('/login')
        return;
      }
      if(errorCode === 200 && data && Array.isArray(data)){
        const mappedData = await mapDataSource(data, requestId);
        if (requestId === latestRequestId && mappedData) {
          dataSource.value = mappedData;
        }
      } else {
        dataSource.value = [];
      }
    })
    .catch((err) => {
      console.log(err);
      message.error('按产品筛选失败');
    });
};

const handleEdit = (record) => {
  // Emit event to parent component to handle editing
  emit('editRecord', record);
};

const handleDelete = (record) => {
  // 添加删除确认
  if (confirm(`确定要删除模型 "${record.modelName}" 吗？`)) {
    console.log('Deleting record:', record);
    deleteProductLlmModel({id: record.id}).then((res) => {
        const { data, errorCode } = res.data;
        if(errorCode === 200){
          message.success("删除成功")
        }else if(errorCode === 2001){
          router.push('/login')
        }else{
          message.error("删除失败")
        }
        console.log(data)
      })
      .catch((err) => {
        console.log(err);
      });
  }
};
</script>

<style lang="scss" scoped>
.table-container {
  .search-bar {
    display: flex;
    align-items: center;
    margin-bottom: 16px;
    padding-bottom: 16px;
    border-bottom: 1px solid #f0f0f0;
  }

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
