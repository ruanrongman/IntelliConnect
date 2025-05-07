## 创建物模型
物模型是指产品的功能、属性、事件、服务等。本物联网平台提供了物模型创建接口，方便用户使用物模型能力。  
请注意设备为agent控制的控制单位，一个产品下可以创建多个物模型，每个物模型下可以创建多个属性，事件，功能。  
一个物模型可以绑定多个设备，agent可以精确识别每个设备，并对其进行分别控制。
> 使用物模型之前需要先创建产品，创建产品之后，才能创建物模型。（产品是agent控制的命名空间，即不同产品默认被隔离，提高了平台的安全性。） 

以下是物模型接口的参数列表：
* String name; // 物模型名称（请注意，该名称为agent智能体控制时候的显示名称，必须保证其唯一性）  
* int productId;// 产品id  
* String description;// 物模型描述（请认真描述该物模型的场景和用途，例子：空调物模型，你应该这样描述：可以制暖和制冷，放置于客厅）

> 创建完物模型之后需要添加其属性，事件，功能（同步或异步模式），当前已完整支持物模型全部功能。

以下是物模型属性接口的参数列表：  
* private String jsonKey; // 属性名称  
* private int modelId; // 物模型id  
* private String description; // 属性描述（请注意，该描述应该具体描述该属性的的控制和读取的细节，这个描述影响agent智能体的理解，最好提供一些few shot。）   
* private String storageType; // 属性存储类型   
* private int rRw;// 属性读写类型，0表示只读，1表示可读写  
* private String type;// 属性类型，目前支持：int、float、string（后续将支持更多的属性类型）   
* private String max;// 属性最大值  (默认为null)     
* private String min;// 属性最小值  (默认为null)   
* private String step;// 属性步长  (默认为null)   
* private String unit;// 属性单位  (默认为null)

### 物模型属性通讯相关MQTT主题：
* 属性上报主题：/oc/devices/{device_name}/sys/properties/report 
* 属性上报响应主题：/oc/devices/{device_name}/sys/properties/report_reply 
* 平台下发主题：/oc/devices/{device_name}/sys/properties/update 
> 物模型功能模块适用于更复杂的场景，例如：工作模式、复合指令等。

以下是物模型功能接口的参数列表：
* private String functionName; // 功能名称
* private String jsonKey; // 功能参数名称
* private int modelId; // 物模型id
* private string dataType; // input or output参数类型，input表示输入参数，output表示输出参数
* private String description; // 功能描述（请注意，该描述应该具体描述该功能参数的作用，这个描述影响agent智能体的理解，最好提供一些few shot。）
* private String type;// 服务参数类型，目前支持：int、float、string（后续将支持更多的属性类型）
* private String max;// 服务参数最大值  (默认为null)
* private String min;// 服务参数最小值  (默认为null)
* private String step;// 服务参数步长  (默认为null)
* private String unit;// 服务参数单位  (默认为null)

### 物模型功能通讯相关MQTT主题：
* 功能下发主题：/oc/devices/{devicename}/sys/services/invoke
* 功能参数主题：/oc/devices/{devicename}/sys/services/report
* 功能响应主题：/oc/devices/{devicename}/sys/services/report_reply
* 异步参数响应主题：/oc/devices/{devicename}/sys/services/async_outputData

> 物模型事件模块适用于设备告警，用于通知平台或其他系统设备发生了需要及时处理或通知的状况。这些信息无法通过查询设备属性得知，通常具有突发性和重要性。

以下是物模型提交事件入参接口的参数列表（事件接口由两组接口组成，提交事件和提交事件入参组成）：
* private String description // 事件描述 （请注意，该描述应该具体描述该事件参数的作用，这个描述影响agent智能体的理解，最好提供一些few shot。）
* private String jsonKey; // 功能参数名称 
* private int modelId; // 物模型id
* private String type;// 事件参数类型，目前支持：int、float、string（后续将支持更多的属性类型）

以下是物模型事件接口的参数列表（事件接口由两组接口组成，提交事件和提交事件入参组成）：
* private String description // 事件描述 （请注意，该描述应该具体描述该事件参数的作用，这个描述影响agent智能体的理解，最好提供一些few shot。）
* private int modelId; // 物模型id
* private String name; // 事件名称

上述接口联合使用，提交事件和提交事件入参接口，提交事件入参接口用于提交事件入参，提交事件接口用于创建事件。

