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
            @click="handleGetMcpUrl(record)"    
          >    
            <template #icon><LinkOutlined /></template>    
            获取MCP端点    
          </a-button>  
          <a-button     
            type="link"     
            @click="handleToolManage(record)"    
          >    
            <template #icon><SettingOutlined /></template>    
            工具管理    
          </a-button>  
          <a-button     
            type="link"     
            danger    
            @click="handleDelete(record)"    
          >    
            <template #icon><DeleteOutlined /></template>    
            删除信息    
          </a-button>   
        </a-space>   
      </template>    
    </a-table>    
      
    <!-- MCP URL 显示弹窗 -->  
    <a-modal  
      v-model:visible="mcpUrlModalVisible"  
      title="MCP端点信息"  
      :footer="null"  
      width="800px"  
    >  
      <div v-if="mcpUrlData && !mcpUrlLoading">  
        <!-- MCP端点URL部分 -->  
        <a-card 
          title="MCP端点URL" 
          size="small" 
          style="margin-bottom: 16px;"
        >  
          <div style="display: flex; align-items: center; gap: 8px;">  
            <a-input   
              :value="mcpUrlData.url"   
              readonly   
              style="font-family: monospace; flex: 1;"  
            />  
            <a-button   
              type="primary"   
              @click="copyToClipboard(mcpUrlData.url)"  
            >  
              <template #icon><CopyOutlined /></template>  
              复制  
            </a-button>  
          </div>  
        </a-card>  
          
        <!-- MCP工具列表部分 -->  
        <a-card   
          v-if="mcpToolsData && mcpToolsData.length > 0"   
          title="可用MCP工具"   
          size="small"  
        >  
          <template #extra>
            <a-button 
              size="small" 
              type="text" 
              :loading="mcpToolsRefreshing"
              @click="refreshMcpTools"
            >
              <template #icon><ReloadOutlined /></template>
              刷新工具
            </a-button>
          </template>
          <div class="tools-container">  
            <a-collapse v-model:activeKey="activeToolKeys" ghost>  
              <a-collapse-panel  
                v-for="(tool, index) in mcpToolsData"  
                :key="index"  
                :header="tool.name"  
              >  
                <template #extra>  
                  <a-tag color="blue">工具</a-tag>  
                </template>  
                  
                <div class="tool-detail">  
                  <!-- 工具描述 -->  
                  <div class="tool-section">  
                    <h4>描述</h4>  
                    <p class="tool-description">{{ tool.description }}</p>  
                  </div>  
                    
                  <!-- 输入参数 -->  
                  <div class="tool-section" v-if="tool.inputSchema">  
                    <h4>输入参数</h4>  
                    <div class="schema-container">  
                      <!-- 参数列表 -->  
                      <div v-if="tool.inputSchema.properties" class="properties-list">  
                        <a-descriptions   
                          :column="1"   
                          bordered   
                          size="small"  
                          title="参数列表"  
                        >  
                          <a-descriptions-item   
                            v-for="(prop, propName) in tool.inputSchema.properties"   
                            :key="propName"  
                            :label="propName"  
                          >  
                            <div class="property-info">  
                              <a-tag :color="getTypeColor(prop.type)">{{ prop.type }}</a-tag>  
                              <span v-if="tool.inputSchema.required && tool.inputSchema.required.includes(propName)">  
                                <a-tag color="red" size="small">必填</a-tag>  
                              </span>  
                              <div v-if="prop.title" class="property-title">{{ prop.title }}</div>  
                              <div v-if="prop.description" class="property-description">{{ prop.description }}</div>  
                            </div>  
                          </a-descriptions-item>  
                        </a-descriptions>  
                      </div>  
                        
                      <!-- JSON Schema原始数据 -->  
                      <div class="schema-raw" style="margin-top: 16px;">  
                        <a-collapse size="small">  
                          <a-collapse-panel header="查看完整Schema" key="schema">  
                            <pre class="json-schema">{{ JSON.stringify(tool.inputSchema, null, 2) }}</pre>  
                          </a-collapse-panel>  
                        </a-collapse>  
                      </div>  
                    </div>  
                  </div>  
                    
                  <!-- 使用示例 -->  
                  <div class="tool-section">  
                    <h4>调用示例</h4>  
                    <div class="example-container">  
                      <pre class="tool-example">{{generateToolExample(tool)}}</pre>  
                      <a-button   
                        size="small"   
                        @click="copyToClipboard(generateToolExample(tool))"  
                      >  
                        <template #icon><CopyOutlined /></template>  
                        复制示例  
                      </a-button>  
                    </div>  
                  </div>  
                </div>  
              </a-collapse-panel>  
            </a-collapse>  
          </div>  
        </a-card>  
          
        <!-- 无工具提示 -->  
        <a-card v-else title="MCP工具" size="small">  
          <template #extra>
            <a-button 
              size="small" 
              type="text" 
              :loading="mcpToolsRefreshing"
              @click="refreshMcpTools"
            >
              <template #icon><ReloadOutlined /></template>
              刷新工具
            </a-button>
          </template>
          <a-empty description="该产品暂无可用的MCP工具" />  
        </a-card>  
      </div>  
        
      <!-- 加载状态 -->  
      <div v-if="mcpUrlLoading" style="text-align: center; padding: 40px;">  
        <a-spin size="large" />  
        <div style="margin-top: 16px; color: #666;">正在获取MCP端点信息...</div>  
      </div>  
    </a-modal>  

    <!-- 工具管理弹窗 -->
    <a-modal
      v-model:visible="toolManageModalVisible"
      title="工具管理"
      :width="700"
      :footer="null"
    >
      <a-spin :spinning="toolManageLoading">
        <div class="tool-manage-header">
          <a-alert
            message="选择要禁用的工具"
            description="默认不禁用任何工具，勾选后该工具将被禁用。已禁用的工具显示为灰色。"
            type="info"
            show-icon
            style="margin-bottom: 16px;"
          />
          <div class="tool-manage-actions">
            <a-space>
              <a-button size="small" @click="selectAllTools">全选</a-button>
              <a-button size="small" @click="clearAllTools">清空</a-button>
              <a-button size="small" type="primary" :loading="toolSubmitLoading" @click="submitToolBans">
                保存设置
              </a-button>
              <a-button 
                size="small" 
                danger 
                :loading="toolClearLoading" 
                @click="clearAllBannedTools"
              >
                清除所有禁用
              </a-button>
            </a-space>
          </div>
          
          <!-- 统计信息 -->
          <div class="tool-stats">
            <a-space>
              <a-tag color="green">可用: {{ availableToolsCount }}</a-tag>
              <a-tag color="red">已禁用: {{ selectedBannedTools.length }}</a-tag>
              <a-tag color="blue">总计: {{ Object.keys(availableTools).length }}</a-tag>
            </a-space>
          </div>
        </div>

        <div class="tools-list">
          <div class="tool-item" v-for="(tool, key) in availableTools" :key="key">
            <div 
              class="tool-content" 
              :class="{ 'tool-banned': selectedBannedTools.includes(key) }"
            >
              <div class="tool-checkbox">
                <a-checkbox 
                  :checked="selectedBannedTools.includes(key)"
                  @change="(e) => handleToolChange(key, e.target.checked)"
                />
              </div>
              <div class="tool-info">
                <div class="tool-header">
                  <div class="tool-name">
                    <a-tag :color="selectedBannedTools.includes(key) ? 'default' : 'blue'">
                      {{ key }}
                    </a-tag>
                    <span class="tool-title" :class="{ 'banned-title': selectedBannedTools.includes(key) }">
                      {{ tool.name }}
                    </span>
                    <a-tag v-if="selectedBannedTools.includes(key)" color="red" size="small">
                      已禁用
                    </a-tag>
                  </div>
                </div>
                <div class="tool-desc" :class="{ 'banned-desc': selectedBannedTools.includes(key) }">
                  {{ tool.description }}
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="tool-manage-footer">
          <a-space>
            <span>已选择 {{ selectedBannedTools.length }} 个工具禁用</span>
            <a-button type="primary" :loading="toolSubmitLoading" @click="submitToolBans">
              保存设置
            </a-button>
            <a-button @click="toolManageModalVisible = false">取消</a-button>
          </a-space>
        </div>
      </a-spin>
    </a-modal>
  </div>    
