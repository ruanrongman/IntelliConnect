# 实时语音协议

`rodak-realtime-voice/v1` 在同一条 WebSocket 上用 JSON 文本帧发送控制事件，用二进制帧传送 Opus 音频。普通会话由设备发起，语音识别、智能体调用和语音合成使用设备绑定产品的配置。[设备绑定与鉴权](binding.md)

## 连接和协商

设备从 Bootstrap 或设备凭据响应的 `realtimeVoice.endpoint` 取得地址。生产部署使用 WSS。WebSocket Upgrade 请求需要以下请求头。

```http
GET /api/v1/aiot/devices/realtime-voice
Upgrade: websocket
Connection: Upgrade
Authorization: Bearer <device-token>
Device-Id: aa:bb:cc:dd:ee:ff
Client-Id: bigsmart-voice
Protocol-Version: 1
```

`Device-Id` 与 JWT 对应的 `deviceKey` 一致。`Client-Id` 必须非空，最多 128 个字符。同一设备的新语音连接会替换旧连接。每条连接只发送一次 `session.open`，每次重连重新协商。`generation` 是设备为本次连接选择的 `1` 至 `4294967295` 的整数，设备应使用它隔离新旧连接的回调和音频。

```json
{
  "event": "session.open",
  "protocol": "rodak-realtime-voice",
  "protocolVersion": 1,
  "generation": 1,
  "uplink": {"codec": "opus", "sampleRateHz": 16000, "channels": 1, "frameDurationMs": 60},
  "downlink": {"codec": "opus", "sampleRateHz": 16000, "channels": 1, "frameDurationMs": 60},
  "features": {"mcp": false, "device_vad_epoch": 1, "turn_accepted": 1, "turn_progress": 1},
  "vadStrategies": ["server-authoritative"],
  "preferredVadStrategy": "server-authoritative"
}
```

服务端返回 `session.ready`，回显 `generation`、`protocol`、`protocolVersion` 和确认的 `uplink`、`downlink`，并提供 `sessionId`、`transport: "websocket"` 和选定的 `vadStrategy`。设备在收到它之后才能发送唤醒、收音和音频。后续设备控制帧使用返回的 `sessionId`。

`turn_accepted` 和 `turn_progress` 是可选的整数开关，取值为 `0` 或 `1`，缺省为 `0`。启用 `turn_progress` 需要同时启用 `turn_accepted`。这两个事件通过 `session.open` 单独协商，Bootstrap 描述符的 `events` 列出固件通用的基础事件。

| 参数 | IntelliConnect 支持范围 |
| --- | --- |
| 编码和声道 | 双向 Opus，单声道。 |
| 上行 | 固定 `16000 Hz / 60 ms`，每个包解码为 960 个采样点。 |
| 下行 | 采样率 `16000`、`24000`、`48000 Hz`，帧长 `5`、`10`、`20`、`40`、`60 ms`。Bootstrap 默认建议 `16000 Hz / 60 ms`。 |
| 控制帧 | 单条 JSON 文本消息最多 65,536 字节。会话协商与各控制事件按字段定义解析。 |
| 音频帧 | 单个 Opus 负载最多 8,192 字节，另有 12 字节 RAV1 头。 |

## 一轮对话和打断

```text
设备       → 服务端  session.open
服务端     → 设备    session.ready
设备       → 服务端  wake.detected → input.start → 若干 RAV1 上行帧
设备       → 服务端  input.stop，或由协商的 VAD 检测结束
服务端     → 设备    turn.accepted → 零条或多条 turn.progress（按会话协商）
服务端     → 设备    output.start → 若干 RAV1 下行帧 → output.stop
设备                 播完本地缓冲后，可在同一会话中再次 input.start
```

| 方向 | `event` | `sessionId` 之外的字段与处理 |
| --- | --- | --- |
| 设备 → 服务端 | `wake.detected` | `text` 为非空字符串，报告本次唤醒。首次唤醒后还需 `input.start` 打开输入。 |
| 设备 → 服务端 | `input.start` | 可带 `mode`，取 `manual-stop`、`auto-stop` 或 `realtime`。录音的二进制帧在此事件之后发送。新一轮输入可打断仍在处理或播报的旧轮次。 |
| 设备 → 服务端 | `input.stop` | 可带 `reason`，用于显式结束当前输入，常见于按键松开。 |
| 设备 → 服务端 | `vad` | `state` 为 `start` 或 `end`，并带 `source`、正整数 `sequence`、`triggerMs`，可带 `playbackEpoch`。设备边界按协商的 VAD 策略处理。 |
| 服务端 → 设备 | `turn.accepted` | `turnId` 为正整数。识别到非空输入并决定回答时发送一次。每次建立会话时从 `1` 开始，仅已确认的轮次占用序号。 |
| 服务端 → 设备 | `turn.progress` | 当前轮的 `turnId`、从 `1` 开始递增的 `seq` 和 `stage`。阶段包括 `thinking`（开始模型路由）、`tool`（开始分派工具）和 `composing`（开始准备最终回复）。 |
| 服务端 → 设备 | `output.start` | `text` 为开始播放时可用的回复文本，流式回复通常为首句。`playbackEpoch` 标识此次输出。第一帧下行音频随后到达。 |
| 服务端 → 设备 | `output.stop` | `reason` 和 `playbackEpoch` 标识输出结束，例如 `complete`、`interrupted`、`timeout` 或 `error`。 |
| 设备 → 服务端 | `playback.abort` | `reason` 必填，建议携带当前 `playbackEpoch`。旧批次的打断会被忽略。 |
| 双向 | `session.end` | 可带 `reason`。服务端发出结束事件后会正常关闭连接。 |
| 服务端 → 设备 | `error` | `code`、`message`、`retryable`。如果已协商会话，还会带 `sessionId`。 |

