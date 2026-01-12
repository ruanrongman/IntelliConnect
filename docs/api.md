# API 文档

## 概述

IntelliConnect 平台提供丰富的 REST API 接口，支持设备管理、物模型配置、Agent 智能体交互、OTA升级、微信服务等功能。所有 API 接口均通过 JWT Token 进行身份验证。

## 基础信息

- **基础 URL**: `http://localhost:8080/api/v2`
- **认证方式**: Bearer Token (JWT)
- **内容类型**: `application/json`
- **字符编码**: UTF-8

## 用户认证

### 用户登录

```
POST /login
```

**请求参数:**
```json
{
  "username": "string",
  "password": "string"
}
```

**请求头:**
```
Content-Type: application/json
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": "Bearer {jwt_token}"
}
```

### 创建新用户

```
POST /newUser
```

**请求参数:**
```json
{
  "username": "string",
  "email": "string",
  "password": "string"
}
```

**请求头:**
```
Content-Type: application/json
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 忘记密码

```
POST /forgotPassword
```

**请求参数:**
```json
{
  "username": "string",
  "email": "string",
  "password": "string"
}
```

**请求头:**
```
Content-Type: application/json
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 获取邮箱验证码

```
POST /getUserCode
```

**请求参数:**
```json
{
  "username": "string",
  "email": "string"
}
```

**请求头:**
```
Content-Type: application/json
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 用户产品绑定

```
POST /userProductBind
```

**请求参数:**
```json
{
  "productId": 1
}
```

**请求头:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 获取用户产品绑定列表

```
GET /userProductBindList
```

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": []
}
```

### 用户产品解绑

```
POST /userProductUnbind
```

**请求参数:**
```json
{
  "productId": 1
}
```

**请求头:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

## 产品管理

### 获取产品信息

```
GET /Product
```

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 提交产品信息

```
POST /Product
```

**请求参数:**
```json
{
  "name": "string",
  "description": "string",
  "secret": "string",
  "registerType": 0
}
```

**请求头:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 删除产品信息

```
DELETE /Product
```

**查询参数:**
- `id`: 产品ID

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 通过产品ID获取产品名称

```
GET /getProductName
```

**查询参数:**
- `id`: 产品ID

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

## 物模型管理

### 获取物模型

```
GET /ProductModel
```

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": []
}
```

### 通过产品ID获取物模型

```
GET /ProductModelByproductId
```

**查询参数:**
- `productId`: 产品ID

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": []
}
```

### 创建物模型

```
POST /ProductModel
```

**请求参数:**
```json
{
  "name": "string",
  "productId": 1,
  "description": "string"
}
```

**请求头:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 删除物模型

```
DELETE /ProductModel
```

**查询参数:**
- `id`: 物模型ID

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

## 设备管理

### 获取设备

```
GET /ProductDevice
```

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": []
}
```

### 创建设备

```
POST /ProductDevice
```

**请求参数:**
```json
{
  "name": "string",
  "modelId": 1,
  "description": "string",
  "password": "string"
}
```

**请求头:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 删除设备

```
DELETE /ProductDevice
```

**查询参数:**
- `id`: 设备ID

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

## 属性管理

### 获取属性

```
GET /ProductData
```

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": []
}
```

### 创建属性

```
POST /ProductData
```

**请求参数:**
```json
{
  "jsonKey": "string",
  "modelId": 1,
  "description": "string",
  "storageType": "string",
  "rRw": 0,
  "type": "string",
  "max": "string",
  "min": "string",
  "step": "string",
  "unit": "string"
}
```

**请求头:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 删除属性

```
DELETE /ProductData
```

**查询参数:**
- `id`: 属性ID

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

## 功能管理

### 获取功能

```
GET /ProductFunction
```

**请求头:**
```
Authorization: Bearer {token}
```

### 提交功能

```
POST /ProductFunction
```

**请求参数:**
```json
{
  "functionName": "string",
  "jsonKey": "string",
  "modelId": 1,
  "dataType": "string",
  "description": "string",
  "type": "string",
  "max": "string",
  "min": "string",
  "step": "string",
  "unit": "string"
}
```

**请求头:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

### 删除功能

```
DELETE /ProductFunction
```

**查询参数:**
- `id`: 功能ID

**请求头:**
```
Authorization: Bearer {token}
```

## 事件管理

