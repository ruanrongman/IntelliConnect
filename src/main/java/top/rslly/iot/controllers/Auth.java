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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import top.rslly.iot.param.request.*;
import top.rslly.iot.services.*;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

@RestController
@RequestMapping(value = "/api/v2")
public class Auth {

  @Autowired
  private UserServiceImpl userService;
  @Autowired
  private ProductServiceImpl productService;
  @Autowired
  private ProductModelServiceImpl productModelService;
  @Autowired
  private ProductDeviceServiceImpl productDeviceService;
  @Autowired
  private ProductDataServiceImpl productDataService;
  @Autowired
  private EventDataServiceImpl eventDataService;
  @Autowired
  private ProductEventServiceImpl productEventService;
  @Autowired
  private ProductFunctionServiceImpl productFunctionService;
  @Autowired
  private UserProductBindServiceImpl userProductBindService;
  @Autowired
  private MqttUserServiceImpl mqttUserService;
  @Autowired
  private SafetyServiceImpl safetyService;
  @Autowired
  private ProductRoleServiceImpl productRoleService;


  @PreAuthorize("hasRole('ROLE_admin')")
  @Operation(summary = "创建新用户", description = "目前仅管理员用户支持")
  @RequestMapping(value = "/newUser", method = RequestMethod.POST)
  public JsonResult<?> createUser(@RequestBody User user) {
    return userService.newUser(user);
  }

  @Operation(summary = "获取用户产品绑定列表", description = "获取用户产品绑定列表")
  @RequestMapping(value = "/userProductBindList", method = RequestMethod.GET)
  public JsonResult<?> userProductBindList(@RequestHeader("Authorization") String token) {
    return userProductBindService.userProductBindList(token);
  }

  @Operation(summary = "用户产品绑定", description = "用户产品绑定")
  @RequestMapping(value = "/userProductBind", method = RequestMethod.POST)
  public JsonResult<?> userProductBind(@RequestBody UserBindProduct userBindProduct,
      @RequestHeader("Authorization") String token) {
    return userProductBindService.userProductBind(userBindProduct, token);
  }

  @Operation(summary = "用户产品解绑", description = "用户产品解绑")
  @RequestMapping(value = "/userProductUnbind", method = RequestMethod.POST)
  public JsonResult<?> userProductUnbind(@RequestBody UserBindProduct userBindProduct,
      @RequestHeader("Authorization") String token) {
    return userProductBindService.userProductUnbind(userBindProduct, token);
  }

  // 获取所有mqtt用户信息
  @PreAuthorize("hasRole('ROLE_admin')")
  @Operation(summary = "获取MQTT用户信息", description = "获取所有MQTT用户信息(管理员专属)")
  @RequestMapping(value = "/mqttUser", method = RequestMethod.GET)
  public JsonResult<?> MqttUser() {
    return mqttUserService.getMqttUser();
  }

  @Operation(summary = "获取产品信息", description = "比如产品密钥和注册状态")
  @RequestMapping(value = "/Product", method = RequestMethod.GET)
  public JsonResult<?> Product(@RequestHeader("Authorization") String header) {
    return productService.getProduct(header);
  }

  // 添加产品信息，如产品密钥这些，后续版本将会添加更多细节。
  @Operation(summary = "提交产品信息", description = "比如产品密钥和注册状态")
  @RequestMapping(value = "/Product", method = RequestMethod.POST)
  public JsonResult<?> Product(@RequestBody Product product,
      @RequestHeader("Authorization") String header) {
    return productService.postProduct(product, header);
  }

  @Operation(summary = "删除产品信息（慎用）", description = "比如产品密钥和注册状态")
  @RequestMapping(value = "/Product", method = RequestMethod.DELETE)
  public JsonResult<?> Product(@RequestParam("id") int id,
      @RequestHeader("Authorization") String header) {
    if (!safetyService.controlAuthorizeProduct(header, id))
      return ResultTool.fail(ResultCode.NO_PERMISSION);
    return productService.deleteProduct(id);
  }

  @Operation(summary = "获取物模型", description = "获取所有产品的物模型")
  @RequestMapping(value = "/ProductModel", method = RequestMethod.GET)
  public JsonResult<?> ProductModel(@RequestHeader("Authorization") String header) {
    return productModelService.getProductModel(header);
  }

  @Operation(summary = "获取物模型", description = "获取对应产品的物模型")
  @RequestMapping(value = "/ProductModelByproductId", method = RequestMethod.GET)
  public JsonResult<?> ProductModelByproductId(@RequestParam("productId") int productId) {
    return productModelService.getProductModel(productId);
  }

