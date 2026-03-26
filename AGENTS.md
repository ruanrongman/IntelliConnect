# IntelliConnect 项目上下文

## 项目概述

IntelliConnect（创万联 IoT 平台）是一个基于 Spring Boot 3.5.7 和 Java 21 开发的智能物联网平台，是国内首个基于 Agent 智能体设计的物联网平台。项目集成了先进的 AI 能力和完善的物联网功能，支持快速搭建智能物联网应用。

### 核心特性

- **AI 智能体能力**：支持多种大模型（Qwen3 系列、GLM 系列、DeepSeek 等）和 Multi-Agent 技术
- **MCP 协议支持**：支持模型上下文协议（Model Context Protocol），可自由扩展智能体能力
- **知识库技术**：支持 RAG 检索增强生成，包括向量知识库和原生知识图谱
- **语音交互**：支持语音识别（ASR）和语音合成（TTS），可快速构建智能语音应用
- **物联网协议**：使用 EMQX exhook 作为 MQTT 通讯，支持多种 IoT 协议
- **物模型抽象**：完善的物模型（属性、功能、事件模块）和监控模块
- **OTA 升级**：支持固件上传、主动升级、被动升级及远程设备管理
- **微信集成**：支持微信小程序和微信服务号集成
- **小智 AI 硬件**：为开源智能硬件项目 xiaozhi-esp32 提供完整后端服务

## 技术栈

### 后端框架
- **Java 21** - 主要开发语言
- **Spring Boot 3.5.7** - 核心框架
- **Spring Security** - 安全框架
- **Spring Data JPA** - ORM 框架
- **Spring WebSocket** - 实时通信

### 数据存储
- **MySQL 8.0** - 主数据库
- **Redis 6.2** - 缓存数据库
- **InfluxDB 1.6** - 时序数据库（可选，高性能场景推荐）

### AI 相关
- **LangChain4j** - LLM 应用开发框架
- **ChromaDB** - 向量数据库（用于 RAG）
- **ONNX Runtime** - VAD 模型推理
- 多种 LLM SDK：阿里 DashScope、智谱 GLM、讯飞语音等

### 消息通信
- **EMQX 5.8** - MQTT 消息代理
- **gRPC** - exhook 通信协议

### 构建工具
- **Maven** - 项目构建管理
- **Lombok** - 简化代码

## 项目结构

```
src/main/java/top/rslly/iot/
├── config/              # 配置类（安全、Redis、WebSocket、CORS等）
├── controllers/         # 控制器层
│   ├── Auth.java       # 认证相关接口
│   ├── Tool.java       # 工具接口
│   ├── Wx.java         # 微信相关接口
│   └── XiaoZhi.java    # 小智硬件接口
├── dao/                 # 数据访问层（JPA Repository）
├── models/              # 实体类
│   └── influxdb/       # InfluxDB 实体
├── param/               # 参数类
│   ├── prompt/         # Prompt 描述类
│   ├── request/        # 请求参数
│   └── response/       # 响应参数
├── services/            # 服务层
│   ├── agent/          # AI Agent 服务
│   ├── iot/            # 物联网服务
│   ├── knowledgeGraphic/ # 知识图谱服务
│   ├── storage/        # 存储服务
│   ├── thingsModel/    # 物模型服务
│   └── wechat/         # 微信服务
├── transfer/            # 数据传输层
│   └── mqtt/           # MQTT 通信
├── utility/             # 工具类
│   ├── ai/             # AI 相关工具
│   │   ├── chain/      # 路由链（Router）
│   │   ├── llm/        # LLM 工厂和实现
│   │   ├── mcp/        # MCP 协议实现
│   │   ├── prompts/    # Prompt 模板
│   │   ├── rag/        # RAG 检索
│   │   ├── toolAgent/  # Agent 实现
│   │   ├── tools/      # 各种工具实现
│   │   └── voice/      # 语音处理
│   ├── exhook/         # EMQX exhook
│   └── influxdb/       # InfluxDB 工具
└── DemoApplication.java # 应用入口
```

