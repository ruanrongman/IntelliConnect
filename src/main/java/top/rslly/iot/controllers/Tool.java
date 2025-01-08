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
package top.rslly.iot.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import top.rslly.iot.param.request.AiControl;
import top.rslly.iot.param.request.ControlParam;
import top.rslly.iot.param.request.MetaData;
import top.rslly.iot.param.request.ReadData;
import top.rslly.iot.services.*;
import top.rslly.iot.utility.RedisUtil;
import top.rslly.iot.utility.RuntimeMessage;
import top.rslly.iot.utility.SseEmitterUtil;
import top.rslly.iot.utility.ai.chain.Router;
import top.rslly.iot.utility.ai.tools.ControlTool;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultTool;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

@RestController
@RequestMapping(value = "/api/v2")
public class Tool {
  @Autowired
  private DataServiceImpl dataService;
  @Autowired
  private EventStorageServiceImpl eventStorageService;
  @Autowired
  private HardWareServiceImpl hardWareService;
  @Autowired
  private OtaServiceImpl otaService;
  @Autowired
  private AiServiceImpl aiService;

  @Operation(summary = "用于获取平台运行环境信息", description = "单位为百分比")
  @RequestMapping(value = "/machineMessage", method = RequestMethod.GET)
  public JsonResult<?> machineMessage() {
    return ResultTool.success(RuntimeMessage.getMessage());
  }

  @Operation(summary = "设备属性或服务控制api接口", description = "注意传入参数为ControlParam,属性或服务设置重复时候取第一个")
  @RequestMapping(value = "/control", method = RequestMethod.POST)
  public JsonResult<?> control(@RequestBody ControlParam controlParam,
      @RequestHeader("Authorization") String header) throws MqttException {

    return hardWareService.control(controlParam, header);
  }

  @Operation(summary = "用于获取物联网一段时间的设备数据", description = "时间参数请使用两个毫秒时间戳")
  @RequestMapping(value = "/readData", method = RequestMethod.POST)
  public JsonResult<?> readData(@RequestBody ReadData readData) {
    return dataService.findAllByTimeBetweenAndDeviceNameAndJsonKey(readData.getTime1(),
        readData.getTime2(),
        readData.getName(), readData.getJsonKey());
  }

  @Operation(summary = "用于获取物联网一段时间的设备事件数据", description = "时间参数请使用两个毫秒时间戳")
  @RequestMapping(value = "/readEvent", method = RequestMethod.POST)
  public JsonResult<?> readEvent(@RequestBody ReadData readData) {
    return eventStorageService.findAllByTimeBetweenAndDeviceNameAndJsonKey(readData.getTime1(),
        readData.getTime2(),
        readData.getName(), readData.getJsonKey());
  }

  @Operation(summary = "获取属性实时数据", description = "高性能接口(带redis缓存)")
  @RequestMapping(value = "/metaData", method = RequestMethod.POST)
  public JsonResult<?> metaData(@RequestBody MetaData metaData) {
    return dataService.metaData(metaData.getDeviceId(), metaData.getJsonKey());
  }

  @Operation(summary = "使用大模型控制设备", description = "响应速度取决于大模型速度")
  @RequestMapping(value = "/aiControl", method = RequestMethod.POST)
  public JsonResult<?> aiControl(@RequestBody AiControl aiControl) {
    return aiService.getAiResponse(aiControl);
  }

  @Operation(summary = "使用大模型控制设备(语音)", description = "响应速度取决于大模型速度")
  @RequestMapping(value = "/aiControl/audio", method = RequestMethod.POST)
  public JsonResult<?> aiControl(@RequestParam("chatId") String chatId,
      @RequestParam("productId") int productId,
      @RequestParam("tts") boolean tts,
      @RequestPart("file") MultipartFile multipartFile) {
    return aiService.getAiResponse(chatId, tts, false, productId, multipartFile);
  }

  // 流式 使用大模型控制设备(语音）
  @Operation(summary = "使用大模型控制设备(语音）", description = "响应速度取决于大模型速度")
  @RequestMapping(value = "/aiControl/audio/stream", method = RequestMethod.POST)
  public SseEmitter aiControlStream(@RequestParam("chatId") String chatId,
      @RequestParam("productId") int productId,
      @RequestPart("file") MultipartFile multipartFile) {
    aiService.getAiResponse(chatId, true, true, productId, multipartFile);
    return SseEmitterUtil.connect(chatId);
  }

  @Operation(summary = "获取缓存音频文件(禁止调用)", description = "禁止调用")
  @RequestMapping(value = "/ai/tmp_voice/{name}", method = RequestMethod.GET)
  public void audioTmpGet(@PathVariable("name") String name, HttpServletResponse response)
      throws IOException {
    aiService.audioTmpGet(name, response);
  }


  // ota list and delete
  @RequestMapping(value = "/micro/{name}", method = RequestMethod.GET)
  public void micro(@PathVariable("name") String name, HttpServletResponse response)
      throws IOException {
    otaService.otaDevice(name, response);
  }

  @RequestMapping(value = "/otaUpload", method = RequestMethod.POST)
  public JsonResult<?> ota(@RequestParam("name") String name,
      @RequestPart("file") MultipartFile multipartFile) {
    return otaService.uploadBin(name, multipartFile);
  }

  @RequestMapping(value = "/otaList", method = RequestMethod.GET)
  public JsonResult<?> otaList() {
    return otaService.otaList();
  }

  @RequestMapping(value = "/otaDelete", method = RequestMethod.DELETE)
  public JsonResult<?> otaDelete(@RequestParam("name") String name) {
    return otaService.deleteBin(name);
  }

  @RequestMapping(value = "/otaEnable", method = RequestMethod.POST)
  public JsonResult<?> otaEnable(@RequestParam("name") String name,
      @RequestParam("deviceName") String deviceName) {
    return otaService.otaEnable(name, deviceName);
  }



}
