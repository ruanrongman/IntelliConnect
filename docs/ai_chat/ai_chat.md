## AI文本调试

AI文本调试功能提供了一个基于SSE流式传输的文本交互界面，用于实时调试和测试Agent智能体的对话能力，无需语音设备即可验证智能体的工具调用、知识检索等功能。

## 使用方式

### 通过智控台

在智控台侧边栏中选择"AI调试"即可进入文本调试界面，选择产品后即可开始对话。

### 通过API

#### 1. 发起流式对话

```bash
curl -X POST "http://localhost:8080/api/v2/aiControl/stream" \
-H "Authorization: Bearer {token}" \
-H "Content-Type: application/json" \
-H "Accept: text/event-stream" \
-d '{"productId":0,"message":"你好","chatId":"test-chat-1"}'
```

流式响应使用以下 SSE 事件：

| 事件 | 数据说明 |
|------|----------|
| `message` | 最终回答的正文增量 |
| `reasoning` | 供应商返回的原生 `reasoning_content` 增量 |
| `complete` | 完整的最终回答正文 |
| `error` | 流处理错误信息 |

连接建立后立即发送 SSE 注释，并每 10 秒发送 `:heartbeat` 保活；这些注释不进入聊天正文。只有收到 `complete` 才视为成功结束，收到 `error` 或连接提前关闭时显示失败并保留已接收内容，不自动重新提交任务。

`reasoning` 与 `message` 相互独立，只在模型返回原生思考内容时产生。调试面板将思考过程默认折叠展示；
停止或失败时保留本次页面已接收的部分内容，但思考过程不拼接到最终回答、不包含在复制内容中，也不写入历史消息。
刷新页面或重新进入对话后不会恢复思考过程。

#### 2. 停止对话

```bash
curl -X POST "http://localhost:8080/api/v2/aiControl/stream/stop" \
-H "Authorization: Bearer {token}" \
-H "Content-Type: application/json" \
-d '{"chatId":"test-chat-1"}'
```

#### 3. 查询历史消息

```bash
curl -X GET "http://localhost:8080/api/v2/historyMessage?chatId=chatProduct1&pageNum=1&pageSize=20&includeToolEvents=true" \
-H "Authorization: Bearer {token}"
```

`includeToolEvents` 默认 `false`，供聊天页面只加载用户及最终助手消息。历史管理弹窗默认传 `true`，在权限范围内分页展示全部事件，包括 `assistant_tool_call` 和 `tool_result`，并显示请求 ID、顺序及调用 ID，便于对应参数和执行结果。该开关只控制查询展示，不补造过去未保存的工具事件。

#### 4. 删除历史消息

```bash
curl -X DELETE "http://localhost:8080/api/v2/historyMessage?chatId=test-chat-1" \
-H "Authorization: Bearer {token}"
```

## 功能特点

- **SSE流式传输**: 分别实时获取AI回复正文和模型原生思考过程
- **思考过程折叠展示**: 原生思考内容默认折叠，且仅在当前调试流中保留
- **对话历史持久化**: 用户消息和AI最终回答自动保存到数据库，支持分页查询
- **按用户权限隔离**: 历史消息按用户权限过滤，确保数据安全
- **产品隔离**: 每个产品的对话独立管理
- **主动停止**: 支持随时中断正在进行的流式对话

## Agent模式配置

文本调试的Agent行为受 `application.yaml` 中以下配置影响：

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `ai.agent.mode` | Agent模式：`react`（ReAct提示词）或 `function`（原生函数调用） | `function` |
| `ai.agent.include-thought` | 是否在输出中包含AI推理过程 | `false` |
| `ai.agent-llm` | Agent使用的大模型 | `dashscope-deepseek-v4-flash` |

Agent 的沙箱工具名为 `bashTool`、`editTool`。`bashTool` 的原生函数参数为 `{"command":"完整 shell 命令"}`；ReAct 模式使用 `{"action":{"name":"bashTool","args":{"command":"完整 shell 命令"}}}`。Agent 在派发时提取 `command`，BashTool 仅执行命令字符串。

函数模式中，未知工具名会以配对的工具错误返回模型，由模型在现有步数上限内纠正。仅适配器不支持函数调用或首轮明确返回 `UNSUPPORTED` 时切换 ReAct；调用失败、超时或中途断流直接报错，不通过切换模式重新执行任务。日志中的未知工具错误不代表模型缺少函数调用能力。

## 数据存储

对话历史消息存储在 `history_message` 表中，包含以下字段：

| 字段 | 说明 |
|------|------|
| chat_id | 对话ID |
| request_id | 请求ID |
| sequence_num | 消息序号 |
| message_type | 消息类型（user/assistant/tool等） |
| content | 消息内容 |
| time | 消息时间 |

函数路由执行一次后直接使用现有 `RouteExecutionResult.answer()` 作为最终答复，不增加 Router 循环，也不在执行后调用模型改写结果。函数模式的记忆摘要在路由前处理此前的历史。

实际执行函数路由时，同一 `request_id` 按 `sequence_num` 保存四条事件：`user`（原始输入）、`assistant_tool_call`（`FunctionToolCallMessage` JSON）、`tool_result`（`FunctionToolResultMessage` JSON）、`assistant`（用户可见的最终答复）。调用与结果的 `toolCallId` 相同；原始工具结果与包含前缀、音乐 URL 等内容的最终答复分别保存。调用携带的 assistant 内容及 `reasoningContent` 保存在协议 JSON 中，用于还原模型上下文；不会单独显示为聊天消息。

DIRECT_REPLY、未执行调用的错误或 legacy fallback 仍保存普通 user/assistant 两条记录。执行异常会保存真实错误结果；写历史或 Redis 失败不会重新执行路由。中断留下的不完整工具配对不会进入后续模型上下文，旧 user/assistant 内容继续按普通文本恢复，无需清库。历史窗口按完整 request_id 回合裁剪；普通 Agent/McpAgent 只带入用户消息和最终答复。

历史列表接口在分页前仅选择 `user` 与 `assistant`，按 `time`、`sequence_num` 排序，内部协议事件只保留在持久化历史和 Router 上下文中。

2026-10-22 已将当前配置数据库的 `history_message.content` 从 `VARCHAR(6144)` 直接扩容为 `LONGTEXT`，变更前后均为 1502 条历史记录。应用已移除 6144 字符静默截断。[扩容 SQL](../sql/20261022_router_history_content.sql) 留作其他部署环境更新参考。
