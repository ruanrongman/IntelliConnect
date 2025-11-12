<template>
  <a-table :columns="columns" :data-source="dataSource" :pagination="pagination">
    <template #action="{ record }">
        <div class="action-buttons">
          <a-button 
            type="link" 
            danger
            @click="handleDelete(record)"
            class="action-button"
          >
            <template #icon><DeleteOutlined /></template>
            删除信息
          </a-button>
        </div>
      </template>
  </a-table>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { getOtaPassive,deleteOtaPassive } from '@/api/productOtaPassive';
import { Descriptions, message, Modal } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { descriptionsContext } from 'ant-design-vue/lib/descriptions';
const router = useRouter()

const pagination = {
  pageSize: 5,
};

const dataSource = ref([]);

const columns = [
  {
    title: '被动升级id',
    dataIndex: 'id',
    key: 'id',
  },
  {
    title: '固件id',
    dataIndex: 'otaId',
    key: 'otaId',
  },
  {
    title: '固件名称',
    dataIndex: 'otaName',
    key: 'otaName',
  },
  {
    title: '设备id',
    dataIndex: 'deviceId',
    key: 'deviceId',
  },
  {
    title: '设备名称',
    dataIndex: 'deviceName',
    key: 'deviceName',
  },
  {
    title: '版本名称',
    dataIndex: 'versionName',
    key: 'versionName',
  },
  {
    title: '新固件描述',
    dataIndex: 'description',
    key: 'description',
    width: 200,
    ellipsis: true,
  },
  {
    title: 'Action',
    key: 'action',
    slots: { customRender: 'action' },
  },
];

let intervalId;

onMounted(() => {
  fetchproductModel();
  intervalId = setInterval(fetchproductModel, 1000); // 每 60 秒钟刷新一次数据
});

onUnmounted(() => {
  clearInterval(intervalId);
});

const fetchproductModel = () => {
  getOtaPassive()
    .then((res) => {
      const { data, errorCode } = res.data;
      if(errorCode==2001){
        router.push('/login')
      }
      if(errorCode==200&& data && Array.isArray(data)){
        dataSource.value = data.map((item, index) => ({
          key: index,
          id: item.id,
          otaId: item.otaId,
          deviceId: item.deviceId,
          versionName: item.versionName,
          otaName: item.otaName,
          deviceName: item.deviceName,
          description: item.description
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

const handleDelete = (record) => {  
  // 添加删除确认
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除设备"${record.deviceName}"的升级信息吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk() {
      deleteOtaPassive({id:record.id}).then((res) => {  
          const { data, errorCode } = res.data;  
          if(errorCode==200){  
            message.success("删除成功");
            // 删除成功后刷新数据
            fetchproductModel();
          }else if(errorCode==2001){  
            router.push('/login')  
          } else {
            message.error("删除失败");
          }
        })  
        .catch((err) => {  
          console.log(err);
          message.error("删除失败");
        });
    },
  });
};  
</script>