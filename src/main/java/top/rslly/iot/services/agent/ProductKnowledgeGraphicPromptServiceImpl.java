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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.rslly.iot.dao.ProductKnowledgeGraphicPromptRepository;
import top.rslly.iot.dao.ProductRepository;
import top.rslly.iot.models.ProductKnowledgeGraphicPromptEntity;
import top.rslly.iot.param.request.ProductKnowledgeGraphicPrompt;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import jakarta.annotation.Resource;
import java.util.List;

@Service
public class ProductKnowledgeGraphicPromptServiceImpl
    implements ProductKnowledgeGraphicPromptService {
  @Resource
  private ProductKnowledgeGraphicPromptRepository productKnowledgeGraphicPromptRepository;
  @Resource
  private ProductRepository productRepository;

  @Override
  public List<ProductKnowledgeGraphicPromptEntity> findAllByProductId(int productId) {
    return productKnowledgeGraphicPromptRepository.findAllByProductId(productId);
  }

  @Override
  public JsonResult<?> getProductKnowledgeGraphicPrompt(int productId) {
    var productKnowledgeGraphicPromptEntityList =
        productKnowledgeGraphicPromptRepository.findAllByProductId(productId);
    if (productKnowledgeGraphicPromptEntityList.isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return ResultTool.success(productKnowledgeGraphicPromptEntityList);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> postProductKnowledgeGraphicPrompt(
      ProductKnowledgeGraphicPrompt productKnowledgeGraphicPrompt) {
    if (productRepository.findAllById(productKnowledgeGraphicPrompt.getProductId()).isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    ProductKnowledgeGraphicPromptEntity productKnowledgeGraphicPromptEntity =
        new ProductKnowledgeGraphicPromptEntity();
    var productKnowledgeGraphicPromptEntityList =
        productKnowledgeGraphicPromptRepository
            .findAllByProductId(productKnowledgeGraphicPrompt.getProductId());
    if (!productKnowledgeGraphicPromptEntityList.isEmpty()) {
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    }
    productKnowledgeGraphicPromptEntity.setProductId(productKnowledgeGraphicPrompt.getProductId());
    productKnowledgeGraphicPromptEntity.setPrompt(productKnowledgeGraphicPrompt.getPrompt());
    var result = productKnowledgeGraphicPromptRepository.save(productKnowledgeGraphicPromptEntity);
    return ResultTool.success(result);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> putProductKnowledgeGraphicPrompt(
      ProductKnowledgeGraphicPrompt productKnowledgeGraphicPrompt) {
    if (productKnowledgeGraphicPrompt.getId() == null
        || productRepository.findAllById(productKnowledgeGraphicPrompt.getProductId()).isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    var productKnowledgeGraphicPromptEntityList =
        productKnowledgeGraphicPromptRepository.findAllByIdAndProductId(
            productKnowledgeGraphicPrompt.getId(), productKnowledgeGraphicPrompt.getProductId());
    if (productKnowledgeGraphicPromptEntityList.isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    var duplicateList =
        productKnowledgeGraphicPromptRepository
            .findAllByProductId(productKnowledgeGraphicPrompt.getProductId());
    for (var duplicate : duplicateList) {
      if (!duplicate.getId().equals(productKnowledgeGraphicPrompt.getId())) {
        return ResultTool.fail(ResultCode.COMMON_FAIL);
      }
    }
    ProductKnowledgeGraphicPromptEntity productKnowledgeGraphicPromptEntity =
        productKnowledgeGraphicPromptEntityList.get(0);
    productKnowledgeGraphicPromptEntity.setPrompt(productKnowledgeGraphicPrompt.getPrompt());
    var result = productKnowledgeGraphicPromptRepository.save(productKnowledgeGraphicPromptEntity);
    return ResultTool.success(result);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> deleteProductKnowledgeGraphicPrompt(int id) {
    if (productKnowledgeGraphicPromptRepository.findAllById(id).isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    var result = productKnowledgeGraphicPromptRepository.deleteAllById(id);
    return ResultTool.success(result);
  }
}
