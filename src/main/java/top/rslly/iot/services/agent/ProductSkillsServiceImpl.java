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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@Slf4j
public class ProductSkillsServiceImpl implements ProductSkillsService {

  /** zip 技能包体积上限 */
  private static final long MAX_ZIP_BYTES = 20L * 1024 * 1024;

  /** zip 技能包条目数上限 */
  private static final int MAX_ZIP_ENTRIES = 500;

  /** zip 技能包解压后总字节数上限 */
  private static final long MAX_ZIP_UNCOMPRESSED_BYTES = 100L * 1024 * 1024;

  /** zip 包内技能元数据文件名 */
  private static final String SKILL_MD_NAME = "SKILL.md";

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
      content = "Failed to read skill file";
      log.error("文件读取失败{}", e.getMessage());
    }
    return content;
  }

  @Override
  public byte[] readSkillFileBytes(String filePath) throws IOException {
    return Files.readAllBytes(Paths.get(binPath + filePath));
  }

  @Override
  public String readSkillMarkdown(String filePath) throws IOException {
    byte[] fileBytes = readSkillFileBytes(filePath);
    if (filePath != null && filePath.endsWith(".zip")) {
      byte[] skillMd = extractSkillMarkdown(fileBytes);
      return new String(skillMd, StandardCharsets.UTF_8);
    }
    return new String(fileBytes, StandardCharsets.UTF_8);
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
    boolean isZip = originalFilename != null && originalFilename.endsWith(".zip");
    if (originalFilename == null
        || (!originalFilename.endsWith(".md") && !isZip)) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }

    String fileName = UUID.randomUUID() + (isZip ? ".zip" : ".md");

    try {
      byte[] bytes = multipartFile.getBytes();

      // zip 包解析出 SKILL.md 的 front-matter 元数据; md 直接解析
      ClaudeSkillMeta meta;
      if (isZip) {
        meta = ClaudeSkillMarkdownParser.parse(extractSkillMarkdown(bytes));
      } else {
        meta = ClaudeSkillMarkdownParser.parse(bytes);
      }

      if (!productSkillsRepository.findAllByProductIdAndName(productId, meta.getName()).isEmpty()) {
        return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
      }

      // 入库前校验长度, 避免事务中途 ConstraintViolationException 导致回滚异常
      if (meta.getName().length() > 255) {
        log.warn("Skill name exceeds 255 chars: {}", meta.getName().length());
        return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
      }
      if (meta.getDescription().length() > 6144) {
        log.warn("Skill description exceeds 6144 chars: {}", meta.getDescription().length());
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

  /**
   * 从 zip 技能包中提取 SKILL.md 字节内容.
   *
   * <p>
   * 校验规则: zip ≤ 20MB、条目 ≤ 500、解压总量 ≤ 100MB、条目路径不允许越出包根(zip-slip 防护)。 SKILL.md 必须位于 zip 根目录,
   * 或包内仅有一个顶层文件夹且 SKILL.md 在该文件夹内。
   *
   * @param zipBytes zip 技能包原始字节
   * @return SKILL.md 文件内容
   * @throws IllegalArgumentException zip 不满足上述任一规则时
   */
  private byte[] extractSkillMarkdown(byte[] zipBytes) {
    if (zipBytes.length > MAX_ZIP_BYTES) {
      throw new IllegalArgumentException("zip skill package exceeds the 20MB size limit");
    }
    Set<String> topLevelNames = new HashSet<>();
    // 候选: 根目录 SKILL.md 或 <singleTopDir>/SKILL.md
    byte[] rootSkillMd = null;
    byte[] nestedSkillMd = null;
    int entries = 0;
    long totalUncompressed = 0;
    try (ZipInputStream zis =
        new ZipInputStream(new ByteArrayInputStream(zipBytes), StandardCharsets.UTF_8)) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        if (++entries > MAX_ZIP_ENTRIES) {
          throw new IllegalArgumentException(
              "zip skill package contains too many entries (limit 500)");
        }
        String name = entry.getName();
        if (name == null || name.isBlank()) {
          continue;
        }
        // zip-slip 防护: normalize 后不得越出包根
        Path normalized = Paths.get(name).normalize();
        if (normalized.startsWith("..") || normalized.isAbsolute()) {
          throw new IllegalArgumentException(
              "zip skill package contains an unsafe entry path: " + name);
        }
        String firstSegment = normalized.getName(0).toString();
        topLevelNames.add(firstSegment);
        if (entry.isDirectory()) {
          continue;
        }
        boolean isSkillMdFile = normalized.getFileName().toString().equals(SKILL_MD_NAME);
        boolean isRootSkillMd = isSkillMdFile && normalized.getNameCount() == 1;
        boolean isNestedSkillMd = isSkillMdFile && normalized.getNameCount() == 2;
        if (isRootSkillMd || isNestedSkillMd) {
          byte[] content = readEntryWithLimit(zis, name);
          totalUncompressed += content.length;
          if (isRootSkillMd) {
            rootSkillMd = content;
          } else {
            nestedSkillMd = content;
          }
        } else {
          totalUncompressed += drainEntry(zis);
        }
        if (totalUncompressed > MAX_ZIP_UNCOMPRESSED_BYTES) {
          throw new IllegalArgumentException(
              "zip skill package exceeds the 100MB uncompressed size limit");
        }
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to read zip skill package: " + e.getMessage());
    }
    if (rootSkillMd != null) {
      return rootSkillMd;
    }
    if (nestedSkillMd != null && topLevelNames.size() == 1) {
      return nestedSkillMd;
    }
    throw new IllegalArgumentException(
        "zip skill package must contain a SKILL.md at its root or inside a single top-level folder");
  }

  /** 读取单个条目内容, 超出单条 20MB 视为异常 */
  private byte[] readEntryWithLimit(ZipInputStream zis, String name) throws IOException {
    java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
    byte[] buffer = new byte[8192];
    int n;
    while ((n = zis.read(buffer)) != -1) {
      bos.write(buffer, 0, n);
      if (bos.size() > MAX_ZIP_BYTES) {
        throw new IllegalArgumentException("zip entry too large: " + name);
      }
    }
    return bos.toByteArray();
  }

  /** 丢弃单个条目内容并统计字节数 */
  private long drainEntry(ZipInputStream zis) throws IOException {
    byte[] buffer = new byte[8192];
    long total = 0;
    int n;
    while ((n = zis.read(buffer)) != -1) {
      total += n;
    }
    return total;
  }
}
