# IntelliConnect

> 一个智慧的物联网平台内核

> 本项目正在快速开发中，文档正不断更新，欢迎提issue或email提醒作者。

## 概述
* 本项目基于springboot2.7开发，使用spring security作为安全框架
* 配备物模型和完善的监控模块
* 支持多种大模型和先进的Agent智能体技术提供出色的AI智能，可以快速搭建智能物联网应用
* 支持多种iot协议，使用emqx exhook作为mqtt通讯，可扩展性强
* 支持OTA空中升级技术
* 支持微信小程序和微信服务号
* 使用常见的mysql和redis数据库，上手简单
* 支持时序数据库influxdb

## 安装运行
* 安装mysql和redis数据库，高性能运行推荐安装时序数据库influxdb
* 安装EMQX集群,并配置好exhook，本项目使用exhook作为mqtt消息的处理器
* 安装java17环境
* 修改配置文件application.yaml
* java -jar IntelliConnect-1.8-SNAPSHOT.jar

## 项目特色
* 极简主义，层次分明，符合mvc分层结构
* 完善的物模型抽象，使得iot开发者可以专注于业务本身
* AI能力丰富，支持Agent智能体技术，快速开发AI智能应用

## 接入微信服务号使用效果

<video  controls="controls" width="500" height="200">
<source src="../video/demo.mp4" type="video/mp4">
</video>

## Agent语音交互效果

<video  controls="controls" width="500" height="200">
<source src="../video/demo2.mp4" type="video/mp4">
</video>

## 交流群

欢迎加入群聊一起交流讨论有关Aiot相关的话题，链接过期了可以issue或email提醒一下作者。

<div style="width: 250px;margin: 0 auto;">
    <img src="./images/7c89ca5804c4a6ca5ac1c3a2ac5e031.jpg" width="250px"/>
</div>


## 贡献

本人正在尝试一些更加完善的抽象模式，支持更多的物联网协议和数据存储形式以及Agent智能体技术，如果你有更好的建议，欢迎一起讨论交流。