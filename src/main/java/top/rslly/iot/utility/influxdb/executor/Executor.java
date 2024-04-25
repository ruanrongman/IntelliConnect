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
package top.rslly.iot.utility.influxdb.executor;

import java.util.List;

public interface Executor {
  /**
   * 测试连接是否正常
   *
   * @return
   */
  Boolean ping();

  /**
   * 创建数据库 说明：方法参数没有指定时，默认使用配置文件中数据库名
   */
  void createDataBase(String... dataBaseName);

  /**
   * 删除数据库 说明：方法参数没有指定时，默认使用配置文件中数据库名
   */
  void deleteDataBase(String... dataBaseName);

  /**
   * 插入数据 支持：对象,集合(集合时对应实体类必须使用@Tag注解指定一个字段)
   *
   * @param object 数据
   */
  <T> void insert(T object);

  /**
   * 查询数据
   *
   * @param sql string
   */
  <T> List<T> query(Class<T> clazz, String sql);

  /**
   * delete data
   * 
   * @param sql string
   */
  void delete(String sql);
}