</template>    
    
    
<script setup>    
import { ref, onMounted, onUnmounted, computed } from 'vue';    
import { message } from 'ant-design-vue'    
import { getProduct, deleteProduct } from '@/api/product';    
import { getMcpPointUrl, getMcpPointTools } from '@/api/mcpPoint'; // 导入新的API函数  
import { getProductToolsBan, postProductToolsBan, deleteProductToolsBan } from '@/api/productToolsBan'; // 导入工具管理API
import { useRouter } from 'vue-router'    
import { DeleteOutlined, LinkOutlined, CopyOutlined, ReloadOutlined, SettingOutlined } from '@ant-design/icons-vue'    
    
    
const router = useRouter()    
    
// MCP URL 相关状态  
const mcpUrlModalVisible = ref(false)  
const mcpUrlData = ref(null)  
const mcpToolsData = ref([])  
const mcpUrlLoading = ref(false)  
const mcpUrlRefreshing = ref(false) // 新增：刷新端点URL的loading状态
const mcpToolsRefreshing = ref(false) // 新增：刷新工具的loading状态
const activeToolKeys = ref([]) // 控制工具面板展开状态  
const currentProductId = ref(null) // 新增：保存当前产品ID用于刷新

// 工具管理相关状态
const toolManageModalVisible = ref(false)
const toolManageLoading = ref(false)
const toolSubmitLoading = ref(false)
const toolClearLoading = ref(false)
const selectedBannedTools = ref([])
const currentManageProductId = ref(null)

