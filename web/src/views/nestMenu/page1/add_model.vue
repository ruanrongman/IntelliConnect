<template>
  <div class="add-model">
    <a-button 
      type="primary" 
      @click="showModal"
      class="add-button"
    >
      <template #icon>
        <PlusOutlined />
      </template>
      新建配置
    </a-button>

    <a-modal
      v-model:visible="visible"
      title="新建配置"
      @ok="handleCreate"
      @cancel="handleCancel"
      class="custom-modal"
    >
      <a-form
        :model="formState"
        name="basic"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
        autocomplete="off"
        @finish="onFinish"
        @finishFailed="onFinishFailed"
      >
        <a-form-item
          label="用户名"
          name="username"
          :rules="[{ required: true, message: '请输入用户名' }]"
        >
          <a-input v-model:value="formState.username" placeholder="请输入用户名" />
        </a-form-item>

        <a-form-item
          label="密码"
          name="password"
          :rules="[{ required: true, message: '请输入密码' }]"
        >
          <a-input-password v-model:value="formState.password" placeholder="请输入密码" />
        </a-form-item>

        <a-form-item
          label="产品"
          name="id"
          :rules="[{ required: true, message: '请选择产品' }]"
        >
          <a-select
            ref="select"
            v-model:value="formState.id"
            placeholder="请选择产品"
            :options="options1"
            class="custom-select"
          />
        </a-form-item>

        <a-form-item name="remember" :wrapper-col="{ offset: 6, span: 16 }">
          <a-checkbox v-model:checked="formState.remember">记住我</a-checkbox>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { reactive,ref ,onMounted } from 'vue';
import { getProduct,deleteProduct} from '@/api/product';
import { PlusOutlined } from '@ant-design/icons-vue'

const visible = ref(false)
const options1 = ref([]);
const fetchProduct = () => {
  getProduct()
    .then((res) => {
      const { data, errorCode } = res.data;
      if(errorCode==2001){
        router.push('/login')
      }
      options1.value = data.map((item, index) => ({
        value: item.id,
        label: item.productName,
      }));
    })
    .catch((err) => {
      console.log(err);
    });
};
onMounted(() => {
  fetchProduct()
})
  const showModal = () => {
  visible.value = !visible.value
}

const handleCancel = () => {
  visible.value = false
}

const handleCreate = () => {
  visible.value = false
}
  const formState = reactive({
    username: '',
    password: '',
    id: '',
    remember: true,
  });
  const onFinish = values => {
    console.log('Success:', values);
  };
  const onFinishFailed = errorInfo => {
    console.log('Failed:', errorInfo);
  };
  </script>

<style lang="scss" scoped>
.add-model {
  .add-button {
    display: flex;
    align-items: center;
    gap: 6px;
    height: 38px;
    border-radius: 6px;
    font-weight: 500;
    
    &:hover {
      opacity: 0.85;
    }
  }
}

:deep(.custom-modal) {
  .ant-modal-content {
    border-radius: 8px;
  }

  .ant-modal-header {
    border-radius: 8px 8px 0 0;
  }

  .ant-form-item {
    margin-bottom: 24px;
  }

  .ant-input,
  .ant-input-password,
  .custom-select {
    border-radius: 6px;
  }
}
</style>