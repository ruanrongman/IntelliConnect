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
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import top.rslly.iot.utility.ai.tools.BaseTool;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;

@Slf4j
public class AgentEventSourceListener extends EventSourceListener {
  // 累积完整JSON内容和thought检测缓冲区
  private final StringBuilder jsonBuffer = new StringBuilder();
  private final StringBuilder thoughtBuffer = new StringBuilder();

  private final Map<String, Queue<String>> queueMap;
  private final String chatId;
  private final BaseTool<?> chatTool;

  // 新增：可配置的字段名
  private final String targetFieldName;

  // 错误处理常量
  private static final String ERROR_MESSAGE = "对不起你购买的产品尚不支持这个请求或者设备不在线，请检查你的小程序的设置";
  private static final String DONE_SIGNAL = "[DONE]";
  private static final int THOUGHT_BUFFER_MAX_SIZE = 300;
  private static final int THOUGHT_BUFFER_RETAIN_SIZE = 50;

  // 用于跟踪thought字段解析状态的内部类
  private static class ThoughtParseState {
    boolean inThoughtField = false;
    boolean thoughtStarted = false;
  }

  // 每个chatId对应一个解析状态
  private final Map<String, ThoughtParseState> parseStateMap = new ConcurrentHashMap<>();

  public AgentEventSourceListener(Map<String, Queue<String>> queueMap, String chatId,
      BaseTool<?> tool) {
    this(queueMap, chatId, tool, "thought"); // 默认使用 "thought"
  }

  public AgentEventSourceListener(Map<String, Queue<String>> queueMap, String chatId,
      BaseTool<?> tool, String targetFieldName) {
    this.queueMap = queueMap;
    this.chatId = chatId;
    this.chatTool = tool;
    this.targetFieldName = targetFieldName; // 保存字段名
    this.parseStateMap.put(chatId, new ThoughtParseState());
  }

  @Override
  public void onOpen(EventSource eventSource, Response response) {
    log.info("OpenAI建立sse连接...");
  }

  @Override
  public void onEvent(EventSource eventSource, String id, String type, String data) {
    if (data == null) {
      log.warn("Received null data from OpenAI");
      notifyUserAndCleanup("服务响应异常，请稍后重试");
      return;
    }

    if (DONE_SIGNAL.equals(data)) {
      handleStreamComplete();
      return;
    }

    processStreamData(data);
  }

  /**
   * 处理流式传输完成
   */
  private void handleStreamComplete() {
    log.info("OpenAI返回数据结束了");
    Lock lock = chatTool.getLockMap().get(chatId);

    if (lock == null) {
      log.error("未找到chatId对应的锁: {}", chatId);
      return;
    }

    lock.lock();
    try {
      chatTool.getDataMap().put(chatId, jsonBuffer.toString());
      chatTool.getConditionMap().get(chatId).signal();
    } catch (Exception e) {
      log.error("处理流式传输完成时异常: {}", e.getMessage(), e);
      chatTool.getDataMap().put(chatId, ERROR_MESSAGE);
    } finally {
      lock.unlock();
      parseStateMap.remove(chatId);
    }
  }

  /**
   * 处理流式数据
   */
  private void processStreamData(String data) {
    try {
      log.debug("收到SSE数据: {}", data);

      JSONObject jsonObject = JSON.parseObject(data);

      if (jsonObject == null) {
        log.warn("SSE数据解析失败: {}", data);
        notifyUserAndCleanup("服务响应解析失败，请稍后重试");
        return;
      }

      // **检查error字段**
      if (jsonObject.containsKey("error")) {
        JSONObject error = jsonObject.getJSONObject("error");
        String errorCode = error.getString("code");
        String errorMessage = error.getString("message");
        log.error("OpenAI返回错误 - code: {}, message: {}", errorCode, errorMessage);

        // String userMessage = buildUserFriendlyErrorMessage(errorCode, errorMessage);
        notifyUserAndCleanup("抱歉，您的请求包含不适当的内容，请修改后重试");
        return;
      }

      if (!jsonObject.containsKey("choices")) {
        log.warn("SSE数据缺少choices字段: {}", data);
        notifyUserAndCleanup("服务响应格式异常，请稍后重试");
        return;
      }

      JSONArray choicesArray = jsonObject.getJSONArray("choices");
      if (choicesArray == null || choicesArray.isEmpty()) {
        log.warn("choices数组为空: {}", data);
        return;
      }

      JSONObject choice = choicesArray.getJSONObject(0);
      if (choice == null) {
        log.warn("choice对象为空");
        return;
      }

      JSONObject delta = choice.getJSONObject("delta");
      if (delta == null) {
        log.debug("delta字段为空，跳过");
        return;
      }

      String content = delta.getString("content");
      if (content == null || content.trim().isEmpty()) {
        return;
      }

      // 累积完整的JSON内容
      jsonBuffer.append(content);

      // 处理thought字段的流式提取
      processThoughtStream(content);

    } catch (Exception e) {
      log.error("解析SSE数据异常: {}, 原始数据: {}", e.getMessage(), data, e);
      notifyUserAndCleanup("服务处理异常，请稍后重试");
    }
  }

