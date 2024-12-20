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
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import top.rslly.iot.utility.SpringBeanUtils;
import top.rslly.iot.utility.wx.DealWx;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public class RemindJob implements Job {

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    JobDataMap jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();
    String jobName = jobExecutionContext.getJobDetail().getKey().getName();
    log.info("task name: {}", jobName);

    DealWx dealWx = (DealWx) SpringBeanUtils.getBean("dealWx");
    String openid = jobDataMap.get("data1").toString();
    String microappid = jobDataMap.get("data2").toString();
    log.info("openid:{}", openid);
    log.info("microappid:{}", microappid);
    try {
      dealWx.sendContent(openid, jobName + "提醒时间到了！！！", microappid);
    } catch (Exception e) {
      log.error("发送失败", e);
    }
    // 创建一个事件，下面仅创建一个输出语句作演示
    log.info(Thread.currentThread().getName() + "--"
        + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()));
  }
}
