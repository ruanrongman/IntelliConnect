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

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import top.rslly.iot.utility.ai.tools.MyJobListener;

public class QuartzManager {
  /**
   * 功能： 添加一个定时任务
   * 
   * @param jobName 任务名
   * @param jobGroupName 任务组名
   * @param triggerName 触发器名
   * @param triggerGroupName 触发器组名
   * @param jobClass 任务的类类型 eg:TimedMassJob.class
   * @param cron 时间设置 表达式，参考quartz说明文档
   * @param objects 可变参数需要进行传参的值
   */
  private static SchedulerFactory schedulerFactory = null;

  public static void init() {
    schedulerFactory = new StdSchedulerFactory();
  }

  public static void addJob(String jobName, String jobGroupName, String triggerName,
      String triggerGroupName, Class jobClass, String cron, Object... objects) {
    try {
      Scheduler scheduler = schedulerFactory.getScheduler();
      scheduler.getListenerManager().addJobListener(new MyJobListener());
      // 任务名，任务组，任务执行类
      JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(jobName, jobGroupName).build();
      // 触发器
      if (objects != null) {
        for (int i = 0; i < objects.length; i++) {
          // 该数据可以通过Job中的JobDataMap dataMap = context.getJobDetail().getJobDataMap();来进行参数传递值
          jobDetail.getJobDataMap().put("data" + (i + 1), objects[i]);
        }
      }
      TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger();
      // 触发器名,触发器组
      triggerBuilder.withIdentity(triggerName, triggerGroupName);
      triggerBuilder.startNow();
      // 触发器时间设定
      triggerBuilder.withSchedule(CronScheduleBuilder.cronSchedule(cron));
      // 创建Trigger对象
      CronTrigger trigger = (CronTrigger) triggerBuilder.build();
      // 调度容器设置JobDetail和Trigger
      scheduler.scheduleJob(jobDetail, trigger);
      // 启动
      if (!scheduler.isShutdown()) {
        scheduler.start();
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * 功能：修改一个任务的触发时间
   * 
   * @param jobName
   * @param jobGroupName
   * @param triggerName 触发器名
   * @param triggerGroupName 触发器组名
   * @param cron 时间设置，参考quartz说明文档
   */
  public static void modifyJobTime(String jobName, String jobGroupName, String triggerName,
      String triggerGroupName, String cron) {
    try {
      Scheduler scheduler = schedulerFactory.getScheduler();
      TriggerKey triggerKey = TriggerKey.triggerKey(triggerName, triggerGroupName);
      CronTrigger trigger = (CronTrigger) scheduler.getTrigger(triggerKey);
      if (trigger == null) {
        return;
      }
      String oldTime = trigger.getCronExpression();
      if (!oldTime.equalsIgnoreCase(cron)) {
        // 触发器
        TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger();
        // 触发器名,触发器组
        triggerBuilder.withIdentity(triggerName, triggerGroupName);
        triggerBuilder.startNow();
        // 触发器时间设定
        triggerBuilder.withSchedule(CronScheduleBuilder.cronSchedule(cron));
        // 创建Trigger对象
        trigger = (CronTrigger) triggerBuilder.build();
        // 方式一 ：修改一个任务的触发时间
        scheduler.rescheduleJob(triggerKey, trigger);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * 功能: 移除一个任务
   * 
   * @param jobName
   * @param jobGroupName
   * @param triggerName
   * @param triggerGroupName
   */
  public static void removeJob(String jobName, String jobGroupName, String triggerName,
      String triggerGroupName) {
    try {
      Scheduler scheduler = schedulerFactory.getScheduler();

      TriggerKey triggerKey = TriggerKey.triggerKey(triggerName, triggerGroupName);
      // 停止触发器
      scheduler.pauseTrigger(triggerKey);
      // 移除触发器
      scheduler.unscheduleJob(triggerKey);
      // 删除任务
      scheduler.deleteJob(JobKey.jobKey(jobName, jobGroupName));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   *
   * 功能：启动所有定时任务
   */
  public static void startJobs() {
    try {
      Scheduler scheduler = schedulerFactory.getScheduler();
      scheduler.start();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static boolean checkExist(String jobName, String jobGroupName) {
    try {
      Scheduler scheduler = schedulerFactory.getScheduler();
      return scheduler.checkExists(JobKey.jobKey(jobName, jobGroupName));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * 功能：关闭所有定时任务
   */
  public static void shutdownJobs() {
    try {
      Scheduler scheduler = schedulerFactory.getScheduler();
      if (!scheduler.isShutdown()) {
        scheduler.shutdown();
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
