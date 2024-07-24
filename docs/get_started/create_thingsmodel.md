## 创建物模型
物模型是指产品的功能、属性、事件、服务等。本物联网平台提供了物模型创建接口，方便用户使用物模型能力。  
请注意设备为agent控制的控制单位，一个产品下可以创建多个物模型，每个物模型下可以创建多个属性，事件，功能。  
一个物模型可以绑定多个设备，agent可以精确识别每个设备，并对其进行分别控制。
> 使用物模型之前需要先创建产品，创建产品之后，才能创建物模型。（产品是agent控制的命名空间，即不同产品默认被隔离，提高了平台的安全性。） 

以下是物模型接口的参数列表：
* String name; // 物模型名称（请注意，该名称为agent智能体控制时候的显示名称，必须保证其唯一性）  
* int productId;// 产品id  
* String description;// 物模型描述（请认真描述该物模型的场景和用途，例子：空调物模型，你应该这样描述：可以制暖和制冷，放置于客厅）

> 创建完物模型之后需要添加其属性，事件，功能，目前仅支持添加属性，后续将会支持添加事件，功能。

以下是物模型属性接口的参数列表：  
private String jsonKey; // 属性名称  
private int modelId; // 物模型id  
private String description; // 属性描述（请注意，该描述应该具体描述该属性的范围和可取值，这个描述影响agent智能体的理解。例如：温度属性，描述应该为：温度范围0-100，单位为摄氏度）   
private String storageType; // 属性存储类型   
private int rRw;// 属性读写类型，0表示只读，1表示可读写  
private String type;// 属性类型，目前支持：int、float、string（后续将支持更多的属性类型） 
## 设备接入
有了物模型并添加好属性，事件，功能后之后，就可以将设备接入物联网平台了。本平台默认使用mqtt协议接入，并且支持ssl加密。
设备接入物联网平台需要先创建设备，创建设备之后，就可以通过mqtt协议进行设备接入。