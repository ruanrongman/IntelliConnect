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

import top.rslly.iot.models.EventStorageEntity;
import top.rslly.iot.utility.influxdb.InfluxDBBaseMapper;
import top.rslly.iot.utility.influxdb.ano.Delete;
import top.rslly.iot.utility.influxdb.ano.Param;
import top.rslly.iot.utility.influxdb.ano.Select;

import java.util.List;

public interface EventStorageTimeRepository extends InfluxDBBaseMapper<EventStorageEntity> {
  @Select(
      value = "SELECT * FROM \"event_storage\" where time >=#{time}ms and time <=#{time2}ms and deviceId='#{deviceId}'",
      resultType = EventStorageEntity.class)
  List<EventStorageEntity> findAllByTimeBetweenAndDeviceId(@Param("time") long time,
      @Param("time2") long time2, @Param("deviceId") int deviceId);

  @Select(
      value = "SELECT * FROM \"event_storage\" where time >=#{time}ms and time <=#{time2}ms and deviceId='#{deviceId}' and jsonKey=#{jsonKey}",
      resultType = EventStorageEntity.class)
  List<EventStorageEntity> findAllByTimeBetweenAndDeviceIdAndJsonKey(@Param("time") long time,
      @Param("time2") long time2, @Param("deviceId") int deviceId,
      @Param("jsonKey") String jsonKey);

  @Select(
      value = "SELECT * FROM \"event_storage\" WHERE deviceId='#{deviceId}' and jsonKey=#{jsonKey} ORDER BY time DESC LIMIT 1",
      resultType = EventStorageEntity.class)
  List<EventStorageEntity> findAllBySort(int deviceId, String jsonKey);

  @Select(
      value = "SELECT * FROM \"event_storage\" WHERE deviceId='#{deviceId}'",
      resultType = EventStorageEntity.class)
  List<EventStorageEntity> findAllByDeviceId(@Param("deviceId") int deviceId);

  @Select(
      value = "SELECT * FROM \"event_storage\" WHERE deviceId='#{deviceId}' and jsonKey=#{jsonKey}",
      resultType = EventStorageEntity.class)
  List<EventStorageEntity> findAllByDeviceIdAndJsonKey(@Param("deviceId") int deviceId,
      @Param("jsonKey") String jsonKey);

  @Delete(
      value = "DELETE FROM \"event_storage\" where time <=#{time}ms and deviceId='#{deviceId}' and jsonKey=#{jsonKey}")
  void deleteAllByTimeBeforeAndDeviceIdAndJsonKey(@Param("time") long time,
      @Param("deviceId") int deviceId, @Param("jsonKey") String jsonKey);

  @Delete(
      value = "DELETE FROM \"event_storage\" where deviceId='#{deviceId}' and jsonKey=#{jsonKey}")
  void deleteAllByDeviceIdAndJsonKey(@Param("deviceId") int deviceId,
      @Param("jsonKey") String jsonKey);

  @Delete(value = "DELETE FROM \"event_storage\" where deviceId='#{deviceId}'")
  void deleteByDeviceId(@Param("deviceId") int deviceId);
}
