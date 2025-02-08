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
package top.rslly.iot.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import top.rslly.iot.dao.EventStorageRepository;
import top.rslly.iot.dao.EventStorageTimeRepository;
import top.rslly.iot.dao.ProductDeviceRepository;
import top.rslly.iot.models.EventStorageEntity;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import javax.annotation.Resource;
import java.util.List;

@Service
public class EventStorageServiceImpl implements EventStorageService {
  @Value("${storage.database}")
  private String database;

  @Resource
  private EventStorageRepository eventStorageRepository;
  @Resource
  private ProductDeviceRepository deviceRepository;
  @Resource
  private EventStorageTimeRepository eventStorageTimeRepository;

  @Override
  public void insert(EventStorageEntity eventStorageEntity) {
    if (database.equals("influxdb")) {
      eventStorageTimeRepository.insert(eventStorageEntity);
    } else
      eventStorageRepository.save(eventStorageEntity);
  }

  @Override
  public JsonResult<?> findAllByTimeBetweenAndDeviceNameAndJsonKey(long time, long time2,
      String name, String jsonKey) {
    var productDeviceEntities = deviceRepository.findAllByName(name);
    if (productDeviceEntities.isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    int deviceId = productDeviceEntities.get(0).getId();
    List<EventStorageEntity> res;
    if (database.equals("influxdb")) {
      res = eventStorageTimeRepository.findAllByTimeBetweenAndDeviceIdAndJsonKey(time, time2,
          deviceId, jsonKey);
    } else {
      res = eventStorageRepository.findAllByTimeBetweenAndDeviceIdAndJsonKey(time, time2, deviceId,
          jsonKey);
    }
    if (res.isEmpty())
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    return ResultTool.success(res);
  }

  @Override
  public List<EventStorageEntity> findAllByTimeBetweenAndDeviceIdAndJsonKey(long time, long time2,
      int deviceId, String jsonKey) {
    if (database.equals("influxdb")) {
      return eventStorageTimeRepository.findAllByTimeBetweenAndDeviceIdAndJsonKey(time, time2,
          deviceId,
          jsonKey);
    } else
      return eventStorageRepository.findAllByTimeBetweenAndDeviceIdAndJsonKey(time, time2, deviceId,
          jsonKey);
  }

  @Override
  public void deleteAllByDeviceId(int deviceId) {
    if (database.equals("influxdb")) {
      if (!eventStorageTimeRepository.findAllByDeviceId(deviceId).isEmpty())
        eventStorageTimeRepository.deleteByDeviceId(deviceId);
    } else if (!eventStorageRepository.findAllByDeviceId(deviceId).isEmpty())
      eventStorageRepository.deleteByDeviceId(deviceId);
  }

  @Override
  public void deleteAllByDeviceIdAndJsonKey(int deviceId, String jsonKey) {

    if (database.equals("influxdb")) {
      if (!eventStorageTimeRepository.findAllByDeviceIdAndJsonKey(deviceId, jsonKey).isEmpty())
        eventStorageTimeRepository.deleteAllByDeviceIdAndJsonKey(deviceId, jsonKey);
    } else if (!eventStorageRepository.findAllByDeviceIdAndJsonKey(deviceId, jsonKey).isEmpty())
      eventStorageRepository.deleteAllByDeviceIdAndJsonKey(deviceId, jsonKey);
  }


  @Override
  public void deleteAllByTimeBeforeAndDeviceIdAndJsonKey(long time, int deviceId, String jsonKey) {
    if (database.equals("influxdb")) {
      eventStorageTimeRepository.deleteAllByTimeBeforeAndDeviceIdAndJsonKey(time, deviceId,
          jsonKey);
    } else
      eventStorageRepository.deleteAllByTimeBeforeAndDeviceIdAndJsonKey(time, deviceId, jsonKey);
  }
}
