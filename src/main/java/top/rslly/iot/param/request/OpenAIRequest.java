package top.rslly.iot.param.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * OpenAI API 请求格式解析类
 * 使用Lombok注解生成构造函数，支持Fastjson等JSON库解析
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenAIRequest {

    /**
     * 模型名称，例如：gpt-3.5-turbo, gpt-4
     */
    private String model;

    /**
     * 消息列表
     */
    private List<Message> messages;

    /**
     * 生成的最大token数
     */
    @JsonProperty("max_tokens")
    private Integer maxTokens;

    /**
     * 温度参数，控制随机性 (0-2)
     */
    private Double temperature;

    /**
     * 核采样参数 (0-1)
     */
    @JsonProperty("top_p")
    private Double topP;

    /**
     * 生成数量
     */
    private Integer n;

    /**
     * 是否流式输出
     */
    private Boolean stream;

    /**
     * 停止词
     */
    private List<String> stop;

    /**
     * 频率惩罚 (-2.0 到 2.0)
     */
    @JsonProperty("frequency_penalty")
    private Double frequencyPenalty;

    /**
     * 存在惩罚 (-2.0 到 2.0)
     */
    @JsonProperty("presence_penalty")
    private Double presencePenalty;

    /**
     * 用户标识
     */
    private String user;

    /**
     * 函数调用相关参数
     */
    private List<Function> functions;

    /**
     * 函数调用控制
     */
    @JsonProperty("function_call")
    private Object functionCall;

    /**
     * logit偏置
     */
    @JsonProperty("logit_bias")
    private Map<String, Integer> logitBias;

    /**
     * 响应格式
     */
    @JsonProperty("response_format")
    private ResponseFormat responseFormat;

    /**
     * 消息类 - 内部静态类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {

        /**
         * 角色：system, user, assistant, function
         */
        private String role;

        /**
         * 消息内容
         */
        private String content;

        /**
         * 函数名称（当role为function时）
         */
        private String name;

        /**
         * 函数调用参数
         */
        @JsonProperty("function_call")
        private FunctionCall functionCall;
    }

    /**
     * 函数调用类 - 内部静态类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FunctionCall {

        /**
         * 函数名称
         */
        private String name;

        /**
         * 函数参数（JSON字符串）
         */
        private String arguments;
    }

    /**
     * 函数定义类 - 内部静态类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Function {

        /**
         * 函数名称
         */
        private String name;

        /**
         * 函数描述
         */
        private String description;

        /**
         * 函数参数（JSON Schema格式）
         */
        private Object parameters;
    }

    /**
     * 响应格式类 - 内部静态类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseFormat {

        /**
         * 格式类型：text, json_object
         */
        private String type;

        /**
         * JSON Schema（当type为json_object时可选）
         */
        private Object jsonSchema;
    }

    /**
     * 工具类 - 内部静态类（用于function_calling扩展）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Tool {

        /**
         * 工具类型：function
         */
        private String type;

        /**
         * 函数定义
         */
        private Function function;
    }

    /**
     * 工具选择类 - 内部静态类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolChoice {

        /**
         * 选择类型：none, auto, function
         */
        private String type;

        /**
         * 函数选择（当type为function时）
         */
        private FunctionChoice function;
    }

    /**
     * 函数选择类 - 内部静态类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FunctionChoice {

        /**
         * 函数名称
         */
        private String name;
    }

    /**
     * 种子参数 - 用于可重现的输出
     */
    private Integer seed;

    /**
     * 工具列表
     */
    private List<Tool> tools;

    /**
     * 工具选择策略
     */
    @JsonProperty("tool_choice")
    private Object toolChoice;

    /**
     * 助手回复中的思考日志
     */
    @JsonProperty("logprobs")
    private Boolean logprobs;

    /**
     * 返回最可能的token的logprobs数量
     */
    @JsonProperty("top_logprobs")
    private Integer topLogprobs;

    /**
     * 服务额外参数
     */
    private Map<String, Object> extraParams;
}