### 获取事件

```
GET /ProductEvent
```

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": []
}
```

### 提交事件

```
POST /ProductEvent
```

**请求参数:**
```json
{
  "jsonKey": "string",
  "modelId": 1,
  "description": "string"
}
```

**请求头:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 删除事件

```
DELETE /ProductEvent
```

**查询参数:**
- `id`: 事件ID

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 获取事件入参

```
GET /EventData
```

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": []
}
```

### 提交事件入参

```
POST /EventData
```

**请求参数:**
```json
{
  "jsonKey": "string",
  "modelId": 1,
  "description": "string",
  "type": "string",
  "max": "string",
  "min": "string",
  "step": "string",
  "unit": "string"
}
```

**请求头:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 删除事件入参

```
DELETE /EventData
```

**查询参数:**
- `id`: 事件入参ID

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

## 设备控制

### 设备属性或服务控制

```
POST /control
```

**请求参数:**
```json
{
  "key": ["string"],
  "mode": "string",
  "functionName": "string",
  "name": "string",
  "qos": 0,
  "status": "string",
  "value": ["string"]
}
```

**请求头:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 获取属性实时数据

```
POST /metaData
```

**请求参数:**
```json
{
  "deviceId": "string",
  "jsonKey": "string"
}
```

**请求头:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 读取设备数据

```
POST /readData
```

**请求参数:**
```json
{
  "jsonKey": "string",
  "name": "string",
  "time1": 0,
  "time2": 0
}
```

**请求头:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": []
}
```

### 读取事件数据

```
POST /readEvent
```

**请求 parameters:**
```json
{
  "jsonKey": "string",
  "name": "string",
  "time1": 0,
  "time2": 0
}
```

**请求头:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": []
}
```

## Agent 智能体

### 使用大模型控制设备

```
POST /aiControl
```

**请求参数:**
```json
{
  "message": "string",
  "productId": 0
}
```

**请求头:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 使用大模型控制设备(语音)

```
POST /aiControl/audio
```

**查询参数:**
- `productId`: 产品ID
- `tts`: 是否使用TTS

**表单数据:**
- `file`: 音频文件

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 使用大模型控制设备(语音)流式

```
POST /aiControl/audio/stream
```

**查询参数:**
- `productId`: 产品ID

**表单数据:**
- `file`: 音频文件

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

## 知识库

### 获取知识库

```
GET /knowledgeChat
```

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": []
}
```

### 搜索知识库

```
POST /knowledgeChatRecall
```

**请求参数:**
```json
{
  "productId": 0,
  "query": "string"
}
```

**请求头:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 提交知识库

```
POST /knowledgeChat
```

**查询参数:**
- `productId`: 产品ID
- `filename`: 文件名

**表单数据:**
- `file`: 知识库文件

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 删除知识库

```
DELETE /knowledgeChat
```

**查询参数:**
- `id`: 知识库ID

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

## OTA 升级

### 获取OTA列表

```
GET /otaList
```

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": []
}
```

### 上传OTA固件

```
POST /otaUpload
```

**查询参数:**
- `name`: 固件名称
- `productId`: 产品ID

**表单数据:**
- `file`: 固件文件

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 删除OTA固件

```
DELETE /otaDelete
```

**查询参数:**
- `id`: 固件ID

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 启用OTA升级

```
POST /otaEnable
```

**查询参数:**
- `name`: 固件名称
- `deviceName`: 设备名称

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 获取被动OTA列表

```
GET /otaPassive
```

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": []
}
```

### 提交被动OTA

```
POST /otaPassive
```

**请求参数:**
```json
{
  "deviceName": "string",
  "name": "string",
  "description": "string",
  "versionName": "string"
}
```

**请求头:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 启用被动OTA

```
GET /otaPassiveEnable
```

**查询参数:**
- `deviceName`: 设备名称

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 删除被动OTA

```
DELETE /otaPassive
```

**查询参数:**
- `id`: 被动OTA ID

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

## 小智相关

### 小智OTA管理

```
GET /xiaozhi/otaManage
```

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": []
}
```

### 小智OTA绑定

```
POST /xiaozhi/otaManage
```

**请求参数:**
```json
{
  "code": "string",
  "productId": 0
}
```

**请求头:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 小智OTA解绑

```
DELETE /xiaozhi/otaManage
```