## 构建和运行

### 环境要求
- Java 21+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+
- EMQX 5.x（配置 exhook）
- InfluxDB 1.6+（可选，用于时序数据）

### Docker 快速部署

```bash
cd docker
docker-compose up -d
```

这将启动以下服务：
- MySQL（端口 3306）
- Redis（端口 6379）
- InfluxDB（端口 8086）
- EMQX（端口 1883/8083/8084/18083）
- ChromaDB（端口 8000）
- FunASR（端口 10095/10096）

### 本地构建

```bash
# 编译项目
mvn clean package -DskipTests

# 运行（首次启动请将 application.yaml 中的 ddl-auto 设置为 update）
java -jar target/IntelliConnect-1.8-SNAPSHOT.jar
```

### 配置文件

主要配置文件位于 `src/main/resources/application.yaml`，关键配置项：

| 配置项 | 说明 |
|--------|------|
| `spring.datasource.*` | MySQL 数据库配置 |
| `spring.data.redis.*` | Redis 配置 |
| `spring.influx.*` | InfluxDB 配置 |
| `mqtt.*` | MQTT 连接配置 |
| `grpc.server.port` | gRPC 服务端口（默认 9090） |
| `ai.*` | AI 模型配置（API Key、模型选择等） |
| `wx.*` | 微信配置 |
| `rag.*` | RAG/知识库配置 |

## AI 工具系统

### Router 路由器

`Router.java` 是 AI 系统的核心路由组件，负责根据用户意图分发到不同的工具：

| 分类 | 工具 | 说明 |
|------|------|------|
| 1 | WeatherTool | 天气查询 |
| 2 | ControlTool | 设备控制 |
| 3 | MusicTool | 音乐播放 |
| 4 | Agent | 通用 Agent |
| 5 | ChatTool | 闲聊对话 |
| 6 | WxBoundProductTool | 微信绑定产品 |
| 7 | WxProductActiveTool | 微信产品激活 |
| 8 | ScheduleTool | 日程管理 |
| 9 | ProductRoleTool | 角色切换 |
| 10 | McpAgent | MCP Agent |
| 11 | GoodByeTool | 结束对话 |

### LLM 工厂

`LLMFactory.java` 支持 LLM 的动态创建，支持的供应商前缀：

- `dashscope-*` - 阿里云 DashScope（Qwen 系列）
- `silicon-*` - SiliconFlow
- `uniapi-*` - UniAPI
- `custom-*` - 自定义 API
- `glm` - 智谱 GLM

支持 `*-think[-N]` 后缀启用思考模式。

### 分支预测优化

Router 支持分支预测功能（`ai.branch-prediction.enabled`），可并行执行分类和闲聊预测，提升响应速度。

## 开发规范

### 代码风格
- 使用 Lombok 简化 POJO
- 遵循 MVC 分层架构
- Service 层使用接口+实现类模式
- 使用 Spring 注解进行依赖注入

### 命名约定
- Entity 实体类以 `Entity` 结尾
- Repository 接口以 `Repository` 结尾
- Service 接口以 `Service` 结尾，实现类以 `ServiceImpl` 结尾
- Tool 工具类以 `Tool` 结尾
- Prompt 模板以 `Prompt` 结尾

### License Header

所有 Java 文件需包含 Apache 2.0 许可证头：

```java
/**
 * Copyright © 2023-2030 The ruanrongman Authors
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
```

### 格式化

项目使用 Spotless 插件进行代码格式化：

```bash
mvn spotless:apply
```

## 相关资源

- **项目文档**: https://ruanrongman.github.io/IntelliConnect/
- **技术博客**: https://wordpress.rslly.top
- **GitHub**: https://github.com/ruanrongman/IntelliConnect
- **小智硬件项目**: https://github.com/78/xiaozhi-esp32

## 授权协议

- 个人使用：Apache 2.0 协议
- 商业使用：请联系创万联官方获取授权
