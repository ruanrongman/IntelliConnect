<template>  
  <div class="table-container">  
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
import { getAgentMemory, putAgentMemory, deleteAgentMemory } from '@/api/agentMemory';   
import { useRouter } from 'vue-router';   
import { DeleteOutlined, EyeOutlined, EditOutlined } from '@ant-design/icons-vue';   

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

onMounted(() => {   
  fetchMemory();   
  intervalId = setInterval(fetchMemory, 1000); // 每 60 秒钟刷新一次数据  
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

// 查看记忆详情  
const handleView = (record: MemoryRecord) => {  
  console.log('handleView called', record);
  currentRecord.value = { ...record };  
  viewModalVisible.value = true;  
  console.log('viewModalVisible:', viewModalVisible.value);
};  

// 打开修改弹窗  
const handleEdit = (record: MemoryRecord) => {  
  console.log('handleEdit called', record);
  currentRecord.value = { ...record };  
  editForm.chatId = record.chatId;  
  editForm.deviceName = record.deviceName;  
  editForm.content = record.content;  
  editModalVisible.value = true;  
  console.log('editModalVisible:', editModalVisible.value);
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
        fetchMemory(); // 刷新列表  
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
  // 重置表单  
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
      console.log('Deleting record:', record);   
      deleteAgentMemory({ id: record.id })  
        .then((res) => {   
          const { errorCode } = res.data;   
          if(errorCode == 200){   
            message.success('删除成功');  
            fetchMemory(); // 刷新列表  
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