  /**
   * 通知用户错误并清理资源
   */
  private void notifyUserAndCleanup(String message) {
    Lock lock = chatTool.getLockMap().get(chatId);
    if (lock == null) {
      log.error("未找到chatId对应的锁: {}", chatId);
      parseStateMap.remove(chatId);
      return;
    }

    lock.lock();
    try {
      // 将错误消息放入队列，用户可以看到
      Queue<String> queue = queueMap.get(chatId);
      if (queue != null) {
        queue.add(message);
      }

      // 保存错误信息并通知等待线程
      chatTool.getDataMap().put(chatId, message);
      chatTool.getConditionMap().get(chatId).signal();

    } catch (Exception e) {
      log.error("通知用户错误时异常: {}", e.getMessage(), e);
    } finally {
      lock.unlock();
      // 清理解析状态
      parseStateMap.remove(chatId);
    }
  }

  /**
   * 流式处理thought字段内容
   */
  private void processThoughtStream(String content) {
    Queue<String> queue = queueMap.get(chatId);
    if (queue == null) {
      log.warn("未找到chatId对应的队列: {}", chatId);
      return;
    }

    ThoughtParseState state = parseStateMap.get(chatId);
    if (state == null) {
      log.warn("未找到chatId对应的解析状态: {}", chatId);
      return;
    }

    for (int i = 0; i < content.length(); i++) {
      char c = content.charAt(i);

      if (!state.thoughtStarted) {
        handleThoughtDetection(c, queue, state);
      } else if (state.inThoughtField) {
        handleThoughtContent(content, i, c, queue, state);
      }
    }
  }

  /**
   * 检测thought字段开始
   */
  private void handleThoughtDetection(char c,
      Queue<String> queue, ThoughtParseState state) {
    thoughtBuffer.append(c);
    String buffer = thoughtBuffer.toString();

    // 修改：使用可配置的字段名
    String fieldPattern = "\"" + targetFieldName + "\"";
    // 检测到 "fieldName": " 模式
    if (buffer.contains(fieldPattern)) {
      int thoughtIndex = buffer.indexOf(fieldPattern);
      int colonIndex = buffer.indexOf(":", thoughtIndex);

      if (colonIndex != -1) {
        int quoteIndex = buffer.indexOf("\"", colonIndex + 1);
        if (quoteIndex != -1) {
          state.thoughtStarted = true;
          state.inThoughtField = true;
          log.debug("检测到{}字段开始", targetFieldName); // 修改日志

          // 处理引号后面可能已有的内容
          processRemainingContent(buffer.substring(quoteIndex + 1), queue, state);
          thoughtBuffer.setLength(0);
          return;
        }
      }
    }

    // 限制缓冲区大小，避免内存溢出
    if (thoughtBuffer.length() > THOUGHT_BUFFER_MAX_SIZE) {
      thoughtBuffer.delete(0, thoughtBuffer.length() - THOUGHT_BUFFER_RETAIN_SIZE);
    }
  }

  /**
   * 处理thought字段内容
   */
  private void handleThoughtContent(String content, int position, char c,
      Queue<String> queue, ThoughtParseState state) {
    // 检测结束引号
    if (c == '\"' && !isEscapedAtPosition(content, position)) {
      state.inThoughtField = false;
      log.debug("{}字段结束", targetFieldName);
      return;
    }

    // 将有效字符加入队列
    addCharToQueue(c, queue);
  }

  /**
   * 处理剩余内容
   */
  private void processRemainingContent(String remaining, Queue<String> queue,
      ThoughtParseState state) {
    for (int i = 0; i < remaining.length(); i++) {
      char rc = remaining.charAt(i);
      if (rc == '\"' && !isEscapedAtPosition(remaining, i)) {
        state.inThoughtField = false;
        log.debug("{}字段结束", targetFieldName);
        break;
      } else if (state.inThoughtField) {
        addCharToQueue(rc, queue);
      }
    }
  }

  /**
   * 将字符添加到队列
   */
  private void addCharToQueue(char c, Queue<String> queue) {
    String processedChar = String.valueOf(c)
        .replace("\n", "")
        .replace("\r", "");
    if (!processedChar.isEmpty()) {
      queue.add(processedChar);
    }
  }

  /**
   * 检查字符串中指定位置的字符是否被转义
   */
  private boolean isEscapedAtPosition(String str, int position) {
    if (position == 0) {
      return false;
    }

    int backslashCount = 0;
    for (int i = position - 1; i >= 0 && str.charAt(i) == '\\'; i--) {
      backslashCount++;
    }

    // 奇数个反斜杠表示被转义
    return backslashCount % 2 == 1;
  }

  @Override
  public void onClosed(EventSource eventSource) {
    log.info("OpenAI关闭sse连接...");
    parseStateMap.remove(chatId);
  }

  @Override
  public void onFailure(EventSource eventSource, Throwable t, Response response) {
    log.error("OpenAI sse连接异常: {}", t.getMessage(), t);

    Lock lock = chatTool.getLockMap().get(chatId);
    if (lock == null) {
      log.error("未找到chatId对应的锁: {}", chatId);
      parseStateMap.remove(chatId);
      return;
    }

    lock.lock();
    try {
      if (eventSource != null) {
        eventSource.cancel();
      }

      // 通知用户
      Queue<String> queue = queueMap.get(chatId);
      if (queue != null) {
        queue.add(ERROR_MESSAGE);
      }

      chatTool.getDataMap().put(chatId, ERROR_MESSAGE);
      chatTool.getConditionMap().get(chatId).signal();
    } catch (Exception e) {
      log.error("处理SSE连接失败时异常: {}", e.getMessage(), e);
    } finally {
      lock.unlock();
      // 清理解析状态
      parseStateMap.remove(chatId);
    }
  }
}
