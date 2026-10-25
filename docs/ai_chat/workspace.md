# 会话工作区

工作区按产品和稳定对话 ID 隔离，有产品权限的用户共享该产品对应的会话文件。
上传原文件、智能体生成的文本和显式导出的沙箱产物长期保存在后端磁盘；删除对话、
清空记忆或沙箱回收不会删除文件。文件须在“产品页面 → 会话工作区”中手动删除。

## 部署

启动新版后端前，在应用使用的 MySQL 数据库中执行
[`20261020_agent_workspace.sql`](../sql/20261020_agent_workspace.sql)。若部署环境使用
`ddl-auto: none` 或 `validate`，需先完成建表；当前仓库配置为 `update`。该 SQL 不改变现有对话数据。

默认配置如下，可合并到现有 `application.yaml` 的 `ai` 节点：

```yaml
ai:
  workspace:
    root: data/workspaces
    max-file-bytes: 52428800
    max-workspace-bytes: 524288000
    max-read-chars: 16000
```

也可通过 `AI_WORKSPACE_ROOT`、`AI_WORKSPACE_MAX_FILE_BYTES`、
`AI_WORKSPACE_MAX_WORKSPACE_BYTES`、`AI_WORKSPACE_MAX_READ_CHARS` 环境变量配置。
文件目录为 `<root>/<productId>/<SHA-256(chatId)>/<fileId>`，原文件名只存入数据库，
同名上传创建独立文件。上传批次同时受现有 Spring multipart 请求上限约束（当前 200MB）。

生产部署必须将根目录映射至持久化卷，并同时备份数据库和该目录。目录及其父目录不能是符号链接。
首版按单后端实例运行，使用进程内分段锁保障配额；多实例需要共享存储和跨实例并发控制。
空工作区记录会保留。更改根路径时，必须停机迁移原文件，不能只更改配置。

## 页面与聊天附件

工作区页面支持按产品、会话查看文件，上传、下载、UTF-8 文本分段预览及删除。
已删除对话的工作区继续列出。智能体对话调试页在输入框下方提供“工作区”入口，点击后弹窗
管理当前会话文件，支持上传、预览、下载及删除，无需跳转页面，也不会清空输入草稿或中断生成中的回复。

聊天附件通过原有内容校验后自动保存原件，文件 ID 和名称写入模型上下文与历史记录。
原有办公文档文字提取保持可用。保存失败会终止本次请求并返回错误；已保存文件不会因后续模型失败而删除。
重新发送同一附件会创建新文件；不会覆盖原文件。

二进制文件和非 UTF-8 文件可上传、下载，并通过沙箱处理；文本预览不执行 HTML 或 JavaScript。
文件内容为不可信数据，不能作为系统指令执行。

## workspaceTool

工具在 Agent、McpAgent 的 ReAct 和 Function Calling 模式均可使用。示例参数：

```json
{"action":"list","pageNum":1,"pageSize":20}
{"action":"read","fileId":"...","offset":0,"limit":8000}
{"action":"write","name":"报告.md","content":"报告正文"}
{"action":"import_to_sandbox","fileId":"..."}
{"action":"export_from_sandbox","path":"results/报告.pdf"}
```

工作区由服务器上下文中的 productId 和稳定 conversationChatId 确定，工具禁止自选归属。
默认会话、调试会话和设备会话沿用现有会话校验；微信会话按数据库中精确的 appid+openid 产品绑定校验。
没有稳定会话上下文的调用失败，不使用共享 default 工作区。文本 offset 使用 UTF-16 字符计数，
响应包含 nextOffset 和 truncated。

导入返回 `/workspace/imports/<fileId>/<原文件名>`，可交给 bashTool 或 editTool 使用。
重复导入到相同目标会失败，不覆盖已经加工的文件。导出支持相对 `/workspace` 的路径或以
`/workspace/` 开头的绝对路径；拒绝路径穿越、其他绝对路径、符号链接及非普通文件。
导入和导出均在现有沙箱租约内完成，SDK 流在释放租约前关闭；二进制数据不经过模型。
传输使用沙箱预装的 Python 3，以不跟随符号链接的目录描述符访问文件。

沙箱关闭时，工作区列表和文本读写仍可使用。沙箱实例已经丢失或过期时，导出失败，不创建空沙箱替代。
因此智能体应在交付前显式导出产物。远程 MCP 服务本地路径的文件提取不在首版范围内。

`/workspace` 是沙箱内的临时工作目录，文件仅在该沙箱实例存活期间保留；沙箱关闭、回收或过期会丢失。
工作区与沙箱之间通过显式导入、导出复制文件，不自动挂载或同步。沙箱部署、镜像构建及完整文件处理流程见
[云电脑文档](../sandbox/cloud_computer.md)。
Router 的普通模式及 Function Calling 模式都在 Agent 路由中描述 `workspaceTool`，有 MCP 路由时也提供该描述。
工作区文本读写不依赖沙箱或远程 MCP 服务。执行智能体会接收原始会话历史：例如助手提出“打开 part1.txt”，
用户回复“好的”后应继续读取并展示内容。明确取消、结束对话或完成任务后的普通确认不会触发额外操作。

## HTTP API

所有接口位于 `/api/v2/workspaces`，需要 Authorization，且每次验证产品权限。
文件操作同时携带 productId、chatId，后端核对 fileId 所属工作区。

| 方法与路径 | 参数/用途 |
| --- | --- |
| GET / | productId、pageNum、pageSize；已保存工作区列表 |
| GET /files | productId、chatId、pageNum、pageSize；文件分页及容量 |
| POST /files | multipart 的 productId、chatId 和一个或多个 file |
| GET /files/{fileId}/text | productId、chatId、offset、limit；分段文本 |
| GET /files/{fileId}/download | productId、chatId；附件流 |
| DELETE /files/{fileId} | productId、chatId；删除单个文件 |

分页从 1 开始，pageSize 上限 100。普通响应使用 JsonResult；下载通过鉴权接口流式返回，
不公开静态文件目录。权限失败返回 403，非法参数或文件读取失败返回 400。

写入采用独立临时文件和同目录原子移动，数据库事务提交失败时清理文件；批量上传整批回滚。
删除先暂存文件，数据库提交失败则恢复。异常断电可能留下不可见临时文件，运维可在停机、备份后清理
`.upload-*`、`.delete-*` 以及未关联数据库的文件；不能仅备份磁盘而丢弃元数据。
