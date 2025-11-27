<template>      
  <div class="table-container">      
    <a-table       
      :columns="columns"       
      :data-source="dataSource"       
      :pagination="pagination"      
      class="custom-table"      
    >      
      <template #action="{ record }">      
        <a-space direction="vertical" size="small">  
          <a-space size="small">  
            <a-button       
              type="link"       
              size="small"  
              @click="handleEdit(record)"      
            >      
              <template #icon><EditOutlined /></template>      
              修改角色      
            </a-button>  
            <a-button       
              type="link"       
              size="small"  
              danger      
              @click="handleDelete(record)"      
            >      
              <template #icon><DeleteOutlined /></template>      
              删除角色      
            </a-button>      
          </a-space>  
          <a-button       
            type="link"       
            size="small"  
            @click="handleVoiceSetting(record)"      
            style="padding-left: 0"  
          >      
            <template #icon><SoundOutlined /></template>      
            自定义音色设置      
          </a-button>  
        </a-space>  
      </template>      
    </a-table>      
  
    <!-- 修改角色弹窗 -->  
    <a-modal  
      v-model:visible="editModalVisible"  
      title="修改角色信息"  
      :footer="null"  
      width="700px"  
      @cancel="handleEditCancel"  
    >  
      <div v-if="!editLoading">  
        <a-form  
          :model="editForm"  
          :label-col="{ span: 6 }"  
          :wrapper-col="{ span: 18 }"  
          :rules="formRules"  
          ref="editFormRef"  
        >  
          <a-form-item label="产品ID" name="productId">  
            <a-input-number   
              v-model:value="editForm.productId"   
              :min="0"  
              :disabled="true"  
              style="width: 100%"  
            />  
          </a-form-item>  
            
          <a-form-item label="助手名称" name="assistantName">  
            <a-input   
              v-model:value="editForm.assistantName"   
              placeholder="请输入助手名称"  
            />  
          </a-form-item>  
            
          <a-form-item label="用户名" name="userName">  
            <a-input   
              v-model:value="editForm.userName"   
              placeholder="请输入用户名"  
            />  
          </a-form-item>  
            
          <a-form-item label="角色" name="role">  
            <a-input   
              v-model:value="editForm.role"   
              placeholder="请输入角色名称"  
            />  
          </a-form-item>  
            
          <a-form-item label="角色介绍" name="roleIntroduction">  
            <a-textarea   
              v-model:value="editForm.roleIntroduction"   
              :rows="4"  
              :maxlength="6000"  
              show-count  
              placeholder="请输入角色介绍"  
            />  
          </a-form-item>  
            
          <a-form-item label="语音" name="voice">  
            <a-select   
              v-model:value="editForm.voice"   
              placeholder="请选择语音类型"  
              style="width: 100%"  
              :options="voiceOptions"  
              show-search  
              :filter-option="filterVoiceOption"  
            >  
              <template #option="{ value, label }">  
                <div class="voice-option">  
                  <div class="voice-name">{{ value }}</div>  
                  <div class="voice-description">{{ label.split(' - ')[1] }}</div>  
                </div>  
              </template>  
            </a-select>  
          </a-form-item>  
        </a-form>  
  
        <!-- 操作按钮 -->  
        <div class="modal-footer">  
          <a-space>  
            <a-button @click="handleEditCancel">  
              取消  
            </a-button>  
            <a-button   
              type="primary"   
              :loading="editSubmitting"  
              @click="handleEditSubmit"  
            >  
              <template #icon><SaveOutlined /></template>  
              保存修改  
            </a-button>  
          </a-space>  
        </div>  
      </div>  
        
      <!-- 加载状态 -->  
      <div v-if="editLoading" style="text-align: center; padding: 40px;">  
        <a-spin size="large" />  
        <div style="margin-top: 16px; color: #666;">正在加载角色信息...</div>  
      </div>  
    </a-modal>  
  
    <!-- 自定义音色设置弹窗 - 优化版 -->  
    <a-modal  
      v-model:visible="voiceSettingModalVisible"  
      :footer="null"  
      width="680px"  
      :mask-closable="false"  
      :keyboard="true"  
      @cancel="handleVoiceSettingCancel"  
      class="voice-setting-modal"
    >  
      <template #title>  
        <div class="voice-modal-header">  
          <div class="title-section">
            <SoundOutlined class="title-icon" />
            <span class="title-text">自定义音色设置</span>
          </div>
          <a-button   
            type="text"   
            size="small"  
            @click="forceCloseModal"  
            class="force-close-btn"
          >  
            强制关闭  
          </a-button>  
        </div>  
      </template>  
        
      <div v-if="!voiceLoading" class="voice-setting-content">  
        <a-form  
          :model="voiceForm"  
          :label-col="{ span: 6 }"  
          :wrapper-col="{ span: 18 }"  
          ref="voiceFormRef"  
          class="voice-form"
        >  
          <a-form-item label="产品ID">  
            <a-input-number   
              v-model:value="voiceForm.productId"   
              :min="0"  
              :disabled="true"  
              style="width: 100%"  
            />  
          </a-form-item>  
            
          <!-- 音高设置 - 优化版 -->  
          <a-form-item label="音高 (Pitch)">  
            <div class="slider-control-container">  
              <div class="slider-labels">
                <span class="label-left">
                  <SoundOutlined class="label-icon low-pitch" />
                  低沉
                </span>
                <span class="label-center">{{ voiceForm.pitch.toFixed(1) }}</span>
                <span class="label-right">
                  尖锐
                  <SoundOutlined class="label-icon high-pitch" />
                </span>
              </div>
              <div class="slider-wrapper">
                <a-slider
                  v-model:value="voiceForm.pitch"
                  :min="0.5"
                  :max="2"
                  :step="0.1"
                  :marks="pitchMarks"
                  :tooltip-visible="false"
                  class="pitch-slider"
                />
                <div class="slider-track-indicator" :style="{ left: `${((voiceForm.pitch - 0.5) / 1.5) * 100}%` }"></div>
              </div>
              <div class="slider-description">
                <span class="desc-item" :class="{ active: voiceForm.pitch <= 0.8 }">浑厚低沉</span>
                <span class="desc-item" :class="{ active: voiceForm.pitch > 0.8 && voiceForm.pitch <= 1.2 }">标准音调</span>
                <span class="desc-item" :class="{ active: voiceForm.pitch > 1.2 }">高亢尖锐</span>
              </div>
            </div>
          </a-form-item>  
            
          <!-- 语速设置 - 优化版 -->  
          <a-form-item label="语速 (Speed)">  
            <div class="slider-control-container">  
              <div class="slider-labels">
                <span class="label-left">
                  <ClockCircleOutlined class="label-icon slow-speed" />
                  缓慢
                </span>
                <span class="label-center">{{ voiceForm.speed.toFixed(1) }}</span>
                <span class="label-right">
                  快速
                  <RocketOutlined class="label-icon fast-speed" />
                </span>
              </div>
              <div class="slider-wrapper">
                <a-slider
                  v-model:value="voiceForm.speed"
                  :min="0.5"
                  :max="2"
                  :step="0.1"
                  :marks="speedMarks"
                  :tooltip-visible="false"
                  class="speed-slider"
                />
                <div class="slider-track-indicator" :style="{ left: `${((voiceForm.speed - 0.5) / 1.5) * 100}%` }"></div>
              </div>
              <div class="slider-description">
                <span class="desc-item" :class="{ active: voiceForm.speed <= 0.8 }">慢速清晰</span>
                <span class="desc-item" :class="{ active: voiceForm.speed > 0.8 && voiceForm.speed <= 1.2 }">正常语速</span>
                <span class="desc-item" :class="{ active: voiceForm.speed > 1.2 }">快速流畅</span>
              </div>
            </div>
          </a-form-item>

          <!-- 预设快捷选项 -->
          <a-form-item label="快捷预设">
            <div class="preset-buttons">
              <a-button 
                v-for="preset in voicePresets" 
                :key="preset.name"
                size="small"
                @click="applyPreset(preset)"
                :type="isCurrentPreset(preset) ? 'primary' : 'default'"
                class="preset-btn"
              >
                {{ preset.name }}
              </a-button>
            </div>
          </a-form-item>
        </a-form>  
  
        <!-- 操作按钮 -->  
        <div class="modal-footer voice-modal-footer">  
          <a-space size="middle">
            <a-button @click="handleVoiceSettingCancel" size="large">
              取消
            </a-button>
            <a-button   
              v-if="voiceForm.id"  
              danger  
              :loading="voiceSubmitting"  
              @click="handleResetVoice"  
              size="large"
            >  
              <template #icon><ReloadOutlined /></template>  
              恢复默认
            </a-button>
            <a-button   
              type="primary"   
              :loading="voiceSubmitting"  
              @click="handleSaveVoice"  
              size="large"
            >  
              <template #icon><SaveOutlined /></template>  
              保存设置
            </a-button>  
          </a-space>  
        </div>  
      </div>  
        
      <!-- 加载状态 -->  
      <div v-if="voiceLoading" class="loading-container">  
        <a-spin size="large" />  
        <div class="loading-text">正在加载音色设置...</div>  
      </div>  
    </a-modal>  
  </div>      
