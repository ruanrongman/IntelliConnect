<template>
  <div class="table-container">
    <div class="search-bar">
      <a-select
        v-model:value="searchForm.productId"
        :options="productOptions"
        placeholder="请选择产品"
        allowClear
        show-search
        :filter-option="filterOption"
        style="width: 200px; margin-right: 12px;"
      />
      <a-input
        v-model:value="searchForm.nickName"
        placeholder="输入昵称搜索"
        allowClear
        style="width: 200px; margin-right: 12px;"
        @pressEnter="handleSearch"
      />
      <a-button type="primary" @click="handleSearch" style="margin-right: 8px;">
        <template #icon><SearchOutlined /></template>
        搜索
      </a-button>
      <a-button @click="handleReset">
        重置
      </a-button>
    </div>
    <a-table
      :columns="columns"
      :data-source="dataSource"
      :pagination="pagination"
      class="custom-table"
    >
      <template #content="{ record }">
        <div class="content-cell">
          {{ record.content }}
        </div>
      </template>
      <template #action="{ record }">
        <a-space>
          <a-button
            type="link"
            @click="handleView(record)"
          >
            <template #icon><EyeOutlined /></template>
            查看
          </a-button>
          <a-button
            type="link"
            @click="handleEdit(record)"
          >
            <template #icon><EditOutlined /></template>
            修改
          </a-button>
          <a-button
            type="link"
            danger
            @click="handleDelete(record)"
          >
            <template #icon><DeleteOutlined /></template>
            删除
          </a-button>
        </a-space>
      </template>
    </a-table>

    <!-- 查看记忆弹窗 -->
    <a-modal
      :visible="viewModalVisible"
      @update:visible="val => viewModalVisible = val"
      @cancel="viewModalVisible = false"
      title="查看记忆详情"
      :footer="null"
      width="600px"
    >
      <div class="memory-detail">
        <a-descriptions :column="1" bordered>
          <a-descriptions-item label="记忆ID">
            {{ currentRecord.id }}
          </a-descriptions-item>
          <a-descriptions-item label="Chat ID">
            {{ currentRecord.chatId }}
          </a-descriptions-item>
          <a-descriptions-item label="设备名称">
            {{ currentRecord.deviceName }}
          </a-descriptions-item>
          <a-descriptions-item label="记忆内容">
            <div class="content-text">{{ currentRecord.content }}</div>
          </a-descriptions-item>
        </a-descriptions>
      </div>
    </a-modal>

    <!-- 修改记忆弹窗 -->
    <a-modal
      :visible="editModalVisible"
      @update:visible="val => editModalVisible = val"
      title="修改记忆内容"
      @ok="handleEditSubmit"
      @cancel="handleEditCancel"
      width="600px"
      okText="保存"
      cancelText="取消"
    >
      <a-form :model="editForm" layout="vertical">
        <a-form-item label="Chat ID">
          <a-input v-model:value="editForm.chatId" disabled />
        </a-form-item>
        <a-form-item label="设备名称">
          <a-input v-model:value="editForm.deviceName" disabled />
        </a-form-item>
        <a-form-item label="记忆内容" required>
          <a-textarea
            v-model:value="editForm.content"
            :rows="8"
            placeholder="请输入记忆内容"
            :maxlength="1000"
            show-count
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, reactive } from 'vue';
import { message, Modal } from 'ant-design-vue';
import { getAgentMemory, getAgentMemoryByNickName, putAgentMemory, deleteAgentMemory } from '@/api/agentMemory';
import { getProduct } from '@/api/product';
import { useRouter } from 'vue-router';
import { DeleteOutlined, EyeOutlined, EditOutlined, SearchOutlined } from '@ant-design/icons-vue';

const router = useRouter();

const pagination = {
  pageSize: 5,
};

interface MemoryRecord {
  key: number | string;
  id: number | string;
  chatId: string;
  deviceName: string;
  content: string;
}

const dataSource = ref<MemoryRecord[]>([]);
const viewModalVisible = ref(false);
const editModalVisible = ref(false);
const currentRecord = ref<MemoryRecord>({
  key: '',
  id: '',
  chatId: '',
  deviceName: '',
  content: ''
});

const editForm = reactive({
  chatId: '',
  deviceName: '',
  content: ''
});

const productOptions = ref<{ value: number; label: string }[]>([]);
const searchForm = reactive({
  productId: null as number | null,
  nickName: ''
});

const columns = [
  {
    title: '记忆ID',
    dataIndex: 'id',
    key: 'id',
    width: 80,
  },
  {
    title: 'Chat ID',
    dataIndex: 'chatId',
    key: 'chatId',
    width: 200,
    ellipsis: true,
  },
  {
    title: '设备名称',
    dataIndex: 'deviceName',
    key: 'deviceName',
    width: 150,
  },
  {
    title: '记忆内容',
    dataIndex: 'content',
    key: 'content',
    ellipsis: true,
    slots: { customRender: 'content' },
  },
  {
    title: '操作',
    key: 'action',
    width: 240,
    fixed: 'right',
    slots: { customRender: 'action' },
  },
];

