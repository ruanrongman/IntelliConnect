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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class ClassifierToolEventSourceListener extends EventSourceListener {
  private static final String DONE_SIGNAL = "[DONE]";
  private static final String ERROR_MESSAGE =
      "对不起你购买的产品尚不支持这个请求或者设备不在线，请检查你的小程序的设置";
  private static final Pattern CLASSIFICATION_PATTERN = Pattern.compile("\\[([^\\]]*)\\]");

  private final StringBuilder jsonBuffer = new StringBuilder();
  private final String question;
  private final int[] filter;
  private final ClassifierTool classifierTool;
  private final String chatId;

  public ClassifierToolEventSourceListener(String question, int[] filter, String chatId,
      ClassifierTool classifierTool) {
    this.question = question;
    this.filter = filter;
    this.chatId = chatId;
    this.classifierTool = classifierTool;
  }

  @Override
  public void onOpen(EventSource eventSource, Response response) {
    log.debug("ClassifierTool SSE opened, chatId={}", chatId);
  }

  @Override
  public void onEvent(EventSource eventSource, String id, String type, String data) {
    if (data == null) {
      log.warn("Received null data from classifier stream, chatId={}", chatId);
      return;
    }

    SyncContext context = getSyncContext();
    if (context == null) {
      log.warn("ClassifierTool sync context already released, chatId={}", chatId);
      cancelQuietly(eventSource);
      return;
    }

    if (DONE_SIGNAL.equals(data)) {
      handleDone(eventSource, context);
      return;
    }

    handleStreamChunk(eventSource, context, data);
  }

  @Override
  public void onClosed(EventSource eventSource) {
    log.debug("ClassifierTool SSE closed, chatId={}", chatId);
  }

  @Override
  public void onFailure(EventSource eventSource, Throwable t, Response response) {
    log.error("ClassifierTool SSE failure, chatId={}", chatId, t);
    SyncContext context = getSyncContext();
    if (context == null) {
      cancelQuietly(eventSource);
      return;
    }

    context.lock().lock();
    try {
      cancelQuietly(eventSource);
      Map<String, Object> dataMap = new ConcurrentHashMap<>();
      dataMap.put("value", "[]");
      dataMap.put("args", "");
      dataMap.put("answer", ERROR_MESSAGE);
      context.data().putAll(dataMap);
      context.condition().signalAll();
    } finally {
      context.lock().unlock();
    }
  }

  private void handleDone(EventSource eventSource, SyncContext context) {
    context.lock().lock();
    try {
      JSONObject payload = JSON.parseObject(stripMarkdownFence(jsonBuffer.toString()));
      JSONObject action = payload.getJSONObject("action");
      if (action == null) {
        throw new IllegalStateException("Missing action object");
      }

      JSONArray valueJson = action.getJSONArray("value");
      String argsJson = action.getString("args");

      Map<String, Object> dataMap = new ConcurrentHashMap<>();
      dataMap.put("value",
          valueJson == null ? "[]" : JSONObject.parseArray(valueJson.toJSONString(), String.class));
      dataMap.put("args", argsJson == null ? "" : argsJson);
      dataMap.put("answer", "yes");
      context.data().putAll(dataMap);
      context.condition().signalAll();
      log.debug("ClassifierTool parsed final result, chatId={}", chatId);
    } catch (Exception e) {
      log.error("ClassifierTool failed to parse final payload, chatId={}", chatId, e);
      Map<String, Object> dataMap = new ConcurrentHashMap<>();
      dataMap.put("value", "");
      dataMap.put("args", "");
      dataMap.put("answer", ERROR_MESSAGE);
      context.data().putAll(dataMap);
      context.condition().signalAll();
    } finally {
      context.lock().unlock();
      cancelQuietly(eventSource);
    }
  }

  private void handleStreamChunk(EventSource eventSource, SyncContext context, String data) {
    try {
      JSONObject root = JSON.parseObject(data);
      if (root == null) {
        return;
      }

      JSONArray choices = root.getJSONArray("choices");
      if (choices == null || choices.isEmpty()) {
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
      if (content == null) {
        return;
      }

      context.lock().lock();
      try {
        jsonBuffer.append(content);
        Matcher matcher = CLASSIFICATION_PATTERN.matcher(jsonBuffer.toString());
        while (matcher.find()) {
          String matched = matcher.group();
          if (isMatchedClassification(matched)) {
            Map<String, Object> dataMap = new ConcurrentHashMap<>();
            dataMap.put("value", matched);
            dataMap.put("args", question);
            dataMap.put("answer", "yes");
            context.data().putAll(dataMap);
            context.condition().signalAll();
            cancelQuietly(eventSource);
            return;
          }
        }
      } finally {
        context.lock().unlock();
      }
    } catch (Exception e) {
      log.error("ClassifierTool failed to parse stream chunk, chatId={}", chatId, e);
    }
  }

  private boolean isMatchedClassification(String matched) {
    return matched.equals("[]")
        || matched.equals("[" + filter[0] + "]")
        || matched.equals("[" + filter[1] + "]");
  }

  private String stripMarkdownFence(String raw) {
    return raw.replace("```json", "")
        .replace("```JSON", "")
        .replace("```", "")
        .trim();
  }

  private SyncContext getSyncContext() {
    Lock lock = classifierTool.getLockMap().get(chatId);
    Condition condition = classifierTool.getConditionMap().get(chatId);
    Map<String, Object> data = classifierTool.getDataMap().get(chatId);
    if (lock == null || condition == null || data == null) {
      return null;
    }
    return new SyncContext(lock, condition, data);
  }

  private void cancelQuietly(EventSource eventSource) {
    if (eventSource != null) {
      eventSource.cancel();
    }
  }

  private record SyncContext(Lock lock, Condition condition, Map<String, Object> data) {}
}