// 可用工具列表
const availableTools = {
  '1': { name: 'Query weather', description: '查询天气信息' },
  '2': { name: 'Control electrical', description: '控制和查询电器（不包括播放音乐和小智设备）' },
  '3': { name: 'Music control', description: '点歌或播放音乐（包括推荐音乐）' },
  '4': { name: 'Complex tasks', description: '需要深度规划和思考的复杂任务（关闭后内部工具将无法与外部工具协同）' },
  '6': { name: 'Product binding', description: '绑定或解绑产品(微信环境才会自动启用，设备聊天无需手动禁用)' },
  '7': { name: 'Switch products', description: '切换控制的产品(微信环境才会自动启用，设备聊天无需手动禁用)' },
  '8': { name: 'Schedule management', description: '日程管理和提醒任务(微信环境才会自动启用，设备聊天无需手动禁用)' },
  '9': { name: 'Role and voice', description: '所有关于角色和语音的功能' },
  '10': { name: 'MCP', description: '外部MCP总开关' },
  'knowledge': {name: '知识库', description: '知识库开关'}
}

// 计算可用工具数量
const availableToolsCount = computed(() => {
  return Object.keys(availableTools).length - selectedBannedTools.value.length
})
    
const pagination = {    
  pageSize: 5,    
};    
    
    
const dataSource = ref([]);    
    
    
const columns = [    
  {    
    title: '产品id',    
    dataIndex: 'id',    
    key: 'id',    
  },    
  {    
    title: '产品名称',    
    dataIndex: 'name',    
    key: 'name',    
  },    
  {    
    title: '密钥',    
    dataIndex: 'key',    
    key: 'key',    
  },    
  {    
    title: '注册状态',    
    dataIndex: 'register',    
    key: 'register',    
  },    
  {    
    title: 'Action',    
    key: 'action',    
    slots: { customRender: 'action' },    
    width: 280, // 增加操作列宽度以容纳三个按钮  
  },    
];    
    
    
let intervalId;    
    
    
onMounted(() => {    
  fetchProduct();    
  intervalId = setInterval(fetchProduct, 1000); // 每 60 秒钟刷新一次数据
});    
    
    
onUnmounted(() => {    
  clearInterval(intervalId);    
});    
    
    
const fetchProduct = () => {    
  getProduct()    
    .then((res) => {    
      const { data, errorCode } = res.data;    
      if(errorCode===2001){
        router.push('/login')    
      }    
      if(errorCode===200&& data && Array.isArray(data)){
        dataSource.value = data.map((item, index) => ({
          id: item.id,    
          name: item.productName,    
          key: item.keyvalue,    
          register: item.register,    
          mqttUser: item.mqttUser    
        }));    
      } else {    
        // 当没有数据时，设置为空数组    
        dataSource.value = [];    
      }    
    })    
    .catch((err) => {    
      console.log(err);    
    });    
};    
  
  
// 获取MCP端点URL  
const handleGetMcpUrl = async (record) => {  
  try {  
    mcpUrlModalVisible.value = true  
    mcpUrlLoading.value = true  
    mcpUrlData.value = null  
    mcpToolsData.value = []  
    activeToolKeys.value = []  
    currentProductId.value = record.id // 保存当前产品ID
  
  
    // 获取MCP端点URL  
    const urlResponse = await getMcpPointUrl({ productId: record.id })  
    const { data: urlData, errorCode: urlErrorCode } = urlResponse.data  
  
  
    if (urlErrorCode === 200) {  
      mcpUrlData.value = {  
        url: urlData.url || urlData // 根据实际返回数据结构调整  
      }  
        
      // 同时获取工具列表  
      try {  
        const toolsResponse = await getMcpPointTools({ productId: record.id })  
        const { data: toolsData, errorCode: toolsErrorCode } = toolsResponse.data  
          
        if (toolsErrorCode === 200 && toolsData && Array.isArray(toolsData)) {  
          mcpToolsData.value = toolsData  
          // 默认展开第一个工具  
          if (toolsData.length > 0) {  
            activeToolKeys.value = ['0']  
          }  
        }  
      } catch (toolsError) {  
        console.log('获取工具列表失败:', toolsError)  
        message.warning('获取MCP工具列表失败，但端点URL获取成功')  
      }  
        
    } else if (urlErrorCode === 2001) {  
      router.push('/login')  
      return  
    } else {  
      message.error('获取MCP端点失败')  
    }  
  } catch (error) {  
    console.log('获取MCP端点错误:', error)  
    message.error('获取MCP端点失败')  
  } finally {  
    mcpUrlLoading.value = false  
  }  
}  


