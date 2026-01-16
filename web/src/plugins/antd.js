import {
  Form,
  Input,
  Button,
  Table,
  Dropdown,
  Layout,
  Menu,
  Switch,
  Breadcrumb,
  ConfigProvider,
  Tag,
  Divider,
  Drawer,
  List,
  Card,
  Descriptions,
  Modal,
  Radio,
  Collapse,
  Tree,
  Select,
  Col,
  Row,
  DatePicker,
  Transfer,
  Tabs,
  Checkbox,
  Avatar,
  Statistic,
  Alert,
  Result,
  Upload,
  InputNumber,
  Space ,
  Slider,  // 添加 Slider 组件导入
  // Input
} from 'ant-design-vue'
import * as AntdIcon from '@ant-design/icons-vue'
import 'ant-design-vue/dist/reset.css'
export function setupAntd(app) {
  app.use(Button)
  app.use(Table)
  app.use(Dropdown)
  app.use(Layout)
  app.use(Layout)
  app.use(Menu)
  app.use(Switch)
  app.use(Breadcrumb)
  app.use(Input)
  app.use(ConfigProvider)
  app.use(Tag)
  app.use(Divider)
  app.use(Drawer)
  app.use(Form)
  app.use(List)
  app.use(Card)
  app.use(Descriptions)
  app.use(Modal)
  app.use(Radio)
  app.use(Collapse)
  app.use(Tree)
  app.use(Select)
  app.use(Col)
  app.use(Row)
  app.use(DatePicker)
  app.use(Transfer)
  app.use(Tabs)
  app.use(Checkbox)
  app.use(Avatar)
  app.use(Statistic)
  app.use(Alert)
  app.use(Result)
  app.use(Upload)
  app.use(InputNumber)
  app.use(Space)
  app.use(Slider)  // 添加 Slider 组件注册
  for (let c in AntdIcon) {
    app.component(c, AntdIcon[c])
  }
}
