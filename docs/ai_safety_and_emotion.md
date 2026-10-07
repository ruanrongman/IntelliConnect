# 全局角色审核、青少年保护、Jev 与 Laya 情绪能力

本文说明 IntelliConnect 当前提供的全局角色审核、青少年保护审核、会话使用限制、Jev 接入以及本地 Laya 情绪识别能力。

## 全局角色审核

全局角色审核是独立于青少年模式的系统级能力。管理员可以在系统配置页开启或关闭
`global-role-review.enabled`，并编辑 `global-role-review.requirements`。开关默认关闭；要求文本默认审核角色名称和角色介绍中的黄色、色情、性暗示、暴力、自残、违法犯罪、诈骗、毒品、赌博、恐怖主义、仇恨、骚扰、隐私索取和诱导未成年人危险行为等内容，同时允许正常教育、科普、健康、安全和普通陪伴内容。

自定义要求只会追加到默认安全规则，不能通过编辑框删除基础规则。编辑框最多 2000 个字符，配置保存后立即生效，不需要重启应用。角色创建和修改按“基础参数/音色校验 → 全局审核 → 青少年审核 → 保存”的顺序执行；全局开关关闭时不会调用审核模型，也不会增加普通角色保存延迟。明确返回 `block` 时拒绝保存并返回 `ROLE_REVIEW_REJECTED`（3007）。

审核模型由独立配置 `ai.globalRoleReview-llm` 选择，默认值为 `decision-model`：

- Jev 路径使用 `JevClient` 的 `choice` 结果，`allow` 放行、`block` 拒绝，并复用 Jev 的重试、响应校验和脱敏日志机制。
- 普通 LLM 路径使用 `LLMFactory.getLLM(...)` 和 JSON 审核提示词，严格解析 `allow`/`block` 决定。
- 网络异常、空响应、格式错误或未知决定不会被误判为“审核通过”；按已确认策略记录告警并放行角色，只有模型明确返回 `block` 才拒绝。

全局审核只检查角色配置中的角色名称、角色设定和角色介绍，不审核普通聊天消息。青少年模式若同时开启，仍会独立执行其更严格的角色审核与会话限制。

## 青少年保护能力

青少年保护是产品级能力，由产品运行参数控制。配置保存在产品用户配置中，可在产品设置弹窗中修改：

| 配置名称 | 配置键 | 默认值 | 范围 | 作用 |
|---|---|---:|---:|---|
| 青少年模式 | `youth-protection.enabled` | `false` | `true/false` | 开启角色审核和会话限制 |
| 青少年模式最大对话轮次 | `youth-protection-talk.maxLimit` | `1000` | `1..3000` | 最近两小时允许的用户消息数量 |

### 角色审核

开启青少年模式后，产品角色的创建和修改都会审核角色设定和角色介绍。审核失败时不会保存角色，接口返回错误码 `3006`（`YOUTH_HARMFUL`）。

审核规则覆盖：

- 虚拟亲密关系、恋爱或暧昧关系，以及要求对监护人保密的互动；
- 性内容、性剥削、骚扰、私下见面和诱导隐瞒；
- 自残、自杀、暴力、危险挑战、极端节食等危险行为；
- 欺凌、仇恨、羞辱、威胁、操纵和严重情绪伤害；
- 索取或泄露密码、验证码、证件、地址、联系方式、精确位置、学校信息等隐私；
- 赌博、毒品、酒精、烟草、犯罪、恐怖主义、极端主义、欺诈和网络攻击；
- 诱导未成年人转账、充值、购买商品或作出不合理决定；
- 明确表示未满 14 岁并要求绕过父母或监护人同意的服务。

适龄教育、科学、隐私保护、心理支持和危险预防内容，在不提供可执行的有害、色情或违法细节时可以放行。模型返回为空、格式错误、不支持的决定或服务调用失败时，审核按保护性策略拒绝，不保存角色。

### 最近两小时会话限制

每次请求进入统一 `Router` 后，系统按 `chatId` 检查最近两小时的历史记录。当前实现只统计：

- `chat_id = 当前会话 chatId`；
- `message_type = "user"`；
- `time` 位于当前时间向前两小时的窗口内。

当用户消息数量大于或等于 `youth-protection-talk.maxLimit` 时，系统不会继续调用模型，而是返回：

