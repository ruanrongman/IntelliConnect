<template>
  <div class="table-container">
    <a-table
      :columns="columns"
      :data-source="dataSource"
      :pagination="pagination"
      class="custom-table"
    >
      <template #appKey="{ record }">
        <div style="display: flex; align-items: center; gap: 8px;">
          <span v-if="!record.showKey">********</span>
          <span v-else>{{ record.appKey }}</span>
          <a-button
            type="link"
            size="small"
            @click="toggleKeyVisibility(record)"
          >
            <template #icon>
              <EyeOutlined v-if="!record.showKey" />
              <EyeInvisibleOutlined v-else />
            </template>
          </a-button>
        </div>
      </template>

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
import { getLlmProviderInformation, deleteLlmProviderInformation } from '@/api/llmProviderInformation';
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'

// Define emits for parent component communication
const emit = defineEmits(['editRecord']);

const router = useRouter()

const pagination = {
  pageSize: 5,
};

const dataSource = ref([]);

const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    key: 'id',
  },
  {
    title: '服务商名称',
    dataIndex: 'providerName',
    key: 'providerName',
  },
  {
    title: 'API URL',
    dataIndex: 'baseUrl',
    key: 'baseUrl',
  },
  {
    title: 'API Key',
    dataIndex: 'appKey',
    key: 'appKey',
    slots: { customRender: 'appKey' },
  },
  {
    title: '类型',
    dataIndex: 'type',
    key: 'type',
  },
  {
    title: '用户名',
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
  fetchLlmProviderInformation();
  intervalId = setInterval(fetchLlmProviderInformation, 1000);
});

onUnmounted(() => {
  clearInterval(intervalId);
});

const fetchLlmProviderInformation = () => {
  getLlmProviderInformation()
    .then((res) => {
      const { data, errorCode } = res.data;
      if(errorCode === 2001){
        router.push('/login')
      }
      if(errorCode === 200 && data && Array.isArray(data)){
        dataSource.value = data.map((item, index) => ({
          key: item.id, // Use the actual ID as the key for better performance
          id: item.id,
          providerName: item.providerName,
          baseUrl: item.baseUrl,
          appKey: item.appKey,
          type: item.type,
          userName: item.userName,
          showKey: false // 默认隐藏 API Key
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

const toggleKeyVisibility = (record) => {
  record.showKey = !record.showKey;
};

const handleEdit = (record) => {
  // Emit event to parent component to handle editing
  emit('editRecord', record);
};

const handleDelete = (record) => {
  // 添加删除确认
  if (confirm(`确定要删除服务商 "${record.providerName}" 吗？`)) {
    console.log('Deleting record:', record);
    deleteLlmProviderInformation({id: record.id}).then((res) => {
        const { data, errorCode } = res.data;
        if(errorCode === 200){
          message.success("删除成功")
        }else if(errorCode === 2001){
          router.push('/login')
        }else if(errorCode === 3002){
          message.error("存在依赖关系，无法删除！请先删除使用此服务商的产品LLM模型")
        }else{
          message.error("删除失败")
        }
        console.log(data)
      })
      .catch((err) => {
        console.log(err);
        message.error("删除失败")
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