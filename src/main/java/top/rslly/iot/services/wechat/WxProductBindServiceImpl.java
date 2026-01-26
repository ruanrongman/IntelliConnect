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
package top.rslly.iot.services.wechat;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.rslly.iot.dao.ProductRepository;
import top.rslly.iot.dao.WxProductActiveRepository;
import top.rslly.iot.dao.WxProductBindRepository;
import top.rslly.iot.dao.WxUserRepository;
import top.rslly.iot.models.WxProductBindEntity;
import top.rslly.iot.param.request.WxBindProduct;
import top.rslly.iot.param.response.WxBindProductResponse;
import top.rslly.iot.services.wechat.WxProductBindService;
import top.rslly.iot.utility.JwtTokenUtil;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import jakarta.annotation.Resource;
import java.util.List;

@Service
public class WxProductBindServiceImpl implements WxProductBindService {
  @Resource
  private WxProductBindRepository wxProductBindRepository;
  @Resource
  private WxProductActiveRepository wxProductActiveRepository;
  @Resource
  private WxUserRepository wxUserRepository;
  @Resource
  private ProductRepository productRepository;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> wxBindProduct(WxProductBindEntity wxProductBindEntity) {
    if (wxUserRepository
        .findAllByAppidAndOpenid(wxProductBindEntity.getAppid(), wxProductBindEntity.getOpenid())
        .isEmpty() ||
        productRepository.findAllById(wxProductBindEntity.getProductId()).isEmpty())
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    var result = wxProductBindRepository.save(wxProductBindEntity);
    return ResultTool.success(result);

  }

  @Override
  public JsonResult<?> wxGetBindProduct(String token) {
    String token_deal = token.replace(JwtTokenUtil.TOKEN_PREFIX, "");
    String role = JwtTokenUtil.getUserRole(token_deal);
    String wx_username = JwtTokenUtil.getUsername(token_deal);
    var wxUsers = wxUserRepository.findAllByName(wx_username);
    if (!role.equals("ROLE_" + "wx_user") || wxUsers.isEmpty())
      return ResultTool.fail(ResultCode.USER_ACCOUNT_NOT_EXIST);
    List<WxBindProductResponse> wxBindProductResponses =
        wxProductBindRepository.findProductIdByAppidAndOpenid(wxUsers.get(0).getAppid(),
            wxUsers.get(0).getOpenid());
    if (wxBindProductResponses.isEmpty())
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    else {
      return ResultTool.success(wxBindProductResponses);
    }
  }

  @Override
  public JsonResult<?> wxBindProduct(WxBindProduct wxBindProduct, String token) {
    String token_deal = token.replace(JwtTokenUtil.TOKEN_PREFIX, "");
    String role = JwtTokenUtil.getUserRole(token_deal);
    String wx_username = JwtTokenUtil.getUsername(token_deal);
    var wxUsers = wxUserRepository.findAllByName(wx_username);
    if (!role.equals("ROLE_" + "wx_user") || wxUsers.isEmpty())
      return ResultTool.fail(ResultCode.USER_ACCOUNT_NOT_EXIST);
    var res = this.wxBindProduct(wxUsers.get(0).getAppid(), wxUsers.get(0).getOpenid(),
        wxBindProduct.getProductName(), wxBindProduct.getProductKey());
    if (res)
      return ResultTool.success();
    else
      return ResultTool.fail(ResultCode.USER_CREDENTIALS_ERROR);
  }

  @Override
  public JsonResult<?> wxUnBindProduct(WxBindProduct wxBindProduct, String token) {
    String token_deal = token.replace(JwtTokenUtil.TOKEN_PREFIX, "");
    String role = JwtTokenUtil.getUserRole(token_deal);
    String wx_username = JwtTokenUtil.getUsername(token_deal);
    var wxUsers = wxUserRepository.findAllByName(wx_username);
    if (!role.equals("ROLE_" + "wx_user") || wxUsers.isEmpty())
      return ResultTool.fail(ResultCode.USER_ACCOUNT_NOT_EXIST);
    var res = this.wxUnBindProduct(wxUsers.get(0).getAppid(), wxUsers.get(0).getOpenid(),
        wxBindProduct.getProductName(), wxBindProduct.getProductKey());
    if (res)
      return ResultTool.success();
    else
      return ResultTool.fail(ResultCode.USER_CREDENTIALS_ERROR);
  }


  @Override
  @Transactional(rollbackFor = Exception.class)
  public boolean wxBindProduct(String appid, String openid, String productName, String productKey) {
    if (productName == null || productKey == null)
      return false;
    var productList = productRepository.findAllByProductNameAndKeyvalue(productName, productKey);
    if (wxUserRepository.findAllByAppidAndOpenid(appid, openid).isEmpty() || productList.isEmpty()
        ||
        productRepository.findAllById(productList.get(0).getId()).isEmpty())
      return false;
    WxProductBindEntity wxProductBindEntity = new WxProductBindEntity();
    wxProductBindEntity.setProductId(productList.get(0).getId());
    wxProductBindEntity.setOpenid(openid);
    wxProductBindEntity.setAppid(appid);
    wxProductBindRepository.save(wxProductBindEntity);
    return true;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public boolean wxUnBindProduct(String appid, String openid, String productName,
      String productKey) {
    if (productName == null || productKey == null)
      return false;
    var productList = productRepository.findAllByProductNameAndKeyvalue(productName, productKey);
    if (wxUserRepository.findAllByAppidAndOpenid(appid, openid).isEmpty() || productList.isEmpty()
        ||
        productRepository.findAllById(productList.get(0).getId()).isEmpty())
      return false;
    wxProductBindRepository.deleteByAppidAndOpenidAndProductId(appid, openid,
        productList.get(0).getId());
    // 设置成这样是为了将其归位，让用户重新选择
    if (!wxProductActiveRepository.findAllByAppidAndOpenid(appid, openid).isEmpty()) {
      var wxProductActiveList = wxProductActiveRepository.findAllByAppidAndOpenid(appid, openid);
      if (wxProductActiveList.get(0).getProductId() == productList.get(0).getId()) {
        wxProductActiveRepository.updateProperty(appid, openid, 0);
      }
    }
    return true;
  }

  @Override
  public List<WxProductBindEntity> findByAppidAndOpenidAndProductId(String appid, String openid,
      int productId) {
    return wxProductBindRepository.findByAppidAndOpenidAndProductId(appid, openid, productId);
  }

  @Override
  public List<WxProductBindEntity> findAllByAppidAndOpenid(String appid, String openid) {
    return wxProductBindRepository.findAllByAppidAndOpenid(appid, openid);
  }


  @Override
  public JsonResult<?> getWxProductBind() {
    return ResultTool.success(wxProductBindRepository.findAll());
  }

}
