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

import org.springframework.stereotype.Service;
import top.rslly.iot.dao.ProductRepository;
import top.rslly.iot.dao.WxProductBindRepository;
import top.rslly.iot.dao.WxUserRepository;
import top.rslly.iot.models.WxProductBindEntity;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import javax.annotation.Resource;
import java.util.List;

@Service
public class WxProductBindServiceImpl implements WxProductBindService {
  @Resource
  private WxProductBindRepository wxProductBindRepository;
  @Resource
  private WxUserRepository wxUserRepository;
  @Resource
  private ProductRepository productRepository;

  @Override
  public JsonResult<?> wxBindProduct(WxProductBindEntity wxProductBindEntity) {
    if (wxUserRepository.findAllByOpenid(wxProductBindEntity.getOpenid()).isEmpty() ||
        productRepository.findAllById(wxProductBindEntity.getProductId()).isEmpty())
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    var result = wxProductBindRepository.save(wxProductBindEntity);
    return ResultTool.success(result);

  }

  @Override
  public boolean wxBindProduct(String openid, String productName, String productKey) {
    if (productName == null || productKey == null)
      return false;
    var productList = productRepository.findAllByProductNameAndKeyvalue(productName, productKey);
    if (wxUserRepository.findAllByOpenid(openid).isEmpty() || productList.isEmpty() ||
        productRepository.findAllById(productList.get(0).getId()).isEmpty())
      return false;
    WxProductBindEntity wxProductBindEntity = new WxProductBindEntity();
    wxProductBindEntity.setProductId(productList.get(0).getId());
    wxProductBindEntity.setOpenid(openid);
    wxProductBindRepository.save(wxProductBindEntity);
    return true;
  }

  @Override
  public JsonResult<?> wxUnBindProduct(Long id) {
    wxUserRepository.deleteById(id);
    return ResultTool.success();
  }

  @Override
  public List<WxProductBindEntity> findByOpenidAndProductId(String openid, int productId) {
    return wxProductBindRepository.findByOpenidAndProductId(openid, productId);
  }

  @Override
  public List<WxProductBindEntity> findAllByOpenid(String openid) {
    return wxProductBindRepository.findAllByOpenid(openid);
  }

  @Override
  public JsonResult<?> getWxProductBind() {
    return ResultTool.success(wxProductBindRepository.findAll());
  }

}
