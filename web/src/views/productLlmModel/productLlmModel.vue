<template>
    <a-button type="primary" @click="showModal">
      新建产品LLM模型
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
        <!-- 产品选择下拉框 -->
        <a-form-item
          label="产品"
          name="productId"
          :rules="[{ required: true, message: '请选择产品!' }]"
        >
          <a-select
            v-model:value="formState.productId"
            :options="productOptions"
            placeholder="请选择产品"
            allowClear
            :disabled="isEditing"
          />
        </a-form-item>

        <!-- LLM提供者选择下拉框 -->
        <a-form-item
          label="模型服务商"
          name="providerId"
          :rules="[{ required: true, message: '请选择模型服务商!' }]"
        >
          <a-select
            v-model:value="formState.providerId"
            :options="providerOptions"
            placeholder="请选择模型服务商"
            allowClear
          />
        </a-form-item>

        <a-form-item
          label="模型名称"
          name="modelName"
          :rules="[{ required: true, message: '请输入模型名称!' }]"
        >
          <a-input v-model:value="formState.modelName" placeholder="请输入模型名称"/>
        </a-form-item>

        <a-form-item
          label="工具类型"
          name="toolsId"
          :rules="[{ required: true, message: '请选择工具类型!' }]"
        >
          <a-select
            v-model:value="formState.toolsId"
            :options="toolsOptions"
            placeholder="请选择工具类型"
            allowClear
            :disabled="isEditing"
          />
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
import { postProductLlmModel } from '@/api/productLlmModel';
import { getProduct, getProductName } from '@/api/product';
import { getLlmProviderInformation } from '@/api/llmProviderInformation';
import { useRouter } from 'vue-router';

const router = useRouter();
const visible = ref(false);
const isEditing = ref(false);
const productOptions = ref([]);
const providerOptions = ref([]);
const toolsOptions = ref([
  { value: '1', label: '天气工具' },
  { value: '2', label: '控制工具' },
  { value: '3', label: '音乐工具' },
  { value: '4', label: 'AI代理工具(内部工具与外部工具协同)' },
  { value: '5', label: '聊天工具' },
  { value: '6', label: '微信绑定产品工具' },
  { value: '7', label: '微信产品激活工具' },
  { value: '8', label: '定时任务工具' },
  { value: '9', label: '产品角色工具' },
  { value: '10', label: 'MCP代理工具' },
  { value: 'classifier', label: '分类器工具' },
  { value: 'longMemory', label: '长期记忆工具' },
  { value: 'memory', label: '记忆工具' },
  { value: 'knowledgeGraphic', label: '知识图谱' },
]);

const formState = reactive({
  id: null,
  productId: null,
  providerId: null,
  modelName: "",
  toolsId: ""
});

const resetForm = () => {
  formState.id = null;
  formState.productId = null;
  formState.providerId = null;
  formState.modelName = "";
  formState.toolsId = "";
  isEditing.value = false;
};

// 获取产品数据
const fetchProducts = async () => {
  try {
    const res = await getProduct();
    const { data, errorCode } = res.data;
    if (errorCode === 2001) {
      router.push('/login');
      return;
    }

    if (errorCode === 200 && data && Array.isArray(data)) {
      // 获取每个产品的详细信息以获取名称
      productOptions.value = await Promise.all(data.map(async (item) => {
        try {
          const nameRes = await getProductName({ id: item.id });
          const { data: nameData, errorCode: nameErrorCode } = nameRes.data;
          if (nameErrorCode === 200 && nameData) {
            return {
              value: item.id,
              label: nameData
            };
          } else {
            return {
              value: item.id,
              label: `产品ID: ${item.id}`
            };
          }
        } catch (err) {
          console.error(`获取产品ID ${item.id} 名称失败:`, err);
          return {
            value: item.id,
            label: `产品ID: ${item.id}`
          };
        }
      }));
    } else {
      productOptions.value = [];
    }
  } catch (err) {
    console.error('获取产品数据失败:', err);
    message.error('获取产品数据失败');
    productOptions.value = [];
  }
};

// 获取LLM提供者数据
const fetchLlmProviders = async () => {
  try {
    const res = await getLlmProviderInformation();
    const { data, errorCode } = res.data;
    if (errorCode === 2001) {
      router.push('/login');
      return;
    }

    if (errorCode === 200 && data && Array.isArray(data)) {
      providerOptions.value = data.map(item => {
        // 格式: "服务商名称 (用户名)" 以区分不同用户的相同服务商名称
        const displayName = item.userName
          ? `${item.providerName} (${item.userName})`
          : item.providerName || `服务商ID: ${item.id}`;
        return {
          value: item.id,
          label: displayName
        };
      });
    } else {
      providerOptions.value = [];
    }
  } catch (err) {
    console.error('获取模型服务商数据失败:', err);
    message.error('获取模型服务商数据失败');
    providerOptions.value = [];
  }
};

const showModal = () => {
  isEditing.value = false;
  fetchProducts(); // 显示模态框时获取产品数据
  fetchLlmProviders(); // 显示模态框时获取LLM提供者数据
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

const showEditModal = (record) => {
  Object.assign(formState, record);
  isEditing.value = true;
  fetchProducts(); // 确保下拉框数据加载
  fetchLlmProviders();
  visible.value = true;
};

// Expose functions to parent component
defineExpose({
  showModal,
  showEditModal
});

const handleSubmit = () => {
  postProductLlmModel(toRaw(formState))
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