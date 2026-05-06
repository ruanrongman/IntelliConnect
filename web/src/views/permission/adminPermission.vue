```javascript
<template>  
  <div class="admin-config-container">
    <!-- 头部区域 -->
    <div class="header-section">
      <div class="header-content">
        <div class="title-group">
          <h1 class="main-title">
            <span class="title-icon">⚙️</span>
            系统配置管理
          </h1>
          <p class="sub-title">精细化管理您的系统配置，打造卓越用户体验(系统第一次运行，请点击保存按钮，默认值才能生效。)</p>
        </div>
      </div>
      <div class="header-divider"></div>
    </div>

    <!-- 配置表单区域 -->
    <div class="form-wrapper">
      <a-form  
        :model="configList"
        name="adminConfigForm"  
        autocomplete="off"
        class="config-form"
      >  
        <div 
          v-for="(item, index) in configList"  
          :key="item.setKey"
          class="config-card"
          :style="{ animationDelay: `${index * 0.1}s` }"
        >
          <!-- 配置项标题 -->
          <div class="config-header">
            <div class="config-label">
              <span class="label-icon">{{ getIconForKey(item.setKey) }}</span>
              <span class="label-text">{{ item.label }}</span>
            </div>
            <div class="config-badge">{{ getTypeText(item.setKey) }}</div>
          </div>

          <!-- 配置项内容 -->
          <div class="config-body">
            <!-- 动态渲染输入框 -->  
            <div class="input-section">
              <template v-if="item.setKey === 'wx_default_product'">  
                <a-select  
                  v-model:value="item.setValue"  
                  placeholder="请选择默认产品"  
                  allowClear
                  :disabled="loading"
                  size="large"
                  class="custom-select"
                >  
                  <a-select-option  
                    v-for="product in productOptions"  
                    :key="product.value"  
                    :value="String(product.value)"  
                  >  
                    {{ product.label }}  
                  </a-select-option>
                  <a-select-option v-if="productOptions.length === 0" :value="''" disabled>
                    暂无产品
                  </a-select-option>
                </a-select>  
              </template>
              <!-- 新增：布尔类型开关 -->
              <template v-else-if="isBooleanConfig(item.setKey)">
                <div class="switch-container">
                  <a-switch
                      v-model:checked="item.boolValue"
                      :disabled="loading"
                      size="large"
                      class="custom-switch"
                      @change="(checked) => item.setValue = String(checked)"
                  >
                    <template #checkedChildren>
                      <span class="switch-text">✓ 开启</span>
                    </template>
                    <template #unCheckedChildren>
                      <span class="switch-text">✕ 关闭</span>
                    </template>
                  </a-switch>
                  <span class="switch-status">
                   {{ item.boolValue ? '已启用' : '已禁用' }}
                  </span>
                </div>
              </template>
              <template v-else>  
                <a-textarea  
                  v-model:value="item.setValue"  
                  :placeholder="`请输入${item.label}`"  
                  :rows="4"
                  :disabled="loading"
                  class="custom-textarea"
                  :maxlength="255"
                  show-count
                />  
              </template>
            </div>
            
            <!-- 操作按钮组 -->  
            <div class="action-buttons">
              <a-button  
                type="primary"  
                @click="handleSave(item)"
                :loading="item.saving"
                size="large"
                class="save-button"
              >  
                <template #icon>
                  <span class="button-icon">💾</span>
                </template>
                {{ item.buttonText }}  
              </a-button>  
                
              <a-button  
                @click="restoreDefault(item)"
                :loading="item.restoring"
                size="large"
                class="reset-button"
              >  
                <template #icon>
                  <span class="button-icon">🔄</span>
                </template>
                恢复默认  
              </a-button>
            </div>
          </div>

          <!-- 配置说明 -->
          <div class="config-footer">
            <span class="footer-tip">{{ getDescriptionForKey(item.setKey) }}</span>
          </div>
        </div>
      </a-form>  
    </div>

    <!-- 底部提示 -->
    <div class="bottom-notice">
      <span class="notice-icon">💡</span>
      <span>配置修改后将立即生效，请谨慎操作</span>
    </div>
  </div>  
</template>  
  
  
<script setup>  
import { reactive, ref, onMounted } from 'vue';  
import { message } from 'ant-design-vue';  
import { useRouter } from 'vue-router';  
import { getAdminConfig, postAdminConfig, putAdminConfig, deleteAdminConfig } from '@/api/adminConfig';  
import { getProduct } from '@/api/product';  
  
  
const router = useRouter();  
  
  
// --- 响应式状态 ---  
const configList = ref([]);  
const productOptions = ref([]);  
const loading = ref(false);  
  
  
// --- 常量定义 ---  
const allowedKeys = [  
  { value: 'wx_default_product', label: '微信默认产品', buttonText: '保存设置' },  
  { value: 'wx_trigger-keyword', label: '微信触发关键词', buttonText: '保存设置' },  
  { value: 'wx_success-message', label: '注册成功消息', buttonText: '保存设置' },  
  { value: 'wx_unregistered-message', label: '未注册提示消息', buttonText: '保存设置' },
  { value: 'ai_classifier_include_thought', label: '语义路由深度思考', buttonText: '保存设置' },
  { value: 'ai_agent_include_thought', label: 'Agent 深度思考', buttonText: '保存设置' },
  { value: 'ai_mcp_agent_include_thought', label: 'MCP Agent 深度思考', buttonText: '保存设置' },
  { value: 'ai_detect_random', label: '小智唤醒词随机问候', buttonText: '保存设置' },
];
// 布尔类型配置的 key 列表
const booleanConfigKeys = [
  'ai_classifier_include_thought',
  'ai_agent_include_thought',
  'ai_mcp_agent_include_thought',
  'ai_detect_random',
];

// 判断是否为布尔配置
const isBooleanConfig = (key) => {
  return booleanConfigKeys.includes(key);
};
  
// 默认配置值  
const defaultConfigValues = {  
  'wx_default_product': '',  
  'wx_trigger-keyword': '',  
  'wx_success-message': '注册成功，感谢您对我们的支持',  
  'wx_unregistered-message': '您尚未注册，请先完成注册流程',
  'ai_classifier_include_thought': 'true',
  'ai_agent_include_thought': 'false',
  'ai_mcp_agent_include_thought': 'false',
  'ai_detect_random': 'false',
};

// 获取配置项图标
const getIconForKey = (key) => {
  const iconMap = {
    'wx_default_product': '📦',
    'wx_trigger-keyword': '🔑',
    'wx_success-message': '✅',
    'wx_unregistered-message': '⚠️',
    'ai_classifier_include_thought': '🧠',
    'ai_agent_include_thought': '🤖',
    'ai_mcp_agent_include_thought': '🔌',
    'ai_detect_random': '🎲',
  };
  return iconMap[key] || '⚙️';
};

// 获取配置项类型文本
const getTypeText = (key) => {
  if (key === 'wx_default_product') return '产品配置';
  if (key.includes('keyword')) return '关键词配置';
  if (key.includes('message')) return '消息模板';
  if (key.startsWith('ai_')) return 'AI配置';
  return '系统配置';
};

// 获取配置项说明
const getDescriptionForKey = (key) => {
  const descMap = {
    'wx_default_product': '设置用户首次注册时的默认产品选项',
    'wx_trigger-keyword': '设置触发系统响应的关键词',
    'wx_success-message': '用户注册成功后显示的欢迎消息',
    'wx_unregistered-message': '未注册用户访问时的提示信息',
    'ai_classifier_include_thought': '控制语义路由是否启用深度思考',
    'ai_agent_include_thought': '控制 ai.agent.include-thought，关闭可减少 Agent 简单任务延时',
    'ai_mcp_agent_include_thought': '控制 ai.mcp.agent-include-thought，关闭可减少 MCP Agent 简单任务延时',
    'ai_detect_random': '小智唤醒词随机问候',
  };
  return descMap[key] || '系统配置项';
};
  
  
// --- 方法定义 ---  
// 获取所有配置  
const fetchConfigs = () => {  
  loading.value = true;  
  getAdminConfig({})  
    .then((res) => {  
      if (res.data.success) {  
        const data = res.data.data || [];  
        const fetchedConfigs = data.map((item) => {  
          const configDef = allowedKeys.find(k => k.value === item.setKey);
          const isBool = isBooleanConfig(item.setKey);
          return {  
            id: item.id,  
            setKey: item.setKey,  
            setValue: item.setValue,
            boolValue: isBool ? (item.setValue === 'true') : false,
            label: configDef ? configDef.label : item.setKey,  
            buttonText: configDef ? configDef.buttonText : '保存',
            saving: false,
            restoring: false,
          };  
        });  
          
        configList.value = allowedKeys.map(key => {
          const existingConfig = fetchedConfigs.find(item => item.setKey === key.value);
          if (existingConfig) {
            return existingConfig;
          }
          const isBool = isBooleanConfig(key.value);
          const defaultVal = defaultConfigValues[key.value];
          return {  
            id: null,  
            setKey: key.value,  
            setValue: defaultConfigValues[key.value],
            boolValue: isBool ? (defaultVal === 'true') : false,
            label: key.label,  
            buttonText: key.buttonText,
            saving: false,
            restoring: false,
          };
        });
      } else if (res.data.errorCode === 2001) {  
        message.error('登录已过期，请重新登录');  
        router.push('/login');  
      } else {  
        configList.value = allowedKeys.map(key => ({  
          id: null,  
          setKey: key.value,  
          setValue: defaultConfigValues[key.value],  
          boolValue: isBooleanConfig(key.value) ? (defaultConfigValues[key.value] === 'true') : false,
          label: key.label,  
          buttonText: key.buttonText,
          saving: false,
          restoring: false,
        }));  
      }  
    })  
    .catch((err) => {  
      console.error('获取配置API错误:', err);  
      configList.value = allowedKeys.map(key => ({  
        id: null,  
        setKey: key.value,  
        setValue: defaultConfigValues[key.value],  
        boolValue: isBooleanConfig(key.value) ? (defaultConfigValues[key.value] === 'true') : false,
        label: key.label,  
        buttonText: key.buttonText,
        saving: false,
        restoring: false,
      }));  
    })  
    .finally(() => {  
      loading.value = false;  
    });  
};  
  
  
// 获取产品列表用于下拉框  
const fetchProducts = () => {  
  getProduct()  
    .then((res) => {  
      if (res.data.success) {  
        productOptions.value = res.data.data.map((item) => ({  
          value: item.id,  
          label: item.productName,  
        }));  
      } else if (res.data.errorCode === 2001) {  
        message.error('登录已过期，请重新登录');  
        router.push('/login');  
      }  
    })  
    .catch((err) => {  
      console.error('获取产品API错误:', err);  
    });  
};  
  
  
// 保存单个配置项  
const handleSave = (item) => {
  item.saving = true;
  const data = {  
    setKey: item.setKey,  
    setValue: item.setValue,  
  };  
    
  const action = item.id ? putAdminConfig(data) : postAdminConfig(data);  
    
  action  
    .then((res) => {  
      if (res.data.success) {  
        message.success({
          content: `${item.buttonText}成功！`,
          duration: 2,
        });
        if (!item.id && res.data.data && res.data.data.id) {
          item.id = res.data.data.id;
        }
        const currentValue = item.setValue;
        fetchConfigs();
        setTimeout(() => {
          const updatedItem = configList.value.find(i => i.setKey === item.setKey);
          if (updatedItem) {
            updatedItem.setValue = currentValue;
          }
        }, 100);
      } else if (res.data.errorCode === 2001) {  
        message.error('登录已过期，请重新登录');  
        router.push('/login');  
      } else {  
        message.error(`${item.buttonText}失败: ${res.data.errorMsg}`);  
      }  
    })  
    .catch((err) => {  
      console.error('保存配置API错误:', err);  
      message.error(`${item.buttonText}失败`);  
    })
    .finally(() => {
      item.saving = false;
    });  
};  
  
  
// 恢复单个配置项的默认值  
const restoreDefault = (item) => {
  item.restoring = true;
  const defaultValue = defaultConfigValues[item.setKey];  
    
  if (item.id) {  
    deleteAdminConfig({ id: item.id })  
      .then((res) => {  
        if (res.data.success) {  
          message.success({
            content: `已恢复${item.label}的默认值`,
            duration: 2,
          });  
          item.setValue = defaultValue;
          // 新增：同步布尔值
          if (isBooleanConfig(item.setKey)) {
            item.boolValue = defaultValue === 'true';
          }
          item.id = null;
        } else if (res.data.errorCode === 2001) {  
          message.error('登录已过期，请重新登录');  
          router.push('/login');  
        } else {  
          message.error(`恢复默认值失败: ${res.data.errorMsg}`);  
        }  
      })  
      .catch((err) => {  
        console.error('恢复默认值API错误:', err);  
        message.error('恢复默认值失败');  
      })
      .finally(() => {
        item.restoring = false;
      });  
  } else {  
    item.setValue = defaultValue;
    // 新增：同步布尔值
    if (isBooleanConfig(item.setKey)) {
      item.boolValue = defaultValue === 'true';
    }
    message.success({
      content: `已恢复${item.label}的默认值`,
      duration: 2,
    });
    item.restoring = false;
  }  
};  
  
  
// --- 生命周期 ---  
onMounted(() => {  
  fetchConfigs();  
  fetchProducts();  
});  
</script>  
  
  
<style scoped>  
.admin-config-container {  
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
  padding: 0;
}

/* 头部区域 */
.header-section {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 48px 64px 32px;
  position: relative;
  overflow: hidden;
}

.header-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: url('data:image/svg+xml,<svg width="100" height="100" xmlns="http://www.w3.org/2000/svg"><defs><pattern id="grid" width="40" height="40" patternUnits="userSpaceOnUse"><path d="M 40 0 L 0 0 0 40" fill="none" stroke="rgba(255,255,255,0.1)" stroke-width="1"/></pattern></defs><rect width="100" height="100" fill="url(%23grid)"/></svg>');
  opacity: 0.3;
}

.header-content {
  position: relative;
  z-index: 1;
}

.title-group {
  color: white;
}

.main-title {
  font-size: 36px;
  font-weight: 700;
  margin: 0 0 12px 0;
  color: white;
  display: flex;
  align-items: center;
  gap: 12px;
  letter-spacing: -0.5px;
}

.title-icon {
  font-size: 40px;
  filter: drop-shadow(0 2px 4px rgba(0,0,
  0,0.2));
}

.sub-title {
  font-size: 16px;
  margin: 0;
  opacity: 0.95;
  font-weight: 400;
  letter-spacing: 0.3px;
}

.header-divider {
  height: 4px;
  background: linear-gradient(90deg, 
    rgba(255,255,255,0.8) 0%, 
    rgba(255,255,255,0.4) 50%, 
    rgba(255,255,255,0) 100%
  );
  margin-top: 24px;
  border-radius: 2px;
}

/* 表单区域 */
.form-wrapper {
  max-width: 1400px;
  margin: 0 auto;
  padding: 48px 64px;
}

.config-form {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(600px, 1fr));
  gap: 32px;
}

/* 配置卡片 */
.config-card {
  background: white;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  animation: fadeInUp 0.6s ease-out forwards;
  opacity: 0;
  position: relative;
  overflow: hidden;
}

.config-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.3s ease;
}

.config-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(102, 126, 234, 0.15);
}

.config-card:hover::before {
  transform: scaleX(1);
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 配置项头部 */
.config-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 2px solid #f0f2f5;
}

.config-label {
  display: flex;
  align-items: center;
  gap: 12px;
}

.label-icon {
  font-size: 28px;
  filter: drop-shadow(0 2px 4px rgba(0,0,0,0.1));
}

.label-text {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  letter-spacing: -0.3px;
}

.config-badge {
  padding: 6px 16px;
  background: linear-gradient(135deg, #667eea15 0%, #764ba215 100%);
  color: #667eea;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.5px;
  border: 1px solid #667eea30;
}

/* 配置项内容 */
.config-body {
  margin-bottom: 20px;
}

.input-section {
  margin-bottom: 20px;
}

/* 自定义输入框样式 */
.custom-select,
.custom-textarea {
  width: 100%;
  border-radius: 12px;
  border: 2px solid #e8ecf1;
  transition: all 0.3s ease;
  font-size: 15px;
}

.custom-select:hover,
.custom-textarea:hover {
  border-color: #667eea;
}

.custom-select:focus,
.custom-textarea:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1);
}

.custom-textarea {
  padding: 12px 16px;
  line-height: 1.6;
  resize: vertical;
  font-family: inherit;
}

:deep(.ant-select-selector) {
  border-radius: 12px !important;
  border: 2px solid #e8ecf1 !important;
  padding: 8px 16px !important;
  height: auto !important;
  min-height: 48px;
  transition: all 0.3s ease !important;
}

:deep(.ant-select-focused .ant-select-selector) {
  border-color: #667eea !important;
  box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1) !important;
}

:deep(.ant-input-textarea-show-count::after) {
  color: #999;
  font-size: 12px;
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  gap: 12px;
}

.save-button,
.reset-button {
  flex: 1;
  height: 48px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: none;
  position: relative;
  overflow: hidden;
}

.save-button {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
}

.save-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 25px rgba(102, 126, 234, 0.4);
}

.save-button:active {
  transform: translateY(0);
}

.reset-button {
  background: white;
  color: #666;
  border: 2px solid #e8ecf1;
}

.reset-button:hover {
  background: #f8f9fa;
  border-color: #667eea;
  color: #667eea;
  transform: translateY(-2px);
}

.button-icon {
  font-size: 18px;
  display: inline-flex;
  align-items: center;
}

/* 配置项底部 */
.config-footer {
  padding-top: 16px;
  border-top: 1px solid #f0f2f5;
}

.footer-tip {
  font-size: 13px;
  color: #8c8c8c;
  line-height: 1.6;
  display: flex;
  align-items: center;
  gap: 6px;
}

.footer-tip::before {
  content: '💡';
  font-size: 14px;
}

/* 底部提示 */
.bottom-notice {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px 64px 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #8c8c8c;
  font-size: 14px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  margin-bottom: 48px;
  margin-left: 64px;
  margin-right: 64px;
}

.notice-icon {
  font-size: 20px;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .config-form {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .header-section {
    padding: 32px 24px 24px;
  }
  
  .main-title {
    font-size: 28px;
  }
  
  .sub-title {
    font-size: 14px;
  }
  
  .form-wrapper {
    padding: 32px 24px;
  }
  
  .config-card {
    padding: 24px;
  }
  
  .action-buttons {
    flex-direction: column;
  }
  
  .save-button,
  .reset-button {
    width: 100%;
  }
  
  .bottom-notice {
    margin-left: 24px;
    margin-right: 24px;
    padding: 20px 24px 20px;
    flex-direction: column;
    text-align: center;
  }
}

/* Loading 状态优化 */
:deep(.ant-btn-loading-icon) {
  margin-right: 8px;
}

/* 选择框下拉菜单样式 */
:deep(.ant-select-dropdown) {
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  overflow: hidden;
}

:deep(.ant-select-item) {
  padding: 12px 16px;
  transition: all 0.2s ease;
}

:deep(.ant-select-item-option-selected) {
  background: linear-gradient(135deg, #667eea15 0%, #764ba215 100%);
  color: #667eea;
  font-weight: 600;
}

:deep(.ant-select-item-option-active) {
  background: #f8f9fa;
}

/* 消息提示样式优化 */
:deep(.ant-message-notice-content) {
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  padding: 16px 24px;
}
/* 布尔开关容器 */
.switch-container {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border-radius: 12px;
  border: 2px solid #e8ecf1;
  transition: all 0.3s ease;
}

.switch-container:hover {
  border-color: #667eea;
  background: linear-gradient(135deg, #fff 0%, #f8f9fa 100%);
}

/* 自定义开关样式 */
.custom-switch {
  min-width: 80px;
}

:deep(.custom-switch.ant-switch) {
  height: 32px;
  line-height: 32px;
  background: #d9d9d9;
}

:deep(.custom-switch.ant-switch-checked) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

:deep(.custom-switch .ant-switch-handle) {
  width: 28px;
  height: 28px;
  top: 2px;
}

:deep(.custom-switch .ant-switch-inner) {
  padding: 0 28px 0 10px;
  font-size: 13px;
  font-weight: 600;
}

:deep(.custom-switch-checked .ant-switch-inner) {
  padding: 0 10px 0 28px;
}

.switch-text {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

/* 开关状态文本 */
.switch-status {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  padding: 6px 16px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
}

.switch-container:hover .switch-status {
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
}
</style>
