## 什么是Ota升级
Ota升级，也叫Over The Air，即通过无线传输升级，是一种软件升级方式，通过手机或电脑将软件升级包上传到服务器，
在设备端通过网络连接服务器，下载升级包，并进行升级。

## Ota mqtt通讯主题
* Ota升级主题：/oc/devices/{deviceName}/sys/ota  
替换{deviceName}为你的设备名称。

## Ota升级流程
* 1.上传软件升级包到服务器，请使用以下命令进行升级：

```bash
curl --location 'http://localhost:8080/api/v2/otaUpload?name={name}&productId={productId}' \
--header 'Authorization: {your token}' \
--form 'file=@"{file_path}"'
```
替换{file_path}为升级包路径，{your token}为token,{name}为升级包名称，{productId}为产品id。

* 2.发起升级请求，请使用以下命令进行升级：

```bash
curl --location --request POST 'http://localhost:8080/api/v2/otaEnable?name=serialTest&deviceName={deviceName}' \
--header 'Authorization: {your token}'
```
替换{deviceName}为设备名称，{your token}为token。

* 3.设备端通过网络连接mqtt服务器，订阅ota升级主题下载升级包，并进行升级。

服务端将会通过主题返回下载升级包的信息，如下所示：
```json
{ 
  "fileName":"xxx.bin",
  "version":"ae3c773e-1b8c-4388-84f1-ef5a1ea602a1",
  "md5":"c4c42785415e99d01c4814cd344d880e"
}
```

* 4.设备端通过网络连接，下载升级包，并进行升级。
  【下载升级包地址】：http://localhost:8080/api/v2/micro/{fileName}
