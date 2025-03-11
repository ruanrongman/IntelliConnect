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
package top.rslly.iot.services.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import top.rslly.iot.dao.DataRepository;
import top.rslly.iot.dao.ProductDeviceRepository;
import top.rslly.iot.dao.DataTimeRepository;
import top.rslly.iot.models.DataEntity;
import top.rslly.iot.services.storage.DataService;
import top.rslly.iot.utility.Cast;
import top.rslly.iot.utility.RedisUtil;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import javax.annotation.Resource;
import java.util.List;

@Service
public class DataServiceImpl implements DataService {

  @Value("${storage.database}")
  private String database;

  @Resource
  private DataRepository dataRepository;
  @Resource
  private ProductDeviceRepository deviceRepository;
  @Resource
  private DataTimeRepository dataTimeRepository;
  @Resource
  private RedisUtil redisUtil;

  @Override
  public void insert(DataEntity dataEntity) {
    if (database.equals("influxdb")) {
      dataTimeRepository.insert(dataEntity);
    } else
      dataRepository.save(dataEntity);
  }

  @Override
  public JsonResult<?> findAllByTimeBetweenAndDeviceNameAndJsonKey(long time, long time2,
      String name, String jsonKey) {
    var productDeviceEntities = deviceRepository.findAllByName(name);
    if (productDeviceEntities.isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    int deviceId = productDeviceEntities.get(0).getId();
    List<DataEntity> res;
    if (database.equals("influxdb")) {
      res = dataTimeRepository.findAllByTimeBetweenAndDeviceIdAndJsonKey(time, time2, deviceId,
          jsonKey);
    } else {
      res =
          dataRepository.findAllByTimeBetweenAndDeviceIdAndJsonKey(time, time2, deviceId, jsonKey);
    }
    if (res.isEmpty())
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    return ResultTool.success(res);
  }

  @Override
  public List<DataEntity> findAllByTimeBetweenAndDeviceIdAndJsonKey(long time, long time2,
      int deviceId, String jsonKey) {
    if (database.equals("influxdb")) {
      return dataTimeRepository.findAllByTimeBetweenAndDeviceIdAndJsonKey(time, time2, deviceId,
          jsonKey);
    } else
      return dataRepository.findAllByTimeBetweenAndDeviceIdAndJsonKey(time, time2, deviceId,
          jsonKey);
  }

  @Override
  public List<DataEntity> findAllBySort(int deviceId, String jsonKey) {
    List<DataEntity> res;
    var memory = redisUtil.get("property" + deviceId + jsonKey);
    if (memory == null) {
      if (database.equals("influxdb")) {
        res = dataTimeRepository.findAllBySort(deviceId, jsonKey);
      } else {
        res = dataRepository.findAllBySort(deviceId, jsonKey);
      }
      redisUtil.set("property" + deviceId + jsonKey, res, 60);
    } else {
      redisUtil.expire("property" + deviceId + jsonKey, 120);
      return Cast.castList(memory, DataEntity.class);
    }
    return res;
  }

  @Override
  public void deleteAllByDeviceId(int deviceId) {
    if (database.equals("influxdb")) {
      if (!dataTimeRepository.findAllByDeviceId(deviceId).isEmpty())
        dataTimeRepository.deleteByDeviceId(deviceId);
    } else {
      if (!dataRepository.findAllByDeviceId(deviceId).isEmpty())
        dataRepository.deleteByDeviceId(deviceId);
    }
  }

  @Override
  public void deleteAllByDeviceIdAndJsonKey(int deviceId, String jsonKey) {
    if (database.equals("influxdb")) {
      if (!dataTimeRepository.findAllByDeviceIdAndJsonKey(deviceId, jsonKey).isEmpty())
        dataTimeRepository.deleteAllByDeviceIdAndJsonKey(deviceId, jsonKey);
    } else {
      if (!dataRepository.findAllByDeviceIdAndJsonKey(deviceId, jsonKey).isEmpty())
        dataRepository.deleteAllByDeviceIdAndJsonKey(deviceId, jsonKey);
    }
  }

  @Override
  public void deleteAllByTimeBeforeAndDeviceIdAndJsonKey(long time, int deviceId, String jsonKey) {
    if (database.equals("influxdb")) {
      dataTimeRepository.deleteAllByTimeBeforeAndDeviceIdAndJsonKey(time, deviceId, jsonKey);
    } else
      dataRepository.deleteAllByTimeBeforeAndDeviceIdAndJsonKey(time, deviceId, jsonKey);
  }

  @Override
  public JsonResult<?> metaData(int deviceId, String jsonKey) {
    List<DataEntity> res;
    var memory = redisUtil.get("property" + deviceId + jsonKey);
    if (memory == null) {
      if (database.equals("influxdb")) {
        res = dataTimeRepository.findAllBySort(deviceId, jsonKey);
      } else {
        res = dataRepository.findAllBySort(deviceId, jsonKey);
      }
      redisUtil.set("property" + deviceId + jsonKey, res, 60);
    } else {
      redisUtil.expire("property" + deviceId + jsonKey, 120);
      return ResultTool.success(memory);
    }
    if (res.isEmpty())
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    return ResultTool.success(res);
  }


}
