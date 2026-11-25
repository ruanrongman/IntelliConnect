# AIoT 数据协议

设备绑定后可以通过 HTTP 上报遥测、同步影子并拉取命令。启用 `rodak.aiot.enabled` 以使用数据接口，启用 `rodak.mqtt.enabled` 并配置 EMQX 后可通过 MQTT 上报和接收推送。HTTP 与 MQTT 共用同一份设备状态和 Ping 命令记录。[设备绑定与鉴权](binding.md) · [部署和 EMQX 配置](../get_started/rodakos.md#启用-mqtt-推送)

## HTTP 与 MQTT 接口

下表中的 HTTP 路径以 `/api/v1/aiot/devices` 开头，请求携带 `Authorization: Bearer <device-token>`。HTTP 响应体使用 `code`、`message`、`data`。例如鉴权失败会返回 HTTP 401，业务错误可能在 HTTP 200 中通过 `code` 返回。上报和回执的 JSON 请求体上限为 65,536 字节。

MQTT Broker 和主题由凭据响应的 `unifiedMqtt` 下发。使用 `broker_address` 和 `broker_port` 连接，以 `username` 和 `password` 登录。用户名为 `deviceKey`，密码为设备 JWT。BigSmart 的客户端 ID 为 `rodakos_` 加去掉冒号的小写 MAC，例如 `rodakos_aabbccddeeff`。下表的 MQTT 路径以 `devices/{deviceKey}/` 开头。设备使用自己的主题，按表中方向发布或订阅。平台下行以 QoS 1、非保留消息发送。

| 操作 | HTTP | MQTT |
| --- | --- | --- |
| 心跳 | `POST /heartbeat` | Broker 连接单独跟踪 |
| 上报遥测 | `POST /telemetry` | 发布 `telemetry` |
| 上报实际状态 | `POST /shadow/report` | 发布 `shadow/report` |
| 获取目标状态 | `GET /shadow/desired` | 订阅 `shadow/desired` |
| 接收普通命令 | `GET /commands/pull?limit=20` | 订阅 `commands/+` |
| 确认命令 | `POST /commands/{commandNo}/ack` | 发布 `commands/{commandNo}/ack` |

例如凭据响应的 `data.unifiedMqtt` 包含以下字段。`protocol_version` 是描述的版本，主题按设备下发。表中列出了 IntelliConnect 当前用于状态和命令的主题。

```json
{
  "protocol_version": 2,
  "broker_address": "mqtt.example.com",
  "broker_port": 1883,
  "username": "aa:bb:cc:dd:ee:ff",
  "password": "<device-token>",
  "keepalive": 240,
  "device_key": "aa:bb:cc:dd:ee:ff",
  "topics": {
    "telemetry": "devices/aa:bb:cc:dd:ee:ff/telemetry",
    "shadow_report": "devices/aa:bb:cc:dd:ee:ff/shadow/report",
    "shadow_desired": "devices/aa:bb:cc:dd:ee:ff/shadow/desired",
    "commands": "devices/aa:bb:cc:dd:ee:ff/commands/+"
  }
}
```

## 心跳和遥测

`POST /heartbeat` 无需请求体，成功时 `data` 包含 `deviceId`、`deviceKey`、`status: "online"` 和以毫秒表示的 Unix `timestamp`。智控台的最近活跃状态使用 180 秒窗口，另外显示语音 WebSocket 和 MQTT 的连接状态。HTTP 心跳、遥测、影子上报等均可更新最后活跃时间。

遥测通过 HTTP 或 MQTT 上报同样的 JSON 对象。IntelliConnect 保留最近一次遥测快照及其上报时间，智控台从该快照显示设备读数。

```json
{
  "firmware": "1.4.0",
  "wifi_rssi": -55,
  "volume": 45,
  "battery": 82,
  "charging": false,
  "free_heap": 120000,
  "minimum_free_heap_size": 98000,
  "uptime_ms": 60000,
  "ota_slot": "ota_0"
}
```

`wifi_rssi` 的单位为 dBm，`battery` 是整数百分比，`volume` 是 `0` 至 `100` 的整数。电量或充电状态不可读时，可省略相应字段。HTTP 遥测成功时 `data` 为 `true`。

## 设备影子和音量

`desired` 是平台期望值，`reported` 是设备上报的实际状态。操作者通过智控台设定音量，管理端调用 `PUT /api/v2/rodak/devices/{id}/volume?productId=7`，请求体为 `{"volume":30}`。设备收到目标状态后应用，然后上报实际值。智控台可同时显示两者。

- HTTP `POST /shadow/report` 接收 `{"reported":{"volume":30}}` 或平铺的 `{"volume":30}`。
- MQTT `shadow/report` 发布平铺的实际状态对象。每次上报替换上一份 `reported` 快照。设备上报时应包含希望保留的实际状态字段。
- HTTP `GET /shadow/desired` 返回完整快照。MQTT 下行在设备订阅后同步快照，并在 `desired` 变化时推送。`version` 与 `shadowVersion` 值相同，实际状态或目标值发生变化时递增。

```json
{
  "deviceKey": "aa:bb:cc:dd:ee:ff",
  "shadowVersion": 7,
  "version": 7,
  "desired": {"volume": 30},
  "reported": {"firmware": "1.4.0", "volume": 45},
  "state": {"desired": {"volume": 30}, "reported": {"firmware": "1.4.0", "volume": 45}}
}
```

上例表示设备实际音量还是 45，平台希望调整到 30。设备从根层 `desired` 或兼容字段 `state.desired` 读取目标。此快照在 HTTP 响应的 `data` 中返回，MQTT 直接发送对象并附加 `_meta["rodak/shadowWrite"].dispatchId`。

## Ping 命令和回执

智控台调用 `POST /api/v2/rodak/devices/{id}/ping?productId=7` 创建 Ping。平台返回 `commandNo`，并将命令保存为 `pending`，有效期 120 秒。它可由 MQTT 推送，也可由设备调用 `GET /commands/pull?limit=20` 拉取。拉取只返回当前绑定下未过期且未确认的命令，按创建时间从早到晚排列，`limit` 限制在 `1` 至 `100`。

HTTP 拉取返回命令数组，单项中的 `payload` 是 JSON 字符串。设备先解析外层响应，再解析 `payload`。

```json
{
  "id": "0123456789abcdef0123456789abcdef",
  "deviceId": "11111111-1111-4111-8111-111111111111",
  "deviceKey": "aa:bb:cc:dd:ee:ff",
  "commandNo": "0123456789abcdef0123456789abcdef",
  "topic": "devices/aa:bb:cc:dd:ee:ff/commands/0123456789abcdef0123456789abcdef",
  "payload": "{\"command\":\"ping\"}",
  "status": "pending",
  "createdAt": "2026-11-25T08:00:00Z"
}
```

MQTT 在同一个 `topic` 上直接发送 JSON 对象 `{"command":"ping"}`。设备从 MQTT 主题或 HTTP 结果取 `commandNo`。同一条命令可能经两个通道到达，设备应按 `commandNo` 去重，在回执中使用原编号。

```json
{"status":"ok","result":{"pong":true}}
```

失败回执可使用 `{"status":"error","errorCode":"<error-code>"}`。第一个有效回执写入命令记录，重复回执保留已有结果，过期且从未确认的命令返回业务码 `410`。管理端可通过 `GET /api/v2/rodak/devices/{id}/commands/{commandNo}?productId=7` 查询状态 `pending`、`ok`、`error` 或 `expired`。其中 `{id}` 使用设备 UUID。

主动播报用同一 MQTT 命令命名空间，但有单独的回执和领取流程。[主动播报协议](announce.md)

## 物模型和 OTA 查询

- `GET /api/v1/aiot/products/{productId}/thing-model` 返回有活跃 Rodak 绑定产品的属性描述，例如 `firmware`、`wifi_rssi`、`volume` 和 `battery`。`identifier`、`dataType`、`accessMode` 描述单个属性。此接口可公开查询。
- `GET /api/v1/ota/check` 携带设备 JWT。当前返回 `{"code":200,"message":"ok","data":{"upgradeAvailable":false}}`。
