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
package top.rslly.iot.utility.influxdb;

import top.rslly.iot.utility.influxdb.ano.Param;

import java.lang.reflect.Parameter;

public class ParameterHandler {
  /**
   * 拼接sql
   *
   * @param parameters 参数名
   * @param args 参数实际值
   * @param sql 未拼接参数的sql语句
   * @return 拼接好的sql
   */
  public String handleParameter(Parameter[] parameters, Object[] args, String sql) {
    for (int i = 0; i < parameters.length; i++) {
      Class<?> parameterType = parameters[i].getType();
      String parameterName = parameters[i].getName();

      Param param = parameters[i].getAnnotation(Param.class);
      if (param != null) {
        parameterName = param.value();
      }

      if (parameterType == String.class) {
        sql = sql.replaceAll("\\#\\{" + parameterName + "\\}", "'" + args[i] + "'");
      } else {
        sql = sql.replaceAll("\\#\\{" + parameterName + "\\}", args[i].toString());
      }
      sql = sql.replaceAll("\\$\\{" + parameterName + "\\}", args[i].toString());
    }
    return sql;
  }
}