  @Operation(summary = "创建物模型", description = "创建产品的物模型")
  @RequestMapping(value = "/ProductModel", method = RequestMethod.POST)
  public JsonResult<?> ProductModel(@RequestBody ProductModel productModel,
      @RequestHeader("Authorization") String header) {
    if (!safetyService.controlAuthorizeProduct(header, productModel.getProductId()))
      return ResultTool.fail(ResultCode.NO_PERMISSION);
    return productModelService.postProductModel(productModel);
  }

  @Operation(summary = "删除物模型", description = "删除产品的物模型")
  @RequestMapping(value = "/ProductModel", method = RequestMethod.DELETE)
  public JsonResult<?> ProductModel(@RequestParam("id") int id,
      @RequestHeader("Authorization") String header) {
    try {
      if (!safetyService.controlAuthorizeModel(header, id))
        return ResultTool.fail(ResultCode.NO_PERMISSION);
    } catch (NullPointerException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return productModelService.deleteProductModel(id);
  }


  @Operation(summary = "获取事件", description = "获取所有物模型的事件")
  @RequestMapping(value = "/ProductEvent", method = RequestMethod.GET)
  public JsonResult<?> ProductEvent(@RequestHeader("Authorization") String header) {
    return productEventService.getProductEvent(header);
  }

  @Operation(summary = "提交事件", description = "提交物模型的事件")
  @RequestMapping(value = "/ProductEvent", method = RequestMethod.POST)
  public JsonResult<?> ProductEvent(@RequestBody ProductEvent productEvent,
      @RequestHeader("Authorization") String header) {
    try {
      if (!safetyService.controlAuthorizeModel(header, productEvent.getModelId()))
        return ResultTool.fail(ResultCode.NO_PERMISSION);
    } catch (NullPointerException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return productEventService.postProductEvent(productEvent);
  }

  @Operation(summary = "删除事件", description = "删除物模型的事件")
  @RequestMapping(value = "/ProductEvent", method = RequestMethod.DELETE)
  public JsonResult<?> ProductEvent(@RequestParam("id") int id,
      @RequestHeader("Authorization") String header) {
    try {
      if (!safetyService.controlAuthorizeEvent(header, id))
        return ResultTool.fail(ResultCode.NO_PERMISSION);
    } catch (NullPointerException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return productEventService.deleteProductEvent(id);
  }


  @Operation(summary = "获取设备", description = "获取所有物模型的设备")
  @RequestMapping(value = "/ProductDevice", method = RequestMethod.GET)
  public JsonResult<?> ProductDevice(@RequestHeader("Authorization") String header) {
    return productDeviceService.getProductDevice(header);
  }

  @Operation(summary = "创建设备", description = "创建物模型的设备")
  @RequestMapping(value = "/ProductDevice", method = RequestMethod.POST)
  public JsonResult<?> ProductDevice(@RequestBody ProductDevice productDevice,
      @RequestHeader("Authorization") String header) {
    try {
      if (!safetyService.controlAuthorizeModel(header, productDevice.getModelId()))
        return ResultTool.fail(ResultCode.NO_PERMISSION);
    } catch (NullPointerException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return productDeviceService.postProductDevice(productDevice);
  }

  @Operation(summary = "删除设备", description = "删除物模型的设备")
  @RequestMapping(value = "/ProductDevice", method = RequestMethod.DELETE)
  public JsonResult<?> ProductDevice(@RequestParam("id") int id,
      @RequestHeader("Authorization") String header) {
    try {
      if (!safetyService.controlAuthorizeDevice(header, id))
        return ResultTool.fail(ResultCode.NO_PERMISSION);
    } catch (NullPointerException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return productDeviceService.deleteProductDevice(id);
  }

  @Operation(summary = "获取属性", description = "获取所有物模型的属性")
  @RequestMapping(value = "/ProductData", method = RequestMethod.GET)
  public JsonResult<?> ProductData(@RequestHeader("Authorization") String header) {
    return productDataService.getProductData(header);
  }

  @Operation(summary = "创建属性", description = "创建物模型的属性")
  @RequestMapping(value = "/ProductData", method = RequestMethod.POST)
  public JsonResult<?> ProductData(@RequestBody ProductData productData,
      @RequestHeader("Authorization") String header) {
    try {
      if (!safetyService.controlAuthorizeModel(header, productData.getModelId()))
        return ResultTool.fail(ResultCode.NO_PERMISSION);
    } catch (NullPointerException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return productDataService.postProductData(productData);
  }

  @Operation(summary = "删除属性", description = "删除物模型的属性")
  @RequestMapping(value = "/ProductData", method = RequestMethod.DELETE)
  public JsonResult<?> ProductData(@RequestParam("id") int id,
      @RequestHeader("Authorization") String header) {
    try {
      if (!safetyService.controlAuthorizeProductData(header, id))
        return ResultTool.fail(ResultCode.NO_PERMISSION);
    } catch (NullPointerException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return productDataService.deleteProductData(id);
  }


  @Operation(summary = "获取功能", description = "获取所有物模型的功能")
  @RequestMapping(value = "/ProductFunction", method = RequestMethod.GET)
  public JsonResult<?> ProductFunction(@RequestHeader("Authorization") String header) {
    return productFunctionService.getProductFunction(header);
  }

  @Operation(summary = "提交功能", description = "创建物模型的功能")
  @RequestMapping(value = "/ProductFunction", method = RequestMethod.POST)
  public JsonResult<?> ProductFunction(@RequestBody ProductFunction productFunction,
      @RequestHeader("Authorization") String header) {
    try {
      if (!safetyService.controlAuthorizeModel(header, productFunction.getModelId()))
        return ResultTool.fail(ResultCode.NO_PERMISSION);
    } catch (NullPointerException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return productFunctionService.postProductFunction(productFunction);
  }

  @Operation(summary = "删除功能", description = "删除物模型的功能")
  @RequestMapping(value = "/ProductFunction", method = RequestMethod.DELETE)
  public JsonResult<?> ProductFunction(@RequestParam("id") int id,
      @RequestHeader("Authorization") String header) {
    try {
      if (!safetyService.controlAuthorizeFunction(header, id))
        return ResultTool.fail(ResultCode.NO_PERMISSION);
    } catch (NullPointerException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return productFunctionService.deleteProductFunction(id);
  }

  @Operation(summary = "获取事件入参", description = "获取所有事件入参")
  @RequestMapping(value = "/EventData", method = RequestMethod.GET)
  public JsonResult<?> EventData(@RequestHeader("Authorization") String header) {
    return eventDataService.getEventData(header);
  }

  @Operation(summary = "提交事件入参", description = "提交事件入参")
  @RequestMapping(value = "/EventData", method = RequestMethod.POST)
  public JsonResult<?> EventData(@RequestBody EventData eventData,
      @RequestHeader("Authorization") String header) {
    try {
      if (!safetyService.controlAuthorizeModel(header, eventData.getModelId()))
        return ResultTool.fail(ResultCode.NO_PERMISSION);
    } catch (NullPointerException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return eventDataService.postEventData(eventData);
  }

  @Operation(summary = "删除事件入参", description = "删除事件入参")
  @RequestMapping(value = "/EventData", method = RequestMethod.DELETE)
  public JsonResult<?> EventData(@RequestParam("id") int id,
      @RequestHeader("Authorization") String header) {
    try {
      if (!safetyService.controlAuthorizeEventData(header, id))
        return ResultTool.fail(ResultCode.NO_PERMISSION);
    } catch (NullPointerException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return eventDataService.deleteEventData(id);
  }

  @Operation(summary = "获取产品角色列表", description = "仅获取当前用户产品角色列表")
  @RequestMapping(value = "/productRole", method = RequestMethod.GET)
  public JsonResult<?> getProductRole(@RequestHeader("Authorization") String header) {
    return productRoleService.getProductRole(header);
  }

  @Operation(summary = "添加产品角色", description = "仅添加当前用户产品角色")
  @RequestMapping(value = "/productRole", method = RequestMethod.POST)
  public JsonResult<?> postProductRole(@RequestBody ProductRole productRole,
      @RequestHeader("Authorization") String header) {
    try {
      if (!safetyService.controlAuthorizeProduct(header, productRole.getProductId()))
        return ResultTool.fail(ResultCode.NO_PERMISSION);
    } catch (NullPointerException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return productRoleService.postProductRole(productRole);
  }

  @Operation(summary = "修改产品角色", description = "仅修改当前用户产品角色")
  @RequestMapping(value = "/productRole", method = RequestMethod.PUT)
  public JsonResult<?> putProductRole(@RequestBody ProductRole productRole,
      @RequestHeader("Authorization") String header) {
    if (!safetyService.controlAuthorizeProduct(header, productRole.getProductId()))
      return ResultTool.fail(ResultCode.NO_PERMISSION);
    return productRoleService.putProductRole(productRole);
  }

  @Operation(summary = "删除产品角色", description = "仅删除当前用户产品角色")
  @RequestMapping(value = "/productRole", method = RequestMethod.DELETE)
  public JsonResult<?> deleteProductRole(@RequestParam("id") int id,
      @RequestHeader("Authorization") String header) {
    try {
      if (!safetyService.controlAuthorizeProductRole(header, id))
        return ResultTool.fail(ResultCode.NO_PERMISSION);
    } catch (NullPointerException e) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return productRoleService.deleteProductRole(id);
  }

}
