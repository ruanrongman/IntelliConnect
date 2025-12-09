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
import io.modelcontextprotocol.client.McpSyncClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.rslly.iot.utility.ai.DescriptionUtil;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.ModelMessageRole;
import top.rslly.iot.utility.ai.llm.LLM;
import top.rslly.iot.utility.ai.llm.LLMFactory;
import top.rslly.iot.utility.ai.Manage;
import top.rslly.iot.utility.ai.prompts.ReactPrompt;
import top.rslly.iot.utility.ai.tools.BaseTool;
import top.rslly.iot.utility.ai.tools.ToolPrefix;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Data
@Slf4j
public class Agent implements BaseTool<String> {
  @Autowired
  private Manage manage;
  @Value("${ai.agent-llm}")
  private String llmName;
  @Value("${ai.agent-speedUp:true}")
  private boolean speedUp;
  @Autowired
  private ReactPrompt reactPrompt;
  @Autowired
  private DescriptionUtil descriptionUtil;
  @Value("${ai.agent-epoch-limit}")
  private int epochLimit = 8;

  // 添加锁和条件变量映射（与ChatTool保持一致）
  private final Map<String, Lock> lockMap = new ConcurrentHashMap<>();
  private final Map<String, Condition> conditionMap = new ConcurrentHashMap<>();
  private final Map<String, String> dataMap = new ConcurrentHashMap<>();

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
    StringBuilder conversationPrompt = new StringBuilder();
    int productId = (int) globalMessage.get("productId");
    Map<String, Queue<String>> queueMap =
        (Map<String, Queue<String>>) globalMessage.get("queueMap");
    String chatId = (String) globalMessage.get("chatId");
    // 初始化当前会话的锁和条件变量
    lockMap.putIfAbsent(chatId, new ReentrantLock());
    conditionMap.putIfAbsent(chatId, lockMap.get(chatId).newCondition());
    globalMessage.put("mcpIsTool", true);
    var queue = queueMap.get(chatId);
    if (queue != null) {
      queue.add(ToolPrefix.AGENT.getPrefix());
      queue.add(getRandomComfortPhrase());
    }
    String system =
        reactPrompt.getReact(descriptionUtil.getTools(productId, chatId), question, productId);
    List<ModelMessage> messages = new ArrayList<>();
    String toolResult = "";
    conversationPrompt.append(system);
    // System.out.println("agent epoch limit" + epochLimit);
    log.info("agent epoch limit{}", epochLimit);
    int iteration = 0;
    while (iteration < epochLimit) {
      if (isQueueRemoved(chatId, queueMap)) {
        cleanupResources(chatId);
        return "操作已取消";
      }
      messages.clear();
      ModelMessage systemMessage =
          new ModelMessage(ModelMessageRole.SYSTEM.value(), conversationPrompt);
      ModelMessage userMessage = new ModelMessage(ModelMessageRole.USER.value(), question);
      messages.add(systemMessage);
      messages.add(userMessage);
      var obj = callLLMForThought(question, messages, queueMap, chatId);
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
        conversationPrompt.append(obj);
        conversationPrompt.append(String.format("Observation: %s\n", toolResult));
        if (queue != null) {
          queue.add(ToolPrefix.ToolCall.getPrefix());
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
      Map<String, Queue<String>> queueMap, String chatId) {
    if (speedUp) {
      // 使用流式调用，实时获取thought内容
      dataMap.remove(chatId);

      try {
        LLMFactory.getLLM(llmName).streamJsonChat(question, messages, false,
            new AgentEventSourceListener(queueMap, chatId, this));

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
        return LLMFactory.getLLM(llmName).jsonChat(question, messages, false);
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

}
