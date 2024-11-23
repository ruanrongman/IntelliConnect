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

> 物模型功能模块适用于更复杂的场景，例如：工作模式、复合指令等。

以下是物模型功能接口的参数列表：
* private String jsonKey; // 功能名称
* private int modelId; // 物模型id
* private string dataType; // input or output参数类型，input表示输入参数，output表示输出参数
* private String description; // 功能描述（请注意，该描述应该具体描述该功能的作用，这个描述影响agent智能体的理解，最好提供一些few shot。）
* private String type;// 服务参数类型，目前支持：int、float、string（后续将支持更多的属性类型）
* private String max;// 服务参数最大值  (默认为null)
* private String min;// 服务参数最小值  (默认为null)
* private String step;// 服务参数步长  (默认为null)
* private String unit;// 服务参数单位  (默认为null)

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