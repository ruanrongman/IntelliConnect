## 什么是mcp协议
MCP 是由 Anthropic 于 2024 年 11 月推出并开源的一种开放标准协议，旨在实现大型语言模型（LLM）与外部数据源、工具和服务之间的无缝通信。
它将模型与各种资源、API、插件的集成抽象为统一的接口，就像 HTTP 之于网页、USB-C 之于外设，为 AI 应用提供了“即插即用”的上下文管理框架。
详情请参考：https://www.anthropic.com/news/model-context-protocol
本项目支持mcp协议，使用mcp协议可以自由扩展智能体能力，使得平台的智能体能力更加丰富。

## 如何添加MCP服务器
* 1：使用SSE方式添加MCP服务器
>平台支持添加SSE MCP服务器，可以使用以下指令进行添加（以高德地图为例）,token获取方法参见快速开始章节：

```bash
curl -X POST "http://localhost:8080/api/v2/mcpServer" 
-H "accept: */*" 
-H "Authorization: {token}" 
-H "Content-Type: application/json" 
-d "{\"description\":\"mcp-gaode-maps\",\"productId\":{your productId},\"sseEndpoint\":\"https://mcp.amap.com\",\"url\":\"/sse?key={your key}\"}"
```
使用时候要替换{your productId}和{your key}
具体key配置请参见高德地图文档：https://lbs.amap.com/api/mcp-server/summary

* 2：使用小智上下文MCP

若您正在使用ic平台的小智接入服务，可直接在小智上下文中注册MCP服务器，ic平台将自动发现这些服务器，无需额外配置。当前暂不支持通过小智接入点使用MCP协议，该功能正在开发中，请关注后续更新。

## 如何开发自己的MCP服务器(SSE协议)
MCP服务器的开发支持多种语言，Anthropic官方推出了多种语言的SDK，包括我们熟悉的python和java语言
下面以python为例子介绍如何开发mcp服务器，官方文档请参考：https://modelcontextprotocol.io/introduction
```python
from fastmcp import FastMCP
from docx import Document

mcp = FastMCP("walkerAPI-mcp-server")


# 官方演示方法1
@mcp.tool()
def add(a: int, b: int) -> int:
    """Add two numbers"""
    return a + b


@mcp.tool()
def text(a: str) -> str:
    """Write the provided text into a Word document and save it as 'output.docx'"""
    try:
        # 创建Word文档对象
        doc = Document()
        # 添加文本段落
        doc.add_paragraph(a)
        # 保存文档
        filename = "output.docx"
        doc.save(filename)
        return f"Text successfully saved to {filename}"
    except Exception as e:
        # 异常处理
        return f"Error saving document: {str(e)}"


# 官方演示方法2
@mcp.tool()
def calculate_bmi(weight_kg: float, height_m: float) -> float:
    """Calculate BMI given weight in kg and height in meters"""
    return weight_kg / (height_m ** 2)


# 本地启动方式
if __name__ == "__main__":
    mcp.run(
        transport="sse",
        port=8000
    )

```
本项目依赖了fastmcp库，该库是Anthropic官方提供的mcp服务器开发库，使用前请先安装fastmcp库,具体
安装依赖请参考https://github.com/modelcontextprotocol/python-sdk
运行方式：
```bash
uv run mcp
```
运行后，会启动一个sse服务器，监听在8000端口。如果想让ic平台使用该MCP服务器则按照上述描述添加地址即可。
添加后平台就会识别该MCP服务器，并将其能力添加到平台智能体中。
## 开发小智上下文MCP服务器
从1.7.6版本小智开始，MCP协议取代了原来的IOT协议成为了，原有的 type: "iot" 方案已废弃。ESP32设备
开发者可通过以下方式接入平台。
## 典型使用流程

1. 设备启动后通过基础协议（如 WebSocket/MQTT）与后台建立连接。
2. 后台通过 MCP 协议的 `initialize` 方法初始化会话。
3. 后台通过 `tools/list` 获取设备支持的所有工具（功能）及参数说明。
4. 后台通过 `tools/call` 调用具体工具，实现对设备的控制。

详细协议格式与交互请见虾哥的 [`mcp-protocol.md`](https://github.com/78/xiaozhi-esp32/blob/main/docs/mcp-protocol.md)。

## 设备端工具注册方法说明

设备通过 `McpServer::AddTool` 方法注册可被后台调用的"工具"。其常用函数签名如下：

```cpp
void AddTool(
    const std::string& name,           // 工具名称，建议唯一且有层次感，如 self.dog.forward
    const std::string& description,    // 工具描述，简明说明功能，便于大模型理解
    const PropertyList& properties,    // 输入参数列表（可为空），支持类型：布尔、整数、字符串
    std::function<ReturnValue(const PropertyList&)> callback // 工具被调用时的回调实现
);
```
- name：工具唯一标识，建议用"模块.功能"命名风格。
- description：自然语言描述，便于 AI/用户理解。
- properties：参数列表，支持类型有布尔、整数、字符串，可指定范围和默认值。
- callback：收到调用请求时的实际执行逻辑，返回值可为 bool/int/string。

## 典型注册示例（以 ESP-Hi 为例）

```cpp
void InitializeTools() {
    auto& mcp_server = McpServer::GetInstance();
    // 例1：无参数，控制机器人前进
    mcp_server.AddTool("self.dog.forward", "机器人向前移动", PropertyList(), [this](const PropertyList&) -> ReturnValue {
        servo_dog_ctrl_send(DOG_STATE_FORWARD, NULL);
        return true;
    });
    // 例2：带参数，设置灯光 RGB 颜色
    mcp_server.AddTool("self.light.set_rgb", "设置RGB颜色", PropertyList({
        Property("r", kPropertyTypeInteger, 0, 255),
        Property("g", kPropertyTypeInteger, 0, 255),
        Property("b", kPropertyTypeInteger, 0, 255)
    }), [this](const PropertyList& properties) -> ReturnValue {
        int r = properties["r"].value<int>();
        int g = properties["g"].value<int>();
        int b = properties["b"].value<int>();
        led_on_ = true;
        SetLedColor(r, g, b);
        return true;
    });
}
```

## 常见工具调用 JSON-RPC 示例

### 1. 获取工具列表
```json
{
  "jsonrpc": "2.0",
  "method": "tools/list",
  "params": { "cursor": "" },
  "id": 1
}
```

### 2. 控制底盘前进
```json
{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "self.chassis.go_forward",
    "arguments": {}
  },
  "id": 2
}
```

### 3. 切换灯光模式
```json
{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "self.chassis.switch_light_mode",
    "arguments": { "light_mode": 3 }
  },
  "id": 3
}
```

### 4. 摄像头翻转
```json
{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "self.camera.set_camera_flipped",
    "arguments": {}
  },
  "id": 4
}
```

## 备注
- 工具名称、参数及返回值请以设备端 `AddTool` 注册为准。
- 推荐所有新项目统一采用 MCP 协议进行物联网控制。
- 详细协议与进阶用法请查阅 [`mcp-protocol.md`](https://github.com/78/xiaozhi-esp32/blob/main/docs/mcp-protocol.md)。 