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

import java.io.IOException;
import java.util.List;

public class JsonCreate {
  public static StringBuffer create(List<String> key, List<String> value, List<String> type)
      throws IOException {
    if (key.size() == value.size() && value.size() == type.size()) {
      StringBuffer sBuffer = new StringBuffer("{");
      for (int i = 0; i < key.size(); i++) {
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
      throw new IOException("禁止三个输入长度不等的String数组！！！");
  }
}
