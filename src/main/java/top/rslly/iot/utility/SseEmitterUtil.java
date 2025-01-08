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
package top.rslly.iot.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class SseEmitterUtil {

  private static final Logger LOG = LoggerFactory.getLogger(SseEmitterUtil.class);
  /**
   * 当前连接数
   */
  private static final AtomicInteger count = new AtomicInteger(0);

  /**
   * 使用map对象，便于根据userId来获取对应的SseEmitter，或者放redis里面
   */
  private static final Map<String, SseEmitter> sseEmitterMap = new ConcurrentHashMap<>();

  /**
   * 创建用户连接并返回 SseEmitter
   *
   * @param userId 用户ID
   * @return SseEmitter
   */
  public static SseEmitter connect(String userId) {
    // 设置超时时间，0表示不过期。默认30秒，超过时间未完成会抛出异常：AsyncRequestTimeoutException
    SseEmitter sseEmitter = new SseEmitter(0L);
    // 注册回调
    sseEmitter.onCompletion(completionCallBack(userId));
    sseEmitter.onError(errorCallBack(userId));
    sseEmitter.onTimeout(timeoutCallBack(userId));
    sseEmitterMap.put(userId, sseEmitter);
    // 数量+1
    count.getAndIncrement();
    LOG.info("创建新的sse连接，当前用户：{}", userId);
    return sseEmitter;
  }

  /**
   * 给指定用户发送信息(这里面没有返回，最好添加异常返回)
   */
  public static void sendMessage(String userId, Object message) {
    if (sseEmitterMap.containsKey(userId)) {
      try {
        // sseEmitterMap.get(userId).send(message, MediaType.APPLICATION_JSON);
        sseEmitterMap.get(userId).send(message);
      } catch (IOException e) {
        LOG.error("用户[{}]推送异常:{}", userId, e.getMessage());
        removeUser(userId);
      }
    }
  }

  /**
   * 群发消息
   */
  public static void batchSendMessage(String wsInfo, List<Object> ids) {
    ids.forEach(userId -> sendMessage(wsInfo, userId));
  }

  /**
   * 群发所有人
   */
  public static void batchSendMessage(String wsInfo) {
    sseEmitterMap.forEach((k, v) -> {
      try {
        v.send(wsInfo, MediaType.APPLICATION_JSON);
      } catch (IOException e) {
        LOG.error("用户[{}]推送异常:{}", k, e.getMessage());
        removeUser(k);
      }
    });
  }

  /**
   * 移除用户连接
   */
  public static void removeUser(String userId) {
    sseEmitterMap.remove(userId);
    // 数量-1
    count.getAndDecrement();
    LOG.info("移除用户：{}", userId);
  }

  /**
   * 获取当前连接信息
   */
  public static List<String> getIds() {
    return new ArrayList<>(sseEmitterMap.keySet());
  }

  /**
   * 获取当前连接数量
   */
  public static int getUserCount() {
    return count.intValue();
  }

  private static Runnable completionCallBack(String userId) {
    return () -> {
      LOG.info("结束连接：{}", userId);
      removeUser(userId);
    };
  }

  private static Runnable timeoutCallBack(String userId) {
    return () -> {
      LOG.info("连接超时：{}", userId);
      removeUser(userId);
    };
  }

  private static Consumer<Throwable> errorCallBack(String userId) {
    return throwable -> {
      LOG.info("连接异常：{}", userId);
      removeUser(userId);
    };
  }

}
