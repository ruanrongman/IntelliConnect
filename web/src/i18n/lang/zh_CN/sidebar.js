const nestMenu = {
  nestMenu: '多级菜单',
  page1: '页面1',
  page2: '页面2',
  page2_1: '页面2-1',
  page2_2: '页面2-2',
  page3: '页面3',
  page3_1: '页面3-1',
  page3_2: '页面3_2',
  page3_2_1: '页面3-2-1',
}

const component = {
  icon: '图标',
  component: '常用组件',
  table: '表格',
  basicTable: '基础表格',
  form: '表单',
  basicForm: '基础表单',
  modal: '弹窗',
  basicModal: '基础弹窗',
  drawer: '抽屉',
  basicDrawer: '基础抽屉',
  list: '列表',
  basicList: '基础列表',
  tree: '树列表',
  basicTree: '基础树列表',
  upload: '上传',
  basicUpload: '基础上传',
  bigFileUpload: '大文件上传',
}

const plugin = {
  plugin: '第三方插件',
  excel: 'Excel',
  zip: 'Zip',
  print: '打印',
  pdf: 'PDF',
  richEditor: '富文本编辑器',
  markdown: 'MarkDown',
}
const chart = {
  chart: '图表',
  aMap: '高德地图',
  bMap: '百度地图',
  threeDimensional: '三维地图',
  echarts: 'Echarts图表',
  basicChart: '基础折线',
  mapChart: 'Echarts地图',
}

const permission = {
  permission: '系统管理',
  btnPermission: '按钮权限',
  adminPermission: '管理员页面',
  userPermission: '用户页面',
}
const product ={
  productPage: '产品页面',
  productXiaoZhi:'小智绑定',
  productRole:'角色设置',
  agentMemory:'记忆体设置',
  agentLongMemory:'长期记忆设置',
  productRouterSet:'语义路由配置',
  productKnowledge:'知识库配置',
  productMcp: 'mcp服务器接入',
  productAdd : '配置产品',
  productModelAdd: '配置物模型',
  productDataAdd : '配置属性',
  productFunctionAdd: "配置功能",
  productDeviceAdd: '配置设备',
  productEventAdd: '配置事件',
  productEventDataAdd: '配置事件参数',
  productOta: '配置空中升级',
  productOtaPassiveXiaoZhi: '配置小智空中升级',
  productOtaPassive: '配置被动空中升级包',
  alarmEventAdd: '配置事件告警'
}
const deviceData = {
  deviceDataPage: '设备数据',
  deviceData: '设备数据',
}
const realProject = {
  realProject: '实际开发',
  complexTable: '复杂表格',
  axiosCancel: '取消axios请求',
  download: '文件下载',
  websocket: 'WebSocket',
  virtualList: '虚拟列表',
  lazyLoad: '懒加载',
  drag: '拖放',
  keepAliveComponent: '组件缓存',
  aliveRouter1: '缓存1',
  aliveRouter2: '缓存2',
  aliveRouter3: '缓存3',
  smallProblems: '小问题',
  liveVideo: '直播视频',
}
export default {
  dashboard: '首页',
  ...nestMenu,
  ...component,
  ...plugin,
  ...product,
  ...chart,
  ...permission,
  ...realProject,
  ...deviceData,
  setting: '系统设置',
  about: '关于',
}
