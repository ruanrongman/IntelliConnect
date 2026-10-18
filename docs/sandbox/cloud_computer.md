# 云电脑（OpenSandbox 沙箱）

## 什么是云电脑

云电脑是 IntelliConnect 为智能体提供的远程隔离执行环境，基于开源项目 [OpenSandbox](https://github.com/opensandbox-group/OpenSandbox) 实现。启用后，智能体可以在云端沙箱中执行 shell 命令、读写文件，完成代码运行、数据处理、文件加工等复杂多步任务。

平台暴露给模型的工具：

- `bashTool`：在远程 Ubuntu 24.04 沙箱中执行任意 shell 命令，返回 stdout / stderr / 退出码
- `editTool`：在沙箱中读写文件（write / read / list / delete），相对路径自动落在 `/workspace` 工作目录下

关键特性：

- **会话隔离**：每个会话（chatId）独占一个沙箱实例，互相看不到对方的文件系统与执行状态
- **状态保留**：同一会话内文件系统状态跨调用保留，空闲超过 `idle-timeout` 自动回收
- **预热池**：后台预创建空闲实例，新会话秒级取用，失败自动回退冷创建
- **容量控制**：全局实例上限 `max-instances`，超限回收最久未用的实例

路由层面，沙箱可用时 `route_agent`（复杂多步任务）的描述会声明"云电脑可用，bash 和 edit 是其工具且拥有全部权限"，引导模型把需要计算、代码执行、文件操作的任务路由到 agent。

## 1. 安装 OpenSandbox Server

OpenSandbox Server 部署在沙箱宿主机上（可以是独立的服务器，也可以是本机）。安装要求：

- Docker（本地运行必需）
- Python 3.10+ 与 [uv](https://docs.astral.sh/uv/)

```bash
# 初始化示例配置（docker 模式）
uvx opensandbox-server init-config ~/.sandbox.toml --example docker

# 启动 Server（默认监听 8080 端口）
uvx opensandbox-server

# 查看帮助
# uvx opensandbox-server -h
```

更多部署细节（含 Kubernetes 部署）请参考[官方文档](https://github.com/opensandbox-group/OpenSandbox)。

## 2. 拉取基础镜像

沙箱宿主机需要**预拉取**镜像，避免首次创建沙箱时现场 pull 导致 SDK 读超时：

```bash
docker pull sandbox-registry.cn-zhangjiakou.cr.aliyuncs.com/opensandbox/code-interpreter:v1.1.0
```

## 3. 制作自带 pip 的自定义镜像（推荐）

官方 `code-interpreter:v1.1.0` 镜像内的 Python 3.12 **没有 pip**。以下步骤基于官方镜像制作一个补装了 pip 的自定义镜像：

```bash
# 1. 创建构建目录
mkdir -p opensandbox-with-pip
cd opensandbox-with-pip

# 2. 拉取原始镜像
docker pull sandbox-registry.cn-zhangjiakou.cr.aliyuncs.com/opensandbox/code-interpreter:v1.1.0

# 3. 创建 Dockerfile
cat > Dockerfile <<'EOF'
FROM sandbox-registry.cn-zhangjiakou.cr.aliyuncs.com/opensandbox/code-interpreter:v1.1.0

USER root

# 给所有已安装的 Python 版本补装 pip
RUN set -eu; \
    for py in /opt/python/versions/cpython-*/bin/python3; do \
        [ -x "$py" ] || continue; \
        if ! "$py" -m pip --version >/dev/null 2>&1; then \
            "$py" -m ensurepip --upgrade \
                || uv pip install --python "$py" pip; \
        fi; \
        "$py" -m pip --version; \
    done

# pip 始终跟随当前 PATH 中的 python3
RUN printf '%s\n' '#!/bin/sh' 'exec python3 -m pip "$@"' \
        > /usr/local/bin/pip \
    && chmod +x /usr/local/bin/pip \
    && ln -sf /usr/local/bin/pip /usr/local/bin/pip3
EOF

# 4. 构建新镜像
docker build -t my-code-interpreter:v1.1.0-pip .

# 5. 验证 Python 3.12 环境中的 pip
docker run --rm \
  --entrypoint /bin/bash \
  my-code-interpreter:v1.1.0-pip \
  -c 'source /opt/code-interpreter/code-interpreter-env.sh python 3.12 && python3 --version && pip --version'
```

> 如需预装其他运行时或依赖，可在 Dockerfile 中继续追加 `RUN` 层，构建自己的镜像版本。

## 4. 平台配置

在 `application.yaml` 中配置 `ai.sandbox.*`：

```yaml
ai:
  sandbox:
    enabled: true            # true 启用; 沙箱宿主机需预拉取 image
    domain: 192.168.49.129:8080   # OpenSandbox Server 地址(不带 http:// 前缀)
    protocol: http
    api-key: ""          # server 未启用 api_key 时可任意填写
    use-server-proxy: true   # 客户端与沙箱随机代理端口段不通时置 true, 流量全部经 server(8080) 转发
    skip-health-check: false # 跳过创建后的 ping 健康检查, 就绪性由首条命令失败重试兜底
    image: my-code-interpreter:v1.1.0-pip   # 使用上文制作的自定义镜像
    request-timeout: 300s    # 创建沙箱等待容器就绪的超时
    command-timeout: 120s    # 单条命令执行超时
    idle-timeout: 10m        # 空闲多久后自动回收沙箱
    max-instances: 8         # 同时存活的沙箱实例上限(每个会话独占一个), 超限回收最久未用
    pool-enabled: true       # 启用 SandboxPool 预热池: 新会话秒级取用, 失败自动回退冷创建
    pool-max-idle: 2         # 预热池空闲实例上限(常驻缓冲容器数, 不计入 max-instances)
```

**配置项说明**：

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `enabled` | 是否启用沙箱服务 | `false` |
| `domain` | OpenSandbox Server 地址（不带协议前缀，协议由 `protocol` 指定） | - |
| `protocol` | 连接协议 | `http` |
| `api-key` | API Key，server 未启用 api_key 时可任意填写 | - |
| `use-server-proxy` | 客户端与沙箱随机代理端口段不通（容器/集群内部署未发布端口段）时置 `true`，流量全部经 server 转发 | `false` |
| `skip-health-check` | 跳过创建后的 ping 健康检查（对 server 到沙箱连通性敏感，易误判） | `false` |
| `image` | 沙箱镜像 | - |
| `request-timeout` | 创建沙箱等待容器就绪的超时 | `300s` |
| `command-timeout` | 单条命令执行超时 | `120s` |
| `idle-timeout` | 空闲自动回收时间，文件系统状态在此时间内保留 | `10m` |
| `max-instances` | 同时存活的沙箱实例上限，超限回收最久未使用实例 | `8` |
| `pool-enabled` | 是否启用预热池 | `true` |
| `pool-max-idle` | 预热池空闲实例上限 | `2` |

## 5. 常见问题

- **首次创建沙箱超时**：确认宿主机已预拉取镜像（见第 2 步），现场 pull 容易导致 SDK 读超时
- **客户端连不上沙箱代理端口**：容器/集群内部署未发布随机代理端口段时，将 `use-server-proxy` 置为 `true`，全部流量经 server 8080 端口转发
- **健康检查误判创建失败**：容器冷启动慢或代理路由异常时会误判，可置 `skip-health-check: true`，就绪性由首条命令失败重试兜底
- **模型不调用 bash/edit**：确认 `enabled: true`，路由描述中"云电脑可用"的声明仅在沙箱可用时注入
- **kill 失败的容器残留**：应用会自动延后重试回收；超过 30 分钟仍失败会在日志中输出沙箱 ID，需在宿主机手动清理