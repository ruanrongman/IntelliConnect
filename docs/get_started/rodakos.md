# RodakOS 接入

运行 RodakOS 的设备可以绑定到 IntelliConnect 产品，使用该产品配置的智能体语音对话。智控台可以查看设备连接状态和遥测，设置音量、发送 Ping 或解除绑定。固件下载、编译和烧录说明见 [RodakOS 项目](https://github.com/rymcu/rodakos)。

## 部署 IntelliConnect

1. 完成 IntelliConnect 的基础部署，登录智控台并创建用来绑定设备的产品。需要语音对话时，先为该产品配置角色、模型、语音识别和语音合成。[快速开始](quick_start.md)
2. 在 IntelliConnect 使用的 MySQL 数据库中先执行配对表脚本，再执行影子和命令表脚本。[配对表](https://github.com/ruanrongman/IntelliConnect/blob/main/docs/sql/rodak_p1.sql) · [影子和命令表](https://github.com/ruanrongman/IntelliConnect/blob/main/docs/sql/rodak_p3.sql)
3. 在 `application.yaml` 中设置设备可达的 HTTPS 地址和独立的 Rodak 签名密钥。先用下列配置验证配对、语音及 HTTP 数据通道，需要 MQTT 推送时再配置 EMQX。HTTPS 证书应能被设备信任，反向代理需要转发 WebSocket Upgrade 和请求头。

```yaml
rodak:
  http-base-url: https://iot.example.com
  security:
    secret: ${RODAK_SECURITY_SECRET}
  aiot:
    enabled: true
  mqtt:
    enabled: false
```

将 `RODAK_SECURITY_SECRET` 设为持续保存的随机密钥。设备凭据校验也依赖此密钥，变更前应安排重新配对。重启服务后，从设备所在网络访问 `https://iot.example.com/api/v1/aiot/devices/bootstrap`，确认 `data.protocol` 是 `rodak-aiot`，且 `data.realtimeVoice.endpoint` 指向设备可以访问的 `wss://` 地址。

### 启用 MQTT 推送

其他设备可以继续使用现有的 EMQX 和 ExHook。Rodak MQTT 需要支持 HTTP 认证返回 `client_attrs` 和 `expire_at` 的 EMQX 版本，仓库的 Compose 使用 5.8.4。

1. 设置 `rodak.mqtt.enabled: true`。将 `rodak.mqtt.host` 和 `rodak.mqtt.port` 指向设备可达的 MQTT 监听器，把设备侧 `mqtt://` 监听器部署在可信网络内。`rodak.mqtt.bridge-uri` 是 IntelliConnect 后端访问 Broker 的地址，默认沿用 `mqtt.Host`。同时填写 `emq.api.url`（EMQX 的 `/api/v5/clients` 地址）、`emq.api.key` 和 `emq.api.secret`。
2. 为 `rodak.mqtt.callback-key` 和 `rodak.mqtt.service-password` 分别设置至少 24 个字符的不同随机值。在 EMQX 认证链中，将 HTTP 密码认证放在原有 MySQL 认证之前。认证请求为 `POST /internal/rodak/mqtt/authenticate`，请求头 `X-Rodak-Broker-Key` 使用回调密钥，JSON 请求体为 `{"username":"${username}","clientid":"${clientid}","password":"${password}"}`。让这个回调通过 Broker 可达的内部地址访问。
3. 将现有 MySQL 认证和授权查询换成公开仓库提供的共享查询，以隔离 Rodak 和已有 MQTT 客户端。将授权未匹配时的行为设为 `deny`，关闭授权缓存，并确保其他授权源也遵守对 `devices/` 和 `_rodak/` 的隔离。[共享认证 SQL](https://github.com/ruanrongman/IntelliConnect/blob/main/docker/rodak/shared-mysql-authn.sql) · [共享授权 SQL](https://github.com/ruanrongman/IntelliConnect/blob/main/docker/rodak/shared-mysql-authz.sql)
4. 在 EMQX 导入 Rodak 规则，转发设备上线、订阅和设备上报事件给 IntelliConnect 桥接服务。重启 IntelliConnect，已绑定的设备可在刷新凭据后取得新的 Broker 地址。[EMQX 消息规则](https://github.com/ruanrongman/IntelliConnect/blob/main/docker/rodak/shared-rules.conf)

## 配对与验证

1. 在 RodakOS 上配置 Wi-Fi 和 IntelliConnect 的 HTTPS 服务地址，例如 `https://iot.example.com`。可按固件版本使用设备设置、USB 串口或“连接 Rodak”下的扫码配置。[RodakOS 串口配网](https://github.com/rymcu/rodakos/blob/main/docs/serial-provisioning.md) · [RodakOS 扫码配网](https://github.com/rymcu/rodakos/blob/main/docs/qr-provisioning.md)
2. 设备联网后会显示 **8 位数字配对码**。在智控台进入 **Rodak 设备绑定 → 新建配置**，选择产品并输入配对码，有效期为 10 分钟。管理员也可以按设备标识选中待认领申请。
3. 设备取得凭据后，确认列表中的绑定状态为 **已绑定**。在设备上开始一轮对话，验证能收到语音回复。启用 MQTT 时，可单独查看 **MQTT 已连接**。语音 WebSocket、MQTT 连接和最近活跃时间分别显示。
4. 打开 **设备详情**，查看固件版本、Wi-Fi 信号和电量等设备已上报的数据。发送 Ping 后等待设备返回 Pong。设置音量后，**目标音量**显示平台设置的值，**设备音量**在设备应用并上报后更新。

设备显示配对失败时，请确认它访问的是当前部署的 Bootstrap 地址，并重新获取有效配对码。已绑定但语音不可用时，先检查 WSS 地址、证书和反向代理，再检查产品的语音配置。HTTP 可用而 MQTT 未连接时，检查设备能否访问 Bootstrap 下发的 Broker 地址，以及 EMQX 的认证、授权和转发规则。

## Rodak 设备协议

完整的请求、响应和消息示例见 [Rodak 协议文档](../rodak/README.md)。

- [设备绑定与鉴权](../rodak/binding.md)
- [AIoT 数据协议](../rodak/aiot.md)
- [实时语音协议](../rodak/voice.md)
- [主动播报协议](../rodak/announce.md)

## Rodak 与小智协议对比

以下比较的是 IntelliConnect 提供的两种接入方式。部署时按设备固件选择对应入口。[小智接入指南](../xiaozhi/xiaozhi.md)

| 项目 | RodakOS / Rodak | 小智 ESP32 |
| --- | --- | --- |
| 首次接入 | 从 `/api/v1/aiot/devices/bootstrap` 获取能力和地址，设备显示 8 位配对码，在 **Rodak 设备绑定** 中关联产品。 | 设备访问 `/xiaozhi/ota/`，通过激活码绑定产品，由 OTA 响应取得语音连接信息。 |
| 设备鉴权 | 绑定后签发短时设备 JWT，由设备密钥刷新，用于设备 HTTP、MQTT 和语音连接。 | 小智 OTA 响应返回小智 WebSocket 地址和连接令牌。 |
| 语音连接 | `/api/v1/aiot/devices/realtime-voice`，使用 JSON `event`，例如 `session.open`、`input.start`、`playback.abort`。 | `/xiaozhi/v1/{chatId}`，使用 JSON `type`，例如 `hello`、`listen`、`abort` 和 `tts`。 |
| 音频帧 | `RAV1` 封装 Opus，上行固定 `16 kHz / 60 ms`，下行独立协商。 | 小智 WebSocket 使用 Opus 二进制消息，通过 `hello` 的 `audio_params` 协商参数。 |
| 设备管理 | 独立的 HTTP 和 MQTT 通道处理遥测、设备影子、命令及回执。 | 小智 OTA 和 WebSocket 负责激活与语音。物模型设备管理可按 IntelliConnect 的物模型接入流程配置。 |
| 设备端 MCP | Rodak 语音握手的 `features.mcp` 为 `false`。 | 小智 WebSocket 支持协商设备端 MCP，使用 `type: "mcp"` 交换消息。 |
