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
            type="link"     
            @click="handleEdit(record)"    
          >    
            <template #icon><EditOutlined /></template>    
            修改角色    
          </a-button>
          <a-button     
            type="link"     
            danger    
            @click="handleDelete(record)"    
          >    
            <template #icon><DeleteOutlined /></template>    
            删除角色    
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
  </div>    
</template>    
  
  
<script setup>    
import { ref, onMounted, onUnmounted, computed } from 'vue';    
import { message } from 'ant-design-vue'    
import { getProductRole, deleteProductRole, putProductRole } from '@/api/productRole';    
import { useRouter } from 'vue-router'    
import { DeleteOutlined, EditOutlined, SaveOutlined } from '@ant-design/icons-vue'    
  
  
const router = useRouter()    
  
// 语音选项配置  
const voiceOptions = ref([  
  {  
    value: 'longxiaochun',  
    label: '龙小醇 - 嗓音如丝般柔滑，温暖中流淌着亲切与抚慰，恰似春风吹过心田。'  
  },  
  {  
    value: 'longxiaoxia',  
    label: '龙小夏 - 以温润磁性的声线，宛如夏日细雨，悄然滋润听者心灵，营造恬静氛围。'  
  },  
  {  
    value: 'longxiaocheng',  
    label: '龙小诚 - 深邃而稳重的声音犹如醇厚佳酿，散发出成熟魅力。'  
  },  
  {  
    value: 'longxiaobai',  
    label: '龙小白 - 以轻松亲和的声调演绎闲适日常，其嗓音如邻家女孩般亲切自然。'  
  },  
  {  
    value: 'longshu',  
    label: '龙叔 - 以专业沉稳的播报风格传递新闻资讯，其嗓音富含权威与信赖感。'  
  },  
  {  
    value: 'longtong',  
    label: '龙童 - 以稚嫩的童声撒欢，像是春日里的小溪，清脆跳跃，流淌着生机勃勃的旋律。'  
  },
  // Edge TTS 系列音色
  {
    value: 'edge-zh-CN-XiaoxiaoNeural',
    label: '晓晓 - 中文 (简体) 普通话 女'
  },
  {
    value: 'edge-zh-CN-XiaoyiNeural',
    label: '晓伊 - 中文 (简体) 普通话 女'
  },
  {
    value: 'edge-zh-CN-YunjianNeural',
    label: '云健 - 中文 (简体) 普通话 男'
  },
  {
    value: 'edge-zh-CN-YunxiNeural',
    label: '云希 - 中文 (简体) 普通话 男'
  },
  {
    value: 'edge-zh-CN-YunxiaNeural',
    label: '云夏 - 中文 (简体) 普通话 男'
  },
  {
    value: 'edge-zh-CN-YunyangNeural',
    label: '云扬 - 中文 (简体) 普通话 男'
  },
  {
    value: 'edge-zh-CN-liaoning-XiaobeiNeural',
    label: '晓北 - 中文 (简体) 辽宁方言 女'
  },
  {
    value: 'edge-zh-CN-shaanxi-XiaoniNeural',
    label: '晓妮 - 中文 (简体) 陕西方言 女'
  },
  {
    value: 'edge-zh-HK-HiuGaaiNeural',
    label: 'HiuGaai - 中文 (繁体) 粤语 女'
  },
  {
    value: 'edge-zh-HK-HiuMaanNeural',
    label: 'HiuMaan - 中文 (繁体) 粤语 女'
  },
  {
    value: 'edge-zh-HK-WanLungNeural',
    label: 'WanLung - 中文 (繁体) 粤语 男'
  },
  {
    value: 'edge-zh-TW-HsiaoChenNeural',
    label: '晓臻 - 中文 (繁体) 台湾 女'
  },
  {
    value: 'edge-zh-TW-HsiaoYuNeural',
    label: '晓雨 - 中文 (繁体) 台湾 女'
  },
  {
    value: 'edge-zh-TW-YunJheNeural',
    label: '云哲 - 中文 (繁体) 台湾 男'
  }
]);

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
  
// 编辑相关状态
const editModalVisible = ref(false)
const editLoading = ref(false)
const editSubmitting = ref(false)
const editFormRef = ref(null)
const currentRecord = ref(null)

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
  },    
  {    
    title: '助手名称',    
    dataIndex: 'assistantName',    
    key: 'assistantName',    
  },    
  {    
    title: '用户名',    
    dataIndex: 'userName',    
    key: 'userName',    
  },    
  {    
    title: '角色',    
    dataIndex: 'role',    
    key: 'role',    
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
    customRender: ({ record }) => {
      return voiceDisplayMap.value[record.voice] || record.voice
    }
  },  
  {    
    title: '操作',    
    key: 'action',    
    slots: { customRender: 'action' },
    width: 200,    
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
  
  
<style src="./Mytable.css" lang="scss" scoped></style>