let intervalId: NodeJS.Timeout;

const filterOption = (input: string, option: any) => {
  return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0;
};

const fetchProductList = () => {
  getProduct()
    .then((res) => {
      const { data, errorCode } = res.data;
      if (errorCode == 2001) {
        router.push('/login');
        return;
      }
      if (data && Array.isArray(data)) {
        productOptions.value = data.map((item: any) => ({
          value: item.id,
          label: item.productName,
        }));
      }
    })
    .catch((err) => {
      console.error('获取产品列表失败:', err);
    });
};

onMounted(() => {
  fetchProductList();
  fetchMemory();
  intervalId = setInterval(fetchMemory, 1000);
});

onUnmounted(() => {
  clearInterval(intervalId);
});

const fetchMemory = () => {
  getAgentMemory()
    .then((res) => {
      const { data, errorCode } = res.data;
      if(errorCode == 2001){
        router.push('/login');
      }
      if(errorCode == 200 && data && Array.isArray(data)){
        dataSource.value = data.map((item: any) => ({
          key: item.id,
          id: item.id,
          chatId: item.chatId,
          deviceName: item.deviceName || '未知设备',
          content: item.content
        }));
      } else {
        dataSource.value = [];
      }
    })
    .catch((err) => {
      console.error('获取记忆列表失败:', err);
      message.error('获取记忆列表失败');
    });
};

const handleSearch = () => {
  if (!searchForm.productId) {
    message.warning('请先选择产品');
    return;
  }
  // Stop polling during search
  clearInterval(intervalId);
  const params: Record<string, any> = { productId: searchForm.productId };
  if (searchForm.nickName && searchForm.nickName.trim()) {
    params.nickName = searchForm.nickName.trim();
  }
  getAgentMemoryByNickName(params)
    .then((res) => {
      const { data, errorCode } = res.data;
      if (errorCode == 2001) {
        router.push('/login');
      }
      if (errorCode == 200 && data && Array.isArray(data)) {
        dataSource.value = data.map((item: any) => ({
          key: item.id,
          id: item.id,
          chatId: item.chatId,
          deviceName: item.deviceName || '未知设备',
          content: item.content
        }));
      } else {
        dataSource.value = [];
      }
    })
    .catch((err) => {
      console.error('按昵称搜索记忆失败:', err);
      message.error('按昵称搜索记忆失败');
    });
};

const handleReset = () => {
  searchForm.productId = null;
  searchForm.nickName = '';
  dataSource.value = [];
  fetchMemory();
  // Resume polling
  clearInterval(intervalId);
  intervalId = setInterval(fetchMemory, 1000);
};

// 查看记忆详情
const handleView = (record: MemoryRecord) => {
  currentRecord.value = { ...record };
  viewModalVisible.value = true;
};

// 打开修改弹窗
const handleEdit = (record: MemoryRecord) => {
  currentRecord.value = { ...record };
  editForm.chatId = record.chatId;
  editForm.deviceName = record.deviceName;
  editForm.content = record.content;
  editModalVisible.value = true;
};

// 提交修改
const handleEditSubmit = () => {
  if (!editForm.content || !editForm.content.trim()) {
    message.warning('记忆内容不能为空');
    return;
  }

  const params = {
    chatId: editForm.chatId,
    content: editForm.content.trim()
  };

  putAgentMemory(params)
    .then((res) => {
      const { errorCode } = res.data;
      if(errorCode == 200) {
        message.success('修改成功');
        editModalVisible.value = false;
        fetchMemory();
      } else if(errorCode == 2001) {
        router.push('/login');
      } else {
        message.error('修改失败');
      }
    })
    .catch((err) => {
      console.error('修改记忆失败:', err);
      message.error('修改记忆失败');
    });
};

// 取消修改
const handleEditCancel = () => {
  editModalVisible.value = false;
  editForm.chatId = '';
  editForm.deviceName = '';
  editForm.content = '';
};

// 删除记忆
const handleDelete = (record: MemoryRecord) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除设备"${record.deviceName}"的记忆吗？此操作不可恢复。`,
    okText: '确认',
    cancelText: '取消',
    okType: 'danger',
    onOk() {
      deleteAgentMemory({ id: record.id })
        .then((res) => {
          const { errorCode } = res.data;
          if(errorCode == 200){
            message.success('删除成功');
            fetchMemory();
          } else if(errorCode == 2001){
            router.push('/login');
          } else {
            message.error('删除失败');
          }
        })
        .catch((err) => {
          console.error('删除记忆失败:', err);
          message.error('删除记忆失败');
        });
    }
  });
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

    .content-cell {
      max-width: 300px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}

.memory-detail {
  .content-text {
    max-height: 400px;
    overflow-y: auto;
    white-space: pre-wrap;
    word-break: break-word;
    line-height: 1.6;
  }
}

:deep(.ant-descriptions-item-label) {
  font-weight: 500;
  background-color: #fafafa;
}
</style>