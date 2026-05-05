<pre align="center">

██╗ ███╗   ██╗ ████████╗ ███████╗ ██╗      ██╗      ██╗    ██████╗  ██████╗  ███╗   ██╗ ███╗   ██╗ ███████╗  ██████╗ ████████╗
██║ ████╗  ██║ ╚══██╔══╝ ██╔════╝ ██║      ██║      ██║   ██╔════╝ ██╔═══██╗ ████╗  ██║ ████╗  ██║ ██╔════╝ ██╔════╝ ╚══██╔══╝
██║ ██╔██╗ ██║    ██║    █████╗   ██║      ██║      ██║   ██║      ██║   ██║ ██╔██╗ ██║ ██╔██╗ ██║ █████╗   ██║         ██║   
██║ ██║╚██╗██║    ██║    ██╔══╝   ██║      ██║      ██║   ██║      ██║   ██║ ██║╚██╗██║ ██║╚██╗██║ ██╔══╝   ██║         ██║   
██║ ██║ ╚████║    ██║    ███████╗ ███████╗ ███████╗ ██║   ╚██████╗ ╚██████╔╝ ██║ ╚████║ ██║ ╚████║ ███████╗ ╚██████╗    ██║   
╚═╝ ╚═╝  ╚═══╝    ╚═╝    ╚══════╝ ╚══════╝ ╚══════╝ ╚═╝    ╚═════╝  ╚═════╝  ╚═╝  ╚═══╝ ╚═╝  ╚═══╝ ╚══════╝  ╚═════╝    ╚═╝   


Built by RSLLY
</pre>

<p align="center">
    <a target="_blank" href="">
        <img src="https://img.shields.io/badge/license-apache2.0-yellow?style=flat-square"/>
    </a>
    <a target="_blank" href=''>
        <img src="https://img.shields.io/badge/release-v0.1-blue?style=flat-square"/>
    </a>
    <a target="_blank" href="https://wordpress.rslly.top">
        <img src="https://img.shields.io/badge/cwl-project1.8-green?style=flat-square&link=https://wordpress.rslly.top"/>
    </a>
    <a href="https://deepwiki.com/ruanrongman/IntelliConnect"><img src="https://deepwiki.com/badge.svg" alt="Ask DeepWiki"></a>
</p>

<p align="center">
  <img src="./docs/images/logo2.png" width="300px" height="250px"/>
</p>

## 概述
* 本项目基于springboot3.5.7开发，使用spring security作为安全框架
* 配备物模型(属性，功能和事件模块)和完善的监控模块
* 支持多种大模型(Qwen3系列，GLM系列和deepSeek等先进模型)和先进的Agent智能体技术提供出色的AI智能，可以快速搭建智能物联网应用(首个基于Agent智能体设计的物联网平台)
* 支持知识库技术，可以针对专有领域构建知识库使得智能体更贴心
* 支持MCP协议（模型上下文协议）可以自由扩展智能体能力
* 支持快速构建智能语音应用，支持语音识别和语音合成
* 支持多种iot协议，使用emqx exhook作为mqtt通讯，可扩展性强
* 支持OTA空中升级技术
* 支持微信小程序和微信服务号
* 支持小智AI硬件
* 使用常见的mysql和redis数据库，上手简单
* 支持时序数据库influxdb

## 安装运行
> 推荐使用docker安装，docker-compose.yaml文件在docker目录下，执行 docker-compose up 可初始化
mysql,redis,emqx和influxdb环境，安装详情请看官方文档。
* 安装mysql和redis数据库，高性能运行推荐安装时序数据库influxdb
* 安装EMQX集群,并配置好exhook，本项目使用exhook作为mqtt消息的处理器
* 安装java21环境
* 修改配置文件application.yaml(设置ddl-auto为update模式)
* java -jar IntelliConnect-1.8-SNAPSHOT.jar

## 功能模块 

| 功能模块                 | 状态 | 描述                              |
|----------------------|------|---------------------------------|
| **首句响应**             | ✅ | 唤醒词响应时间 <1秒，极速响应体验              |
| **平均响应速度**           | ✅ | 平均对话响应时间 <2.5秒，流畅对话体验           |
| **双向流式交互**           | ✅ | 支持阿里,minimax流式播放，实时语音输入和回复输出    |
| **用户端**              | ✅ | 友好的用户端操作界面，列表式管理，适合多设备管理        |
| **角色配置和语义路由设置**      | ✅ | 角色设置和语义路由设置分离，用户体验更佳            |
| **MCP接入点**           | ✅ | 基于角色的MCP工具接入点，扩展功能接入            |
| **MCP服务**            | ✅ | SSE MCP接入方式，支持更多第三方服务集成         |
| **Function Call安抚词** | ✅ | 工具调用前置安抚词，提升用户体验                |
| **长期记忆**             | ✅ | 根据用户对话，提取关键信息记录，智能记忆管理          |
| **知识库**              | ✅ | RAG检索知识库（后期拓展图知识库），文档上传，智能检索    |
| **记忆总结**             | ✅ | 基于知识库长期记忆总结，智能对话分析              |
| **多设备协同**            | ✅ | AB设备协同控制（物模型MQTT独立信道），全屋智能协同工作  |
| **OTA固件升级**          | ✅ | 固件上传，主动升级，被动升级以及远程设备管理          |
| **远程控制**             | ✅ | 支持远程控制设备，强大的物联网智能控制能力           |
| **设备数据可视化**          | ✅ | 数据可视化，设备数据可视化，设备在线/离线状态         |
| **事件告警**             | ✅ | 设备错误告警，设备异常处理，邮件下发，微信告警         |
| **Skills技能整合**       | ✅ | 重磅功能，独家提供智能体Skills集成            |
| **微信小智**             | ✅ | 重磅功能，独家提供微信小程序，服务号集成            |
| **原生知识图谱**           | ✅ | 重磅功能，原生知识图谱，知识图谱可视化和智能遗忘机制      |
| **定时Agent**          | ✅ | 重磅功能，实现类似openclaw的7*24小时定时调用智能体 |

