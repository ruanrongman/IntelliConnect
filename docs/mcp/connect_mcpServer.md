## 什么是mcp协议
MCP 是由 Anthropic 于 2024 年 11 月推出并开源的一种开放标准协议，旨在实现大型语言模型（LLM）与外部数据源、工具和服务之间的无缝通信。
它将模型与各种资源、API、插件的集成抽象为统一的接口，就像 HTTP 之于网页、USB-C 之于外设，为 AI 应用提供了“即插即用”的上下文管理框架。
详情请参考：https://www.anthropic.com/news/model-context-protocol

## 如何添加mcp服务器
平台支持添加mcp服务器，可以使用以下指令进行添加（以高德地图为例）,token获取方法参见快速开始章节：
```bash
curl -X POST "http://localhost:8080/api/v2/mcpServer" 
-H "accept: */*" 
-H "Authorization: {token}" 
-H "Content-Type: application/json" 
-d "{\"description\":\"mcp-gaode-maps\",\"productId\":{your productId},\"sseEndpoint\":\"https://mcp.amap.com\",\"url\":\"/sse?key={your key}\"}"
```
使用时候要替换{your productId}和{your key}
具体key配置请参见高德地图文档：https://lbs.amap.com/api/mcp-server/summary

## 如何开发自己的mcp服务器
mcp服务器的开发支持多种语言，Anthropic官方推出了多种语言的SDK，包括我们熟悉的python和java语言
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
运行后，会启动一个sse服务器，监听在8000端口。如果想让ic平台使用该mcp服务器则按照上述描述添加地址即可。