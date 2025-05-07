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
import com.alibaba.fastjson.JSONObject;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import top.rslly.iot.utility.ai.IcAiException;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class ClassifierToolEventSourceListener extends EventSourceListener {
  private final StringBuilder jsonBuffer = new StringBuilder();
  private final String question;
  public int filter;

  public ClassifierToolEventSourceListener(String question, int filter) {
    this.question = question;
    this.filter = filter;
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
      log.info("OpenAI最终返回数据：{}", jsonBuffer.toString());
      var lock = ClassifierTool.lock;
      lock.lock();
      try {
        var content = JSON.parseObject(
            jsonBuffer.toString().replace("```json", "").replace("```JSON", "").replace("```", "")
                .replace("json", ""))
            .getJSONObject("action");
        if (content.get("code").equals("200") || content.get("code").equals(200)) {
          var valueJson = content.getJSONArray("value");
          var argsJson = content.getString("args");
          ClassifierTool.dataMap.put("value",
              JSONObject.parseArray(valueJson.toJSONString(), String.class));
          ClassifierTool.dataMap.put("args", argsJson);
          ClassifierTool.dataMap.put("answer", "yes");
          // ClassifierTool.dataCondition.signal();
        } else
          throw new IcAiException("llm response error");
        ClassifierTool.dataCondition.signal();
        log.info("数据{}", ClassifierTool.dataMap);
      } catch (Exception e) {
        log.error("OpenAi error{}", e.getMessage());
        ClassifierTool.dataCondition.signal();
        ClassifierTool.dataMap.put("value", "");
        ClassifierTool.dataMap.put("args", "");
        ClassifierTool.dataMap.put("answer", "对不起你购买的产品尚不支持这个请求或者设备不在线，请检查你的小程序的设置");
      } finally {
        ClassifierTool.dataCondition.signal();
        lock.unlock();
      }
      return;
    }
    try {
      var jsonObject = JSON.parseObject(data).getJSONArray("choices").get(0);
      var delta = JSON.parseObject(jsonObject.toString()).getString("delta");
      var content = JSON.parseObject(delta).get("content");
      if (content == null) {
        // log.warn("Received null data from OpenAI");
        return;
      }
      ClassifierTool.lock.lock();
      try {
        jsonBuffer.append(content);
        String regex = "\\[([^\\]]*)\\]";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(jsonBuffer.toString());
        while (matcher.find()) {
          if (matcher.group().equals("[" + filter + "]") || matcher.group().equals("[]")) {
            ClassifierTool.dataMap.put("value", matcher.group());
            ClassifierTool.dataMap.put("args", question);
            ClassifierTool.dataMap.put("answer", "yes");
            ClassifierTool.dataCondition.signal();
            return;
          }
        }
      } finally {
        ClassifierTool.lock.unlock();
      }
      // log.info("OpenAI返回数据：{}", content);
    } catch (Exception e) {
      log.error("OpenAi error{}", e.getMessage());
    }
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
      ClassifierTool.lock.lock();
      try {
        if (eventSource != null)
          eventSource.cancel();
        ClassifierTool.dataMap.put("answer", "对不起你购买的产品尚不支持这个请求或者设备不在线，请检查你的小程序的设置");
        ClassifierTool.dataCondition.signal();
      } finally {
        ClassifierTool.lock.unlock();
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
