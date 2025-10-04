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
package top.rslly.iot.utility.ai.tools;

import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import top.rslly.iot.services.agent.TimeScheduleServiceImpl;
import top.rslly.iot.utility.SpringBeanUtils;

import java.util.Date;

@Slf4j
public class MyJobListener implements JobListener {

  @Override
  public String getName() {
    return "myJobListener";
  }

  @Override
  public void jobToBeExecuted(JobExecutionContext jobExecutionContext) {

  }

  @Override
  public void jobExecutionVetoed(JobExecutionContext jobExecutionContext) {

  }

  @Override
  public void jobWasExecuted(JobExecutionContext jobExecutionContext, JobExecutionException e) {
    // JobDataMap jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();
    Date now = new Date();
    var time = jobExecutionContext.getNextFireTime();
    boolean isBefore = true;
    if (time != null)
      isBefore = time.before(now);
    TimeScheduleServiceImpl timeScheduleService =
        (TimeScheduleServiceImpl) SpringBeanUtils.getBean("timeScheduleServiceImpl");
    // log.info("isBefore {}",isBefore);
    if (isBefore) {
      timeScheduleService.deleteByAppidAndOpenidAndTaskName(
          jobExecutionContext.getJobDetail().getJobDataMap().get("data2").toString(),
          jobExecutionContext.getJobDetail().getJobDataMap().get("data1").toString(),
          jobExecutionContext.getJobDetail().getKey().getName());
      log.info("TimeSchedule db clean!!!");
    }
    log.info("jobWasExecuted");
  }
}
