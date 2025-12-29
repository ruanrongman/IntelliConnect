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

import io.swagger.v3.oas.annotations.Operation;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import top.rslly.iot.param.request.*;
import top.rslly.iot.services.*;
import top.rslly.iot.services.agent.*;
import top.rslly.iot.services.iot.AlarmEventServiceImpl;
import top.rslly.iot.services.iot.HardWareServiceImpl;
import top.rslly.iot.services.iot.OtaPassiveServiceImpl;
import top.rslly.iot.services.iot.OtaServiceImpl;
import top.rslly.iot.services.storage.DataServiceImpl;
import top.rslly.iot.services.storage.EventStorageServiceImpl;
import top.rslly.iot.services.thingsModel.ProductDeviceServiceImpl;
import top.rslly.iot.utility.RuntimeMessage;
import top.rslly.iot.utility.SseEmitterUtil;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/v2")
@Validated
public class Tool {
  @Autowired
  private DataServiceImpl dataService;
  @Autowired
  private EventStorageServiceImpl eventStorageService;
  @Autowired
  private ProductDeviceServiceImpl productDeviceService;
  @Autowired
  private HardWareServiceImpl hardWareService;
  @Autowired
  private OtaServiceImpl otaService;
  @Autowired
  private AiServiceImpl aiService;
  @Autowired
  private AlarmEventServiceImpl alarmEventService;
  @Autowired
  private SafetyServiceImpl safetyService;
  @Autowired
  private OtaPassiveServiceImpl otaPassiveService;
  @Autowired
  private OtaXiaozhiServiceImpl otaXiaozhiService;
  @Autowired
  private KnowledgeChatServiceImpl knowledgeChatService;
  @Autowired
  private OtaXiaozhiPassiveServiceImpl otaXiaozhiPassiveService;
  @Autowired
  private ProductToolsBanServiceImpl productToolsBanService;
  @Autowired
  private AgentLongMemoryServiceImpl agentLongMemoryService;
  @Autowired
  private ProductVoiceDiyServiceImpl productVoiceDiyService;
  @Autowired
  private AgentMemoryServiceImpl agentMemoryService;

  @Operation(summary = "用于获取平台运行环境信息", description = "单位为百分比")
  @RequestMapping(value = "/machineMessage", method = RequestMethod.GET)
  public JsonResult<?> machineMessage() {
    return ResultTool.success(RuntimeMessage.getMessage());
  }

  @Operation(summary = "用于获取连接的设备数量", description = "仅包含当前用户绑定的设备")
  @RequestMapping(value = "/getConnectedNum", method = RequestMethod.GET)
  public JsonResult<?> getConnectedNum(@RequestHeader("Authorization") String header) {
    return ResultTool.success(productDeviceService.getProductDeviceConnectedNum(header));
  }

  @Operation(summary = "设备属性或服务控制api接口", description = "注意传入参数为ControlParam,属性或服务设置重复时候取第一个")
  @RequestMapping(value = "/control", method = RequestMethod.POST)
  public JsonResult<?> control(@Valid @RequestBody ControlParam controlParam,
      @RequestHeader("Authorization") String header) throws MqttException {

    return hardWareService.control(controlParam, header);
  }

