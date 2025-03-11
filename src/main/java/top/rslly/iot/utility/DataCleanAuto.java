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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.rslly.iot.models.ProductDataEntity;
import top.rslly.iot.models.ProductDeviceEntity;
import top.rslly.iot.services.storage.DataServiceImpl;
import top.rslly.iot.services.thingsModel.ProductDataServiceImpl;
import top.rslly.iot.services.thingsModel.ProductDeviceServiceImpl;

import java.util.Calendar;
import java.util.List;

@Slf4j
@Component
public class DataCleanAuto {

  @Autowired
  private ProductDataServiceImpl productDataService;
  @Autowired
  private ProductDeviceServiceImpl productDeviceService;
  @Autowired
  private DataServiceImpl dataService;

  @Scheduled(cron = "0 0 0 * * ?")
  public void task() {
    log.info("时间到了，执行清理任务！！！");
    // 当前时间日历
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.DATE, -7);
    long lastTime = calendar.getTime().getTime();
    // long lastTime = System.currentTimeMillis();
    // System.out.println(lastTime);
    List<ProductDataEntity> productDataEntityList =
        productDataService.findAllByStorageType(DataSave.week.getStorageType());
    for (var s : productDataEntityList) {
      List<ProductDeviceEntity> productDeviceEntityList =
          productDeviceService.findAllByModelId(s.getModelId());
      if (productDeviceEntityList.isEmpty())
        continue;
      for (var device : productDeviceEntityList) {
        /*
         * var dataId = dataService.findAllByTimeBetweenAndDeviceIdAndJsonKey(0, lastTime,
         * device.getId(), s.getJsonKey()); if (dataId.isEmpty()) continue; List<Integer> idList =
         * new ArrayList<>(); for (var id : dataId) { idList.add(id.getId()); } //
         * System.out.println(dataId.get(0).getId()); dataService.deleteByList(idList);
         */
        dataService.deleteAllByTimeBeforeAndDeviceIdAndJsonKey(lastTime, device.getId(),
            s.getJsonKey());
      }
    }
  }
}
