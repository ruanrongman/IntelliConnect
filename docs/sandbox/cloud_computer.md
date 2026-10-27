# 云电脑（OpenSandbox 沙箱）

## 什么是云电脑

云电脑是 IntelliConnect 为智能体提供的远程隔离执行环境，基于开源项目 [OpenSandbox](https://github.com/opensandbox-group/OpenSandbox) 实现，配合会话工作区保存输入文件和任务产物。智能体可以读取上传文件，在云端沙箱中执行 shell 命令、运行代码和加工文件，再将结果保存回工作区供用户预览或下载。

平台暴露给模型的工具：

- `bashTool`：在远程 Ubuntu 24.04 沙箱中执行任意 shell 命令，返回 stdout / stderr / 退出码
- `editTool`：在沙箱中读写文件（write / read / list / delete），相对路径自动落在 `/workspace` 临时工作目录下
- `imageTool`：读取当前会话沙箱中的图片，调用多模态视觉模型检查截图、图表或渲染结果，返回文字观察
- `workspaceTool`：管理当前会话的持久文件，支持列表、读取文本、保存文本，以及在工作区与沙箱之间导入、导出文件；列表和文本读写不依赖沙箱

关键特性：

- **会话隔离**：工作区按产品和稳定会话 ID 隔离，有产品权限的用户可管理该产品的会话文件；每个会话（chatId）独占一个沙箱实例
- **持久文件**：聊天附件原件、智能体直接保存的文本和显式导出的沙箱产物保存在后端磁盘，删除对话、清空记忆或回收沙箱后仍保留
- **临时执行状态**：同一沙箱实例内文件系统状态跨调用保留，空闲超过 `idle-timeout` 自动回收；需要保留的产物应在回收前导出到工作区
- **预热池**：后台预创建空闲实例，新会话秒级取用，失败自动回退冷创建
- **容量控制**：全局实例上限 `max-instances`，超限回收最久未用的实例

路由层面，Agent 路由会描述 `workspaceTool` 的文件管理能力；沙箱可用时还会声明云电脑执行能力。即使关闭沙箱，智能体仍可列出、读取和保存工作区文本文件。

### 文件保存在哪里

| 存储位置 | 用途 | 生命周期 |
| --- | --- | --- |
| 会话工作区（后端 `ai.workspace.root`） | 保存上传原件、文本和导出的产物，支持预览、下载、删除 | 独立于对话记忆和沙箱实例，需在工作区手动删除文件 |
| 沙箱 `/workspace` | 运行代码、安装依赖、处理导入的文件和生成中间结果 | 仅随当前沙箱实例保留，回收或重建后丢失 |

两处通过 `workspaceTool` 显式复制文件，不会自动挂载或同步。上传到工作区的文件需要导入后才能由 `bashTool`、`editTool` 处理；沙箱内的新产物需要导出才能持久保存。

### 视觉检查

先通过 `bashTool` 生成图片或保存页面截图，再调用 `imageTool`，工具输入为 JSON 字符串：

```json
{"path":"screenshots/page.png","prompt":"检查标题是否被裁切、文字是否重叠、按钮是否对齐，并说明问题位置。"}
```

`path` 支持 `/workspace` 下的绝对路径或相对路径，`prompt` 可省略，默认进行内容与排版检查。工具支持 PNG、JPEG、WebP、GIF，单张图片最多 4 MiB；SVG、PDF 需先渲染为 PNG。工作区附件需先通过 `workspaceTool` 的 `import_to_sandbox` 导入。工具检查已有图片，不会自动截图；修复代码后应重新截图再检查。

模型默认复用 `ai.vision-model`，也可在产品模型配置中选择“图像视觉检查工具”（`toolsId=image`），指定支持图片输入的模型和提供商。Agent 与 McpAgent 的函数参数统一使用 `args:string`，其中字符串内容为上述 JSON。返回结果为视觉模型的文字观察，不会将 Base64 图片写入对话历史。

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

以下步骤直接基于原始 `code-interpreter:v1.1.0` 镜像，为系统 Python 和 OpenSandbox 自带的 Python 补装 pip，配置阿里云 PyPI 镜像，并保留 OpenSandbox 原始入口配置。请在沙箱宿主机的 Bash 中执行：

```bash
set -eu

mkdir -p ~/opensandbox-final
cd ~/opensandbox-final

cat > Dockerfile <<'EOF'
# 直接从原始 OpenSandbox 镜像构建
FROM sandbox-registry.cn-zhangjiakou.cr.aliyuncs.com/opensandbox/code-interpreter:v1.1.0

USER root

# 1. 配置阿里云 PyPI 镜像
RUN mkdir -p /etc && \
    printf '%s\n' \
      '[global]' \
      'index-url = https://mirrors.aliyun.com/pypi/simple/' \
      'timeout = 60' \
      'retries = 5' \
      'disable-pip-version-check = true' \
      'break-system-packages = true' \
      > /etc/pip.conf

# 2. 为系统 Python 和 OpenSandbox 自带的 Python 安装 pip
# --break-system-packages 只用于构建阶段的 pip 安装；
# 普通 pip 命令的默认行为由 /etc/pip.conf 控制。
RUN set -eux; \
    command -v uv; \
    for py in \
      /usr/bin/python3 \
      /opt/python/versions/cpython-*/bin/python3; do \
      if [ -x "$py" ]; then \
        echo "===== Configure pip for $py ====="; \
        if ! "$py" -m pip --version >/dev/null 2>&1; then \
          uv pip install \
            --break-system-packages \
            --index-url https://mirrors.aliyun.com/pypi/simple/ \
            --python "$py" pip; \
        fi; \
        "$py" -m pip --version; \
      fi; \
    done

# 3. 确保 pip / pip3 命令存在
# 使用当前 PATH 中的 python3，以兼容 OpenSandbox 的 Python 版本切换。
RUN printf '%s\n' \
      '#!/bin/sh' \
      'exec python3 -m pip "$@"' \
      > /usr/local/bin/pip && \
    chmod +x /usr/local/bin/pip && \
    ln -sf /usr/local/bin/pip /usr/local/bin/pip3

# 4. 验证基础 Python 的 pip
RUN /usr/bin/python3 -m pip --version

# 5. 保留 OpenSandbox 原始入口配置
EOF

# 构建最终镜像
docker build --no-cache \
  -t my-code-interpreter:v1.1.0-final \
  .

# 查看镜像
docker image inspect my-code-interpreter:v1.1.0-final \
  --format 'Image={{.RepoTags}}'
```

构建后可验证 Python 3.12 环境中的 pip：

```bash
docker run --rm \
  --entrypoint /bin/bash \
  my-code-interpreter:v1.1.0-final \
  -c 'source /opt/code-interpreter/code-interpreter-env.sh python 3.12 && python3 --version && pip --version'
```

> 如需预装其他运行时或依赖，可在 Dockerfile 中继续追加 `RUN` 层，构建自己的镜像版本。

## 4. 平台配置

### 4.1 沙箱配置

在 `application.yaml` 中配置 `ai.sandbox.*`：

```yaml
ai:
  sandbox:
    enabled: true            # true 启用; 沙箱宿主机需预拉取 image
    domain: 192.168.49.129:8080   # OpenSandbox Server 地址(不带 http:// 前缀)
    protocol: http
    api-key: ""          # server 未启用 api_key 时可任意填写
    use-server-proxy: true   # 客户端与沙箱随机代理端口段不通时置 true, 流量全部经 server(8080) 转发
    skip-health-check: false # 是否跳过创建后的 ping 健康检查
    image: my-code-interpreter:v1.1.0-final   # 使用上文制作的自定义镜像
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
| `idle-timeout` | 沙箱空闲自动回收时间，只影响临时执行状态，不影响持久工作区文件 | `10m` |
| `max-instances` | 同时存活的沙箱实例上限，超限回收最久未使用实例 | `8` |
| `pool-enabled` | 是否启用预热池 | `true` |
| `pool-max-idle` | 预热池空闲实例上限 | `2` |

### 4.2 工作区配置与存储

将以下配置合并到同一个 `ai` 节点；未显式配置时使用这些默认值：

```yaml
ai:
  workspace:
    root: data/workspaces                 # 后端文件存储目录，相对路径基于后端进程工作目录
    max-file-bytes: 52428800              # 单文件 50 MiB
    max-workspace-bytes: 524288000        # 每个产品会话工作区 500 MiB
    max-read-chars: 16000                 # 单次文本读取上限，按 UTF-16 字符计数
```

工作区元数据存储在 MySQL 的 `agent_workspace`、`agent_workspace_file` 表。升级部署前，在应用数据库中执行 [工作区建表脚本](../sql/20261020_agent_workspace.sql)，该脚本不改变现有对话数据。若部署环境使用 `spring.jpa.hibernate.ddl-auto: none` 或 `validate`，需先完成建表。

生产部署应将 `ai.workspace.root` 指向持久磁盘或映射到后端容器的持久化卷，同时备份该目录和数据库。该目录位于 IntelliConnect 后端，不是 OpenSandbox 容器的 `/workspace`。目录及其父目录不能是符号链接；修改根路径前需停机迁移已有文件。当前实现按单后端实例运行，多实例部署需要共享存储和跨实例并发控制。

上传还受 `spring.servlet.multipart.max-file-size`、`max-request-size` 及反向代理请求大小限制；当前仓库中的两个 multipart 上限均为 `200MB`，单文件仍受工作区的 50 MiB 默认限制。更多部署和接口细节见 [会话工作区文档](../ai_chat/workspace.md)。

## 5. 使用会话工作区

### 页面入口与上传

在“产品页面 → 会话工作区”选择产品和会话，即可上传、预览文本、下载或删除文件，并查看已用容量。智能体对话调试页的输入框下方也有“工作区”按钮，可直接管理当前会话文件。已删除对话的工作区仍可从独立工作区页面访问。

聊天附件通过原有内容校验后，会自动将原件保存到当前会话工作区，并将文件 ID 和名称提供给模型。也可以在工作区页面直接上传文件，再在同一会话中要求智能体处理。重复上传同名文件会生成独立文件，不覆盖原件。

文本预览支持 UTF-8，并以普通文本显示；HTML、JavaScript 不会在预览中执行。PDF、办公文档、图片等二进制文件可下载，或导入沙箱后用相应工具处理。

### 智能体如何使用文件

`workspaceTool` 每次执行一个操作，产品和会话由服务端上下文确定，调用参数无需也不能指定 `productId`、`chatId`：

| 操作 | 参数示例 | 用途 |
| --- | --- | --- |
| `list` | `{"action":"list","pageNum":1,"pageSize":20}` | 查找上传文件和已有产物，获取真实文件 ID |
| `read` | `{"action":"read","fileId":"实际文件ID","offset":0,"limit":8000}` | 读取 UTF-8 文本，分段继续读取时使用返回的 `nextOffset` |
| `write` | `{"action":"write","name":"报告.md","content":"报告正文"}` | 直接创建持久文本文件，无需启动沙箱 |
| `import_to_sandbox` | `{"action":"import_to_sandbox","fileId":"实际文件ID"}` | 将文件复制到沙箱，返回 `sandboxPath` |
| `export_from_sandbox` | `{"action":"export_from_sandbox","path":"results/报告.pdf"}` | 将沙箱产物保存到持久工作区，供页面下载 |

例如，用户上传表格并要求生成分析报告时，智能体先用 `list` 找到文件，再用 `import_to_sandbox` 导入；根据返回的路径，通过 `bashTool` 运行分析，将报告生成到 `/workspace/results/报告.pdf`，最后用 `export_from_sandbox` 保存。用户在工作区刷新后即可下载报告。如果只是生成 Markdown、HTML 或代码文本，可以直接使用 `write` 保存。

导入路径为 `/workspace/imports/<fileId>/<原文件名>`；同一实例内重复导入到相同路径会失败，以免覆盖已加工内容。导出仅支持 `/workspace` 下的普通文件，可传相对路径或 `/workspace/` 开头的绝对路径，拒绝路径穿越和符号链接。导入、导出都需要沙箱可用，且受文件大小和工作区配额限制。

交付沙箱产物前应完成导出并确认成功。导出是文件副本，之后修改沙箱文件不会自动更新工作区；再次导出会创建新的文件记录。沙箱重建后，可重新导入已保存的输入文件或产物继续处理，尚未导出的中间结果无法恢复。

## 6. 常见问题

- **同一对话的文件保留**：bash、文件编辑和技能解压共用稳定的对话 ID，流式请求 ID 改变不会切换沙箱。每次操作前刷新远端有效期，冷创建和预热池使用相同策略；`request-timeout` 只控制请求和就绪等待，不再作为取出沙箱后的存活时长。空闲时间从操作结束计算，超过 `idle-timeout`、容量驱逐或后端重启仍会回收沙箱及其中的临时文件；已上传、直接写入或导出到工作区的文件继续保留。
- **操作异常与重建**：文件不存在、命令错误或请求超时会返回失败并保留原沙箱，不会因此删除其他文件，也不会自动重复执行命令。普通沙箱操作只有在 SDK 明确确认沙箱不存在时才重建并重试一次，命令结果会提示旧实例文件已丢失。新实例不会自动恢复旧实例文件，可从持久工作区重新导入。导出只读取已有实例，实例丢失或过期时直接失败，不创建空实例替代。
- **上传后 bash 找不到文件**：上传保存到持久工作区，需要先通过 `workspaceTool` 的 `import_to_sandbox` 获取沙箱路径，再交给 bash 或文件编辑工具使用。
- **生成了文件但工作区列表为空**：`bashTool`、`editTool` 生成的文件仍在临时沙箱中，需用 `export_from_sandbox` 显式保存；仅有沙箱路径不代表已经持久保存。
- **删除对话或清空记忆后文件还在**：这是工作区的保留策略，可在“会话工作区”页面手动删除不再需要的文件，释放配额。
- **工作区存储不可用或上传失败**：检查工作区表是否已创建、后端存储目录是否可写，以及单文件、工作区总容量、multipart 和代理请求大小限制。
- **并发调用与技能包**：同一对话只允许一个创建者；运行中的实例不会被空闲清扫或容量驱逐。失效实例停止接收新租约，已有操作结束并完成回收后才允许重建。技能检查、上传、解压、标记和目录查询使用同一实例的租约；实例重建后会重新上传技能包。
- **维护调度与关闭**：空闲回收和失败回收重试使用独立的 `sandboxTaskScheduler`，阻塞的远程请求不会占用默认调度器的心跳线程。应用关闭后拒绝新操作，正在执行的操作在最后一次租约释放时回收实例，关闭期间创建完成的实例也会被回收。
- **首次创建沙箱超时**：确认宿主机已预拉取镜像（见第 2 步），现场 pull 容易导致 SDK 读超时
- **客户端连不上沙箱代理端口**：容器/集群内部署未发布随机代理端口段时，将 `use-server-proxy` 置为 `true`，全部流量经 server 8080 端口转发
- **健康检查误判创建失败**：容器冷启动慢或代理路由异常时会误判，可置 `skip-health-check: true`；首条命令的普通连接错误会保留实例并返回失败，可稍后重试
- **模型不调用 bash/edit**：确认 `enabled: true`，路由描述中"云电脑可用"的声明仅在沙箱可用时注入
- **kill 失败的容器残留**：应用会自动延后重试回收；超过 30 分钟仍失败会在日志中输出沙箱 ID，需在宿主机手动清理
