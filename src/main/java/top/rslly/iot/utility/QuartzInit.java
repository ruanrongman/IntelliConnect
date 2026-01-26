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
package top.rslly.iot.utility;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import top.rslly.iot.models.TimeScheduleEntity;
import top.rslly.iot.services.agent.TimeScheduleServiceImpl;
import top.rslly.iot.utility.ai.tools.RemindJob;

import java.util.List;

@Component
@Slf4j
public class QuartzInit implements CommandLineRunner {
  @Autowired
  private TimeScheduleServiceImpl timeScheduleService;

  @Override
  public void run(String... args) throws Exception {
    log.info("开始进行定时任务初始化...");
    List<TimeScheduleEntity> timeScheduleEntities = timeScheduleService.findAll();
    QuartzManager.init();
    for (var s : timeScheduleEntities) {
      // CronChecker.isCronExpired(cronExpression)
      if (!QuartzCronDateUtils.isCronExpired(s.getCron())) {
        String groupName = s.getAppid() + ":" + s.getOpenid();
        QuartzManager.addJob(s.getTaskName(), groupName, s.getTaskName(), groupName,
            RemindJob.class, s.getCron(), s.getOpenid(), s.getAppid());
      } else
        timeScheduleService.deleteByAppidAndOpenidAndTaskName(s.getAppid(), s.getOpenid(),
            s.getTaskName());
    }

  }
}