</template>      
    
    
<script setup>      
import { ref, onMounted, onUnmounted, computed } from 'vue';      
import { message } from 'ant-design-vue'      
import { getProductRole, deleteProductRole, putProductRole } from '@/api/productRole';      
import { getProductVoiceDiy, postProductVoiceDiy, deleteProductVoiceDiy } from '@/api/productVoiceDiy';      
import { useRouter } from 'vue-router'      
import { DeleteOutlined, EditOutlined, SaveOutlined, SoundOutlined, ReloadOutlined, ClockCircleOutlined, RocketOutlined } from '@ant-design/icons-vue'      
import voiceOptionsData from './voiceOptions.js'  // 重命名导入  
    
const router = useRouter()      
    
// 语音选项配置    
const voiceOptions = ref(voiceOptionsData)  
  
// 语音显示名称映射  
const voiceDisplayMap = computed(() => {  
  const map = {}  
  voiceOptions.value.forEach(option => {  
    map[option.value] = option.label.split(' - ')[0]  
  })  
  return map  
})  
  
// 语音搜索过滤  
const filterVoiceOption = (input, option) => {  
  return option.label.toLowerCase().includes(input.toLowerCase()) ||   
         option.value.toLowerCase().includes(input.toLowerCase())  
}  
  
// 滑块标记 - 优化版
const pitchMarks = computed(() => {
  const marks = {}
  for (let i = 0.5; i <= 2; i += 0.5) {
    marks[i] = i.toString()
  }
  return marks
})

