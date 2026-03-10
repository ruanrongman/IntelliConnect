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
import top.rslly.iot.models.ProductRoleEntity;
import top.rslly.iot.models.TimeScheduleEntity;
import top.rslly.iot.models.WxProductActiveEntity;
import top.rslly.iot.models.WxUserEntity;
import top.rslly.iot.param.request.TimeScheduleParam;
import top.rslly.iot.param.request.TimeSchedulePutParam;
import top.rslly.iot.services.agent.TimeScheduleService;

import jakarta.annotation.Resource;
import top.rslly.iot.utility.JwtTokenUtil;
import top.rslly.iot.utility.QuartzManager;
import top.rslly.iot.utility.ai.tools.RemindJob;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import java.util.ArrayList;
import java.util.List;

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
    String token_deal = token.replace(JwtTokenUtil.TOKEN_PREFIX, "");
    String role = JwtTokenUtil.getUserRole(token_deal);
    String username = JwtTokenUtil.getUsername(token_deal);
    List<TimeScheduleEntity> result;
    if (role.equals("ROLE_" + "wx_user")) {
      if (wxUserRepository.findAllByName(username).isEmpty()) {
        return ResultTool.fail(ResultCode.COMMON_FAIL);
      }
      List<WxUserEntity> wxUserEntityList = wxUserRepository.findAllByName(username);
      String appid = wxUserEntityList.get(0).getAppid();
      String openid = wxUserEntityList.get(0).getOpenid();
      result = new ArrayList<>();
      var wxBindProductResponseList =
          wxProductBindRepository.findAllByAppidAndOpenid(appid, openid);
      if (wxBindProductResponseList.isEmpty()) {
        return ResultTool.fail(ResultCode.COMMON_FAIL);
      }
      for (var s : wxBindProductResponseList) {
        List<TimeScheduleEntity> timeScheduleEntityList =
            timeScheduleRepository.findAllByProductId(s.getProductId());
        result.addAll(timeScheduleEntityList);
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
        List<TimeScheduleEntity> timeScheduleEntityList =
            timeScheduleRepository.findAllByProductId(s.getProductId());
        result.addAll(timeScheduleEntityList);
      }
    } else {
      result = timeScheduleRepository.findAll();
    }
    if (result.isEmpty()) {
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    } else
      return ResultTool.success(result);
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

    if (!isValidCronExpression(timeScheduleParam.getCron())) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }

    List<TimeScheduleEntity> saveList = new ArrayList<>();

    for (WxProductActiveEntity wxProductActiveEntity : wxProductActiveEntityList) {

      String appid = wxProductActiveEntity.getAppid();
      String openid = wxProductActiveEntity.getOpenid();

      // 判断任务是否已存在
      if (!timeScheduleRepository
          .findAllByAppidAndOpenidAndTaskName(appid, openid, timeScheduleParam.getTaskName())
          .isEmpty()) {
        continue; // 已存在则跳过当前用户
      }

      TimeScheduleEntity timeScheduleEntity = new TimeScheduleEntity();
      timeScheduleEntity.setAppid(appid);
      timeScheduleEntity.setOpenid(openid);
      timeScheduleEntity.setTaskName(timeScheduleParam.getTaskName());
      timeScheduleEntity.setCron(timeScheduleParam.getCron());
      timeScheduleEntity.setExec(timeScheduleParam.getExec().toString());
      timeScheduleEntity.setProductId(timeScheduleParam.getProductId());

      if (Boolean.FALSE.equals(timeScheduleParam.getExec())) {
        timeScheduleEntity.setExecCommand("");
      } else {
        timeScheduleEntity.setExecCommand(timeScheduleParam.getExecCommand());
      }

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
    if (timeScheduleRepository.findAllById(timeSchedulePutParam.getId()).isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    if (!isValidCronExpression(timeSchedulePutParam.getCron())) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    TimeScheduleEntity timeScheduleEntity =
        timeScheduleRepository.findAllById(timeSchedulePutParam.getId()).get(0);
    timeScheduleEntity.setCron(timeSchedulePutParam.getCron());
    if (timeSchedulePutParam.getExec() == false) {
      timeScheduleEntity.setExecCommand("");
    } else {
      timeScheduleEntity.setExecCommand(timeSchedulePutParam.getExecCommand());
    }
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
}
