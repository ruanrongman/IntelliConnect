<template>
  <div class="table-container">
    <a-table
      :columns="columns"
      :data-source="dataSource"
      :pagination="pagination"
      class="custom-table"
    >
      <template #status="{ record }">
        <a-tag
          :key="record.status"
          :color="record.status === 'disconnected' ? 'error' : 'success'"
          class="status-tag"
        >
          {{ record.status === 'connected' ? '在线' : '离线' }}
        </a-tag>
      </template>
      <template #action="{ record }">
        <a-button
          type="link"
          @click="handleEdit(record)"
        >
          <template #icon><EditOutlined /></template>
          编辑
        </a-button>
        <a-button
          type="link"
          danger
          @click="handleDelete(record)"
        >
          <template #icon><DeleteOutlined /></template>
          解除绑定
        </a-button>
      </template>
    </a-table>

    <!-- 编辑弹窗 -->
    <a-modal
      v-model:visible="editVisible"
      title="编辑设备昵称"
      @ok="handleEditOk"
      @cancel="handleEditCancel"
    >
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="设备昵称">
          <a-input v-model:value="editForm.nickName" placeholder="请输入设备昵称" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, reactive } from 'vue';
import { message , Modal } from 'ant-design-vue'
import { getXiaoZhiManager,deleteXiaoZhiManager, putXiaoZhiManager} from '@/api/xiaoZhi';
import { useRouter } from 'vue-router'
import { DeleteOutlined, EditOutlined } from '@ant-design/icons-vue'

const router = useRouter()

const pagination = {
  pageSize: 5,
};

const dataSource = ref([]);
const editVisible = ref(false);
const editForm = reactive({
  id: null,
  nickName: ''
});

const columns = [
  {
    title: '产品id',
    dataIndex: 'productId',
    key: 'productId',
  },
  {
    title: '产品mac地址',
    dataIndex: 'deviceId',
    key: 'deviceId',
  },
  {
    title: '设备昵称',
    dataIndex: 'nickName',
    key: 'nickName',
    customRender: ({ text }) => text || '无'
  },
  {
    title: '板子类型',
    dataIndex: 'boardType',
    key: 'boardType',
  },
  {
    title: '板子名称',
    dataIndex: 'boardName',
    key: 'boardName',
  },
  {
    title: '绑定用户名',
    dataIndex: 'userName',
    key: 'userName',
  },
  {
    title: '连接状态',
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

const fetchProduct = () => {
  getXiaoZhiManager()
    .then((res) => {
      const { data, errorCode } = res.data;
      if(errorCode==2001){
        router.push('/login')
      }
      if(errorCode==200&& data && Array.isArray(data)){
        dataSource.value = data.map((item, index) => ({
          key: index,
          id: item.id,
          productId: item.productId,
          deviceId: item.deviceId,
          nickName: item.nickName,
          boardType: item.boardType,
          boardName: item.boardName,
          userName: item.userName,
          status: item.status
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
  editForm.id = record.id;
  editForm.nickName = record.nickName || '';
  editVisible.value = true;
};

const handleEditOk = () => {
  if (!editForm.nickName.trim()) {
    message.warning('请输入设备昵称');
    return;
  }

  putXiaoZhiManager({
    id: editForm.id,
    nickName: editForm.nickName
  }).then((res) => {
    const { data, errorCode } = res.data;
    if(errorCode==200){
      message.success("修改成功");
      editVisible.value = false;
      fetchProduct();
    } else if(errorCode==2001){
      router.push('/login')
    } else {
      message.error("修改失败");
    }
  }).catch((err) => {
    console.log(err);
    message.error("修改失败");
  });
};

const handleEditCancel = () => {
  editVisible.value = false;
  editForm.id = null;
  editForm.nickName = '';
};

const handleDelete = (record) => {
  // 实现删除功能
  Modal.confirm({
    title: '确认解绑',
    // 使用 record.productName 来获取产品名称
    content: `确定要解绑"${record.deviceId}"的设备吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk() {
    console.log('Deleting record:', record);
    deleteXiaoZhiManager({id:record.id}).then((res) => {
        const { data, errorCode } = res.data;
        if(errorCode==200){
          message.success("解绑成功")
        }else if(errorCode==2001){
          router.push('/login')
        }
        console.log(data)
      })
      .catch((err) => {
        console.log(err);
      });
  }});
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

    .status-tag {
      border-radius: 4px;
      padding: 4px 8px;
      font-size: 13px;
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