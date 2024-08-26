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
package top.rslly.iot.dao;

import top.rslly.iot.models.DataEntity;
import top.rslly.iot.utility.influxdb.InfluxDBBaseMapper;
import top.rslly.iot.utility.influxdb.ano.Delete;
import top.rslly.iot.utility.influxdb.ano.Param;
import top.rslly.iot.utility.influxdb.ano.Select;

import java.util.List;

public interface DataTimeRepository extends InfluxDBBaseMapper<DataEntity> {
  @Select(value = "SELECT * FROM \"data\" where time >=#{time1}ms and time <=#{time2}ms",
      resultType = DataEntity.class)
  List<DataEntity> findAllByTimeBetween(@Param("time1") Long time1, @Param("time2") Long time2);

  @Select(
      value = "SELECT * FROM \"data\" where time >=#{time}ms and time <=#{time2}ms and deviceId='#{deviceId}'",
      resultType = DataEntity.class)
  List<DataEntity> findAllByTimeBetweenAndDeviceId(@Param("time") long time,
      @Param("time2") long time2, @Param("deviceId") int deviceId);

  @Select(
      value = "SELECT * FROM \"data\" where time >=#{time}ms and time <=#{time2}ms and deviceId='#{deviceId}' and jsonKey=#{jsonKey}",
      resultType = DataEntity.class)
  List<DataEntity> findAllByTimeBetweenAndDeviceIdAndJsonKey(@Param("time") long time,
      @Param("time2") long time2, @Param("deviceId") int deviceId,
      @Param("jsonKey") String jsonKey);

  @Select(
      value = "SELECT * FROM \"data\" WHERE deviceId='#{deviceId}' and jsonKey=#{jsonKey} ORDER BY time DESC LIMIT 1",
      resultType = DataEntity.class)
  List<DataEntity> findAllBySort(int deviceId, String jsonKey);

  @Delete(
      value = "DELETE FROM \"data\" where time <=#{time}ms and deviceId='#{deviceId}' and jsonKey=#{jsonKey}")
  void deleteAllByTimeBeforeAndDeviceIdAndJsonKey(@Param("time") long time,
      @Param("deviceId") int deviceId, @Param("jsonKey") String jsonKey);
}
