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

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.rslly.iot.dao.*;
import top.rslly.iot.models.AgentMemoryEntity;
import top.rslly.iot.models.OtaXiaozhiEntity;
import top.rslly.iot.models.ProductRouterSetEntity;
import top.rslly.iot.models.WxUserEntity;
import top.rslly.iot.param.request.AgentMemory;
import top.rslly.iot.param.response.AgentMemoryResponse;
import top.rslly.iot.services.thingsModel.ProductServiceImpl;
import top.rslly.iot.utility.JwtTokenUtil;
import top.rslly.iot.utility.RedisUtil;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AgentMemoryServiceImpl implements AgentMemoryService {
  private static final Pattern UUID_PATTERN = Pattern.compile(
      "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89aAbB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}");

  @Resource
  private AgentMemoryRepository agentMemoryRepository;
  @Resource
  private WxProductBindRepository wxProductBindRepository;
  @Resource
  private UserProductBindRepository userProductBindRepository;
  @Resource
  private WxUserRepository wxUserRepository;
  @Resource
  private UserRepository userRepository;
  @Resource
  private OtaXiaozhiRepository otaXiaozhiRepository;
  @Autowired
  private RedisUtil redisUtil;
  @Autowired
  private ProductRepository productRepository;

  @Override
  public List<AgentMemoryEntity> findAllById(int id) {
    return agentMemoryRepository.findAllById(id);
  }

  @Override
  public List<AgentMemoryEntity> findAllByChatIdStartingWith(String chatId) {
    return agentMemoryRepository.findAllByChatIdStartingWith(chatId);
  }

  @Override
  public List<AgentMemoryEntity> findAllByChatId(String chatId) {
    return agentMemoryRepository.findAllByChatId(chatId);
  }

  @Override
  public JsonResult<?> getMemory(String token) {
    String token_deal = token.replace(JwtTokenUtil.TOKEN_PREFIX, "");
    String role = JwtTokenUtil.getUserRole(token_deal);
    String username = JwtTokenUtil.getUsername(token_deal);
    List<AgentMemoryEntity> result;
    if (role.equals("ROLE_" + "wx_user")) {
      if (wxUserRepository.findAllByName(username).isEmpty()) {
        return ResultTool.fail(ResultCode.COMMON_FAIL);
      }
      List<WxUserEntity> wxUserEntityList = wxUserRepository.findAllByName(username);
      String appid = wxUserEntityList.get(0).getAppid();
      String openid = wxUserEntityList.get(0).getOpenid();
      result = new ArrayList<>();
      var wxBindProductResponseList =
          wxProductBindRepository.findAllByAppidAndOpenid(appid, openid);
      if (wxBindProductResponseList.isEmpty()) {
        return ResultTool.fail(ResultCode.COMMON_FAIL);
      }
      for (var s : wxBindProductResponseList) {
        List<AgentMemoryEntity> agentMemoryEntities =
            agentMemoryRepository.findAllByChatIdStartingWith("chatProduct" + s.getProductId());
        result.addAll(agentMemoryEntities);
        List<AgentMemoryEntity> agentMemoryEntities1 =
            agentMemoryRepository.findAllByChatId(s.getAppid() + s.getOpenid());
        result.addAll(agentMemoryEntities1);
      }
    } else if (!role.equals("[ROLE_admin]")) {
      var userList = userRepository.findAllByUsername(username);
      if (userList.isEmpty()) {
        return ResultTool.fail(ResultCode.COMMON_FAIL);
      }
      int userId = userList.get(0).getId();
      result = new ArrayList<>();
      var userProductBindEntityList = userProductBindRepository.findAllByUserId(userId);
      if (userProductBindEntityList.isEmpty()) {
        return ResultTool.fail(ResultCode.COMMON_FAIL);
      }
      for (var s : userProductBindEntityList) {
        List<AgentMemoryEntity> agentMemoryEntities =
            agentMemoryRepository.findAllByChatIdStartingWith("chatProduct" + s.getProductId());
        result.addAll(agentMemoryEntities);
      }
    } else {
      result = agentMemoryRepository.findAll();
    }
    if (result.isEmpty()) {
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    } else {
      List<AgentMemoryResponse> result1 = new ArrayList<>();
      for (var s : result) {
        AgentMemoryResponse agentMemoryResponse = new AgentMemoryResponse();
        agentMemoryResponse.setChatId(s.getChatId());
        agentMemoryResponse.setContent(s.getContent());
        agentMemoryResponse.setId(s.getId());

        // 提取MAC地址
        String mac = extractMacFromChatId(s.getChatId());
        agentMemoryResponse.setDeviceName(Objects.requireNonNullElse(mac, "未知设备"));
        result1.add(agentMemoryResponse);
      }
      return ResultTool.success(result1);
    }
  }

  @Override
  public JsonResult<?> getMemoryByNickName(int productId, String nickName) {
    if (productRepository.findAllById(productId).isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    String defaultChatId = buildDefaultChatId(productId);
    Map<String, AgentMemoryEntity> memoryByChatId = new LinkedHashMap<>();
    Map<String, OtaXiaozhiEntity> deviceByChatId = new LinkedHashMap<>();
    List<OtaXiaozhiEntity> otaXiaozhiEntities;
    if (nickName != null && !nickName.isBlank()) {
      otaXiaozhiEntities =
          otaXiaozhiRepository.findAllByProductIdAndNickName(productId, nickName);
    } else {
      otaXiaozhiEntities = otaXiaozhiRepository.findAllByProductId(productId);
      List<AgentMemoryEntity> textConversationMemories =
          agentMemoryRepository.findAllByChatIdStartingWith(defaultChatId);
      for (AgentMemoryEntity memory : textConversationMemories) {
        if (isDefaultOrDebugChatId(productId, memory.getChatId())) {
          memoryByChatId.putIfAbsent(memory.getChatId(), memory);
        }
      }
    }
    for (OtaXiaozhiEntity device : otaXiaozhiEntities) {
      String chatId = defaultChatId + device.getDeviceId();
      deviceByChatId.putIfAbsent(chatId, device);
    }
    if (!deviceByChatId.isEmpty()) {
      for (AgentMemoryEntity memory : agentMemoryRepository
          .findAllByChatIdIn(new ArrayList<>(deviceByChatId.keySet()))) {
        String chatId = memory.getChatId();
        memoryByChatId.putIfAbsent(chatId, memory);
      }
    }
    if (memoryByChatId.isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    List<AgentMemoryResponse> agentMemoryResponses = new ArrayList<>();
    for (AgentMemoryEntity memory : memoryByChatId.values()) {
      AgentMemoryResponse agentMemoryResponse = new AgentMemoryResponse();
      agentMemoryResponse.setId(memory.getId());
      agentMemoryResponse.setContent(memory.getContent());
      agentMemoryResponse.setChatId(memory.getChatId());
      OtaXiaozhiEntity device = deviceByChatId.get(memory.getChatId());
      if (device != null) {
        agentMemoryResponse.setDeviceName(device.getDeviceId());
        agentMemoryResponse.setNickName(device.getNickName());
      }
      agentMemoryResponses.add(agentMemoryResponse);
    }
    agentMemoryResponses.sort(Comparator.comparingInt(AgentMemoryResponse::getId).reversed());
    return ResultTool.success(agentMemoryResponses);
  }

  @Override
  public boolean isChatIdValidForProduct(int productId, String chatId) {
    if (chatId == null || chatId.isBlank()) {
      return false;
    }
    String defaultChatId = buildDefaultChatId(productId);
    if (chatId.equals(defaultChatId)) {
      return true;
    }
    String debugPrefix = defaultChatId + "debug";
    if (chatId.startsWith(debugPrefix)) {
      return UUID_PATTERN.matcher(chatId.substring(debugPrefix.length())).matches();
    }
    if (!chatId.startsWith(defaultChatId)) {
      return false;
    }
    String deviceId = chatId.substring(defaultChatId.length());
    return !deviceId.isBlank()
        && !otaXiaozhiRepository.findAllByProductIdAndDeviceId(productId, deviceId).isEmpty();
  }

  private boolean isDefaultOrDebugChatId(int productId, String chatId) {
    String defaultChatId = buildDefaultChatId(productId);
    if (defaultChatId.equals(chatId)) {
      return true;
    }
    String debugPrefix = defaultChatId + "debug";
    return chatId != null && chatId.startsWith(debugPrefix)
        && UUID_PATTERN.matcher(chatId.substring(debugPrefix.length())).matches();
  }

  private String buildDefaultChatId(int productId) {
    return "chatProduct" + productId;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> updateMemory(AgentMemory agentMemory) {
    AgentMemoryEntity agentMemoryEntity = new AgentMemoryEntity();
    List<AgentMemoryEntity> agentMemoryEntities =
        agentMemoryRepository.findAllByChatId(agentMemory.getChatId());
    if (agentMemoryEntities.isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    agentMemoryEntity.setId(agentMemoryEntities.get(0).getId());
    agentMemoryEntity.setChatId(agentMemory.getChatId());
    agentMemoryEntity.setContent(agentMemory.getContent());
    AgentMemoryEntity result = agentMemoryRepository.save(agentMemoryEntity);
    return ResultTool.success(result);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> deleteMemory(int id) {
    List<AgentMemoryEntity> agentMemoryEntityList = agentMemoryRepository.findAllById(id);
    if (agentMemoryEntityList.isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    if (redisUtil.hasKey("memory" + agentMemoryEntityList.get(0).getChatId())) {
      redisUtil.del("memory" + agentMemoryEntityList.get(0).getChatId());
    }
    var result = agentMemoryRepository.deleteAllById(id);
    return ResultTool.success(result);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public AgentMemoryEntity insertAndUpdate(AgentMemory agentMemory) {
    AgentMemoryEntity agentMemoryEntity = new AgentMemoryEntity();
    List<AgentMemoryEntity> agentMemoryEntities =
        agentMemoryRepository.findAllByChatId(agentMemory.getChatId());
    if (!agentMemoryEntities.isEmpty()) {
      agentMemoryEntity = agentMemoryEntities.get(0);
    }
    BeanUtils.copyProperties(agentMemory, agentMemoryEntity);
    return agentMemoryRepository.save(agentMemoryEntity);
  }

  /**
   * 从chatId中提取MAC地址 示例: chatProduct174:5d:22:c7:a5:44 -> 74:5d:22:c7:a5:44 示例:
   * ozNrn6cEspMN0Yee_YDo-hTZ12gg -> null
   *
   * @param chatId 聊天ID
   * @return MAC地址，如果不存在则返回null
   */
  private String extractMacFromChatId(String chatId) {
    if (chatId == null || chatId.isEmpty()) {
      return null;
    }

    // MAC地址正则表达式: 匹配 xx:xx:xx:xx:xx 或 xx:xx:xx:xx:xx:xx 格式
    String macPattern =
        "([0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}(:[0-9a-fA-F]{2})?)";
    Pattern pattern = Pattern.compile(macPattern);
    Matcher matcher = pattern.matcher(chatId);

    if (matcher.find()) {
      return matcher.group(1);
    }

    return null;
  }
}
