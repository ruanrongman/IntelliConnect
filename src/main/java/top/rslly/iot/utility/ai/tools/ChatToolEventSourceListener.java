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
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;

import java.util.Map;
import java.util.Objects;
import java.util.Queue;

@Slf4j
public class ChatToolEventSourceListener extends EventSourceListener {
  private final StringBuilder jsonBuffer = new StringBuilder();
  private final Map<String, Queue<String>> queueMap;
  private final String chatId;
  private final BaseTool<?> chatTool; // 持有ChatTool实例引用

  public ChatToolEventSourceListener(Map<String, Queue<String>> queueMap, String chatId,
      BaseTool<?> tool) {
    this.queueMap = queueMap;
    this.chatId = chatId;
    this.chatTool = tool;
  }

  @Override
  public void onOpen(EventSource eventSource, Response response) {
    if (queueMap == null || !queueMap.containsKey(chatId)) {
      log.warn("资源已被释放，关闭连接: chatId={}", chatId);
      eventSource.cancel();
      return;
    }
    log.info("OpenAI建立sse连接...");
  }

  @Override
  public void onEvent(EventSource eventSource, String id, String type, String data) {
    if (queueMap == null || !queueMap.containsKey(chatId)) {
      log.warn("queueMap已被释放，切断连接: chatId={}", chatId);
      eventSource.cancel();
      return;
    }
    if (data == null) {
      log.warn("Received null data from OpenAI");
      return;
    }
    if ("[DONE]".equals(data)) {
      log.info("OpenAI返回数据结束了");
      var lock = chatTool.getLockMap().get(chatId);
      lock.lock();
      try {
        var queue = queueMap.get(chatId);
        queue.add("[DONE]");
        chatTool.getDataMap().put(chatId, jsonBuffer.toString());
        chatTool.getConditionMap().get(chatId).signal();
      } catch (Exception e) {
        chatTool.getDataMap().put(chatId, "对不起你购买的产品尚不支持这个请求或者设备不在线，请检查你的小程序的设置");
        log.error("OpenAI  sse连接异常:{}", e.getMessage());
      } finally {
        lock.unlock();
      }
      return;
    }
    var jsonObject = JSON.parseObject(data).getJSONArray("choices").get(0);
    var delta = JSON.parseObject(jsonObject.toString()).getString("delta");
    var content = JSON.parseObject(delta).get("content");
    if (content == null || content.toString().trim().isEmpty()) {
      return;
    }
    String processedContent = content.toString()
        .replace("\n", "") // 移除换行符
        .replace("\r", ""); // 移除回车符

    var queue = queueMap.get(chatId);
    queue.add(processedContent); // 使用处理后的内容
    jsonBuffer.append(content);
  }

  @Override
  public void onClosed(EventSource eventSource) {
    log.info("OpenAI关闭sse连接...");
  }

  @SneakyThrows
  @Override
  public void onFailure(EventSource eventSource, Throwable t, Response response) {
    log.error("OpenAI  sse连接异常");
    if (queueMap == null || !queueMap.containsKey(chatId)) {
      log.warn("资源已被释放，直接关闭连接");
      if (eventSource != null)
        eventSource.cancel();
      return;
    }
    var lock = chatTool.getLockMap().get(chatId);
    if (lock != null) {
      lock.lock();
    }
    try {
      if (eventSource != null)
        eventSource.cancel();
      var queue = queueMap.get(chatId);
      queue.add("[DONE]");
      chatTool.getConditionMap().get(chatId).signal();
      chatTool.getDataMap().put(chatId, "对不起你购买的产品尚不支持这个请求或者设备不在线，请检查你的小程序的设置");
    } catch (Exception e) {
      log.error("OpenAI  sse连接异常:{}", e.getMessage());
    } finally {
      if (lock != null)
        lock.unlock();
    }
  }
}
