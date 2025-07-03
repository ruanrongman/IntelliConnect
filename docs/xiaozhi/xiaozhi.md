## 什么是AI小智
AI小智是由虾哥研发的一款以情感交互为核心的开源AI语音对话系统，它通过多模态交互、低延迟响应和开源生态，实现了从“工具”到“情感伙伴”的跨越。
其终端代码完全开源，并由开源社区提供了多种设备客户端，适合作为aiot平台的语音终端接入。

## 如何将AI小智接入本平台
> 注意：本平台使用的是AI小智的开源终端代码，AI小智的终端代码用于商业用途请联系作者获取授权。
> 以下接入属于开发者使用教程，如需简单体验请直接使用本平台提供的智控台进行体验。
* 1. 获取AI小智终端代码：[https://github.com/78/xiaozhi-esp32](https://github.com/78/xiaozhi-esp32)
* 2. 根据文档进行终端代码的编译和烧录，并替换成自己的WiFi和OTA服务器地址，详情可以参考小智的百科全书。
     本平台的OTA地址为[http://localhost:8080/xiaozhi/ota/](http://localhost:8080/xiaozhi/ota/)
     接入该地址后，如果AI小智没有绑定就会自动播报验证码。
* 3.修改配置文件OTA的websocket更新地址（如需远程部署，否则可以忽略）
```yaml
ota:
  bin:
    path: D://temp-rainy//
  xiaozhi:
    url: ws://127.0.0.1:8080(改成你部署的域名)
```

3. 通过下面接口进行绑定

```bash
curl -X POST "http://localhost:8080/api/v2/xiaozhi/otaManage" \
   --header 'Authorization: {your token}' \
   -H "accept: */*" \
   -H "Content-Type: application/json" \
   -d "{\"code\":\"{你的验证码}\",\"productId\":0}"
```
其中code为验证码，productId为产品id。 
至此小智AI应该绑定成功后，AI小智就会开始工作，并可以无缝对接本平台的全部功能，使用其多智能体的能力。