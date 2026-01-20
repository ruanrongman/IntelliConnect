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
import { ref, onMounted, onUnmounted, computed } from 'vue';
import { getProductLlmModel, deleteProductLlmModel } from '@/api/productLlmModel';
import { getProductName } from '@/api/product';
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

onMounted(() => {
  fetchProductLlmModel();
  intervalId = setInterval(fetchProductLlmModel, 1000); // 每5秒刷新一次数据
});

onUnmounted(() => {
  clearInterval(intervalId);
});

const fetchProductLlmModel = () => {
  getProductLlmModel()
    .then(async (res) => {
      const { data, errorCode } = res.data;
      if(errorCode === 2001){
        router.push('/login')
        return;
      }
      if(errorCode === 200 && data && Array.isArray(data)){
        // 获取产品名称映射
        const newProductNameMap = {};
        const uniqueProductIds = [...new Set(data.map(item => item.productId))];
        for(const productId of uniqueProductIds) {
          try {
            const nameRes = await getProductName({ id: productId });
            const { data: nameData, errorCode: nameErrorCode } = nameRes.data;
            if(nameErrorCode === 200 && nameData) {
              newProductNameMap[productId] = nameData;
            } else {
              newProductNameMap[productId] = `产品ID: ${productId}`;
            }
          } catch (err) {
            console.error(`获取产品ID ${productId} 名称失败:`, err);
            newProductNameMap[productId] = `产品ID: ${productId}`;
          }
        }

        // 获取提供者名称映射
        const providerRes = await getLlmProviderInformation();
        const { data: providerData, errorCode: providerErrorCode } = providerRes.data;
        const newProviderNameMap = {};
        if(providerErrorCode === 200 && providerData && Array.isArray(providerData)){
          providerData.forEach(item => {
            // 格式: "服务商名称 (用户名)" 以区分不同用户的相同服务商名称
            const displayName = item.userName
              ? `${item.providerName} (${item.userName})`
              : item.providerName || `提供者ID: ${item.id}`;
            newProviderNameMap[item.id] = displayName;
          });
        }

        // 更新映射
        Object.assign(productNameMap.value, newProductNameMap);
        Object.assign(providerNameMap.value, newProviderNameMap);

        dataSource.value = data.map((item, index) => ({
          key: item.id, // Use the actual ID as the key for better performance
          id: item.id,
          productId: item.productId,
          providerId: item.providerId,
          modelName: item.modelName,
          toolsId: item.toolsId,
          toolsName: getToolsName(item.toolsId),
          productName: newProductNameMap[item.productId] || `产品ID: ${item.productId}`,
          providerName: newProviderNameMap[item.providerId] || `服务商ID: ${item.providerId}`
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