```text
小朋友，是时候休息一下啰。
```

限制按会话 `chatId` 独立计算，不跨会话合并。文本、文本流式、语音转文字以及微信/设备调用，只要进入同一 `Router` 链路，就使用同一规则。历史消息仍按一轮写入 `user` 和 `assistant` 两条记录，但实时限制只统计 `user` 记录。

当前策略是轻量的历史计数方案：它不引入 Redis 预占或数据库锁，因此同一 `chatId` 的极端并发请求可能略微超过配置值；这属于当前产品对精确度和实现复杂度的取舍。

### 青少年回答提示

路由器在构建本次请求上下文时读取 `youth-protection.enabled`，并把状态复用给各提示词。启用后，路由提示（`FunctionCallingRouterPrompt` 和旧路由的 `ClassifierToolPrompt`）以及 `ChatToolPrompt` 会追加一段很短的适龄回答规则：保持友善、清晰，危险请求不调用工具，简短拒绝并提供安全替代。关闭时不追加这段提示，避免增加普通请求的上下文长度；它是回答引导，不能替代角色审核和会话次数限制。

## Jev 接入

决策模型客户端通过 TypeSafe 的 `POST /v1/systemone` 接口提供结构化评估，支持 `noul`、`choice` 和 `score` 三种问题类型。配置位于 `application.yaml` 的 `ai.decision-model`：

```yaml
ai:
  decision-model:
    base-url: https://api.typesafe.ai
    key: ${JEV_API_KEY:}
    model: decision-model-preview
  emotionTool-llm: decision-model
  youthProtectionTool-llm: decision-model
```

内置工具使用 `decision-model` 时走类型安全的决策模型客户端：

- `EmotionTool`：根据当前问题、最近对话和长期记忆选择情绪名称，再映射为设备/前端表情；
- `YouthProtectionTool`：使用 `allow`、`block`、`guardian_consent` 三类决定审核内容；
- Jev 网络错误、服务繁忙、返回格式错误或未知结果时，情绪识别回退到 `neutral`，青少年审核按保护性策略拒绝。

Key 可以通过 `JEV_API_KEY` 注入。未配置 Key 不影响应用启动，但实际调用会失败。客户端只对 HTTP 429/529 按 `Retry-After` 或退避策略进行有限重试，不记录 Key、原始请求正文或原始错误正文。

真实服务测试不会自动执行，使用以下命令显式开启：

```powershell
.\mvnw.cmd '-Dtest=JevClientLiveTest' '-Djev.live=true' test
```

## 本地 Laya 情绪识别

仓库中的 `laya-jev` 是一个 FastAPI 本地服务，加载 `convaiinnovations/laya-multilingual`，并提供兼容决策模型的 `POST /v1/systemone` 接口。因此 IntelliConnect 不需要新增另一套情绪协议，只需把 `ai.decision-model.base-url` 指向本地服务，并保持 `emotionTool-llm: decision-model`：

```yaml
ai:
  decision-model:
    base-url: http://127.0.0.1:8001
    key: ${JEV_API_KEY:local-placeholder}
    model: decision-model-preview
  emotionTool-llm: decision-model
```

Windows 启动：

```powershell
cd laya-jev
py -3.11 -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe download_model.py
.\start.ps1 -Device cpu
```

GPU 环境可使用 `.\start.ps1 -Device cuda`。服务启动后先确认：

```powershell
Invoke-RestMethod http://127.0.0.1:8001/health
```

`/health` 返回 `status: ok` 后，Java 应用才会调用本地模型。服务支持 `GET /health` 和 `POST /v1/systemone`，并提供请求超时、模型加载、并发槽位、token 预算和必要时重启保护。生产部署请设置 `LAYA_API_KEY`、限制监听地址和端口访问范围，并使用 HTTPS 反向代理保护跨机器访问。

Laya 服务可同时承载情绪识别和其他结构化 Jev 评估，但情绪类别、提示词和回退策略仍由 IntelliConnect 的 `EmotionTool` 控制。Laya 本地测试、Java 单元测试和真实 Jev 服务测试分别验证不同层次，不能互相替代。

## 相关实现与文档

- [Jev 独立客户端](jev-client.md)
- [Laya Jev 兼容服务](../laya-jev/README.md)
- [AI 文本调试](ai_chat/ai_chat.md)
