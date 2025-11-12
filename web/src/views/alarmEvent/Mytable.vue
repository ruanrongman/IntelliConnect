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
import { getAlarmEvent,deleteAlarmEvent } from '@/api/alarmEvent';
import { useRouter } from 'vue-router'
const router = useRouter()

const pagination = {
  pageSize: 5,
};

const dataSource = ref([]);

const columns = [
  {
    title: '事件告警id',
    dataIndex: 'id',
    key: 'id',
  },
  {
    title: '事件id',
    dataIndex: 'eventId',
    key: 'eventId',
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
  getAlarmEvent()
    .then((res) => {
      const { data, errorCode } = res.data;
      if(errorCode==2001){
        router.push('/login')
      }
      if(errorCode==200&& data && Array.isArray(data)){
      dataSource.value = data.map((item, index) => ({
        key: index,
        id: item.id,
        eventId: item.eventId,
      }));
    }else{
      dataSource.value = [];
    }
    })
    .catch((err) => {
      console.log(err);
    });
};

const handleDelete = (record) => {
  // 实现删除功能
  console.log('Deleting record:', record);
  deleteAlarmEvent({id:record.id}).then((res) => {
      const { data, errorCode } = res.data;
      if(errorCode==2001){
        router.push('/login')
      }
      console.log(data)
    })
    .catch((err) => {
      console.log(err);
    });
};
</script>