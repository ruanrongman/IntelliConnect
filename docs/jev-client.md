# Jev 独立客户端与内置工具接入

Jev 使用 TypeSafe 的同步评估接口 `POST /v1/systemone`，支持判断（noul）、分类（choice）和评分（score）。Java 封装既可以由业务自行注入，也已经被内置的 `EmotionTool` 和 `YouthProtectionTool` 使用。

参考：[API](https://docs.typesafe.ai/api)、[模型](https://docs.typesafe.ai/models)。

## 配置

在 `application.yaml` 的 `ai` 下配置：

```yaml
ai:
  jev:
    base-url: https://api.typesafe.ai
    key: ${JEV_API_KEY:}
    model: jev-latest
```

三个字段通过 Spring 绑定到 `JevProperties`。Key 可由环境变量 `JEV_API_KEY` 提供；未配置时不影响应用启动，但调用会抛出 `IllegalStateException`。模型可改为官方支持的具体版本，以固定模型行为。配置在应用启动时读取，修改后重启生效。

`base-url` 可为服务根地址或以 `/v1` 结尾的地址，均可带末尾斜杠；不要填写完整 `/systemone` 地址。代理地址可带路径前缀。

## 内置工具

当配置值为 `jev-latest` 时，内置工具会使用类型安全的 Jev 客户端：

```yaml
ai:
  emotionTool-llm: jev-latest
  youthProtectionTool-llm: jev-latest
```

`EmotionTool` 根据当前问题优先级、最近对话和长期记忆选择情绪；异常、服务繁忙、空答案或未知情绪会回退到 `neutral`。`YouthProtectionTool` 使用 `allow`、`block`、`guardian_consent` 三种决定审核角色内容；调用失败或返回不合规时按保护性策略拒绝。青少年审核与最近两小时会话限制的配置、范围和行为详见 [青少年保护、Jev 与 Laya 情绪能力](ai_safety_and_emotion.md)。

`laya-jev` 提供兼容同一协议的本地服务，因此可以将 `ai.jev.base-url` 指向 `http://127.0.0.1:8001`，继续使用 `jev-latest` 为 EmotionTool 提供本地 Laya 情绪识别。具体安装、CPU/GPU 启动和健康检查见 [Laya Jev 兼容服务](../laya-jev/README.md)。

## 调用示例

通过构造器注入 `JevClient`，一次提交多种问题：

```java
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import top.rslly.iot.utility.ai.jev.JevClient;
import top.rslly.iot.utility.ai.jev.JevQuestion;
import top.rslly.iot.utility.ai.jev.JevResponse;

@Service
public class TicketEvaluator {
  private final JevClient jevClient;

  public TicketEvaluator(JevClient jevClient) {
    this.jevClient = jevClient;
  }

  public JevResponse evaluate(String text) {
    return jevClient.evaluate(
        Map.of("ticket", text),
        Map.of(
            "urgent", JevQuestion.noul("用户是否表达了紧急处理的需求？"),
            "department", JevQuestion.choice("应由哪个部门处理？", Map.of(
                "billing", "支付、账单、退款",
                "technical", "故障、集成、服务不可用")),
            "frustration", JevQuestion.score("用户的愤怒程度如何？",
                List.of("平静", "不满", "非常愤怒"))));
  }
}
```

读取结果：

```java
JevResponse.NoulAnswer urgent = (JevResponse.NoulAnswer) result.answers().get("urgent");
double urgencyProbability = urgent.noul(); // 保留概率，由业务自行决定阈值
JevResponse.ChoiceAnswer department =
    (JevResponse.ChoiceAnswer) result.answers().get("department");
String selected = department.choice();
Map<String, Double> distribution = department.probabilities();
double confidence = department.confidence();
JevResponse.ScoreAnswer frustration =
    (JevResponse.ScoreAnswer) result.answers().get("frustration");
double score = frustration.score(); // 概率加权分数，可以不是整数
String actualModel = result.model();
long inputTokens = result.usage().inputTokens();
long outputTokens = result.usage().outputTokens();
```

`state` 和 `instructions` 支持字符串、对象（如 Map）和数组（如 List）；内容应是文本或文本的结构化表示。choice 的 criteria 支持 1–255 个选项，描述可为字符串、对象、数组或 null；含 null 时请用 `LinkedHashMap` 等允许 null 的容器，不能用 `Map.of`。score 接受 2–10 个有序等级。noul 的可选 criteria 使用字符串键 `"true"`、`"false"`。

## 错误与重试

- 参数错误抛出 `IllegalArgumentException`，配置缺失抛出 `IllegalStateException`，均在发起网络请求前检查。
- HTTP、网络、响应协议错误和中断抛出 `JevException`。`getStatusCode()` 返回已收到的 HTTP 状态（无响应时为 null），`getResponseBody()` 提供可检查的响应正文；异常消息不包含该正文。
- 429、529 最多重试两次，默认等待 1 秒、2 秒；有效的 `Retry-After` 秒数或 HTTP 日期优先。其他 HTTP 错误及网络错误不做客户端重试。
- 重试等待可以被线程中断，中断标记会保留。调用前已中断的线程不会发送请求。
- 使用已有 `HttpRequestUtils` 的 OkHttp 连接池与默认超时设置；每次响应在解析后或重试等待前关闭。不自动记录 Key、请求正文或错误响应正文。

客户端不支持通用聊天、SSE、工具调用或图像输入。真实服务的权限、可用性及业务准确率需使用实际账户单独验证；本地 HTTP 测试不代表已完成线上验证。

## 真实服务测试

`JevClientLiveTest` 默认跳过，显式设置 `-Djev.live=true` 后才会调用配置的真实服务。测试读取正常的 `application.yaml` 配置及 Spring 环境覆盖值，只初始化 Jev 相关组件，不连接数据库或 MQTT。

配置好 `ai.jev.key`（默认来自 `JEV_API_KEY`）后，用 JDK 21 在项目根目录执行：

```powershell
$env:JAVA_HOME='C:\Users\Lenovo\.jdks\ms-21.0.9'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd '-Dtest=JevClientLiveTest' '-Djev.live=true' test
```

一次评估同时提交 noul、choice、score 三种问题，检查答案类型、概率/分数范围及 token 用量。测试不固定具体分数或分类结果，避免将模型输出变化误判为协议故障。429/529 仍遵循客户端的重试策略；测试总时限为 90 秒。显式启用后，Key 缺失、网络失败或服务错误都会使测试失败，不会被当作跳过或成功。

成功时只输出实际模型版本、答案数量及 token 用量，不输出 Key、请求正文或原始响应。