const speedMarks = computed(() => {
  const marks = {}
  for (let i = 0.5; i <= 2; i += 0.5) {
    marks[i] = i.toString()
  }
  return marks
})

// 音色预设
const voicePresets = ref([
  { name: '温柔女声', pitch: 1.3, speed: 0.9 },
  { name: '沉稳男声', pitch: 0.8, speed: 1.0 },
  { name: '活泼童声', pitch: 1.5, speed: 1.2 },
  { name: '新闻播报', pitch: 1.0, speed: 1.1 },
  { name: '故事讲述', pitch: 0.9, speed: 0.8 }
])

// 判断是否为当前预设
const isCurrentPreset = (preset) => {
  return Math.abs(voiceForm.value.pitch - preset.pitch) < 0.05 && 
         Math.abs(voiceForm.value.speed - preset.speed) < 0.05
}

// 应用预设
const applyPreset = (preset) => {
  voiceForm.value.pitch = preset.pitch
  voiceForm.value.speed = preset.speed
}
    
// 编辑相关状态  
const editModalVisible = ref(false)  
const editLoading = ref(false)  
const editSubmitting = ref(false)  
const editFormRef = ref(null)  
const currentRecord = ref(null)  
  
// 音色设置相关状态  
const voiceSettingModalVisible = ref(false)  
const voiceLoading = ref(false)  
const voiceSubmitting = ref(false)  
const voiceFormRef = ref(null)  
const voiceForm = ref({  
  id: null,  
  productId: 0,  
  pitch: 1.0,  
  speed: 1.0  
})  
  
// 编辑表单数据  
const editForm = ref({  
  productId: 0,  
  assistantName: '',  
  userName: '',  
  role: '',  
  roleIntroduction: '',  
  voice: ''  
})  
  
// 表单验证规则  
const formRules = {  
  assistantName: [  
    { required: true, message: '请输入助手名称', trigger: 'blur' }  
  ],  
  userName: [  
    { required: true, message: '请输入用户名', trigger: 'blur' }  
  ],  
  role: [  
    { required: true, message: '请输入角色名称', trigger: 'blur' }  
  ],  
  roleIntroduction: [  
    { required: true, message: '请输入角色介绍', trigger: 'blur' }  
  ],  
  voice: [  
    { required: true, message: '请选择语音类型', trigger: 'change' }  
  ]  
}  
    
