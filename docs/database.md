# 数据库架构

## 概述

IntelliConnect 平台使用 MySQL 作为主要的关系型数据库，存储用户信息、设备数据、物模型配置等核心业务数据。同时使用 Redis 作为缓存和会话存储，InfluxDB 作为时序数据库存储设备数据。

## 数据库设计原则

- **命名规范**: 表名使用下划线命名法，如 `user`, `product_device`
- **主键设计**: 所有表使用自增整型主键 `id`
- **外键约束**: 通过 `product_id`, `model_id` 等字段建立表间关联
- **字符集**: 使用 UTF-8 字符集支持国际化
- **索引优化**: 在常用查询字段上建立索引

## 核心数据表

### 1. 用户表 (user)

存储系统用户信息。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | INT (PK) | 用户唯一标识，自增主键 |
| username | VARCHAR | 用户名 |
| email | VARCHAR | 邮箱地址 |
| password | VARCHAR | 密码（BCrypt 加密） |
| role | VARCHAR | 用户角色 (ADMIN, USER, GUEST) |

### 2. 产品表 (product)

定义产品类别，用于设备分组和隔离。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | INT (PK) | 产品唯一标识，自增主键 |
| product_name | VARCHAR | 产品名称 |
| keyvalue | VARCHAR | 产品密钥 |
| register | INT | 注册状态 |

### 3. 物模型表 (product_model)

定义设备的功能模板。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | INT (PK) | 物模型唯一标识，自增主键 |
| product_id | INT (FK) | 所属产品ID |
| name | VARCHAR | 物模型名称 |
| description | VARCHAR | 物模型描述 |

### 4. 设备表 (product_device)

存储具体设备信息。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | INT (PK) | 设备唯一标识，自增主键 |
| model_id | INT (FK) | 所属物模型ID |
| name | VARCHAR | 设备名称 |
| client_id | VARCHAR | MQTT客户端ID |
| password | VARCHAR | 设备密码 |
| description | VARCHAR | 设备描述 |
| subscribe_topic | VARCHAR | 订阅主题 |
| online | VARCHAR | 在线状态 |
| allow | INT | 允许状态 |

### 5. 功能表 (product_function)

定义物模型的功能接口。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | INT (PK) | 功能唯一标识，自增主键 |
| model_id | INT (FK) | 所属物模型ID |
| function_name | VARCHAR | 功能名称 |
| json_key | VARCHAR | JSON键名 |
| data_type | VARCHAR | 数据类型 (input/output) |
| description | VARCHAR | 功能描述 |
| type | VARCHAR | 参数类型 (int, float, string) |
| max | VARCHAR | 最大值 |
| min | VARCHAR | 最小值 |
| step | VARCHAR | 步长 |
| unit | VARCHAR | 单位 |

### 6. OTA固件表 (ota)

存储OTA升级固件信息。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | INT (PK) | 固件唯一标识，自增主键 |
| product_id | INT (FK) | 所属产品ID |
| name | VARCHAR | 固件名称 |
| path | VARCHAR | 固件存储路径 |

### 7. 事件数据表 (event_data)

存储设备事件数据。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | INT (PK) | 事件唯一标识，自增主键 |
| device_name | VARCHAR | 设备名称 |
| json_key | VARCHAR | 事件键名 |
| value | VARCHAR | 事件值 |
| time | DATETIME | 事件时间 |

### 8. Agent记忆表 (agent_memory)

存储Agent智能体对话记忆。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | INT (PK) | 记忆唯一标识，自增主键 |
| chat_id | VARCHAR | 对话ID |
| content | TEXT | 记忆内容 |

### 9. MCP服务器表 (mcp_server)

存储MCP协议服务器配置。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | INT (PK) | 服务器唯一标识，自增主键 |
| product_id | INT (FK) | 所属产品ID |
| description | VARCHAR | 服务器描述 |
| sse_endpoint | VARCHAR | SSE端点地址 |
| url | VARCHAR | 服务器URL |

### 10. 微信用户表 (wx_user)

存储微信用户信息。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | INT (PK) | 微信用户唯一标识，自增主键 |
| open_id | VARCHAR | 微信OpenID |
| nickname | VARCHAR | 微信昵称 |
| avatar | VARCHAR | 头像URL |
| union_id | VARCHAR | 微信UnionID |

## 数据库关系

```
[product] 1 -- * [product_model] 1 -- * [product_device]
    |                    |                    |
    |                    |                    *-- * [event_data]
    |                    |
    |                    *-- * [product_function]
    |
    *-- * [ota]
    |
    *-- * [mcp_server]
    |
    *-- * [user_product_bind]

[user] 1 -- * [user_product_bind] * -- 1 [product]
```

## 时序数据库 (InfluxDB)

用于存储设备的时序数据：

### measurement: device_data
- **tags**: device_name, product_id
- **fields**: 
  - value (数值)
  - type (数据类型)
- **time**: 时间戳

## Redis 缓存结构

### 1. 用户会话
- **key**: `session:{sessionId}`
- **value**: JSON 格式的会话信息

### 2. 设备状态缓存
- **key**: `device:status:{deviceName}`
- **value**: 设备在线状态

### 3. Token 缓存
- **key**: `token:{jwtToken}`
- **value**: 用户认证信息

## 数据库配置

### application.yaml 配置示例

```yaml
spring:
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/${MYSQL_DB:cwliot1.8}?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8
    username: ${MYSQL_USER:root}
    password: ${MYSQL_PASSWORD:password}
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: update  # create, update, validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
        format_sql: true

  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    timeout: 2000ms
  # InfluxDB 配置
  influx:
    user: ${INFLUXDB_USER:admin}
    # 你的数据库url
    url: http://${INFLUXDB_HOST:localhost}:${INFLUXDB_PORT:8086}
    password: ${INFLUXDB_PASSWORD:password}
    mapper-location: top.rslly.iot.dao
    dataBaseName: ${INFLUXDB_NAME:iot_data}
    retention: autogen     #保存策略
```

## 数据备份策略

### MySQL 备份
- **全量备份**: 每日执行完整数据库备份
- **增量备份**: 每小时备份事务日志
- **备份保留**: 保留最近7天的备份

### Redis 备份
- **RDB 快照**: 每6小时生成一次快照
- **AOF 日志**: 启用持久化确保数据完整性

## 性能优化

### 索引策略
- 在外键字段上创建索引
- 在常用查询条件字段上创建复合索引
- 定期分析查询执行计划

### 分区策略
- 时序数据按时间分区存储
- 大表按业务维度进行水平分表

## 安全措施

### 数据库安全
- **访问控制**: 限制数据库访问权限
- **传输加密**: 使用 SSL 连接数据库
- **审计日志**: 记录数据库操作日志
- **敏感数据**: 对敏感信息进行加密存储