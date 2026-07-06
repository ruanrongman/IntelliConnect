/**
 * Copyright © 2023-2030 The ruanrongman Authors
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package top.rslly.iot.utility.ai.toolAgent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.rslly.iot.models.AdminConfigEntity;
import top.rslly.iot.services.AdminConfigServiceImpl;
import top.rslly.iot.services.UserConfigServiceImpl;
import top.rslly.iot.services.agent.LlmProviderInformationServiceImpl;
import top.rslly.iot.services.agent.ProductLlmModelServiceImpl;
import top.rslly.iot.utility.ai.*;
import top.rslly.iot.utility.ai.llm.LLM;
import top.rslly.iot.utility.ai.llm.LLMFactory;
import top.rslly.iot.utility.ai.llm.FunctionResult;
import top.rslly.iot.utility.ai.llm.FunctionStreamHandler;
import top.rslly.iot.utility.ai.llm.FunctionToolCallMessage;
import top.rslly.iot.utility.ai.llm.FunctionToolResultMessage;
import top.rslly.iot.utility.ai.llm.FunctionToolSpec;
import top.rslly.iot.utility.ai.prompts.ReactPrompt;
import top.rslly.iot.utility.ai.tools.BaseTool;
import top.rslly.iot.utility.ai.tools.ToolPrefix;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;

@Component
@Data
@Slf4j
public class Agent implements BaseTool<String> {
  private static final String EPOCH_LIMIT_CONFIG_KEY = "agent.epoch_limit";
  private static final int MAX_EPOCH_LIMIT = 100;

  @Autowired
  private Manage manage;
  @Autowired
  private ProductLlmModelServiceImpl productLlmModelService;
  @Autowired
  private LlmProviderInformationServiceImpl llmProviderInformationService;
  @Value("${ai.agent-llm}")
  private String llmName;
  @Value("${ai.agent-speedUp:true}")
  private boolean speedUp;
  @Autowired
  private ReactPrompt reactPrompt;
  @Autowired
  private DescriptionUtil descriptionUtil;
  @Autowired
  private LlmDiyUtility llmDiyUtility;
  @Value("${ai.agent-epoch-limit}")
  private int epochLimit = 8;
  @Value("${ai.agent.showThinking:true}")
  private boolean showThinking;
  @Value("${ai.agent.include-thought:false}")
  private boolean includeThought;
  @Autowired
  private AdminConfigServiceImpl adminConfigService;
  @Autowired
  private UserConfigServiceImpl userConfigService;
  @Value("${ai.router.mode:prompt}")
  private String routerMode;
  @Value("${ai.agent.mode:prompt}")
  private String agentMode;
  @Value("${ai.agent.function-timeout-seconds:60}")
  private long functionTimeoutSeconds;

  // 添加锁和条件变量映射（与ChatTool保持一致）
  private final Map<String, Lock> lockMap = new ConcurrentHashMap<>();
  private final Map<String, Condition> conditionMap = new ConcurrentHashMap<>();
  private final Map<String, String> dataMap = new ConcurrentHashMap<>();
  private final Map<String, FunctionResult> functionDataMap = new ConcurrentHashMap<>();

  private static final List<String> COMFORT_PHRASES = Arrays.asList(
      "嗯～",
      "哦～",
      "好～",
      "行～",
      "来啦～",
      "稍等～",
      "马上～",
      "正在～",
      "处理中～",
      "查询中～",
      "思考中～",
      "嗯哼～",
      "好的～",
      "收到～",
      "了解～");

  private String getRandomComfortPhrase() {
    return COMFORT_PHRASES.get(ThreadLocalRandom.current().nextInt(COMFORT_PHRASES.size()));
  }

  private boolean isQueueRemoved(String chatId, Map<String, Queue<String>> queueMap) {
    if (queueMap == null) {
      return false;
    }
    boolean removed = !queueMap.containsKey(chatId);
    if (removed) {
      log.info("检测到队列被移除，Agent 中断执行，chatId: {}", chatId);
    }
    return removed;
  }

  @Override
  public String run(String question) {
    return null;
  }

  public String run(String question, Map<String, Object> globalMessage) {
    if (isFunctionAgentMode()) {
      String functionResult = runFunctionCalling(question, globalMessage);
      if (functionResult != null) {
        return functionResult;
      }
      log.info("Agent function calling unavailable, fallback to ReAct prompt mode");
    }
    StringBuilder conversationPrompt = new StringBuilder();
    int productId = (int) globalMessage.get("productId");
    Map<String, Queue<String>> queueMap =
        (Map<String, Queue<String>>) globalMessage.get("queueMap");
    String chatId = (String) globalMessage.get("chatId");
    // 仅在 speedUp 模式下初始化同步资源
    if (speedUp) {
      lockMap.computeIfAbsent(chatId, k -> new ReentrantLock());
      conditionMap.computeIfAbsent(chatId, k -> lockMap.get(k).newCondition());
    }
    globalMessage.put("mcpIsTool", true);
    var queue = queueMap.get(chatId);
    if (queue != null) {
      queue.add(ToolPrefix.AGENT.getPrefix());
      if (!isFunctionRouterMode()) {
        queue.add(getRandomComfortPhrase());
      }
    }
    int currentEpochLimit = getEpochLimitConfig(productId);
    boolean includeThoughtEnabled = getIncludeThoughtConfig();
    String system =
        reactPrompt.getReact(descriptionUtil.getTools(productId, chatId), question, productId, 1,
            currentEpochLimit, includeThoughtEnabled);
    List<ModelMessage> messages = new ArrayList<>();
    String toolResult = "";
    conversationPrompt.append(system);
    // System.out.println("agent epoch limit" + epochLimit);
    log.info("agent epoch limit{}", currentEpochLimit);
    int iteration = 0;
    while (iteration < currentEpochLimit) {
      if (isQueueRemoved(chatId, queueMap)) {
        cleanupResources(chatId);
        return "操作已取消";
      }
      // 更新当前轮次信息
      if (iteration > 0) {
        conversationPrompt
            .append(String.format("\n## Current Step: %d of %d\n", iteration + 1,
                currentEpochLimit));
      }
      messages.clear();
      ModelMessage systemMessage =
          new ModelMessage(ModelMessageRole.SYSTEM.value(), conversationPrompt);
      ModelMessage userMessage = new ModelMessage(ModelMessageRole.USER.value(), question);
      messages.add(systemMessage);
      messages.add(userMessage);
      var obj =
          callLLMForThought(question, messages, queueMap, chatId, productId, includeThoughtEnabled);
      if (obj == null) {
        cleanupResources(chatId);
        return "小主人抱歉哦，服务器现在繁忙。";
      }
      String answer;
      try {
        answer = obj.getJSONObject("action").getString("answer");
      } catch (Exception e) {
        log.error("解析LLM响应失败: {}", e.getMessage());
        cleanupResources(chatId);
        return "解析LLM响应失败";
      }
      try {
        Map<String, String> res = process_llm_result(obj);
        if (res.get("action_name").equals("finish")) {
          cleanupResources(chatId);
          String args = res.get("action_parameters");
          String content = JSON.parseObject(args).getString("content");
          if (queue != null) {
            queue.add(content);
            queue.add("[DONE]");
          }
          return content;
        }
        toolResult =
            manage.runTool(res.get("action_name"), res.get("action_parameters"), globalMessage);
        conversationPrompt.append(String.format(
            "%s\nAction: %s(%s)\nObservation: %s\n",
            buildThoughtLine(res.get("thought")), res.get("action_name"),
            res.get("action_parameters"), toolResult));
        if (queue != null) {
          queue.add(ToolPrefix.getToolCallPrefix(res.get("action_name")));
        }
        log.info("Thought:{}", res.get("thought"));
        log.info("Observation:{}", toolResult);
      } catch (Exception e) {
        if (queue != null) {
          queue.add(answer);
          queue.add("[DONE]");
        }
        cleanupResources(chatId);
        return answer;
      }
      iteration += 1;
    }
    cleanupResources(chatId);
    // 超出迭代轮次，调用模型进行总结
    String summaryPrompt = "请根据以上对话历史和最终观察结果，总结回答最初的问题: " + question;
    messages.clear();
    messages.add(new ModelMessage(ModelMessageRole.SYSTEM.value(), summaryPrompt));
    messages.add(new ModelMessage(ModelMessageRole.USER.value(), conversationPrompt.toString()));

    try {
      toolResult = LLMFactory.getLLM(llmName).commonChat(summaryPrompt, messages, false);
      try {
        var temp = toolResult.replace("```json", "").replace("```JSON", "").replace("```", "")
            .replace("json", "");
        JSONObject jsonResponse = JSON.parseObject(temp);
        if (jsonResponse == null)
          throw new Exception("json parse error");
        try {
          toolResult =
              jsonResponse.getJSONObject("action").getJSONObject("args").getString("content");
        } catch (Exception ignore) {
          toolResult = jsonResponse.getJSONObject("action").getJSONObject("args").toString();
        }
      } catch (Exception e) {
        log.error("Error during summary generation: ", e);
      }
    } catch (Exception e) {
      log.error("Error generating summary after max iterations: ", e);
      toolResult = "经过多次尝试仍未找到完整答案，请重新提问或提供更多细节。";
    }
    if (queue != null) {
      queue.add(toolResult);
      queue.add("[DONE]");
    }
    return toolResult;
  }

  /**
   * 调用LLM获取thought，支持流式和非流式
   */
  private JSONObject callLLMForThought(String question, List<ModelMessage> messages,
      Map<String, Queue<String>> queueMap, String chatId, int productId,
      boolean includeThoughtEnabled) {
    LLM llm = llmDiyUtility.getDiyLlm(productId, llmName, "4");
    if (speedUp) {
      // 使用流式调用，实时获取thought内容
      dataMap.remove(chatId);

      try {
        llm.streamJsonChat(question, messages, false,
            new AgentEventSourceListener(queueMap, chatId, this, "thought",
                showThinking && includeThoughtEnabled));

        Lock chatLock = lockMap.get(chatId);
        Condition chatCondition = conditionMap.get(chatId);

        chatLock.lock();
        try {
          // 等待流式响应完成
          while (dataMap.get(chatId) == null) {
            chatCondition.await();
          }
        } catch (InterruptedException e) {
          log.error("等待LLM响应时被中断: {}", e.getMessage());
          Thread.currentThread().interrupt();
          return null;
        } finally {
          chatLock.unlock();
        }

        String data = dataMap.get(chatId);
        if (data == null || data.isEmpty()) {
          log.error("LLM返回空数据");
          return null;
        }

        try {
          return JSON.parseObject(data.replace("```json", "")
              .replace("```JSON", "").replace("```", ""));
        } catch (Exception e) {
          log.error("解析LLM响应JSON失败: {}, data: {}", e.getMessage(), data);
          return null;
        }
      } catch (Exception e) {
        log.error("流式调用LLM失败: {}", e.getMessage());
        return null;
      }
    } else {
      // 非流式调用
      try {
        return llm.jsonChat(question, messages, false);
      } catch (Exception e) {
        log.error("调用LLM失败: {}", e.getMessage());
        return null;
      }
    }
  }

  /**
   * 清理资源（MCP客户端、锁、条件变量等）
   */
  private void cleanupResources(String chatId) {
    // 清理锁和条件变量
    dataMap.remove(chatId);
    conditionMap.remove(chatId);
    lockMap.remove(chatId);
  }

  private Map<String, String> process_llm_result(JSONObject obj) {
    Map<String, String> resultMap = new HashMap<>();
    resultMap.put("thought", obj.getString("thought"));
    resultMap.put("action_name", obj.getJSONObject("action").getString("name"));
    resultMap.put("action_parameters", obj.getJSONObject("action").getString("args"));
    return resultMap;
  }

  private String buildThoughtLine(String thought) {
    if (thought == null || thought.isBlank()) {
      return "";
    }
    return "\nThought: " + thought;
  }

  private boolean getIncludeThoughtConfig() {
    try {
      List<AdminConfigEntity> configs =
          adminConfigService.findAllBySetKey("ai_agent_include_thought");
      if (!configs.isEmpty() && configs.get(0).getSetValue() != null) {
        return Boolean.parseBoolean(configs.get(0).getSetValue());
      }
    } catch (Exception e) {
      log.error("从数据库获取Agent thought配置失败", e);
    }
    return includeThought;
  }

  private int getEpochLimitConfig(int productId) {
    try {
      String value = userConfigService.getConfigValue(productId, EPOCH_LIMIT_CONFIG_KEY);
      if (value == null || value.isBlank()) {
        return epochLimit;
      }
      int configuredEpochLimit = Integer.parseInt(value.trim());
      if (configuredEpochLimit < 1) {
        return epochLimit;
      }
      return Math.min(MAX_EPOCH_LIMIT, configuredEpochLimit);
    } catch (NumberFormatException e) {
      log.warn("Invalid Agent epoch limit config: productId={}, key={}", productId,
          EPOCH_LIMIT_CONFIG_KEY, e);
      return epochLimit;
    } catch (Exception e) {
      log.error("从用户配置获取Agent epochLimit失败", e);
      return epochLimit;
    }
  }

  private boolean isFunctionRouterMode() {
    return "function".equalsIgnoreCase(routerMode);
  }

  private boolean isFunctionAgentMode() {
    return "function".equalsIgnoreCase(agentMode);
  }

  private String runFunctionCalling(String question, Map<String, Object> globalMessage) {
    int productId = (int) globalMessage.get("productId");
    Map<String, Queue<String>> queueMap =
        (Map<String, Queue<String>>) globalMessage.get("queueMap");
    String chatId = (String) globalMessage.get("chatId");
    Queue<String> queue = queueMap == null ? null : queueMap.get(chatId);
    LLM llm = llmDiyUtility.getDiyLlm(productId, llmName, "4");
    if (!llm.supportsFunctionCalling()) {
      return null;
    }
    globalMessage.put("mcpIsTool", true);
    if (queue != null) {
      queue.add(ToolPrefix.AGENT.getPrefix());
    }

    List<FunctionToolSpec> toolSpecs = buildFunctionToolSpecs(productId, chatId);
    int currentEpochLimit = getEpochLimitConfig(productId);
    List<ModelMessage> messages = new ArrayList<>();
    messages.add(new ModelMessage(ModelMessageRole.SYSTEM.value(),
        reactPrompt.getFunctionCalling(question, productId)));
    messages.add(new ModelMessage(ModelMessageRole.USER.value(), question));
    String finalAnswer = null;
    List<String> toolObservations = new ArrayList<>();
    for (int iteration = 0; iteration < currentEpochLimit; iteration++) {
      if (isQueueRemoved(chatId, queueMap)) {
        return "操作已取消";
      }
      FunctionResult result = callFunctionLlm(question, messages, toolSpecs, queueMap, chatId, llm);
      if (result == null || result.isUnsupported() || result.isError()) {
        return null;
      }
      if (result.isDirectReply()) {
        finalAnswer = result.getReply();
        if (shouldSuppressDirectReply(finalAnswer, toolObservations)) {
          messages.add(new ModelMessage(ModelMessageRole.ASSISTANT.value(), finalAnswer));
          messages.add(new ModelMessage(ModelMessageRole.USER.value(),
              "这只是进度提示，不是最终回答。请继续调用合适的工具，或在确实不需要工具时给出完整答案。"));
          continue;
        }
        if (queue != null) {
          if (!speedUp) {
            queue.add(finalAnswer);
          }
          queue.add("[DONE]");
        }
        return finalAnswer;
      }
      if (!result.isToolCall()) {
        return null;
      }
      String functionName = result.getFunctionName();
      String assistantText = extractAssistantText(result.getArguments());
      String toolArgs = extractFunctionArgs(result.getArguments());
      String toolCallArguments = normalizeToolCallArguments(result.getArguments());
      if (!isKnownAgentFunction(functionName, toolSpecs)) {
        log.warn("Unknown Agent function call: {}", functionName);
        return null;
      }
      if (queue != null) {
        queue.add(ToolPrefix.getToolCallPrefix(functionName));
      }
      String observation = manage.runTool(functionName, toolArgs, globalMessage);
      addToolObservation(toolObservations, observation);
      messages.add(new ModelMessage(ModelMessageRole.ASSISTANT.value(),
          new FunctionToolCallMessage(result.getToolCallId(), functionName, toolCallArguments,
              assistantText)));
      messages.add(new ModelMessage(ModelMessageRole.TOOL.value(),
          new FunctionToolResultMessage(result.getToolCallId(), observation)));
      finalAnswer = observation;
    }

    String summary = summarizeFunctionConversation(question, productId, toolObservations,
        finalAnswer);
    if (queue != null) {
      queue.add(summary);
      queue.add("[DONE]");
    }
    return summary;
  }


  private FunctionResult callFunctionLlm(String question, List<ModelMessage> messages,
      List<FunctionToolSpec> toolSpecs, Map<String, Queue<String>> queueMap, String chatId,
      LLM llm) {
    if (!speedUp) {
      return llm.functionChat(question, messages, toolSpecs);
    }
    lockMap.computeIfAbsent(chatId, k -> new ReentrantLock());
    conditionMap.computeIfAbsent(chatId, k -> lockMap.get(k).newCondition());
    functionDataMap.remove(chatId);
    Lock lock = lockMap.get(chatId);
    Condition condition = conditionMap.get(chatId);
    StringBuilder replyBuffer = new StringBuilder();

    llm.streamFunctionChat(question, messages, toolSpecs, new FunctionStreamHandler() {
      @Override
      public void onTextDelta(String text) {
        if (text == null || text.isBlank()) {
          return;
        }
        replyBuffer.append(text);
        Queue<String> queue = queueMap == null ? null : queueMap.get(chatId);
        if (queue != null) {
          queue.add(text.replace("\n", "").replace("\r", ""));
        }
      }

      @Override
      public void onDirectReplyComplete(String reply) {
        finish(FunctionResult.directReply(reply == null || reply.isBlank()
            ? replyBuffer.toString()
            : reply));
      }

      @Override
      public void onToolCall(String toolCallId, String functionName, String arguments) {
        FunctionResult result = FunctionResult.toolCall(toolCallId, functionName, arguments);
        if (!replyBuffer.isEmpty()) {
          result = FunctionResult.toolCall(toolCallId, functionName,
              mergeAssistantText(arguments, replyBuffer.toString()));
        }
        finish(result);
      }

      @Override
      public void onFailure(Throwable throwable) {
        log.error("Agent function calling stream failed, chatId={}", chatId, throwable);
        if (!replyBuffer.isEmpty()) {
          finish(FunctionResult.directReply(replyBuffer.toString()));
          return;
        }
        finish(FunctionResult.error("function calling stream failed"));
      }

      @Override
      public void onUnsupported() {
        finish(FunctionResult.unsupported());
      }

      private void finish(FunctionResult result) {
        lock.lock();
        try {
          if (!functionDataMap.containsKey(chatId)) {
            functionDataMap.put(chatId, result);
            condition.signalAll();
          }
        } finally {
          lock.unlock();
        }
      }
    });

    lock.lock();
    try {
      while (!functionDataMap.containsKey(chatId)) {
        if (!condition.await(functionTimeoutSeconds, TimeUnit.SECONDS)) {
          functionDataMap.put(chatId, FunctionResult.error("function calling timeout"));
          break;
        }
      }
      return functionDataMap.get(chatId);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return FunctionResult.error("function calling interrupted");
    } finally {
      lock.unlock();
      functionDataMap.remove(chatId);
    }
  }

  private List<FunctionToolSpec> buildFunctionToolSpecs(int productId, String chatId) {
    List<FunctionToolSpec> toolSpecs = new ArrayList<>();
    JSONObject tools = JSON.parseObject(descriptionUtil.getTools(productId, chatId));
    for (Map.Entry<String, Object> entry : tools.entrySet()) {
      String toolName = entry.getKey();
      String description = Objects.toString(entry.getValue(), "");
      if (toolName == null || toolName.isBlank()) {
        continue;
      }
      toolSpecs.add(new FunctionToolSpec(toolName,
          description.isBlank() ? toolName : description,
          FunctionToolSpec.defaultStringArgsSchema()));
    }
    return toolSpecs;
  }

  private boolean isKnownAgentFunction(String functionName, List<FunctionToolSpec> toolSpecs) {
    if (functionName == null || toolSpecs == null) {
      return false;
    }
    return toolSpecs.stream().anyMatch(toolSpec -> functionName.equals(toolSpec.name()));
  }

  private String extractFunctionArgs(String arguments) {
    if (arguments == null || arguments.isBlank()) {
      return "";
    }
    try {
      JSONObject object = JSON.parseObject(arguments);
      object.remove("_assistant_text");
      String args = object.getString("args");
      if (args != null) {
        return args;
      }
    } catch (Exception ignored) {
      // Raw arguments are still accepted for compatibility with older tool prompts.
    }
    return arguments;
  }

  private String normalizeToolCallArguments(String arguments) {
    if (arguments == null || arguments.isBlank()) {
      return "{\"args\":\"\"}";
    }
    try {
      JSONObject object = JSON.parseObject(arguments);
      object.remove("_assistant_text");
      if (!object.containsKey("args")) {
        object.put("args", "");
      }
      return object.toJSONString();
    } catch (Exception ignored) {
      JSONObject object = new JSONObject();
      object.put("args", arguments);
      return object.toJSONString();
    }
  }

  private String extractAssistantText(String arguments) {
    if (arguments == null || arguments.isBlank()) {
      return "";
    }
    try {
      JSONObject object = JSON.parseObject(arguments);
      return Objects.toString(object.getString("_assistant_text"), "");
    } catch (Exception ignored) {
      return "";
    }
  }

  private String mergeAssistantText(String arguments, String assistantText) {
    try {
      JSONObject object = arguments == null || arguments.isBlank()
          ? new JSONObject()
          : JSON.parseObject(arguments);
      object.put("_assistant_text", assistantText);
      return object.toJSONString();
    } catch (Exception e) {
      JSONObject object = new JSONObject();
      object.put("args", arguments == null ? "" : arguments);
      object.put("_assistant_text", assistantText);
      return object.toJSONString();
    }
  }

  private String summarizeFunctionConversation(String question, int productId,
      List<String> toolObservations, String fallback) {
    String observations = joinToolObservations(toolObservations);
    if (observations.isBlank()) {
      return fallback == null || fallback.isBlank()
          ? "工具调用次数已达上限，但未获得有效工具结果，请缩小问题范围或提供更多细节后重试。"
          : fallback;
    }
    String summaryPrompt = """
        请根据以下工具返回结果，回答用户最初的问题。
        不要复述工具调用名称、函数名、参数或调用日志。
        如果工具结果不足以回答，请明确说明信息不足。

        用户问题：
        %s

        工具结果：
        %s
        """.formatted(question, observations);
    List<ModelMessage> summaryMessages = new ArrayList<>();
    summaryMessages.add(new ModelMessage(ModelMessageRole.SYSTEM.value(),
        "你是一个负责总结工具结果的助手，只能基于工具返回结果回答。"));
    summaryMessages.add(new ModelMessage(ModelMessageRole.USER.value(), summaryPrompt));
    try {
      return llmDiyUtility.getDiyLlm(productId, llmName, "4")
          .commonChat(summaryPrompt, summaryMessages, false);
    } catch (Exception e) {
      log.error("Agent function calling summary failed", e);
      return fallback == null || fallback.isBlank()
          ? "工具调用次数已达上限，且总结工具结果失败，请重新提问或提供更多细节。"
          : fallback;
    }
  }

  private void addToolObservation(List<String> toolObservations, String observation) {
    if (toolObservations == null || observation == null || observation.isBlank()
        || hasFunctionCallTrace(observation)) {
      return;
    }
    toolObservations.add("工具返回结果 " + (toolObservations.size() + 1) + ":\n" + observation);
  }

  private String joinToolObservations(List<String> toolObservations) {
    if (toolObservations == null || toolObservations.isEmpty()) {
      return "";
    }
    return toolObservations.stream()
        .filter(Objects::nonNull)
        .filter(observation -> !observation.isBlank())
        .filter(observation -> !hasFunctionCallTrace(observation))
        .reduce((left, right) -> left + "\n\n" + right)
        .orElse("");
  }

  private boolean shouldSuppressDirectReply(String reply, List<String> toolObservations) {
    if (hasFunctionCallTrace(reply)) {
      return true;
    }
    return (toolObservations == null || toolObservations.isEmpty())
        && isProgressOnlyReply(reply);
  }

  private boolean isProgressOnlyReply(String content) {
    String text = content == null ? "" : content.strip().toLowerCase(Locale.ROOT);
    if (text.isBlank()) {
      return true;
    }
    return text.contains("function call");
  }

  private boolean hasFunctionCallTrace(String content) {
    String text = content == null ? "" : content.strip().toLowerCase(Locale.ROOT);
    return text.contains("called function ") && text.contains(" with arguments:");
  }
}