## 物模型事件通讯相关MQTT主题：
* 事件上报主题：/oc/devices/{devicename}/sys/events/report
* 事件响应主题：/oc/devices/{devicename}/sys/events/event_reply

## 设备接入
有了物模型并添加好属性，事件，功能后之后，就可以将设备接入物联网平台了。本平台默认使用`MQTT`协议接入，并且支持`SSL`加密。
设备接入物联网平台需要先创建设备，创建设备之后，就可以通过mqtt协议进行设备接入。

## 同步和异步功能
物模型支持同步和异步两种模式，同步模式下，平台下发指令后，设备应立即执行，并返回执行结果。如果执行结果在8秒内没有返回，平台将认为设备执行失败。
异步模式下，平台下发指令后，设备可以异步执行，并返回执行结果。（适用于长时间执行需要等待的场景）
同步和异步模式均可以通过mqtt服务响应主题获取信息。

## 使用Agent控制设备
使用Agent控制设备，需要先按照上述流程进行设备接入，成功后请查看控制台的设备列表，确保设备已经接入。并配置application.yaml中
的key。请仔细考虑具体项目所需的大模型，不同的大模型会影响平台的运行效果。

## 用https协议控制设备
平台支持使用https协议来控制设备，以下是调用其api进行物模型服务调用一个例子：
先获取设备的token，并使用以下指令进行控制。
```bash
curl --location 'http://localhost:8080/api/v2/login' \
--header 'Content-Type: application/json' \
--data '{"username":"{username}","password":"{password}"}'
```
控制指令
```bash
curl -X POST "http://localhost:8080/api/v2/control" \
-H "accept: */*" \
-H "Authorization: Bearer XXXX" \
-H "Content-Type: application/json" \
-d "{\"key\":[\"time\"],\"mode\":\"service\",\"functionName\":\"time_set\",\"name\":\"{device_name}\",\"qos\":1,\"status\":\"sync\",\"value\":[\"off\"]}"
```
通过以上指令你可以向你的设备发起物模型同步服务调用，并控制其以`MQTT`协议 `qos=1`的信息质量来进行通信，通过该接口你可以灵活地实现各种设备应用需求。
参数含义：
* key：表示要操作的属性名称，多个属性用逗号分隔。
* mode：表示要操作的模式，目前支持：attribute（属性）、service（服务）。
* functionName：表示要操作的服务名称，多个服务用逗号分隔，没有的服务为空字符串。
* name：表示要操作的设备名称。
* qos：表示要操作的设备的QoS等级，可选值为0、1、2。
* status：表示要操作的设备的服务状态，可选值为sync（同步）、async（异步）。
* value：表示要操作的设备的属性值，多个属性值用逗号分隔，没有的属性值为空字符串。
以下是一个简单的Python脚本，用于使用requests库发送HTTP POST请求，并使用JSON格式的数据进行通信。
```python
import requests
import json
def send_http_request():
    url = "http://localhost:8080/api/v2/control"
    headers = {"accept": "*/*", "Authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiW1JPTEVfZ3Vlc3RdIiwiZXhwIjoxNzQyMjg1NTQ0LCJpYXQiOjE3NDIxOTkxNDQsInVzZXJuYW1lIjoid3VrYWkifQ.hrzZ0ShssfMTol7qOuNYSWY9BlxV2gIYPaT1GEYH4XM", "Content-Type": "application/json"}
    data = {}
    data["key"] = ["time"]
    data["mode"] = "service"
    data["functionName"] = "time_set"
    data["name"] = "{device_name}"
    data["qos"] = 1
    data["status"] = "sync"
    data["value"] = ["off"]
    response = requests.post(url, headers=headers, data=json.dumps(data))
    print(response.text)
if __name__ == '__main__':
    send_http_request()
```
请注意，上述代码中的`{device_name}`和`{username}`和`{password}`需要替换为实际的设备名称和用户名密码。

## 获取事件记录
平台支持获取事件记录，可以使用以下指令进行获取,token获取方法如上述所示：
```bash
curl --location 'http://localhost:8080/api/v2/readEvent' \
-H "Authorization: Bearer XXXX"\
--header 'Content-Type: application/json' \
--data '{
  "jsonKey": "power",
  "name": "light1",
  "time1": 0,
  "time2": {当前毫秒时间戳}
}'
```
参数含义：
* jsonKey：表示要读取的事件参数名称。
* name：表示要读取的设备名称。
* time1：表示要读取的起始时间，单位为毫秒。
* time2：表示要读取的结束时间，单位为毫秒，表示读取当前时间之前的所有事件。