**查询参数:**
- `id`: 绑定ID

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 小智OTA被动升级

```
GET /xiaozhi/otaPassive
```

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": []
}
```

### 提交小智OTA被动升级

```
POST /xiaozhi/otaPassive
```

**请求参数:**
```json
{
  "productId": 0,
  "name": "string",
  "description": "string",
  "versionName": "string"
}
```

**请求头:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 删除小智OTA被动升级

```
DELETE /xiaozhi/otaPassive
```

**查询参数:**
- `id`: 小智OTA被动升级ID

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

## MCP 服务

### 获取MCP服务器列表

```
GET /mcpServer
```

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": []
}
```

### 添加MCP服务器

```
POST /mcpServer
```

**请求参数:**
```json
{
  "description": "string",
  "productId": 0,
  "sseEndpoint": "string",
  "url": "string"
}
```

**请求头:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 删除MCP服务器

```
DELETE /mcpServer
```

**查询参数:**
- `id`: MCP服务器ID

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 获取MCP接入点URL

```
GET /mcpEndpoint
```

**查询参数:**
- `productId`: 产品ID

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 获取MCP接入点工具

```
GET /mcpEndpoint/tools
```

**查询参数:**
- `productId`: 产品ID

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": []
}
```

## 系统信息

### 获取平台运行环境信息

```
GET /machineMessage
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 获取连接的设备数量

```
GET /getConnectedNum
```

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

## 告警事件

### 获取告警事件

```
GET /alarmEvent
```

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": []
}
```

### 提交告警事件

```
POST /alarmEvent
```

**请求参数:**
```json
{
  "modelId": 0,
  "name": "string",
  "description": "string",
  "alarmType": "string",
  "alarmLevel": "string",
  "alarmRule": "string"
}
```

**请求头:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 删除告警事件

```
DELETE /alarmEvent
```

**查询参数:**
- `id`: 告警事件ID

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

## 产品角色

### 获取产品角色列表

```
GET /productRole
```

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": []
}
```

### 添加产品角色

```
POST /productRole
```

**请求参数:**
```json
{
  "productId": 0,
  "roleName": "string",
  "description": "string",
  "permissions": "string"
}
```

**请求头:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 修改产品角色

```
PUT /productRole
```

**请求参数:**
```json
{
  "id": 0,
  "productId": 0,
  "roleName": "string",
  "description": "string",
  "permissions": "string"
}
```

**请求头:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 删除产品角色

```
DELETE /productRole
```

**查询参数:**
- `id`: 产品角色ID

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

## 产品语义路由设置

### 获取产品语义路由设置列表

```
GET /productRouterSet
```

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": []
}
```

### 添加产品语义路由设置

```
POST /productRouterSet
```

**请求 parameters:**
```json
{
  "productId": 0,
  "routerName": "string",
  "description": "string",
  "routerRule": "string"
}
```

**请求头:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 删除产品语义路由设置

```
DELETE /productRouterSet
```

**查询参数:**
- `id`: 产品语义路由设置ID

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

## 产品工具禁用

### 获取产品工具禁用列表

```
GET /productToolsBan
```

**查询参数:**
- `productId`: 产品ID

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": []
}
```

### 提交产品工具禁用

```
POST /productToolsBan
```

**请求参数:**
```json
{
  "productId": 0,
  "tools": "string",
  "description": "string"
}
```

**请求头:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 删除产品工具禁用

```
DELETE /productToolsBan
```

**查询参数:**
- `productId`: 产品ID

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

## 长期记忆

### 获取长期记忆

```
GET /longMemory
```

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": []
}
```

### 提交/修改长期记忆

```
POST /longMemory
```

**请求参数:**
```json
{
  "productId": 0,
  "memoryKey": "string",
  "memoryValue": "string",
  "description": "string"
}
```

**请求头:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 删除长期记忆

```
DELETE /longMemory
```

**查询参数:**
- `id`: 长期记忆ID

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

## 产品语音定制

### 获取产品语音定制

```
GET /productVoiceDiy
```

**查询参数:**
- `productId`: 产品ID

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": []
}
```

### 提交/修改产品语音定制

```
POST /productVoiceDiy
```

**请求参数:**
```json
{
  "productId": 0,
  "voiceName": "string",
  "voiceContent": "string",
  "description": "string"
}
```

