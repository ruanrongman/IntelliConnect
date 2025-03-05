<h1 align="center">
    IntelliConnect
</h1>

<p align="center">
    <a target="_blank" href="">
        <img src="https://img.shields.io/badge/license-apache2.0-yellow?style=flat-square"/>
    </a>
    <a target="_blank" href=''>
        <img src="https://img.shields.io/badge/release-v0.1-blue?style=flat-square"/>
    </a>
    <a target="_blank" href="">
        <img src="https://img.shields.io/badge/cwl-project1.8-green?style=flat-square&link=https://wordpress.rslly.top"/>
    </a>
</p>

<p align="center">
  <img src="./docs/images/logo2.png" width="300px" height="250px"/>
</p>

## 概述
* 本项目基于springboot2.7开发，使用spring security作为安全框架
* 配备物模型(属性，功能和事件模块)和完善的监控模块
* 支持多种大模型和先进的Agent智能体技术提供出色的AI智能，可以快速搭建智能物联网应用(首个基于Agent智能体设计的物联网平台)
* 支持快速构建智能语音应用，支持语音识别和语音合成
* 支持多种iot协议，使用emqx exhook作为mqtt通讯，可扩展性强
* 支持OTA空中升级技术
* 支持微信小程序和微信服务号
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
* AI能力丰富，支持Agent智能体技术，快速开发AI智能应用

## 项目文档和视频演示
* 项目文档和视频演示地址https://ruanrongman.github.io/IntelliConnect/
* 技术博客地址https://wordpress.rslly.top
* 社区地址https://github.com/cwliot
* 创万联社区公众号：微信直接搜索创万联

## 相关项目和社区
* 创万联(cwl): 专注于物联网和人工智能技术的开源社区。
* Promptulate https://github.com/Undertone0809/promptulate [A LLM application and Agent development framework.]
* Rymcu https://github.com/rymcu [为数百万人服务的开源嵌入式知识学习交流平台]
## 交流群

欢迎加入群聊一起交流讨论有关Aiot相关的话题，链接过期了可以issue或email提醒一下作者。

<div style="width: 250px;margin: 0 auto;">
    <img src="./docs/images/9a3e5507e151c3be478ecf57e19ebb8.jpg" width="250px"/>
</div>


## 贡献

本人正在尝试一些更加完善的抽象模式，支持更多的物联网协议和数据存储形式，如果你有更好的建议，欢迎一起讨论交流。