// 新增：刷新MCP工具列表
const refreshMcpTools = async () => {
  if (!currentProductId.value) {
    message.error('无法获取产品信息，请重新打开弹窗')
    return
  }

  try {
    mcpToolsRefreshing.value = true
    
    const toolsResponse = await getMcpPointTools({ productId: currentProductId.value })
    const { data: toolsData, errorCode: toolsErrorCode } = toolsResponse.data
    
    if (toolsErrorCode === 200 && toolsData && Array.isArray(toolsData)) {
      mcpToolsData.value = toolsData
      // 保持之前展开的工具面板状态，或默认展开第一个
      if (toolsData.length > 0 && activeToolKeys.value.length === 0) {
        activeToolKeys.value = ['0']
      }
      message.success(`已刷新，共获取到 ${toolsData.length} 个MCP工具`)
    } else if (toolsErrorCode === 2001) {
      router.push('/login')
      return
    } else {
      mcpToolsData.value = []
      message.warning('刷新成功，但未获取到MCP工具')
    }
  } catch (error) {
    console.log('刷新MCP工具列表错误:', error)
    message.error('刷新MCP工具列表失败')
  } finally {
    mcpToolsRefreshing.value = false
  }
}

// 工具管理相关函数
const handleToolManage = async (record) => {
  try {
    toolManageModalVisible.value = true
    toolManageLoading.value = true
    selectedBannedTools.value = []
    currentManageProductId.value = record.id

    // 获取已禁用的工具列表
    const response = await getProductToolsBan({ productId: record.id })
    const { data, errorCode } = response.data

    if (errorCode === 200 && data && Array.isArray(data)) {
      selectedBannedTools.value = data
    } else if (errorCode === 2001) {
      router.push('/login')
      return
    } else {
      // 如果不是200，认为没有禁用的工具
      selectedBannedTools.value = []
    }
  } catch (error) {
    console.log('获取禁用工具列表失败:', error)
    selectedBannedTools.value = []
  } finally {
    toolManageLoading.value = false
  }
}

// 处理工具选择变化
const handleToolChange = (toolKey, checked) => {
  if (checked) {
    if (!selectedBannedTools.value.includes(toolKey)) {
      selectedBannedTools.value.push(toolKey)
    }
  } else {
    const index = selectedBannedTools.value.indexOf(toolKey)
    if (index > -1) {
      selectedBannedTools.value.splice(index, 1)
    }
  }
}

