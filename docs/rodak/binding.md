# 设备绑定与鉴权

设备先发现服务，再申请配对。操作者在智控台把设备绑定到指定产品，设备通过轮询获取凭据。之后只需用设备密钥刷新短时 JWT。解绑会撤销当前绑定和该绑定签发的 JWT。

## 发现服务

设备访问 `GET /api/v1/aiot/devices/bootstrap`，该请求无需认证。响应中的 `data` 提供 `protocol: "rodak-aiot"`、`protocolVersion: 1`、`schema: "rodak-aiot/1"`、`productKey`、`deviceTokenTtl`、`capabilities`、`endpoints` 和 `realtimeVoice`。设备通过 `endpoints.httpBaseUrl` 拼接后续请求，通过 `realtimeVoice.endpoint` 取得语音地址。`mqttGatewayEnabled` 表示服务端是否启用 Rodak MQTT 网关。地址以实际返回值为准。

```bash
curl 'https://iot.example.com/api/v1/aiot/devices/bootstrap'
```

设备 HTTP 响应统一使用 `{ "code": 200, "message": "ok", "data": ... }`。示例中的 `data` 可为对象、数组或布尔值。客户端应同时检查 HTTP 状态和 `code`，配对控制器的业务错误通常在 HTTP 200 响应体中返回。

## 提交配对申请

设备向 `POST /api/v1/aiot/devices/binding/request` 提交 JSON。`deviceSecret` 由设备持有，之后仍用同一个密钥刷新凭据。兼容字段 `deviceCode`、`device_key` 对应 `deviceKey`，`credentialSecret`、`device_secret` 对应 `deviceSecret`，`product_key`、`credentialKey` 对应 `productKey`。同时发送同义字段时，值必须一致，设备标识按去首尾空格和转小写后比较。`productKey` 可省略，显式提供时使用 `rymcu-bigsmart`。

```http
POST /api/v1/aiot/devices/binding/request
Content-Type: application/json

{"deviceKey":"aa:bb:cc:dd:ee:ff","deviceSecret":"<device-secret>","productKey":"rymcu-bigsmart"}
```

```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "requestId": "0123456789abcdef0123456789abcdef",
    "pairingCode": "01234567",
    "requestToken": "<request-token>",
    "status": "pending",
    "expiresAt": "2026-11-25T08:10:00Z",
    "pollIntervalSeconds": 2
  }
}
```

配对码是有效期 10 分钟的 **8 位数字字符串**，保留前导零。设备向用户显示 `pairingCode`，并保存 `requestId` 和 `requestToken` 供本次轮询使用。已绑定设备以原密钥提交申请时，首次响应仍为 `pending`，下一次状态查询即可收到 `confirmed` 和原绑定的凭据。

## 在智控台确认并轮询

操作者进入 **Rodak 设备绑定 → 新建配置**，选择有控制权限的产品并输入配对码。管理员可额外按设备标识在待认领列表中选中 `requestId`。管理接口使用用户 JWT，响应以 `success` 和 `errorCode` 表示结果。

```http
POST /api/v2/rodak/pairing-requests/confirm
Authorization: Bearer <user-jwt>
Content-Type: application/json

{"productId":7,"pairingCode":"01234567","requestId":"0123456789abcdef0123456789abcdef"}
```

`requestId` 可省略，此时服务端按有效配对码查找申请。确认成功后的响应为 `{"success":true,"errorCode":200,"data":{"deviceId":"<device-uuid>"}}`。设备以 `pollIntervalSeconds` 为轮询间隔，按下列方式查询申请。

```http
GET /api/v1/aiot/devices/binding/requests/0123456789abcdef0123456789abcdef/status
Authorization: Bearer <request-token>
```

| `data.status` | 设备处理 |
| --- | --- |
| `pending` | 继续轮询，等待操作者确认。 |
| `confirmed` | 保存 `deviceId`、`deviceToken` 和连接描述，停止轮询。 |
| `expired` | 停止轮询。仍需绑定时提交新申请。 |
| `revoked` | 停止轮询。核对设备的绑定状态，必要时提交新申请。 |

常见错误包括 `400`（配对字段无效）、`401`（轮询令牌无效）、`403`（产品无权限或凭据已撤销，视接口而定）、`404`（申请或产品不存在）、`409`（设备已绑定，凭据或操作与现有绑定冲突）和 `429`（请求或确认过于频繁）。

## 设备凭据与刷新

`confirmed` 响应和 `POST /api/v1/aiot/devices/auth/token` 的 `data` 均包含下列字段。刷新接口使用提交配对时的设备标识和设备密钥，例如 `{"deviceKey":"aa:bb:cc:dd:ee:ff","deviceSecret":"<device-secret>"}`。

| 字段 | 含义 |
| --- | --- |
| `deviceId`、`deviceKey`、`productKey` | 设备 UUID、固件设备标识、固件产品键。 |
| `deviceToken`、`tokenType` | 设备 JWT 和固定值 `Bearer`。`token`、`accessToken` 为同值别名。 |
| `expiresIn`、`expiresAt` | 有效期秒数（默认 600）和到期的 Unix 秒时间戳。申请刚创建时的 `expiresAt` 则是配对申请的 ISO 8601 时间。 |
| `unifiedMqtt`、`realtimeVoice` | MQTT 登录及主题描述、语音端点及音频能力。 |

HTTP 数据和 WebSocket 握手携带 `Authorization: Bearer <deviceToken>`，MQTT 将 `deviceToken` 作为密码。设备应在令牌到期前刷新，并用最新 JWT 建立需要重连的通道。每条语音和 MQTT 连接使用建立时的凭据，到期后应携带新凭据重连。

## 解绑与重新配对

- 设备可携带设备 JWT 调用 `POST /api/v1/aiot/devices/binding/unbind`，成功响应的 `data` 包含 `status: "unbound"` 和 `deviceId`。
- 操作者可在智控台点 **解除绑定**，对应 `POST /api/v2/rodak/devices/{id}/unbind?productId=7`，需要相应产品权限。
- 解绑增加绑定版本，旧 JWT 随即失效。平台会结束旧语音连接，并在 MQTT 启用时处理旧客户端断开。重新连接前应重新配对。绑定相关的 AIoT 影子和未完成命令只在对应绑定版本内有效。
- 如需更换产品，在原产品下解绑，然后用新配对码选择目标产品。

绑定完成后可继续查看 [AIoT 数据协议](aiot.md) 和 [实时语音协议](voice.md)。
