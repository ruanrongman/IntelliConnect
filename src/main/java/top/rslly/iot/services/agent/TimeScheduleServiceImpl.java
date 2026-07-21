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

import org.quartz.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.rslly.iot.dao.*;
import top.rslly.iot.models.TimeScheduleEntity;
import top.rslly.iot.models.WxProductActiveEntity;
import top.rslly.iot.models.WxUserEntity;
import top.rslly.iot.param.request.TimeScheduleParam;
import top.rslly.iot.param.request.TimeSchedulePutParam;
import top.rslly.iot.param.response.TimeScheduleResponse;
import top.rslly.iot.services.agent.TimeScheduleService;

import jakarta.annotation.Resource;
import top.rslly.iot.utility.JwtTokenUtil;
import top.rslly.iot.utility.QuartzManager;
import top.rslly.iot.utility.ai.tools.RemindJob;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TimeScheduleServiceImpl implements TimeScheduleService {
  @Resource
  private TimeScheduleRepository timeScheduleRepository;
  @Resource
  private ProductRepository productRepository;
  @Resource
  private WxProductBindRepository wxProductBindRepository;
  @Resource
  private WxProductActiveRepository wxProductActiveRepository;
  @Resource
  private UserProductBindRepository userProductBindRepository;
  @Resource
  private WxUserRepository wxUserRepository;
  @Resource
  private UserRepository userRepository;

  @Override
  public List<TimeScheduleEntity> findAllByAppidAndOpenid(String appid, String openid) {
    return timeScheduleRepository.findAllByAppidAndOpenid(appid, openid);
  }

  @Override
  public List<TimeScheduleEntity> findAllByAppidAndOpenidAndTaskName(String appid, String openid,
      String taskName) {
    return timeScheduleRepository.findAllByAppidAndOpenidAndTaskName(appid, openid, taskName);
  }

  @Override
  public List<TimeScheduleEntity> findAll() {
    return timeScheduleRepository.findAll();
  }

  @Override
  public List<TimeScheduleEntity> findAllById(int id) {
    return timeScheduleRepository.findAllById(id);
  }

  @Override
  public JsonResult<?> getTimeSchedule(String token) {
    String tokenDeal = token.replace(JwtTokenUtil.TOKEN_PREFIX, "");
    String role = JwtTokenUtil.getUserRole(tokenDeal);
    String username = JwtTokenUtil.getUsername(tokenDeal);
    List<TimeScheduleEntity> result;
    if (role.equals("ROLE_" + "wx_user")) {
      List<WxUserEntity> wxUserEntityList = wxUserRepository.findAllByName(username);
      if (wxUserEntityList.isEmpty()) {
        return ResultTool.fail(ResultCode.COMMON_FAIL);
      }
      String appid = wxUserEntityList.getFirst().getAppid();
      String openid = wxUserEntityList.getFirst().getOpenid();
      var wxBindProductResponseList =
          wxProductBindRepository.findAllByAppidAndOpenid(appid, openid);
      if (wxBindProductResponseList.isEmpty()) {
        return ResultTool.fail(ResultCode.COMMON_FAIL);
      }
      List<Integer> productIds = wxBindProductResponseList.stream()
          .map(s -> s.getProductId())
          .distinct()
          .toList();
      result = timeScheduleRepository.findAllByProductIdIn(productIds);
    } else if (!role.equals("[ROLE_admin]")) {
      var userList = userRepository.findAllByUsername(username);
      if (userList.isEmpty()) {
        return ResultTool.fail(ResultCode.COMMON_FAIL);
      }
      int userId = userList.getFirst().getId();
      var userProductBindEntityList = userProductBindRepository.findAllByUserId(userId);
      if (userProductBindEntityList.isEmpty()) {
        return ResultTool.fail(ResultCode.COMMON_FAIL);
      }
      List<Integer> productIds = userProductBindEntityList.stream()
          .map(s -> s.getProductId())
          .distinct()
          .toList();
      result = timeScheduleRepository.findAllByProductIdIn(productIds);
    } else {
      result = timeScheduleRepository.findAll();
    }
    if (result.isEmpty()) {
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    }

    List<Integer> productIds = result.stream()
        .map(TimeScheduleEntity::getProductId)
        .distinct()
        .toList();
    Map<Integer, String> productNames = productRepository.findAllByIdIn(productIds).stream()
        .filter(product -> product.getProductName() != null)
        .collect(Collectors.toMap(product -> product.getId(), product -> product.getProductName(),
            (first, second) -> first));
    Date evaluationTime = new Date();
    List<TimeScheduleResponse> resultResponse = result.stream()
        .map(schedule -> toResponse(schedule, productNames, evaluationTime))
        .toList();
    return ResultTool.success(resultResponse);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> postTimeSchedule(TimeScheduleParam timeScheduleParam) {
    if (productRepository.findAllById(timeScheduleParam.getProductId()).isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }

    List<WxProductActiveEntity> wxProductActiveEntityList =
        wxProductActiveRepository.findAllByProductId(timeScheduleParam.getProductId());

    if (wxProductActiveEntityList.isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }

    String taskName = timeScheduleParam.getTaskName().trim();
    String cron = timeScheduleParam.getCron().trim();
    String execCommand = normalizeExecCommand(timeScheduleParam.getExec(),
        timeScheduleParam.getExecCommand());
    if (!isValidCronExpression(cron)
        || Boolean.TRUE.equals(timeScheduleParam.getExec()) && execCommand == null) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }

    List<TimeScheduleEntity> saveList = new ArrayList<>();

    for (WxProductActiveEntity wxProductActiveEntity : wxProductActiveEntityList) {

      String appid = wxProductActiveEntity.getAppid();
      String openid = wxProductActiveEntity.getOpenid();

      // 判断任务是否已存在
      if (!timeScheduleRepository
          .findAllByAppidAndOpenidAndTaskName(appid, openid, taskName)
          .isEmpty()) {
        continue; // 已存在则跳过当前用户
      }

      TimeScheduleEntity timeScheduleEntity = new TimeScheduleEntity();
      timeScheduleEntity.setAppid(appid);
      timeScheduleEntity.setOpenid(openid);
      timeScheduleEntity.setTaskName(taskName);
      timeScheduleEntity.setCron(cron);
      timeScheduleEntity.setExec(timeScheduleParam.getExec().toString());
      timeScheduleEntity.setProductId(timeScheduleParam.getProductId());

      timeScheduleEntity.setExecCommand(execCommand);

      String groupName = appid + ":" + openid;
      String chatId = appid + openid;

      QuartzManager.addJob(
          timeScheduleEntity.getTaskName(),
          groupName,
          timeScheduleEntity.getTaskName(),
          groupName,
          RemindJob.class,
          timeScheduleEntity.getCron(),
          openid,
          appid,
          timeScheduleEntity.getExec(),
          chatId,
          timeScheduleEntity.getProductId(),
          timeScheduleEntity.getExecCommand());

      saveList.add(timeScheduleEntity);
    }

    // 批量保存
    if (saveList.isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }

    var res = timeScheduleRepository.saveAll(saveList);

    return ResultTool.success(res);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> putTimeSchedule(TimeSchedulePutParam timeSchedulePutParam) {
    List<TimeScheduleEntity> timeScheduleEntityList =
        timeScheduleRepository.findAllById(timeSchedulePutParam.getId());
    if (timeScheduleEntityList.isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    String cron = timeSchedulePutParam.getCron().trim();
    String execCommand = normalizeExecCommand(timeSchedulePutParam.getExec(),
        timeSchedulePutParam.getExecCommand());
    if (!isValidCronExpression(cron)
        || Boolean.TRUE.equals(timeSchedulePutParam.getExec()) && execCommand == null) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    TimeScheduleEntity timeScheduleEntity = timeScheduleEntityList.getFirst();
    timeScheduleEntity.setCron(cron);
    timeScheduleEntity.setExecCommand(execCommand);
    timeScheduleEntity.setExec(timeSchedulePutParam.getExec().toString());
    String groupName = timeScheduleEntity.getAppid() + ":" + timeScheduleEntity.getOpenid();
    String taskName = timeScheduleEntity.getTaskName();
    String chatId = timeScheduleEntity.getAppid() + timeScheduleEntity.getOpenid();
    QuartzManager.removeJob(taskName, groupName, taskName, groupName);
    QuartzManager.addJob(timeScheduleEntity.getTaskName(), groupName,
        timeScheduleEntity.getTaskName(), groupName,
        RemindJob.class, timeScheduleEntity.getCron(), timeScheduleEntity.getOpenid(),
        timeScheduleEntity.getAppid(), timeScheduleEntity.getExec(),
        chatId, timeScheduleEntity.getProductId(), timeScheduleEntity.getExecCommand());
    var res = timeScheduleRepository.save(timeScheduleEntity);
    return ResultTool.success(res);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> deleteTimeSchedule(int id) {
    if (timeScheduleRepository.findAllById(id).isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    var res = timeScheduleRepository.deleteAllById(id);
    String groupName = res.get(0).getAppid() + ":" + res.get(0).getOpenid();
    String taskName = res.get(0).getTaskName();
    QuartzManager.removeJob(taskName, groupName, taskName, groupName);
    return ResultTool.success(res);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void insert(TimeScheduleEntity timeScheduleEntity) {
    timeScheduleRepository.save(timeScheduleEntity);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void deleteByAppidAndOpenidAndTaskName(String appid, String openid, String taskName) {
    timeScheduleRepository.deleteByAppidAndOpenidAndTaskName(appid, openid, taskName);
  }

  private boolean isValidCronExpression(String cron) {
    try {
      return CronExpression.isValidExpression(cron);
    } catch (Exception e) {
      return false;
    }
  }

  private String normalizeExecCommand(Boolean exec, String execCommand) {
    if (Boolean.FALSE.equals(exec)) {
      return "";
    }
    if (execCommand == null || execCommand.isBlank()) {
      return null;
    }
    return execCommand.trim();
  }

  private TimeScheduleResponse toResponse(TimeScheduleEntity schedule,
      Map<Integer, String> productNames, Date evaluationTime) {
    TimeScheduleResponse response = new TimeScheduleResponse();
    response.setId(schedule.getId());
    response.setAppid(schedule.getAppid());
    response.setOpenid(schedule.getOpenid());
    response.setTaskName(schedule.getTaskName());
    response.setCron(schedule.getCron());
    response.setExec(Boolean.parseBoolean(schedule.getExec()));
    response.setExecCommand(schedule.getExecCommand());
    response.setProductId(schedule.getProductId());
    response.setProductName(productNames.get(schedule.getProductId()));

    if (schedule.getCron() == null || schedule.getCron().isBlank()) {
      response.setCronValid(false);
      response.setNextEvalTime(null);
      return response;
    }

    try {
      CronExpression expression = new CronExpression(schedule.getCron());
      Date nextFireTime = expression.getNextValidTimeAfter(evaluationTime);
      response.setCronValid(true);
      response.setNextEvalTime(nextFireTime == null ? null : nextFireTime.toInstant().toString());
    } catch (ParseException e) {
      response.setCronValid(false);
      response.setNextEvalTime(null);
    }
    return response;
  }
}
