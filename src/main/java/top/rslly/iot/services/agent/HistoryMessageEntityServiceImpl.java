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

import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import top.rslly.iot.dao.*;
import top.rslly.iot.models.HistoryMessageEntity;
import top.rslly.iot.models.WxUserEntity;
import top.rslly.iot.services.UserConfigServiceImpl;
import top.rslly.iot.services.thingsModel.ProductServiceImpl;
import top.rslly.iot.utility.JwtTokenUtil;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class HistoryMessageEntityServiceImpl implements HistoryMessageEntityService {
  private static final String YOUTH_PROTECTION_CONFIG_KEY = "youth-protection.enabled";
  private static final String YOUTH_PROTECTION_TALK_MAX_LIMIT = "youth-protection-talk.maxLimit";
  private static final String USER_MESSAGE_TYPE = "user";
  private static final long YOUTH_PROTECTION_WINDOW_MILLIS = 2 * 60 * 60 * 1000L;
  private static final int MAX_EPOCH_LIMIT = 3000;
  private static final int epochLimit = 1000;

  @Resource
  private HistoryMessageRepository historyMessageRepository;
  @Resource
  private WxProductBindRepository wxProductBindRepository;
  @Resource
  private UserProductBindRepository userProductBindRepository;
  @Resource
  private WxUserRepository wxUserRepository;
  @Resource
  private UserRepository userRepository;
  @Autowired
  private UserConfigServiceImpl userConfigService;
  @Autowired
  private ProductServiceImpl productServiceImpl;
  @Autowired
  private ProductRepository productRepository;

  @Override
  public List<HistoryMessageEntity> findAllById(int id) {
    return historyMessageRepository.findAllById(id);
  }

  @Override
  public List<HistoryMessageEntity> findRecentByChatId(String chatId, int limit) {
    if (chatId == null || chatId.isBlank() || limit <= 0) {
      return List.of();
    }
    // The legacy limit counts user/assistant rows. Select whole requests before loading events.
    List<String> requestIds = historyMessageRepository.findRecentRequestIds(chatId,
        PageRequest.of(0, Math.max(1, (limit + 1) / 2)));
    if (requestIds.isEmpty()) {
      return List.of();
    }
    return historyMessageRepository.findAllByChatIdAndRequestIdIn(chatId, requestIds,
        Sort.by("time").and(Sort.by("sequenceNum")).and(Sort.by("id")));
  }

  @Override
  public boolean youthProtectForTimeOut(int productId, String chatId) {
    if (productRepository.findAllById(productId).isEmpty()) {
      return false;
    }
    if (!isYouthProtectionEnabled(productId)) {
      return false;
    }
    long now = System.currentTimeMillis();
    long windowStart = now - YOUTH_PROTECTION_WINDOW_MILLIS;
    Long talkCount = historyMessageRepository.countByChatIdAndMessageTypeAndTimeBetween(
        chatId, USER_MESSAGE_TYPE, windowStart, now);
    return talkCount >= youthProtectionTalkMaxLimit(productId);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void recordHistoryMessage(String chatId, String requestId, String userContent,
      String assistantContent) {
    long now = System.currentTimeMillis();
    List<HistoryMessageEntity> historyMessageEntities = new ArrayList<>();
    historyMessageEntities.add(buildHistoryMessage(chatId, requestId, 1, "user", userContent, now));
    historyMessageEntities.add(
        buildHistoryMessage(chatId, requestId, 2, "assistant", assistantContent, now + 1));
    historyMessageRepository.saveAll(historyMessageEntities);
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
  public void recordHistoryEvent(String chatId, String requestId, int sequenceNum,
      String messageType, String content, long time) {
    historyMessageRepository.saveAndFlush(
        buildHistoryMessage(chatId, requestId, sequenceNum, messageType, content, time));
  }

  @Override
  public JsonResult<?> getHistoryMessage(String token, int pageNum, int pageSize, String chatId,
      boolean includeToolEvents) {
    if (pageNum < 1 || pageSize < 1) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    String tokenDeal = token.replace(JwtTokenUtil.TOKEN_PREFIX, "");
    String role = JwtTokenUtil.getUserRole(tokenDeal);
    String username = JwtTokenUtil.getUsername(tokenDeal);
    Pageable pageable = PageRequest.of(
        pageNum - 1,
        pageSize,
        Sort.by(Sort.Direction.DESC, "time")
            .and(Sort.by(Sort.Direction.DESC, "sequenceNum"))
            .and(Sort.by(Sort.Direction.DESC, "id")));
    Specification<HistoryMessageEntity> visibleMessages =
        (root, query, cb) -> includeToolEvents ? cb.conjunction()
            : root.get("messageType").in("user", "assistant");
    Page<HistoryMessageEntity> result;
    if (role.equals("ROLE_" + "wx_user")) {
      List<WxUserEntity> wxUserEntityList = wxUserRepository.findAllByName(username);
      if (wxUserEntityList.isEmpty()) {
        return ResultTool.fail(ResultCode.COMMON_FAIL);
      }
      String appid = wxUserEntityList.get(0).getAppid();
      String openid = wxUserEntityList.get(0).getOpenid();
      var wxBindProductResponseList =
          wxProductBindRepository.findAllByAppidAndOpenid(appid, openid);
      if (wxBindProductResponseList.isEmpty()) {
        return ResultTool.fail(ResultCode.COMMON_FAIL);
      }
      Set<String> chatIdPrefixes = new LinkedHashSet<>();
      Set<String> chatIds = new LinkedHashSet<>();
      for (var s : wxBindProductResponseList) {
        chatIdPrefixes.add("chatProduct" + s.getProductId());
        chatIds.add(s.getAppid() + s.getOpenid());
      }
      result = historyMessageRepository
          .findAll(buildChatIdSpec(chatIdPrefixes, chatIds, chatId).and(visibleMessages), pageable);
    } else if (!role.equals("[ROLE_admin]")) {
      var userList = userRepository.findAllByUsername(username);
      if (userList.isEmpty()) {
        return ResultTool.fail(ResultCode.COMMON_FAIL);
      }
      int userId = userList.get(0).getId();
      var userProductBindEntityList = userProductBindRepository.findAllByUserId(userId);
      if (userProductBindEntityList.isEmpty()) {
        return ResultTool.fail(ResultCode.COMMON_FAIL);
      }
      Set<String> chatIdPrefixes = new LinkedHashSet<>();
      for (var s : userProductBindEntityList) {
        chatIdPrefixes.add("chatProduct" + s.getProductId());
      }
      result = historyMessageRepository
          .findAll(buildChatIdSpec(chatIdPrefixes, Set.of(), chatId).and(visibleMessages),
              pageable);
    } else {
      if (chatId == null || chatId.isBlank()) {
        result = historyMessageRepository.findAll(visibleMessages, pageable);
      } else {
        result = historyMessageRepository.findAll(visibleMessages.and(
            (root, query, cb) -> cb.equal(root.get("chatId"), chatId)), pageable);
      }
    }
    if (result.isEmpty()) {
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    } else {
      return ResultTool.success(result);
    }
  }

  private Specification<HistoryMessageEntity> buildChatIdSpec(Set<String> chatIdPrefixes,
      Set<String> chatIds,
      String chatId) {
    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();
      for (String chatIdPrefix : chatIdPrefixes) {
        predicates.add(criteriaBuilder.like(root.get("chatId"), chatIdPrefix + "%"));
      }
      if (!chatIds.isEmpty()) {
        predicates.add(root.get("chatId").in(chatIds));
      }
      Predicate permissionPredicate = criteriaBuilder.or(predicates.toArray(new Predicate[0]));
      if (chatId == null || chatId.isBlank()) {
        return permissionPredicate;
      }
      return criteriaBuilder.and(permissionPredicate,
          criteriaBuilder.equal(root.get("chatId"), chatId));
    };
  }

  private HistoryMessageEntity buildHistoryMessage(String chatId, String requestId,
      int sequenceNum, String messageType, String content, long time) {
    HistoryMessageEntity historyMessageEntity = new HistoryMessageEntity();
    historyMessageEntity.setChatId(chatId);
    historyMessageEntity.setRequestId(requestId);
    historyMessageEntity.setSequenceNum(sequenceNum);
    historyMessageEntity.setMessageType(messageType);
    historyMessageEntity.setContent(content == null ? "" : content);
    historyMessageEntity.setTime(time);
    return historyMessageEntity;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public List<HistoryMessageEntity> deleteAllByChatId(String chatId) {
    return historyMessageRepository.deleteAllByChatId(chatId);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> deleteHistoryMessage(int id) {
    if (historyMessageRepository.findAllById(id).isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    } else {
      List<HistoryMessageEntity> historyMessageEntityList =
          historyMessageRepository.deleteAllById(id);
      if (historyMessageEntityList.isEmpty()) {
        return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
      } else {
        return ResultTool.success(historyMessageEntityList);
      }
    }
  }

  private boolean isYouthProtectionEnabled(int productId) {
    try {
      String value = userConfigService.getConfigValue(productId, YOUTH_PROTECTION_CONFIG_KEY);
      return value != null && "true".equals(value.trim());
    } catch (RuntimeException e) {
      log.warn("读取青少年模式配置失败，按关闭处理，productId={}", productId, e);
      return false;
    }
  }

  private Integer youthProtectionTalkMaxLimit(int productId) {
    try {
      String value = userConfigService.getConfigValue(productId, YOUTH_PROTECTION_TALK_MAX_LIMIT);
      if (value == null || value.isBlank()) {
        return epochLimit;
      }
      int configuredEpochLimit = Integer.parseInt(value.trim());
      if (configuredEpochLimit < 1) {
        return epochLimit;
      }
      return Math.min(MAX_EPOCH_LIMIT, configuredEpochLimit);
    } catch (NumberFormatException e) {
      log.warn("Invalid YouthProtection config: productId={}, key={}", productId,
          YOUTH_PROTECTION_TALK_MAX_LIMIT, e);
      return epochLimit;
    } catch (Exception e) {
      log.error("从用户配置获取YouthProtection MAX_LIMIT失败", e);
      return epochLimit;
    }
  }
}
