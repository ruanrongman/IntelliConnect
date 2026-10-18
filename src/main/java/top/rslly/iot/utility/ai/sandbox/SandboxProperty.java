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
package top.rslly.iot.utility.ai.sandbox;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * OpenSandbox 远程沙箱配置.
 *
 * <p>
 * 对应 application.yaml 中 {@code ai.sandbox.*} 配置项。
 */
@Configuration
@Data
@ConfigurationProperties(prefix = "ai.sandbox")
public class SandboxProperty {
  /** 是否启用沙箱服务 */
  private boolean enabled = false;
  /** OpenSandbox Server 地址(不带 http:// 前缀, 协议由 protocol 指定) */
  private String domain;
  /** 连接协议, 默认 http */
  private String protocol = "http";
  /** API Key, server 未启用 api_key 时可任意填写 */
  private String apiKey;
  /**
   * 是否经 OpenSandbox Server 代理转发全部流量(而非直连每个沙箱随机分配的代理端口). 客户端与沙箱代理端口段不通(容器/集群内部署未发布端口段)时置为 true.
   */
  private boolean useServerProxy = false;
  /**
   * 是否跳过创建后的健康检查(ping 沙箱内部服务). 健康检查对 server 到沙箱的连通性敏感, 容器冷启动慢或代理路由异常时会误判创建失败并销毁沙箱;
   * 跳过后就绪性由首条命令的失败重试兜底.
   */
  private boolean skipHealthCheck = false;
  /** 沙箱镜像 */
  private String image;
  /** 创建沙箱等待容器就绪的超时时间, 默认太短需显式加长 */
  private Duration requestTimeout = Duration.ofSeconds(300);
  /** 单条命令执行超时时间 */
  private Duration commandTimeout = Duration.ofSeconds(120);
  /** 沙箱空闲多久后自动回收, 保留文件系统状态在此时间内有效 */
  private Duration idleTimeout = Duration.ofMinutes(10);
  /** 同时存活的沙箱实例上限, 超限时回收最久未使用的实例(按 chatId 隔离, 每个会话独占一个实例) */
  private int maxInstances = 8;
  /** 是否启用 SandboxPool 预热池: 后台预创建空闲实例缓冲, 新会话秒级取用而非现场冷创建 */
  private boolean poolEnabled = true;
  /** 预热池空闲实例上限(常驻缓冲的远端容器数, 不计入 max-instances) */
  private int poolMaxIdle = 2;
}