// 全选工具
const selectAllTools = () => {
  selectedBannedTools.value = Object.keys(availableTools)
}

// 清空选择
const clearAllTools = () => {
  selectedBannedTools.value = []
}

// 提交禁用工具设置
const submitToolBans = async () => {
  if (!currentManageProductId.value) {
    message.error('无法获取产品信息，请重新打开弹窗')
    return
  }

  try {
    toolSubmitLoading.value = true
    
    const response = await postProductToolsBan({
      productId: currentManageProductId.value,
      toolsName: selectedBannedTools.value
    })
    
    const { errorCode } = response.data
    
    if (errorCode === 200) {
      message.success(`已成功禁用 ${selectedBannedTools.value.length} 个工具`)
    } else if (errorCode === 2001) {
      router.push('/login')
      return
    } else {
      message.error('你没选择禁用任何工具,保存设置失败!')
    }
  } catch (error) {
    console.log('提交禁用工具失败:', error)
    message.error('你没选择禁用任何工具,保存设置失败!')
  } finally {
    toolSubmitLoading.value = false
  }
}

// 清除所有禁用的工具
const clearAllBannedTools = async () => {
  if (!currentManageProductId.value) {
    message.error('无法获取产品信息，请重新打开弹窗')
    return
  }

  try {
    toolClearLoading.value = true
    
    const response = await deleteProductToolsBan({ 
      productId: currentManageProductId.value 
    })
    
    const { errorCode } = response.data
    
    if (errorCode === 200) {
      selectedBannedTools.value = []
      message.success('已清除所有禁用的工具')
    } else if (errorCode === 2001) {
      router.push('/login')
      return
    } else {
      message.error('清除失败')
    }
  } catch (error) {
    console.log('清除禁用工具失败:', error)
    message.error('清除失败')
  } finally {
    toolClearLoading.value = false
  }
}
  
  
// 获取类型颜色  
const getTypeColor = (type) => {  
  const colors = {  
    'string': 'green',  
    'number': 'blue',  
    'object': 'purple',  
    'array': 'orange',  
    'boolean': 'red',  
    'integer': 'cyan'  
  }  
  return colors[type] || 'default'  
}  
  
  
// 生成工具调用示例  
const generateToolExample = (tool) => {  
  if (!tool.inputSchema || !tool.inputSchema.properties) {  
    return `// 调用 ${tool.name} 工具  
{  
  "tool": "${tool.name}",  
  "parameters": {}  
}`  
  }  
    
  const exampleParams = {}  
  Object.keys(tool.inputSchema.properties).forEach(key => {  
    const prop = tool.inputSchema.properties[key]  
    switch (prop.type) {  
      case 'string':  
        exampleParams[key] = `"示例${prop.title || key}"`  
        break  
      case 'number':  
      case 'integer':  
        exampleParams[key] = 42  
        break  
      case 'boolean':  
        exampleParams[key] = true  
        break  
      case 'array':  
        exampleParams[key] = []  
        break  
      case 'object':  
        exampleParams[key] = {}  
        break  
      default:  
        exampleParams[key] = `"${key}值"`  
    }  
  })  
    
  return `// 调用 ${tool.name} 工具  
{  
  "tool": "${tool.name}",  
  "parameters": ${JSON.stringify(exampleParams, null, 2)}  
}`  
}  
  
  
// 复制到剪贴板  
const copyToClipboard = async (text) => {  
  try {  
    await navigator.clipboard.writeText(text)  
    message.success('内容已复制到剪贴板')  
  } catch (error) {  
    // 降级处理  
    const textArea = document.createElement('textarea')  
    textArea.value = text  
    document.body.appendChild(textArea)  
    textArea.select()  
    try {  
      document.execCommand('copy')  
      message.success('内容已复制到剪贴板')  
    } catch (err) {  
      message.error('复制失败，请手动复制')  
    }  
    document.body.removeChild(textArea)  
  }  
}  
    
