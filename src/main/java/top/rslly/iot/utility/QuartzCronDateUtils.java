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

import org.quartz.CronExpression;

import java.text.SimpleDateFormat;
import java.util.Date;

public class QuartzCronDateUtils {
  public static String formatDateByPattern(Date date, String dateFormat) {
    SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
    String formatTimeStr = null;
    if (date != null) {
      formatTimeStr = sdf.format(date);
    }
    return formatTimeStr;
  }

  /***
   * @param date :时间点
   * @return
   */
  public static String getCron(java.util.Date date) {
    String dateFormat = "ss mm HH dd MM ? yyyy";

    return formatDateByPattern(date, dateFormat);
  }

  public static boolean isCronExpired(String cronExpression) {
    if (cronExpression == null || cronExpression.trim().isEmpty()) {
      return true;
    }
    try {
      Date now = new Date();
      CronExpression expression = new CronExpression(cronExpression);
      Date nextFireTime = expression.getNextValidTimeAfter(now);
      return nextFireTime.before(now);
    } catch (Exception e) {
      return true;
    }
  }
}
