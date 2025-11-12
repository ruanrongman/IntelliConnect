<template>
  <a-button type="primary" @click="showModal">
    上传文件
  </a-button>
  <a-modal 
    :visible="visible"  
    :footer="null"
    @cancel="handleCancel"
    title="上传文件"
    width="600px"
    :closable="!uploading"
    :mask-closable="!uploading"
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
        label="文件名称"
        name="filename"
        :rules="[{ required: true, message: 'Please input your filename!' }]"
      >
        <a-input v-model:value="formState.filename" :disabled="uploading" />
      </a-form-item>

      <a-form-item
        label="产品id"
        name="productId"
        :rules="[{ required: true, message: 'Please input your productId!' }]"
      >
          <a-select
            v-model:value="formState.productId"
            :options="options"
            placeholder="请选择产品"
            allowClear
          />
      </a-form-item>
      
      <a-form-item
        label="选择文件"
        name="file"
        :rules="[{ required: true, message: '请上传文件！' }]"
      >
        <a-upload
          v-model:file-list="formState.file"
          :max-count="1"
          :multiple="false"
          :before-upload="handleBeforeUpload"
          :show-upload-list="true"
          @remove="handleRemove"
          :accept="allowedTypesString"
          :disabled="uploading"
        >
          <a-button :disabled="uploading">
            <upload-outlined />
            选择文件（200MB以内）
          </a-button>
        </a-upload>
        <div style="margin-top: 8px; color: #8c8c8c; font-size: 12px;">
          支持格式：{{ allowedTypesString }}
        </div>
      </a-form-item>

      <a-form-item :wrapper-col="{ offset: 8, span: 16 }">
        <a-button 
          type="primary" 
          html-type="submit" 
          :loading="uploading"
          :disabled="uploading"
        >
          {{ uploading ? '上传中...' : 'Submit' }}
        </a-button>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { reactive, ref, computed } from 'vue';
import { message } from 'ant-design-vue';
import { UploadOutlined } from '@ant-design/icons-vue';
import { uploadKnowledge } from '@/api/productKnowledge';
import { getProduct} from '@/api/product';

// --- 修改点 1: 定义允许的文件类型 ---
const ALLOWED_FILE_TYPES = [
  ".pdf", ".txt", ".md", ".markdown", ".doc", ".docx",
  ".ppt", ".pptx", ".xls", ".xlsx"
];

// --- 修改点 2: 创建一个用于UI显示的字符串 ---
const allowedTypesString = computed(() => ALLOWED_FILE_TYPES.join(', '));

const visible = ref(false);
const uploading = ref(false);
const options = ref([]);

const showModal = () => {
  fetchProduct()
  visible.value = !visible.value;
  uploading.value = false;
};

const handleCancel = () => {
  if (uploading.value) {
    message.warning('文件正在上传中，请等待完成');
    return;
  }
  visible.value = false;
  uploading.value = false;
};

// 上传文件列表及选中文件
const selectedFile = ref(null);

// --- 修改点 3: 更新文件类型验证逻辑 ---
const handleBeforeUpload = (file) => {
  if (formState.file.length >= 1) {
    message.warning('只能上传一个文件');
    return false;
  }
  
  // 获取文件后缀名并转为小写
  const fileExtension = '.' + file.name.split('.').pop().toLowerCase();

  // 验证文件类型是否在允许的列表中
  if (!ALLOWED_FILE_TYPES.includes(fileExtension)) {
    message.error(`文件类型不支持，请上传 ${allowedTypesString.value} 格式的文件`);
    return false;
  }
  
  // 验证文件大小（200MB）
  const isLt200M = file.size / 1024 / 1024 < 200;
  if (!isLt200M) {
    message.error('文件大小不能超过200MB');
    return false;
  }
  
  selectedFile.value = file;
  formState.file = [file];
  return false;
};

const handleRemove = () => {
  selectedFile.value = null;
  formState.file = [];
  return true;
};

const fetchProduct = () => {
  getProduct()
    .then((res) => {
      const { data, errorCode } = res.data;
      if(errorCode==2001){
        // 假设您有router实例，这里需要引入
        // import { useRouter } from 'vue-router';
        // const router = useRouter();
        // router.push('/login')
        console.error('Authentication error, redirecting to login.'); // 临时提示
      }
      options.value = data.map((item) => ({
        value: item.id,
        label: item.productName,
      }));
    })
    .catch((err) => {
      console.log(err);
    });
};

const formState = reactive({
  filename: "",
  productId: "",
  file: []
});

const handleSubmit = async () => {
  const file = formState.file?.[0];
  if (!file) {
    message.error('请先上传文件');
    return;
  }

  try {
    uploading.value = true;
    
    const params = {
      filename: formState.filename,
      productId: formState.productId
    };

    // 注意：这里的 originFileObj 是 a-upload 组件在手动上传模式下提供的原始文件对象
    // 如果您的 formState.file[0] 本身就是原始 File 对象，则直接使用即可
    const fileToUpload = file.originFileObj || file;

    const res = await uploadKnowledge(fileToUpload, params);
    
    console.log(res.data);
    const { errorCode } = res.data;
    
    if (errorCode === 200) {
      message.success('文件上传成功！');
      
      // 关闭弹窗并重置表单
      setTimeout(() => {
        visible.value = false;
        uploading.value = false;
        
        // 重置表单
        formState.filename = '';
        formState.productId = undefined; // 建议重置为 undefined 而不是 0
        formState.file = [];
        selectedFile.value = null;
      }, 1000);
    } else {
      message.error(`上传失败：${res.data.errorMessage || '未知错误'}`);
    }
  } catch (err) {
    console.log(err);
    message.error('上传失败，请重试！');
  } finally {
    uploading.value = false;
  }
};

const onFinish = (values) => {
  console.log('Success:', values);
  handleSubmit();
};

const onFinishFailed = (errorInfo) => {
  console.log('Failed:', errorInfo);
};
</script>

<style scoped>
:deep(.ant-upload-list) {
  margin-top: 8px;
}
</style>
