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

import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;

@Slf4j
public class ChatToolEventSourceListener extends EventSourceListener {
  private final StringBuilder jsonBuffer = new StringBuilder();
  private Map<String, Queue<String>> queueMap;
  private String chatId;

  public ChatToolEventSourceListener(Map<String, Queue<String>> queueMap, String chatId) {
    this.queueMap = queueMap;
    this.chatId = chatId;
    log.info(chatId);
  }

  @Override
  public void onOpen(EventSource eventSource, Response response) {
    log.info("OpenAI建立sse连接...");
  }

  @Override
  public void onEvent(EventSource eventSource, String id, String type, String data) {
    if (data == null) {
      log.warn("Received null data from OpenAI");
      return;
    }
    if ("[DONE]".equals(data)) {
      log.info("OpenAI返回数据结束了");
      ChatTool.lock.lock();
      try {
        var queue = queueMap.get(chatId);
        queue.add("[DONE]");
        ChatTool.dataMap.put("answer", jsonBuffer.toString());
        ChatTool.dataCondition.signal();
      } catch (Exception e) {
        ChatTool.dataMap.put("answer", "对不起你购买的产品尚不支持这个请求或者设备不在线，请检查你的小程序的设置");
        log.error("OpenAI  sse连接异常:{}", e.getMessage());
      } finally {
        ChatTool.lock.unlock();
      }
      return;
    }
    var jsonObject = JSON.parseObject(data).getJSONArray("choices").get(0);
    var delta = JSON.parseObject(jsonObject.toString()).getString("delta");
    var content = JSON.parseObject(delta).get("content");
    if (content == null || content.toString().trim().isEmpty()) {
      // log.warn("Received null data from OpenAI");
      return;
    }
    String processedContent = content.toString()
        .replace("\n", "") // 移除换行符
        .replace("\r", ""); // 移除回车符

    var queue = queueMap.get(chatId);
    queue.add(processedContent); // 使用处理后的内容
    jsonBuffer.append(content);
    // log.info("OpenAI返回数据：{}", processedContent); // 日志也输出处理后的内容
  }

  @Override
  public void onClosed(EventSource eventSource) {
    // ClassifierTool.dataCondition.signal();
    log.info("OpenAI关闭sse连接...");
  }

  @SneakyThrows
  @Override
  public void onFailure(EventSource eventSource, Throwable t, Response response) {
    if (Objects.isNull(response)) {
      log.error("OpenAI  sse连接异常:{}", t.getMessage());
      ChatTool.lock.lock();
      try {
        if (eventSource != null)
          eventSource.cancel();
        var queue = queueMap.get(chatId);
        queue.add("[DONE]");
        ChatTool.dataCondition.signal();
        ChatTool.dataMap.put("answer", "对不起你购买的产品尚不支持这个请求或者设备不在线，请检查你的小程序的设置");
      } catch (Exception e) {
        log.error("OpenAI  sse连接异常:{}", e.getMessage());
      } finally {
        ChatTool.lock.unlock();
      }
      return;
    }
    ResponseBody body = response.body();
    if (Objects.nonNull(body)) {
      log.error("OpenAI  sse连接异常data：{}，异常：{}", body.string(), t);
    } else {
      log.error("OpenAI  sse连接异常data：{}，异常：{}", response, t);
    }
    if (eventSource != null)
      eventSource.cancel();
  }
}
