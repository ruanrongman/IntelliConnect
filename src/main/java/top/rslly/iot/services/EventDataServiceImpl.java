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

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.rslly.iot.dao.EventDataRepository;
import top.rslly.iot.dao.ProductModelRepository;
import top.rslly.iot.models.EventDataEntity;
import top.rslly.iot.models.ProductModelEntity;
import top.rslly.iot.param.request.EventData;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;

@Service
public class EventDataServiceImpl implements EventDataService {
  @Resource
  private ProductModelRepository productModelRepository;
  @Resource
  private EventDataRepository eventDataRepository;

  @Override
  public List<EventDataEntity> findAllByModelId(int modelId) {
    return eventDataRepository.findAllByModelId(modelId);
  }

  @Override
  public JsonResult<?> getEventData() {
    var result = eventDataRepository.findAll();
    if (result.isEmpty()) {
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    } else
      return ResultTool.success(result);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> postEventData(EventData eventData) {
    EventDataEntity eventDataEntity = new EventDataEntity();
    BeanUtils.copyProperties(eventData, eventDataEntity);
    List<ProductModelEntity> result =
        productModelRepository.findAllById(eventDataEntity.getModelId());
    List<EventDataEntity> p1 = eventDataRepository
        .findAllByModelIdAndJsonKey(eventDataEntity.getModelId(), eventDataEntity.getJsonKey());
    // List<ProductDataEntity> p2 = productDataRepository.findAllByType(productData.getType());
    HashMap<String, Integer> typeMap = new HashMap<>();
    typeMap.put("string", 1);
    typeMap.put("int", 2);
    typeMap.put("float", 3);
    var p2 = typeMap.get(eventDataEntity.getType());
    if (result.isEmpty() || !p1.isEmpty() || p2 == null)
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    else {
      EventDataEntity eventDataEntity1 = eventDataRepository.save(eventDataEntity);
      return ResultTool.success(eventDataEntity1);
    }
  }

  @Override
  public JsonResult<?> deleteEventData(int id) {
    List<EventDataEntity> result = eventDataRepository.deleteById(id);
    if (result.isEmpty())
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    else {
      return ResultTool.success(result);
    }

  }
}
