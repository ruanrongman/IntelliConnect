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
package top.rslly.iot.utility.ai.tools;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

@Slf4j
public class ChatToolEventSourceListener extends EventSourceListener {
  private static final String DONE_SIGNAL = "[DONE]";
  private static final String ERROR_MESSAGE = "对不起你购买的产品尚不支持这个请求或者设备不在线，请检查你的小程序的设置";
  private final StringBuilder jsonBuffer = new StringBuilder();
  private final Map<String, Queue<String>> queueMap;
  private final String chatId;
  private final BaseTool<?> chatTool; // 持有ChatTool实例引用
  private final AtomicReference<EventSource> currentEventSource = new AtomicReference<>();

  public ChatToolEventSourceListener(Map<String, Queue<String>> queueMap, String chatId,
      BaseTool<?> tool) {
    this.queueMap = queueMap;
    this.chatId = chatId;
    this.chatTool = tool;
  }

  @Override
  public void onOpen(EventSource eventSource, Response response) {
    currentEventSource.set(eventSource);
    if (queueMap == null || !queueMap.containsKey(chatId)) {
      log.warn("资源已被释放，关闭连接: chatId={}", chatId);
      eventSource.cancel();
      return;
    }
    log.info("OpenAI建立sse连接...");
  }

  @Override
  public void onEvent(EventSource eventSource, String id, String type, String data) {
    currentEventSource.compareAndSet(null, eventSource);
    if (queueMap == null || !queueMap.containsKey(chatId)) {
      log.warn("queueMap已被释放，切断连接: chatId={}", chatId);
      eventSource.cancel();
      return;
    }
    if (data == null) {
      log.warn("Received null data from OpenAI");
      return;
    }
    if (DONE_SIGNAL.equals(data)) {
      log.info("OpenAI返回数据结束了");
      Lock lock = chatTool.getLockMap().get(chatId);
      Condition condition = chatTool.getConditionMap().get(chatId);
      if (lock == null || condition == null) {
        log.warn("ChatTool同步资源已释放，忽略完成信号: chatId={}", chatId);
        return;
      }
      lock.lock();
      try {
        Queue<String> queue = queueMap.get(chatId);
        if (queue != null) {
          queue.add(DONE_SIGNAL);
        }
        chatTool.getDataMap().put(chatId, jsonBuffer.toString());
        condition.signal();
      } catch (Exception e) {
        chatTool.getDataMap().put(chatId, ERROR_MESSAGE);
        log.error("OpenAI sse结束处理异常", e);
      } finally {
        lock.unlock();
      }
      return;
    }
    try {
      JSONObject jsonObject = JSON.parseObject(data);
      if (jsonObject == null) {
        log.warn("OpenAI返回空JSON对象, chatId={}", chatId);
        return;
      }
      JSONArray choices = jsonObject.getJSONArray("choices");
      if (choices == null || choices.isEmpty()) {
        log.debug("SSE数据缺少choices字段, chatId={}", chatId);
        return;
      }
      JSONObject choice = choices.getJSONObject(0);
      if (choice == null) {
        return;
      }
      JSONObject delta = choice.getJSONObject("delta");
      if (delta == null) {
        return;
      }
      String content = delta.getString("content");
      if (content == null || content.trim().isEmpty()) {
        return;
      }
      String processedContent = content
          .replace("\n", "")
          .replace("\r", "");

      Queue<String> queue = queueMap.get(chatId);
      if (queue != null && !processedContent.isEmpty()) {
        queue.add(processedContent);
      }
      jsonBuffer.append(content);
    } catch (Exception e) {
      log.error("解析ChatTool流式数据失败, chatId={}", chatId, e);
      failStream(ERROR_MESSAGE);
    }
  }

  @Override
  public void onClosed(EventSource eventSource) {
    log.info("OpenAI关闭sse连接...");
    currentEventSource.compareAndSet(eventSource, null);
  }

  @Override
  public void onFailure(EventSource eventSource, Throwable t, Response response) {
    log.error("OpenAI sse连接异常, chatId={}", chatId, t);
    currentEventSource.compareAndSet(eventSource, null);
    if (queueMap == null || !queueMap.containsKey(chatId)) {
      log.warn("资源已被释放，直接关闭连接");
      if (eventSource != null)
        eventSource.cancel();
      return;
    }
    failStream(ERROR_MESSAGE);
  }

  public void cancelCurrentStream() {
    EventSource eventSource = currentEventSource.getAndSet(null);
    if (eventSource != null) {
      eventSource.cancel();
    }
  }

  private void failStream(String message) {
    Lock lock = chatTool.getLockMap().get(chatId);
    Condition condition = chatTool.getConditionMap().get(chatId);
    if (lock == null || condition == null) {
      log.warn("ChatTool同步资源已释放，忽略失败回调: chatId={}", chatId);
      return;
    }
    lock.lock();
    try {
      cancelCurrentStream();
      Queue<String> queue = queueMap.get(chatId);
      if (queue != null) {
        queue.add(DONE_SIGNAL);
      }
      chatTool.getDataMap().put(chatId, message);
      condition.signal();
    } catch (Exception e) {
      log.error("OpenAI sse失败回调处理异常", e);
    } finally {
      lock.unlock();
    }
  }
}