**请求头:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 删除产品语音定制

```
DELETE /productVoiceDiy
```

**查询参数:**
- `id`: 产品语音定制ID

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

## 聊天记忆

### 获取聊天记忆

```
GET /memory
```

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": []
}
```

### 修改聊天记忆

```
PUT /memory
```

**请求参数:**
```json
{
  "chatId": 0,
  "memoryContent": "string",
  "description": "string"
}
```

**请求头:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 删除聊天记忆

```
DELETE /memory
```

**查询参数:**
- `id`: 聊天记忆ID

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

## AI视觉

### AI视觉解释

```
POST /vision/explain
```

**查询参数:**
- `question`: 问题

**表单数据:**
- `file`: 图像文件

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

## 微信相关

### 微信登录

```
POST /wxLogin
```

**请求参数:**
```json
{
  "openid": "string",
  "unionid": "string",
  "nickname": "string",
  "headimgurl": "string"
}
```

**请求头:**
```
Content-Type: application/json
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 微信注册

```
POST /wxRegister
```

**请求参数:**
```json
{
  "openid": "string",
  "unionid": "string",
  "nickname": "string",
  "headimgurl": "string"
}
```

**请求头:**
```
Content-Type: application/json
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 获取微信绑定产品

```
GET /wxBindProduct
```

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": []
}
```

### 微信绑定产品

```
POST /wxBindProduct
```

**请求参数:**
```json
{
  "productId": 0
}
```

**请求头:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 微信解绑产品

```
DELETE /wxBindProduct
```

**请求参数:**
```json
{
  "productId": 0
}
```

**请求头:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

## 系统管理（管理员专用）

### 获取MQTT用户信息

```
GET /mqttUser
```

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": []
}
```

### 获取管理员配置列表

```
GET /adminConfig
```

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": []
}
```

### 添加管理员配置

```
POST /adminConfig
```

**请求参数:**
```json
{
  "configKey": "string",
  "configValue": "string",
  "description": "string"
}
```

**请求头:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 修改管理员配置

```
PUT /adminConfig
```

**请求参数:**
```json
{
  "id": 0,
  "configKey": "string",
  "configValue": "string",
  "description": "string"
}
```

**请求头:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 删除管理员配置

```
DELETE /adminConfig
```

**查询参数:**
- `id`: 配置ID

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

## LLM提供商信息管理

### 获取LLM提供商信息

```
GET /llmProviderInformation
```

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": []
}
```

### 创建或更新LLM提供商信息

```
POST /llmProviderInformation
```

**请求参数:**
```json
{
  "id": 0,
  "providerName": "string",
  "providerType": "string",
  "apiKey": "string",
  "baseUrl": "string",
  "model": "string",
  "temperature": 0.7,
  "maxTokens": 2048,
  "timeout": 30000
}
```

**请求头:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 删除LLM提供商信息

```
DELETE /llmProviderInformation
```

**查询参数:**
- `id`: LLM提供商信息ID

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

## 产品LLM模型配置

### 获取产品LLM模型配置

```
GET /productLlmModel
```

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": []
}
```

### 创建或更新产品LLM模型配置

```
POST /productLlmModel
```

**请求参数:**
```json
{
  "id": 0,
  "productId": 0,
  "providerId": 0,
  "modelName": "string",
  "modelType": "string",
  "enabled": true,
  "priority": 1
}
```

**请求头:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

### 删除产品LLM模型配置

```
DELETE /productLlmModel
```

**查询参数:**
- `id`: 产品LLM模型配置ID

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 999 | 失败 |
| 1001 | 参数无效 |
| 1002 | 参数为空 |
| 2001 | 用户未登录 |
| 2002 | 账号已过期 |
| 2003 | 密码错误 |
| 2004 | 密码过期 |
| 2005 | 账号不可用 |
| 2006 | 账号被锁定 |
| 2007 | 账号不存在 |
| 2008 | 账号已存在 |
| 3001 | 没有权限 |
| 3002 | 存在依赖关系，无法删除 |

## 响应格式

所有 API 接口遵循统一的响应格式：

```json
{
  "success": true,
  "errorCode": 200,
  "errorMsg": "成功",
  "data": {}
}
```

- `success`: 请求是否成功
- `errorCode`: 错误码
- `errorMsg`: 错误消息
- `data`: 响应数据（可选）