### 开发中功能 🚧

| 功能模块 | 状态 | 描述 |
|---------|------|------|
| **MQTT协议** | 🚧 | 支持MQTT通信协议，长连接、服务端主动唤醒 |
| **混合模式角色** | 🚧 | 支持多角色混合模式，通过不同唤醒词唤醒不同角色（自动切换） |
| **声纹识别** | 🚧 | 支持声纹识别功能，实现个性化语音助手 |
| **多语言支持** | 🚧 | 支持多语言界面，满足不同地区用户需求 |
| **情感分析** | 🚧 | 通过语音情感分析，提供更人性化的回复 |
| **自定义插件系统** | 🚧 | 支持自定义插件开发，扩展系统功能 |
| **聊天数据可视化** | 🚧 | 聊天频率统计图表等数据可视化功能，监控对话数据趋势 |

## 项目特色
* 极简主义，层次分明，符合mvc分层结构
* 完善的物模型抽象，使得iot开发者可以专注于业务本身
* AI能力丰富，支持Multi-Agent技术，快速开发AI智能应用

# 小智 ESP-32 后端服务(xiaozhi-esp32-server)
本项目能够为开源智能硬件项目 [xiaozhi-esp32](https://github.com/78/xiaozhi-esp32)
提供完整的后端服务。根据 [小智通信协议](https://ccnphfhqs21z.feishu.cn/wiki/M0XiwldO9iJwHikpXD5cEx71nKh) 使用 `Java` 实现。
适合希望本地部署的用户，不同于单纯语音交互，本项目重点在于提供更强大的物联网和智能体能力。
---

## 项目文档和视频演示
* 项目文档和视频演示地址https://ruanrongman.github.io/IntelliConnect/
* 技术博客地址https://wordpress.rslly.top
* 社区地址https://github.com/cwliot
* 创万联社区公众号：微信直接搜索创万联

## 相关项目和社区
* 创万联(cwl): 专注于物联网和人工智能技术的开源社区。
* Promptulate https://github.com/Undertone0809/promptulate [A LLM application and Agent development framework.]
* Rymcu https://github.com/rymcu [为数百万人服务的开源嵌入式知识学习交流平台]

## 智控台和小智ota地址(仅用于测试，不保证稳定，请勿用于生产环境)
* [智控台] (https://control-ic.atdak.com)
* [小智ota地址] (https://api-ic.atdak.com/xiaozhi/ota/)

## 交流群

欢迎加入群聊一起交流讨论有关Aiot相关的话题，有机会获取项目的免费部署咨询，链接过期了可以issue或email提醒一下作者。

<div style="width: 250px;margin: 0 auto;">
    <img src="./docs/images/c2d1bf94e4cea5074c37ef021b7922d3.jpg" width="250px"/>
</div>

## 致谢
* 感谢项目[xiaozhi-esp32](https://github.com/78/xiaozhi-esp32)提供强大的硬件语音交互。
* 感谢项目[Concentus: Opus for Everyone](https://github.com/lostromb/concentus)提供opus解码和编码。
* 感谢项目[TalkX](https://github.com/big-mouth-cn/talkx)提供了opus解码和编码的参考。
* 感谢项目[py-xiaozhi](https://github.com/huangjunsen0406/py-xiaozhi)方便项目进行小智开发调试。
* 感谢项目[vue3-antd-manage](https://github.com/BaiFangZi/vue3-antd-manage)本项目智控台基于该项目二次开发。
* [电波bilibili君](https://space.bilibili.com/119751)赞助项目开发。

## 授权协议
- 个人授权
    - 遵循 [Apache 2.0 协议](https://www.apache.org/licenses/LICENSE-2.0)
- 商业授权
    - 商业使用请联系 [创万联 官方](https://wordpress.rslly.top) 获取授权

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=ruanrongman/IntelliConnect&type=Date)](https://www.star-history.com/#ruanrongman/IntelliConnect&Date)
## 贡献

本人正在尝试一些更加完善的抽象模式，支持更多的物联网协议和数据存储形式，如果你有更好的建议，欢迎一起讨论交流。
