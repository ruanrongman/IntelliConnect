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
            type="primary"
            size="small"
            @click="showOtaModal(record)"  
          >  
            主动空中升级  
          </a-button>
          <a-button   
            type="link"   
            danger  
            @click="handleDelete(record)"  
          >  
            <template #icon><DeleteOutlined /></template>  
            删除固件  
          </a-button>  
        </a-space>
      </template>  
    </a-table>  

    <!-- 空中升级弹窗 -->
    <a-modal     
      :visible="otaVisible"      
      :footer="null"    
      :closable="!submitting"    
      :mask-closable="!submitting"    
      @cancel="handleOtaCancel"    
      title="空中升级配置"    
    >    
      <a-form    
        :model="otaFormState"    
        name="otaForm"    
        :label-col="{ span: 6 }"    
        :wrapper-col="{ span: 16 }"    
        autocomplete="off"    
        @finish="onOtaFinish"    
        @finishFailed="onOtaFinishFailed"    
      >    
        <a-form-item    
          label="固件名称"    
          name="name"    
        >    
          <a-input v-model:value="otaFormState.name" disabled />    
        </a-form-item>    

        <a-form-item    
          label="设备"    
          name="deviceName"    
          :rules="[{ required: true, message: '请选择设备!' }]"    
        >    
          <a-select    
            v-model:value="otaFormState.deviceName"    
            :options="deviceOptions"    
            placeholder="请选择设备"    
            :disabled="submitting"    
            :loading="loadingDevices"    
            allowClear    
            show-search    
            :filter-option="filterOption"    
          />    
        </a-form-item>    

        <a-form-item :wrapper-col="{ offset: 8, span: 16 }">    
          <a-button     
            type="primary"     
            html-type="submit"    
            :loading="submitting"    
            :disabled="submitting"    
          >    
            {{ submitting ? '启动升级中...' : '启动升级' }}    
          </a-button>    
          <a-button     
            style="margin-left: 10px"     
            @click="handleOtaCancel"    
            :disabled="submitting"    
          >    
            取消    
          </a-button>    
        </a-form-item>    
      </a-form>    
    </a-modal>
  </div>  
</template>  
  
<script setup>  
import { ref, reactive, onMounted, onUnmounted } from 'vue';  
import { message } from 'ant-design-vue'  
import { otaList, otaDelete, otaEnable } from '@/api/productOta';  
import { getProductDevice } from '@/api/device';
import { useRouter } from 'vue-router'  
import { DeleteOutlined } from '@ant-design/icons-vue'  
  
const router = useRouter()  
  
const pagination = {  
  pageSize: 5,  
};  
  
const dataSource = ref([]);  

// 空中升级相关状态
const otaVisible = ref(false);
const submitting = ref(false);
const deviceOptions = ref([]);
const loadingDevices = ref(false);

// 空中升级表单数据
const otaFormState = reactive({
  name: '',        // 固件名称 (从选中的记录获取)
  deviceName: '',  // 设备名称
});

const columns = [  
  {  
    title: '固件id',  
    dataIndex: 'id',  
    key: 'id',  
  },  
  {  
    title: '固件路径',  
    dataIndex: 'path',  
    key: 'path',  
  },  
  {  
    title: '固件名称',  
    dataIndex: 'name',  
    key: 'name',  
  },  
  {  
    title: '产品id',  
    dataIndex: 'productId',  
    key: 'productId',  
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
  intervalId = setInterval(fetchProduct, 1000);
});  
  
onUnmounted(() => {  
  clearInterval(intervalId);  
});  
  
const fetchProduct = () => {  
  otaList()  
    .then((res) => {  
      const { data, errorCode } = res.data;  
      if(errorCode==2001){  
        router.push('/login')  
      }  
      if(errorCode==200&& data && Array.isArray(data)){  
        dataSource.value = data.map((item, index) => ({  
          key: index,  
          id: item.id,  
          path: item.path,  
          name: item.name,  
          productId: item.productId,  
        }));  
      } else {  
        dataSource.value = [];  
      }  
    })  
    .catch((err) => {  
      console.log(err);  
    });  
};  

// 获取设备列表
const fetchDeviceList = async () => {    
  if (loadingDevices.value) return    
      
  loadingDevices.value = true    
  try {    
    const res = await getProductDevice()    
    const { data, errorCode } = res.data    
        
    if (errorCode === 2001) {    
      message.error('登录已过期，请重新登录')    
      router.push('/login')    
      return    
    }    
        
    if (errorCode === 200) {    
      deviceOptions.value = data.map(item => ({
        value: item.name,
        label: item.name,
      }));
    } else {    
      message.error('获取设备列表失败')    
      deviceOptions.value = []    
    }    
  } catch (error) {    
    console.error('获取设备列表错误:', error)    
    message.error('获取设备列表失败')    
    deviceOptions.value = []    
  } finally {    
    loadingDevices.value = false    
  }    
}

// 显示空中升级弹窗
const showOtaModal = (record) => {
  otaFormState.name = record.name;
  otaFormState.deviceName = '';
  fetchDeviceList();
  otaVisible.value = true;
};

// 取消空中升级操作
const handleOtaCancel = () => {
  if (submitting.value) {
    message.warning('正在处理中，请稍候...');
    return;
  }
  resetOtaForm();
  otaVisible.value = false;
};

// 重置空中升级表单
const resetOtaForm = () => {
  otaFormState.name = '';
  otaFormState.deviceName = '';
};

// 搜索过滤函数    
const filterOption = (input, option) => {    
  return option.label.toLowerCase().includes(input.toLowerCase());    
};

// 处理空中升级提交
const handleOtaSubmit = async () => {
  submitting.value = true;
  
  try {
    const hideMessage = message.loading('正在启动空中升级，请稍候...', 0);
    
    const params = {
      name: otaFormState.name,
      deviceName: otaFormState.deviceName
    };
    
    console.log('空中升级参数:', params);
    
    const res = await otaEnable(params);
    
    hideMessage();
    
    const { data, errorCode } = res.data;
    console.log('空中升级响应:', data);
    
    if (errorCode === 200) {
      message.success('空中升级启动成功!');
      resetOtaForm();
      otaVisible.value = false;
    } else if (errorCode === 2001) {
      message.error('登录已过期，请重新登录');
      router.push('/login');
    } else {
      message.error(`升级启动失败: ${res.data.message || '未知错误'}`);
    }
    
  } catch (error) {
    console.error('空中升级错误:', error);
    message.error(`升级启动失败: ${error.message || '网络错误'}`);
  } finally {
    submitting.value = false;
  }
};

// 空中升级表单提交成功
const onOtaFinish = (values) => {
  console.log('空中升级表单验证成功:', values);
  handleOtaSubmit();
};

// 空中升级表单提交失败
const onOtaFinishFailed = (errorInfo) => {
  console.log('空中升级表单验证失败:', errorInfo);
  message.error('请检查表单信息');
};
  
const handleDelete = (record) => {  
  console.log('Deleting record:', record);  
  otaDelete({id:record.id}).then((res) => {  
      const { data, errorCode } = res.data;  
      if(errorCode==200){  
        message.success("删除成功")  
      }else if(errorCode==3002){  
        message.warn("删除失败，固件正在被升级系统使用中")  
      }else if(errorCode==2001){  
        router.push('/login')  
      }else{  
        message.error("删除失败")  
      }  
      console.log(data)  
    })  
    .catch((err) => {  
      console.log(err);  
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
  }  
}  
</style>