手动收音示例，依次发送下列第一条、若干 RAV1 上行帧、第二条。

```json
{"event":"input.start","sessionId":"<session-id>","mode":"manual-stop"}
```

```json
{"event":"input.stop","sessionId":"<session-id>","reason":"button-released"}
```

收到 `output.start` 后开始解码下行音频，收到同一批次的 `output.stop` 后播放完本地缓冲。打断时清理当前播放队列，忽略已结束会话、旧 `generation` 或旧 `playbackEpoch` 的异步结果。一个播放批次的音频序号延续本连接的序列，下一轮回复继续递增。

进度只在 `turn.accepted` 与首次 `output.start` 之间发送。工具开始执行时立即发送 `tool`，其他密集的阶段变化可在 500 毫秒内合并，`seq` 按实际发送次数计数。设备使用阶段对应的本地文案。主动播报会话直接播放音频。

流式回复按标点分句，首句就绪即可合成，同时最多预取下一句。音频按原句顺序发送，整轮共用一个 `playbackEpoch`。收到完整文本后补读尚未播放的尾段。

实时收音在等待回复和播放期间可继续上传音频。服务端 VAD 在等待回复时检测到新语音，会取消正在处理的轮次，并用最近的上行音频接续收音。播放期间收到当前播放批次的语音打断时，服务端停止下行音频并接续收音。

例如服务端发送下列播放起始事件。用户打断时，设备可在同一会话用相同 `playbackEpoch` 发送第二条事件。

```json
{"event":"output.start","sessionId":"<session-id>","playbackEpoch":1,"text":"你好，有什么可以帮你？"}
```

```json
{"event":"playback.abort","sessionId":"<session-id>","playbackEpoch":1,"reason":"user-interrupt"}
```

## VAD 与输入边界

- `server-authoritative` 由服务端根据上传音频判断语音活动。服务端优先采用此策略时，设备的 `vad` 事件会经过校验，由服务端决定收音边界。
- `hybrid-fallback` 接受设备边界，并在设备没有及时结束录音时保留服务端检测。启用 `rodak.voice.hybrid-vad-enabled` 且设备声明支持时，服务端优先选择这一策略。
- `device-authoritative` 由设备报告边界。握手只在服务端首选策略和 `server-authoritative` 都未提供、设备提供 `device-authoritative` 时回退到此模式。

`vadStrategies` 中的值必须唯一，`preferredVadStrategy` 必须位于该列表中。设备按 `session.ready.vadStrategy` 工作。同一个设备语音段的两个 `vad` 事件分别使用 `state: "start"` 和 `state: "end"`，并使用相同的 `sequence` 和 `playbackEpoch`。下一段用更大的 `sequence`。`triggerMs` 使用非负整数。有效输入块累计到 20 秒时，服务端结束该轮录音并进入识别。

## RAV1 二进制帧

每条二进制 WebSocket 消息承载一个完整帧。序号和负载长度均是 4 字节无符号大端整数。

| 字节偏移 | 长度 | 含义 |
| --- | --- | --- |
| `0` | 4 | ASCII `RAV1`，十六进制 `52 41 56 31`。 |
| `4` | 4 | 方向内序号，第一帧为 `1`，后续严格递增。 |
| `8` | 4 | Opus 负载字节数，范围 `1` 至 `8192`。 |
| `12` | 负载长度 | 一个原始 Opus 包。 |

例如序号 `1` 且负载长 `100` 字节时，头部为 `52 41 56 31 00 00 00 01 00 00 00 64`，整条消息长 `112` 字节。上行和下行各自从 `1` 开始编号，连续的多个对话轮次仍沿用同一条连接的序列。新的 WebSocket 重新编号。

## 结束、错误与重连

| 情况 | 服务端行为与设备处理 |
| --- | --- |
| 正常结束 | 例如告别后收到 `session.end`，关闭码 `1000`。设备结束本次对话并恢复本地唤醒。 |
| 鉴权失效 | Upgrade 可返回 HTTP `401`，已连接的会话可关闭为 `1008`。刷新设备凭据，解绑时重新配对。 |
| 协议格式错误 | 例如会话 ID 不匹配、序号重复或音频头无效，返回 `invalid-canonical-event`、`retryable: false`，并关闭为 `1002`。 |
| 服务短暂不可用 | 例如 `voice-busy`、`tts-failed`、`turn-timeout` 或关闭码 `1011`。设备按 `retryable` 和固件重连策略处理。 |
| 新版轮次超时或回复为空 | 超时发送 `voice-turn-timeout`。接受后回复为空发送 `voice-turn-no-reply`。两者均为 `retryable: false`，随后关闭会话。 |
| 连接被替换 | 同一设备建立新连接后，旧连接关闭为 `4001`，旧连接停止重试。 |

打开后约 10 秒未完成 `session.open` 时服务端会关闭连接。未启用 `turn_accepted` 的普通轮次超时为 90 秒。启用后，语音识别、路由和等待首帧的上限分别为 20、90 和 20 秒。接受轮次后，`output.start`、`error` 或 `session.end` 须在 120 秒内到达，进度事件不延长时限。开始播放后，连续 20 秒没有新的 RAV1 音频帧时发送 `output.stop`。需要重连时重新获取有效凭据，建立新的 WebSocket 并从 `session.open` 开始。同一入口也可承载仅播放的 [主动播报会话](announce.md)。
