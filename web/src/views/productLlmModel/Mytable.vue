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
      @change="handleTableChange"
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
import { getProductLlmModelPage, deleteProductLlmModel } from '@/api/productLlmModel';
import { getProduct } from '@/api/product';
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { DeleteOutlined, EditOutlined } from '@ant-design/icons-vue'

// Define emits for parent component communication
const emit = defineEmits(['editRecord']);

const router = useRouter()

const pagination = ref({
  current: 1,
  pageSize: 5,
  total: 0,
  showSizeChanger: true,
  pageSizeOptions: ['5', '10', '20', '50'],
  showTotal: (total) => `共 ${total} 条`
});

const dataSource = ref([]);
const productNameMap = ref({});
const selectedProductId = ref(null);
const productOptions = ref([]);
const FILTER_STORAGE_KEY = 'productLlmModel:selectedProductId';

const filterOption = (input, option) => {
  return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0;
};

const getProductDisplayName = (product) => {
  return product.productName || `产品ID: ${product.id}`;
};

const fetchProductList = () => {
  return getProduct()
    .then((res) => {
      const { data, errorCode } = res.data;
      if (errorCode === 2001) {
        router.push('/login');
        return;
      }
      if (errorCode === 200 && data && Array.isArray(data)) {
        const newProductNameMap = {};
        productOptions.value = data.map((item) => {
          const productName = getProductDisplayName(item);
          newProductNameMap[item.id] = productName;
          return { value: item.id, label: productName };
        });
        Object.assign(productNameMap.value, newProductNameMap);
      } else {
        productOptions.value = [];
      }
    })
    .catch((err) => {
      console.error('获取产品列表失败:', err);
      productOptions.value = [];
    });
};

const handleFilterChange = (value) => {
  selectedProductId.value = value === null || value === undefined ? null : value;
  pagination.value.current = 1;
  persistSelectedProductId();
  stopPolling();
  fetchCurrentData({ force: true });
  startPolling();
};

const handleFilterReset = () => {
  selectedProductId.value = null;
  pagination.value.current = 1;
  persistSelectedProductId();
  stopPolling();
  fetchCurrentData({ force: true });
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
  'memory': '记忆工具',
  'knowledgeGraphic': '知识图谱'
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
    title: '思考模式',
    dataIndex: 'thinkingStatus',
    key: 'thinkingStatus',
  },
  {
    title: '思考预算',
    dataIndex: 'thinkingBudgetDisplay',
    key: 'thinkingBudgetDisplay',
  },
  {
    title: 'Action',
    key: 'action',
    slots: { customRender: 'action' },
  },
];

let intervalId;
let latestRequestId = 0;
let requestInFlight = false;

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

const startPolling = () => {
  clearInterval(intervalId);
  intervalId = setInterval(() => {
    fetchCurrentData();
  }, 1000);
};

const stopPolling = () => {
  clearInterval(intervalId);
  intervalId = null;
};

onMounted(async () => {
  restoreSelectedProductId();
  await fetchProductList();
  fetchCurrentData();
  startPolling();
});

onUnmounted(() => {
  stopPolling();
});

const normalizeThinkingBudget = (value) => {
  const budget = Number(value);
  if (!Number.isInteger(budget) || budget < 0 || budget > 8192) {
    return 1024;
  }
  return budget;
};

const mapDataSource = (data) => {
  if (!data || !Array.isArray(data)) return [];

  return data.map((item) => ({
    key: item.id,
    id: item.id,
    productId: item.productId,
    providerId: item.providerId,
    modelName: item.modelName,
    toolsId: item.toolsId,
    toolsName: getToolsName(item.toolsId),
    productName: productNameMap.value[item.productId] || `产品ID: ${item.productId}`,
    thinking: item.thinking === true,
    thinkingBudget: normalizeThinkingBudget(item.thinkingBudget),
    thinkingStatus: item.thinking === true ? '开启' : '关闭',
    thinkingBudgetDisplay: item.thinking === true ? normalizeThinkingBudget(item.thinkingBudget) : '-',
    providerName: item.providerName || `服务商ID: ${item.providerId}`
  }));
};

const getPageContent = (pageData) => {
  if (Array.isArray(pageData)) {
    return pageData;
  }
  if (Array.isArray(pageData?.content)) {
    return pageData.content;
  }
  if (Array.isArray(pageData?.page?.content)) {
    return pageData.page.content;
  }
  return [];
};

const getPageNumber = (pageData, fallback) => {
  const pageNumber = Number(pageData?.number ?? pageData?.page?.number);
  return Number.isFinite(pageNumber) ? pageNumber + 1 : fallback;
};

const getPageSize = (pageData, fallback) => {
  const pageSize = Number(pageData?.size ?? pageData?.page?.size);
  return Number.isFinite(pageSize) ? pageSize : fallback;
};

const getPageTotal = (pageData, fallback) => {
  const total = Number(pageData?.totalElements ?? pageData?.page?.totalElements ?? pageData?.total);
  return Number.isFinite(total) ? total : fallback;
};

const fetchCurrentData = (options = {}) => {
  if (requestInFlight && !options.force) {
    return;
  }
  requestInFlight = true;
  const requestId = ++latestRequestId;
  const params = {
    pageNum: pagination.value.current,
    pageSize: pagination.value.pageSize
  };
  if (selectedProductId.value !== null && selectedProductId.value !== undefined) {
    params.productId = selectedProductId.value;
  }
  getProductLlmModelPage(params)
    .then((res) => {
      const { data, errorCode } = res.data;
      if (requestId !== latestRequestId) {
        return;
      }
      if(errorCode === 2001){
        router.push('/login')
        return;
      }
      if(errorCode === 200 && data){
        const pageContent = getPageContent(data);
        const mappedData = mapDataSource(pageContent);
        if (requestId === latestRequestId && mappedData) {
          dataSource.value = mappedData;
          pagination.value = {
            ...pagination.value,
            current: getPageNumber(data, pagination.value.current),
            pageSize: getPageSize(data, pagination.value.pageSize),
            total: getPageTotal(data, pageContent.length)
          };
          const maxPage = Math.max(1,
            Math.ceil(pagination.value.total / pagination.value.pageSize));
          if (pagination.value.current > maxPage) {
            pagination.value.current = maxPage;
            fetchCurrentData({ force: true });
          }
        }
      } else {
        dataSource.value = [];
        pagination.value.total = 0;
      }
    })
    .catch((err) => {
      console.log(err);
      if (options.force && requestId === latestRequestId) {
        message.error('获取产品LLM模型失败');
      }
    })
    .finally(() => {
      if (requestId === latestRequestId) {
        requestInFlight = false;
      }
    });
};

const handleTableChange = (paginationInfo) => {
  const pageSizeChanged = paginationInfo.pageSize !== pagination.value.pageSize;
  pagination.value = {
    ...pagination.value,
    current: pageSizeChanged ? 1 : paginationInfo.current,
    pageSize: paginationInfo.pageSize
  };
  fetchCurrentData({ force: true });
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
          fetchCurrentData();
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

defineExpose({
  fetchCurrentData
});
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
