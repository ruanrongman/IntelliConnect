<template>
  <div class="table-container">
    <a-table
      :columns="columns"
      :data-source="dataSource"
      :pagination="pagination"
      class="custom-table"
    >
      <template #action="{ record }">
        <a-space>
          <a-button
            type="link"
            size="small"
            @click="handleEdit(record)"
          >
            <template #icon><EditOutlined /></template>
            修改配置
          </a-button>
          <a-button
            type="link"
            size="small"
            danger
            @click="handleDelete(record)"
          >
            <template #icon><DeleteOutlined /></template>
            删除配置
          </a-button>
        </a-space>
      </template>
    </a-table>

    <!-- 修改配置弹窗 -->
    <a-modal
      v-model:visible="editModalVisible"
      title="修改ASR配置"
      :footer="null"
      width="600px"
      @cancel="handleEditCancel"
    >
      <div v-if="!editLoading">
        <a-form
          :model="editForm"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 18 }"
          ref="editFormRef"
        >
          <a-form-item label="产品ID" name="productId">
            <a-input-number
              v-model:value="editForm.productId"
              :min="0"
              :disabled="true"
              style="width: 100%"
            />
          </a-form-item>

          <a-form-item label="ASR服务" name="asrName">
            <a-select
              v-model:value="editForm.asrName"
              placeholder="请选择ASR服务"
              style="width: 100%"
              :options="asrOptions"
            />
          </a-form-item>

          <a-form-item label="提供商" name="providerName">
            <a-select
              v-model:value="editForm.providerName"
              placeholder="请选择提供商"
              style="width: 100%"
              :options="providerOptions"
            />
          </a-form-item>
        </a-form>

        <!-- 操作按钮 -->
        <div class="modal-footer">
          <a-space>
            <a-button @click="handleEditCancel">
              取消
            </a-button>
            <a-button
              type="primary"
              :loading="editSubmitting"
              @click="handleEditSubmit"
            >
              <template #icon><SaveOutlined /></template>
              保存修改
            </a-button>
          </a-space>
        </div>
      </div>

      <!-- 加载状态 -->
      <div v-if="editLoading" style="text-align: center; padding: 40px;">
        <a-spin size="large" />
        <div style="margin-top: 16px; color: #666;">正在加载配置信息...</div>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { message, Modal } from 'ant-design-vue'
import { getProductAsr, deleteProductAsr, putProductAsr } from '@/api/productAsr';
import { useRouter } from 'vue-router'
import { DeleteOutlined, EditOutlined, SaveOutlined } from '@ant-design/icons-vue'

const router = useRouter()

// ASR服务选项
const asrOptions = ref([
  { value: 'dashscope', label: 'DashScope (阿里云)' },
  { value: 'funasr', label: 'FunASR' }
])

// 提供商选项
const providerOptions = ref([
  { value: 'default', label: '默认' }
])

// ASR名称显示映射
const asrDisplayMap = {
  'dashscope': 'DashScope (阿里云)',
  'funasr': 'FunASR'
}

// 编辑相关状态
const editModalVisible = ref(false)
const editLoading = ref(false)
const editSubmitting = ref(false)
const editFormRef = ref(null)
const currentRecord = ref(null)

// 编辑表单数据
const editForm = ref({
  productId: 0,
  asrName: '',
  providerName: ''
})

const pagination = {
  pageSize: 10,
};

const dataSource = ref([]);

const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    key: 'id',
    width: 80,
  },
  {
    title: '产品ID',
    dataIndex: 'productId',
    key: 'productId',
    width: 100,
  },
  {
    title: 'ASR服务',
    dataIndex: 'asrName',
    key: 'asrName',
    width: 180,
    customRender: ({ record }) => {
      return asrDisplayMap[record.asrName] || record.asrName
    }
  },
  {
    title: '提供商',
    dataIndex: 'providerName',
    key: 'providerName',
    width: 120,
  },
  {
    title: '操作',
    key: 'action',
    slots: { customRender: 'action' },
    width: 200,
    fixed: 'right',
  },
];

let intervalId;

onMounted(() => {
  fetchProductAsr();
  intervalId = setInterval(fetchProductAsr, 1000);
});

onUnmounted(() => {
  clearInterval(intervalId);
});

const fetchProductAsr = () => {
  getProductAsr()
    .then((res) => {
      const { data, errorCode } = res.data;
      if (errorCode == 2001) {
        router.push('/login')
      }
      if (errorCode == 200 && data && Array.isArray(data)) {
        dataSource.value = data.map((item, index) => ({
          key: index,
          id: item.id,
          productId: item.productId,
          asrName: item.asrName,
          providerName: item.providerName,
        }));
      } else {
        dataSource.value = [];
      }
    })
    .catch((err) => {
      console.error('获取ASR配置数据失败:', err);
      message.error('获取数据失败');
    });
};

// 处理编辑按钮点击
const handleEdit = async (record) => {
  try {
    editModalVisible.value = true
    editLoading.value = true
    currentRecord.value = record

    // 模拟加载过程
    await new Promise(resolve => setTimeout(resolve, 300))

    // 填充表单数据
    editForm.value = {
      productId: record.productId,
      asrName: record.asrName,
      providerName: record.providerName
    }

  } catch (error) {
    console.error('获取配置详情失败:', error)
    message.error('获取配置详情失败')
  } finally {
    editLoading.value = false
  }
}

// 处理编辑提交
const handleEditSubmit = async () => {
  try {
    editSubmitting.value = true

    const response = await putProductAsr(editForm.value)
    const { data, errorCode } = response.data

    if (errorCode === 200) {
      message.success('配置修改成功')
      editModalVisible.value = false
      fetchProductAsr() // 刷新数据
    } else if (errorCode === 2001) {
      router.push('/login')
    } else {
      message.error('修改失败，请重试')
    }

  } catch (error) {
    console.error('修改配置失败:', error)
    message.error('修改失败，请重试')
  } finally {
    editSubmitting.value = false
  }
}

// 处理编辑取消
const handleEditCancel = () => {
  editModalVisible.value = false
  currentRecord.value = null

  // 重置表单
  editForm.value = {
    productId: 0,
    asrName: '',
    providerName: ''
  }

  // 清除表单验证
  if (editFormRef.value) {
    editFormRef.value.resetFields()
  }
}

const handleDelete = (record) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除产品ID为 ${record.productId} 的ASR配置吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk() {
      return new Promise((resolve, reject) => {
        deleteProductAsr({ id: record.id })
          .then((res) => {
            const { data, errorCode } = res.data;
            if (errorCode == 200) {
              message.success("删除成功");
              fetchProductAsr();
              resolve();
            } else if (errorCode == 2001) {
              router.push('/login');
              resolve();
            } else {
              message.error('删除失败');
              reject();
            }
          })
          .catch((err) => {
            console.error('删除失败:', err);
            message.error('删除失败');
            reject();
          });
      });
    }
  });
};
</script>

<style lang="scss" scoped>
.table-container {
  overflow-x: auto;
}

.custom-table {
  min-width: 680px;
}

:deep(.ant-btn-link) {
  padding: 0 4px;
  height: auto;
  line-height: 1.2;
}

:deep(.ant-space-item) {
  line-height: 1.2;
}

.modal-footer {
  margin-top: 24px;
  text-align: right;
}
</style>
