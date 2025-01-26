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
package top.rslly.iot.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.rslly.iot.dao.*;
import top.rslly.iot.models.ProductDeviceEntity;
import top.rslly.iot.models.ProductModelEntity;
import top.rslly.iot.param.prompt.ProductDeviceDescription;
import top.rslly.iot.param.request.ProductDevice;
import top.rslly.iot.utility.JwtTokenUtil;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Service
@Slf4j
public class ProductDeviceServiceImpl implements ProductDeviceService {

  @Resource
  private ProductDeviceRepository productDeviceRepository;
  @Resource
  private ProductModelRepository productModelRepository;
  @Resource
  private ProductDataRepository productDataRepository;
  @Resource
  private WxProductBindRepository wxProductBindRepository;
  @Resource
  private UserProductBindRepository userProductBindRepository;
  @Resource
  private WxUserRepository wxUserRepository;
  @Resource
  private UserRepository userRepository;
  @Resource
  private DataServiceImpl dataService;

  @Override
  public List<ProductDeviceEntity> findAllBySubscribeTopic(String subscribeTopic) {
    return productDeviceRepository.findAllBySubscribeTopic(subscribeTopic);
  }

  @Override
  public List<ProductDeviceEntity> findAll() {
    return productDeviceRepository.findAll();
  }

  @Override
  public List<ProductDeviceEntity> findAllByClientId(String clientId) {
    return productDeviceRepository.findAllByClientId(clientId);
  }

  @Override
  public List<ProductDeviceEntity> findAllByName(String name) {
    return productDeviceRepository.findAllByName(name);
  }

  @Override
  public List<ProductDeviceEntity> findAllByModelId(int modelId) {
    return productDeviceRepository.findAllByModelId(modelId);
  }

  @Override
  public List<ProductDeviceEntity> deleteById(int id) {
    return productDeviceRepository.deleteById(id);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public List<ProductDeviceDescription> getDescription(int modelId) {
    var devices = productDeviceRepository.findAllByModelId(modelId);
    var productDataEntities = productDataRepository.findAllByModelId(modelId);
    List<ProductDeviceDescription> productDeviceDescriptionList = new LinkedList<>();
    if (devices.isEmpty() || productDataEntities.isEmpty())
      return productDeviceDescriptionList;
    List<String> properties = new ArrayList<>();
    for (var s : productDataEntities) {
      properties.add(s.getJsonKey());
    }
    for (var s : devices) {
      List<String> valueList = new ArrayList<>();
      ProductDeviceDescription productDeviceDescription = new ProductDeviceDescription();
      productDeviceDescription.setDevice_name(s.getName());
      productDeviceDescription.setOnline(s.getOnline());
      if (s.getAllow() == 0)
        productDeviceDescription.setAllow("false");
      else
        productDeviceDescription.setAllow("true");
      for (var jsonKey : properties) {
        var dataServiceAllBySort = dataService.findAllBySort(s.getId(), jsonKey);
        if (dataServiceAllBySort == null)
          valueList.add("null");// cast error
        else {
          if (!dataServiceAllBySort.isEmpty()) {
            valueList.add(dataServiceAllBySort.get(0).getValue());
          } else {
            valueList.add("null");
          }
        }
      }
      productDeviceDescription.setProperties(properties);
      productDeviceDescription.setValues(valueList);
      productDeviceDescription.setDescription(s.getDescription());
      productDeviceDescriptionList.add(productDeviceDescription);
    }

    return productDeviceDescriptionList;
  }

  @Override
  public int updateOnlineByClientId(String online, String clientId) {
    return productDeviceRepository.updateOnlineByClientId(online, clientId);
  }

  @Override
  public JsonResult<?> getProductDevice(String token) {
    String token_deal = token.replace(JwtTokenUtil.TOKEN_PREFIX, "");
    String role = JwtTokenUtil.getUserRole(token_deal);
    String username = JwtTokenUtil.getUsername(token_deal);
    List<ProductDeviceEntity> result;
    if (role.equals("ROLE_" + "wx_user")) {
      if (wxUserRepository.findAllByName(username).isEmpty()) {
        return ResultTool.fail(ResultCode.COMMON_FAIL);
      }
      String openid = wxUserRepository.findAllByName(username).get(0).getOpenid();
      result = new ArrayList<>();
      var wxBindProductResponseList = wxProductBindRepository.findProductIdByOpenid(openid);
      if (wxBindProductResponseList.isEmpty()) {
        return ResultTool.fail(ResultCode.COMMON_FAIL);
      }
      for (var s : wxBindProductResponseList) {
        List<ProductModelEntity> productModelEntities =
            productModelRepository.findAllByProductId(s.getProductId());
        for (var s1 : productModelEntities) {
          List<ProductDeviceEntity> productDeviceEntityList =
              productDeviceRepository.findAllByModelId(s1.getId());
          result.addAll(productDeviceEntityList);
        }
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
        List<ProductModelEntity> productModelEntities =
            productModelRepository.findAllByProductId(s.getProductId());
        for (var s1 : productModelEntities) {
          List<ProductDeviceEntity> productDeviceEntityList =
              productDeviceRepository.findAllByModelId(s1.getId());
          result.addAll(productDeviceEntityList);
        }
      }
    } else {
      result = productDeviceRepository.findAll();
    }
    if (result.isEmpty()) {
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    } else
      return ResultTool.success(result);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> postProductDevice(ProductDevice productDevice) {
    ProductDeviceEntity productDeviceEntity = new ProductDeviceEntity();
    productDeviceEntity.setOnline("disconnected");
    BeanUtils.copyProperties(productDevice, productDeviceEntity);
    List<ProductModelEntity> result =
        productModelRepository.findAllById(productDeviceEntity.getModelId());
    List<ProductDeviceEntity> p1 =
        productDeviceRepository.findAllByClientId(productDeviceEntity.getClientId());
    List<ProductDeviceEntity> p2 =
        productDeviceRepository.findAllByName(productDeviceEntity.getName());
    if (result.isEmpty() || !p1.isEmpty() || !p2.isEmpty())
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    else if (productDeviceEntity.getName().matches(".*[\\u4E00-\\u9FA5]+.*"))
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    else {
      productDeviceEntity.setSubscribeTopic(
          "/oc/devices/" + productDeviceEntity.getName() + "/sys/" + "properties/report");
      ProductDeviceEntity productDeviceEntity1 = productDeviceRepository.save(productDeviceEntity);
      return ResultTool.success(productDeviceEntity1);
    }
  }

  @Override
  public JsonResult<?> deleteProductDevice(int id) {
    List<ProductDeviceEntity> result = productDeviceRepository.deleteById(id);
    if (result.isEmpty())
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    else {
      return ResultTool.success(result);
    }
  }
}