const pagination = {      
  pageSize: 5,      
};      
    
    
const dataSource = ref([]);      
    
    
const columns = [      
  {      
    title: '产品ID',      
    dataIndex: 'productId',      
    key: 'productId',      
    width: 100,  
  },      
  {      
    title: '助手名称',      
    dataIndex: 'assistantName',      
    key: 'assistantName',      
    width: 120,  
  },      
  {      
    title: '用户名',      
    dataIndex: 'userName',      
    key: 'userName',      
    width: 120,  
  },      
  {      
    title: '角色',      
    dataIndex: 'role',      
    key: 'role',      
    width: 120,  
  },    
  {      
    title: '角色介绍',      
    dataIndex: 'roleIntroduction',      
    key: 'roleIntroduction',  
    width: 200,  
    ellipsis: true,  
  },    
  {      
    title: '语音',      
    dataIndex: 'voice',      
    key: 'voice',  
    width: 120,  
    customRender: ({ record }) => {  
      return voiceDisplayMap.value[record.voice] || record.voice  
    }  
  },    
  {      
    title: '操作',      
    key: 'action',      
    slots: { customRender: 'action' },  
    width: 200,  
    fixed: 'right',  
  },      
];      
    
    
let intervalId;      
    
    
onMounted(() => {      
  fetchProductRole();      
  intervalId = setInterval(fetchProductRole, 1000);      
});      
    
    
onUnmounted(() => {      
  clearInterval(intervalId);      
});      
    
    
const fetchProductRole = () => {      
  getProductRole()      
    .then((res) => {      
      const { data, errorCode } = res.data;      
      if(errorCode == 2001){      
        router.push('/login')      
      }      
      if(errorCode == 200 && data && Array.isArray(data)){      
        dataSource.value = data.map((item, index) => ({      
          key: index,      
          id: item.id,      
          productId: item.productId,      
          assistantName: item.assistantName,      
          userName: item.userName,    
          role: item.role,    
          roleIntroduction: item.roleIntroduction,    
          voice: item.voice    
        }));      
      } else {      
        dataSource.value = [];      
      }      
    })      
    .catch((err) => {      
      console.error('获取产品角色数据失败:', err);    
      message.error('获取数据失败');    
    });      
};      
  
// 处理编辑按钮点击  
const handleEdit = async (record) => {  
  try {  
    editModalVisible.value = true  
    editLoading.value = true  
    currentRecord.value = record  
      
    // 模拟加载过程  
    await new Promise(resolve => setTimeout(resolve, 500))  
      
    // 填充表单数据  
    editForm.value = {  
      productId: record.productId,  
      assistantName: record.assistantName,  
      userName: record.userName,  
      role: record.role,  
      roleIntroduction: record.roleIntroduction,  
      voice: record.voice  
    }  
      
  } catch (error) {  
    console.error('获取角色详情失败:', error)  
    message.error('获取角色详情失败')  
  } finally {  
    editLoading.value = false  
  }  
}  
  
// 处理编辑提交  
const handleEditSubmit = async () => {  
  try {  
    // 表单验证  
    await editFormRef.value.validate()  
      
    editSubmitting.value = true  
      
    const response = await putProductRole(editForm.value)  
    const { data, errorCode } = response.data  
      
    if (errorCode === 200) {  
      message.success('角色信息修改成功')  
      editModalVisible.value = false  
      fetchProductRole() // 刷新数据  
    } else if (errorCode === 2001) {  
      router.push('/login')  
    } else {  
      message.error('修改失败，请重试')  
    }  
      
  } catch (error) {  
    if (error.errorFields) {  
      message.warning('请检查表单填写是否正确')  
    } else {  
      console.error('修改角色失败:', error)  
      message.error('修改失败，请重试')  
    }  
  } finally {  
    editSubmitting.value = false  
  }  
}  
  
// 处理编辑取消  
const handleEditCancel = () => {  
  editModalVisible.value = false  
  currentRecord.value = null  
    
  // 重置表单  
  editForm.value = {  
    productId: 0,  
    assistantName: '',  
    userName: '',  
    role: '',  
    roleIntroduction: '',  
    voice: ''  
  }  
    
  // 清除表单验证  
  if (editFormRef.value) {  
    editFormRef.value.resetFields()  
  }  
}  
  
// 处理音色设置按钮点击  
const handleVoiceSetting = async (record) => {  
  try {  
    voiceSettingModalVisible.value = true  
    voiceLoading.value = true  
      
    // 初始化表单  
    voiceForm.value = {  
      id: null,  
      productId: record.productId,  
      pitch: 1.0,  
      speed: 1.0  
    }  
      
    // 获取当前产品的音色设置  
    const response = await getProductVoiceDiy({ productId: record.productId })  
    const { data, errorCode } = response.data  
      
    if (errorCode === 200 && data && data.length > 0) {  
      const voiceSetting = data[0]  
      voiceForm.value = {  
        id: voiceSetting.id,  
        productId: voiceSetting.productId,  
        pitch: parseFloat(voiceSetting.pitch),  
        speed: parseFloat(voiceSetting.speed)  
      }  
    }  
      
  } catch (error) {  
    console.error('获取音色设置失败:', error)  
    message.error('获取音色设置失败')  
  } finally {  
    voiceLoading.value = false  
  }  
}  
  
