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

import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import top.rslly.iot.param.request.AiControl;
import top.rslly.iot.param.request.ControlParam;
import top.rslly.iot.param.request.MetaData;
import top.rslly.iot.param.request.ReadData;
import top.rslly.iot.services.DataServiceImpl;
import top.rslly.iot.services.HardWareServiceImpl;
import top.rslly.iot.utility.RedisUtil;
import top.rslly.iot.utility.RuntimeMessage;
import top.rslly.iot.utility.ai.chain.Router;
import top.rslly.iot.utility.ai.tools.ControlTool;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultTool;

@RestController
@RequestMapping(value = "/api/v2")
public class Tool {
  @Autowired
  private RedisUtil redisUtil;
  @Autowired
  private DataServiceImpl dataService;
  @Autowired
  private HardWareServiceImpl hardWareService;
  @Autowired
  private Router router;

  @Operation(summary = "用于获取平台运行环境信息", description = "单位为百分比")
  @RequestMapping(value = "/machineMessage", method = RequestMethod.GET)
  public JsonResult<?> machineMessage() {
    return ResultTool.success(RuntimeMessage.getMessage());
  }

  @RequestMapping(value = "/control", method = RequestMethod.POST)
  public JsonResult<?> control(@RequestBody ControlParam controlParam) throws MqttException {

    return hardWareService.control(controlParam);
  }

  @Operation(summary = "用于获取物联网一段时间的设备数据", description = "使用两个毫秒时间戳")
  @RequestMapping(value = "/readData", method = RequestMethod.POST)
  public JsonResult<?> readData(@RequestBody ReadData readData) {
    return dataService.findAllByTimeBetweenAndDeviceName(readData.getTime1(), readData.getTime2(),
        readData.getName());
  }

  @Operation(summary = "获取属性实时数据", description = "高性能接口(带redis缓存)")
  @RequestMapping(value = "/metaData", method = RequestMethod.POST)
  public JsonResult<?> metaData(@RequestBody MetaData metaData) {
    return dataService.metaData(metaData.getDeviceId(), metaData.getJsonKey());
  }

  @Operation(summary = "使用大模型控制设备", description = "响应速度取决于大模型速度")
  @RequestMapping(value = "/aiControl", method = RequestMethod.POST)
  public JsonResult<?> aiControl(@RequestBody AiControl aiControl) {
    var answer =
        router.response(aiControl.getContent(), aiControl.getChatId(), aiControl.getProductId());
    return ResultTool.success(answer);
  }
}
