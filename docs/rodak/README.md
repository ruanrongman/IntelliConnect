# Rodak 协议

本目录说明 IntelliConnect 对 RodakOS 的设备端接口和消息格式。部署、智控台操作和选型请先看 [RodakOS 接入指南和小智协议对比](../get_started/rodakos.md)。

| 阶段 | 通道 | 协议文档 |
| --- | --- | --- |
| 发现服务、绑定产品、领取或刷新设备凭据 | HTTPS | [设备绑定与鉴权](binding.md) |
| 上报遥测、同步设备影子、执行命令和回执 | HTTPS、MQTT | [AIoT 数据协议](aiot.md) |
| 唤醒、录音、回复、多轮交互和打断 | WSS，JSON 控制帧和 RAV1 音频帧 | [实时语音协议](voice.md) |
| 服务端通过 MQTT 发出播放邀请，设备另建语音会话播放 | MQTT → WSS | [主动播报协议](announce.md) |

设备首先通过 `/api/v1/aiot/devices/bootstrap` 读取 `rodak-aiot/1` 描述，绑定成功后取得设备 JWT。设备数据通过 HTTP 或 MQTT 交互，语音音频通过 `rodak-realtime-voice/v1` 的 WebSocket 传输。智控台使用用户 JWT 访问 `/api/v2/rodak`，负责选定产品、确认配对和管理设备。

| 标识 | 用途 |
| --- | --- |
| `deviceKey` | 固件的设备标识。BigSmart 使用小写、有冒号的 MAC 地址，用于设备鉴权和 MQTT 主题。 |
| `deviceId` | IntelliConnect 分配的设备 UUID。智控台设备列表把它返回为 `id`。 |
| `productKey` / `productId` | 固件产品键为 `rymcu-bigsmart`，智控台的 `productId` 是用户在 IntelliConnect 中选择的整数产品 ID。 |
| `requestId` / `commandNo` | 32 位小写十六进制字符串，分别标识配对申请和 AIoT 命令。 |
| `sessionId` / `playbackEpoch` | WebSocket 会话 ID 和该会话内的播放批次，用于关联控制帧。 |
| `announceSeq` | 主动播报的短时领取编号，用来关联 MQTT 调度命令和新的 WebSocket 会话。 |

各页示例用 `https://iot.example.com` 表示部署地址，`aa:bb:cc:dd:ee:ff` 表示设备标识，尖括号表示需要替换的值。