// 保存音色设置  
const handleSaveVoice = async () => {  
  try {  
    voiceSubmitting.value = true  
      
    const params = {  
      productId: voiceForm.value.productId,  
      pitch: voiceForm.value.pitch,  
      speed: voiceForm.value.speed  
    }  
      
    const response = await postProductVoiceDiy(params)  
    const { data, errorCode } = response.data  
      
    if (errorCode === 200) {  
      message.success('音色设置保存成功')  
      voiceForm.value.id = data.id  
        
      // 保存成功后自动关闭弹窗  
      setTimeout(() => {  
        handleVoiceSettingCancel()  
      }, 1000)  
    } else if (errorCode === 2001) {  
      router.push('/login')  
    } else {  
      message.error('保存失败，请重试')  
    }  
      
  } catch (error) {  
    console.error('保存音色设置失败:', error)  
    message.error('保存失败，请重试')  
  } finally {  
    voiceSubmitting.value = false  
  }  
}  
  
// 恢复默认音色设置  
const handleResetVoice = async () => {  
  try {  
    voiceSubmitting.value = true  
      
    const response = await deleteProductVoiceDiy({ id: voiceForm.value.id })  
    const { errorCode } = response.data  
      
    if (errorCode === 200) {  
      message.success('已恢复默认音色设置')  
        
      // 重置表单数据  
      voiceForm.value = {  
        id: null,  
        productId: voiceForm.value.productId,  
        pitch: 1.0,  
        speed: 1.0  
      }  
        
      // 恢复成功后自动关闭弹窗  
      setTimeout(() => {  
        handleVoiceSettingCancel()  
      }, 1000)  
    } else if (errorCode === 2001) {  
      router.push('/login')  
    } else {  
      message.error('恢复默认失败，请重试')  
    }  
      
  } catch (error) {  
    console.error('恢复默认音色设置失败:', error)  
    message.error('恢复默认失败，请重试')  
  } finally {  
    voiceSubmitting.value = false  
  }  
}  
  
// 取消音色设置 - 修复版本  
const handleVoiceSettingCancel = () => {  
  try {  
    // 立即关闭弹窗  
    voiceSettingModalVisible.value = false  
      
    // 重置表单数据  
    voiceForm.value = {  
      id: null,  
      productId: 0,  
      pitch: 1.0,  
      speed: 1.0  
    }  
      
    // 重置加载状态  
    voiceLoading.value = false  
    voiceSubmitting.value = false  
      
    // 清除表单验证（如果有）  
    if (voiceFormRef.value) {  
      voiceFormRef.value.resetFields()  
    }  
      
    console.log('音色设置弹窗已关闭')  
  } catch (error) {  
    console.error('关闭弹窗时出错:', error)  
    // 强制关闭弹窗  
    voiceSettingModalVisible.value = false  
  }  
}  
  
// 强制关闭弹窗  
const forceCloseModal = () => {  
  voiceSettingModalVisible.value = false  
  voiceLoading.value = false  
  voiceSubmitting.value = false  
  console.log('弹窗已强制关闭')  
}  
    
    
const handleDelete = (record) => {      
  console.log('Deleting record:', record);      
  deleteProductRole({ id: record.id })    
    .then((res) => {      
      const { data, errorCode } = res.data;      
      if(errorCode == 200){      
        message.success("删除成功");    
        fetchProductRole();      
      } else if(errorCode == 2001){      
        router.push('/login')      
      } else {    
        message.error('删除失败');    
      }    
      console.log(data)      
    })      
    .catch((err) => {      
      console.error('删除失败:', err);    
      message.error('删除失败');    
    });      
};      
</script>      
    
    
<style src="./Mytable.css" lang="scss" scoped>  
/* 音色设置弹窗样式 - 优化版 */
.voice-setting-modal {
  .voice-modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .title-section {
      display: flex;
      align-items: center;
      gap: 8px;
      
      .title-icon {
        font-size: 20px;
        color: #1890ff;
      }
      
      .title-text {
        font-size: 16px;
        font-weight: 600;
      }
    }
    
    .force-close-btn {
      color: #ff4d4f;
      font-size: 12px;
      padding: 2px 8px;
      height: auto;
      
      &:hover {
        background-color: #fff2f0;
      }
    }
  }
}

