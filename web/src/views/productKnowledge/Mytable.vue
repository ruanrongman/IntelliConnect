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
      <template #description="{ text }">
        <a-tooltip :title="text || '-'">
          <span class="description-text">{{ text || '-' }}</span>
        </a-tooltip>
      </template>
      <template #action="{ record }">
        <a-space>
          <a-button type="link" @click="handleEdit(record)">
            <template #icon><EditOutlined /></template>
            编辑
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

    <a-modal
      :visible="editVisible"
      title="修改知识描述"
      :confirm-loading="updating"
      :closable="!updating"
      :mask-closable="!updating"
      ok-text="保存"
      cancel-text="取消"
      @ok="handleUpdate"
      @cancel="handleEditCancel"
    >
      <a-form ref="editFormRef" :model="editState" layout="vertical">
        <a-form-item
          label="知识描述"
          name="description"
          :rules="[
            { required: true, whitespace: true, message: '请输入知识描述！' },
            { max: 1024, message: '知识描述不能超过 1024 个字符！' }
          ]"
        >
          <a-textarea
            v-model:value="editState.description"
            :disabled="updating"
            :maxlength="1024"
            :rows="6"
            show-count
            placeholder="请输入该知识文件的内容说明"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, onUnmounted } from 'vue';
import { message, Modal } from 'ant-design-vue'
import { getKnowledgeChat, putKnowledgeChat, deleteKnowledgeChat } from '@/api/productKnowledge';
import { useRouter } from 'vue-router'
import { DeleteOutlined, EditOutlined } from '@ant-design/icons-vue'

const router = useRouter()

const pagination = {
  pageSize: 5,
};

const dataSource = ref([]);
const editFormRef = ref();
const editVisible = ref(false);
const updating = ref(false);
const editState = reactive({
  id: undefined,
  description: '',
});

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
    title: '知识描述',
    dataIndex: 'description',
    key: 'description',
    slots: { customRender: 'description' },
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
          description: item.description,
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

const handleEdit = (record) => {
  editState.id = record.id;
  editState.description = record.description || '';
  editVisible.value = true;
};

const handleEditCancel = () => {
  if (updating.value) return;
  editVisible.value = false;
  editFormRef.value?.resetFields();
};

const handleUpdate = async () => {
  try {
    await editFormRef.value?.validate();
    updating.value = true;
    const res = await putKnowledgeChat({
      id: editState.id,
      description: editState.description.trim(),
    });
    const { errorCode, errorMsg } = res.data;
    if (errorCode === 200) {
      message.success('知识描述修改成功');
      editVisible.value = false;
      editFormRef.value?.resetFields();
      await fetchProduct();
    } else if (errorCode === 2001) {
      router.push('/login');
    } else {
      message.error(errorMsg || '知识描述修改失败');
    }
  } catch (error) {
    if (!error?.errorFields) {
      console.log(error);
      message.error('知识描述修改失败，请重试');
    }
  } finally {
    updating.value = false;
  }
};

const handleDelete = (record) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除知识文件 "${record.filename}" 吗？此操作不可恢复。`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk() {
      return doDelete(record.id);
    },
  });
};

const doDelete = async (id) => {
  try {
    const res = await deleteKnowledgeChat({ id });
    const { errorCode, errorMsg } = res.data;
    if (errorCode === 200) {
      message.success('删除成功');
      fetchProduct();
    } else if (errorCode === 3002) {
      message.warn('删除失败，知识库正在被系统使用中');
    } else if (errorCode === 2001) {
      router.push('/login');
    } else {
      message.error(errorMsg || '删除失败');
    }
  } catch (err) {
    console.log(err);
    message.error('删除失败，请重试');
  }
};
</script>

<style lang="scss" scoped>
.table-container {
  .description-text {
    display: block;
    max-width: 320px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
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
  }
}
</style>
