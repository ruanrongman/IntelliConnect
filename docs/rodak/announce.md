# 主动播报协议

主动播报由服务端调度。服务端通过 MQTT 发送 `voice.announce`，设备接受后主动建立一条只播放音频的 WebSocket。MQTT 传递调度编号，曲名和音频由鉴权后的语音连接下发。[AIoT 数据协议](aiot.md) · [实时语音协议](voice.md)

## 何时使用

IntelliConnect 当前通过 **RodakOS 语音对话中的智能体音乐工具** 触发此流程。为产品启用音乐工具，在 `ai.music.path` 配置后端可读取的 MP3 目录，并完成设备绑定和 Rodak MQTT 配置。设备需在线并订阅 `devices/{deviceKey}/commands/+`。服务端选中曲目后，先用 `session.end` 和原因 `music-announce` 结束对话，再发送播报邀请，以便设备释放对话占用的音频资源。

```text
设备语音点歌 → 服务端选曲 → 对话 session.end（music-announce）
→ MQTT voice.announce → 设备 ACK 接受或拒绝
→ 设备建立新的 WSS → session.open（announceSeq）→ session.ready
→ output.start → RAV1 下行音频 → output.stop → session.end
```

## 发送命令和确认

服务端在 `devices/{deviceKey}/commands/{commandNo}` 发送 QoS 1、非保留消息。`commandNo` 为本次 MQTT 命令的 32 位小写十六进制编号。`announceSeq` 为 `1` 至 `4294967295` 的整数，之后用于领取本次播放。IntelliConnect 当前发送 `priority: "normal"`。RodakOS 能识别 `urgent`，但正常的 IntelliConnect 音乐播报按普通优先级调度。

```json
{"command":"voice.announce","announceSeq":17,"priority":"normal"}
```

设备在 `devices/{deviceKey}/commands/{commandNo}/ack` 回执，主题必须使用原 `commandNo`。

```json
{"status":"ok","result":{}}
```

`status: "ok"` 表示设备接受命令并已安排打开播放会话。接下来的 WSS 连接或播放仍可能失败。设备忙时返回下列回执。

```json
{"status":"error","errorCode":"busy"}
```

| 回执码 | 含义与后续处理 |
| --- | --- |
| `ok`（`status`） | 等待设备使用 `announceSeq` 领取音频。 |
| `busy` | 设备的语音助手正在工作、普通优先级下本地音乐正在播放，或设备暂时无法安排会话。若仍在有效期内，IntelliConnect 会等待约 1 秒再发，最多重试两次。 |
| `invalid_payload` | 播报编号缺失或超出允许范围，检查命令字段。 |
| `voice_unavailable` | 固件的语音回调不可用，确认设备固件和语音能力。 |

每次重试使用新的 `commandNo` 和 `announceSeq`，沿用首次注册的 30 秒有效窗口。同一个 `commandNo` 的重复投递应当去重。播放邀请通过 MQTT 发出，其领取记录只在当前服务进程中保留，HTTP `/commands/pull` 返回的是普通命令。设备重连时应等待新的播报命令。

## 打开播放会话

设备用正常的语音鉴权头和当前设备 JWT 连接 `/api/v1/aiot/devices/realtime-voice`。`session.open` 保留实时语音协议的完整字段，`features` 在普通会话字段的基础上携带 `announce: true` 和命令中的同一个 `announceSeq`。

```json
{
  "event": "session.open",
  "protocol": "rodak-realtime-voice",
  "protocolVersion": 1,
  "generation": 2,
  "uplink": {"codec": "opus", "sampleRateHz": 16000, "channels": 1, "frameDurationMs": 60},
  "downlink": {"codec": "opus", "sampleRateHz": 48000, "channels": 1, "frameDurationMs": 20},
  "features": {"mcp": false, "device_vad_epoch": 1, "announce": true, "announceSeq": 17},
  "vadStrategies": ["server-authoritative"],
  "preferredVadStrategy": "server-authoritative"
}
```

服务端发出 `session.ready`，再校验播报编号是否属于当前设备、产品和绑定版本，是否仍在有效期内，并且是否已经领取。每个 `announceSeq` 只可领取一次。设备在 `session.ready` 后等待 `output.start`，保持麦克风关闭。`wake.detected`、`input.start`、`input.stop`、`vad` 和上行音频用于普通语音对话。

## 播放和结束

领取成功时，IntelliConnect 将选中的 MP3 转为会话协商的 Opus 格式。`output.start.text` 携带曲名，随后发送同一连接上的 RAV1 音频。`output.stop` 对应相同的 `playbackEpoch`。设备播完本地缓冲后恢复本地唤醒。

| 信号 | 含义 |
| --- | --- |
| `output.stop` 的 `reason: "complete"`，随后 `session.end` 的 `reason: "announce-finished"` | 服务端完成了本次音频下发。 |
| `session.end` 的 `reason: "announce-expired"` | 编号过期、已领取或与当前绑定不匹配。服务端在 `session.ready` 后结束本次会话。 |
| `session.end` 的 `reason: "announce-failed"` | 转码或下发未完成。已开始发送时，服务端先发 `output.stop`，原因为 `error`。 |
| `session.end` 的 `reason: "announce-interrupted"` | 客户端主动发送 `playback.abort` 且匹配当前播放。RodakOS 本地打断也可以直接结束连接。 |

播放连接中断时，设备结束本次播报并释放音频资源。服务端对播报会话约 20 秒无活动的情况以 `1011` 关闭。排查时依次查看设备是否收到命令、ACK 是否为 `ok`、`session.open` 是否带原始 `announceSeq`、服务端是否发出 RAV1 以及设备扬声器是否实际播出。ACK、领取成功和服务端完成下发是三个不同的阶段。
