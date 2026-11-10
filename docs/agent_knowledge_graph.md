# Agent 知识图谱工具

知识图谱页面的“知识图谱自动生成开关”同时控制 Agent 的图谱能力。开启后，Agent 可读取、检索、从文件生成、查询生成进度、整图清空，以及导出 Mermaid、JSON。关闭后工具入口仍保留并标明关闭状态，两种 Router 仍将图谱请求交给 Agent；工具描述要求 Agent 优先检查开关，不预先扫描或搬运文件。所有 Agent 图谱调用都会被拒绝并提示“知识图谱自动生成开关已关闭”，引导用户到知识图谱页面开启开关后重试。提示通过现有工具结果流程交给模型，不在 Agent 执行循环中增加特殊终止逻辑。运行中的 Agent 生成任务在下一个检查点停止后续写入，已写入数据保留。页面手动操作不受此限制。

## 使用示例

可以直接向 Agent 提出以下请求：

- “用当前工作区里的设备说明书生成知识图谱，完成后把 Mermaid 和 JSON 都保存到工作区。”
- “把沙箱 `/workspace/reports/design.md` 导入知识图谱，然后把 Mermaid 保存到 `/workspace/exports/design.mmd`。”
- “读取当前产品知识图谱，查找温度传感器与空调之间的关系。”
- “清空当前产品的知识图谱。”

图谱属于当前产品，因此同一产品的各会话读取同一份图谱；源文件、生成任务进度和输出文件限定在当前会话内。整图清空会清理节点、属性、关系和对应向量数据。

Agent 不能直接修改节点描述、属性或关系，也不能单独增删节点和关系。文件生成复用页面上传的合并规则，允许补充已有图谱内容，不执行抽取结果中的删除指令。Agent 有生成或清空任务运行时，同一产品的其他 Agent 导入、清空请求返回忙碌；页面手动操作和既有对话自动生成不纳入此互斥范围。

## 工具调用协议

工具名为 `knowledgeGraphicAgentTool`。ReAct 与 Function Calling 共用相同描述；参数为一个 JSON 操作对象，不接收 `productId`、`chatId` 或调用方提供的图谱内容。

```json
{"action":"read"}
{"action":"query","query":"温度传感器控制哪些设备"}
{"action":"import","sources":[{"source":"workspace","fileId":"真实文件ID"},{"source":"sandbox","path":"/workspace/input.md"}]}
{"action":"progress","taskId":"导入返回的任务ID"}
{"action":"export","format":"mermaid","target":"workspace","name":"设备图谱.mmd"}
{"action":"export","format":"mermaid","target":"sandbox","path":"/workspace/exports/设备图谱.mmd"}
{"action":"export","format":"json","target":"workspace"}
{"action":"clear"}
```

一次只能执行一个操作；需要多种格式或两个存储目标时分别调用。`import` 返回任务 ID，不表示生成完成；通过 `progress` 的 `finished=true`、`success=true` 确认成功后，再导出最终图谱。进度只保存在当前后端进程内，重启后不可恢复。

导入支持 PDF、TXT、Markdown、Word、PowerPoint、Excel 和 ZIP，最多 100 个来源。单文件大小使用 `ai.workspace.max-file-bytes`（默认 50 MiB）。ZIP 内容合计解压大小也受此限制，条目数最多 1000。输入不支持主机文件路径或任意 URL。

## 产物与存储

导出支持两种纯文本格式：

- `format: "mermaid"`：UTF-8 Mermaid 流程图源文件，扩展名 `.mmd`，以 `flowchart LR` 开头。包含完整节点名称、关系名称及方向，支持孤立节点、自环和多条关系。名称中的特殊字符转义，固定生成节点 ID；不接收 Agent 提供的 Mermaid 内容或脚本。节点描述和属性请使用 JSON 获取。
- `format: "json"`：沿用图谱数据结构，完整保留节点名称、描述、属性和关系。

Mermaid 内容由已持久化的产品图谱快照直接序列化，按节点、关系排序，输出稳定。空图谱导出有效的 Mermaid 空图及“暂无图谱数据”注释。导出只生成源文件；需要查看图形时，可使用支持 Mermaid 的编辑器。Agent 工具不再提供 PNG/SVG 导出，不执行渲染脚本，不依赖前端代码、浏览器、Python 绘图库或字体。

工作区保存返回 `fileId`、`name`、`size`、`downloadPath`，沿用工作区文件权限和配额。两种格式保存到工作区都不需要沙盒。工作区同名产物使用不同文件 ID 保存，不覆盖已有文件。

沙箱保存返回 `sandboxPath` 和 `size`，默认路径为 `/workspace/exports/` 下的唯一文件名。可指定相对 `/workspace` 的路径或完整路径，但禁止越界、符号链接及覆盖已有文件。`name` 与 `path` 同时提供时，文件名必须一致。沙箱不可用时明确失败，不自动改存其他位置。沙箱产物随沙箱回收而丢失，需长期保留时保存到工作区。

默认文件名包含产品 ID、时间和唯一后缀；Mermaid 使用 `.mmd`，JSON 使用 `.json`，自定义文件名必须匹配。导出开始及保存前检查原有开关，关闭后拒绝尚未保存的导出。文件大小和工作区配额继续沿用现有设置。生成或保存失败会返回错误，不报告成功。页面自身的图片下载保持原有行为。
