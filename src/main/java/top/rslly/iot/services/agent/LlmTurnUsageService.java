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
package top.rslly.iot.services.agent;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import top.rslly.iot.dao.LlmTurnUsageRepository;
import top.rslly.iot.models.LlmTurnUsageEntity;
import top.rslly.iot.utility.ai.llm.usage.LlmUsageSummary;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores one usage summary per chat turn and adds them up into a conversation total for the status
 * bar under the chat input. Usage is diagnostic: failures are logged and never reach the chat flow.
 */
@Service
@Slf4j
public class LlmTurnUsageService {
  private final LlmTurnUsageRepository repository;

  @Value("${ai.usage.enabled:true}")
  private boolean enabled;
  @Value("${ai.usage.persist:true}")
  private boolean persist;

  public LlmTurnUsageService(LlmTurnUsageRepository repository) {
    this.repository = repository;
  }

  public void record(Integer productId, String chatId, String requestId,
      LlmUsageSummary summary) {
    if (!enabled || !persist || summary == null || isBlank(chatId) || isBlank(requestId)) {
      return;
    }
    try {
      LlmTurnUsageEntity entity = new LlmTurnUsageEntity();
      entity.setRequestId(requestId);
      entity.setChatId(chatId);
      entity.setProductId(productId);
      entity.setTime(System.currentTimeMillis());
      entity.setUsageJson(JSON.toJSONString(summary));
      repository.save(entity);
    } catch (RuntimeException e) {
      log.warn("record turn usage failed, chatId={}, requestId={}", chatId, requestId, e);
    }
  }

  /**
   * Token usage of a whole conversation: the sum of its stored turns. Only rows of the given
   * product are read, so a caller authorized for that product cannot see other products'
   * conversations. Turns before this feature existed have no record and are not counted.
   *
   * @return null when the conversation has no recorded turn
   */
  public LlmUsageSummary conversationTotal(int productId, String chatId) {
    if (!enabled || isBlank(chatId)) {
      return null;
    }
    try {
      List<LlmUsageSummary> turns = new ArrayList<>();
      for (LlmTurnUsageEntity entity : repository
          .findAllByChatIdAndProductIdOrderByTimeAscIdAsc(chatId, productId)) {
        turns.add(JSON.parseObject(entity.getUsageJson(), LlmUsageSummary.class));
      }
      return LlmUsageSummary.sumOfTurns(turns);
    } catch (RuntimeException e) {
      log.warn("load conversation usage failed, chatId={}", chatId, e);
      return null;
    }
  }

  /**
   * Cleanup runs outside the caller's transaction (NOT_SUPPORTED suspends it), so a failure here is
   * only logged and can never roll back the history or product deletion that triggered it.
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void deleteByChatId(String chatId) {
    try {
      repository.deleteAllByChatId(chatId);
    } catch (RuntimeException e) {
      log.warn("delete turn usage failed, chatId={}", chatId, e);
    }
  }

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void deleteByProductId(int productId) {
    try {
      repository.deleteAllByProductId(productId);
    } catch (RuntimeException e) {
      log.warn("delete turn usage failed, productId={}", productId, e);
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
