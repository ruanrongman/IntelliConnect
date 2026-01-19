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
package top.rslly.iot.config;

import org.hibernate.HibernateException;
import org.hibernate.dialect.MySQL5Dialect;

import java.sql.Types;

public class MySQLUTF8Dialect extends MySQL5Dialect {
  public MySQLUTF8Dialect() {
    super();
    registerColumnType(Types.VARCHAR,
        "VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
    registerColumnType(Types.CHAR, "CHAR(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
    registerColumnType(Types.LONGVARCHAR, "TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
    registerColumnType(Types.CLOB, "TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
    registerColumnType(Types.NVARCHAR,
        "VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
    registerColumnType(Types.NCHAR, "CHAR(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
    registerColumnType(Types.LONGNVARCHAR, "TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
  }

  @Override
  public String getTableTypeString() {
    // 为所有表添加UTF-8字符集配置
    return " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
  }

  @Override
  public String getTypeName(int code, long length, int precision, int scale)
      throws HibernateException {
    // 确保特定类型使用UTF-8
    if (code == Types.VARCHAR) {
      if (length <= 255) {
        return "VARCHAR(" + length + ") CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";
      } else if (length <= 65535) {
        return "TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";
      }
    }
    return super.getTypeName(code, length, precision, scale);
  }

  @Override
  public boolean supportsColumnCheck() {
    // MySQL 5.7支持列级约束检查
    return true;
  }
}
