<template>
  <div>
    <!-- 数据表格 -->
    <a-table :columns="columns" :data-source="dataSource" :pagination="pagination">
      <template #action="{ record }">
        <div class="action-buttons">
          <a-button 
            type="link" 
            @click="handleEdit(record)"
            class="action-button"
          >
            <template #icon><EditOutlined /></template>
            修改
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

    <!-- 修改模态框 -->
    <a-modal
      v-model:visible="modalVisible"
      title="修改长期记忆"
      @ok="handleModalOk"
      @cancel="handleModalCancel"
    >
      <a-form
        :model="formState"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 18 }"
      >
        <a-form-item label="想要记忆的概念" name="memoryKey">
          <a-input 
            v-model:value="formState.memoryKey" 
            disabled
            placeholder="记忆的唯一标识（不可修改）"
          />
        </a-form-item>
        <a-form-item
        label="描述"
        name="description"
        :rules="[{ required: true, message: '请输入描述!' }]"
      >
        <a-input 
          v-model:value="formState.description" 
          placeholder="请输入描述"
          :maxlength="255"
          show-count
        />
      </a-form-item>

      <a-form-item
        label="记忆内容"
        name="memoryValue"
        :rules="[{ required: true, message: '请输入初始的记忆内容!' }]"
      >
        <a-textarea 
          v-model:value="formState.memoryValue" 
          placeholder="请输入记忆内容"
          :rows="4"
          :maxlength="1000"
          show-count
        />
      </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, reactive } from 'vue';
import { message, Modal } from 'ant-design-vue'; // 从这里导入 Modal
import { useRouter } from 'vue-router';
// 引入新的API
import { getLongMemory, postLongMemory, deleteLongMemory } from '@/api/agentLongMemory';
// 引入图标
import { EditOutlined, DeleteOutlined } from '@ant-design/icons-vue';

const router = useRouter();

const pagination = {
  pageSize: 5,
};

const dataSource = ref([]);

// 表格列定义
const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    key: 'id',
    width: 80,
  },
  {
    title: '想要记忆的概念',
    dataIndex: 'memoryKey',
    key: 'memoryKey',
  },
  {
    title: '描述',
    dataIndex: 'description',
    key: 'description',
  },
  {
    title: '记忆内容',
    dataIndex: 'memoryValue',
    key: 'memoryValue',
  },
  {
    title: '产品ID',
    dataIndex: 'productId',
    key: 'productId',
    width: 100,
  },
  {
    title: '操作',
    key: 'action',
    fixed: 'right',
    width: 150,
    slots: { customRender: 'action' },
  },
];

// 模态框相关状态
const modalVisible = ref(false);

// 表单状态，使用reactive更方便
const formState = reactive({
  id: null,
  memoryKey: '',
  description: '',
  memoryValue: '',
  productId: null,
});

let intervalId;

onMounted(() => {
  fetchLongMemory();
  // 每1秒钟刷新一次数据
  intervalId = setInterval(fetchLongMemory, 1000);
});

onUnmounted(() => {
  clearInterval(intervalId);
});

// 获取数据
const fetchLongMemory = () => {
  getLongMemory()
    .then((res) => {
      const { data, errorCode } = res.data;
      if (errorCode == 2001) {
        router.push('/login');
        return;
      }
      if (errorCode == 200 && data && Array.isArray(data)) {
        dataSource.value = data;
      } else {
        dataSource.value = [];
      }
    })
    .catch((err) => {
      console.error('获取长期记忆失败:', err);
      message.error('获取数据失败');
    });
};

// 打开编辑模态框
const handleEdit = (record) => {
  // 填充表单数据
  Object.assign(formState, record);
  modalVisible.value = true;
};

// 模态框确认按钮
const handleModalOk = async () => {
  // 基本校验
  if (!formState.memoryKey || !formState.description || !formState.memoryValue || !formState.productId) {
    message.warn('请填写所有必填项');
    return;
  }

  try {
    // 提交时，不发送id，由后端根据memoryKey等判断是新增还是修改
    const { id, ...payload } = formState;
    const res = await postLongMemory(payload);
    
    if (res.data.errorCode == 200) {
      message.success('修改成功');
      modalVisible.value = false;
      fetchLongMemory(); // 刷新列表
    } else if (res.data.errorCode == 2001) {
      router.push('/login');
    } else {
      message.error(res.data.errorMsg || '操作失败');
    }
  } catch (err) {
    console.error('提交失败:', err);
    message.error('操作失败，请重试');
  }
};

// 模态框取消按钮
const handleModalCancel = () => {
  modalVisible.value = false;
};

// 删除功能 - 增加了确认弹窗
const handleDelete = (record) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除 Memory Key 为 "${record.memoryKey}" 的这条记录吗？`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk() {
      // 用户点击确认后，执行真正的删除操作
      doDelete(record.id);
    },
  });
};

const doDelete = async (id) => {
  try {
    const res = await deleteLongMemory({ id });
    if (res.data.errorCode == 200) {
      message.success('删除成功');
      fetchLongMemory(); // 刷新列表
    } else if (res.data.errorCode == 2001) {
      router.push('/login');
    } else {
      message.error(res.data.errorMsg || '删除失败');
    }
  } catch (err) {
    console.error('删除失败:', err);
    message.error('删除失败，请重试');
  }
};
</script>

<style scoped>
.action-buttons {
  display: flex;
  justify-content: space-around;
}
.action-button {
  padding: 0;
}
</style>
