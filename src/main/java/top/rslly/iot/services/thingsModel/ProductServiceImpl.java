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
package top.rslly.iot.services.thingsModel;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.rslly.iot.dao.*;
import top.rslly.iot.models.*;
import top.rslly.iot.param.request.Product;
import top.rslly.iot.utility.JwtTokenUtil;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {
  @Resource
  private ProductRepository productRepository;
  @Resource
  private ProductModelRepository productModelRepository;
  @Resource
  private WxProductBindRepository wxProductBindRepository;
  @Resource
  private UserProductBindRepository userProductBindRepository;
  @Resource
  private WxUserRepository wxUserRepository;
  @Resource
  private UserRepository userRepository;
  @Resource
  private OtaRepository otaRepository;
  @Resource
  private McpServerRepository mcpServerRepository;
  @Resource
  private OtaXiaozhiRepository otaXiaozhiRepository;
  @Resource
  private ProductRoleRepository productRoleRepository;
  @Resource
  private KnowledgeChatRepository knowledgeChatRepository;
  @Resource
  private AgentMemoryRepository agentMemoryRepository;
  @Resource
  private ProductRouterSetRepository productRouterSetRepository;
  @Resource
  private ProductToolsBanRepository productToolsBanRepository;
  @Resource
  private AgentLongMemoryRepository agentLongMemoryRepository;
  @Resource
  private ProductVoiceDiyRepository productVoiceDiyRepository;
  @Resource
  private AdminConfigRepository adminConfigRepository;
  @Resource
  private KnowledgeGraphicNodeRepository knowledgeGraphicNodeRepository;
  @Resource
  private KnowledgeGraphicRelationRepository knowledgeGraphicRelationRepository;
  @Resource
  private KnowledgeGraphicAttributeRepository knowledgeGraphicAttributeRepository;
  @Resource
  private ProductLlmModelRepository productLlmModelRepository;
  @Resource
  private ProductSkillsRepository productSkillsRepository;


  @Override
  public JsonResult<?> getProductName(int id) {
    var productList = productRepository.findAllById(id);
    if (!productList.isEmpty())
      return ResultTool.success(productList.get(0).getProductName());
    return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
  }

  @Override
  public JsonResult<?> getProduct(String token) {
    String token_deal = token.replace(JwtTokenUtil.TOKEN_PREFIX, "");
    String role = JwtTokenUtil.getUserRole(token_deal);
    String username = JwtTokenUtil.getUsername(token_deal);
    List<ProductEntity> result;
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
        List<ProductEntity> productEntities = productRepository.findAllById(s.getProductId());
        result.addAll(productEntities);
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
        List<ProductEntity> productEntities = productRepository.findAllById(s.getProductId());
        result.addAll(productEntities);
      }
    } else {
      result = productRepository.findAll();
    }
    if (result.isEmpty()) {
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    } else
      return ResultTool.success(result);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> postProduct(Product product, String token) {
    String token_deal = token.replace(JwtTokenUtil.TOKEN_PREFIX, "");
    String role = JwtTokenUtil.getUserRole(token_deal);
    String username = JwtTokenUtil.getUsername(token_deal);
    ProductEntity productEntity = new ProductEntity();
    BeanUtils.copyProperties(product, productEntity);
    // List<MqttUserEntity> result = mqttUserRepository.findALLById(productEntity.getMqttUser());
    List<ProductEntity> result =
        productRepository.findAllByProductName(productEntity.getProductName());
    if (!result.isEmpty())
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    else {
      ProductEntity productEntity1 = productRepository.save(productEntity);
      if (role.equals("ROLE_" + "wx_user")) {
        WxProductBindEntity wxProductBindEntity = new WxProductBindEntity();
        var userList = wxUserRepository.findAllByName(username);
        if (userList.isEmpty()) {
          return ResultTool.fail(ResultCode.COMMON_FAIL);
        }
        wxProductBindEntity.setAppid(userList.get(0).getAppid());
        wxProductBindEntity.setOpenid(userList.get(0).getOpenid());
        wxProductBindEntity.setProductId(productEntity1.getId());
        wxProductBindRepository.save(wxProductBindEntity);
      } else if (!role.equals("[ROLE_admin]")) {
        UserProductBindEntity userProductBindEntity = new UserProductBindEntity();
        var userList = userRepository.findAllByUsername(username);
        if (userList.isEmpty()) {
          return ResultTool.fail(ResultCode.COMMON_FAIL);
        }
        userProductBindEntity.setUserId(userList.get(0).getId());
        userProductBindEntity.setProductId(productEntity1.getId());
        userProductBindRepository.save(userProductBindEntity);
      }
      ProductToolsBanEntity entity = new ProductToolsBanEntity();
      entity.setProductId(productEntity1.getId());
      entity.setToolsName("knowledgeGraphic");
      productToolsBanRepository.save(entity);
      return ResultTool.success(productEntity1);
    }
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> deleteProduct(int id) {
    List<ProductModelEntity> productModelEntityList = productModelRepository.findAllByProductId(id);
    List<WxProductBindEntity> wxProductBindEntityList =
        wxProductBindRepository.findAllByProductId(id);
    List<UserProductBindEntity> userProductBindEntityList =
        userProductBindRepository.findAllByProductId(id);
    List<OtaEntity> otaEntityList = otaRepository.findAllByProductId(id);
    List<OtaXiaozhiEntity> otaXiaozhiEntityList = otaXiaozhiRepository.findAllByProductId(id);
    List<McpServerEntity> mcpServerEntityList = mcpServerRepository.findAllByProductId(id);
    List<ProductRoleEntity> productRoleEntityList = productRoleRepository.findAllByProductId(id);
    List<AgentMemoryEntity> agentMemoryEntityList =
        agentMemoryRepository.findAllByChatIdStartingWith("chatProduct" + id);
    List<KnowledgeChatEntity> knowledgeChatEntityList =
        knowledgeChatRepository.findAllByProductId(id);
    List<ProductRouterSetEntity> productRouterSetEntityList =
        productRouterSetRepository.findAllByProductId(id);
    List<ProductToolsBanEntity> productToolsBanEntityList =
        productToolsBanRepository.findAllByProductId(id);
    List<AgentLongMemoryEntity> agentLongMemoryEntityList =
        agentLongMemoryRepository.findAllByProductId(id);
    List<ProductVoiceDiyEntity> productVoiceDiyEntityList =
        productVoiceDiyRepository.findAllByProductId(id);
    List<AdminConfigEntity> adminConfigEntityList =
        adminConfigRepository.findAllBySetKey("wx_default_product");
    List<KnowledgeGraphicNodeEntity> knowledgeGraphicNodeList =
        knowledgeGraphicNodeRepository.findAllByProductId(id);
    List<ProductLlmModelEntity> productLlmModelEntityList =
        productLlmModelRepository.findAllByProductId(id);
    List<ProductSkillsEntity> productSkillsEntityList =
        productSkillsRepository.findAllByProductId(id);
    productToolsBanEntityList.removeIf(productToolsBanEntity -> {
      productToolsBanRepository.delete(productToolsBanEntity);
      return true;
    });
    boolean condition = true;
    condition &= productModelEntityList.isEmpty();
    condition &= wxProductBindEntityList.isEmpty();
    condition &= otaEntityList.isEmpty();
    condition &= otaXiaozhiEntityList.isEmpty();
    condition &= mcpServerEntityList.isEmpty();
    condition &= knowledgeChatEntityList.isEmpty();
    condition &= productRouterSetEntityList.isEmpty();
    condition &= agentLongMemoryEntityList.isEmpty();
    condition &= productVoiceDiyEntityList.isEmpty();
    condition &= productLlmModelEntityList.isEmpty();
    condition &= productSkillsEntityList.isEmpty();
    if (condition) {
      List<ProductEntity> result = productRepository.deleteById(id);
      if (result.isEmpty())
        return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
      else {
        if (!userProductBindEntityList.isEmpty()) {
          userProductBindRepository.deleteAllByProductId(id);
        }
        if (!productRoleEntityList.isEmpty()) {
          productRoleRepository.deleteAllByProductId(id);
        }
        if (!agentMemoryEntityList.isEmpty()) {
          agentMemoryRepository.deleteAllByChatIdStartingWith("chatProduct" + id);
        }
        if (!productLlmModelEntityList.isEmpty()) {
          productLlmModelRepository.deleteAllByProductId(id);
        }
        if (!adminConfigEntityList.isEmpty()) {
          try {
            if (adminConfigEntityList.get(0).getSetValue().equals(String.valueOf(id))) {
              adminConfigRepository.deleteAllById(adminConfigEntityList.get(0).getId());
            }
          } catch (Exception ignore) {
          }
        }
        if (!knowledgeGraphicNodeList.isEmpty()) {
          for (KnowledgeGraphicNodeEntity knowledgeGraphicNodeEntity : knowledgeGraphicNodeList) {
            long kId = knowledgeGraphicNodeEntity.getId();
            knowledgeGraphicRelationRepository.deleteAllByFrom(kId);
            knowledgeGraphicRelationRepository.deleteAllByTo(kId);
            knowledgeGraphicAttributeRepository.deleteByBelong(kId);
          }
          knowledgeGraphicNodeRepository.deleteAllByProductId(id);
        }
        if(!productToolsBanEntityList.isEmpty()){
          productToolsBanRepository.deleteAllByProductId(id);
        }
        return ResultTool.success(result);
      }
    } else {
      return ResultTool.fail(ResultCode.HAS_DEPENDENCIES);
    }

  }

  @Override
  public List<ProductEntity> findAllByProductName(String productName) {
    return productRepository.findAllByProductName(productName);
  }

  @Override
  public List<ProductEntity> findAllById(int productId) {
    return productRepository.findAllById(productId);
  }
}
