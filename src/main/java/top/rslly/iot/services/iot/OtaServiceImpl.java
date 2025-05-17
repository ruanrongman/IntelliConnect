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
package top.rslly.iot.services.iot;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import top.rslly.iot.dao.*;
import top.rslly.iot.models.OtaEntity;
import top.rslly.iot.models.ProductEntity;
import top.rslly.iot.transfer.mqtt.MqttConnectionUtils;
import top.rslly.iot.utility.JwtTokenUtil;
import top.rslly.iot.utility.MyFileUtil;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Service
@Slf4j
public class OtaServiceImpl implements OtaService {
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
  @Resource
  private OtaRepository otaRepository;
  @Resource
  private ProductDeviceRepository productDeviceRepository;
  @Resource
  private ProductModelRepository productModelRepository;
  @Value("${ota.bin.path}")
  private String binPath;

  @Override
  public List<OtaEntity> findAllByName(String name) {
    return otaRepository.findAllByName(name);
  }

  @Override
  public JsonResult<?> uploadBin(String name, int productId, MultipartFile multipartFile) {
    if (multipartFile.isEmpty())
      return ResultTool.fail(ResultCode.PARAM_NOT_COMPLETE);
    if (!otaRepository.findAllByName(name).isEmpty())
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    // String binName = UUID.randomUUID().toString();

    String fileName = multipartFile.getOriginalFilename();
    if (fileName == null)
      return ResultTool.fail(ResultCode.PARAM_NOT_COMPLETE);
    String suffixName = fileName.substring(fileName.lastIndexOf(".")); // 后缀名
    if (!suffixName.equals(".bin"))
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    String filePath = binPath; // 上传后的路径
    fileName = UUID.randomUUID() + suffixName; // 新文件名

    OtaEntity otaEntity = new OtaEntity();
    otaEntity.setName(name);
    otaEntity.setProductId(productId);
    otaEntity.setPath(fileName);
    try {

      MyFileUtil.uploadFile(multipartFile.getBytes(), filePath, fileName);
      otaRepository.save(otaEntity);
    } catch (Exception e) {
      log.error(e.getMessage());
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    }
    return ResultTool.success();
  }

  @Override
  public JsonResult<?> otaList(String token) {
    String token_deal = token.replace(JwtTokenUtil.TOKEN_PREFIX, "");
    String role = JwtTokenUtil.getUserRole(token_deal);
    String username = JwtTokenUtil.getUsername(token_deal);
    List<OtaEntity> result;
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
        List<OtaEntity> otaEntities = otaRepository.findAllByProductId(s.getProductId());
        result.addAll(otaEntities);
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
        List<OtaEntity> otaEntities = otaRepository.findAllByProductId(s.getProductId());
        result.addAll(otaEntities);
      }
    } else {
      result = otaRepository.findAll();
    }
    if (result.isEmpty()) {
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    } else
      return ResultTool.success(result);
  }

  @Override
  public void otaDevice(String name, HttpServletResponse response) throws IOException {
    ServletOutputStream out = response.getOutputStream();
    try {
      String filePath = binPath; // 上传后的路径
      File file = new File(filePath + name);
      response.setCharacterEncoding("UTF-8");
      response.setHeader("Content-Disposition", "attachment; filename=" +
          URLEncoder.encode(name.substring(0, name.lastIndexOf(".")) + ".bin",
              StandardCharsets.UTF_8));
      response.addHeader("Content-Length", "" + file.length());
      out.write(Files.readAllBytes(Paths.get(filePath + name)));
      out.flush();
      out.close();


    } catch (Exception e) {
      log.error(e.getMessage());
      response.setStatus(404);
      out.close();
    }
  }

  @Override
  public JsonResult<?> otaEnable(String name, String deviceName) {
    if (name == null)
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    if (deviceName == null)
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    var productDeviceEntityList = productDeviceRepository.findAllByName(deviceName);
    if (productDeviceEntityList.isEmpty())
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    var otaList = otaRepository.findAllByName(name);
    if (otaList.isEmpty())
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    if (otaList.get(0).getProductId() != productModelRepository
        .findAllById(productDeviceEntityList.get(0).getModelId()).get(0).getProductId()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    // MqttConnectionUtils.publish("/ota/"+deviceName, otaList.get(0).getPath());
    // var jsonStr="{\"fileName\""+"\""+otaList.get(0).getPath()+"\""+"}";
    Map<String, String> map = new HashMap<>();
    map.put("fileName", otaList.get(0).getPath());
    String version = UUID.randomUUID().toString();
    map.put("version", version);
    try {
      map.put("md5", DigestUtils.md5Hex(new FileInputStream(binPath + otaList.get(0).getPath())));
    } catch (IOException e) {
      log.error(e.getMessage());
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    }
    var jsonStr = JSON.toJSONString(map);
    try {
      MqttConnectionUtils.publish("/oc/devices/" + deviceName + "/sys/" + "ota", jsonStr, 1);
    } catch (MqttException e) {
      log.error(e.getMessage());
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    }
    return ResultTool.success(otaList);
  }

  @Override
  public JsonResult<?> deleteBin(String name) {
    if (name == null)
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    var otaList = otaRepository.findAllByName(name);
    if (otaList.isEmpty())
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    try {
      MyFileUtil.deleteFile(binPath + otaList.get(0).getPath());
    } catch (IOException e) {
      log.error(e.getMessage());
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    }
    otaRepository.delete(otaList.get(0));
    return ResultTool.success();
  }
}