const handleDelete = (record) => {    
  // 实现删除功能    
  console.log('Deleting record:', record);    
  deleteProduct({id:record.id}).then((res) => {    
      const { data, errorCode } = res.data;    
      if(errorCode==200){    
        message.success("删除成功")    
      }else if(errorCode==3002){    
        message.warn("删除失败，产品被绑定或者下面有物模型")    
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
      
    :deep(.ant-space) {  
      .ant-space-item {  
        display: flex;  
        align-items: center;  
      }  
    }  
  }    
}  
  
  
// MCP工具显示样式  
.tools-container {  
  .tool-detail {  
    .tool-section {  
      margin-bottom: 20px;  
        
      h4 {  
        color: #1890ff;  
        margin-bottom: 8px;  
        font-size: 14px;  
        font-weight: 600;  
      }  
        
      .tool-description {  
        background: #f6f8fa;  
        padding: 12px;  
        border-radius: 6px;  
        margin: 0;  
        color: #586069;  
        line-height: 1.5;  
        border-left: 3px solid #1890ff;  
      }  
    }  
  }  
    
  .property-info {  
    .property-title {  
      font-weight: 500;  
      color: #24292e;  
      margin-top: 4px;  
    }  
      
    .property-description {  
      color: #586069;  
      font-size: 12px;  
      margin-top: 2px;  
    }  
  }  
    
  .json-schema {  
    background: #f6f8fa;  
    padding: 12px;  
    border-radius: 6px;  
    font-size: 12px;  
    color: #24292e;  
    margin: 0;  
    max-height: 300px;  
    overflow-y: auto;  
    border: 1px solid #e1e4e8;  
  }  
    
  .example-container {  
    background: #f6f8fa;  
    padding: 12px;  
    border-radius: 6px;  
    border: 1px solid #e1e4e8;  
      
    .tool-example {  
      margin: 0 0 8px 0;  
      font-size: 12px;  
      color: #24292e;  
      background: transparent;  
    }  
  }  
}  
  
  
:deep(.ant-descriptions-item-label) {  
  background: #fafafa !important;  
  font-weight: 500;  
}  
  
  
:deep(.ant-collapse) {  
  .ant-collapse-header {  
    font-weight: 500;  
    color: #1890ff;  
  }  
}

// 新增：刷新按钮样式
:deep(.ant-card-extra) {
  .ant-btn-text {
    color: #1890ff;
    
    &:hover {
      background: #f0f9ff;
    }
    
    .anticon {
      margin-right: 4px;
    }
  }
}

// 工具管理弹窗样式
.tool-manage-header {
  margin-bottom: 20px;
  
  .tool-manage-actions {
    display: flex;
    justify-content: flex-end;
    margin-bottom: 16px;
  }
  
  .tool-stats {
    display: flex;
    justify-content: center;
    padding: 8px;
    background: #f5f5f5;
    border-radius: 6px;
  }
}

.tools-list {
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  padding: 16px;
  background: #fafafa;

  .tool-item {
    margin-bottom: 12px;
    
    &:last-child {
      margin-bottom: 0;
    }

    .tool-content {
      display: flex;
      align-items: flex-start;
      padding: 12px;
      background: white;
      border-radius: 6px;
      border: 1px solid #e8e8e8;
      transition: all 0.3s;

      &:hover {
        border-color: #1890ff;
        box-shadow: 0 2px 8px rgba(24, 144, 255, 0.1);
      }

      &.tool-banned {
        background: #f5f5f5;
        border-color: #d9d9d9;
        opacity: 0.7;

        &:hover {
          border-color: #ff4d4f;
          box-shadow: 0 2px 8px rgba(255, 77, 79, 0.1);
        }
      }

      .tool-checkbox {
        margin-right: 12px;
        padding-top: 2px;
      }

      .tool-info {
        flex: 1;

        .tool-header {
          margin-bottom: 6px;

          .tool-name {
            display: flex;
            align-items: center;
            gap: 8px;

            .tool-title {
              font-weight: 500;
              color: #262626;
              transition: color 0.3s;

              &.banned-title {
                color: #8c8c8c;
                text-decoration: line-through;
              }
            }
          }
        }

        .tool-desc {
          color: #8c8c8c;
          font-size: 12px;
          line-height: 1.4;
          transition: color 0.3s;

          &.banned-desc {
            color: #bfbfbf;
          }
        }
      }
    }
  }
}

.tool-manage-footer {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #e8e8e8;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

:deep(.ant-checkbox-wrapper) {
  align-items: flex-start;
}
</style>
