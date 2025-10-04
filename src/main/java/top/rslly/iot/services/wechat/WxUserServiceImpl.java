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

import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.rslly.iot.dao.WxUserRepository;
import top.rslly.iot.models.WxUserEntity;
import top.rslly.iot.param.request.WxUser;
import top.rslly.iot.utility.JwtTokenUtil;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;
import top.rslly.iot.utility.wx.DealWx;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class WxUserServiceImpl implements WxUserService {
  @Resource
  private WxUserRepository wxUserRepository;
  @Autowired
  private DealWx dealWx;
  @Value("${wx.micro.appid}")
  private String microAppid;
  @Value("${wx.micro.appsecret}")
  private String microAppSecret;

  @Override
  public JsonResult<?> wxLogin(WxUser wxUser) throws IOException {
    String s = dealWx.getOpenid(wxUser.getCode(), microAppid, microAppSecret);
    String openid = (String) JSON.parseObject(s).get("openid");
    // System.out.println(openid);
    List<WxUserEntity> user = wxUserRepository.findAllByAppidAndOpenid(microAppid, openid);
    if (user.isEmpty())
      return ResultTool.fail(ResultCode.USER_ACCOUNT_NOT_EXIST);
    return ResultTool.success(JwtTokenUtil.TOKEN_PREFIX
        + JwtTokenUtil.createToken(user.get(0).getName(), "ROLE_" + "wx_user"));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> wxRegister(WxUser wxUser) throws IOException {
    String s = dealWx.getOpenid(wxUser.getCode(), microAppid, microAppSecret);
    String openid = (String) JSON.parseObject(s).get("openid");
    List<WxUserEntity> user = wxUserRepository.findAllByAppidAndOpenid(microAppid, openid);
    if (!user.isEmpty())
      return ResultTool.fail(ResultCode.USER_ACCOUNT_ALREADY_EXIST);
    WxUserEntity wxUserEntity = new WxUserEntity();
    wxUserEntity.setName(UUID.randomUUID().toString());
    wxUserEntity.setOpenid(openid);
    wxUserEntity.setAppid(microAppid);
    var res = wxUserRepository.save(wxUserEntity);
    return ResultTool.success(res);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public WxUserEntity wxRegister(String appid, String openid) {
    if (appid == null || appid.trim().isEmpty()) {
      throw new IllegalArgumentException("Invalid appid");
    }
    // 校验 openid 是否有效
    if (openid == null || openid.trim().isEmpty()) {
      throw new IllegalArgumentException("Invalid openid");
    }
    if (wxUserRepository.findAllByAppidAndOpenid(appid, openid).isEmpty()) {
      WxUserEntity wxUserEntity = new WxUserEntity();
      wxUserEntity.setName(UUID.randomUUID().toString());
      wxUserEntity.setOpenid(openid);
      wxUserEntity.setAppid(appid);
      wxUserRepository.save(wxUserEntity);
      return wxUserEntity;
    }
    return null;
  }

  @Override
  public List<WxUserEntity> findAllByAppidAndOpenid(String appid, String openid) {
    return wxUserRepository.findAllByAppidAndOpenid(appid, openid);
  }

  @Override
  public List<WxUserEntity> findAllByName(String name) {
    return wxUserRepository.findAllByName(name);
  }

  @Override
  public JsonResult<?> wxGetAllUser() {
    return ResultTool.success(wxUserRepository.findAll());
  }
}
