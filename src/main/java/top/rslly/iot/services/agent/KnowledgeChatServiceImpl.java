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

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import top.rslly.iot.dao.*;
import top.rslly.iot.models.KnowledgeChatEntity;
import top.rslly.iot.models.WxUserEntity;
import top.rslly.iot.utility.JwtTokenUtil;
import top.rslly.iot.utility.ai.rag.RagUtility;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class KnowledgeChatServiceImpl implements KnowledgeChatService {
  @Resource
  private KnowledgeChatRepository knowledgeChatRepository;
  @Resource
  private UserRepository userRepository;
  @Resource
  private UserProductBindRepository userProductBindRepository;
  @Resource
  private WxUserRepository wxUserRepository;
  @Resource
  private WxProductBindRepository wxProductBindRepository;
  @Autowired
  private EmbeddingModel embeddingModel;
  @Autowired
  private EmbeddingStore<TextSegment> knowledgeChatEmbeddingStore;
  @Autowired
  @Qualifier("taskExecutor")
  private ThreadPoolTaskExecutor taskExecutor;

  @Override
  public List<KnowledgeChatEntity> findAllById(int id) {
    return knowledgeChatRepository.findAllById(id);
  }

  @Override
  public String searchByProductId(String productId, String query) {
    var res = knowledgeChatRepository.findAllByProductId(Integer.parseInt(productId));
    if (res.isEmpty())
      return "";
    try {
      EmbeddingSearchResult<TextSegment> searchResult = RagUtility.searchByProductId(
          knowledgeChatEmbeddingStore, embeddingModel, query, productId);
      // 返回match.embedded().text()匹配结果即可
      return searchResult.matches().stream()
          .map(match -> match.embedded().text())
          .collect(Collectors.joining("\n"));
    } catch (Exception e) {
      log.error("查询知识库失败{}", e.getMessage());
      return "";
    }
  }

  @Override
  public JsonResult<?> getKnowledgeChat(String token) {
    String token_deal = token.replace(JwtTokenUtil.TOKEN_PREFIX, "");
    String role = JwtTokenUtil.getUserRole(token_deal);
    String username = JwtTokenUtil.getUsername(token_deal);
    List<KnowledgeChatEntity> result;
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
        List<KnowledgeChatEntity> knowledgeChatEntities =
            knowledgeChatRepository.findAllByProductId(s.getProductId());
        result.addAll(knowledgeChatEntities);
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
        List<KnowledgeChatEntity> knowledgeChatEntities =
            knowledgeChatRepository.findAllByProductId(s.getProductId());
        result.addAll(knowledgeChatEntities);
      }
    } else {
      result = knowledgeChatRepository.findAll();
    }
    if (result.isEmpty()) {
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    } else
      return ResultTool.success(result);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> postKnowledgeChat(int productId, String fileName,
      MultipartFile multipartFile) {
    if (multipartFile.isEmpty())
      return ResultTool.fail(ResultCode.PARAM_NOT_COMPLETE);
    if (!knowledgeChatRepository.findAllByProductIdAndFilename(productId, fileName).isEmpty())
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    String realFileName = multipartFile.getOriginalFilename();
    if (realFileName == null)
      return ResultTool.fail(ResultCode.PARAM_NOT_COMPLETE);
    String suffixName = realFileName.substring(realFileName.lastIndexOf(".")); // 后缀名
    if (!suffixName.equals(".pdf"))
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    try {
      File tempFile = Files.createTempFile("upload-", ".pdf").toFile();
      multipartFile.transferTo(tempFile);
      KnowledgeChatEntity knowledgeChatEntity = new KnowledgeChatEntity();
      knowledgeChatEntity.setFilename(fileName);
      knowledgeChatEntity.setProductId(productId);
      knowledgeChatEntity.setStatus("training");
      var result = knowledgeChatRepository.save(knowledgeChatEntity);
      taskExecutor.execute(() -> {
        try {
          RagUtility.ingestPdfToChroma(tempFile.getAbsolutePath(), String.valueOf(productId),
              fileName, embeddingModel, knowledgeChatEmbeddingStore);
          knowledgeChatEntity.setStatus("success");
          knowledgeChatEntity.setId(result.getId());
          knowledgeChatRepository.save(knowledgeChatEntity);
        } catch (Exception e) {
          log.error("knowledge chat ingest error: {}", e.getMessage());
          knowledgeChatEntity.setStatus("error");
          knowledgeChatEntity.setId(result.getId());
          knowledgeChatRepository.save(knowledgeChatEntity);
        }
      });
      return ResultTool.success();
    } catch (IOException e) {
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    }
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> deleteKnowledgeChat(int id) {
    List<KnowledgeChatEntity> knowledgeChatEntities =
        knowledgeChatRepository.findAllById(id);
    if (!knowledgeChatEntities.isEmpty()) {
      int productId = knowledgeChatEntities.get(0).getProductId();
      String fileName = knowledgeChatEntities.get(0).getFilename();
      List<KnowledgeChatEntity> result =
          knowledgeChatRepository.deleteAllByProductIdAndFilename(productId, fileName);
      if (!result.isEmpty()) {
        if (!result.get(0).getStatus().equals("error")) {
          RagUtility.deleteAllByProductIdAndFilename(knowledgeChatEmbeddingStore,
              String.valueOf(productId), fileName);
        }
        return ResultTool.success(result);
      } else {
        return ResultTool.fail(ResultCode.COMMON_FAIL);
      }
    } else {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
  }
}