.voice-setting-content {
  padding: 8px 0;
}

.voice-form {
  .ant-form-item {
    margin-bottom: 24px;
  }
}

/* 滑块控制容器 - 优化版 */
.slider-control-container {
  background: #fafafa;
  border-radius: 8px;
  padding: 20px;
  border: 1px solid #f0f0f0;
  transition: all 0.3s ease;
  
  &:hover {
    border-color: #1890ff;
    box-shadow: 0 2px 8px rgba(24, 144, 255, 0.1);
  }
}

/* 滑块标签 - 优化版 */
.slider-labels {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  font-size: 14px;
  
  .label-left, .label-right {
    display: flex;
    align-items: center;
    gap: 6px;
    color: #666;
    font-weight: 500;
    
    .label-icon {
      font-size: 16px;
      
      &.low-pitch {
        color: #52c41a;
      }
      
      &.high-pitch {
        color: #ff4d4f;
      }
      
      &.slow-speed {
        color: #1890ff;
      }
      
      &.fast-speed {
        color: #fa8c16;
      }
    }
  }
  
  .label-center {
    font-size: 24px;
    font-weight: bold;
    color: #1890ff;
    background: linear-gradient(135deg, #1890ff, #36cfc9);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    padding: 4px 12px;
    background: #fff;
    border-radius: 4px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  }
}

/* 滑块包装器 - 优化版 */
.slider-wrapper {
  position: relative;
  margin: 16px 0;
  
  .slider-track-indicator {
    position: absolute;
    top: 50%;
    left: 0;
    width: 4px;
    height: 16px;
    background: #1890ff;
    border-radius: 2px;
    transform: translate(-50%, -50%);
    transition: left 0.3s ease;
    z-index: 2;
    box-shadow: 0 0 8px rgba(24, 144, 255, 0.5);
  }
}

/* 滑块样式 - 优化版 */
:deep(.ant-slider) {
  .ant-slider-rail {
    background: linear-gradient(to right, #52c41a, #1890ff, #ff4d4f);
    height: 6px;
    border-radius: 3px;
  }
  
  .ant-slider-track {
    background: rgba(255, 255, 255, 0.3);
    height: 6px;
  }
  
  .ant-slider-handle {
    width: 20px;
    height: 20px;
    border: 3px solid #fff;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
    
    &:hover, &:focus {
      border-color: #1890ff;
      box-shadow: 0 4px 12px rgba(24, 144, 255, 0.3);
    }
  }
  
  .ant-slider-mark-text {
    font-size: 12px;
    color: #999;
  }
}

/* 滑块描述 - 优化版 */
.slider-description {
  display: flex;
  justify-content: space-between;
  margin-top: 12px;
  padding: 8px;
  background: #fff;
  border-radius: 4px;
  border: 1px solid #e8e8e8;
  
  .desc-item {
    flex: 1;
    text-align: center;
    padding: 4px 8px;
    border-radius: 4px;
    font-size: 12px;
    color: #999;
    transition: all 0.3s ease;
    cursor: default;
    
    &.active {
      background: #e6f7ff;
      color: #1890ff;
      font-weight: 500;
      transform: scale(1.05);
    }
  }
}

/* 预设按钮 */
.preset-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  
  .preset-btn {
    transition: all 0.3s ease;
    
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
    }
  }
}

/* 弹窗底部按钮 */
.voice-modal-footer {
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
}

/* 加载容器 */
.loading-container {
  text-align: center;
  padding: 60px 40px;
  
  .loading-text {
    margin-top: 16px;
    color: #666;
    font-size: 14px;
  }
}

/* 原有样式保持 */
.voice-option {
  .voice-name {
    font-weight: bold;
    color: #1890ff;
  }
    
  .voice-description {
    font-size: 12px;
    color: #666;
    margin-top: 4px;
  }
}

/* 确保表格不会溢出 */
.table-container {
  overflow-x: auto;
}

.custom-table {
  min-width: 1000px;
}

/* 操作按钮样式优化 */
:deep(.ant-btn-link) {
  padding: 0 4px;
  height: auto;
  line-height: 1.2;
}

:deep(.ant-space-item) {
  line-height: 1.2;
}

.modal-footer {
  margin-top: 24px;
  text-align: right;
}
</style>
