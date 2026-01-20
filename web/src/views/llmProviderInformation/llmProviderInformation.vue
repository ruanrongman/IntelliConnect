<template>
    <a-button type="primary" @click="showModal">
      新建模型服务商
    </a-button>
    <a-modal
      :visible="visible"
      :footer="null"
      @cancel="handleCancel"
      @create="handleCreate">
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
          label="服务商名称"
          name="providerName"
          :rules="[{ required: true, message: '请输入服务商名称!' }]"
        >
          <a-input
            v-model:value="formState.providerName"
            placeholder="请输入模型服务商名称"
            :disabled="isEditing"
          />
        </a-form-item>

        <a-form-item
          label="API Key"
          name="appKey"
          :rules="[{ required: true, message: '请输入API Key!' }]"
        >
          <a-input-password v-model:value="formState.appKey" placeholder="请输入API Key"/>
        </a-form-item>

        <a-form-item
          label="API URL"
          name="baseUrl"
          :rules="[{ required: true, message: '请输入API URL!' }]"
        >
          <a-input v-model:value="formState.baseUrl" placeholder="请输入API URL"/>
        </a-form-item>

        <a-form-item
          label="类型"
          name="type"
          :rules="[{ required: true, message: '请选择类型!' }]"
        >
          <a-select
            v-model:value="formState.type"
            placeholder="请选择类型"
          >
            <a-select-option value="openai">OpenAI</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item :wrapper-col="{ offset: 8, span: 16 }">
          <a-button type="primary" html-type="submit">{{ isEditing ? '更新' : '提交' }}</a-button>
        </a-form-item>
      </a-form>
    </a-modal>
</template>

<script setup>
import { reactive, ref, toRaw } from 'vue';
import { message } from 'ant-design-vue'
import { postLlmProviderInformation } from '@/api/llmProviderInformation';
import { useRouter } from 'vue-router';

const router = useRouter();
const visible = ref(false);
const isEditing = ref(false);

const showModal = () => {
  isEditing.value = false;
  visible.value = true;
}

const handleCancel = () => {
  visible.value = false;
  resetForm();
}

const handleCreate = () => {
  visible.value = false;
  resetForm();
}

const formState = reactive({
  id: null,
  providerName: "",
  appKey: "",
  baseUrl: "",
  type: null
});

const resetForm = () => {
  formState.id = null;
  formState.providerName = "";
  formState.appKey = "";
  formState.baseUrl = "";
  formState.type = null;
  isEditing.value = false;
};

const showEditModal = (record) => {
  Object.assign(formState, record);
  isEditing.value = true;
  visible.value = true;
};

// Expose functions to parent component
defineExpose({
  showModal,
  showEditModal
});

const handleSubmit = () => {
  // For editing, we use the same endpoint as creating since the backend handles upserts
  postLlmProviderInformation(toRaw(formState))
    .then((res) => {
      const { data, errorCode } = res.data;
      if (errorCode !== 200) {
        message.error(isEditing.value ? "更新失败!" : "创建失败!");
        console.log("error", data);
      } else {
        message.success(isEditing.value ? "更新成功!" : "创建成功!");
        console.log(data);
      }
    })
    .catch((err) => {
      console.log(err);
      message.error(isEditing.value ? "更新失败!" : "创建失败!");
    });
}

const onFinish = values => {
  console.log('Success:', values);
  handleSubmit();
  visible.value = false;
  resetForm();
};

const onFinishFailed = errorInfo => {
  console.log('Failed:', errorInfo);
};
</script>