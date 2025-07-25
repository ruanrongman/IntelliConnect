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
* 本项目基于springboot2.7开发，使用spring security作为安全框架
* 配备物模型(属性，功能和事件模块)和完善的监控模块
* 支持多种大模型(Qwen3系列，GLM系列和deepSeek等先进模型)和先进的Agent智能体技术提供出色的AI智能，可以快速搭建智能物联网应用(首个基于Agent智能体设计的物联网平台)
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
* 安装java17环境
* 修改配置文件application.yaml(设置ddl-auto为update模式)
* java -jar IntelliConnect-1.8-SNAPSHOT.jar

## 项目特色
* 极简主义，层次分明，符合mvc分层结构
* 完善的物模型抽象，使得iot开发者可以专注于业务本身
* AI能力丰富，支持Multi-Agent技术，快速开发AI智能应用

# 小智 ESP-32 后端服务(xiaozhi-esp32-server)
本项目能够为开源智能硬件项目 [xiaozhi-esp32](https://github.com/78/xiaozhi-esp32)
提供后端服务。根据 [小智通信协议](https://ccnphfhqs21z.feishu.cn/wiki/M0XiwldO9iJwHikpXD5cEx71nKh) 使用 `Java` 实现。
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
* [智控台] (https://control.rslly.top)
* [小智ota地址] (https://api.rslly.top/xiaozhi/ota/)

## 交流群

欢迎加入群聊一起交流讨论有关Aiot相关的话题，链接过期了可以issue或email提醒一下作者。

<div style="width: 250px;margin: 0 auto;">
    <img src="./docs/images/9de17b0d11395da91b6eddd190a1710.jpg" width="250px"/>
</div>

## 致谢
* 感谢项目[xiaozhi-esp32](https://github.com/78/xiaozhi-esp32)提供强大的硬件语音交互。
* 感谢项目[Concentus: Opus for Everyone](https://github.com/lostromb/concentus)提供opus解码和编码。
* 感谢项目[TalkX](https://github.com/big-mouth-cn/talkx)提供了opus解码和编码的参考。
* 感谢项目[py-xiaozhi](https://github.com/huangjunsen0406/py-xiaozhi)方便项目进行小智开发调试。
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
