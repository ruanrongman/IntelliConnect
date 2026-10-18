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

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;
import top.rslly.iot.models.ProductSkillsEntity;
import top.rslly.iot.param.request.ProductSkills;
import top.rslly.iot.utility.result.JsonResult;

import java.util.List;

public interface ProductSkillsService {
  List<ProductSkillsEntity> findAllById(int id);

  List<ProductSkillsEntity> findAllByProductId(int productId);

  List<ProductSkillsEntity> findAllByProductIdAndName(int productId, String name);

  String readSkills(String filePath);

  /**
   * 读取技能文件的原始字节内容(md 与 zip 均可).
   */
  byte[] readSkillFileBytes(String filePath) throws IOException;

  /**
   * 读取技能的 markdown 内容: md 文件直接返回文本; zip 包返回其中 SKILL.md 的文本.
   */
  String readSkillMarkdown(String filePath) throws IOException;

  JsonResult<?> getProductSkill(String token);

  JsonResult<?> addProductSkill(Integer productId, MultipartFile multipartFile);

  JsonResult<?> deleteProductSkill(int id);
}