  @Operation(summary = "用于获取物联网一段时间的设备数据", description = "时间参数请使用两个毫秒时间戳")
  @RequestMapping(value = "/readData", method = RequestMethod.POST)
  public JsonResult<?> readData(@Valid @RequestBody ReadData readData,
      @RequestHeader("Authorization") String header) {
    try {
      if (!safetyService.controlAuthorizeDevice(header, readData.getName()))
        return ResultTool.fail(ResultCode.NO_PERMISSION);
    } catch (NullPointerException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return dataService.findAllByTimeBetweenAndDeviceNameAndJsonKey(readData.getTime1(),
        readData.getTime2(),
        readData.getName(), readData.getJsonKey());
  }

  @Operation(summary = "用于获取物联网一段时间的设备事件数据", description = "时间参数请使用两个毫秒时间戳")
  @RequestMapping(value = "/readEvent", method = RequestMethod.POST)
  public JsonResult<?> readEvent(@Valid @RequestBody ReadData readData,
      @RequestHeader("Authorization") String header) {
    try {
      if (!safetyService.controlAuthorizeDevice(header, readData.getName()))
        return ResultTool.fail(ResultCode.NO_PERMISSION);
    } catch (NullPointerException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return eventStorageService.findAllByTimeBetweenAndDeviceNameAndJsonKey(readData.getTime1(),
        readData.getTime2(),
        readData.getName(), readData.getJsonKey());
  }

  @Operation(summary = "获取属性实时数据", description = "高性能接口(带redis缓存)")
  @RequestMapping(value = "/metaData", method = RequestMethod.POST)
  public JsonResult<?> metaData(@Valid @RequestBody MetaData metaData,
      @RequestHeader("Authorization") String header) {
    try {
      if (!safetyService.controlAuthorizeDevice(header, metaData.getDeviceId()))
        return ResultTool.fail(ResultCode.NO_PERMISSION);
    } catch (NullPointerException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return dataService.metaData(metaData.getDeviceId(), metaData.getJsonKey());
  }

  @Operation(summary = "使用大模型控制设备", description = "响应速度取决于大模型速度")
  @RequestMapping(value = "/aiControl", method = RequestMethod.POST)
  public JsonResult<?> aiControl(@Valid @RequestBody AiControl aiControl,
      @RequestHeader("Authorization") String header) {
    return aiService.getAiResponse(aiControl, header);
  }

  @Operation(summary = "使用大模型控制设备(语音)", description = "响应速度取决于大模型速度")
  @RequestMapping(value = "/aiControl/audio", method = RequestMethod.POST)
  public JsonResult<?> aiControl(
      @RequestParam("productId") int productId,
      @RequestParam("tts") boolean tts,
      @RequestPart("file") @NotNull(message = "file 不能为空") MultipartFile multipartFile,
      @RequestHeader("Authorization") String header) {
    return aiService.getAiResponse(tts, false, productId, multipartFile, header);
  }

  // 流式 使用大模型控制设备(语音）
  @Operation(summary = "使用大模型控制设备(语音）", description = "响应速度取决于大模型速度")
  @RequestMapping(value = "/aiControl/audio/stream", method = RequestMethod.POST)
  public SseEmitter aiControlStream(
      @RequestParam("productId") int productId,
      @RequestPart("file") @NotNull(message = "file 不能为空") MultipartFile multipartFile,
      @RequestHeader("Authorization") String header) {
    aiService.getAiResponse(true, true, productId, multipartFile, header);
    return SseEmitterUtil.connect("chatProduct" + productId);
  }

  // 知识库
  @Operation(summary = "知识库", description = "获取产品的知识库")
  @RequestMapping(value = "/knowledgeChat", method = RequestMethod.GET)
  public JsonResult<?> getKnowledgeChat(@RequestHeader("Authorization") String header) {
    return knowledgeChatService.getKnowledgeChat(header);
  }

  @Operation(summary = "知识库", description = "搜索产品的知识库")
  @RequestMapping(value = "/knowledgeChatRecall", method = RequestMethod.POST)
  public JsonResult<?> searchKnowledgeChat(
      @Valid @RequestBody KnowledgeChatRecall knowledgeChatRecall,
      @RequestHeader("Authorization") String header) {
    try {
      if (!safetyService.controlAuthorizeProduct(header, knowledgeChatRecall.getProductId()))
        return ResultTool.fail(ResultCode.NO_PERMISSION);
    } catch (NullPointerException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return knowledgeChatService.searchByProductId(knowledgeChatRecall.getProductId(),
        knowledgeChatRecall.getQuery());
  }

  @Operation(summary = "知识库", description = "提交产品的知识库")
  @RequestMapping(value = "/knowledgeChat", method = RequestMethod.POST)
  public JsonResult<?> postKnowledgeChat(@RequestParam("productId") int productId,
      @RequestParam("filename") @NotBlank(message = "filename 不能为空")
      @Size(min = 1, max = 255, message = "fileName 长度必须在 1 到 255 之间") String fileName,
      @RequestPart("file") @NotNull(message = "file 不能为空") MultipartFile multipartFile,
      @RequestHeader("Authorization") String header) {
    try {
      if (!safetyService.controlAuthorizeProduct(header, productId))
        return ResultTool.fail(ResultCode.NO_PERMISSION);
    } catch (NullPointerException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return knowledgeChatService.postKnowledgeChat(productId, fileName, multipartFile);
  }

  @RequestMapping(value = "/knowledgeChat", method = RequestMethod.DELETE)
  public JsonResult<?> deleteKnowledgeChat(@RequestParam("id") int id,
      @RequestHeader("Authorization") String header) {
    try {
      if (!safetyService.controlAuthorizeKnowledgeChat(header, id))
        return ResultTool.fail(ResultCode.NO_PERMISSION);
    } catch (NullPointerException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return knowledgeChatService.deleteKnowledgeChat(id);
  }

  @Operation(summary = "获取缓存音频文件(禁止调用)", description = "禁止调用")
  @RequestMapping(value = "/ai/tmp_voice/{name}", method = RequestMethod.GET)
  public void audioTmpGet(@PathVariable("name") @NotBlank(message = "name 不能为空")
  @Size(min = 1, max = 255, message = "name 长度必须在 1 到 255 之间") String name,
      HttpServletResponse response)
      throws IOException {
    aiService.audioTmpGet(name, response);
  }

  // ota list and delete
  @RequestMapping(value = "/micro/{name}", method = RequestMethod.GET)
  public void micro(@PathVariable("name") @NotBlank(message = "name 不能为空")
  @Size(min = 1, max = 255, message = "name 长度必须在 1 到 255 之间") String name,
      HttpServletResponse response)
      throws IOException {
    otaService.otaDevice(name, response);
  }

  @RequestMapping(value = "/otaUpload", method = RequestMethod.POST)
  public JsonResult<?> ota(@RequestParam("name") @NotBlank(message = "name 不能为空")
  @Size(min = 1, max = 255, message = "name 长度必须在 1 到 255 之间") String name,
      @RequestParam("productId") int productId,
      @RequestPart("file") @NotNull(message = "file 不能为空") MultipartFile multipartFile,
      @RequestHeader("Authorization") String header) {
    try {
      if (!safetyService.controlAuthorizeProduct(header, productId))
        return ResultTool.fail(ResultCode.NO_PERMISSION);
    } catch (NullPointerException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return otaService.uploadBin(name, productId, multipartFile);
  }

  @RequestMapping(value = "/otaList", method = RequestMethod.GET)
  public JsonResult<?> otaList(@RequestHeader("Authorization") String header) {
    return otaService.otaList(header);
  }

  @RequestMapping(value = "/otaDelete", method = RequestMethod.DELETE)
  public JsonResult<?> otaDelete(@RequestParam("id") int id,
      @RequestHeader("Authorization") String header) {
    try {
      if (!safetyService.controlAuthorizeOta(header, id))
        return ResultTool.fail(ResultCode.NO_PERMISSION);
    } catch (NullPointerException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return otaService.deleteBin(id);
  }

  @RequestMapping(value = "/otaEnable", method = RequestMethod.POST)
  public JsonResult<?> otaEnable(@RequestParam("name") @NotBlank(message = "name 不能为空")
  @Size(min = 1, max = 255, message = "name 长度必须在 1 到 255 之间") String name,
      @RequestParam("deviceName") @NotBlank(message = "deviceName 不能为空")
      @Size(min = 1, max = 255, message = "deviceName 长度必须在 1 到 255 之间") String deviceName,
      @RequestHeader("Authorization") String header) {
    try {
      if (!safetyService.controlAuthorizeOta(header, name, deviceName))
        return ResultTool.fail(ResultCode.NO_PERMISSION);
    } catch (NullPointerException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return otaService.otaEnable(name, deviceName);
  }

  @RequestMapping(value = "/otaPassive", method = RequestMethod.GET)
  public JsonResult<?> otaPassiveList(@RequestHeader("Authorization") String header) {
    return otaPassiveService.otaPassiveList(header);
  }

  @RequestMapping(value = "/otaPassive", method = RequestMethod.POST)
  public JsonResult<?> otaPassivePost(@Valid @RequestBody OtaPassive otaPassive,
      @RequestHeader("Authorization") String header) {
    try {
      if (!safetyService.controlAuthorizeDevice(header, otaPassive.getDeviceName()))
        return ResultTool.fail(ResultCode.NO_PERMISSION);
    } catch (NullPointerException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return otaPassiveService.otaPassivePost(otaPassive);
  }

  @RequestMapping(value = "/otaPassiveEnable", method = RequestMethod.GET)
  public JsonResult<?> otaPassiveEnable(
      @RequestParam("deviceName") @NotBlank(message = "deviceName 不能为空")
      @Size(min = 1, max = 255, message = "deviceName 长度必须在 1 到 255 之间") String deviceName) {
    return otaPassiveService.otaPassiveEnable(deviceName);
  }

  @RequestMapping(value = "/otaPassive", method = RequestMethod.DELETE)
  public JsonResult<?> otaPassiveDelete(@RequestParam("id") int id,
      @RequestHeader("Authorization") String header) {
    try {
      if (!safetyService.controlAuthorizeOtaPassive(header, id))
        return ResultTool.fail(ResultCode.NO_PERMISSION);
    } catch (NullPointerException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return otaPassiveService.otaPassiveDelete(id);
  }

  @RequestMapping(value = "/alarmEvent", method = RequestMethod.GET)
  public JsonResult<?> alarmEvent(@RequestHeader("Authorization") String header) {
    return alarmEventService.getAlarmEvent(header);
  }

  @RequestMapping(value = "/alarmEvent", method = RequestMethod.POST)
  public JsonResult<?> postAlarmEvent(@Valid @RequestBody AlarmEvent alarmEvent,
      @RequestHeader("Authorization") String header) {
    try {
      if (!safetyService.controlAuthorizeModel(header, alarmEvent.getModelId()))
        return ResultTool.fail(ResultCode.NO_PERMISSION);
    } catch (NullPointerException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return alarmEventService.postAlarmEvent(alarmEvent);
  }

  // delete
  @RequestMapping(value = "/alarmEvent", method = RequestMethod.DELETE)
  public JsonResult<?> deleteAlarmEvent(@RequestParam("id") int id,
      @RequestHeader("Authorization") String header) {
    try {
      if (!safetyService.controlAuthorizeAlarmEvent(header, id))
        return ResultTool.fail(ResultCode.NO_PERMISSION);
    } catch (NullPointerException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return alarmEventService.deleteAlarmEvent(id);
  }

  @RequestMapping(value = "/xiaozhi/otaManage", method = RequestMethod.GET)
  public JsonResult<?> xiaoZhiOtaList(@RequestHeader("Authorization") String header) {
    return otaXiaozhiService.otaList(header);
  }

  @RequestMapping(value = "/xiaozhi/otaManage", method = RequestMethod.POST)
  public JsonResult<?> xiaoZhiOtaBind(@Valid @RequestBody OtaXiaozhi otaXiaozhi,
      @RequestHeader("Authorization") String header) {
    try {
      if (!safetyService.controlAuthorizeProduct(header, otaXiaozhi.getProductId()))
        return ResultTool.fail(ResultCode.NO_PERMISSION);
    } catch (NullPointerException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return otaXiaozhiService.bindDevice(otaXiaozhi, header);
  }

  @RequestMapping(value = "/xiaozhi/otaManage", method = RequestMethod.DELETE)
  public JsonResult<?> xiaoZhiOtaUnbind(@RequestParam("id") int id,
      @RequestHeader("Authorization") String header) {
    try {
      if (!safetyService.controlAuthorizeOtaXiaoZhi(header, id))
        return ResultTool.fail(ResultCode.NO_PERMISSION);
    } catch (NullPointerException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return otaXiaozhiService.unbound(id);
  }

  @RequestMapping(value = "/vision/explain", method = RequestMethod.POST)
  public String aiVision(@RequestParam("question") @NotBlank(message = "question 不能为空")
  @Size(min = 1, max = 2048, message = "question 长度必须在 1 到 2048 之间") String question,
      @RequestPart("file") @NotNull(message = "file 不能为空") MultipartFile imageFile) {
    return aiService.getAiVisionIntent(question, imageFile);
  }

  @Operation(summary = "mcp接入点url获取", description = "获取mcp接入点url")
  @RequestMapping(value = "/mcpEndpoint", method = RequestMethod.GET)
  public JsonResult<?> getMcpPointUrl(@RequestParam("productId") int productId,
      @RequestHeader("Authorization") String header) {
    try {
      if (!safetyService.controlAuthorizeProduct(header, productId))
        return ResultTool.fail(ResultCode.NO_PERMISSION);
    } catch (NullPointerException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return aiService.getMcpPointUrl(productId);
  }

  @Operation(summary = "mcp接入点工具获取", description = "获取mcp接入点工具详情")
  @RequestMapping(value = "/mcpEndpoint/tools", method = RequestMethod.GET)
  public JsonResult<?> getMcpPointTools(@RequestParam("productId") int productId,
      @RequestHeader("Authorization") String header) {
    try {
      if (!safetyService.controlAuthorizeProduct(header, productId))
        return ResultTool.fail(ResultCode.NO_PERMISSION);
    } catch (NullPointerException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return aiService.getMcpPointTools(productId);
  }

  @Operation(summary = "ota小智升级", description = "获取ota小智升级包详情")
  @RequestMapping(value = "/xiaozhi/otaPassive", method = RequestMethod.GET)
  public JsonResult<?> getXiaoZhiOtaPassive(@RequestHeader("Authorization") String header) {
    return otaXiaozhiPassiveService.otaXiaozhiPassiveList(header);
  }

  @Operation(summary = "ota小智升级", description = "选择ota小智升级包")
  @RequestMapping(value = "/xiaozhi/otaPassive", method = RequestMethod.POST)
  public JsonResult<?> postXiaoZhiOtaPassive(
      @Valid @RequestBody OtaXiaozhiPassive otaXiaozhiPassive,
      @RequestHeader("Authorization") String header) {
    try {
      if (!safetyService.controlAuthorizeProduct(header, otaXiaozhiPassive.getProductId()))
        return ResultTool.fail(ResultCode.NO_PERMISSION);
    } catch (NullPointerException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return otaXiaozhiPassiveService.otaXiaozhiPassivePost(otaXiaozhiPassive);
  }

  @Operation(summary = "ota小智升级", description = "删除ota小智升级包")
  @RequestMapping(value = "/xiaozhi/otaPassive", method = RequestMethod.DELETE)
  public JsonResult<?> deleteXiaoZhiOtaPassive(@RequestParam("id") int id,
      @RequestHeader("Authorization") String header) {
    try {
      if (!safetyService.controlAuthorizeXiaoZhiOtaPassive(header, id))
        return ResultTool.fail(ResultCode.NO_PERMISSION);
    } catch (NullPointerException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return otaXiaozhiPassiveService.otaXiaozhiPassiveDelete(id);
  }

  // 禁止内部工具productban
  @Operation(summary = "禁止内部工具", description = "禁止内部工具")
  @RequestMapping(value = "/productToolsBan", method = RequestMethod.GET)
  public JsonResult<?> getProductToolsBan(@RequestParam("productId") int productId,
      @RequestHeader("Authorization") String header) {
    try {
      if (!safetyService.controlAuthorizeProduct(header, productId))
        return ResultTool.fail(ResultCode.NO_PERMISSION);
    } catch (NullPointerException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return productToolsBanService.getProductToolsBan(productId);
  }

  @Operation(summary = "禁止内部工具", description = "禁止内部工具")
  @RequestMapping(value = "/productToolsBan", method = RequestMethod.POST)
  public JsonResult<?> postProductToolsBan(
      @Valid @RequestBody ProductToolsBan productToolsBan,
      @RequestHeader("Authorization") String header) {
    try {
      if (!safetyService.controlAuthorizeProduct(header, productToolsBan.getProductId()))
        return ResultTool.fail(ResultCode.NO_PERMISSION);
    } catch (NullPointerException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return productToolsBanService.postProductToolsBan(productToolsBan);
  }

  @Operation(summary = "禁止内部工具", description = "禁止内部工具")
  @RequestMapping(value = "/productToolsBan", method = RequestMethod.DELETE)
  public JsonResult<?> deleteProductToolsBan(@RequestParam("productId") int productId,
      @RequestHeader("Authorization") String header) {
    try {
      if (!safetyService.controlAuthorizeProduct(header, productId))
        return ResultTool.fail(ResultCode.NO_PERMISSION);
    } catch (NullPointerException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return productToolsBanService.deleteProductToolsBan(productId);
  }

  // 长期记忆
  @Operation(summary = "获取长期记忆", description = "获取长期记忆")
  @RequestMapping(value = "/longMemory", method = RequestMethod.GET)
  public JsonResult<?> getLongMemory(@RequestHeader("Authorization") String header) {
    return agentLongMemoryService.getLongMemory(header);
  }

  @Operation(summary = "提交/修改长期记忆详情", description = "提交/修改长期记忆")
  @RequestMapping(value = "/longMemory", method = RequestMethod.POST)
  public JsonResult<?> postLongMemory(
      @Valid @RequestBody AgentLongMemory agentLongMemory,
      @RequestHeader("Authorization") String header) {
    try {
      if (!safetyService.controlAuthorizeProduct(header, agentLongMemory.getProductId()))
        return ResultTool.fail(ResultCode.NO_PERMISSION);
    } catch (NullPointerException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return agentLongMemoryService.postLongMemory(agentLongMemory);
  }

  @Operation(summary = "删除长期记忆", description = "删除长期记忆")
  @RequestMapping(value = "/longMemory", method = RequestMethod.DELETE)
  public JsonResult<?> deleteLongMemory(@RequestParam("id") int id,
      @RequestHeader("Authorization") String header) {
    try {
      if (!safetyService.controlAuthorizeAgentLongMemory(header, id))
        return ResultTool.fail(ResultCode.NO_PERMISSION);
    } catch (NullPointerException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return agentLongMemoryService.deleteLongMemory(id);
  }

  @Operation(summary = "获取产品语音定制", description = "获取产品语音定制")
  @RequestMapping(value = "/productVoiceDiy", method = RequestMethod.GET)
  public JsonResult<?> getProductVoiceDiy(@RequestParam("productId") int productId,
      @RequestHeader("Authorization") String header) {
    try {
      if (!safetyService.controlAuthorizeProduct(header, productId))
        return ResultTool.fail(ResultCode.NO_PERMISSION);
    } catch (NullPointerException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return productVoiceDiyService.getProductVoiceDiy(productId);
  }

  @Operation(summary = "提交/修改产品语音定制", description = "提交/修改产品语音定制")
  @RequestMapping(value = "/productVoiceDiy", method = RequestMethod.POST)
  public JsonResult<?> postProductVoiceDiy(
      @Valid @RequestBody ProductVoiceDiy productVoiceDiy,
      @RequestHeader("Authorization") String header) {
    try {
      if (!safetyService.controlAuthorizeProduct(header, productVoiceDiy.getProductId()))
        return ResultTool.fail(ResultCode.NO_PERMISSION);
    } catch (NullPointerException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return productVoiceDiyService.postProductVoiceDiy(productVoiceDiy);
  }

  @Operation(summary = "删除产品语音定制", description = "删除产品语音定制")
  @RequestMapping(value = "/productVoiceDiy", method = RequestMethod.DELETE)
  public JsonResult<?> deleteProductVoiceDiy(@RequestParam("id") int id,
      @RequestHeader("Authorization") String header) {
    try {
      if (!safetyService.controlAuthorizeProductVoiceDiy(header, id))
        return ResultTool.fail(ResultCode.NO_PERMISSION);
    } catch (NullPointerException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return productVoiceDiyService.deleteProductVoiceDiy(id);
  }

  @Operation(summary = "获取聊天记忆", description = "获取聊天记忆")
  @RequestMapping(value = "/memory", method = RequestMethod.GET)
  public JsonResult<?> getMemory(@RequestHeader("Authorization") String header) {
    return agentMemoryService.getMemory(header);
  }

  @Operation(summary = "修改聊天记忆", description = "修改聊天记忆")
  @RequestMapping(value = "/memory", method = RequestMethod.PUT)
  public JsonResult<?> updateMemory(
      @Valid @RequestBody AgentMemory agentMemory,
      @RequestHeader("Authorization") String header) {
    try {
      if (!safetyService.controlAuthorizeAgentMemory(header, agentMemory.getChatId()))
        return ResultTool.fail(ResultCode.NO_PERMISSION);
    } catch (NullPointerException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return agentMemoryService.updateMemory(agentMemory);
  }

  @Operation(summary = "删除聊天记忆", description = "删除聊天记忆")
  @RequestMapping(value = "/memory", method = RequestMethod.DELETE)
  public JsonResult<?> deleteMemory(@RequestParam("id") int id,
      @RequestHeader("Authorization") String header) {
    try {
      if (!safetyService.controlAuthorizeAgentMemory(header, id))
        return ResultTool.fail(ResultCode.NO_PERMISSION);
    } catch (NullPointerException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return agentMemoryService.deleteMemory(id);
  }
}
