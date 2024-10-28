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

import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class JsonCreate {
  public static StringBuffer create(List<String> key, List<String> value, List<String> type,
      List<String> dataMax, List<String> dataMin)
      throws IOException {
    if (key.size() == value.size() && value.size() == type.size() && type.size() == dataMax.size()
        && dataMax.size() == dataMin.size()) {
      StringBuffer sBuffer = new StringBuffer("{");
      for (int i = 0; i < key.size(); i++) {
        if (dataMax.get(i) != null && dataMin.get(i) != null) {
          checkValueRange(value.get(i), dataMax.get(i), dataMin.get(i));
        }
        sBuffer.append("\"");
        sBuffer.append(key.get(i));
        sBuffer.append("\"");
        sBuffer.append(":");
        if (type.get(i).equals("string"))
          sBuffer.append("\"");
        sBuffer.append(value.get(i));
        if (type.get(i).equals("string"))
          sBuffer.append("\"");
        if (i < key.size() - 1)
          sBuffer.append(",");
        else
          break;
      }
      sBuffer.append("}");
      return sBuffer;
    } else
      throw new IOException("禁止五个输入长度不等的String数组！！！");
  }

  private static void checkValueRange(String valueStr, String maxStr, String minStr)
      throws IllegalArgumentException {
    try {
      BigDecimal value = new BigDecimal(valueStr).setScale(8, RoundingMode.HALF_UP);
      BigDecimal max = new BigDecimal(maxStr).setScale(8, RoundingMode.HALF_UP);
      BigDecimal min = new BigDecimal(minStr).setScale(8, RoundingMode.HALF_UP);

      if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
        throw new IllegalArgumentException("数据超出范围！");
      }
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("无法解析数值", e);
    }
  }
}
