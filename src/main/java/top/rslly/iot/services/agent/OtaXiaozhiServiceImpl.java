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

import cn.hutool.captcha.generator.RandomGenerator;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import springfox.documentation.oas.mappers.OasLicenceMapper;
import top.rslly.iot.dao.*;
import top.rslly.iot.models.OtaXiaozhiEntity;
import top.rslly.iot.models.ProductEntity;
import top.rslly.iot.param.request.OtaXiaozhi;
import top.rslly.iot.utility.JwtTokenUtil;
import top.rslly.iot.utility.RedisUtil;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class OtaXiaozhiServiceImpl implements OtaXiaozhiService {
  @Resource
  private OtaXiaozhiRepository otaXiaozhiRepository;
  @Resource
  private ProductRepository productRepository;
  @Resource
  private WxProductBindRepository wxProductBindRepository;
  @Resource
  private UserProductBindRepository userProductBindRepository;
  @Resource
  private WxUserRepository wxUserRepository;
  @Resource
  private UserRepository userRepository;
  @Autowired
  private RedisUtil redisUtil;
  @Value("${ota.xiaozhi.url}")
  private String otaUrl;
  @Value("${ota.xiaozhi.version}")
  private String otaVersion;

  @Override
  public JsonResult<?> otaList(String token) {
    String token_deal = token.replace(JwtTokenUtil.TOKEN_PREFIX, "");
    String role = JwtTokenUtil.getUserRole(token_deal);
    String username = JwtTokenUtil.getUsername(token_deal);
    List<OtaXiaozhiEntity> result;
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
        List<OtaXiaozhiEntity> otaXiaozhiEntityList =
            otaXiaozhiRepository.findAllByProductId(s.getProductId());
        result.addAll(otaXiaozhiEntityList);
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
        List<OtaXiaozhiEntity> otaXiaozhiEntityList =
            otaXiaozhiRepository.findAllByProductId(s.getProductId());
        result.addAll(otaXiaozhiEntityList);
      }
    } else {
      result = otaXiaozhiRepository.findAll();
    }
    if (result.isEmpty()) {
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    } else
      return ResultTool.success(result);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> bindDevice(OtaXiaozhi otaXiaozhi, String token) {
    String token_deal = token.replace(JwtTokenUtil.TOKEN_PREFIX, "");
    String role = JwtTokenUtil.getUserRole(token_deal);
    String username = JwtTokenUtil.getUsername(token_deal);
    OtaXiaozhiEntity otaXiaozhiEntity = new OtaXiaozhiEntity();
    List<ProductEntity> productEntityList =
        productRepository.findAllById(otaXiaozhi.getProductId());
    if (productEntityList.isEmpty() || role.equals("ROLE_" + "wx_user")
        || !redisUtil.hasKey(otaXiaozhi.getCode())) {
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    }
    String mac = (String) redisUtil.get(otaXiaozhi.getCode());
    var p1 = otaXiaozhiRepository.findAllByDeviceId(mac);
    var p2 = otaXiaozhiRepository.findAllByProductIdAndDeviceId(otaXiaozhi.getProductId(), mac);
    if (!p1.isEmpty() || !p2.isEmpty()) {
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    }
    otaXiaozhiEntity.setDeviceId(mac);
    otaXiaozhiEntity.setProductId(otaXiaozhi.getProductId());
    otaXiaozhiEntity.setUserName(username);
    otaXiaozhiEntity.setRole(role);
    var result = otaXiaozhiRepository.save(otaXiaozhiEntity);
    return ResultTool.success(result);
  }

  @Override
  public String otaEnable(HttpServletRequest request) {
    Map<String, Object> response = new HashMap<>();
    try {
      // Retrieve and validate headers
      String activationVersion = request.getHeader("Activation-Version");
      String deviceId = request.getHeader("Device-Id");
      String clientId = request.getHeader("Client-Id");
      if (activationVersion == null || deviceId == null || clientId == null) {
        throw new IllegalArgumentException(
            "Required headers missing: Activation-Version, Device-Id or Client-Id");
      }
      String version = "1";
      // Parse JSON body safely
      JSONObject body = JSON.parseObject(request.getInputStream(), JSONObject.class);
      log.info("body: " + body);
      if (body == null || !body.containsKey("application")) {
        throw new IllegalArgumentException("Missing 'application' object in request body");
      }
      JSONObject app = body.getJSONObject("application");
      String appVersion = app.getString("version");
      if (appVersion == null) {
        throw new IllegalArgumentException("Missing 'version' in application object");
      }
      if (body.containsKey("version")) {
        version = body.getString("version");
      }

      // server_time
      Map<String, Object> serverTime = new HashMap<>();
      long timestamp = Instant.now().toEpochMilli();
      ZoneId zone = ZoneId.systemDefault();
      ZoneOffset offset = ZonedDateTime.now(zone).getOffset();
      serverTime.put("timestamp", timestamp);
      serverTime.put("timeZone", zone.toString());
      serverTime.put("timezone_offset", offset.getTotalSeconds() / 60);
      response.put("server_time", serverTime);

      // firmware based on activation status
      Map<String, Object> firmware = new HashMap<>();
      firmware.put("version", otaVersion);
      firmware.put("url", "");
      response.put("firmware", firmware);

      var otaXiaozhiEntityList = otaXiaozhiRepository.findAllByDeviceId(deviceId);
      // websocket
      Map<String, Object> websocket = new HashMap<>();
      if (otaXiaozhiEntityList.isEmpty()) {
        websocket.put("url", otaUrl + "/xiaozhi/v1/register");
        String token = JwtTokenUtil.createToken(deviceId, "xiaozhi_ota");
        websocket.put("token", token);
      } else {
        var res = otaXiaozhiRepository.findAllByDeviceId(deviceId);
        websocket.put("url", otaUrl + "/xiaozhi/v1/" + res.get(0).getProductId());
        String token = JwtTokenUtil.createToken(res.get(0).getUserName(), res.get(0).getRole());
        websocket.put("token", token);
      }
      response.put("websocket", websocket);

      // activation info if not yet activated
      if (otaXiaozhiEntityList.isEmpty() && version.equals("2")) {
        RandomGenerator randomGenerator = new RandomGenerator("0123456789", 6);
        String code = randomGenerator.generate();
        redisUtil.set(code, deviceId, 60 * 5);
        Map<String, Object> activation = new HashMap<>();
        activation.put("message", "control.rslly.top\n" + code);
        activation.put("code", code);
        activation.put("challenge", deviceId);
        activation.put("timeout_ms", 30000);
        response.put("activation", activation);
      }

    } catch (IOException e) {
      response.put("status", "error");
      response.put("message", "Failed to read request: " + e.getMessage());
    } catch (IllegalArgumentException e) {
      response.clear();
      response.put("status", "error");
      response.put("message", e.getMessage());
    } catch (Exception e) {
      response.clear();
      response.put("status", "error");
      response.put("message", "Unexpected error: " + e.getMessage());
    }
    return JSON.toJSONString(response, SerializerFeature.PrettyFormat);
  }

  @Override
  public void otaActivate(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    JSONObject result = new JSONObject();
    try {
      JSONObject body = JSON.parseObject(request.getInputStream(), JSONObject.class);
      if (body == null) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        result.put("error", "Request body is missing or malformed JSON");
      } else if (!body.containsKey("Payload")) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        result.put("error", "Missing Payload in request body");
      } else {
        if (!otaXiaozhiRepository
            .findAllByDeviceId(body.getJSONObject("Payload").getString("challenge")).isEmpty()) {
          response.setStatus(HttpServletResponse.SC_OK);
        } else {
          response.setStatus(HttpServletResponse.SC_ACCEPTED);
        }
      }
    } catch (IOException e) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      result.put("error", "Failed to read request: " + e.getMessage());
    } catch (Exception e) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      result.put("error", "Unexpected error: " + e.getMessage());
    }
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.getWriter().write(JSON.toJSONString(result, SerializerFeature.PrettyFormat));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> unbound(String deviceId, String token) {
    String token_deal = token.replace(JwtTokenUtil.TOKEN_PREFIX, "");
    String role = JwtTokenUtil.getUserRole(token_deal);
    String username = JwtTokenUtil.getUsername(token_deal);
    var otaXiaozhiEntityList =
        otaXiaozhiRepository.findAllByDeviceIdAndUserNameAndRole(deviceId, username, role);
    if (otaXiaozhiEntityList.isEmpty()) {
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    } else {
      otaXiaozhiRepository.delete(otaXiaozhiEntityList.get(0));
      return ResultTool.success();
    }
  }
}
