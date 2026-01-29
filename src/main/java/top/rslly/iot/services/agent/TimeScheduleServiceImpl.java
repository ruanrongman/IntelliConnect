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

import org.springframework.stereotype.Service;
import top.rslly.iot.dao.TimeScheduleRepository;
import top.rslly.iot.models.TimeScheduleEntity;
import top.rslly.iot.services.agent.TimeScheduleService;

import jakarta.annotation.Resource;
import java.util.List;

@Service
public class TimeScheduleServiceImpl implements TimeScheduleService {
  @Resource
  private TimeScheduleRepository timeScheduleRepository;

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
  public void insert(TimeScheduleEntity timeScheduleEntity) {
    timeScheduleRepository.save(timeScheduleEntity);
  }

  @Override
  public void deleteByAppidAndOpenidAndTaskName(String appid, String openid, String taskName) {
    timeScheduleRepository.deleteByAppidAndOpenidAndTaskName(appid, openid, taskName);
  }
}
