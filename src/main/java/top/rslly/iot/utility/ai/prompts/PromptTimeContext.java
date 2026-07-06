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
package top.rslly.iot.utility.ai.prompts;

import cn.hutool.core.date.ChineseDate;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class PromptTimeContext {
  private PromptTimeContext() {}

  public static Map<String, String> build() {
    Date date = new Date();
    Map<String, String> params = new HashMap<>();
    params.put("time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date));
    params.put("weekday", new SimpleDateFormat("EEEE", Locale.CHINA).format(date));
    params.put("lunar_date", getLunarDateString(date));
    return params;
  }

  private static String getLunarDateString(Date date) {
    try {
      ChineseDate chineseDate = new ChineseDate(date);
      return chineseDate.getCyclical()
          + "(" + chineseDate.getChineseZodiac() + "年)"
          + chineseDate.getChineseMonthName()
          + chineseDate.getChineseDay()
          + chineseDate.getFestivals();
    } catch (Exception e) {
      return "";
    }
  }
}
