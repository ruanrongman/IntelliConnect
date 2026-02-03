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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import top.rslly.iot.dao.*;
import top.rslly.iot.models.McpServerEntity;
import top.rslly.iot.models.ProductSkillsEntity;
import top.rslly.iot.models.WxUserEntity;
import top.rslly.iot.param.prompt.ClaudeSkillMeta;
import top.rslly.iot.param.request.ProductSkills;
import top.rslly.iot.utility.ClaudeSkillMarkdownParser;
import top.rslly.iot.utility.JwtTokenUtil;
import top.rslly.iot.utility.MyFileUtil;
import top.rslly.iot.utility.ai.tools.JsExecuteTool;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class ProductSkillsServiceImpl implements ProductSkillsService {
  @Resource
  private ProductSkillsRepository productSkillsRepository;
  @Resource
  private WxProductBindRepository wxProductBindRepository;
  @Resource
  private UserProductBindRepository userProductBindRepository;
  @Resource
  private WxUserRepository wxUserRepository;
  @Resource
  private UserRepository userRepository;
  @Autowired
  private JsExecuteTool jsExecuteTool;
  @Value("${ota.bin.path}")
  private String binPath;

  @Override
  public List<ProductSkillsEntity> findAllById(int id) {
    return productSkillsRepository.findAllById(id);
  }

  @Override
  public List<ProductSkillsEntity> findAllByProductId(int productId) {
    return productSkillsRepository.findAllByProductId(productId);
  }

  @Override
  public List<ProductSkillsEntity> findAllByProductIdAndName(int productId, String name) {
    return productSkillsRepository.findAllByProductIdAndName(productId, name);
  }

  @Override
  public String readSkills(String filePath) {
    String skillsPath = binPath; // 上传后的路径
    String content;
    try {
      content = Files.readString(Paths.get(skillsPath + filePath));
    } catch (IOException e) {
      content = "读取skills文件失败";
      log.error("文件读取失败{}", e.getMessage());
    }
    return content;
  }

  @Override
  public JsonResult<?> getProductSkill(String token) {
    String token_deal = token.replace(JwtTokenUtil.TOKEN_PREFIX, "");
    String role = JwtTokenUtil.getUserRole(token_deal);
    String username = JwtTokenUtil.getUsername(token_deal);
    List<ProductSkillsEntity> result;
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
        List<ProductSkillsEntity> productSkillsEntityList =
            productSkillsRepository.findAllByProductId(s.getProductId());
        result.addAll(productSkillsEntityList);
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
        List<ProductSkillsEntity> productSkillsEntityList =
            productSkillsRepository.findAllByProductId(s.getProductId());
        result.addAll(productSkillsEntityList);
      }
    } else {
      result = productSkillsRepository.findAll();
    }
    if (result.isEmpty()) {
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    } else
      return ResultTool.success(result);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> addProductSkill(Integer productId, MultipartFile multipartFile) {
    if (multipartFile == null || multipartFile.isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_COMPLETE);
    }

    String originalFilename = multipartFile.getOriginalFilename();
    if (originalFilename == null || !originalFilename.endsWith(".md")) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }

    String fileName = UUID.randomUUID() + ".md";

    try {
      byte[] bytes = multipartFile.getBytes();

      ClaudeSkillMeta meta = ClaudeSkillMarkdownParser.parse(bytes);

      if (!productSkillsRepository.findAllByProductIdAndName(productId, meta.getName()).isEmpty()) {
        return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
      }

      String markdownPath = binPath;
      MyFileUtil.uploadFile(bytes, markdownPath, fileName);

      ProductSkillsEntity productSkillsEntity = new ProductSkillsEntity();
      productSkillsEntity.setProductId(productId);
      productSkillsEntity.setName(meta.getName());
      productSkillsEntity.setDescription(meta.getDescription());
      productSkillsEntity.setFilePath(fileName);

      productSkillsRepository.save(productSkillsEntity);

      return ResultTool.success();

    } catch (IllegalArgumentException e) {
      log.warn("Invalid Claude skill markdown: {}", e.getMessage());
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    } catch (Exception e) {
      log.error("Upload Claude skill failed", e);
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    }
  }

  public String combineToolDescription(Integer productId) {
    if (productId == null) {
      return "";
    }
    List<ProductSkillsEntity> productSkillsEntityList =
        productSkillsRepository.findAllByProductId(productId);
    if (productSkillsEntityList.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (var toolDescription : productSkillsEntityList) {
      sb.append("Tool: ").append("skills").append(":").append(toolDescription.getName())
          .append("\n");
      sb.append("  Description: ").append(toolDescription.getDescription()).append("\n");
      sb.append("  Input Schema: ").append("args:string").append("\n");
      sb.append("\n");
    }
    sb.append("Tool: ").append("skills").append(":").append(jsExecuteTool.getName()).append("\n");
    sb.append("  Description: ").append(jsExecuteTool.getDescription()).append("\n");
    log.info("getTools: {}", sb);
    return sb.toString();
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> deleteProductSkill(int id) {
    if (productSkillsRepository.findAllById(id).isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    try {
      var result = productSkillsRepository.deleteAllById(id);
      MyFileUtil.deleteFile(binPath + result.get(0).getFilePath());
      return ResultTool.success(result);
    } catch (IOException e) {
      log.error(e.getMessage());
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    }
  }
}
