<template>
  <a-button type="primary" @click="showModal">
    知识库召回测试
  </a-button>
  <a-modal
    :visible="visible"
    :footer="null"
    @cancel="handleCancel"
    title="知识库召回测试"
    width="800px"
    :closable="!testing"
    :mask-closable="!testing"
  >
    <a-form
      :model="formState"
      name="recall_test"
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 16 }"
      autocomplete="off"
      @finish="onFinish"
      @finishFailed="onFinishFailed"
    >
      <a-form-item
        label="产品ID"
        name="productId"
        :rules="[{ required: true, message: '请选择产品！' }]"
      >
        <a-select
          v-model:value="formState.productId"
          :options="options"
          placeholder="请选择产品"
          allowClear
          :disabled="testing"
        />
      </a-form-item>

      <a-form-item
        label="测试查询"
        name="query"
        :rules="[
          { required: true, message: '请输入测试查询！' },
          { max: 2048, message: '查询内容不能超过2048个字符！' }
        ]"
      >
        <a-textarea
          v-model:value="formState.query"
          placeholder="请输入您想测试的查询内容"
          :rows="4"
          :maxlength="2048"
          show-count
          :disabled="testing"
        />
      </a-form-item>

      <a-form-item :wrapper-col="{ offset: 8, span: 16 }">
        <a-button
          type="primary"
          html-type="submit"
          :loading="testing"
          :disabled="testing"
        >
          {{ testing ? '测试中...' : '开始测试' }}
        </a-button>
      </a-form-item>
    </a-form>

    <!-- 结果展示区域 -->
    <a-divider v-if="results.length > 0 || hasSearched">召回结果</a-divider>
    <div v-if="results.length > 0">
      <a-list item-layout="vertical" size="large" :data-source="results">
        <template #renderItem="{ item }">
          <a-list-item key="item.text">
            <a-list-item-meta>
              <template #title>
                <span>相似度得分: <span :style="getScoreStyle(item.score)">{{ (item.score * 100).toFixed(2) }}%</span></span>
              </template>
              <template #description>
                <a-typography-paragraph
                  :ellipsis="{ rows: 3, expandable: true, symbol: '更多' }"
                  style="white-space: pre-wrap;"
                >
                  {{ item.text }}
                </a-typography-paragraph>
              </template>
            </a-list-item-meta>
          </a-list-item>
        </template>
      </a-list>
    </div>
    <a-empty v-else-if="hasSearched" description="未召回到相关内容" />
  </a-modal>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import { postKnowledgeChatRecall } from '@/api/productKnowledge'; 
import { getProduct } from '@/api/product';

const visible = ref(false);
const testing = ref(false);
const options = ref([]);
const results = ref([]); // 用于存储召回结果
const hasSearched = ref(false); // 标记是否已经进行过一次搜索

const showModal = () => {
  fetchProduct();
  visible.value = true;
  // 重置状态
  testing.value = false;
  results.value = [];
  hasSearched.value = false;
  formState.productId = undefined;
  formState.query = '';
};

const handleCancel = () => {
  if (testing.value) {
    message.warning('正在测试中，请稍候...');
    return;
  }
  visible.value = false; // 通过手动设置来关闭弹窗
};

const fetchProduct = () => {
  getProduct()
    .then((res) => {
      const { data, errorCode } = res.data;
      if (errorCode == 2001) {
        // router.push('/login'); // 假设有router
        console.error('Authentication error, redirecting to login.');
      }
      options.value = data.map((item) => ({
        value: item.id,
        label: item.productName,
      }));
    })
    .catch((err) => {
      console.error('Failed to fetch products:', err);
      message.error('获取产品列表失败');
    });
};

const formState = reactive({
  productId: undefined,
  query: '',
});

const handleSubmit = async () => {
  try {
    testing.value = true;
    hasSearched.value = false;
    results.value = [];

    const res = await postKnowledgeChatRecall(formState);
    const { errorCode, data: recallData, errorMsg } = res.data;

    if (errorCode === 200) {
      message.success('测试完成！');
      results.value = recallData || []; // 确保results是数组
    } else {
      message.error(`测试失败: ${errorMsg || '未知错误'}`);
    }
  } catch (err) {
    console.error('Recall test failed:', err);
    message.error('测试请求失败，请重试！');
  } finally {
    testing.value = false;
    hasSearched.value = true; // 标记已完成一次搜索
  }
};

const onFinish = (values) => {
  console.log('Form Success:', values);
  handleSubmit();
};

const onFinishFailed = (errorInfo) => {
  console.log('Form Failed:', errorInfo);
};

// 根据分数返回对应的样式
const getScoreStyle = (score) => {
  const percentage = score * 100;
  let color = '#8c8c8c'; // 默认灰色
  let fontWeight = 'normal';
  
  if (percentage >= 80) {
    color = '#52c41a'; // 绿色 - 优秀
    fontWeight = 'bold';
  } else if (percentage >= 60) {
    color = '#faad14'; // 橙色 - 良好
    fontWeight = 'bold';
  } else if (percentage >= 40) {
    color = '#fa8c16'; // 深橙色 - 一般
  } else {
    color = '#f5222d'; // 红色 - 较差
  }
  
  return {
    color: color,
    fontWeight: fontWeight,
    fontSize: percentage >= 60 ? '15px' : '14px'
  };
};
</script>

<style scoped>
/* 可以添加一些自定义样式 */
:deep(.ant-list-item-meta-description) {
  margin-top: 8px;
}

/* 美化列表项样式 */
:deep(.ant-list-item) {
  padding: 16px;
  margin-bottom: 12px;
  background: #fafafa;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  transition: all 0.3s ease;
}

:deep(.ant-list-item:hover) {
  background: #f5f5f5;
  border-color: #d9d9d9;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

/* 美化标题样式 */
:deep(.ant-list-item-meta-title) {
  margin-bottom: 8px;
  font-size: 15px;
}

/* 美化折叠文本样式 */
:deep(.ant-typography) {
  margin-bottom: 0;
}

:deep(.ant-typography-expand) {
  color: #1890ff;
  font-weight: 500;
}

/* 美化空状态样式 */
:deep(.ant-empty) {
  padding: 40px 0;
}
</style>
