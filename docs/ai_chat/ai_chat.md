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

#### 2. 停止对话

```bash
curl -X POST "http://localhost:8080/api/v2/aiControl/stream/stop" \
-H "Authorization: Bearer {token}" \
-H "Content-Type: application/json" \
-d '{"chatId":"test-chat-1"}'
```

#### 3. 查询历史消息

```bash
curl -X GET "http://localhost:8080/api/v2/historyMessage?productId=0&page=0&size=20" \
-H "Authorization: Bearer {token}"
```

#### 4. 删除历史消息

```bash
curl -X DELETE "http://localhost:8080/api/v2/historyMessage?chatId=test-chat-1" \
-H "Authorization: Bearer {token}"
```

## 功能特点

- **SSE流式传输**: 实时获取AI回复，无需等待完整响应
- **对话历史持久化**: 所有对话记录自动保存到数据库，支持分页查询
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